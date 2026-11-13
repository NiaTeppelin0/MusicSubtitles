package com.example.yandexmusichud.network;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.local.LocalMusicPlayer;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NowPlayingClient {
    public static NowPlayingClient INSTANCE;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(2L))
        .build();
    private final AtomicReference<TrackState> currentState = new AtomicReference<TrackState>(TrackState.INACTIVE);
    private final ExecutorService coverExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicSubtitles-CoverFetcher");
        t.setDaemon(true);
        return t;
    });
    private ScheduledExecutorService scheduler;
    private final CoverTextureManager coverManager;
    private volatile String baseUrl;
    private volatile String lastCoverHash = null;
    private volatile String lastCoverETag = null;
    private volatile String pendingCoverHash = null;
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

    public synchronized void start(int port) {
        this.stop();
        this.baseUrl = "http://127.0.0.1:" + port;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MusicSubtitles-Poller");
            t.setDaemon(true);
            return t;
        });
        this.scheduleNextPoll(0);
    }

    public synchronized void stop() {
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
            this.scheduler = null;
        }
    }

    public synchronized void shutdown() {
        this.stop();
        this.coverExecutor.shutdownNow();
    }

    private synchronized void scheduleNextPoll(int delayMs) {
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
            String urlStr = this.baseUrl + "/now-playing";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .timeout(Duration.ofMillis(400L))
                .GET()
                .build();
            long tStart = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            long tEnd = System.currentTimeMillis();
            int code = response.statusCode();
            long receivedTime = tEnd;
            if (code == 200) {
                boolean isActive;
                this.connected = true;
                this.backoffMs = Math.max(150, HudConfig.INSTANCE.pollIntervalMs);
                String body = response.body();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                JsonElement activeEl = json.get("isActive");
                boolean bl = isActive = activeEl != null && activeEl.isJsonPrimitive() && activeEl.getAsBoolean();
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
                    long posMs = this.getLongOrDefault(json, "positionMs", 0L);
                    long durMs = this.getLongOrDefault(json, "durationMs", 0L);
                    String coverHash = this.getStringOrDefault(json, "coverHash", null);
                    String detected = TrackState.detectPlatform(title, artist, album);
                    String platform = this.getStringOrDefault(json, "platform", null);
                    if (platform == null) {
                        platform = detected;
                    }
                    String platformName = this.getStringOrDefault(json, "platformName", null);
                    if (platformName == null) {
                        platformName = TrackState.platformDisplayName(platform);
                    }
                    String playUrl = this.getStringOrDefault(json, "playUrl", null);
                    if (playUrl == null) {
                        playUrl = TrackState.generatePlayUrlForPlatform(detected, title, artist);
                    }
                    if (!"music".equals(detected) && !"yandex".equals(detected)) {
                        platform = detected;
                        platformName = TrackState.platformDisplayName(detected);
                        playUrl = TrackState.generatePlayUrlForPlatform(detected, title, artist);
                    } else if ("yandex".equalsIgnoreCase(platform) && "\u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430".equals(platformName)) {
                        boolean hasYandexTrace;
                        String fullLower = (title + " " + artist + " " + album).toLowerCase(Locale.ROOT);
                        boolean bl2 = hasYandexTrace = fullLower.contains("\u044f\u043d\u0434\u0435\u043a\u0441") || fullLower.contains("yandex");
                        if (!(hasYandexTrace || playUrl != null && playUrl.contains("music.yandex"))) {
                            platform = "music";
                            platformName = "\u041c\u0443\u0437\u044b\u043a\u0430";
                        }
                    }
                    float speed = this.getFloatOrDefault(json, "playbackSpeed", 1.0f);
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
                    TrackState state = new TrackState(true, title, artist, album, status, posMs, durMs, coverHash, receivedTime, platform, platformName, playUrl, speed);
                    if (pulseState != null && pulseState.isActive() && !state.isPlaying()) {
                        this.currentState.set(pulseState);
                    } else {
                        this.currentState.set(state);
                        if (coverHash != null && !coverHash.equals(this.lastCoverHash)) {
                            this.fetchCoverAsync(coverHash);
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

    private void fetchCoverAsync(String coverHash) {
        if (coverHash == null || coverHash.equals(this.lastCoverHash) || coverHash.equals(this.pendingCoverHash) || this.coverExecutor.isShutdown()) {
            return;
        }
        this.pendingCoverHash = coverHash;
        String urlBase = this.baseUrl;
        if (urlBase == null) {
            this.pendingCoverHash = null;
            return;
        }
        try {
            this.coverExecutor.execute(() -> {
                try {
                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(urlBase + "/cover"))
                        .timeout(Duration.ofSeconds(2L))
                        .GET();
                    if (this.lastCoverETag != null) {
                        reqBuilder.header("If-None-Match", this.lastCoverETag);
                    }
                    HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                    int code = response.statusCode();
                    if (code == 304) {
                        this.lastCoverHash = coverHash;
                        return;
                    }
                    if (code == 200) {
                        JsonObject coverJson = JsonParser.parseString(response.body()).getAsJsonObject();
                        String base64 = this.getStringOrDefault(coverJson, "coverBase64", null);
                        if (base64 != null && !base64.isEmpty()) {
                            this.lastCoverHash = coverHash;
                            this.lastCoverETag = response.headers().firstValue("ETag").orElse(coverHash);
                            this.coverManager.updateCover(base64);
                        }
                    }
                }
                catch (Exception exception) {

                }
                finally {
                    if (coverHash.equals(this.pendingCoverHash)) {
                        this.pendingCoverHash = null;
                    }
                }
            });
        }
        catch (Exception e) {
            if (coverHash.equals(this.pendingCoverHash)) {
                this.pendingCoverHash = null;
            }
        }
    }

    private String getStringOrDefault(JsonObject json, String key, String defaultVal) {
        JsonElement el = json.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : defaultVal;
    }

    private long getLongOrDefault(JsonObject json, String key, long defaultVal) {
        JsonElement el = json.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsLong();
            }
            catch (Exception exception) {}
        }
        return defaultVal;
    }

    private float getFloatOrDefault(JsonObject json, String key, float defaultVal) {
        JsonElement el = json.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsFloat();
            }
            catch (Exception exception) {}
        }
        return defaultVal;
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
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(this.baseUrl + path))
            .timeout(Duration.ofSeconds(1L))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    public static boolean isGenericArtist(String a) {
        return TrackMetadataSanitizer.isGenericArtist(a);
    }
}

