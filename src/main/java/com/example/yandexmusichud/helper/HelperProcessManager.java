package com.example.yandexmusichud.helper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;

public class HelperProcessManager {
    public static HelperProcessManager INSTANCE;
    private static final String HELPER_EXE_NAME = "YandexMusicHelper.exe";
    private static final String DUCK_HELPER_EXE_NAME = "SessionDuckHelper.exe";
    private static final String PORT_FILE_NAME = "yandexmusichud_port.txt";
    private static final String PID_FILE_NAME = "yandexmusichud_pid.txt";
    private Process helperProcess;
    private Process duckHelperProcess;
    private OutputStream duckHelperStdin;
    private volatile float lastSentDuckFactor = 1.0f;
    private double linuxBaseVolume = -1.0;
    private int macSpotifyBaseVol = -1;
    private int macMusicBaseVol = -1;
    private final ScheduledExecutorService duckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MusicSubtitles-DuckBridge");
        t.setDaemon(true);
        return t;
    });
    private int port = -1;
    private boolean started = false;

    private HttpServer linuxServer;
    private ScheduledExecutorService linuxPoller;
    private HttpServer macServer;
    private ScheduledExecutorService macPoller;

    private volatile String cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
    private volatile String lastCoverUrl = null;
    private volatile String cachedCoverBase64 = null;
    private volatile String lastCoverHash = null;
    private volatile String readyCoverUrl = null;
    private volatile String readyCoverHash = null;

    private volatile long lastLinuxSampleTime = 0L;
    private volatile long lastLinuxPosMs = 0L;
    private volatile long lastLinuxDurMs = 0L;
    private volatile String lastLinuxStatus = "stopped";
    private volatile String lastLinuxTitle = "";
    private volatile String lastLinuxArtist = "";
    private volatile String lastLinuxAlbum = "";
    private volatile String lastLinuxCoverHash = null;
    private volatile boolean lastLinuxActive = false;

    private volatile List<String> cachedPlayerctlPrefix = null;
    private volatile boolean linuxPlayerctlAvailable = false;
    private volatile long lastLinuxPlayerctlCheckTime = 0L;
    private volatile String cachedNowPlayingCliPath = null;
    private volatile boolean checkedNowPlayingCli = false;
    private static volatile boolean shutdownHookRegistered = false;

    public HelperProcessManager() {
        INSTANCE = this;
        if (!shutdownHookRegistered) {
            shutdownHookRegistered = true;
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (INSTANCE != null) {
                            INSTANCE.stop();
                        }
                    } catch (Throwable ignored) {}
                }, "MusicSubtitles-HelperShutdownHook"));
            } catch (Throwable ignored) {}
        }
    }

    public static boolean isRunningInFlatpak() {
        return System.getenv("FLATPAK_ID") != null
            || System.getenv("FLATPAK_SANDBOX_DIR") != null
            || Files.exists(Path.of("/.flatpak-info"))
            || (Files.exists(Path.of("/app")) && Files.exists(Path.of("/usr/bin/flatpak-spawn")));
    }

    public int getPort() {
        return this.port;
    }

    public boolean isStarted() {
        return this.started;
    }

    private boolean isPortAlive(int checkPort) {
        if (checkPort <= 0) {
            return false;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(700L)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + checkPort + "/now-playing")).GET().timeout(Duration.ofMillis(700L)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        }
        catch (Exception e) {
            return false;
        }
    }

    private void killStaleHelperProcesses() {
        try {
            for (Path pidFile : new Path[]{this.getTempDir().resolve(PID_FILE_NAME), FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve(PID_FILE_NAME)}) {
                if (!Files.exists(pidFile, new LinkOption[0])) continue;
                try {
                    long pid = Long.parseLong(Files.readString(pidFile).trim());
                    ProcessHandle.of(pid).ifPresent(ph -> {
                        String cmd = ph.info().command().orElse("").toLowerCase();
                        if (cmd.contains("yandexmusichelper")) {
                            ph.destroyForcibly();
                        }
                    });
                } catch (Exception ignored) {}
                try {
                    Files.deleteIfExists(pidFile);
                } catch (Exception ignored) {}
            }
            Files.deleteIfExists(this.getTempDir().resolve(PORT_FILE_NAME));
            Files.deleteIfExists(FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve(PORT_FILE_NAME));
        } catch (Exception ignored) {}
        try {
            ProcessHandle.allProcesses().forEach(ph -> {
                try {
                    String cmd = ph.info().command().orElse("").toLowerCase();
                    if (cmd.endsWith("yandexmusichelper.exe")) {
                        ph.destroyForcibly();
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public int start() {
        INSTANCE = this;
        int existingPort = this.readPortFromTempFile();
        if (existingPort > 0 && this.isHelperAlreadyRunning() && this.isPortAlive(existingPort)) {
            this.port = existingPort;
            this.started = true;
            return this.port;
        }
        this.killStaleHelperProcesses();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            int linuxPort = this.startLinuxMprisBridge();
            if (linuxPort > 0) {
                this.port = linuxPort;
                this.started = true;
                return this.port;
            }
            return -1;
        }
        if (os.contains("mac")) {
            int macPort = this.startMacOsBridge();
            if (macPort > 0) {
                this.port = macPort;
                this.started = true;
                return this.port;
            }
            return -1;
        }
        if (!os.contains("win")) {
            return -1;
        }
        Path helperDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
        Path helperExePath = helperDir.resolve(HELPER_EXE_NAME);
        try {
            this.extractHelperIfNeeded(helperExePath);
        }
        catch (IOException e) {
            return -1;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(helperExePath.toString());
            pb.directory(helperDir.toFile());
            pb.redirectErrorStream(true);
            Path logFile = helperDir.resolve("helper.log");
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));
            this.helperProcess = pb.start();
            this.port = this.waitForPortFile(10000L);
            if (this.port > 0) {
                this.started = true;
            } else {
                this.stop();
            }
        }
        catch (IOException e) {
            return -1;
        }
        return this.port;
    }

    public boolean isLinuxPlayerctlAvailable() {
        long now = System.currentTimeMillis();
        if (this.cachedPlayerctlPrefix != null && this.linuxPlayerctlAvailable) {
            return true;
        }
        if (now - this.lastLinuxPlayerctlCheckTime > 3000L) {
            this.lastLinuxPlayerctlCheckTime = now;
            this.resolvePlayerctlPrefix();
        }
        return this.linuxPlayerctlAvailable;
    }

    private static String runCommandWithTimeout(List<String> cmd, long timeoutMs) {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            p = pb.start();
            Process procRef = p;
            CompletableFuture<byte[]> reader = CompletableFuture.supplyAsync(() -> {
                try (InputStream is = procRef.getInputStream()) {
                    return is.readAllBytes();
                } catch (Exception e) {
                    return new byte[0];
                }
            });
            boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                return null;
            }
            if (p.exitValue() != 0) {
                return null;
            }
            byte[] bytes = reader.get(150L, TimeUnit.MILLISECONDS);
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
            return null;
        }
    }

    public List<String> getLinuxActivePlayers() {
        if (this.isLinuxPlayerctlAvailable()) {
            try {
                List<String> cmd = new ArrayList<>(this.resolvePlayerctlPrefix());
                cmd.add("-l");
                String output = runCommandWithTimeout(cmd, 350L);
                if (output != null && !output.isEmpty()) {
                    String[] lines = output.split("\\r?\\n");
                    List<String> list = new ArrayList<>();
                    for (String l : lines) {
                        String tr = l.trim();
                        if (!tr.isEmpty()) {
                            list.add(tr);
                        }
                    }
                    return list;
                }
            } catch (Exception ignored) {}
        }
        try {
            List<String> busCmd = isRunningInFlatpak()
                ? List.of("flatpak-spawn", "--host", "busctl", "--user", "list")
                : List.of("busctl", "--user", "list");
            String output = runCommandWithTimeout(busCmd, 350L);
            if (output != null && !output.isEmpty()) {
                String[] lines = output.split("\\r?\\n");
                List<String> list = new ArrayList<>();
                for (String l : lines) {
                    if (l.contains("org.mpris.MediaPlayer2.")) {
                        int idx = l.indexOf("org.mpris.MediaPlayer2.");
                        String sub = l.substring(idx + "org.mpris.MediaPlayer2.".length());
                        String name = sub.split("\\s+")[0].trim();
                        if (!name.isEmpty() && !list.contains(name)) {
                            list.add(name);
                        }
                    }
                }
                return list;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private List<String> resolvePlayerctlPrefix() {
        if (this.cachedPlayerctlPrefix != null && this.linuxPlayerctlAvailable) {
            return this.cachedPlayerctlPrefix;
        }
        List<List<String>> candidates = new ArrayList<>();
        if (isRunningInFlatpak()) {
            candidates.add(List.of("flatpak-spawn", "--host", "playerctl"));
            candidates.add(List.of("playerctl"));
            candidates.add(List.of("flatpak-spawn", "--host", "/usr/bin/playerctl"));
        } else {
            candidates.add(List.of("playerctl"));
            candidates.add(List.of("/usr/bin/playerctl"));
            candidates.add(List.of("/usr/local/bin/playerctl"));
            candidates.add(List.of("flatpak-spawn", "--host", "playerctl"));
        }
        for (List<String> prefix : candidates) {
            try {
                List<String> testCmd = new ArrayList<>(prefix);
                testCmd.add("--version");
                Process p = new ProcessBuilder(testCmd).redirectErrorStream(true).start();
                boolean finished = p.waitFor(350, TimeUnit.MILLISECONDS);
                if (finished && p.exitValue() == 0) {
                    this.cachedPlayerctlPrefix = prefix;
                    this.linuxPlayerctlAvailable = true;
                    return prefix;
                }
                p.destroyForcibly();
            } catch (Exception ignored) {}
        }
        this.linuxPlayerctlAvailable = false;
        List<String> fallback = isRunningInFlatpak()
            ? List.of("flatpak-spawn", "--host", "playerctl")
            : List.of("playerctl");
        return fallback;
    }

    private int startLinuxMprisBridge() {
        try {
            int targetPort = 38472;
            try {
                this.linuxServer = HttpServer.create(new InetSocketAddress("127.0.0.1", targetPort), 0);
            } catch (Exception ex) {
                this.linuxServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                targetPort = this.linuxServer.getAddress().getPort();
            }

            this.linuxServer.createContext("/now-playing", exchange -> {
                String json;
                if (this.lastLinuxActive && "playing".equalsIgnoreCase(this.lastLinuxStatus) && this.lastLinuxSampleTime > 0L) {
                    long elapsed = Math.max(0L, System.currentTimeMillis() - this.lastLinuxSampleTime);
                    long livePos = this.lastLinuxPosMs + elapsed;
                    if (this.lastLinuxDurMs > 0L) {
                        livePos = Math.min(livePos, this.lastLinuxDurMs);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    sb.append("\"isActive\":true,");
                    sb.append("\"title\":\"").append(escapeJson(this.lastLinuxTitle)).append("\",");
                    sb.append("\"artist\":\"").append(escapeJson(this.lastLinuxArtist)).append("\",");
                    sb.append("\"album\":\"").append(escapeJson(this.lastLinuxAlbum)).append("\",");
                    sb.append("\"status\":\"").append(this.lastLinuxStatus).append("\",");
                    sb.append("\"positionMs\":").append(livePos).append(",");
                    sb.append("\"durationMs\":").append(this.lastLinuxDurMs).append(",");
                    if (this.lastLinuxCoverHash != null) {
                        sb.append("\"coverHash\":\"").append(this.lastLinuxCoverHash).append("\",");
                    }
                    sb.append("\"platform\":\"player\",");
                    sb.append("\"platformName\":\"Linux Media\"");
                    sb.append("}");
                    json = sb.toString();
                } else {
                    json = this.cachedNowPlayingJson;
                }
                byte[] resp = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            });

            this.linuxServer.createContext("/cover", exchange -> {
                String b64 = this.cachedCoverBase64;
                String json = b64 != null && !b64.isEmpty() ? "{\"coverBase64\":\"" + b64 + "\"}" : "{}";
                byte[] resp = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            });

            this.linuxServer.createContext("/control/play-pause", exchange -> {
                this.runLinuxControl("play-pause");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            this.linuxServer.createContext("/control/next", exchange -> {
                this.runLinuxControl("next");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            this.linuxServer.createContext("/control/previous", exchange -> {
                this.runLinuxControl("previous");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            this.linuxServer.createContext("/shutdown", exchange -> {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                this.stop();
            });

            this.linuxServer.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "MusicSubtitles-LinuxMprisServer");
                t.setDaemon(true);
                return t;
            }));
            this.linuxServer.start();

            this.writePortFiles(targetPort);

            this.linuxPoller = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MusicSubtitles-LinuxMprisPoller");
                t.setDaemon(true);
                return t;
            });
            this.linuxPoller.scheduleWithFixedDelay(this::pollLinuxMpris, 0, 250, TimeUnit.MILLISECONDS);

            return targetPort;
        } catch (Exception e) {
            return -1;
        }
    }

    private void pollLinuxMpris() {
        if (!this.isLinuxPlayerctlAvailable()) {
            this.lastLinuxActive = false;
            this.lastLinuxStatus = "stopped";
            this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
            return;
        }
        try {
            long queryStartTime = System.currentTimeMillis();
            List<String> cmd = new ArrayList<>(this.resolvePlayerctlPrefix());
            cmd.add("metadata");
            cmd.add("--format");
            cmd.add("{{status}}:::{{artist}}:::{{title}}:::{{album}}:::{{position}}:::{{mpris:length}}:::{{mpris:artUrl}}");
            String output = runCommandWithTimeout(cmd, 350L);
            if (output == null || output.isEmpty() || output.startsWith("No player")) {
                this.lastLinuxActive = false;
                this.lastLinuxStatus = "stopped";
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
                return;
            }
            String[] parts = output.split(":::", -1);
            if (parts.length < 3) {
                this.lastLinuxActive = false;
                this.lastLinuxStatus = "stopped";
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
                return;
            }
            String statusRaw = parts[0].trim().toLowerCase();
            String status = statusRaw.equals("playing") ? "playing" : (statusRaw.equals("paused") ? "paused" : "stopped");
            String artist = parts.length > 1 ? parts[1].trim() : "";
            String title = parts.length > 2 ? parts[2].trim() : "";
            String album = parts.length > 3 ? parts[3].trim() : "";
            long posMs = 0L;
            if (parts.length > 4) {
                try {
                    posMs = Math.max(0L, Long.parseLong(parts[4].trim()) / 1000L);
                } catch (Exception ignored) {}
            }
            long durMs = 0L;
            if (parts.length > 5) {
                try {
                    durMs = Math.max(0L, Long.parseLong(parts[5].trim()) / 1000L);
                } catch (Exception ignored) {}
            }
            String artUrl = parts.length > 6 ? parts[6].trim() : "";

            if (title.isEmpty() && artist.isEmpty()) {
                this.lastLinuxActive = false;
                this.lastLinuxStatus = "stopped";
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
                return;
            }

            if (this.lastLinuxActive && this.lastLinuxTitle.equals(title) && this.lastLinuxArtist.equals(artist) && "playing".equals(status)) {
                long elapsedSinceLast = Math.max(0L, queryStartTime - this.lastLinuxSampleTime);
                long localEst = this.lastLinuxPosMs + elapsedSinceLast;
                if (posMs < localEst && (localEst - posMs) < 1200L) {
                    posMs = localEst;
                }
            }

            String coverHash = null;
            if (!artUrl.isEmpty()) {
                String targetHash = Integer.toHexString(artUrl.hashCode());
                if (!artUrl.equals(this.lastCoverUrl)) {
                    this.lastCoverUrl = artUrl;
                    this.lastCoverHash = targetHash;
                    this.readyCoverUrl = null;
                    this.readyCoverHash = null;
                    this.cachedCoverBase64 = null;
                    this.fetchCoverAsync(artUrl, targetHash);
                }
                if (artUrl.equals(this.readyCoverUrl)) {
                    coverHash = this.readyCoverHash;
                }
            } else {
                this.lastCoverUrl = null;
                this.readyCoverUrl = null;
                this.readyCoverHash = null;
                this.cachedCoverBase64 = null;
            }

            this.lastLinuxSampleTime = queryStartTime;
            this.lastLinuxPosMs = posMs;
            this.lastLinuxDurMs = durMs;
            this.lastLinuxStatus = status;
            this.lastLinuxTitle = title;
            this.lastLinuxArtist = artist;
            this.lastLinuxAlbum = album;
            this.lastLinuxCoverHash = coverHash;
            this.lastLinuxActive = true;

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"isActive\":true,");
            sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
            sb.append("\"artist\":\"").append(escapeJson(artist)).append("\",");
            sb.append("\"album\":\"").append(escapeJson(album)).append("\",");
            sb.append("\"status\":\"").append(status).append("\",");
            sb.append("\"positionMs\":").append(posMs).append(",");
            sb.append("\"durationMs\":").append(durMs).append(",");
            if (coverHash != null) {
                sb.append("\"coverHash\":\"").append(coverHash).append("\",");
            }
            sb.append("\"platform\":\"player\",");
            sb.append("\"platformName\":\"Linux Media\"");
            sb.append("}");

            this.cachedNowPlayingJson = sb.toString();
            } catch (Exception e) {
                this.lastLinuxActive = false;
                this.lastLinuxStatus = "stopped";
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
            }
    }

    private void runLinuxControl(String action) {
        try {
            List<String> cmd = new ArrayList<>(this.resolvePlayerctlPrefix());
            cmd.add(action);
            new ProcessBuilder(cmd).start();
        } catch (Exception ignored) {}
    }

    private String resolveNowPlayingCli() {
        if (this.checkedNowPlayingCli) {
            return this.cachedNowPlayingCliPath;
        }
        this.checkedNowPlayingCli = true;
        String[] candidates = new String[] {
            "/opt/homebrew/bin/nowplaying-cli",
            "/usr/local/bin/nowplaying-cli",
            "nowplaying-cli"
        };
        for (String c : candidates) {
            try {
                Process p = new ProcessBuilder(c, "--help").start();
                boolean ok = p.waitFor(300, TimeUnit.MILLISECONDS);
                if (ok) {
                    this.cachedNowPlayingCliPath = c;
                    return c;
                }
                p.destroyForcibly();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private int startMacOsBridge() {
        try {
            int targetPort = 38472;
            try {
                this.macServer = HttpServer.create(new InetSocketAddress("127.0.0.1", targetPort), 0);
            } catch (Exception ex) {
                this.macServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                targetPort = this.macServer.getAddress().getPort();
            }

            this.macServer.createContext("/now-playing", exchange -> {
                byte[] resp = this.cachedNowPlayingJson.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            });

            this.macServer.createContext("/cover", exchange -> {
                String b64 = this.cachedCoverBase64;
                String json = b64 != null && !b64.isEmpty() ? "{\"coverBase64\":\"" + b64 + "\"}" : "{}";
                byte[] resp = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            });

            this.macServer.createContext("/control/play-pause", exchange -> {
                this.runMacControl("playpause");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            this.macServer.createContext("/control/next", exchange -> {
                this.runMacControl("next");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            this.macServer.createContext("/control/previous", exchange -> {
                this.runMacControl("previous");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            this.macServer.createContext("/shutdown", exchange -> {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                this.stop();
            });

            this.macServer.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "MusicSubtitles-MacOsServer");
                t.setDaemon(true);
                return t;
            }));
            this.macServer.start();

            this.writePortFiles(targetPort);

            this.macPoller = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MusicSubtitles-MacOsPoller");
                t.setDaemon(true);
                return t;
            });
            this.macPoller.scheduleWithFixedDelay(this::pollMacOsMedia, 0, 300, TimeUnit.MILLISECONDS);

            return targetPort;
        } catch (Exception e) {
            return -1;
        }
    }

    private void pollMacOsMedia() {
        try {
            String cli = this.resolveNowPlayingCli();
            if (cli != null) {
                String output = runCommandWithTimeout(List.of(cli, "get", "title", "artist", "album", "playbackRate", "elapsedTime", "duration"), 350L);
                if (output != null && !output.isEmpty() && !output.contains("null")) {
                    String[] lines = output.split("\\r?\\n");
                    if (lines.length >= 2 && (!lines[0].isBlank() || !lines[1].isBlank())) {
                        String title = lines[0].trim();
                        String artist = lines.length > 1 ? lines[1].trim() : "";
                        String album = lines.length > 2 ? lines[2].trim() : "";
                        double rate = 1.0;
                        if (lines.length > 3) {
                            try {
                                rate = Double.parseDouble(lines[3].trim());
                            } catch (Exception ignored) {}
                        }
                        String status = rate > 0.0 ? "playing" : "paused";
                        long posMs = 0L;
                        if (lines.length > 4) {
                            try {
                                posMs = Math.max(0L, (long)(Double.parseDouble(lines[4].trim()) * 1000.0));
                            } catch (Exception ignored) {}
                        }
                        long durMs = 0L;
                        if (lines.length > 5) {
                            try {
                                durMs = Math.max(0L, (long)(Double.parseDouble(lines[5].trim()) * 1000.0));
                            } catch (Exception ignored) {}
                        }
                        if (!title.isEmpty() || !artist.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("{");
                            sb.append("\"isActive\":true,");
                            sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
                            sb.append("\"artist\":\"").append(escapeJson(artist)).append("\",");
                            sb.append("\"album\":\"").append(escapeJson(album)).append("\",");
                            sb.append("\"status\":\"").append(status).append("\",");
                            sb.append("\"positionMs\":").append(posMs).append(",");
                            sb.append("\"durationMs\":").append(durMs).append(",");
                            sb.append("\"platform\":\"player\",");
                            sb.append("\"platformName\":\"macOS Media\"");
                            sb.append("}");
                            this.cachedNowPlayingJson = sb.toString();
                            return;
                        }
                    }
                }
            }

            String script = "set out to \"none\"\n"
                + "tell application \"System Events\"\n"
                + "    set isSpotify to (count (every process whose bundle identifier is \"com.spotify.client\")) > 0\n"
                + "    set isMusic to (count (every process whose bundle identifier is \"com.apple.Music\")) > 0\n"
                + "end tell\n"
                + "if isSpotify then\n"
                + "    tell application \"Spotify\"\n"
                + "        set pState to player state as string\n"
                + "        set tArtist to artist of current track\n"
                + "        set tName to name of current track\n"
                + "        set tAlbum to album of current track\n"
                + "        set tPos to player position\n"
                + "        set tDur to (duration of current track) / 1000\n"
                + "        set tArt to \"\"\n"
                + "        try\n"
                + "            set tArt to artwork url of current track\n"
                + "        end try\n"
                + "        set out to \"Spotify:::\" & pState & \":::\" & tArtist & \":::\" & tName & \":::\" & tAlbum & \":::\" & tPos & \":::\" & tDur & \":::\" & tArt\n"
                + "    end tell\n"
                + "else if isMusic then\n"
                + "    tell application \"Music\"\n"
                + "        set pState to player state as string\n"
                + "        set tArtist to artist of current track\n"
                + "        set tName to name of current track\n"
                + "        set tAlbum to album of current track\n"
                + "        set tPos to player position\n"
                + "        set tDur to duration of current track\n"
                + "        set out to \"Apple Music:::\" & pState & \":::\" & tArtist & \":::\" & tName & \":::\" & tAlbum & \":::\" & tPos & \":::\" & tDur & \":::\"\n"
                + "    end tell\n"
                + "end if\n"
                + "return out";

            String output = runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", script), 400L);
            if (output == null || output.isEmpty() || output.equals("none")) {
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
                return;
            }
            String[] parts = output.split(":::", -1);
            if (parts.length < 5) {
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
                return;
            }
            String platformName = parts[0].trim();
            String statusRaw = parts[1].trim().toLowerCase();
            String status = statusRaw.contains("play") ? "playing" : (statusRaw.contains("pause") ? "paused" : "stopped");
            String artist = parts[2].trim();
            String title = parts[3].trim();
            String album = parts[4].trim();
            long posMs = 0L;
            if (parts.length > 5) {
                try {
                    posMs = Math.max(0L, (long)(Double.parseDouble(parts[5].trim()) * 1000.0));
                } catch (Exception ignored) {}
            }
            long durMs = 0L;
            if (parts.length > 6) {
                try {
                    durMs = Math.max(0L, (long)(Double.parseDouble(parts[6].trim()) * 1000.0));
                } catch (Exception ignored) {}
            }
            String artUrl = parts.length > 7 ? parts[7].trim() : "";

            if (title.isEmpty() && artist.isEmpty()) {
                this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
                return;
            }

            String coverHash = null;
            if (!artUrl.isEmpty()) {
                String targetHash = Integer.toHexString(artUrl.hashCode());
                if (!artUrl.equals(this.lastCoverUrl)) {
                    this.lastCoverUrl = artUrl;
                    this.lastCoverHash = targetHash;
                    this.readyCoverUrl = null;
                    this.readyCoverHash = null;
                    this.cachedCoverBase64 = null;
                    this.fetchCoverAsync(artUrl, targetHash);
                }
                if (artUrl.equals(this.readyCoverUrl)) {
                    coverHash = this.readyCoverHash;
                }
            } else {
                this.lastCoverUrl = null;
                this.readyCoverUrl = null;
                this.readyCoverHash = null;
                this.cachedCoverBase64 = null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"isActive\":true,");
            sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
            sb.append("\"artist\":\"").append(escapeJson(artist)).append("\",");
            sb.append("\"album\":\"").append(escapeJson(album)).append("\",");
            sb.append("\"status\":\"").append(status).append("\",");
            sb.append("\"positionMs\":").append(posMs).append(",");
            sb.append("\"durationMs\":").append(durMs).append(",");
            if (coverHash != null) {
                sb.append("\"coverHash\":\"").append(coverHash).append("\",");
            }
            sb.append("\"platform\":\"player\",");
            sb.append("\"platformName\":\"").append(escapeJson(platformName)).append("\"");
            sb.append("}");

            this.cachedNowPlayingJson = sb.toString();
        } catch (Exception e) {
            this.cachedNowPlayingJson = "{\"isActive\":false,\"status\":\"stopped\"}";
        }
    }

    private void runMacControl(String action) {
        try {
            String cli = this.resolveNowPlayingCli();
            if (cli != null) {
                String cliAction = action.equals("playpause") ? "togglePlayPause" : (action.equals("next") ? "next" : "previous");
                new ProcessBuilder(cli, cliAction).start();
                return;
            }
            String appCmd = action.equals("playpause") ? "playpause" : (action.equals("next") ? "next track" : "previous track");
            String script = "tell application \"System Events\"\n"
                + "    set isSpotify to (count (every process whose bundle identifier is \"com.spotify.client\")) > 0\n"
                + "    set isMusic to (count (every process whose bundle identifier is \"com.apple.Music\")) > 0\n"
                + "end tell\n"
                + "if isSpotify then\n"
                + "    tell application \"Spotify\" to " + appCmd + "\n"
                + "else if isMusic then\n"
                + "    tell application \"Music\" to " + appCmd + "\n"
                + "end if";
            new ProcessBuilder("/usr/bin/osascript", "-e", script).start();
        } catch (Exception ignored) {}
    }

    private void fetchCoverAsync(String artUrl, String targetHash) {
        CompletableFuture.runAsync(() -> {
            try {
                byte[] bytes = null;
                if (artUrl.startsWith("file://")) {
                    Path p = Path.of(URI.create(artUrl));
                    if (Files.exists(p)) {
                        bytes = Files.readAllBytes(p);
                    }
                } else if (artUrl.startsWith("http://") || artUrl.startsWith("https://")) {
                    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(artUrl)).timeout(Duration.ofSeconds(4)).GET().build();
                    HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (res.statusCode() == 200) {
                        bytes = res.body();
                    }
                }
                if (bytes != null && bytes.length > 0 && artUrl.equals(this.lastCoverUrl)) {
                    this.cachedCoverBase64 = Base64.getEncoder().encodeToString(bytes);
                    this.readyCoverUrl = artUrl;
                    this.readyCoverHash = targetHash;
                    this.lastLinuxCoverHash = targetHash;
                }
            } catch (Exception ignored) {}
        });
    }

    public void applyExternalDuckingFactor(float rawFactor) {
        float clamped = Math.max(0.0f, Math.min(1.0f, rawFactor));
        float target = clamped >= 0.995f ? 1.0f : clamped;
        if (target == 1.0f && this.lastSentDuckFactor == 1.0f) {
            return;
        }
        if (target < 1.0f && Math.abs(target - this.lastSentDuckFactor) < 0.025f) {
            return;
        }
        this.lastSentDuckFactor = target;
        try {
            this.duckExecutor.execute(() -> this.sendExternalDuckFactorSync(target));
        } catch (Exception ignored) {}
    }

    private synchronized void sendExternalDuckFactorSync(float factor) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            try {
                if (this.duckHelperProcess == null || !this.duckHelperProcess.isAlive() || this.duckHelperStdin == null) {
                    Path helperDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
                    Path duckExePath = helperDir.resolve(DUCK_HELPER_EXE_NAME);
                    this.extractResourceIfNeeded("/helper/" + DUCK_HELPER_EXE_NAME, duckExePath);
                    if (!Files.exists(duckExePath)) {
                        return;
                    }
                    ProcessBuilder pb = new ProcessBuilder(duckExePath.toString(), String.valueOf(ProcessHandle.current().pid()));
                    pb.directory(helperDir.toFile());
                    pb.redirectErrorStream(true);
                    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    this.duckHelperProcess = pb.start();
                    this.duckHelperStdin = this.duckHelperProcess.getOutputStream();
                }
                String cmd = String.format(Locale.ROOT, "%.3f%n", factor);
                this.duckHelperStdin.write(cmd.getBytes(StandardCharsets.UTF_8));
                this.duckHelperStdin.flush();
            } catch (Exception e) {
                this.closeDuckHelper();
            }
        } else if (os.contains("linux")) {
            if (!this.isLinuxPlayerctlAvailable()) {
                return;
            }
            try {
                if (factor >= 0.995f) {
                    if (this.linuxBaseVolume >= 0.0) {
                        List<String> cmd = new ArrayList<>(this.resolvePlayerctlPrefix());
                        cmd.add("volume");
                        cmd.add(String.format(Locale.ROOT, "%.3f", this.linuxBaseVolume));
                        runCommandWithTimeout(cmd, 300L);
                        this.linuxBaseVolume = -1.0;
                    }
                } else {
                    if (this.linuxBaseVolume < 0.0) {
                        List<String> getCmd = new ArrayList<>(this.resolvePlayerctlPrefix());
                        getCmd.add("volume");
                        String cur = runCommandWithTimeout(getCmd, 300L);
                        if (cur != null && !cur.isEmpty()) {
                            this.linuxBaseVolume = Math.max(0.0, Math.min(1.5, Double.parseDouble(cur.trim())));
                        }
                    }
                    if (this.linuxBaseVolume > 0.0) {
                        List<String> setCmd = new ArrayList<>(this.resolvePlayerctlPrefix());
                        setCmd.add("volume");
                        setCmd.add(String.format(Locale.ROOT, "%.3f", this.linuxBaseVolume * (double)factor));
                        runCommandWithTimeout(setCmd, 300L);
                    }
                }
            } catch (Exception ignored) {}
        } else if (os.contains("mac")) {
            try {
                if (factor >= 0.995f) {
                    if (this.macSpotifyBaseVol >= 0) {
                        runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", "tell application \"Spotify\" to set sound volume to " + this.macSpotifyBaseVol), 300L);
                        this.macSpotifyBaseVol = -1;
                    }
                    if (this.macMusicBaseVol >= 0) {
                        runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", "tell application \"Music\" to set sound volume to " + this.macMusicBaseVol), 300L);
                        this.macMusicBaseVol = -1;
                    }
                } else {
                    String check = runCommandWithTimeout(List.of("/usr/bin/osascript", "-e",
                        "tell application \"System Events\" to return ((count (every process whose bundle identifier is \"com.spotify.client\")) > 0) as string & \":\" & ((count (every process whose bundle identifier is \"com.apple.Music\")) > 0) as string"), 300L);
                    if (check != null) {
                        String[] flags = check.split(":");
                        if (flags.length > 0 && "true".equalsIgnoreCase(flags[0].trim())) {
                            if (this.macSpotifyBaseVol < 0) {
                                String v = runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", "tell application \"Spotify\" to return sound volume"), 300L);
                                if (v != null) this.macSpotifyBaseVol = Integer.parseInt(v.trim());
                            }
                            if (this.macSpotifyBaseVol >= 0) {
                                int scaled = Math.max(0, Math.min(100, Math.round((float)this.macSpotifyBaseVol * factor)));
                                runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", "tell application \"Spotify\" to set sound volume to " + scaled), 300L);
                            }
                        }
                        if (flags.length > 1 && "true".equalsIgnoreCase(flags[1].trim())) {
                            if (this.macMusicBaseVol < 0) {
                                String v = runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", "tell application \"Music\" to return sound volume"), 300L);
                                if (v != null) this.macMusicBaseVol = Integer.parseInt(v.trim());
                            }
                            if (this.macMusicBaseVol >= 0) {
                                int scaled = Math.max(0, Math.min(100, Math.round((float)this.macMusicBaseVol * factor)));
                                runCommandWithTimeout(List.of("/usr/bin/osascript", "-e", "tell application \"Music\" to set sound volume to " + scaled), 300L);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private synchronized void closeDuckHelper() {
        if (this.duckHelperStdin != null) {
            try {
                this.duckHelperStdin.write("1.0\nexit\n".getBytes(StandardCharsets.UTF_8));
                this.duckHelperStdin.flush();
                this.duckHelperStdin.close();
            } catch (Exception ignored) {}
            this.duckHelperStdin = null;
        }
        if (this.duckHelperProcess != null) {
            try {
                if (!this.duckHelperProcess.waitFor(200L, TimeUnit.MILLISECONDS)) {
                    this.duckHelperProcess.destroyForcibly();
                }
            } catch (Exception ignored) {}
            this.duckHelperProcess = null;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        sb.append("0".repeat(Math.max(0, 4 - hex.length())));
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public void stop() {
        this.started = false;
        this.closeDuckHelper();
        if (this.linuxPoller != null) {
            this.linuxPoller.shutdownNow();
            this.linuxPoller = null;
        }
        if (this.linuxServer != null) {
            try {
                this.linuxServer.stop(0);
            } catch (Exception ignored) {}
            this.linuxServer = null;
        }
        if (this.macPoller != null) {
            this.macPoller.shutdownNow();
            this.macPoller = null;
        }
        if (this.macServer != null) {
            try {
                this.macServer.stop(0);
            } catch (Exception ignored) {}
            this.macServer = null;
        }
        if (this.port > 0) {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(800L)).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + this.port + "/shutdown")).POST(HttpRequest.BodyPublishers.noBody()).timeout(Duration.ofMillis(800L)).build();
                client.send(request, HttpResponse.BodyHandlers.discarding());
            }
            catch (Exception ignored) {}
        }
        if (this.helperProcess != null && this.helperProcess.isAlive()) {
            try {
                this.helperProcess.waitFor(500L, TimeUnit.MILLISECONDS);
            }
            catch (Exception ignored) {}
            if (this.helperProcess.isAlive()) {
                this.helperProcess.destroyForcibly();
            }
        }
        this.helperProcess = null;
        this.killStaleHelperProcesses();
    }

    private boolean isHelperAlreadyRunning() {
        Path pidFile = this.getTempDir().resolve(PID_FILE_NAME);
        if (!Files.exists(pidFile, new LinkOption[0])) {
            Path cfgPid = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve(PID_FILE_NAME);
            if (!Files.exists(cfgPid, new LinkOption[0])) {
                return false;
            }
            pidFile = cfgPid;
        }
        try {
            long pid = Long.parseLong(Files.readString(pidFile).trim());
            return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        }
        catch (Exception e) {
            return false;
        }
    }

    private void writePortFiles(int targetPort) {
        try {
            Path portFile = this.getTempDir().resolve(PORT_FILE_NAME);
            Files.writeString(portFile, String.valueOf(targetPort), StandardCharsets.UTF_8);
            Path pidFile = this.getTempDir().resolve(PID_FILE_NAME);
            Files.writeString(pidFile, String.valueOf(ProcessHandle.current().pid()), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        try {
            Path cfgDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
            Files.createDirectories(cfgDir);
            Files.writeString(cfgDir.resolve(PORT_FILE_NAME), String.valueOf(targetPort), StandardCharsets.UTF_8);
            Files.writeString(cfgDir.resolve(PID_FILE_NAME), String.valueOf(ProcessHandle.current().pid()), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    private int readPortFromTempFile() {
        try {
            Path cfgPortFile = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve(PORT_FILE_NAME);
            if (Files.exists(cfgPortFile)) {
                int p = Integer.parseInt(Files.readString(cfgPortFile).trim());
                if (p > 0) {
                    return p;
                }
            }
        } catch (Exception ignored) {}
        Path portFile = this.getTempDir().resolve(PORT_FILE_NAME);
        try {
            if (Files.exists(portFile, new LinkOption[0])) {
                return Integer.parseInt(Files.readString(portFile).trim());
            }
        }
        catch (Exception ignored) {}
        return -1;
    }

    private int waitForPortFile(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            int p = this.readPortFromTempFile();
            if (p > 0) {
                return p;
            }
            try {
                Thread.sleep(200L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
            if (this.helperProcess == null || this.helperProcess.isAlive()) continue;
            return -1;
        }
        return -1;
    }

    private void extractHelperIfNeeded(Path targetPath) throws IOException {
        this.extractResourceIfNeeded("/helper/YandexMusicHelper.exe", targetPath);
    }

    private void extractResourceIfNeeded(String resourcePath, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent(), new FileAttribute[0]);
        try (InputStream in = this.getClass().getResourceAsStream(resourcePath);){
            if (in == null) {
                if (!Files.exists(targetPath, new LinkOption[0])) {
                    throw new IOException("Helper resource not found: " + resourcePath);
                }
                return;
            }
            try {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException copyEx) {
                if (!Files.exists(targetPath, new LinkOption[0])) {
                    throw copyEx;
                }
            }
        }
    }

    private Path getTempDir() {
        String temp = System.getenv("TEMP");
        if (temp == null) {
            temp = System.getProperty("java.io.tmpdir");
        }
        return Path.of(temp, new String[0]);
    }
}
