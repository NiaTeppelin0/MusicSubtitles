package com.example.yandexmusichud.network;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PulseSyncClient {
    public static final PulseSyncClient INSTANCE = new PulseSyncClient();
    private volatile boolean available = false;
    private volatile long lastProbeTime = 0L;
    private volatile String lastCoverHash = null;
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
            URL url = URI.create("http://127.0.0.1:" + port + "/get_track").toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(200);
            conn.setReadTimeout(200);
            conn.setUseCaches(false);
            long tStart = System.currentTimeMillis();
            int code = conn.getResponseCode();
            long tEnd = System.currentTimeMillis();
            long receivedTime = (tStart + tEnd) / 2L;
            if (code == 200) {
                String body;
                try (InputStream is = conn.getInputStream()) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                conn.disconnect();
                if (body.isBlank()) {
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
                    if (coverManager != null && !coverHash.equals(this.lastCoverHash)) {
                        this.lastCoverHash = coverHash;
                        this.fetchCoverAsync(fullCoverUrl, coverManager);
                    }
                }
                String playUrl = !realId.isEmpty() ? "https://music.yandex.ru/track/" + realId : "https://music.yandex.ru";
                this.available = true;
                return new TrackState(true, title, artist, album, status, posMs, durMs, coverHash, receivedTime, "yandex", "\u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430", playUrl, 1.0f);
            }
            conn.disconnect();
            this.available = false;
        } catch (Exception e) {
            this.available = false;
        }
        return null;
    }

    private void fetchCoverAsync(String urlStr, CoverTextureManager coverManager) {
        CompletableFuture.runAsync(() -> {
            try {
                URL u = URI.create(urlStr).toURL();
                HttpURLConnection c = (HttpURLConnection)u.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(2500);
                c.setReadTimeout(2500);
                if (c.getResponseCode() == 200) {
                    byte[] bytes;
                    try (InputStream is = c.getInputStream()) {
                        bytes = is.readAllBytes();
                    }
                    if (bytes.length > 0) {
                        String b64 = Base64.getEncoder().encodeToString(bytes);
                        coverManager.updateCover(b64);
                    }
                }
                c.disconnect();
            } catch (Exception ignored) {}
        });
    }

    private String getString(JsonObject obj, String key, String def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : def;
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
            if (trackObj.has("durationMs")) {
                long d = trackObj.get("durationMs").getAsLong();
                if (d > 0L) {
                    return d;
                }
            }
            if (trackObj.has("duration")) {
                double d = trackObj.get("duration").getAsDouble();
                if (d > 0.0) {
                    return d < 10000.0 ? (long)(d * 1000.0) : (long)d;
                }
            }
        }
        if (json.has("progress") && json.get("progress").isJsonObject()) {
            JsonObject prog = json.getAsJsonObject("progress");
            if (prog.has("duration")) {
                double d = prog.get("duration").getAsDouble();
                if (d > 0.0) {
                    return d < 10000.0 ? (long)(d * 1000.0) : (long)d;
                }
            }
        }
        if (json.has("durationMs")) {
            long d = json.get("durationMs").getAsLong();
            if (d > 0L) {
                return d;
            }
        }
        if (json.has("duration")) {
            double d = json.get("duration").getAsDouble();
            if (d > 0.0) {
                return d < 10000.0 ? (long)(d * 1000.0) : (long)d;
            }
        }
        return 0L;
    }

    private long parsePositionMs(JsonObject json, JsonObject trackObj, long durMs) {
        double pos = -1.0;
        if (json.has("progress")) {
            JsonElement p = json.get("progress");
            if (p.isJsonObject()) {
                JsonObject po = p.getAsJsonObject();
                if (po.has("position")) {
                    pos = po.get("position").getAsDouble();
                } else if (po.has("current")) {
                    pos = po.get("current").getAsDouble();
                } else if (po.has("pos")) {
                    pos = po.get("pos").getAsDouble();
                }
            } else if (p.isJsonPrimitive()) {
                pos = p.getAsDouble();
            }
        }
        if (pos < 0.0) {
            if (json.has("positionMs")) {
                return json.get("positionMs").getAsLong();
            }
            if (json.has("position")) {
                pos = json.get("position").getAsDouble();
            } else if (json.has("pos")) {
                pos = json.get("pos").getAsDouble();
            } else if (json.has("playbackPosition")) {
                pos = json.get("playbackPosition").getAsDouble();
            } else if (trackObj != null) {
                if (trackObj.has("positionMs")) {
                    return trackObj.get("positionMs").getAsLong();
                }
                if (trackObj.has("position")) {
                    pos = trackObj.get("position").getAsDouble();
                }
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
