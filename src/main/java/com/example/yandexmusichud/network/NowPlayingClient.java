package com.example.yandexmusichud.network;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.local.LocalMusicPlayer;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NowPlayingClient {
    public static NowPlayingClient INSTANCE;
    private final AtomicReference<TrackState> currentState = new AtomicReference<TrackState>(TrackState.INACTIVE);
    private ScheduledExecutorService scheduler;
    private final CoverTextureManager coverManager;
    private String baseUrl;
    private String lastCoverHash = null;
    private String lastCoverETag = null;
    private int backoffMs = 300;
    private static final int MAX_BACKOFF_MS = 10000;
    private volatile boolean connected = false;
    private long lastRawReportedPosMs = -1L;
    private long lastRawPosChangeTimeMs = 0L;
    private String lastRawTitle = null;
    private String lastRawArtist = null;
    private String lastRawAlbum = null;
    private TrackMetadataSanitizer.SanitizedMetadata cachedSanitizedMeta = null;

    public NowPlayingClient(CoverTextureManager coverManager) {
        this.coverManager = coverManager;
        INSTANCE = this;
    }

    public TrackState getState() {
        return this.currentState.get();
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void start(int port) {
        this.stop();
        this.baseUrl = "http://127.0.0.1:" + port;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MusicSubtitles-Poller");
            t.setDaemon(true);
            return t;
        });
        this.scheduleNextPoll(0);
    }

    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
            this.scheduler = null;
        }
    }

    private void scheduleNextPoll(int delayMs) {
        if (this.scheduler == null || this.scheduler.isShutdown()) {
            return;
        }
        this.scheduler.schedule(this::poll, (long)Math.max(0, delayMs), TimeUnit.MILLISECONDS);
    }

    private void poll() {
        if (com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.hasSyncedTrack()) {
            TrackState synced = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getSyncedTrack();
            if (synced != null && synced.isActive()) {
                this.connected = true;
                this.backoffMs = 500;
                this.currentState.set(synced);
                com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().syncToTrack(synced);
                LocalMusicPlayer.getInstance().pollState(this.coverManager);
                this.scheduleNextPoll(this.backoffMs);
                return;
            }
        }
        TrackState localState = LocalMusicPlayer.getInstance().pollState(this.coverManager);
        if (localState != null && localState.isActive()) {
            this.connected = true;
            this.backoffMs = localState.isPlaying() ? Math.max(150, HudConfig.INSTANCE.pollIntervalMs) : Math.max(500, HudConfig.INSTANCE.pollIntervalMs);
            this.currentState.set(localState);
            this.scheduleNextPoll(this.backoffMs);
            return;
        }
        TrackState pulseState = null;
        if (HudConfig.INSTANCE.pulseSyncIntegration) {
            pulseState = PulseSyncClient.INSTANCE.poll(this.coverManager);
        }
        if (pulseState != null && pulseState.isActive() && pulseState.isPlaying()) {
            this.connected = true;
            this.backoffMs = Math.max(150, HudConfig.INSTANCE.pollIntervalMs);
            this.currentState.set(pulseState);
            this.scheduleNextPoll(this.backoffMs);
            return;
        }
        try {
            URL url = URI.create(this.baseUrl + "/now-playing").toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(300);
            conn.setReadTimeout(300);
            conn.setUseCaches(false);
            long tStart = System.currentTimeMillis();
            int code = conn.getResponseCode();
            long tEnd = System.currentTimeMillis();
            long receivedTime = System.currentTimeMillis();
            if (code == 200) {
                boolean isActive;
                String body;
                this.connected = true;
                this.backoffMs = Math.max(150, HudConfig.INSTANCE.pollIntervalMs);
                try (InputStream is = conn.getInputStream();){
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                JsonObject json = JsonParser.parseString((String)body).getAsJsonObject();
                boolean bl = isActive = json.has("isActive") && json.get("isActive").getAsBoolean();
                if (isActive) {
                    String rawTitle = this.getStringOrDefault(json, "title", "");
                    String rawArtist = this.getStringOrDefault(json, "artist", "");
                    String album = this.getStringOrDefault(json, "album", "").replace('\u2014', '-').replace('\u2013', '-');
                    TrackMetadataSanitizer.SanitizedMetadata meta;
                    if (this.cachedSanitizedMeta != null && rawTitle.equals(this.lastRawTitle) && rawArtist.equals(this.lastRawArtist) && album.equals(this.lastRawAlbum)) {
                        meta = this.cachedSanitizedMeta;
                    } else {
                        meta = TrackMetadataSanitizer.sanitize(rawTitle, rawArtist, album);
                        this.lastRawTitle = rawTitle;
                        this.lastRawArtist = rawArtist;
                        this.lastRawAlbum = album;
                        this.cachedSanitizedMeta = meta;
                    }
                    String title = meta.cleanTitle();
                    String artist = meta.cleanArtist();
                    String status = this.getStringOrDefault(json, "status", "stopped");
                    boolean isPlaying = "playing".equalsIgnoreCase(status);
                    this.backoffMs = isPlaying ? Math.max(150, HudConfig.INSTANCE.pollIntervalMs) : Math.max(500, HudConfig.INSTANCE.pollIntervalMs);
                    long posMs = json.has("positionMs") ? json.get("positionMs").getAsLong() : 0L;
                    long durMs = json.has("durationMs") ? json.get("durationMs").getAsLong() : 0L;
                    String coverHash = json.has("coverHash") && !json.get("coverHash").isJsonNull() ? json.get("coverHash").getAsString() : null;
                    String platform = json.has("platform") && !json.get("platform").isJsonNull() ? json.get("platform").getAsString() : TrackState.detectPlatform((String)title, artist, album);
                    String platformName = json.has("platformName") && !json.get("platformName").isJsonNull() ? json.get("platformName").getAsString() : TrackState.detectPlatformName((String)title, artist, album);
                    String playUrl = json.has("playUrl") && !json.get("playUrl").isJsonNull() ? json.get("playUrl").getAsString() : TrackState.generatePlayUrl((String)title, artist);
                    String detected = TrackState.detectPlatform((String)title, artist, album);
                    if (!"music".equals(detected) && !"yandex".equals(detected)) {
                        platform = detected;
                        platformName = TrackState.detectPlatformName((String)title, artist, album);
                        playUrl = TrackState.generatePlayUrl((String)title, artist);
                    } else if ("yandex".equalsIgnoreCase(platform) && "\u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430".equals(platformName)) {
                        boolean hasYandexTrace;
                        String fullLower = ((String)title + " " + artist + " " + album).toLowerCase();
                        boolean bl2 = hasYandexTrace = fullLower.contains("\u044f\u043d\u0434\u0435\u043a\u0441") || fullLower.contains("yandex");
                        if (!(hasYandexTrace || playUrl != null && playUrl.contains("music.yandex"))) {
                            platform = "music";
                            platformName = "\u041c\u0443\u0437\u044b\u043a\u0430";
                        }
                    }
                    float speed = json.has("playbackSpeed") && !json.get("playbackSpeed").isJsonNull() ? json.get("playbackSpeed").getAsFloat() : 1.0f;
                    TrackState prev = this.currentState.get();
                    long reportedPosMs = posMs;
                    long latencyMs = Math.max(0L, Math.min(200L, (tEnd - tStart) / 2L));
                    long rawPosMs = reportedPosMs > 0L ? reportedPosMs + latencyMs : reportedPosMs;
                    posMs = rawPosMs;
                    if (prev != null && prev.isActive() && prev.title().equals(title) && prev.artist().equals(artist)) {
                        boolean rawChanged = reportedPosMs != this.lastRawReportedPosMs;
                        if (rawChanged) {
                            this.lastRawReportedPosMs = reportedPosMs;
                            this.lastRawPosChangeTimeMs = receivedTime;
                        }
                        if (prev.isPlaying() && isPlaying) {
                            long prevPos = prev.getInterpolatedPositionMs();
                            long diff = rawPosMs - prevPos;
                            if (!rawChanged) {
                                posMs = durMs > 0L ? Math.min(durMs, prevPos) : prevPos;
                                receivedTime = System.currentTimeMillis();
                            } else if (Math.abs(diff) < 250L) {
                                posMs = prevPos + (long)((double)diff * 0.5);
                                receivedTime = System.currentTimeMillis();
                            } else {
                                posMs = rawPosMs;
                                receivedTime = System.currentTimeMillis();
                            }
                        }
                    } else {
                        if (prev != null && prev.isActive() && rawPosMs > 3000L && (reportedPosMs == this.lastRawReportedPosMs || Math.abs(rawPosMs - prev.getInterpolatedPositionMs()) < 2500L)) {
                            posMs = 0L;
                        }
                        this.lastRawReportedPosMs = reportedPosMs;
                        this.lastRawPosChangeTimeMs = receivedTime;
                    }
                    TrackState state = new TrackState(true, (String)title, artist, album, status, posMs, durMs, coverHash, receivedTime, platform, platformName, playUrl, speed);
                    if (pulseState != null && pulseState.isActive() && !state.isPlaying()) {
                        this.currentState.set(pulseState);
                    } else {
                        this.currentState.set(state);
                        if (coverHash != null && !coverHash.equals(this.lastCoverHash)) {
                            this.fetchCover(coverHash);
                        }
                    }
                } else if (pulseState != null && pulseState.isActive()) {
                    this.currentState.set(pulseState);
                    this.backoffMs = Math.max(150, HudConfig.INSTANCE.pollIntervalMs);
                } else {
                    this.currentState.set(TrackState.INACTIVE);
                    this.backoffMs = 1000;
                }
            } else {
                conn.disconnect();
                this.backoffMs = 1000;
            }
        }
        catch (Exception e) {
            if (pulseState != null && pulseState.isActive()) {
                this.connected = true;
                this.currentState.set(pulseState);
                this.backoffMs = Math.max(150, HudConfig.INSTANCE.pollIntervalMs);
            } else {
                this.connected = false;
                this.currentState.set(TrackState.INACTIVE);
                this.backoffMs = 1200;
            }
        }
        this.scheduleNextPoll(this.backoffMs);
    }

    private void fetchCover(String coverHash) {
        try {
            int code;
            URL url = URI.create(this.baseUrl + "/cover").toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            if (this.lastCoverETag != null) {
                conn.setRequestProperty("If-None-Match", this.lastCoverETag);
            }
            if ((code = conn.getResponseCode()) == 304) {
                conn.disconnect();
                return;
            }
            if (code == 200) {
                String base64;
                String body;
                try (InputStream is = conn.getInputStream();){
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                JsonObject coverJson = JsonParser.parseString((String)body).getAsJsonObject();
                String string = base64 = coverJson.has("coverBase64") && !coverJson.get("coverBase64").isJsonNull() ? coverJson.get("coverBase64").getAsString() : null;
                if (base64 != null && !base64.isEmpty()) {
                    this.lastCoverHash = coverHash;
                    String etag = conn.getHeaderField("ETag");
                    this.lastCoverETag = etag != null ? etag : coverHash;
                    this.coverManager.updateCover(base64);
                }
            }
            conn.disconnect();
        }
        catch (Exception exception) {

        }
    }

    private String getStringOrDefault(JsonObject json, String key, String defaultVal) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultVal;
    }

    public void sendPlayPauseAsync() {
        if (LocalMusicPlayer.getInstance().isPlaying() || LocalMusicPlayer.getInstance().isPaused()) {
            LocalMusicPlayer.getInstance().togglePlayPause();
            return;
        }
        this.sendControlAsync("/control/play-pause");
    }

    public void sendNextAsync() {
        if (LocalMusicPlayer.getInstance().isPlaying() || LocalMusicPlayer.getInstance().isPaused()) {
            LocalMusicPlayer.getInstance().next();
            return;
        }
        this.sendControlAsync("/control/next");
    }

    public void sendPreviousAsync() {
        if (LocalMusicPlayer.getInstance().isPlaying() || LocalMusicPlayer.getInstance().isPaused()) {
            LocalMusicPlayer.getInstance().previous();
            return;
        }
        this.sendControlAsync("/control/previous");
    }

    private void sendControlAsync(String path) {
        if (!this.connected || this.baseUrl == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                URL url = URI.create(this.baseUrl + path).toURL();
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.setDoOutput(true);
                conn.getOutputStream().close();
                conn.getResponseCode();
                conn.disconnect();
            }
            catch (Exception exception) {

            }
        });
    }

    public static boolean isGenericArtist(String a) {
        return TrackMetadataSanitizer.isGenericArtist(a);
    }
}

