package com.example.yandexmusichud.network;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public record TrackState(boolean isActive, String title, String artist, String album, String status, long positionMs, long durationMs, String coverHash, long receivedAtMs, String platform, String platformName, String playUrl, float playbackSpeed, String directUrl, String coverUrl) {
    public static final TrackState INACTIVE = new TrackState(false, "", "", "", "stopped", 0L, 0L, null, 0L, "music", "\u041c\u0443\u0437\u044b\u043a\u0430", "", 1.0f, null, null);
    private static volatile long lastProcessCheckTime = 0L;
    private static volatile boolean cachedSpotifyRunning = false;
    private static final AtomicBoolean processCheckInFlight = new AtomicBoolean(false);

    public TrackState(boolean isActive, String title, String artist, String album, String status, long positionMs, long durationMs, String coverHash, long receivedAtMs, String platform, String platformName, String playUrl, float playbackSpeed) {
        this(isActive, title, artist, album, status, positionMs, durationMs, coverHash, receivedAtMs, platform, platformName, playUrl, playbackSpeed, null, null);
    }

    public TrackState(boolean isActive, String title, String artist, String album, String status, long positionMs, long durationMs, String coverHash, long receivedAtMs, String platform, String platformName, String playUrl) {
        this(isActive, title, artist, album, status, positionMs, durationMs, coverHash, receivedAtMs, platform, platformName, playUrl, 1.0f, null, null);
    }

    public TrackState(boolean isActive, String title, String artist, String album, String status, long positionMs, long durationMs, String coverHash, long receivedAtMs) {
        this(isActive, title, artist, album, status, positionMs, durationMs, coverHash, receivedAtMs, TrackState.detectPlatform(title, artist, album), TrackState.detectPlatformName(title, artist, album), TrackState.generatePlayUrl(title, artist), 1.0f, null, null);
    }

    public TrackState(boolean isActive, String title, String artist, String album, String status, long positionMs, long durationMs, String coverHash, long receivedAtMs, String directUrl, String coverUrl) {
        this(isActive, title, artist, album, status, positionMs, durationMs, coverHash, receivedAtMs, TrackState.detectPlatform(title, artist, album), TrackState.detectPlatformName(title, artist, album), TrackState.generatePlayUrl(title, artist), 1.0f, directUrl, coverUrl);
    }

    public long updateTime() {
        return this.receivedAtMs();
    }

    public static String detectPlatform(String title, String artist, String album) {
        String full = ((title != null ? title : "") + " " + (artist != null ? artist : "") + " " + (album != null ? album : "")).toLowerCase(Locale.ROOT);
        if (full.contains("\u043c\u043e\u044f \u043c\u0443\u0437\u044b\u043a\u0430") || full.contains("\u043b\u043e\u043a\u0430\u043b\u044c\u043d") || full.contains("local music") || full.contains("local audio")) {
            return "local";
        }
        if (full.contains("spotify") || full.contains("open.spotify")) {
            return "spotify";
        }
        if (full.contains("youtube") || full.contains("\u044e\u0442\u0443\u0431") || full.contains("music.youtube") || full.contains("youtu.be") || full.contains("- topic")) {
            return "youtube";
        }
        if (full.contains("\u0432\u043a\u043e\u043d\u0442\u0430\u043a\u0442\u0435") || full.contains("vk music") || full.contains("vk \u043c\u0443\u0437\u044b\u043a\u0430") || full.contains("vk.com")) {
            return "vk";
        }
        if (full.contains("soundcloud")) {
            return "soundcloud";
        }
        if (full.contains("apple music") || full.contains("itunes") || full.contains("music.apple")) {
            return "apple";
        }
        if (full.contains("yandex music") || full.contains("\u044f\u043d\u0434\u0435\u043a\u0441 \u043c\u0443\u0437\u044b\u043a\u0430") || full.contains("\u044f\u043d\u0434\u0435\u043a\u0441.\u043c\u0443\u0437\u044b\u043a\u0430") || full.contains("music.yandex")) {
            return "yandex";
        }
        if (full.contains("google chrome") || full.contains("microsoft edge") || full.contains("mozilla firefox") || full.contains("opera gx") || full.contains("brave browser") || full.contains("\u044f\u043d\u0434\u0435\u043a\u0441 \u0431\u0440\u0430\u0443\u0437\u0435\u0440") || full.contains("yandex browser")) {
            return "browser";
        }
        if (full.contains("aimp") || full.contains("vlc media player") || full.contains("foobar2000") || full.contains("winamp")) {
            return "player";
        }
        return "music";
    }

    public static boolean isSpotifyRunning() {
        long now = System.currentTimeMillis();
        if (now - lastProcessCheckTime >= 2500L && processCheckInFlight.compareAndSet(false, true)) {
            lastProcessCheckTime = now;
            CompletableFuture.runAsync(() -> {
                try {
                    cachedSpotifyRunning = ProcessHandle.allProcesses().anyMatch(p -> {
                        String cmd = p.info().command().orElse("").toLowerCase(Locale.ROOT);
                        return cmd.contains("spotify.exe") || cmd.endsWith("\\spotify") || cmd.endsWith("/spotify") || cmd.contains("spotify");
                    });
                }
                catch (Exception e) {
                    cachedSpotifyRunning = false;
                }
                finally {
                    processCheckInFlight.set(false);
                }
            });
        }
        return cachedSpotifyRunning;
    }

    public static String platformDisplayName(String p) {
        if (p == null) {
            return "\u041c\u0443\u0437\u044b\u043a\u0430";
        }
        return switch (p) {
            case "local" -> "\u041c\u043e\u044f \u043c\u0443\u0437\u044b\u043a\u0430";
            case "spotify" -> "Spotify";
            case "youtube" -> "YouTube Music";
            case "soundcloud" -> "SoundCloud";
            case "vk" -> "VK \u041c\u0443\u0437\u044b\u043a\u0430";
            case "apple" -> "Apple Music";
            case "yandex" -> "\u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430";
            case "browser" -> "\u0411\u0440\u0430\u0443\u0437\u0435\u0440";
            case "player" -> "\u041c\u0435\u0434\u0438\u0430\u043f\u043b\u0435\u0435\u0440";
            default -> "\u041c\u0443\u0437\u044b\u043a\u0430";
        };
    }

    public static String detectPlatformName(String title, String artist, String album) {
        return TrackState.platformDisplayName(TrackState.detectPlatform(title, artist, album));
    }

    public String detectPlatformName() {
        if (this.platformName != null && !this.platformName.isBlank()) {
            return this.platformName;
        }
        return TrackState.detectPlatformName(this.title, this.artist, this.album);
    }

    public static String generatePlayUrlForPlatform(String p, String title, String artist) {
        if (title == null || title.isBlank()) {
            return "https://music.yandex.ru";
        }
        String query = URLEncoder.encode(((String)(artist != null ? artist + " " : "") + title).trim(), StandardCharsets.UTF_8);
        return switch (p != null ? p : "music") {
            case "spotify" -> "https://open.spotify.com/search/" + query;
            case "youtube" -> "https://music.youtube.com/search?q=" + query;
            case "soundcloud" -> "https://soundcloud.com/search?q=" + query;
            case "vk" -> "https://vk.com/audio";
            case "apple" -> "https://music.apple.com/search?term=" + query;
            case "yandex" -> "https://music.yandex.ru/search?text=" + query;
            default -> "https://music.yandex.ru/search?text=" + query;
        };
    }

    public static String generatePlayUrl(String title, String artist) {
        if (title == null || title.isBlank()) {
            return "https://music.yandex.ru";
        }
        return TrackState.generatePlayUrlForPlatform(TrackState.detectPlatform(title, artist, ""), title, artist);
    }

    public boolean isPlaying() {
        return "playing".equalsIgnoreCase(this.status);
    }

    public boolean isPaused() {
        return "paused".equalsIgnoreCase(this.status);
    }

    public long getInterpolatedPositionMs() {
        if (!this.isPlaying() || this.receivedAtMs == 0L) {
            return this.positionMs;
        }
        long elapsed = System.currentTimeMillis() - this.receivedAtMs;
        float speed = this.playbackSpeed > 0.0f ? this.playbackSpeed : 1.0f;
        long interpolated = this.positionMs + (speed == 1.0f ? elapsed : (long)((float)elapsed * speed));
        return this.durationMs > 0L ? Math.min(interpolated, this.durationMs) : interpolated;
    }

    public float getProgress() {
        if (this.durationMs <= 0L) {
            return 0.0f;
        }
        return Math.min(1.0f, (float)this.getInterpolatedPositionMs() / (float)this.durationMs);
    }

    public String getExplicitTrackId() {
        if (this.playUrl != null && this.playUrl.contains("/track/")) {
            try {
                int idx = this.playUrl.indexOf("/track/");
                String part = this.playUrl.substring(idx + 7);
                int end = part.indexOf('?');
                if (end >= 0) part = part.substring(0, end);
                end = part.indexOf('&');
                if (end >= 0) part = part.substring(0, end);
                end = part.indexOf('/');
                if (end >= 0) part = part.substring(0, end);
                end = part.indexOf('#');
                if (end >= 0) part = part.substring(0, end);
                return part.trim();
            } catch (Exception ignored) {}
        }
        return null;
    }
}
