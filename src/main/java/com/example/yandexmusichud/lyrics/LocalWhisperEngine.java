package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.config.HudConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.loader.api.FabricLoader;

public class LocalWhisperEngine {
    private static final String WHISPER_ZIP_URL = "https://github.com/ggml-org/whisper.cpp/releases/download/b5454/whisper-blas-bin-x64.zip";
    private static final String WHISPER_MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin";
    private static final String MODEL_FILE_NAME = "ggml-base-q5_1.bin";
    private static final String BIN_MARKER_NAME = ".bin_ready_b5454_blas";
    private static final long MIN_MODEL_BYTES = 50000000L;
    private static final Set<String> REQUIRED_EXES = Set.of(
        "whisper-server.exe",
        "whisper.dll",
        "ggml.dll",
        "ggml-base.dll",
        "ggml-blas.dll",
        "libopenblas.dll"
    );
    private static final Map<String, String> LANGUAGE_NAME_TO_BCP47 = buildLanguageNameMap();
    private static final Map<String, String> BCP47_TO_WHISPER_CODE = buildWhisperCodeMap();

    public record WordSpan(String word, long startMs, long endMs, double probability) {
    }

    public record WhisperOutput(
        String text,
        String bcp47Lang,
        double confidence,
        double detectedLangProb,
        double noSpeechProb,
        double avgLogprob,
        List<WordSpan> words
    ) {
    }

    private final HttpClient httpClient;
    private final ExecutorService lifecycleExecutor;
    private volatile Process serverProcess;
    private volatile int serverPort = -1;
    private volatile boolean setupInProgress = false;
    private volatile boolean stopRequested = false;
    private volatile long nextRetryAtMs = 0L;
    private volatile int failureStreak = 0;
    private volatile String statusText = "";
    private final Object lifecycleLock = new Object();

    public LocalWhisperEngine() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.lifecycleExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MusicSubtitles-WhisperEngine");
            t.setDaemon(true);
            return t;
        });
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopServerProcessOnly, "MusicSubtitles-WhisperShutdown"));
        } catch (Throwable ignored) {
        }
    }

    public static boolean isSupportedPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return os.contains("win") && (arch.contains("64") || arch.contains("amd64") || arch.contains("x86_64"));
    }

    public static Path getDefaultWhisperDir() {
        try {
            return FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("whisper");
        } catch (Throwable ignored) {
            return Path.of("config", "yandexmusichud", "whisper");
        }
    }

    public boolean isReady() {
        Process proc = this.serverProcess;
        return this.serverPort > 0 && proc != null && proc.isAlive();
    }

    public boolean areAssetsInstalled() {
        return areAssetsInstalledAt(getDefaultWhisperDir());
    }

    public static boolean areAssetsInstalledAt(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try {
            Path exe = dir.resolve("whisper-server.exe");
            Path marker = dir.resolve(BIN_MARKER_NAME);
            Path model = dir.resolve(MODEL_FILE_NAME);
            return Files.isRegularFile(exe)
                && Files.isRegularFile(marker)
                && Files.isRegularFile(model)
                && Files.size(model) >= MIN_MODEL_BYTES;
        } catch (Exception ignored) {
            return false;
        }
    }

    public String getStatusText() {
        return this.statusText;
    }

    public void ensureReadyAsync() {
        if (!isSupportedPlatform() || !HudConfig.INSTANCE.liveSubtitlesEnabled) {
            return;
        }
        if ("GOOGLE".equalsIgnoreCase(HudConfig.INSTANCE.liveSttEngine)) {
            return;
        }
        if (this.isReady() || this.setupInProgress) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextRetryAtMs) {
            return;
        }
        this.stopRequested = false;
        this.setupInProgress = true;
        this.lifecycleExecutor.execute(() -> {
            try {
                this.ensureReadyBlocking(getDefaultWhisperDir());
            } finally {
                this.setupInProgress = false;
            }
        });
    }

    public boolean ensureReadyBlocking(Path dir) {
        if (!isSupportedPlatform() || dir == null) {
            return false;
        }
        synchronized (this.lifecycleLock) {
            if (this.isReady()) {
                return true;
            }
            if (this.stopRequested) {
                return false;
            }
            try {
                Files.createDirectories(dir);
                if (!this.ensureBinaries(dir)) {
                    this.recordSetupFailure();
                    return false;
                }
                if (this.stopRequested) {
                    return false;
                }
                if (!this.ensureModel(dir)) {
                    this.recordSetupFailure();
                    return false;
                }
                if (this.stopRequested) {
                    return false;
                }
                if (!this.startServer(dir)) {
                    this.recordSetupFailure();
                    return false;
                }
                this.failureStreak = 0;
                this.nextRetryAtMs = 0L;
                this.statusText = "Whisper готов";
                return true;
            } catch (Exception e) {
                this.recordSetupFailure();
                return false;
            }
        }
    }

    private void recordSetupFailure() {
        ++this.failureStreak;
        long delayMs = Math.min(300000L, 25000L * (1L << Math.min(4, this.failureStreak - 1)));
        this.nextRetryAtMs = System.currentTimeMillis() + delayMs;
    }

    private boolean ensureBinaries(Path dir) throws Exception {
        Path exe = dir.resolve("whisper-server.exe");
        Path marker = dir.resolve(BIN_MARKER_NAME);
        if (Files.isRegularFile(exe) && Files.isRegularFile(marker)) {
            return true;
        }
        this.statusText = "Загрузка Whisper движка...";
        Path tmpZip = dir.resolve("whisper-bin.zip.part");
        boolean ok = this.downloadFile(WHISPER_ZIP_URL, tmpZip, "Загрузка Whisper движка");
        if (!ok || this.stopRequested) {
            Files.deleteIfExists(tmpZip);
            return false;
        }
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tmpZip))) {
            ZipEntry entry;
            byte[] buf = new byte[65536];
            while ((entry = zis.getNextEntry()) != null) {
                if (this.stopRequested) {
                    return false;
                }
                if (entry.isDirectory()) {
                    continue;
                }
                String rawName = entry.getName().replace('\\', '/');
                if (rawName.contains("..")) {
                    continue;
                }
                int slash = rawName.lastIndexOf('/');
                String fileName = slash >= 0 ? rawName.substring(slash + 1) : rawName;
                if (!isNeededBinaryFile(fileName)) {
                    continue;
                }
                Path target = dir.resolve(fileName);
                try (OutputStream out = Files.newOutputStream(target)) {
                    int n;
                    while ((n = zis.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(tmpZip);
        }
        if (!Files.isRegularFile(exe)) {
            return false;
        }
        Files.writeString(marker, "b5454-blas", StandardCharsets.UTF_8);
        return true;
    }

    private static boolean isNeededBinaryFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (REQUIRED_EXES.contains(lower)) {
            return true;
        }
        return lower.startsWith("ggml-cpu-") && lower.endsWith(".dll");
    }

    private boolean ensureModel(Path dir) throws Exception {
        Path model = dir.resolve(MODEL_FILE_NAME);
        if (Files.isRegularFile(model) && Files.size(model) >= MIN_MODEL_BYTES) {
            return true;
        }
        this.statusText = "Загрузка модели Whisper (57 МБ)...";
        Path part = dir.resolve(MODEL_FILE_NAME + ".part");
        boolean ok = this.downloadFile(WHISPER_MODEL_URL, part, "Загрузка модели Whisper");
        if (!ok || this.stopRequested || !Files.isRegularFile(part) || Files.size(part) < MIN_MODEL_BYTES) {
            Files.deleteIfExists(part);
            return false;
        }
        Files.move(part, model, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private boolean downloadFile(String url, Path dest, String labelPrefix) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("User-Agent", "MusicSubtitles-WhisperClient/2.8")
                .GET()
                .build();
            HttpResponse<InputStream> resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return false;
            }
            long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
            long downloaded = 0L;
            long lastPct = -1L;
            try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(dest)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (this.stopRequested) {
                        return false;
                    }
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (total > 0L) {
                        long pct = downloaded * 100L / total;
                        if (pct != lastPct && pct % 5L == 0L) {
                            lastPct = pct;
                            this.statusText = labelPrefix + " " + pct + "%";
                        }
                    }
                }
            }
            return Files.isRegularFile(dest) && Files.size(dest) > 0L;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean startServer(Path dir) throws Exception {
        this.stopServerProcessOnly();
        Path exe = dir.resolve("whisper-server.exe");
        if (!Files.isRegularFile(exe) || !Files.isRegularFile(dir.resolve(MODEL_FILE_NAME))) {
            return false;
        }
        int port = findFreePort();
        if (port <= 0) {
            return false;
        }
        int cpus = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(2, Math.min(6, cpus >= 8 ? 4 : Math.max(2, cpus / 2)));
        ProcessBuilder pb = new ProcessBuilder(
            exe.toAbsolutePath().toString(),
            "-m", MODEL_FILE_NAME,
            "--host", "127.0.0.1",
            "--port", String.valueOf(port),
            "-t", String.valueOf(threads),
            "-bo", "1",
            "-nf",
            "-sns",
            "-ac", "384"
        );
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        this.statusText = "Запуск Whisper...";
        Process proc = pb.start();
        this.serverProcess = proc;
        this.drainProcessOutput(proc);

        long deadline = System.currentTimeMillis() + 9000L;
        while (System.currentTimeMillis() < deadline && !this.stopRequested) {
            if (!proc.isAlive()) {
                this.serverProcess = null;
                return false;
            }
            try {
                HttpRequest health = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/"))
                    .timeout(Duration.ofMillis(700))
                    .GET()
                    .build();
                HttpResponse<Void> r = this.httpClient.send(health, HttpResponse.BodyHandlers.discarding());
                if (r.statusCode() == 200) {
                    this.serverPort = port;
                    return true;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(100L);
        }
        this.stopServerProcessOnly();
        return false;
    }

    private void drainProcessOutput(Process proc) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                while (br.readLine() != null) {
                }
            } catch (Exception ignored) {
            }
        }, "MusicSubtitles-WhisperDrain");
        reader.setDaemon(true);
        reader.start();
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public void stopServer() {
        this.stopRequested = true;
        this.stopServerProcessOnly();
    }

    public void shutdown() {
        this.stopServer();
        this.lifecycleExecutor.shutdownNow();
    }

    private void stopServerProcessOnly() {
        this.serverPort = -1;
        Process proc = this.serverProcess;
        this.serverProcess = null;
        if (proc != null) {
            try {
                proc.descendants().forEach(ProcessHandle::destroyForcibly);
            } catch (Throwable ignored) {
            }
            try {
                proc.destroyForcibly();
            } catch (Throwable ignored) {
            }
        }
    }

    public WhisperOutput transcribeWav(byte[] wavBytes, String bcp47LangHint, String promptHint) {
        if (!this.isReady() || wavBytes == null || wavBytes.length <= 44) {
            return null;
        }
        try {
            String boundary = "----MSWhisper" + Long.toHexString(System.nanoTime());
            ByteArrayOutputStream body = new ByteArrayOutputStream(wavBytes.length + 1024);
            writeMultipartFile(body, boundary, "file", "audio.wav", "audio/wav", wavBytes);
            writeMultipartField(body, boundary, "response_format", "verbose_json");
            String whisperLang = toWhisperLangCode(bcp47LangHint);
            writeMultipartField(body, boundary, "language", whisperLang);
            writeMultipartField(body, boundary, "temperature", "0.0");
            if (promptHint != null && !promptHint.isBlank()) {
                writeMultipartField(body, boundary, "prompt", promptHint.trim());
            }
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + this.serverPort + "/inference"))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
            HttpResponse<String> resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
                return null;
            }
            return parseVerboseJson(resp.body(), bcp47LangHint);
        } catch (Exception e) {
            Process proc = this.serverProcess;
            if (proc == null || !proc.isAlive()) {
                this.serverPort = -1;
            }
            return null;
        }
    }

    public static WhisperOutput parseVerboseJson(String json, String requestedLangHint) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String fullText = root.has("text") && root.get("text").isJsonPrimitive()
                ? root.get("text").getAsString().trim()
                : "";
            if (fullText.isEmpty()) {
                return null;
            }
            String detectedRaw = root.has("detected_language") && root.get("detected_language").isJsonPrimitive()
                ? root.get("detected_language").getAsString()
                : (root.has("language") && root.get("language").isJsonPrimitive() ? root.get("language").getAsString() : "");
            double detectedProb = root.has("detected_language_probability") && root.get("detected_language_probability").isJsonPrimitive()
                ? root.get("detected_language_probability").getAsDouble()
                : 0.0;
            String bcp47 = normalizeLanguageName(detectedRaw, requestedLangHint);

            double sumLogprob = 0.0;
            int segCount = 0;
            double maxNoSpeech = 0.0;
            List<WordSpan> mergedWords = new ArrayList<WordSpan>();

            if (root.has("segments") && root.get("segments").isJsonArray()) {
                JsonArray segs = root.getAsJsonArray("segments");
                for (JsonElement segEl : segs) {
                    if (!segEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject seg = segEl.getAsJsonObject();
                    if (seg.has("no_speech_prob") && seg.get("no_speech_prob").isJsonPrimitive()) {
                        maxNoSpeech = Math.max(maxNoSpeech, seg.get("no_speech_prob").getAsDouble());
                    }
                    if (seg.has("avg_logprob") && seg.get("avg_logprob").isJsonPrimitive()) {
                        sumLogprob += seg.get("avg_logprob").getAsDouble();
                        ++segCount;
                    }
                    if (seg.has("words") && seg.get("words").isJsonArray()) {
                        appendMergedSegmentWords(seg.getAsJsonArray("words"), mergedWords);
                    }
                }
            }
            double avgLogprob = segCount > 0 ? sumLogprob / (double)segCount : -0.45;
            if (maxNoSpeech > 0.72 || (maxNoSpeech > 0.58 && avgLogprob < -0.55) || avgLogprob < -0.92) {
                return null;
            }
            int wordCount = mergedWords.isEmpty() ? fullText.split("\\s+").length : mergedWords.size();
            if (detectedProb > 0.0 && detectedProb < 0.42 && wordCount <= 3) {
                return null;
            }
            double confidence = Math.max(0.35, Math.min(0.94, Math.exp(avgLogprob * 0.70)));
            return new WhisperOutput(fullText, bcp47, confidence, detectedProb, maxNoSpeech, avgLogprob, mergedWords);
        } catch (Exception e) {
            return null;
        }
    }

    private static void appendMergedSegmentWords(JsonArray rawWords, List<WordSpan> out) {
        StringBuilder curText = null;
        long curStartMs = 0L;
        long curEndMs = 0L;
        double curMinProb = 1.0;

        for (JsonElement wEl : rawWords) {
            if (!wEl.isJsonObject()) {
                continue;
            }
            JsonObject wObj = wEl.getAsJsonObject();
            String rawTok = wObj.has("word") && wObj.get("word").isJsonPrimitive() ? wObj.get("word").getAsString() : "";
            if (rawTok.isEmpty()) {
                continue;
            }
            long startMs = wObj.has("start") && wObj.get("start").isJsonPrimitive()
                ? Math.max(0L, Math.round(wObj.get("start").getAsDouble() * 1000.0))
                : 0L;
            long endMs = wObj.has("end") && wObj.get("end").isJsonPrimitive()
                ? Math.max(startMs, Math.round(wObj.get("end").getAsDouble() * 1000.0))
                : startMs + 140L;
            double prob = wObj.has("probability") && wObj.get("probability").isJsonPrimitive()
                ? wObj.get("probability").getAsDouble()
                : 0.75;

            boolean startsWithSpace = Character.isWhitespace(rawTok.charAt(0));
            String stripped = rawTok.trim();
            if (stripped.isEmpty()) {
                continue;
            }
            if (isPurePunctuation(stripped)) {
                if (curText != null && endMs > curEndMs) {
                    curEndMs = endMs;
                }
                continue;
            }
            if (curText == null || startsWithSpace) {
                flushWordSpan(curText, curStartMs, curEndMs, curMinProb, out);
                curText = new StringBuilder(stripped);
                curStartMs = startMs;
                curEndMs = endMs;
                curMinProb = prob;
            } else {
                curText.append(stripped);
                curEndMs = Math.max(curEndMs, endMs);
                curMinProb = Math.min(curMinProb, prob);
            }
        }
        flushWordSpan(curText, curStartMs, curEndMs, curMinProb, out);
    }

    private static void flushWordSpan(StringBuilder text, long startMs, long endMs, double prob, List<WordSpan> out) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String clean = trimEdgePunct(text.toString());
        if (!clean.isEmpty() && containsLetterOrDigit(clean)) {
            out.add(new WordSpan(clean, startMs, Math.max(startMs + 80L, endMs), prob));
        }
    }

    private static boolean isPurePunctuation(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (Character.isLetterOrDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsLetterOrDigit(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (Character.isLetterOrDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String trimEdgePunct(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isEdgeTrimChar(s.charAt(start))) {
            ++start;
        }
        while (end > start && isEdgeTrimChar(s.charAt(end - 1))) {
            --end;
        }
        return s.substring(start, end);
    }

    private static boolean isEdgeTrimChar(char c) {
        return Character.isWhitespace(c) || c == ',' || c == '.' || c == ';' || c == ':' || c == '!' || c == '?' || c == '"' || c == '\u2014' || c == '\u2013';
    }

    public static String toWhisperLangCode(String bcp47) {
        if (bcp47 == null || bcp47.isBlank() || "auto".equalsIgnoreCase(bcp47.trim())) {
            return "auto";
        }
        String norm = bcp47.trim().toLowerCase(Locale.ROOT);
        String mapped = BCP47_TO_WHISPER_CODE.get(norm);
        if (mapped != null) {
            return mapped;
        }
        int dash = norm.indexOf('-');
        return dash > 0 ? norm.substring(0, dash) : norm;
    }

    public static String normalizeLanguageName(String raw, String fallbackBcp47) {
        if (raw != null && !raw.isBlank()) {
            String lower = raw.trim().toLowerCase(Locale.ROOT);
            String bcp = LANGUAGE_NAME_TO_BCP47.get(lower);
            if (bcp != null) {
                return bcp;
            }
            if (lower.length() == 2) {
                if ("ru".equals(lower)) {
                    return "ru-RU";
                }
                if ("en".equals(lower)) {
                    return "en-US";
                }
                if ("uk".equals(lower)) {
                    return "uk-UA";
                }
                if ("ja".equals(lower)) {
                    return "ja-JP";
                }
                if ("ko".equals(lower)) {
                    return "ko-KR";
                }
                if ("zh".equals(lower)) {
                    return "zh-CN";
                }
                return lower + "-" + lower.toUpperCase(Locale.ROOT);
            }
        }
        return fallbackBcp47 != null && !fallbackBcp47.isBlank() && !"auto".equalsIgnoreCase(fallbackBcp47) ? fallbackBcp47 : "auto";
    }

    private static Map<String, String> buildLanguageNameMap() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("russian", "ru-RU");
        m.put("english", "en-US");
        m.put("ukrainian", "uk-UA");
        m.put("japanese", "ja-JP");
        m.put("korean", "ko-KR");
        m.put("chinese", "zh-CN");
        m.put("german", "de-DE");
        m.put("french", "fr-FR");
        m.put("spanish", "es-ES");
        m.put("italian", "it-IT");
        m.put("portuguese", "pt-BR");
        m.put("polish", "pl-PL");
        m.put("turkish", "tr-TR");
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, String> buildWhisperCodeMap() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("ru-ru", "ru");
        m.put("en-us", "en");
        m.put("en-gb", "en");
        m.put("uk-ua", "uk");
        m.put("ja-jp", "ja");
        m.put("ko-kr", "ko");
        m.put("zh-cn", "zh");
        m.put("de-de", "de");
        m.put("fr-fr", "fr");
        m.put("es-es", "es");
        return Collections.unmodifiableMap(m);
    }

    private static void writeMultipartField(ByteArrayOutputStream out, String boundary, String name, String value) throws Exception {
        String header = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeMultipartFile(ByteArrayOutputStream out, String boundary, String name, String fileName, String contentType, byte[] data) throws Exception {
        String header = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\nContent-Type: " + contentType + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
