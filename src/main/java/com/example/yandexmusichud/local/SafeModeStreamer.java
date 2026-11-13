package com.example.yandexmusichud.local;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.network.TrackState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import net.minecraft.class_310;

public class SafeModeStreamer {
    public static final SafeModeStreamer INSTANCE = new SafeModeStreamer();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    private static final Pattern ID_JSON_PATTERN = Pattern.compile("\"id\"\\s*:\\s*([0-9]+)");
    private static final Pattern COVER_URI_PATTERN = Pattern.compile("\"coverUri\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern YANDEX_TOKEN_PATTERN = Pattern.compile("y0_[a-zA-Z0-9_-]{20,}");
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    private volatile String currentQuery = "";
    private volatile String inFlightQuery = "";
    private final AtomicInteger fetchGeneration = new AtomicInteger(0);

    private SafeModeStreamer() {}

    public void handleMissingSyncedTrack(TrackState state) {
        if (state == null || !state.isActive()) {
            return;
        }
        if (!HudConfig.INSTANCE.safeMode) {
            this.sendChatMessage("§b♫ [Music Sync] §eТрек §f\"" + state.artist() + " - " + state.title() + "\" §eне найден в локальных файлах.");
            return;
        }

        String artist = state.artist().trim();
        String title = state.title().trim();
        String queryKey = (artist + " - " + title).toLowerCase(Locale.ROOT);
        if (queryKey.equals(this.currentQuery) && queryKey.equals(this.inFlightQuery)) {
            return;
        }
        this.currentQuery = queryKey;
        this.inFlightQuery = queryKey;
        int gen = this.fetchGeneration.incrementAndGet();

        Path cacheDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("cache");
        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
        } catch (Throwable ignored) {}

        String safeFileName = this.sanitizeFileName(artist + " - " + title) + ".mp3";
        Path cachedFile = cacheDir.resolve(safeFileName);
        Path coverFile = cacheDir.resolve(this.sanitizeFileName(artist + " - " + title) + ".cover.jpg");

        if (Files.exists(cachedFile)) {
            try {
                long sz = Files.size(cachedFile);
                if (sz <= 16384L) {
                    Files.deleteIfExists(cachedFile);
                    Files.deleteIfExists(coverFile);
                } else {
                    byte[] coverBytes = null;
                    if (Files.exists(coverFile)) {
                        coverBytes = Files.readAllBytes(coverFile);
                    }
                    this.inFlightQuery = "";
                    LocalMusicTrack track = new LocalMusicTrack(cachedFile, cachedFile.getFileName().toString(), title, artist, "Сеть", state.durationMs(), "MP3", null, coverBytes);
                    LocalMusicPlayer.getInstance().playTrack(track, state.getInterpolatedPositionMs());
                    if (!state.isPlaying()) {
                        LocalMusicPlayer.getInstance().pause();
                    }
                    if (coverBytes != null && coverBytes.length > 0) {
                        this.applyCover(coverBytes);
                    }
                    return;
                }
            } catch (Throwable ignored) {}
        }

        this.sendChatMessage("§b♫ [Безопасный режим] §7Поиск и авто-стриминг трека: §f\"" + artist + " - " + title + "\"§7...");

        String explicitDirectUrl = state.directUrl();
        String explicitCoverUrl = state.coverUrl();
        if ((explicitDirectUrl == null || explicitDirectUrl.isBlank()) && explicitCoverUrl != null && explicitCoverUrl.contains("|")) {
            String[] parts = explicitCoverUrl.split("\\|", 2);
            explicitCoverUrl = parts[0];
            explicitDirectUrl = parts[1];
        }

        final String finalDirectUrl = explicitDirectUrl;
        final String finalCoverUrl = explicitCoverUrl;

        CompletableFuture.runAsync(() -> {
            try {
                boolean success = false;
                if (finalDirectUrl != null && !finalDirectUrl.isBlank()) {
                    if (finalCoverUrl != null && !finalCoverUrl.isBlank()) {
                        this.downloadCoverDirect(finalCoverUrl, coverFile);
                    }
                    success = this.downloadDirectStream(finalDirectUrl, cachedFile, queryKey);
                }
                if (!success) {
                    success = this.searchAndDownload(artist, title, cachedFile, coverFile, queryKey);
                }
                if (success && gen == this.fetchGeneration.get() && queryKey.equals(this.currentQuery)) {
                    this.sendChatMessage("§b♫ [Безопасный режим] §aТрек получен! Воспроизведение синхронизировано.");
                    byte[] coverBytes = null;
                    if (Files.exists(coverFile)) {
                        try {
                            coverBytes = Files.readAllBytes(coverFile);
                        } catch (Throwable ignored) {}
                    }
                    TrackState effectiveState = state;
                    TrackState latestSynced = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getSyncedTrack();
                    if (latestSynced != null && latestSynced.isActive()) {
                        String latestKey = (latestSynced.artist().trim() + " - " + latestSynced.title().trim()).toLowerCase(Locale.ROOT);
                        if (queryKey.equals(latestKey)) {
                            effectiveState = latestSynced;
                        }
                    }
                    long startPosMs = effectiveState.getInterpolatedPositionMs();
                    LocalMusicTrack track = new LocalMusicTrack(cachedFile, cachedFile.getFileName().toString(), title, artist, "Сеть", effectiveState.durationMs(), "MP3", null, coverBytes);
                    LocalMusicPlayer.getInstance().playTrack(track, startPosMs);
                    if (!effectiveState.isPlaying()) {
                        LocalMusicPlayer.getInstance().pause();
                    }
                    if (coverBytes != null && coverBytes.length > 0) {
                        this.applyCover(coverBytes);
                    }
                } else if (!success && gen == this.fetchGeneration.get() && queryKey.equals(this.currentQuery)) {
                    this.sendChatMessage("§b♫ [Безопасный режим] §cНе удалось найти трек в сети. Добавьте его в папку музыки вручную.");
                }
            } finally {
                if (gen == this.fetchGeneration.get()) {
                    this.inFlightQuery = "";
                }
            }
        });
    }

    private void downloadCoverDirect(String coverUrl, Path coverFile) {
        try {
            HttpRequest reqCover = HttpRequest.newBuilder().uri(URI.create(coverUrl)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<byte[]> respCover = HTTP_CLIENT.send(reqCover, HttpResponse.BodyHandlers.ofByteArray());
            if (respCover.statusCode() == 200 && respCover.body() != null && respCover.body().length > 0) {
                Files.write(coverFile, respCover.body());
                this.applyCover(respCover.body());
            }
        } catch (Throwable ignored) {}
    }

    public String[] resolveDirectMp3Url(String artist, String title) {
        try {
            String token = this.resolveYandexToken();
            if (token == null || token.isBlank()) {
                return null;
            }
            String query = (artist.isEmpty() ? title : (artist + " " + title)).trim();
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://api.music.yandex.net/search?text=" + encodedQuery + "&type=track&page=0";

            HttpRequest reqSearch = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("User-Agent", "Yandex-Music-API")
                .header("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                .header("Authorization", "OAuth " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> respSearch = HTTP_CLIENT.send(reqSearch, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respSearch.statusCode() != 200 || respSearch.body() == null || respSearch.body().isBlank()) {
                return null;
            }

            String searchBody = respSearch.body();
            Matcher mId = ID_JSON_PATTERN.matcher(searchBody);
            if (!mId.find()) {
                return null;
            }
            String trackId = mId.group(1);

            String coverUrl = null;
            Matcher mCover = COVER_URI_PATTERN.matcher(searchBody);
            if (mCover.find()) {
                coverUrl = "https://" + mCover.group(1).replace("%%", "200x200");
            }

            String dlInfoUrl = "https://api.music.yandex.net/tracks/" + trackId + "/download-info";
            HttpRequest reqDl = HttpRequest.newBuilder()
                .uri(URI.create(dlInfoUrl))
                .header("User-Agent", "Yandex-Music-API")
                .header("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                .header("Authorization", "OAuth " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> respDl = HTTP_CLIENT.send(reqDl, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respDl.statusCode() != 200 || respDl.body() == null) {
                return null;
            }

            String xmlUrl = null;
            try {
                com.google.gson.JsonElement rootEl = JsonParser.parseString(respDl.body());
                if (rootEl != null && rootEl.isJsonObject()) {
                    JsonObject json = rootEl.getAsJsonObject();
                    if (json.has("result") && json.get("result").isJsonArray()) {
                        JsonArray results = json.getAsJsonArray("result");
                        for (int i = 0; i < results.size(); i++) {
                            com.google.gson.JsonElement itemEl = results.get(i);
                            if (itemEl == null || !itemEl.isJsonObject()) continue;
                            JsonObject item = itemEl.getAsJsonObject();
                            boolean isPreview = item.has("preview") && item.get("preview").isJsonPrimitive() && item.get("preview").getAsBoolean();
                            if (!isPreview && item.has("downloadInfoUrl") && item.get("downloadInfoUrl").isJsonPrimitive()) {
                                xmlUrl = item.get("downloadInfoUrl").getAsString();
                                break;
                            }
                        }
                        if (xmlUrl == null && !results.isEmpty() && results.get(0).isJsonObject()) {
                            JsonObject first = results.get(0).getAsJsonObject();
                            if (first.has("downloadInfoUrl") && first.get("downloadInfoUrl").isJsonPrimitive()) {
                                xmlUrl = first.get("downloadInfoUrl").getAsString();
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            if (xmlUrl == null) {
                return null;
            }

            HttpRequest reqXml = HttpRequest.newBuilder()
                .uri(URI.create(xmlUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> respXml = HTTP_CLIENT.send(reqXml, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respXml.statusCode() != 200 || respXml.body() == null) {
                return null;
            }

            String xml = respXml.body();
            String host = this.extractXmlTag(xml, "host");
            String path = this.extractXmlTag(xml, "path");
            String ts = this.extractXmlTag(xml, "ts");
            String s = this.extractXmlTag(xml, "s");

            if (host.isEmpty() || path.isEmpty() || ts.isEmpty() || s.isEmpty()) {
                return null;
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(("XGRlBW9FXlekgb0rphSQMv" + path.substring(1) + s).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String directMp3Url = "https://" + host + "/get-mp3/" + sb.toString() + "/" + ts + path;
            return new String[] { directMp3Url, coverUrl != null ? coverUrl : "" };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean searchAndDownload(String artist, String title, Path targetFile, Path coverFile, String expectedQuery) {
        if (this.downloadFromYandexMusic(artist, title, targetFile, coverFile, expectedQuery)) {
            return true;
        }
        return this.downloadFromDeezer(artist, title, targetFile, coverFile, expectedQuery);
    }

    private String resolveYandexToken() {
        if (HudConfig.INSTANCE != null && HudConfig.INSTANCE.yandexMusicToken != null && !HudConfig.INSTANCE.yandexMusicToken.isBlank()) {
            return HudConfig.INSTANCE.yandexMusicToken.trim();
        }
        try {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                Path leveldbDir = Path.of(appData, "YandexMusic", "Local Storage", "leveldb");
                if (Files.exists(leveldbDir)) {
                    try (var stream = Files.list(leveldbDir)) {
                        for (Path f : stream.toList()) {
                            String fn = f.getFileName().toString();
                            if (fn.endsWith(".ldb") || fn.endsWith(".log")) {
                                try {
                                    byte[] bytes = Files.readAllBytes(f);
                                    String s = new String(bytes, StandardCharsets.ISO_8859_1);
                                    Matcher m = YANDEX_TOKEN_PATTERN.matcher(s);
                                    if (m.find()) {
                                        String found = m.group();
                                        if (HudConfig.INSTANCE != null) {
                                            HudConfig.INSTANCE.yandexMusicToken = found;
                                            HudConfig.save();
                                        }
                                        return found;
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private boolean downloadFromYandexMusic(String artist, String title, Path targetFile, Path coverFile, String expectedQuery) {
        try {
            if (!expectedQuery.equals(this.currentQuery)) {
                return false;
            }
            String[] res = this.resolveDirectMp3Url(artist, title);
            if (res == null || res[0] == null || res[0].isBlank()) {
                return false;
            }
            String directMp3Url = res[0];
            String coverUrl = res.length > 1 ? res[1] : null;
            if (coverUrl != null && !coverUrl.isBlank()) {
                this.downloadCoverDirect(coverUrl, coverFile);
            }
            return this.downloadDirectStream(directMp3Url, targetFile, expectedQuery);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean downloadFromDeezer(String artist, String title, Path targetFile, Path coverFile, String expectedQuery) {
        try {
            if (!expectedQuery.equals(this.currentQuery)) {
                return false;
            }
            String query = (artist.isEmpty() ? title : (artist + " " + title)).trim();
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://api.deezer.com/search?q=" + encoded;

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200 || resp.body() == null) {
                return false;
            }

            com.google.gson.JsonElement rootEl = JsonParser.parseString(resp.body());
            if (rootEl == null || !rootEl.isJsonObject()) {
                return false;
            }
            JsonObject obj = rootEl.getAsJsonObject();
            if (!obj.has("data") || !obj.get("data").isJsonArray()) {
                return false;
            }
            JsonArray arr = obj.getAsJsonArray("data");
            if (arr.isEmpty() || !arr.get(0).isJsonObject()) {
                return false;
            }

            JsonObject first = arr.get(0).getAsJsonObject();
            if (first.has("album") && first.get("album").isJsonObject()) {
                JsonObject alb = first.getAsJsonObject("album");
                if (alb.has("cover_medium") && alb.get("cover_medium").isJsonPrimitive()) {
                    try {
                        String cUrl = alb.get("cover_medium").getAsString();
                        if (cUrl != null && !cUrl.isEmpty()) {
                            HttpRequest cReq = HttpRequest.newBuilder().uri(URI.create(cUrl)).timeout(Duration.ofSeconds(5)).GET().build();
                            HttpResponse<byte[]> cResp = HTTP_CLIENT.send(cReq, HttpResponse.BodyHandlers.ofByteArray());
                            if (cResp.statusCode() == 200 && cResp.body() != null && cResp.body().length > 0) {
                                Files.write(coverFile, cResp.body());
                                this.applyCover(cResp.body());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }

            if (!first.has("preview") || !first.get("preview").isJsonPrimitive()) {
                return false;
            }
            String previewUrl = first.get("preview").getAsString();
            if (previewUrl == null || previewUrl.isEmpty()) {
                return false;
            }

            return this.downloadDirectStream(previewUrl, targetFile, expectedQuery);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean downloadDirectStream(String directUrl, Path targetFile, String expectedQuery) {
        Path partFile = targetFile.resolveSibling(targetFile.getFileName().toString() + ".part");
        boolean moved = false;
        try {
            if (!expectedQuery.equals(this.currentQuery)) {
                return false;
            }
            HttpRequest audioReq = HttpRequest.newBuilder()
                .uri(URI.create(directUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

            HttpResponse<InputStream> audioResp = HTTP_CLIENT.send(audioReq, HttpResponse.BodyHandlers.ofInputStream());
            if (audioResp.statusCode() == 200) {
                try (InputStream in = audioResp.body(); OutputStream out = Files.newOutputStream(partFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    long total = 0L;
                    long maxLimit = 40L * 1024L * 1024L;
                    while ((read = in.read(buf)) != -1) {
                        if (!expectedQuery.equals(this.currentQuery)) {
                            return false;
                        }
                        out.write(buf, 0, read);
                        total += read;
                        if (total > maxLimit) {
                            return false;
                        }
                    }
                }
                if (Files.exists(partFile) && Files.size(partFile) > 16384L) {
                    Files.move(partFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    moved = true;
                    return true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(partFile);
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    private void applyCover(byte[] coverBytes) {
        if (coverBytes == null || coverBytes.length == 0) {
            return;
        }
        try {
            if (com.example.yandexmusichud.YandexMusicHudMod.INSTANCE != null && com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getCoverManager() != null) {
                com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getCoverManager().updateCover(Base64.getEncoder().encodeToString(coverBytes));
            }
        } catch (Throwable ignored) {}
    }

    private String extractXmlTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open);
        if (start < 0) {
            return "";
        }
        start += open.length();
        int end = xml.indexOf(close, start);
        return end > start ? xml.substring(start, end) : "";
    }

    private String sanitizeFileName(String name) {
        if (name == null) {
            return "track";
        }
        return INVALID_FILENAME_PATTERN.matcher(name).replaceAll("_").trim();
    }

    private void sendChatMessage(String message) {
        class_310 mc = class_310.method_1551();
        if (mc != null) {
            mc.execute(() -> {
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353(class_2561.method_43470(message), false);
                }
            });
        }
    }
}
