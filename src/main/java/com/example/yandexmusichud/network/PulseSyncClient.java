package com.example.yandexmusichud.network;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PulseSyncClient {
    public static final PulseSyncClient INSTANCE = new PulseSyncClient();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(2L))
        .build();
    private volatile boolean available = false;
    private volatile long lastProbeTime = 0L;
    private volatile String lastCoverHash = null;
    private volatile String pendingCoverHash = null;
    private String lastTrackKey = "";
    private long lastRawReportedPosMs = -1L;
    private boolean lastReportedPlaying = false;
    private long interpolatedBasePosMs = 0L;
    private long interpolatedBaseTimeMs = 0L;

    public boolean isAvailable() {
        return this.available;
    }

    public TrackState poll(CoverTextureManager coverManager) {
        if (!HudConfig.INSTANCE.pulseSyncIntegration) {
            this.available = false;
            return null;
        }
        long now = System.currentTimeMillis();
        if (!this.available && (now - this.lastProbeTime < 2000L)) {
            return null;
        }
        this.lastProbeTime = now;
        try {
            int port = HudConfig.INSTANCE.pulseSyncPort > 0 ? HudConfig.INSTANCE.pulseSyncPort : 2007;
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/get_track"))
                .header("Accept", "*/*")
                .timeout(Duration.ofMillis(300L))
                .GET()
                .build();
            long tStart = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            long tEnd = System.currentTimeMillis();
            int code = response.statusCode();
            long receivedTime = (tStart + tEnd) / 2L;
            if (code == 200) {
                String body = response.body();
                if (body == null || body.isBlank()) {
                    this.available = true;
                    return null;
                }
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                JsonObject trackObj = json.has("track") && json.get("track").isJsonObject()
                        ? json.getAsJsonObject("track")
                        : json;
                String title = this.getString(trackObj, "title", "").trim();
                String realId = this.getString(trackObj, "realId", this.getString(trackObj, "id", "")).trim();
                if (title.isEmpty() || "-".equals(title)) {
                    this.available = true;
                    return null;
                }
                StringBuilder artistBuilder = new StringBuilder();
                if (trackObj.has("artists") && trackObj.get("artists").isJsonArray()) {
                    JsonArray arr = trackObj.getAsJsonArray("artists");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonElement el = arr.get(i);
                        if (el.isJsonObject()) {
                            JsonObject a = el.getAsJsonObject();
                            if (a.has("name") && !a.get("name").isJsonNull()) {
                                if (artistBuilder.length() > 0) {
                                    artistBuilder.append(", ");
                                }
                                artistBuilder.append(a.get("name").getAsString());
                            }
                        }
                    }
                }
                String artist = artistBuilder.toString().trim();
                if (artist.isEmpty()) {
                    artist = this.getString(trackObj, "artist", "").trim();
                }
                String album = "";
                if (trackObj.has("albums") && trackObj.get("albums").isJsonArray()) {
                    JsonArray arr = trackObj.getAsJsonArray("albums");
                    if (!arr.isEmpty() && arr.get(0).isJsonObject()) {
                        album = this.getString(arr.get(0).getAsJsonObject(), "title", "").trim();
                    }
                }
                if (album.isEmpty()) {
                    album = this.getString(trackObj, "album", "").trim();
                }
                boolean isPlaying = this.parseIsPlaying(json, trackObj);
                String status = isPlaying ? "playing" : "paused";
                long durMs = this.parseDurationMs(json, trackObj);
                long reportedPosMs = this.parsePositionMs(json, trackObj, durMs);
                long latencyMs = Math.max(0L, Math.min(150L, (tEnd - tStart) / 2L));
                long rawPosMs = isPlaying && reportedPosMs > 0L ? reportedPosMs + latencyMs : reportedPosMs;
                long posMs = rawPosMs;
                long sampleTime = System.currentTimeMillis();
                String trackKey = title + "|" + artist;
                synchronized (this) {
                    if (trackKey.equals(this.lastTrackKey)) {
                        boolean rawChanged = reportedPosMs != this.lastRawReportedPosMs;
                        long prevPos = this.lastReportedPlaying && this.interpolatedBaseTimeMs > 0L
                                ? this.interpolatedBasePosMs + Math.max(0L, sampleTime - this.interpolatedBaseTimeMs)
                                : this.interpolatedBasePosMs;
                        if (durMs > 0L) {
                            prevPos = Math.min(durMs, prevPos);
                        }
                        if (isPlaying && this.lastReportedPlaying) {
                            if (!rawChanged) {
                                posMs = prevPos;
                            } else {
                                this.lastRawReportedPosMs = reportedPosMs;
                                long diff = rawPosMs - prevPos;
                                if (Math.abs(diff) < 1150L) {
                                    posMs = prevPos + (long)((double)diff * 0.35);
                                } else {
                                    posMs = rawPosMs;
                                }
                            }
                        } else {
                            this.lastRawReportedPosMs = reportedPosMs;
                            posMs = reportedPosMs;
                        }
                    } else {
                        this.lastTrackKey = trackKey;
                        this.lastRawReportedPosMs = reportedPosMs;
                    }
                    if (durMs > 0L) {
                        posMs = Math.max(0L, Math.min(durMs, posMs));
                    } else {
                        posMs = Math.max(0L, posMs);
                    }
                    this.interpolatedBasePosMs = posMs;
                    this.interpolatedBaseTimeMs = sampleTime;
                    this.lastReportedPlaying = isPlaying;
                    receivedTime = sampleTime;
                }
                String coverUri = this.getString(trackObj, "coverUri", this.getString(trackObj, "ogImage", ""));
                String coverHash = null;
                if (!coverUri.isEmpty()) {
                    String cleanUri = coverUri.startsWith("http") ? coverUri : "https://" + coverUri;
                    String fullCoverUrl = cleanUri.replace("%%", "400x400");
                    coverHash = "ps_" + (!realId.isEmpty() ? realId : String.valueOf(fullCoverUrl.hashCode()));
                    if (coverManager != null && !coverHash.equals(this.lastCoverHash) && !coverHash.equals(this.pendingCoverHash)) {
                        this.fetchCoverAsync(fullCoverUrl, coverHash, coverManager);
                    }
                }
                String playUrl = !realId.isEmpty() ? "https://music.yandex.ru/track/" + realId : "https://music.yandex.ru";
                this.available = true;
                return new TrackState(true, title, artist, album, status, posMs, durMs, coverHash, receivedTime, "yandex", "\u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430", playUrl, 1.0f);
            }
            this.available = false;
        } catch (Exception e) {
            this.available = false;
        }
        return null;
    }

    private void fetchCoverAsync(String urlStr, String coverHash, CoverTextureManager coverManager) {
        this.pendingCoverHash = coverHash;
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(Duration.ofSeconds(3L))
                    .GET()
                    .build();
                HttpResponse<byte[]> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200) {
                    byte[] bytes = res.body();
                    if (bytes != null && bytes.length > 0) {
                        String b64 = Base64.getEncoder().encodeToString(bytes);
                        this.lastCoverHash = coverHash;
                        coverManager.updateCover(b64);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (coverHash.equals(this.pendingCoverHash)) {
                    this.pendingCoverHash = null;
                }
            }
        });
    }

    private String getString(JsonObject obj, String key, String def) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : def;
    }

    private long getLong(JsonObject obj, String key, long def) {
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsLong();
            } catch (Exception ignored) {}
        }
        return def;
    }

    private double getDouble(JsonObject obj, String key, double def) {
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsDouble();
            } catch (Exception ignored) {}
        }
        return def;
    }

    private boolean parseIsPlaying(JsonObject json, JsonObject trackObj) {
        if (json.has("isPlaying") && json.get("isPlaying").isJsonPrimitive()) {
            try {
                if (json.get("isPlaying").getAsBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        if (json.has("playing") && json.get("playing").isJsonPrimitive()) {
            try {
                if (json.get("playing").getAsBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        if (json.has("paused") && json.get("paused").isJsonPrimitive()) {
            try {
                if (!json.get("paused").getAsBoolean()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        if (trackObj != null) {
            if (trackObj.has("isPlaying") && trackObj.get("isPlaying").isJsonPrimitive()) {
                try {
                    if (trackObj.get("isPlaying").getAsBoolean()) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
            if (trackObj.has("playing") && trackObj.get("playing").isJsonPrimitive()) {
                try {
                    if (trackObj.get("playing").getAsBoolean()) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        String s = this.getString(json, "status", this.getString(json, "state", this.getString(json, "player_state", "")));
        if (s.isEmpty() && trackObj != null) {
            s = this.getString(trackObj, "status", this.getString(trackObj, "state", ""));
        }
        String lower = s.toLowerCase(Locale.ROOT).trim();
        return lower.contains("play") || lower.contains("run") || "active".equals(lower);
    }

    private long parseDurationMs(JsonObject json, JsonObject trackObj) {
        if (trackObj != null) {
            long dMs = this.getLong(trackObj, "durationMs", 0L);
            if (dMs > 0L) {
                return dMs;
            }
            double d = this.getDouble(trackObj, "duration", 0.0);
            if (d > 0.0) {
                return d < 10000.0 ? (long)(d * 1000.0) : (long)d;
            }
        }
        if (json.has("progress") && json.get("progress").isJsonObject()) {
            JsonObject prog = json.getAsJsonObject("progress");
            double d = this.getDouble(prog, "duration", 0.0);
            if (d > 0.0) {
                return d < 10000.0 ? (long)(d * 1000.0) : (long)d;
            }
        }
        long dMs = this.getLong(json, "durationMs", 0L);
        if (dMs > 0L) {
            return dMs;
        }
        double d = this.getDouble(json, "duration", 0.0);
        if (d > 0.0) {
            return d < 10000.0 ? (long)(d * 1000.0) : (long)d;
        }
        return 0L;
    }

    private long parsePositionMs(JsonObject json, JsonObject trackObj, long durMs) {
        double pos = -1.0;
        if (json.has("progress")) {
            JsonElement p = json.get("progress");
            if (p.isJsonObject()) {
                JsonObject po = p.getAsJsonObject();
                pos = this.getDouble(po, "position", -1.0);
                if (pos < 0.0) {
                    pos = this.getDouble(po, "current", -1.0);
                }
                if (pos < 0.0) {
                    pos = this.getDouble(po, "pos", -1.0);
                }
            } else if (p.isJsonPrimitive()) {
                try {
                    pos = p.getAsDouble();
                } catch (Exception ignored) {}
            }
        }
        if (pos < 0.0) {
            long pMs = this.getLong(json, "positionMs", -1L);
            if (pMs >= 0L) {
                return pMs;
            }
            pos = this.getDouble(json, "position", -1.0);
            if (pos < 0.0) {
                pos = this.getDouble(json, "pos", -1.0);
            }
            if (pos < 0.0) {
                pos = this.getDouble(json, "playbackPosition", -1.0);
            }
            if (pos < 0.0 && trackObj != null) {
                long tpMs = this.getLong(trackObj, "positionMs", -1L);
                if (tpMs >= 0L) {
                    return tpMs;
                }
                pos = this.getDouble(trackObj, "position", -1.0);
            }
        }
        if (pos <= 0.0) {
            return 0L;
        }
        if (pos < 10000.0 && durMs > 10000L) {
            return (long)(pos * 1000.0);
        }
        if (pos > 0.0 && pos < 7200.0) {
            return (long)(pos * 1000.0);
        }
        return (long)pos;
    }
}
