package com.example.yandexmusichud.stats;

import com.example.yandexmusichud.network.TrackState;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_2561;
import net.minecraft.class_310;

public class SessionStatsManager {
    public static final SessionStatsManager INSTANCE = new SessionStatsManager();
    private final long sessionStartTime = System.currentTimeMillis();
    private long totalActiveListeningMs = 0L;
    private long lastTickTime = System.currentTimeMillis();
    private int totalTracksPlayed = 0;
    private String lastCountedTrackKey = "";
    private String lastSeenRawArtist = null;
    private String lastSeenRawTitle = null;
    private final Map<String, Integer> artistCount = new ConcurrentHashMap<String, Integer>();
    private final Map<String, Integer> trackCount = new ConcurrentHashMap<String, Integer>();

    private SessionStatsManager() {
    }

    private void recordActiveTrack(String rawArtist, String rawTitle) {
        String artist = rawArtist != null ? rawArtist.trim() : "";
        String title = rawTitle != null ? rawTitle.trim() : "";
        if (title.isEmpty()) {
            return;
        }
        if (artist.equals(this.lastSeenRawArtist) && title.equals(this.lastSeenRawTitle)) {
            return;
        }
        this.lastSeenRawArtist = artist;
        this.lastSeenRawTitle = title;
        String trackKey = (artist + "___" + title).toLowerCase(Locale.ROOT);
        if (!trackKey.equals(this.lastCountedTrackKey)) {
            this.lastCountedTrackKey = trackKey;
            ++this.totalTracksPlayed;
            if (!artist.isEmpty()) {
                this.artistCount.merge(artist, 1, Integer::sum);
            }
            this.trackCount.merge(title, 1, Integer::sum);
        }
    }

    public void onTick(TrackState state) {
        long now = System.currentTimeMillis();
        long delta = now - this.lastTickTime;
        this.lastTickTime = now;
        com.example.yandexmusichud.local.LocalMusicPlayer local = com.example.yandexmusichud.local.LocalMusicPlayer.getInstance();
        boolean localPlaying = local != null && local.isPlaying() && !local.isPaused();
        if (localPlaying) {
            if (delta > 0L && delta < 5000L) {
                this.totalActiveListeningMs += delta;
            }
            com.example.yandexmusichud.local.LocalMusicTrack curTrack = local.getCurrentTrack();
            if (curTrack != null) {
                this.recordActiveTrack(curTrack.getArtist(), curTrack.getTitle());
            }
            return;
        }
        if (state != null && state.isActive() && "playing".equalsIgnoreCase(state.status())) {
            if (delta > 0L && delta < 5000L) {
                this.totalActiveListeningMs += delta;
            }
            this.recordActiveTrack(state.artist(), state.title());
        }
    }

    public long getTotalActiveListeningMs() {
        return this.totalActiveListeningMs;
    }

    public String getFormattedListeningTime() {
        long seconds = this.totalActiveListeningMs / 1000L;
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return String.format("%d ч. %d мин.", hours, minutes);
        }
        if (minutes > 0L) {
            return String.format("%d мин. %d сек.", minutes, secs);
        }
        return String.format("%d сек.", secs);
    }

    public int getTotalTracksPlayed() {
        return this.totalTracksPlayed;
    }

    public String getTopArtist() {
        return this.artistCount.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> (String)e.getKey() + " (" + String.valueOf(e.getValue()) + " тр.)").orElse("—");
    }

    public String getTopTrack() {
        return this.trackCount.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");
    }

    public boolean shareStatsToChat() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return false;
        }
        String timeStr = this.getFormattedListeningTime();
        int count = this.totalTracksPlayed;
        String topArtist = this.getTopArtist();
        String topSong = this.getTopTrack();
        String playerName = mc.field_1724.method_5477().getString();
        String cleanMsg = "[Music] " + playerName + ": сессия — " + count + " треков (" + timeStr + "), топ: " + topArtist + " - " + topSong;
        String lineHeader = "§6§l♫ Music Subtitles §8| §fСессия игрока §e" + playerName;
        String line1 = "§7⏱ Время прослушивания: §a" + timeStr;
        String line2 = "§7♫ Всего треков: §b" + count;
        String line3 = "§7★ Топ артист: §d" + topArtist;
        String line4 = "§7♪ Топ трек: §f" + topSong;
        mc.execute(() -> {
            try {
                if (mc.field_1724 != null) {
                    if (mc.field_1724.field_3944 != null) {
                        try {
                            mc.field_1724.field_3944.method_45729(cleanMsg);
                        } catch (Throwable ignored) {
                        }
                    }
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470(lineHeader), false);
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470(line1 + " §8• " + line2), false);
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470(line3 + " §8• " + line4), false);
                    mc.field_1724.method_5783(net.minecraft.class_3417.field_14627, 0.9f, 1.25f);
                }
            } catch (Throwable ignored) {
            }
        });
        return true;
    }
}
