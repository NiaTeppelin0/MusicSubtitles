package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.lyrics.GeniusLyricsProvider;
import com.example.yandexmusichud.lyrics.LyricLine;
import com.example.yandexmusichud.lyrics.LyricsConsensusEngine;
import com.example.yandexmusichud.lyrics.StudioCascadeAligner;
import com.example.yandexmusichud.lyrics.SubtitleBubble;
import com.example.yandexmusichud.network.TrackState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_124;
import net.minecraft.class_1297;
import net.minecraft.class_2350;
import net.minecraft.class_2382;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2568;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4184;
import net.minecraft.class_5250;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LyricsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MusicSubtitles");
    private static final Pattern LRC_LINE_PATTERN = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$");
    private static final Pattern ENHANCED_WORD_PATTERN = Pattern.compile("<(\\d+):(\\d+(?:\\.\\d+)?)>(.*?)(?=<\\d+:\\d+(?:\\.\\d+)?>|$)");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("^\\[offset:([+-]?\\d+)\\]", 2);
    private static final Pattern NOISE_PATTERN = Pattern.compile("(?i)\\s*[\\[\\(]?(?:official\\s*(?:music\\s*)?(?:video|audio|lyric\\s*video|visualizer|clip)|\u043f\u0440\u0435\u043c\u044c\u0435\u0440\u0430\\s*\u043a\u043b\u0438\u043f\u0430|\u043a\u043b\u0438\u043f|\u043b\u0438\u0440\u0438\u043a\\s*\u0432\u0438\u0434\u0435\u043e|slowed\\s*\\+?\\s*reverb|speed\\s*up|nightcore|sped\\s*up|mood\\s*video|remastered|deluxe|prod\\.?\\s+by|4k|hd|hq|live|acoustic|remix)[\\]\\)]?.*");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("^(.*?)\\s+[-\u2014\u2013]\\s+(.*)$");
    private static final Pattern SPEED_UP_PATTERN = Pattern.compile("(?i)(?:speed\\s*[-_]?\\s*up|sped\\s*[-_]?\\s*up|nightcore|fast\\s*version|fast\\s*ver|\u0443\u0441\u043a\u043e\u0440\u0435\u043d\u043d(?:\u0430\u044f|\u044b\u0439|\u043e\u0435|\u043e))");
    private static final Pattern SLOWED_PATTERN = Pattern.compile("(?i)(?:slowed\\s*(?:(?:\\+|&|and)?\\s*reverb)?|slowed\\s*[-_]?\\s*down|daycore|chopped\\s*(?:and|&)?\\s*screwed|\u0437\u0430\u043c\u0435\u0434\u043b\u0435\u043d\u043d(?:\u0430\u044f|\u044b\u0439|\u043e\u0435|\u043e))");
    private static final double SPEED_UP_FACTOR = 0.75;
    private static final double SLOWED_FACTOR = 1.3;
    private static final Path LOCAL_LYRICS_DIR = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("lyrics");
    public static final long PERCEPTUAL_LEAD_MS = 70L;
    private final Map<String, List<LyricLine>> lyricsCache = new ConcurrentHashMap<String, List<LyricLine>>();
    private final Map<String, Long> failedFetchTimes = new ConcurrentHashMap<String, Long>();
    private final List<SubtitleBubble> activeBubbles = new CopyOnWriteArrayList<SubtitleBubble>();
    private final ExecutorService executor;
    private final Random random = new Random();
    private volatile String currentTrackKey = "";
    private volatile TrackState lastKnownState = null;
    private LyricLine lastSpawnedLine = null;
    private LyricLine cachedSprintLine = null;
    private List<LyricLine> cachedSprintChunks = new ArrayList<LyricLine>();
    private LyricLine lastSpawnedSprintChunk = null;
    private int sprintChunkIndex = 0;
    private final AtomicLong currentFetchId = new AtomicLong(0L);
    private volatile String activeFetchTrackKey = null;
    private volatile long lastFetchedDurationMs = 0L;
    private long lastBlockRaycastTime = 0L;
    private long lastSprintActiveTime = 0L;
    private final Set<String> disabledTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<String> promptedMismatchTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<String> mismatchedTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<String> userSavedTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<String> checkedMismatchTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<String> realSyncedTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private final AtomicInteger acousticMismatchStrikes = new AtomicInteger(0);
    private volatile long lastAcousticCheckTime = 0L;
    private volatile boolean isAcousticChecking = false;
    private volatile double smoothAcousticOffset = 0.0;
    private volatile int acousticPassesDone = 0;
    private volatile long lastAcousticCaptureTime = 0L;
    private volatile boolean acousticSupervisorBusy = false;
    private final List<Double> reliableLagSamples = new ArrayList<Double>();
    private final Set<String> autoShutoffTracks = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final Pattern BROWSER_SUFFIX_PATTERN = Pattern.compile("(?i)\\s*[-\u2013\u2014|/]\\s*(?:YouTube\\s*(?:Music)?|\u042f\u043d\u0434\u0435\u043a\u0441\\s*\u041c\u0443\u0437\u044b\u043a\u0430|Yandex\\s*Music|SoundCloud|Spotify|Google\\s*Chrome|Microsoft\\s*Edge|Opera|Firefox|Brave|Vivaldi)\\s*$");
    private static final Pattern PIPE_NOISE_PATTERN = Pattern.compile("(?i)\\s*[|/]\\s*(?:Official|\u041f\u0440\u0435\u043c\u044c\u0435\u0440\u0430|\u041a\u043b\u0438\u043f|Video|Audio|Visualizer|Lyric|Remastered|Mood|Soundtrack|OST).*$");
    private static final Pattern BRACKET_TAGS_PATTERN = Pattern.compile("(?i)\\s*[\\[\\(]\\s*(?:official(?:\\s+(?:music\\s+)?video|\\s+audio|\\s+visualizer|\\s+live|\\s*\\d+k)?|music\\s+video|video|audio|visualizer|lyric\\s+video|lyrics|\u043a\u043b\u0438\u043f|\u043f\u0440\u0435\u043c\u044c\u0435\u0440\u0430(?:\\s+\u043a\u043b\u0438\u043f\u0430|\\s+\u0442\u0440\u0435\u043a\u0430)?(?:\\s*\\d{4})?|\u043e\u0444\u0438\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439\\s+\u043a\u043b\u0438\u043f|\u0432\u0438\u0434\u0435\u043e\u043a\u043b\u0438\u043f|mood\\s+video|live(?:\\s+performance|\\s+session)?|acoustic(?:\\s+version)?|remastered(?:\\s*\\d{4})?|deluxe(?:\\s+edition)?|\u0430\u043b\u044c\u0431\u043e\u043c(?:\\s+.*)?|album(?:\\s+.*)?|\u0441\u0438\u043d\u0433\u043b(?:\\s+.*)?|single(?:\\s+.*)?|\u043c\u0438\u043d\u0438[-\u0430]\u043b\u044c\u0431\u043e\u043c(?:\\s+.*)?|ep(?:\\s+.*)?|prod(?:\\.|\\s+by)?\\s+[^)\\]]+|feat\\.?\\s+[^)\\]]+|ft\\.?\\s+[^)\\]]+|slowed(?:\\s*\\+?\\s*reverb)?|speed\\s*up|sped\\s*up|nightcore|hd|4k|hq|4k\\s*60fps|\\d{4}|ost(?:\\s+.*)?|soundtrack(?:\\s+.*)?)\\s*[\\]\\)]");
    private static final Pattern TRAILING_NOISE_PATTERN = Pattern.compile("(?i)\\s*[-\u2013\u2014]\\s*(?:\u041f\u0440\u0435\u043c\u044c\u0435\u0440\u0430\\s+\u043a\u043b\u0438\u043f\u0430|\u041e\u0444\u0438\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439\\s+\u043a\u043b\u0438\u043f|Official\\s+Video|Lyric\\s+Video)(?:\\s*\\d{4})?$");

    public void disableCurrentTrack() {
        if (this.currentTrackKey != null && !this.currentTrackKey.isEmpty()) {
            this.disabledTracks.add(this.currentTrackKey);
            this.mismatchedTracks.remove(this.currentTrackKey);
            this.activeBubbles.clear();
            this.lastSpawnedLine = null;
            this.lastSpawnedSprintChunk = null;
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
        }
    }

    public void enableCurrentTrack() {
        if (this.currentTrackKey != null && !this.currentTrackKey.isEmpty()) {
            this.disabledTracks.remove(this.currentTrackKey);
            this.promptedMismatchTracks.remove(this.currentTrackKey);
            this.mismatchedTracks.remove(this.currentTrackKey);
            this.autoShutoffTracks.remove(this.currentTrackKey);
            this.acousticMismatchStrikes.set(0);
            this.activeBubbles.clear();
            this.lastSpawnedLine = null;
            this.lastSpawnedSprintChunk = null;
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
        }
    }

    public boolean isCurrentTrackDisabled() {
        return this.currentTrackKey != null && this.disabledTracks.contains(this.currentTrackKey);
    }

    public boolean isCurrentTrackMismatched() {
        if (this.currentTrackKey == null || this.disabledTracks.contains(this.currentTrackKey) || this.userSavedTracks.contains(this.currentTrackKey)) {
            return false;
        }
        return this.mismatchedTracks.contains(this.currentTrackKey) || this.acousticMismatchStrikes.get() >= 2;
    }

    public void reloadCurrentTrack() {
        this.lyricsCache.clear();
        this.realSyncedTracks.clear();
        this.promptedMismatchTracks.clear();
        this.checkedMismatchTracks.clear();
        this.autoShutoffTracks.clear();
        this.acousticMismatchStrikes.set(0);
        this.smoothAcousticOffset = 0.0;
        this.acousticPassesDone = 0;
        this.lastAcousticCaptureTime = 0L;
        this.acousticSupervisorBusy = false;
        synchronized (this.reliableLagSamples) {
            this.reliableLagSamples.clear();
        }
        this.activeBubbles.clear();
        this.lastSpawnedLine = null;
        this.lastSpawnedSprintChunk = null;
        this.cachedSprintLine = null;
        this.cachedSprintChunks.clear();
        this.currentTrackKey = "";
    }

    public String resolveEffectiveMode(TrackState state) {
        String baseMode = HudConfig.INSTANCE.subtitlesMode != null && !HudConfig.INSTANCE.subtitlesMode.isBlank() ? HudConfig.INSTANCE.subtitlesMode : "WORLD_3D";
        TrackState eff = state != null ? state : this.lastKnownState;
        if (eff != null && eff.isActive()) {
            String trackMode = HudConfig.INSTANCE.getTrackMode(eff.artist(), eff.title(), baseMode);
            if (trackMode != null && !trackMode.isBlank() && !"DEFAULT".equalsIgnoreCase(trackMode)) {
                return trackMode;
            }
        }
        return baseMode;
    }

    private void postLineToChat(LyricLine line, TrackState state) {
        if (line == null || line.text == null || line.text.isBlank()) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        TrackState eff = state != null ? state : this.lastKnownState;
        int baseColor = HudConfig.INSTANCE.subtitlesColor & 0xFFFFFF;
        String baseFont = HudConfig.INSTANCE.subtitlesFont;
        if (eff != null && eff.isActive()) {
            baseColor = HudConfig.INSTANCE.getTrackColor(eff.artist(), eff.title(), baseColor) & 0xFFFFFF;
            baseFont = HudConfig.INSTANCE.getTrackFont(eff.artist(), eff.title(), baseFont);
        }
        final int finalColor = baseColor;
        final String finalFont = baseFont;
        mc.execute(() -> {
            if (mc.field_1705 != null && mc.field_1705.method_1743() != null) {
                class_5250 msg = class_2561.method_43470("\u00a76\u266a ").method_10852(line.toFormattedText(finalColor, finalFont));
                mc.field_1705.method_1743().method_1812((class_2561)msg);
            }
        });
    }

    public LyricsManager() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MusicSubtitles-LyricsWorker");
            t.setDaemon(true);
            return t;
        });
        try {
            Files.createDirectories(LOCAL_LYRICS_DIR, new FileAttribute[0]);
        }
        catch (Exception exception) {

        }
    }

    public String getCurrentTrackKey() {
        return this.currentTrackKey;
    }

    public List<LyricLine> getLyricsForTrack(TrackState state) {
        if (state == null) {
            return null;
        }
        return this.lyricsCache.get(this.getTrackKey(state));
    }

    public List<LyricLine> findLyrics(String artist, String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String cleanTitle = title.trim().toLowerCase();
        String cleanArtist = artist != null ? artist.trim().toLowerCase() : "";
        for (Map.Entry<String, List<LyricLine>> entry : this.lyricsCache.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains(cleanTitle)) {
                if (cleanArtist.isEmpty() || key.contains(cleanArtist)) {
                    List<LyricLine> lines = entry.getValue();
                    if (lines != null && !lines.isEmpty()) {
                        return lines;
                    }
                }
            }
        }
        return null;
    }

    public void shutdown() {
        this.executor.shutdownNow();
    }

    public List<SubtitleBubble> getActiveBubbles() {
        return this.activeBubbles;
    }

    public LyricLine getCurrentActiveLine(TrackState state, long currentPosMs) {
        if (state == null || !state.isActive() || this.isCurrentTrackDisabled()) {
            return null;
        }
        List<LyricLine> lines = this.getLyricsForTrack(state);
        if (lines != null && !lines.isEmpty()) {
            for (int i = lines.size() - 1; i >= 0; --i) {
                LyricLine line = lines.get(i);
                if (currentPosMs >= line.startMs && currentPosMs < line.endMs) {
                    return line;
                }
            }
        }
        for (int i = this.activeBubbles.size() - 1; i >= 0; --i) {
            SubtitleBubble b = this.activeBubbles.get(i);
            if (b != null && b.line != null && b.fadeOutStartSystemTimeMs < 0L) {
                return b.line;
            }
        }
        return null;
    }

    public LyricLine getUpcomingLine(TrackState state, long currentPosMs) {
        if (state == null || !state.isActive() || this.isCurrentTrackDisabled()) {
            return null;
        }
        List<LyricLine> lines = this.getLyricsForTrack(state);
        if (lines != null && !lines.isEmpty()) {
            for (int i = 0; i < lines.size(); ++i) {
                LyricLine line = lines.get(i);
                if (line.startMs > currentPosMs && line.startMs - currentPosMs <= 8000L) {
                    return line;
                }
            }
        }
        return null;
    }

    public void update(TrackState state, int screenWidth, int screenHeight) {
        boolean isSprinting;
        if (!HudConfig.INSTANCE.subtitlesEnabled) {
            this.activeBubbles.clear();
            this.lastSpawnedLine = null;
            return;
        }
        final long now = System.currentTimeMillis();
        boolean hasLiveBubbles = false;
        for (SubtitleBubble b : this.activeBubbles) {
            if (b.isLiveCaption) {
                hasLiveBubbles = true;
                break;
            }
        }
        if (state == null || !state.isActive() || state.title().isEmpty()) {
            this.lastKnownState = state;
            if (!hasLiveBubbles) {
                this.activeBubbles.clear();
            } else {
                this.activeBubbles.removeIf(b -> b.isExpired(0L, now));
            }
            this.lastSpawnedLine = null;
            return;
        }
        this.lastKnownState = state;
        String trackKey = this.getTrackKey(state);
        if (!trackKey.equals(this.currentTrackKey)) {
            this.currentTrackKey = trackKey;
            this.lastFetchedDurationMs = state.durationMs();
            this.lastSpawnedLine = null;
            this.lastSpawnedSprintChunk = null;
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
            this.activeBubbles.removeIf(b -> !b.isLiveCaption);
            this.acousticMismatchStrikes.set(0);
            this.lastAcousticCheckTime = System.currentTimeMillis();
            this.smoothAcousticOffset = 0.0;
            this.acousticPassesDone = 0;
            this.lastAcousticCaptureTime = 0L;
            this.acousticSupervisorBusy = false;
            synchronized (this.reliableLagSamples) {
                this.reliableLagSamples.clear();
            }
            this.fetchLyricsAsync(state);
        } else if (this.lastFetchedDurationMs <= 0L && state.durationMs() > 15000L && !this.userSavedTracks.contains(trackKey)) {
            this.lastFetchedDurationMs = state.durationMs();
            List<LyricLine> existing = this.lyricsCache.get(trackKey);
            if (existing != null && !existing.isEmpty() && this.realSyncedTracks.contains(trackKey) && !this.isLyricsMismatched(existing, state)) {
                this.checkedMismatchTracks.add(trackKey);
            } else if (this.activeFetchTrackKey == null || !this.activeFetchTrackKey.equals(trackKey)) {
                this.lyricsCache.remove(trackKey);
                this.realSyncedTracks.remove(trackKey);
                this.failedFetchTimes.remove(trackKey);
                this.checkedMismatchTracks.remove(trackKey);
                this.mismatchedTracks.remove(trackKey);
                this.fetchLyricsAsync(state);
            }
        } else if (!(this.lyricsCache.containsKey(trackKey) || this.activeFetchTrackKey != null && this.activeFetchTrackKey.equals(trackKey))) {
            this.lastFetchedDurationMs = state.durationMs();
            this.fetchLyricsAsync(state);
        }
        if (this.disabledTracks.contains(trackKey)) {
            this.activeBubbles.removeIf(b -> !b.isLiveCaption);
            this.lastSpawnedLine = null;
            return;
        }
        List<LyricLine> lines = this.lyricsCache.get(trackKey);
        if (lines != null && !lines.isEmpty()) {
            if (!this.checkedMismatchTracks.contains(trackKey)) {
                this.checkedMismatchTracks.add(trackKey);
                if (!this.userSavedTracks.contains(trackKey) && this.isLyricsMismatched(lines, state)) {
                    this.mismatchedTracks.add(trackKey);
                    if (!this.promptedMismatchTracks.contains(trackKey)) {
                        this.promptedMismatchTracks.add(trackKey);
                        this.sendMismatchNotification();
                    }
                }
            }
        }
        long rawPosMs = state.getInterpolatedPositionMs();
        int trackOffset = HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title());
        long currentPosMs = Math.max(0L, rawPosMs + (long)HudConfig.INSTANCE.subtitlesOffsetMs + (long)trackOffset + (long)this.getLiveAcousticOffsetMs() + PERCEPTUAL_LEAD_MS);
        this.activeBubbles.removeIf(b -> b.isExpired(currentPosMs, now));
        if (!state.isPlaying()) {
            for (SubtitleBubble b : this.activeBubbles) {
                if (!b.isLiveCaption) {
                    b.startFadeOutPause(now);
                }
            }
            this.lastSpawnedLine = null;
            this.lastSpawnedSprintChunk = null;
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
            return;
        }
        if (this.lastSpawnedLine != null && currentPosMs < this.lastSpawnedLine.startMs - 400L) {
            this.lastSpawnedLine = null;
            this.lastSpawnedSprintChunk = null;
        }
        if (lines == null || lines.isEmpty()) {
            return;
        }
        this.triggerLiveAcousticSupervisorIfNeeded(state, lines);
        LyricLine activeLine = null;
        for (int i = lines.size() - 1; i >= 0; --i) {
            LyricLine line = lines.get(i);
            if (currentPosMs < line.startMs || currentPosMs >= line.endMs) continue;
            activeLine = line;
            break;
        }
        String effectiveMode = this.resolveEffectiveMode(state);
        boolean modeIs3D = "WORLD_3D".equalsIgnoreCase(effectiveMode);
        class_310 mc = class_310.method_1551();
        boolean physicallySprinting = mc.field_1724 != null && (mc.field_1724.method_5624() || mc.field_1724.method_18798().method_37268() > 0.04 && !mc.field_1724.method_5715());
        if (physicallySprinting) {
            this.lastSprintActiveTime = now;
        }
        isSprinting = physicallySprinting || (now - this.lastSprintActiveTime < 1000L);
        if (modeIs3D && HudConfig.INSTANCE.sprintWordsEnabled && isSprinting) {
            this.lastSpawnedLine = null;
            this.activeBubbles.removeIf(b -> !b.isFastPaced);
            this.updateSprint3DBubbles(activeLine, currentPosMs);
        } else {
            this.lastSpawnedSprintChunk = null;
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
            if (activeLine != null) {
                boolean modeIsBlock = "WORLD_BLOCK".equalsIgnoreCase(effectiveMode) || "WORLD_SURFACE".equalsIgnoreCase(effectiveMode);
                if (activeLine != this.lastSpawnedLine) {
                    this.lastSpawnedLine = activeLine;
                    this.spawnBubble(activeLine, screenWidth, screenHeight);
                } else if (modeIsBlock && (!this.hasBlockAttachedBubble() || this.activeBubbles.isEmpty()) && now - this.lastBlockRaycastTime > 250L) {
                    this.lastBlockRaycastTime = now;
                    this.tryAttachToBlock(activeLine);
                }
            } else {
                this.lastSpawnedLine = null;
            }
        }
    }

    private void updateSprint3DBubbles(LyricLine activeLine, long currentPosMs) {
        if (activeLine == null) {
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
            this.lastSpawnedSprintChunk = null;
            return;
        }
        if (activeLine != this.cachedSprintLine) {
            this.cachedSprintLine = activeLine;
            int chunkSize = Math.max(1, Math.min(3, HudConfig.INSTANCE.sprintWordsChunkSize));
            this.cachedSprintChunks = this.splitLineIntoChunks(activeLine, chunkSize);
            this.lastSpawnedSprintChunk = null;
            this.sprintChunkIndex = 0;
        }
        LyricLine activeChunk = null;
        for (int i = this.cachedSprintChunks.size() - 1; i >= 0; --i) {
            LyricLine chunk = this.cachedSprintChunks.get(i);
            if (currentPosMs < chunk.startMs || currentPosMs >= chunk.endMs) continue;
            activeChunk = chunk;
            break;
        }
        if (activeChunk != null && activeChunk != this.lastSpawnedSprintChunk) {
            this.lastSpawnedSprintChunk = activeChunk;
            this.spawn3DSprintBubble(activeChunk);
        }
    }

    private String reconstructTaggedSubstring(LyricLine line, int startChar, int endChar) {
        if (line == null || line.text == null || line.text.isEmpty()) {
            return "";
        }
        int s = Math.max(0, Math.min(line.text.length(), startChar));
        int e = Math.max(s, Math.min(line.text.length(), endChar));
        StringBuilder sb = new StringBuilder();
        int curColor = -1;
        String curFont = null;
        String curAnim = null;
        for (int idx = s; idx < e; ++idx) {
            int col = line.getRawCharColor(idx);
            String fnt = line.getCharFont(idx);
            String anm = line.getCharAnim(idx);
            if (col != curColor) {
                if (col == -1) {
                    sb.append("</#>");
                } else {
                    sb.append(String.format(Locale.ROOT, "<#%06X>", col & 0xFFFFFF));
                }
                curColor = col;
            }
            if (!Objects.equals(fnt, curFont)) {
                if (fnt == null || fnt.isEmpty()) {
                    sb.append("</font>");
                } else {
                    sb.append("<font:").append(fnt).append(">");
                }
                curFont = fnt;
            }
            if (!Objects.equals(anm, curAnim)) {
                if (anm == null || anm.isEmpty()) {
                    sb.append("</anim>");
                } else {
                    sb.append("<anim:").append(anm.toLowerCase(Locale.ROOT)).append(">");
                }
                curAnim = anm;
            }
            sb.append(line.text.charAt(idx));
        }
        return sb.toString();
    }

    private List<LyricLine> splitLineIntoChunks(LyricLine line, int chunkSize) {
        ArrayList<LyricLine> chunks = new ArrayList<LyricLine>();
        if (line == null || line.text.isEmpty()) {
            return chunks;
        }
        if (line.words != null && !line.words.isEmpty()) {
            List<LyricLine.Word> words = line.words;
            for (int i = 0; i < words.size(); i += chunkSize) {
                int endIdx = Math.min(i + chunkSize, words.size());
                List<LyricLine.Word> subWords = words.subList(i, endIdx);
                StringBuilder taggedSb = new StringBuilder();
                StringBuilder plainSb = new StringBuilder();
                ArrayList<LyricLine.Word> newWordList = new ArrayList<LyricLine.Word>();
                for (int wIdx = 0; wIdx < subWords.size(); ++wIdx) {
                    LyricLine.Word w = subWords.get(wIdx);
                    if (wIdx > 0) {
                        taggedSb.append(" ");
                        plainSb.append(" ");
                    }
                    int charStart = plainSb.length();
                    plainSb.append(w.text());
                    int charEnd = plainSb.length();
                    String taggedWord = (w.charEnd() > w.charStart() && w.charEnd() <= line.text.length())
                        ? this.reconstructTaggedSubstring(line, w.charStart(), w.charEnd())
                        : w.text();
                    taggedSb.append(taggedWord);
                    newWordList.add(new LyricLine.Word(w.startMs(), w.endMs(), w.text(), charStart, charEnd));
                }
                long startMs = subWords.get(0).startMs();
                long vocalEnd = subWords.get(subWords.size() - 1).endMs();
                long chunkVocalDur = Math.max(70L, vocalEnd - startMs);
                long trailLinger = Math.max(160L, Math.min(420L, (long)((float)chunkVocalDur * 1.15f)));
                long endMs = vocalEnd + trailLinger;
                chunks.add(new LyricLine(startMs, endMs, vocalEnd, taggedSb.toString(), newWordList));
            }
        } else {
            String[] tokens = line.text.split("\\s+");
            ArrayList<String> validTokens = new ArrayList<String>();
            ArrayList<int[]> tokenBounds = new ArrayList<int[]>();
            int searchPos = 0;
            for (String t : tokens) {
                if (t.isEmpty()) continue;
                int found = line.text.indexOf(t, searchPos);
                if (found < 0) found = searchPos;
                validTokens.add(t);
                tokenBounds.add(new int[]{found, Math.min(line.text.length(), found + t.length())});
                searchPos = Math.min(line.text.length(), found + t.length());
            }
            if (validTokens.isEmpty()) {
                return chunks;
            }
            long rawDur = line.vocalEndMs - line.startMs;
            long lineDur = Math.max(rawDur, 200L);
            for (int i = 0; i < validTokens.size(); i += chunkSize) {
                int endIdx = Math.min(i + chunkSize, validTokens.size());
                StringBuilder taggedChunk = new StringBuilder();
                for (int tIdx = i; tIdx < endIdx; ++tIdx) {
                    if (tIdx > i) taggedChunk.append(" ");
                    int[] b = tokenBounds.get(tIdx);
                    taggedChunk.append(this.reconstructTaggedSubstring(line, b[0], b[1]));
                }
                long startMs = line.startMs + (long)((double)i / (double)validTokens.size() * (double)lineDur);
                long vocalEnd = line.startMs + (long)((double)endIdx / (double)validTokens.size() * (double)lineDur);
                long chunkVocalDur = Math.max(70L, vocalEnd - startMs);
                long trailLinger = Math.max(160L, Math.min(420L, (long)((float)chunkVocalDur * 1.15f)));
                long endMs = vocalEnd + trailLinger;
                chunks.add(new LyricLine(startMs, endMs, vocalEnd, taggedChunk.toString(), List.of()));
            }
        }
        return chunks;
    }

    private void spawn3DSprintBubble(LyricLine chunk) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1773 == null) {
            return;
        }
        class_4184 camera = mc.field_1773.method_19418();
        if (camera == null) {
            return;
        }
        class_243 cameraPos = camera.method_71156();
        float pitch = camera.method_19329();
        float yaw = camera.method_19330();
        float dist = Math.max(1.5f, Math.min(16.0f, HudConfig.INSTANCE.sprintWordsDistance));
        float heightOffset = Math.max(-1.5f, Math.min(1.5f, HudConfig.INSTANCE.sprintWordsHeight));
        class_243 forward = class_243.method_1030((float)pitch, (float)yaw).method_1029();
        class_243 right = class_243.method_1030((float)0.0f, (float)(yaw + 90.0f)).method_1029();
        class_243 camUp = right.method_1036(forward).method_1029();
        if (camUp.field_1351 < 0.0) {
            camUp = camUp.method_1021(-1.0);
        }
        double speedSq = mc.field_1724.method_18798().method_37268();
        float speedLead = (float)Math.min(3.5, Math.sqrt(speedSq) * 3.0);
        float effectiveDist = Math.max(1.5f, Math.min(20.0f, dist + speedLead));
        float spread = Math.max(0.05f, Math.min(0.6f, HudConfig.INSTANCE.sprintWordsSpread));
        float horizExtent = effectiveDist * spread * 0.5f;
        float vertExtent = effectiveDist * (spread * 0.25f);
        float hOffset = (this.random.nextFloat() - 0.5f) * 2.0f * horizExtent;
        float vOffset = (this.random.nextFloat() - 0.5f) * 2.0f * vertExtent;
        class_243 targetPos = cameraPos.method_1019(forward.method_1021((double)effectiveDist)).method_1019(right.method_1021((double)hOffset)).method_1019(camUp.method_1021((double)(heightOffset + vOffset)));
        if (mc.field_1687 != null) {
            class_3959 rayCtx = new class_3959(cameraPos, targetPos, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, (class_1297)mc.field_1724);
            class_3965 hit = mc.field_1687.method_17742(rayCtx);
            if (hit.method_17783() == class_239.class_240.field_1332) {
                class_243 hitDir = targetPos.method_1020(cameraPos).method_1029();
                double hitDist = hit.method_17784().method_1022(cameraPos);
                double safeDist = Math.max(1.0, hitDist - 0.25);
                targetPos = cameraPos.method_1019(hitDir.method_1021(safeDist));
            }
        }
        class_243 spawnPos = targetPos;
        double playerFeetY = mc.field_1724.method_23318();
        if (spawnPos.field_1351 < playerFeetY + 0.3) {
            spawnPos = new class_243(spawnPos.field_1352, playerFeetY + 0.3 + (double)this.random.nextFloat() * 0.25, spawnPos.field_1350);
        }
        double dx = cameraPos.field_1352 - spawnPos.field_1352;
        double dz = cameraPos.field_1350 - spawnPos.field_1350;
        float lookAngle = (float)Math.atan2(dx, dz);
        Quaternionf rotation = new Quaternionf().rotationY(lookAngle);
        int maxTrail = Math.max(1, Math.min(HudConfig.INSTANCE.sprintWordsTrailLength, 8));
        while (this.activeBubbles.size() >= maxTrail) {
            this.activeBubbles.remove(0);
        }
        this.activeBubbles.add(new SubtitleBubble(chunk, spawnPos, rotation, false, true));
    }

    private void spawnBubble(LyricLine line, int screenWidth, int screenHeight) {
        class_310 mc = class_310.method_1551();
        long now = System.currentTimeMillis();
        boolean physicallySprinting = mc.field_1724 != null && (mc.field_1724.method_5624() || mc.field_1724.method_18798().method_37268() > 0.04);
        if (physicallySprinting) {
            this.lastSprintActiveTime = now;
        }
        boolean isSprinting = physicallySprinting || (now - this.lastSprintActiveTime < 1000L);
        String mode = this.resolveEffectiveMode(this.lastKnownState);
        if ("CHAT".equalsIgnoreCase(mode)) {
            this.postLineToChat(line, this.lastKnownState);
            return;
        }
        if ("WORLD_BLOCK".equalsIgnoreCase(mode) || "WORLD_SURFACE".equalsIgnoreCase(mode)) {
            this.spawnBlockAttachedBubble(line, isSprinting);
        } else if ("WORLD_3D".equalsIgnoreCase(mode)) {
            this.spawn3DWorldBubble(line, isSprinting);
        } else if ("HUD_BOTTOM".equalsIgnoreCase(mode)) {
            this.spawn2DBottomBubble(line, screenWidth, screenHeight, isSprinting);
        } else {
            this.spawn2DRandomBubble(line, screenWidth, screenHeight, isSprinting);
        }
    }

    private void pushNewBubble(SubtitleBubble newBubble) {
        if (newBubble == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean isCurrentMiside = "MISIDE_FALL".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation);
        String mode = this.resolveEffectiveMode(this.lastKnownState);
        boolean isRandomMode = "SCREEN_RANDOM".equalsIgnoreCase(mode) || "HUD_RANDOM".equalsIgnoreCase(mode);
        for (SubtitleBubble b : this.activeBubbles) {
            boolean isBubbleMiside = isCurrentMiside || (b.line != null && b.line.rawText != null && b.line.rawText.contains("<anim:miside>"));
            if (isBubbleMiside || b.worldPos != null || isRandomMode) {
                b.startFadeOut(now);
            } else {
                b.startFadeOutFast(now);
            }
        }
        this.activeBubbles.add(newBubble);
    }

    private void spawnLiveBubble(LyricLine line, int screenWidth, int screenHeight, long dur) {
        class_310 mc = class_310.method_1551();
        long now = System.currentTimeMillis();
        boolean physicallySprinting = mc.field_1724 != null && (mc.field_1724.method_5624() || mc.field_1724.method_18798().method_37268() > 0.04);
        if (physicallySprinting) {
            this.lastSprintActiveTime = now;
        }
        boolean isSprinting = physicallySprinting || (now - this.lastSprintActiveTime < 1000L);
        String mode = this.resolveEffectiveMode(this.lastKnownState);
        if ("CHAT".equalsIgnoreCase(mode)) {
            this.postLineToChat(line, this.lastKnownState);
            return;
        }
        if ("WORLD_BLOCK".equalsIgnoreCase(mode) || "WORLD_SURFACE".equalsIgnoreCase(mode)) {
            SubtitleBubble bubble = this.createBlockBubble(line, isSprinting);
            if (bubble != null) {
                bubble.isLiveCaption = true;
                bubble.liveDurationMs = dur;
                this.pushNewBubble(bubble);
                return;
            }
            this.spawn3DLiveBubble(line, isSprinting, dur);
        } else if ("WORLD_3D".equalsIgnoreCase(mode)) {
            this.spawn3DLiveBubble(line, isSprinting, dur);
        } else if ("HUD_BOTTOM".equalsIgnoreCase(mode)) {
            this.spawn2DBottomLiveBubble(line, screenWidth, screenHeight, isSprinting, dur);
        } else {
            this.spawn2DRandomLiveBubble(line, screenWidth, screenHeight, isSprinting, dur);
        }
    }

    public void spawnTestBubble() {
        List<LyricLine.Word> words = new ArrayList<LyricLine.Word>();
        words.add(new LyricLine.Word(0L, 3000L, "\u266b", 0, 1));
        words.add(new LyricLine.Word(3000L, 7000L, "\u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0435", 2, 10));
        words.add(new LyricLine.Word(7000L, 11000L, "3D", 11, 13));
        words.add(new LyricLine.Word(11000L, 14000L, "\u0421\u0443\u0431\u0442\u0438\u0442\u0440\u044b", 14, 22));
        words.add(new LyricLine.Word(14000L, 15000L, "\u266b", 23, 24));
        LyricLine testLine = new LyricLine(0L, 15000L, "\u266b \u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0435 3D \u0421\u0443\u0431\u0442\u0438\u0442\u0440\u044b \u266b", words);
        class_310 mc = class_310.method_1551();
        int sw = mc != null && mc.method_22683() != null ? mc.method_22683().method_4486() : 854;
        int sh = mc != null && mc.method_22683() != null ? mc.method_22683().method_4502() : 480;
        if (mc == null || mc.field_1724 == null) {
            this.spawn2DBottomLiveBubble(testLine, sw, sh, false, 6000L);
        } else {
            this.spawnLiveBubble(testLine, sw, sh, 15000L);
        }
    }

    private void spawn3DLiveBubble(LyricLine line, boolean isFastPaced, long dur) {
        float baseDist;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1773 == null) {
            return;
        }
        class_4184 camera = mc.field_1773.method_19418();
        if (camera == null) {
            return;
        }
        class_243 cameraPos = camera.method_71156();
        float cfgDist = isFastPaced ? HudConfig.INSTANCE.sprintWordsDistance : HudConfig.INSTANCE.subtitlesDistance;
        float min = baseDist = Math.max(1.5f, Math.min(16.0f, cfgDist));
        float max = baseDist + 1.5f;
        float distance = min + this.random.nextFloat() * (max - min);
        float yawOffset = (this.random.nextFloat() - 0.5f) * (isFastPaced ? 40.0f : 90.0f);
        float pitchOffset = (this.random.nextFloat() - 0.5f) * (isFastPaced ? 20.0f : 40.0f);
        float yaw = camera.method_19330() + yawOffset;
        float pitch = camera.method_19329() + pitchOffset;
        pitch = Math.max(-50.0f, Math.min(50.0f, pitch));
        class_243 direction = class_243.method_1030((float)pitch, (float)yaw).method_1029();
        class_243 spawnPos = cameraPos.method_1019(direction.method_1021((double)distance));
        double playerFeetY = mc.field_1724.method_23318();
        if (spawnPos.field_1351 < playerFeetY + 0.3) {
            spawnPos = new class_243(spawnPos.field_1352, playerFeetY + 0.3 + (double)this.random.nextFloat() * 0.4, spawnPos.field_1350);
        }
        double dx = cameraPos.field_1352 - spawnPos.field_1352;
        double dz = cameraPos.field_1350 - spawnPos.field_1350;
        float lookAngle = (float)Math.atan2(dx, dz);
        org.joml.Quaternionf rotation = new org.joml.Quaternionf().rotationY(lookAngle);
        SubtitleBubble bubble = new SubtitleBubble(line, spawnPos, rotation, false, isFastPaced);
        bubble.isLiveCaption = true;
        bubble.liveDurationMs = dur;
        this.pushNewBubble(bubble);
    }

    private void spawn2DBottomLiveBubble(LyricLine line, int screenWidth, int screenHeight, boolean isFastPaced, long dur) {
        class_310 mc = class_310.method_1551();
        class_327 textRenderer = mc.field_1772;
        int maxAllowedWidth = LyricLine.getMaxAllowedWidth2D(screenWidth);
        String defaultFont = HudConfig.INSTANCE.subtitlesFont;
        List<LyricLine.SubLine> subLines = line.getSubLines(textRenderer, maxAllowedWidth, defaultFont);
        int maxWidth = 0;
        for (LyricLine.SubLine sl : subLines) {
            if (sl.width() <= maxWidth) continue;
            maxWidth = sl.width();
        }
        int lineHeight = 9 + 3;
        int totalHeight = (subLines.size() - 1) * lineHeight + 9;
        double posX = HudConfig.INSTANCE.sub2DPositionX;
        double posY = HudConfig.INSTANCE.sub2DPositionY;
        float x = (float)((double)screenWidth * posX) - (float)maxWidth * 0.5f;
        float y = (float)((double)screenHeight * posY) - (float)totalHeight * 0.5f;
        SubtitleBubble bubble = new SubtitleBubble(line, x, y, isFastPaced);
        bubble.isLiveCaption = true;
        bubble.liveDurationMs = dur;
        this.pushNewBubble(bubble);
    }

    private void spawn2DRandomLiveBubble(LyricLine line, int screenWidth, int screenHeight, boolean isFastPaced, long dur) {
        int paddingX = 80;
        int paddingY = 80;
        int minX = paddingX;
        int maxX = Math.max(minX + 1, screenWidth - paddingX - 160);
        int minY = paddingY;
        int maxY = Math.max(minY + 1, screenHeight - paddingY - 40);
        float x = minX + this.random.nextInt(Math.max(1, maxX - minX));
        float y = minY + this.random.nextInt(Math.max(1, maxY - minY));
        SubtitleBubble bubble = new SubtitleBubble(line, x, y, isFastPaced);
        bubble.isLiveCaption = true;
        bubble.liveDurationMs = dur;
        this.pushNewBubble(bubble);
    }

    private SubtitleBubble createBlockBubble(LyricLine line, boolean isFastPaced) {
        class_243 lookVec;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1773 == null) {
            return null;
        }
        class_4184 camera = mc.field_1773.method_19418();
        if (camera == null) {
            return null;
        }
        class_243 cameraPos = camera.method_71156();
        class_3959 context = new class_3959(cameraPos, cameraPos.method_1019((lookVec = mc.field_1724.method_5828(1.0f)).method_1021(24.0)), class_3959.class_3960.field_17559, class_3959.class_242.field_1348, (class_1297)mc.field_1724);
        class_3965 hit = mc.field_1687.method_17742(context);
        if (hit.method_17783() == class_239.class_240.field_1332) {
            class_2350 side = hit.method_17780();
            class_243 hitPos = hit.method_17784();
            double offsetDistance = 0.065;
            class_243 normal = class_243.method_24954((class_2382)side.method_62675());
            class_243 surfacePos = hitPos.method_1019(normal.method_1021(offsetDistance));
            Quaternionf rotation = new Quaternionf();
            float yaw = mc.field_1724.method_36454();
            switch (side) {
                case field_11036: {
                    rotation.rotateY((float)Math.toRadians(180.0f - yaw));
                    rotation.rotateX((float)Math.toRadians(-90.0));
                    break;
                }
                case field_11033: {
                    rotation.rotateY((float)Math.toRadians(180.0f - yaw));
                    rotation.rotateX((float)Math.toRadians(90.0));
                    break;
                }
                case field_11043: {
                    rotation.rotateY((float)Math.toRadians(180.0));
                    break;
                }
                case field_11035: {
                    rotation.rotateY(0.0f);
                    break;
                }
                case field_11039: {
                    rotation.rotateY((float)Math.toRadians(-90.0));
                    break;
                }
                case field_11034: {
                    rotation.rotateY((float)Math.toRadians(90.0));
                }
            }
            return new SubtitleBubble(line, surfacePos, rotation, true, isFastPaced);
        }
        return null;
    }

    private void spawnBlockAttachedBubble(LyricLine line, boolean isFastPaced) {
        SubtitleBubble bubble = this.createBlockBubble(line, isFastPaced);
        if (bubble != null) {
            this.pushNewBubble(bubble);
        } else {
            this.spawn3DWorldBubble(line, isFastPaced);
        }
    }

    private void tryAttachToBlock(LyricLine line) {
        class_310 mc = class_310.method_1551();
        boolean isSprinting = mc.field_1724 != null && (mc.field_1724.method_5624() || mc.field_1724.method_18798().method_37268() > 0.04);
        SubtitleBubble bubble = this.createBlockBubble(line, isSprinting);
        if (bubble != null) {
            this.pushNewBubble(bubble);
        }
    }

    private boolean hasBlockAttachedBubble() {
        for (SubtitleBubble b : this.activeBubbles) {
            if (!b.isSurfaceAttached) continue;
            return true;
        }
        return false;
    }

    private void spawn3DWorldBubble(LyricLine line, boolean isFastPaced) {
        float baseDist;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1773 == null) {
            return;
        }
        class_4184 camera = mc.field_1773.method_19418();
        if (camera == null) {
            return;
        }
        class_243 cameraPos = camera.method_71156();
        float cfgDist = isFastPaced ? HudConfig.INSTANCE.sprintWordsDistance : HudConfig.INSTANCE.subtitlesDistance;
        float min = baseDist = Math.max(1.5f, Math.min(16.0f, cfgDist));
        float max = baseDist + 1.5f;
        float distance = min + this.random.nextFloat() * (max - min);
        if (isFastPaced) {
            double speedSq = mc.field_1724.method_18798().method_37268();
            distance += (float)Math.min(2.5, Math.sqrt(speedSq) * 3.5);
        }
        float yawOffset = (this.random.nextFloat() - 0.5f) * (isFastPaced ? 40.0f : 90.0f);
        float pitchOffset = (this.random.nextFloat() - 0.5f) * (isFastPaced ? 20.0f : 40.0f);
        float yaw = camera.method_19330() + yawOffset;
        float pitch = camera.method_19329() + pitchOffset;
        pitch = Math.max(-50.0f, Math.min(50.0f, pitch));
        class_243 direction = class_243.method_1030((float)pitch, (float)yaw).method_1029();
        class_243 spawnPos = cameraPos.method_1019(direction.method_1021((double)distance));
        double playerFeetY = mc.field_1724.method_23318();
        if (spawnPos.field_1351 < playerFeetY + 0.3) {
            spawnPos = new class_243(spawnPos.field_1352, playerFeetY + 0.3 + (double)this.random.nextFloat() * 0.4, spawnPos.field_1350);
        }
        double dx = cameraPos.field_1352 - spawnPos.field_1352;
        double dz = cameraPos.field_1350 - spawnPos.field_1350;
        float lookAngle = (float)Math.atan2(dx, dz);
        Quaternionf rotation = new Quaternionf().rotationY(lookAngle);
        this.pushNewBubble(new SubtitleBubble(line, spawnPos, rotation, false, isFastPaced));
    }

    private void spawn2DRandomBubble(LyricLine line, int screenWidth, int screenHeight, boolean isFastPaced) {
        int paddingX = 80;
        int paddingY = 80;
        int minX = paddingX;
        int maxX = Math.max(minX + 1, screenWidth - paddingX - 160);
        int minY = paddingY;
        int maxY = Math.max(minY + 1, screenHeight - paddingY - 40);
        float x = minX + this.random.nextInt(Math.max(1, maxX - minX));
        float y = minY + this.random.nextInt(Math.max(1, maxY - minY));
        this.pushNewBubble(new SubtitleBubble(line, x, y, isFastPaced));
    }

    private void spawn2DBottomBubble(LyricLine line, int screenWidth, int screenHeight, boolean isFastPaced) {
        class_310 mc = class_310.method_1551();
        class_327 textRenderer = mc.field_1772;
        int maxAllowedWidth = LyricLine.getMaxAllowedWidth2D(screenWidth);
        String defaultFont = HudConfig.INSTANCE.subtitlesFont;
        List<LyricLine.SubLine> subLines = line.getSubLines(textRenderer, maxAllowedWidth, defaultFont);
        int maxWidth = 0;
        for (LyricLine.SubLine sl : subLines) {
            if (sl.width() <= maxWidth) continue;
            maxWidth = sl.width();
        }
        Objects.requireNonNull(textRenderer);
        int lineHeight = 9 + 3;
        int totalHeight = (subLines.size() - 1) * lineHeight + 9;
        double posX = HudConfig.INSTANCE.sub2DPositionX;
        double posY = HudConfig.INSTANCE.sub2DPositionY;
        float x = (float)((double)screenWidth * posX) - (float)maxWidth * 0.5f;
        float y = (float)((double)screenHeight * posY) - (float)totalHeight * 0.5f;
        this.pushNewBubble(new SubtitleBubble(line, x, y, isFastPaced));
    }

    private String getTrackKey(TrackState state) {
        return state.artist() + "___" + state.title();
    }

    private void fetchLyricsAsync(TrackState state) {
        String key = this.getTrackKey(state);
        List<LyricLine> existing = this.lyricsCache.get(key);
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        Long lastFail = this.failedFetchTimes.get(key);
        if (lastFail != null && System.currentTimeMillis() - lastFail < 15000L) {
            return;
        }
        long fetchId = this.currentFetchId.incrementAndGet();
        this.activeFetchTrackKey = key;
        CompletableFuture.runAsync(() -> {
            try {
                boolean mismatch;
                if (fetchId != this.currentFetchId.get()) {
                    return;
                }
                String rawTitle = state.title();
                String rawArtist = state.artist();
                long durationSec = state.durationMs() / 1000L;
                boolean isSpeedUp = SPEED_UP_PATTERN.matcher(rawTitle).find() || SPEED_UP_PATTERN.matcher(rawArtist).find();
                boolean isSlowed = SLOWED_PATTERN.matcher(rawTitle).find() || SLOWED_PATTERN.matcher(rawArtist).find();
                String explicitTrackId = state.getExplicitTrackId();
                FetchResult result = null;
                FetchResult plainFallback = null;
                boolean fromExplicitId = false;
                if (explicitTrackId != null && !explicitTrackId.isEmpty()) {
                    long effDur = durationSec > 0L ? durationSec : this.lastFetchedDurationMs / 1000L;
                    FetchResult yRes = this.queryYandexMusicById(explicitTrackId, effDur, fetchId);
                    if (yRes != null && yRes.lines() != null && !yRes.lines().isEmpty()) {
                        if (yRes.isRealSync()) {
                            result = yRes;
                            fromExplicitId = true;
                        } else {
                            plainFallback = yRes;
                            this.lyricsCache.putIfAbsent(key, yRes.lines());
                        }
                    }
                }
                if (result == null || result.lines() == null || result.lines().isEmpty()) {
                    long effDur = durationSec > 0L ? durationSec : this.lastFetchedDurationMs / 1000L;
                    result = this.searchMultiProviderLyrics(rawTitle, rawArtist, effDur, fetchId, key, isSpeedUp, isSlowed);
                }
                if ((result == null || result.lines() == null || result.lines().isEmpty()) && plainFallback != null) {
                    result = plainFallback;
                    fromExplicitId = true;
                }
                if (fetchId != this.currentFetchId.get() || result == null) {
                    return;
                }
                List<LyricLine> lines = result.lines() != null ? result.lines() : Collections.emptyList();
                long finalDurSec = durationSec > 0L ? durationSec : this.lastFetchedDurationMs / 1000L;
                if (!lines.isEmpty() && fromExplicitId && (isSpeedUp || isSlowed)) {
                    lines = this.applySpeedAdjustment(lines, finalDurSec, isSpeedUp, isSlowed);
                }
                boolean bl = mismatch = result != null && result.possibleMismatch() || this.isLyricsMismatched(lines, state);
                if (!lines.isEmpty()) {
                    this.lyricsCache.put(key, lines);
                    if (result.isRealSync()) {
                        this.realSyncedTracks.add(key);
                    }
                    this.failedFetchTimes.remove(key);
                    if (mismatch) {
                        this.mismatchedTracks.add(key);
                        if (!this.promptedMismatchTracks.contains(key)) {
                            this.promptedMismatchTracks.add(key);
                            this.sendMismatchNotification();
                        }
                    }
                } else {
                    this.failedFetchTimes.put(key, System.currentTimeMillis());
                    this.lyricsCache.remove(key);
                    this.realSyncedTracks.remove(key);
                }
            }
            catch (Exception e) {
                if (fetchId == this.currentFetchId.get()) {
                    this.failedFetchTimes.put(key, System.currentTimeMillis());
                    this.lyricsCache.remove(key);
                }
            }
            finally {
                if (fetchId == this.currentFetchId.get()) {
                    this.activeFetchTrackKey = null;
                }
            }
        }, this.executor);
    }

    private void sendMismatchNotification() {
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            if (mc.field_1705 != null && mc.field_1705.method_1743() != null) {
                class_5250 baseMsg = class_2561.method_43470("§6[Музыка] §fКажется, текст песни и субтитры расходятся. ");
                class_5250 cancelBtn = class_2561.method_43470("§c§n[Отменить]§r").method_27694(style -> style.method_10977(class_124.field_1061).method_10982(Boolean.valueOf(true)).method_30938(Boolean.valueOf(true)).method_10958((class_2558)new class_2558.class_10609("/musicsubtitles cancel")).method_10949((class_2568)new class_2568.class_10613((class_2561)class_2561.method_43470("§eНажмите, чтобы отключить субтитры для этой песни"))));
                baseMsg.method_10852((class_2561)cancelBtn);
                mc.field_1705.method_1743().method_1812((class_2561)baseMsg);
            }
        });
    }

    public List<LyricLine> getCurrentTrackLyrics() {
        if (this.currentTrackKey == null) {
            return null;
        }
        return this.lyricsCache.get(this.currentTrackKey);
    }

    public boolean hasLyricsForCurrentTrack() {
        if (this.currentTrackKey == null) {
            return false;
        }
        List<LyricLine> lines = this.lyricsCache.get(this.currentTrackKey);
        return lines != null && !lines.isEmpty();
    }

    public void updateLyricsForCurrentTrack(String artist, String title, List<LyricLine> lines, String lrcContent) {
        boolean persistToDisk = lrcContent != null && !lrcContent.isBlank();
        if (this.currentTrackKey != null) {
            if (persistToDisk) {
                this.userSavedTracks.add(this.currentTrackKey);
            }
            this.mismatchedTracks.remove(this.currentTrackKey);
            this.disabledTracks.remove(this.currentTrackKey);
            this.promptedMismatchTracks.remove(this.currentTrackKey);
        }
        if (artist != null && title != null) {
            String key = artist + "___" + title;
            if (persistToDisk) {
                this.userSavedTracks.add(key);
            }
            this.mismatchedTracks.remove(key);
            this.disabledTracks.remove(key);
            this.promptedMismatchTracks.remove(key);
        }
        this.acousticMismatchStrikes.set(0);
        if (lines != null && !lines.isEmpty()) {
            if (this.currentTrackKey != null) {
                this.lyricsCache.put(this.currentTrackKey, lines);
            }
            if (artist != null && title != null) {
                String key = artist + "___" + title;
                this.lyricsCache.put(key, lines);
            }
            this.activeBubbles.clear();
            this.lastSpawnedLine = null;
            this.lastSpawnedSprintChunk = null;
            this.cachedSprintLine = null;
            this.cachedSprintChunks.clear();
        }
        if (lrcContent != null && !lrcContent.isBlank() && artist != null && title != null) {
            this.saveLyricsToDisk(artist, title, lrcContent);
        }
    }

    public void pushLiveCaption(String text, long durationMs) {
        this.pushLiveCaptionWithWords(text, durationMs, null);
    }

    public void pushLiveCaptionWithWords(String text, long durationMs, List<LyricLine.Word> words) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        long dur = durationMs > 0L ? durationMs : 3500L;
        LyricLine line = new LyricLine(0L, dur, text.trim(), words);
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.method_22683() != null) {
            int sw = mc.method_22683().method_4486();
            int sh = mc.method_22683().method_4502();
            mc.execute(() -> this.spawnLiveBubble(line, sw, sh, dur));
        }
    }

    public void pushTranscript(String videoTitle, List<LyricLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        if (videoTitle != null && !videoTitle.isEmpty()) {
            this.lyricsCache.put(videoTitle, lines);
        }
        if (this.currentTrackKey != null && !this.currentTrackKey.isEmpty()) {
            this.lyricsCache.put(this.currentTrackKey, lines);
        }
        System.out.println("[MusicSubtitles] Received live transcript with " + lines.size() + " lines for: " + videoTitle);
    }

    public long getLiveAcousticOffsetMs() {
        return Math.round(this.smoothAcousticOffset);
    }

    private void triggerLiveAcousticSupervisorIfNeeded(TrackState state, List<LyricLine> lines) {
        if (!HudConfig.INSTANCE.audioAnalysisEnabled || this.acousticSupervisorBusy || state == null || !state.isPlaying()) {
            return;
        }
        if (lines == null || lines.isEmpty() || this.acousticPassesDone >= 4) {
            return;
        }
        String trackKey = this.getTrackKey(state);
        boolean isRealSync = this.realSyncedTracks.contains(trackKey);
        boolean isMismatched = this.mismatchedTracks.contains(trackKey);
        if (isRealSync && !isMismatched) {
            return;
        }
        long now = System.currentTimeMillis();
        long rawPos = state.getInterpolatedPositionMs();
        if (rawPos < 3500L || now - this.lastAcousticCaptureTime < 11000L) {
            return;
        }
        this.checkLiveAcousticMatchAsync(state, lines, this.currentTrackKey);
    }

    private void checkLiveAcousticMatchAsync(TrackState state, List<LyricLine> lines, String trackKey) {
        if (this.acousticSupervisorBusy) {
            return;
        }
        this.acousticSupervisorBusy = true;
        this.lastAcousticCaptureTime = System.currentTimeMillis();
        CompletableFuture.runAsync(() -> {
            try {
                if (trackKey == null || !trackKey.equals(this.currentTrackKey) || !state.isPlaying()) {
                    return;
                }
                int port = this.resolveHelperPort();
                if (port <= 0) {
                    return;
                }
                String helperBase = "http://127.0.0.1:" + port;
                int captureDurMs = 3500;
                String startResp = LyricsManager.httpPost(helperBase + "/capture/start", "{\"durationMs\":" + captureDurMs + "}", 2000);
                if (startResp == null) {
                    return;
                }
                long captureStartPos = state.getInterpolatedPositionMs();
                for (int elapsed = 0; elapsed < captureDurMs + 350; elapsed += 150) {
                    if (trackKey == null || !trackKey.equals(this.currentTrackKey) || !state.isPlaying()) {
                        LyricsManager.httpPost(helperBase + "/capture/stop", "", 1000);
                        return;
                    }
                    Thread.sleep(150L);
                }
                if (trackKey == null || !trackKey.equals(this.currentTrackKey) || !state.isPlaying()) {
                    return;
                }
                byte[] wav = LyricsManager.httpGetBytes(helperBase + "/capture/result", 3000);
                if (wav == null || wav.length <= 44) {
                    return;
                }
                AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wav);
                int minSr = pcm.sampleRate() > 0 ? pcm.sampleRate() * 2 : 32000;
                if (pcm.samples() == null || pcm.samples().length < minSr) {
                    return;
                }
                double tauStartMs = (double)captureStartPos + 180.0;
                boolean isRealSync = this.realSyncedTracks.contains(trackKey);
                int maxLagMs = isRealSync ? 1800 : 14000;
                double winStart = tauStartMs - (double)maxLagMs;
                double winEnd = tauStartMs + (double)captureDurMs + (double)maxLagMs;
                int trackOffset = HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title());
                double effShift = (double)HudConfig.INSTANCE.subtitlesOffsetMs + (double)trackOffset + this.smoothAcousticOffset + (double)PERCEPTUAL_LEAD_MS;
                List<Double> evList = new ArrayList<Double>();
                List<Double> wtList = new ArrayList<Double>();
                List<double[]> ivList = new ArrayList<double[]>();
                for (LyricLine line : lines) {
                    if (line == null) continue;
                    double ls = (double)line.startMs - effShift;
                    double le = (double)line.endMs - effShift;
                    if (le < winStart || ls > winEnd) continue;
                    evList.add(ls);
                    wtList.add(1.0);
                    if (line.words != null && !line.words.isEmpty()) {
                        for (LyricLine.Word w : line.words) {
                            double ws = (double)w.startMs() - effShift;
                            double we = (double)w.endMs() - effShift;
                            evList.add(ws);
                            wtList.add(0.35);
                            ivList.add(new double[]{ws, we});
                        }
                    } else {
                        ivList.add(new double[]{ls, (double)line.vocalEndMs - effShift});
                    }
                }
                if (evList.isEmpty()) {
                    return;
                }
                double[] eventTimes = new double[evList.size()];
                double[] eventWeights = new double[wtList.size()];
                for (int k = 0; k < evList.size(); ++k) {
                    eventTimes[k] = evList.get(k);
                    eventWeights[k] = wtList.get(k);
                }
                double[][] intervals = ivList.toArray(new double[0][]);
                AcousticLagEstimator.Estimate est = AcousticLagEstimator.estimate(pcm.samples(), pcm.sampleRate(), eventTimes, eventWeights, intervals, tauStartMs, maxLagMs);
                if (est.reliable()) {
                    double lag = est.lagMs();
                    double candidateTotalCorrection = (double)trackOffset + this.smoothAcousticOffset + lag;
                    boolean commitConsistent = false;
                    synchronized (this.reliableLagSamples) {
                        this.reliableLagSamples.add(candidateTotalCorrection);
                        if (Math.abs(lag) >= 60.0) {
                            double applyFactor = isRealSync ? 0.35 : 0.85;
                            this.smoothAcousticOffset += lag * applyFactor;
                        }
                        if (this.reliableLagSamples.size() >= 2) {
                            double c1 = this.reliableLagSamples.get(this.reliableLagSamples.size() - 2);
                            double c2 = this.reliableLagSamples.get(this.reliableLagSamples.size() - 1);
                            commitConsistent = Math.abs(c1 - c2) <= 220.0;
                        }
                    }
                    if (commitConsistent) {
                        int finalCorrection = (int)Math.round(candidateTotalCorrection);
                        if (Math.abs(finalCorrection) >= 60 && Math.abs(finalCorrection) <= 30000) {
                            HudConfig.INSTANCE.setTrackSyncOffset(state.artist(), state.title(), finalCorrection);
                            this.smoothAcousticOffset = 0.0;
                            this.realSyncedTracks.add(trackKey);
                        }
                    }
                }
                this.acousticPassesDone++;
            } catch (Exception ignored) {
            } finally {
                this.acousticSupervisorBusy = false;
            }
        }, this.executor);
    }

    private int resolveHelperPort() {
        if (HudConfig.INSTANCE.helperPort > 0) {
            return HudConfig.INSTANCE.helperPort;
        }
        try {
            Path cfgPort = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("yandexmusichud_port.txt");
            if (Files.exists(cfgPort, new LinkOption[0])) {
                int p = Integer.parseInt(Files.readString(cfgPort).trim());
                if (p > 0) {
                    return p;
                }
            }
        } catch (Exception ignored) {}
        try {
            Path portFile;
            String temp = System.getenv("TEMP");
            if (temp == null) {
                temp = System.getProperty("java.io.tmpdir");
            }
            if (Files.exists(portFile = Path.of(temp, new String[0]).resolve("yandexmusichud_port.txt"), new LinkOption[0])) {
                return Integer.parseInt(Files.readString(portFile).trim());
            }
        }
        catch (Exception exception) {

        }
        return 38472;
    }

    private FetchResult searchMultiProviderLyrics(String rawTitle, String rawArtist, long durationSec, long fetchId) {
        return this.searchMultiProviderLyrics(rawTitle, rawArtist, durationSec, fetchId, null, false, false);
    }

    private FetchResult searchMultiProviderLyrics(String rawTitle, String rawArtist, long durationSec, long fetchId, String cacheKey, boolean initialSpeedUp, boolean initialSlowed) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        boolean isSpeedUp = initialSpeedUp || SPEED_UP_PATTERN.matcher(rawTitle).find() || SPEED_UP_PATTERN.matcher(rawArtist).find();
        boolean isSlowed = initialSlowed || SLOWED_PATTERN.matcher(rawTitle).find() || SLOWED_PATTERN.matcher(rawArtist).find();
        List<LyricLine> local = this.checkLocalLyrics(rawTitle, rawArtist);
        if (local != null && !local.isEmpty()) {
            if (isSpeedUp || isSlowed) {
                return new FetchResult(this.applySpeedAdjustment(local, durationSec, isSpeedUp, isSlowed), false, true);
            }
            return new FetchResult(local, false, true);
        }
        List<LyricLine> pushed = this.lyricsCache.get(rawTitle);
        if (pushed == null && !rawTitle.isEmpty()) {
            pushed = this.lyricsCache.get(this.cleanForSearch(rawTitle));
        }
        if (pushed != null && !pushed.isEmpty()) {
            return new FetchResult(pushed, false, true);
        }
        com.example.yandexmusichud.network.TrackMetadataSanitizer.SanitizedMetadata meta = com.example.yandexmusichud.network.TrackMetadataSanitizer.sanitize(rawTitle, rawArtist, "");
        String targetTitle = !meta.searchTitle().isEmpty() ? meta.searchTitle() : this.cleanForSearch(rawTitle);
        String targetArtist = !meta.searchArtist().isEmpty() ? meta.searchArtist() : this.cleanForSearch(rawArtist);
        String normTitle = this.normalizeTrackTitle(targetTitle);
        String primaryArtist = !meta.primaryArtist().isEmpty() ? meta.primaryArtist() : this.extractPrimaryArtist(targetArtist);
        if (meta.isSpeedUp()) isSpeedUp = true;
        if (meta.isSlowed()) isSlowed = true;
        final boolean finalSpeedUp = isSpeedUp;
        final boolean finalSlowed = isSlowed;
        long queryDurationSec = isSpeedUp || isSlowed ? 0L : durationSec;
        ArrayList<CompletableFuture<LyricsConsensusEngine.Candidate>> futures = new ArrayList<CompletableFuture<LyricsConsensusEngine.Candidate>>();
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            LyricsConsensusEngine.Candidate r = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(targetTitle, targetArtist, queryDurationSec, fetchId));
            if (r == null && !normTitle.equalsIgnoreCase(targetTitle)) {
                r = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(normTitle, targetArtist, queryDurationSec, fetchId));
            }
            if (r == null && !primaryArtist.isEmpty() && !primaryArtist.equals(targetArtist)) {
                r = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(targetTitle, primaryArtist, queryDurationSec, fetchId));
            }
            if (r == null && !rawTitle.equalsIgnoreCase(targetTitle)) {
                r = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(rawTitle, rawArtist, queryDurationSec, fetchId));
            }
            if (r == null && targetArtist.isEmpty() && !normTitle.isEmpty()) {
                r = this.wrapSyncedResult("YANDEX_SYNCED", this.queryYandexMusic(normTitle, "", queryDurationSec, fetchId));
            }
            return r;
        }, this.executor));
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            LyricsConsensusEngine.Candidate r = this.queryGeniusLyrics(targetTitle, targetArtist, fetchId);
            if (r == null && !primaryArtist.isEmpty() && !primaryArtist.equals(targetArtist)) {
                r = this.queryGeniusLyrics(targetTitle, primaryArtist, fetchId);
            }
            return r;
        }, this.executor));
        if (!targetTitle.isEmpty() && !targetArtist.isEmpty()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                if (fetchId != this.currentFetchId.get()) {
                    return null;
                }
                LyricsConsensusEngine.Candidate r = this.queryLrclibGetCandidate(targetTitle, targetArtist, queryDurationSec, fetchId);
                if (r == null && !primaryArtist.isEmpty() && !primaryArtist.equals(targetArtist)) {
                    r = this.queryLrclibGetCandidate(targetTitle, primaryArtist, queryDurationSec, fetchId);
                }
                return r;
            }, this.executor));
        }
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            LyricsConsensusEngine.Candidate r = null;
            if (!targetTitle.isEmpty() && !targetArtist.isEmpty()) {
                r = this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(targetArtist + " " + targetTitle, queryDurationSec, targetTitle, targetArtist, fetchId));
                if (r == null && !primaryArtist.isEmpty() && !primaryArtist.equals(targetArtist)) {
                    r = this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(primaryArtist + " " + targetTitle, queryDurationSec, targetTitle, primaryArtist, fetchId));
                }
            }
            if (r == null && !targetTitle.isEmpty()) {
                r = this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(targetTitle, queryDurationSec, targetTitle, targetArtist, fetchId));
            }
            if (r == null && !normTitle.isEmpty() && !normTitle.equalsIgnoreCase(targetTitle)) {
                r = this.wrapSyncedResult("LRCLIB_SYNCED", this.queryLrclibSearchSynced(normTitle, queryDurationSec, normTitle, targetArtist, fetchId));
            }
            return r;
        }, this.executor));
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            LyricsConsensusEngine.Candidate r = null;
            if (!targetTitle.isEmpty() && !targetArtist.isEmpty()) {
                r = this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(targetArtist + " " + targetTitle, targetTitle, targetArtist, queryDurationSec, fetchId));
                if (r == null && !primaryArtist.isEmpty() && !primaryArtist.equals(targetArtist)) {
                    r = this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(primaryArtist + " " + targetTitle, targetTitle, primaryArtist, queryDurationSec, fetchId));
                }
            }
            if (r == null && !targetTitle.isEmpty() && targetArtist.isEmpty()) {
                r = this.wrapSyncedResult("NETEASE", this.queryNeteaseLyrics(targetTitle, targetTitle, "", queryDurationSec, fetchId));
            }
            return r;
        }, this.executor));
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            return this.queryLrclibSearchPlainCandidate((targetArtist + " " + targetTitle).trim(), queryDurationSec, targetTitle, targetArtist, fetchId);
        }, this.executor));
        futures.add(CompletableFuture.supplyAsync(() -> {
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            return this.queryLyricsOvhCandidate(targetArtist, targetTitle, fetchId);
        }, this.executor));
        List<LyricsConsensusEngine.Candidate> candidatePool = Collections.synchronizedList(new ArrayList());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger pending = new AtomicInteger(futures.size());
        AtomicInteger syncedArrivals = new AtomicInteger(0);
        for (CompletableFuture<LyricsConsensusEngine.Candidate> completableFuture : futures) {
            completableFuture.thenAccept((LyricsConsensusEngine.Candidate cand) -> {
                if (cand != null) {
                    candidatePool.add(cand);
                    if (cand.isSynced && !cand.possibleMismatch && cand.lines != null && !cand.lines.isEmpty() && !"NETEASE".equalsIgnoreCase(cand.provider)) {
                        if (cacheKey != null && fetchId == this.currentFetchId.get() && !this.realSyncedTracks.contains(cacheKey)) {
                            long effDur = durationSec > 0L ? durationSec : this.lastFetchedDurationMs / 1000L;
                            List<LyricLine> quickLines = finalSpeedUp || finalSlowed ? this.applySpeedAdjustment(cand.lines, effDur, finalSpeedUp, finalSlowed) : cand.lines;
                            this.lyricsCache.put(cacheKey, quickLines);
                            this.realSyncedTracks.add(cacheKey);
                        }
                        if (syncedArrivals.incrementAndGet() == 1) {
                            CompletableFuture.delayedExecutor(650L, TimeUnit.MILLISECONDS, this.executor).execute(latch::countDown);
                        }
                    }
                    if (cand.isSynced && !"NETEASE".equalsIgnoreCase(cand.provider)) {
                        for (LyricsConsensusEngine.Candidate other : candidatePool) {
                            if (cand == other || cand.provider.equalsIgnoreCase(other.provider) || !"GENIUS".equalsIgnoreCase(other.provider) && !other.isSynced || !(LyricsConsensusEngine.computePhraseOverlap(cand.phrases, other.phrases) >= 0.55)) continue;
                            latch.countDown();
                            break;
                        }
                    }
                }
                if (pending.decrementAndGet() == 0) {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(4000L, TimeUnit.MILLISECONDS);
        }
        catch (Exception exception) {

        }
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        long effectiveDurSec = durationSec > 0L ? durationSec : this.lastFetchedDurationMs / 1000L;
        LyricsConsensusEngine.Candidate winner = LyricsConsensusEngine.selectBest(candidatePool, effectiveDurSec, targetTitle, targetArtist);
        if (winner == null) {
            return new FetchResult(Collections.emptyList(), false);
        }
        FetchResult result;
        if (winner.isSynced && !winner.lines.isEmpty()) {
            result = new FetchResult(winner.lines, winner.possibleMismatch, true);
        } else if (!winner.rawText.isBlank()) {
            long targetDurMs = effectiveDurSec > 0L ? effectiveDurSec * 1000L : 180000L;
            result = this.parsePlainLyrics(winner.rawText, targetDurMs, fetchId);
        } else {
            result = new FetchResult(Collections.emptyList(), false);
        }
        if (result != null && result.lines() != null && !result.lines().isEmpty()) {
            List<LyricLine> lines = result.lines();
            if (isSpeedUp || isSlowed) {
                lines = this.applySpeedAdjustment(lines, effectiveDurSec, isSpeedUp, isSlowed);
            }
            return new FetchResult(lines, result.possibleMismatch(), result.isRealSync());
        }
        return new FetchResult(Collections.emptyList(), false);
    }

    private List<LyricLine> applySpeedAdjustment(List<LyricLine> lines, long actualDurationSec, boolean isSpeedUp, boolean isSlowed) {
        if (lines == null || lines.isEmpty()) {
            return lines;
        }
        double factor;
        long targetDurationMs = actualDurationSec > 0L ? actualDurationSec * 1000L : 0L;
        long origLastMs = lines.get(lines.size() - 1).endMs;
        if (targetDurationMs > 30000L && origLastMs > 30000L) {
            double ratio = (double)targetDurationMs / (double)origLastMs;
            if (Math.abs(ratio - 1.0) < 0.05) {
                return lines;
            }
            if (ratio >= 0.5 && ratio <= 0.94) {
                factor = ratio;
            } else if (ratio >= 1.06 && ratio <= 1.85) {
                factor = ratio;
            } else if (isSpeedUp) {
                factor = 0.75;
            } else if (isSlowed) {
                factor = 1.3;
            } else {
                return lines;
            }
        } else if (isSpeedUp) {
            factor = 0.75;
        } else if (isSlowed) {
            factor = 1.3;
        } else {
            return lines;
        }
        ArrayList<LyricLine> scaled = new ArrayList<LyricLine>(lines.size());
        for (LyricLine l2 : lines) {
            scaled.add(l2.scaled(factor));
        }
        return scaled;
    }

    private List<LyricLine> checkLocalLyrics(String title, String artist) {
        try {
            com.example.yandexmusichud.local.LocalMusicTrack curTrack = com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().getCurrentTrack();
            if (curTrack != null && curTrack.getLrcPath() != null && Files.exists(curTrack.getLrcPath())) {
                String c = Files.readString(curTrack.getLrcPath(), StandardCharsets.UTF_8);
                List<LyricLine> p = this.parseLrc(c);
                if (p != null && !p.isEmpty()) {
                    return p;
                }
            }
            if (!Files.exists(LOCAL_LYRICS_DIR, new LinkOption[0])) {
                return null;
            }
            File[] files = LOCAL_LYRICS_DIR.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".lrc"));
            if (files == null || files.length == 0) {
                return null;
            }
            String normTitle = LyricsManager.normalizeForComparison(title);
            String normArtist = LyricsManager.normalizeForComparison(artist);
            if (normTitle.isEmpty()) {
                return null;
            }
            String exactTarget = !normArtist.isEmpty() ? LyricsManager.normalizeForComparison(artist + " " + title) : normTitle;
            for (File f : files) {
                String content;
                List<LyricLine> parsed;
                String fname = f.getName().toLowerCase().replace(".lrc", "");
                String normFile = LyricsManager.normalizeForComparison(fname);
                boolean exactMatch = normFile.equals(exactTarget);
                boolean bothPresent = !normArtist.isEmpty() && normTitle.length() >= 4 && normFile.contains(normArtist) && normFile.contains(normTitle);
                if (!exactMatch && !bothPresent) continue;
                if ((parsed = this.parseLrc(content = Files.readString(f.toPath(), StandardCharsets.UTF_8))).isEmpty()) continue;
                return parsed;
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    public void saveLyricsToDisk(String artist, String title, String lrcContent) {
        if (lrcContent == null || lrcContent.isBlank()) {
            return;
        }
        try {
            if (!Files.exists(LOCAL_LYRICS_DIR, new LinkOption[0])) {
                Files.createDirectories(LOCAL_LYRICS_DIR);
            }
            String safeName = (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_").trim() + ".lrc";
            Path target = LOCAL_LYRICS_DIR.resolve(safeName);
            Files.writeString(target, lrcContent, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    private static String httpGet(String urlStr, Map<String, String> headers, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream is = conn.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    private static final String YANDEX_SIGN_KEY = "p93jhgh689SBReK6ghtw62";

    private String getYandexSign(String trackId, long timestamp) {
        if (trackId == null || trackId.isEmpty()) {
            return "";
        }
        int colonIdx = trackId.indexOf(':');
        String cleanId = colonIdx >= 0 ? trackId.substring(0, colonIdx) : trackId;
        try {
            String msg = cleanId + timestamp;
            String signKey = HudConfig.INSTANCE != null && HudConfig.INSTANCE.yandexSignKey != null && !HudConfig.INSTANCE.yandexSignKey.isBlank()
                ? HudConfig.INSTANCE.yandexSignKey : YANDEX_SIGN_KEY;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(signKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }

    private FetchResult fetchYandexLrcLyrics(String trackId, String artist, String title, long fetchId, boolean possibleMismatch) {
        if (trackId == null || trackId.isBlank()) {
            return null;
        }
        String cleanToken = com.example.yandexmusichud.hud.YandexMusicSettingsScreen.sanitizeToken(HudConfig.INSTANCE.yandexMusicToken);
        if (cleanToken.isEmpty()) {
            return null;
        }
        int colonIdx = trackId.indexOf(':');
        String cleanId = colonIdx >= 0 ? trackId.substring(0, colonIdx) : trackId;
        long ts = System.currentTimeMillis() / 1000L;
        String sign = this.getYandexSign(cleanId, ts);
        if (sign.isEmpty()) {
            return null;
        }
        try {
            String encSign = URLEncoder.encode(sign, StandardCharsets.UTF_8);
            String lyricsUrl = "https://api.music.yandex.net/tracks/" + cleanId + "/lyrics?format=LRC&timeStamp=" + ts + "&sign=" + encSign;
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Yandex-Music-API");
            headers.put("X-Yandex-Music-Client", "YandexMusicAndroid/24023621");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "OAuth " + cleanToken);
            String resp = LyricsManager.httpGet(lyricsUrl, headers, 3000);
            if (resp == null || fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
            if (json.has("result") && !json.get("result").isJsonNull()) {
                JsonObject resObj = json.getAsJsonObject("result");
                if (resObj.has("downloadUrl") && !resObj.get("downloadUrl").isJsonNull()) {
                    String downloadUrl = resObj.get("downloadUrl").getAsString();
                    String lrcContent = LyricsManager.httpGet(downloadUrl, Collections.emptyMap(), 4000);
                    if (lrcContent != null && !lrcContent.isEmpty() && fetchId == this.currentFetchId.get()) {
                        List<LyricLine> lines = this.parseLrc(lrcContent);
                        if (!lines.isEmpty()) {
                            return new FetchResult(lines, possibleMismatch, true);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private FetchResult fetchYandexTextLyrics(String trackId, long durationMs, long fetchId, boolean possibleMismatch) {
        if (trackId == null || trackId.isBlank()) {
            return null;
        }
        String cleanToken = com.example.yandexmusichud.hud.YandexMusicSettingsScreen.sanitizeToken(HudConfig.INSTANCE.yandexMusicToken);
        if (cleanToken.isEmpty()) {
            return null;
        }
        int colonIdx = trackId.indexOf(':');
        String cleanId = colonIdx >= 0 ? trackId.substring(0, colonIdx) : trackId;
        long ts = System.currentTimeMillis() / 1000L;
        String sign = this.getYandexSign(cleanId, ts);
        if (sign.isEmpty()) {
            return null;
        }
        try {
            String encSign = URLEncoder.encode(sign, StandardCharsets.UTF_8);
            String lyricsUrl = "https://api.music.yandex.net/tracks/" + cleanId + "/lyrics?format=TEXT&timeStamp=" + ts + "&sign=" + encSign;
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Yandex-Music-API");
            headers.put("X-Yandex-Music-Client", "YandexMusicAndroid/24023621");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "OAuth " + cleanToken);
            String resp = LyricsManager.httpGet(lyricsUrl, headers, 3000);
            if (resp == null || fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
            if (json.has("result") && !json.get("result").isJsonNull()) {
                JsonObject resObj = json.getAsJsonObject("result");
                if (resObj.has("downloadUrl") && !resObj.get("downloadUrl").isJsonNull()) {
                    String downloadUrl = resObj.get("downloadUrl").getAsString();
                    String textContent = LyricsManager.httpGet(downloadUrl, Collections.emptyMap(), 4000);
                    if (textContent != null && !textContent.isEmpty() && fetchId == this.currentFetchId.get()) {
                        long targetDur = durationMs > 0L ? durationMs : 180000L;
                        FetchResult pr = this.parsePlainLyrics(textContent, targetDur, fetchId);
                        if (pr != null && pr.lines() != null && !pr.lines().isEmpty()) {
                            return new FetchResult(pr.lines(), possibleMismatch, false);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private FetchResult queryYandexMusic(String trackName, String artistName, long durationSec, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            String searchResp;
            String query = (artistName + " " + trackName).trim();
            String searchUrl = "https://api.music.yandex.net/search?text=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=track&page=0";
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Yandex-Music-API");
            headers.put("X-Yandex-Music-Client", "YandexMusicAndroid/24023621");
            headers.put("Accept", "application/json");
            String cleanToken = com.example.yandexmusichud.hud.YandexMusicSettingsScreen.sanitizeToken(HudConfig.INSTANCE.yandexMusicToken);
            if (!cleanToken.isEmpty()) {
                headers.put("Authorization", "OAuth " + cleanToken);
            }
            if ((searchResp = LyricsManager.httpGet(searchUrl, headers, 2500)) == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject searchJson = JsonParser.parseString((String)searchResp).getAsJsonObject();
            if (!searchJson.has("result")) {
                return null;
            }
            JsonObject resultObj = searchJson.getAsJsonObject("result");
            if (!resultObj.has("tracks")) {
                return null;
            }
            JsonObject tracksObj = resultObj.getAsJsonObject("tracks");
            if (!tracksObj.has("results")) {
                return null;
            }
            JsonArray results = tracksObj.getAsJsonArray("results");
            if (results.isEmpty()) {
                return null;
            }
            int limit = Math.min(results.size(), 6);
            for (int idx = 0; idx < limit; ++idx) {
                if (fetchId != this.currentFetchId.get()) {
                    return null;
                }
                JsonElement elem = results.get(idx);
                if (!elem.isJsonObject()) continue;
                JsonObject trackObj = elem.getAsJsonObject();
                if (idx > 0) {
                    boolean hasLyrics = false;
                    if (trackObj.has("lyricsInfo") && !trackObj.get("lyricsInfo").isJsonNull()) {
                        JsonObject li = trackObj.getAsJsonObject("lyricsInfo");
                        if ((li.has("hasAvailableSyncLyrics") && li.get("hasAvailableSyncLyrics").getAsBoolean()) ||
                            (li.has("hasAvailableTextLyrics") && li.get("hasAvailableTextLyrics").getAsBoolean())) {
                            hasLyrics = true;
                        }
                    }
                    if (!hasLyrics && trackObj.has("lyricsAvailable") && trackObj.get("lyricsAvailable").getAsBoolean()) {
                        hasLyrics = true;
                    }
                    if (!hasLyrics) {
                        continue;
                    }
                }
                String foundTitle = trackObj.has("title") && !trackObj.get("title").isJsonNull() ? trackObj.get("title").getAsString() : "";
                String foundArtist = "";
                double bestArtistSim = 0.0;
                StringBuilder allArtistsSb = new StringBuilder();
                if (trackObj.has("artists") && !trackObj.get("artists").isJsonNull()) {
                    JsonArray artistsArr = trackObj.getAsJsonArray("artists");
                    for (int aIdx = 0; aIdx < artistsArr.size(); ++aIdx) {
                        if (!artistsArr.get(aIdx).isJsonObject()) continue;
                        JsonObject artObj = artistsArr.get(aIdx).getAsJsonObject();
                        if (artObj.has("name") && !artObj.get("name").isJsonNull()) {
                            String aName = artObj.get("name").getAsString();
                            if (foundArtist.isEmpty()) {
                                foundArtist = aName;
                            }
                            if (allArtistsSb.length() > 0) {
                                allArtistsSb.append(" ");
                            }
                            allArtistsSb.append(aName);
                            if (artistName != null && !artistName.isBlank()) {
                                bestArtistSim = Math.max(bestArtistSim, LyricsManager.computeStringSimilarity(artistName, aName));
                            }
                        }
                    }
                }
                if (artistName != null && !artistName.isBlank() && allArtistsSb.length() > 0) {
                    bestArtistSim = Math.max(bestArtistSim, LyricsManager.computeStringSimilarity(artistName, allArtistsSb.toString()));
                }
                double sim = LyricsManager.computeStringSimilarity(trackName, foundTitle);
                if (sim < 0.40) {
                    continue;
                }
                long trackDurationMs = trackObj.has("durationMs") ? trackObj.get("durationMs").getAsLong() : 0L;
                long durDiffSec = durationSec > 20L && trackDurationMs > 20000L ? Math.abs(durationSec - trackDurationMs / 1000L) : 0L;
                if (durationSec > 20L && trackDurationMs > 20000L && durDiffSec > 18L) {
                    continue;
                }
                if (artistName != null && !artistName.isBlank()) {
                    boolean strongDurationAndTitle = sim >= 0.85 && durationSec > 20L && trackDurationMs > 20000L && durDiffSec <= 3L;
                    if (bestArtistSim < 0.30 && !strongDurationAndTitle) {
                        continue;
                    }
                } else if (sim < 0.75 && (durationSec <= 20L || durDiffSec > 4L)) {
                    continue;
                }
                boolean possibleMismatch = sim < 0.70 || (artistName != null && !artistName.isBlank() && bestArtistSim < 0.45 && durDiffSec > 4L);
                String trackId = trackObj.get("id").getAsString();

                FetchResult lrcRes = this.fetchYandexLrcLyrics(trackId, foundArtist.isEmpty() ? artistName : foundArtist, foundTitle.isEmpty() ? trackName : foundTitle, fetchId, possibleMismatch);
                if (lrcRes != null && lrcRes.lines() != null && !lrcRes.lines().isEmpty()) {
                    return lrcRes;
                }

                FetchResult textRes = this.fetchYandexTextLyrics(trackId, durationSec > 0L ? durationSec * 1000L : trackDurationMs, fetchId, possibleMismatch);
                if (textRes != null && textRes.lines() != null && !textRes.lines().isEmpty()) {
                    return textRes;
                }

                String supplementUrl = "https://api.music.yandex.net/tracks/" + trackId + "/supplement";
                String supplementResp = LyricsManager.httpGet(supplementUrl, headers, 2500);
                if (supplementResp == null) {
                    continue;
                }
                if (fetchId != this.currentFetchId.get()) {
                    return null;
                }
                JsonObject supplementJson = JsonParser.parseString((String)supplementResp).getAsJsonObject();
                if (!supplementJson.has("result")) {
                    continue;
                }
                JsonObject suppResult = supplementJson.getAsJsonObject("result");
                if (suppResult.has("lyrics") && !suppResult.get("lyrics").isJsonNull()) {
                    List<LyricLine> lines;
                    JsonObject lyrObj = suppResult.getAsJsonObject("lyrics");
                    if (lyrObj.has("syncedLyrics") && !lyrObj.get("syncedLyrics").isJsonNull() && !(lines = this.parseLrc(lyrObj.get("syncedLyrics").getAsString())).isEmpty()) {
                        return new FetchResult(lines, possibleMismatch, true);
                    }
                    if (lyrObj.has("fullLyrics") && !lyrObj.get("fullLyrics").isJsonNull()) {
                        long targetDur = durationSec > 0L ? durationSec * 1000L : (trackDurationMs > 0L ? trackDurationMs : 180000L);
                        FetchResult pr = this.parsePlainLyrics(lyrObj.get("fullLyrics").getAsString(), targetDur, fetchId);
                        if (pr != null && pr.lines() != null && !pr.lines().isEmpty()) {
                            return pr;
                        }
                    }
                }
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private FetchResult queryYandexMusicById(String trackId, long durationSec, long fetchId) {
        if (fetchId != this.currentFetchId.get() || trackId == null || trackId.isBlank()) {
            return null;
        }
        try {
            FetchResult lrcRes = this.fetchYandexLrcLyrics(trackId, "", "", fetchId, false);
            if (lrcRes != null && lrcRes.lines() != null && !lrcRes.lines().isEmpty()) {
                return lrcRes;
            }
            FetchResult textRes = this.fetchYandexTextLyrics(trackId, durationSec > 0L ? durationSec * 1000L : 180000L, fetchId, false);
            if (textRes != null && textRes.lines() != null && !textRes.lines().isEmpty()) {
                return textRes;
            }
            String supplementUrl = "https://api.music.yandex.net/tracks/" + trackId + "/supplement";
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Yandex-Music-API");
            headers.put("X-Yandex-Music-Client", "YandexMusicAndroid/24023621");
            headers.put("Accept", "application/json");
            String cleanToken = com.example.yandexmusichud.hud.YandexMusicSettingsScreen.sanitizeToken(HudConfig.INSTANCE.yandexMusicToken);
            if (!cleanToken.isEmpty()) {
                headers.put("Authorization", "OAuth " + cleanToken);
            }
            String supplementResp = LyricsManager.httpGet(supplementUrl, headers, 2500);
            if (supplementResp == null || fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject supplementJson = JsonParser.parseString(supplementResp).getAsJsonObject();
            if (!supplementJson.has("result")) {
                return null;
            }
            JsonObject suppResult = supplementJson.getAsJsonObject("result");
            if (suppResult.has("lyrics") && !suppResult.get("lyrics").isJsonNull()) {
                JsonObject lyrObj = suppResult.getAsJsonObject("lyrics");
                if (lyrObj.has("syncedLyrics") && !lyrObj.get("syncedLyrics").isJsonNull()) {
                    List<LyricLine> lines = this.parseLrc(lyrObj.get("syncedLyrics").getAsString());
                    if (!lines.isEmpty()) {
                        return new FetchResult(lines, false, true);
                    }
                }
                if (lyrObj.has("fullLyrics") && !lyrObj.get("fullLyrics").isJsonNull()) {
                    long targetDur = durationSec > 0L ? durationSec * 1000L : 180000L;
                    FetchResult pr = this.parsePlainLyrics(lyrObj.get("fullLyrics").getAsString(), targetDur, fetchId);
                    if (pr != null && pr.lines() != null && !pr.lines().isEmpty()) {
                        return pr;
                    }
                }
            }
        }
        catch (Exception exception) {
        }
        return null;
    }

    private FetchResult queryNeteaseLyrics(String query, String targetTitle, String targetArtist, long durationSec, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            List<LyricLine> lines;
            JsonObject lrc;
            String searchUrl = "https://music.163.com/api/search/get/web?s=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=1&offset=0&total=true&limit=5";
            Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "Referer", "https://music.163.com/");
            String searchResp = LyricsManager.httpGet(searchUrl, headers, 2500);
            if (searchResp == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject searchJson = JsonParser.parseString((String)searchResp).getAsJsonObject();
            if (!searchJson.has("result") || searchJson.get("result").isJsonNull()) {
                return null;
            }
            JsonObject resObj = searchJson.getAsJsonObject("result");
            if (!resObj.has("songs") || resObj.get("songs").isJsonNull()) {
                return null;
            }
            JsonArray songs = resObj.getAsJsonArray("songs");
            if (songs.isEmpty()) {
                return null;
            }
            JsonObject bestSong = null;
            double bestScore = 0.0;
            for (JsonElement se : songs) {
                if (!se.isJsonObject()) continue;
                JsonObject sObj = se.getAsJsonObject();
                String name = sObj.has("name") && !sObj.get("name").isJsonNull() ? sObj.get("name").getAsString() : "";
                double tSim = LyricsManager.computeStringSimilarity(targetTitle, name);
                if (tSim < 0.55) continue;
                double aSim = 0.0;
                StringBuilder artistBuilder = new StringBuilder();
                if (sObj.has("artists") && !sObj.get("artists").isJsonNull()) {
                    JsonArray artArr = sObj.getAsJsonArray("artists");
                    for (int i = 0; i < artArr.size(); ++i) {
                        if (!artArr.get(i).isJsonObject()) continue;
                        JsonObject aObj = artArr.get(i).getAsJsonObject();
                        if (aObj.has("name") && !aObj.get("name").isJsonNull()) {
                            String aName = aObj.get("name").getAsString();
                            if (artistBuilder.length() > 0) artistBuilder.append(" ");
                            artistBuilder.append(aName);
                            if (targetArtist != null && !targetArtist.isBlank()) {
                                aSim = Math.max(aSim, LyricsManager.computeStringSimilarity(targetArtist, aName));
                            }
                        }
                    }
                }
                if (targetArtist != null && !targetArtist.isBlank() && artistBuilder.length() > 0) {
                    aSim = Math.max(aSim, LyricsManager.computeStringSimilarity(targetArtist, artistBuilder.toString()));
                }
                if (targetArtist != null && !targetArtist.isBlank() && aSim < 0.35) continue;
                long songDurMs = sObj.has("duration") && !sObj.get("duration").isJsonNull() ? sObj.get("duration").getAsLong() : 0L;
                if (durationSec > 20L && songDurMs > 20000L) {
                    long diffSec = Math.abs(durationSec - songDurMs / 1000L);
                    if (diffSec > 12L) continue;
                }
                double combined = targetArtist != null && !targetArtist.isBlank() ? (tSim * 0.65 + aSim * 0.35) : tSim;
                if (!(combined > bestScore)) continue;
                bestScore = combined;
                bestSong = sObj;
            }
            if (bestSong == null || bestScore < 0.55) {
                return null;
            }
            boolean possibleMismatch = bestScore < 0.75;
            long songId = bestSong.get("id").getAsLong();
            String lyricUrl = "https://music.163.com/api/song/lyric?os=pc&id=" + songId + "&lv=-1&kv=-1&tv=-1";
            String lyricResp = LyricsManager.httpGet(lyricUrl, headers, 2500);
            if (lyricResp == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject lyricJson = JsonParser.parseString((String)lyricResp).getAsJsonObject();
            if (lyricJson.has("lrc") && !lyricJson.get("lrc").isJsonNull() && (lrc = lyricJson.getAsJsonObject("lrc")).has("lyric") && !lrc.get("lyric").isJsonNull() && !(lines = this.parseLrc(lrc.get("lyric").getAsString())).isEmpty()) {
                return new FetchResult(lines, possibleMismatch, true);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private LyricsConsensusEngine.Candidate wrapSyncedResult(String provider, FetchResult res) {
        if (res == null || res.lines() == null || res.lines().isEmpty()) {
            return null;
        }
        LyricsConsensusEngine.Candidate c = new LyricsConsensusEngine.Candidate(provider, res.lines(), null, res.isRealSync());
        c.possibleMismatch = res.possibleMismatch();
        return c;
    }

    private LyricsConsensusEngine.Candidate queryGeniusLyrics(String title, String artist, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            String lyrics = GeniusLyricsProvider.fetchLyrics(title, artist, 3000L);
            if (lyrics != null && !lyrics.isBlank()) {
                return new LyricsConsensusEngine.Candidate("GENIUS", null, lyrics, false);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private LyricsConsensusEngine.Candidate queryLrclibGetCandidate(String trackName, String artistName, long durationSec, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            String plain;
            List<LyricLine> lines;
            StringBuilder urlBuilder = new StringBuilder("https://lrclib.net/api/get?");
            urlBuilder.append("track_name=").append(URLEncoder.encode(trackName, StandardCharsets.UTF_8));
            urlBuilder.append("&artist_name=").append(URLEncoder.encode(artistName, StandardCharsets.UTF_8));
            if (durationSec > 0L) {
                urlBuilder.append("&duration=").append(durationSec);
            }
            Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String resp = LyricsManager.httpGet(urlBuilder.toString(), headers, 4500);
            if (resp == null || fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject obj = JsonParser.parseString((String)resp).getAsJsonObject();
            if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull() && !(lines = this.parseLrc(obj.get("syncedLyrics").getAsString())).isEmpty()) {
                return new LyricsConsensusEngine.Candidate("LRCLIB_GET", lines, null, true);
            }
            if (obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull() && !(plain = obj.get("plainLyrics").getAsString()).isBlank()) {
                return new LyricsConsensusEngine.Candidate("LRCLIB_PLAIN", null, plain, false);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private LyricsConsensusEngine.Candidate queryLrclibSearchPlainCandidate(String query, long durationSec, String targetTitle, String targetArtist, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            String plain;
            String url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String resp = LyricsManager.httpGet(url, headers, 4500);
            if (resp == null || fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonArray arr = JsonParser.parseString((String)resp).getAsJsonArray();
            if (arr.isEmpty()) {
                return null;
            }
            JsonObject bestPlainObj = null;
            int bestPlainScore = 40;
            for (JsonElement elem : arr) {
                int score;
                JsonObject obj;
                if (!elem.isJsonObject() || !(obj = elem.getAsJsonObject()).has("plainLyrics") || obj.get("plainLyrics").isJsonNull() || (score = this.calculateMatchScore(obj, durationSec, targetTitle, targetArtist)) <= bestPlainScore) continue;
                bestPlainScore = score;
                bestPlainObj = obj;
            }
            if (bestPlainObj != null && !(plain = bestPlainObj.get("plainLyrics").getAsString()).isBlank()) {
                return new LyricsConsensusEngine.Candidate("LRCLIB_PLAIN", null, plain, false);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private LyricsConsensusEngine.Candidate queryLyricsOvhCandidate(String artist, String title, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            String lyrics;
            Map<String, String> headers;
            if (artist.isEmpty() || title.isEmpty()) {
                return null;
            }
            String url = "https://api.lyrics.ovh/v1/" + URLEncoder.encode(artist, StandardCharsets.UTF_8) + "/" + URLEncoder.encode(title, StandardCharsets.UTF_8);
            String resp = LyricsManager.httpGet(url, headers = Map.of("User-Agent", "MusicSubtitles/2.3.0"), 2500);
            if (resp == null || fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject json = JsonParser.parseString((String)resp).getAsJsonObject();
            if (json.has("lyrics") && !json.get("lyrics").isJsonNull() && !(lyrics = json.get("lyrics").getAsString()).isBlank()) {
                return new LyricsConsensusEngine.Candidate("OVH", null, lyrics, false);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private FetchResult queryLyricsOvh(String artist, String title, long durationSec, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            Map<String, String> headers;
            if (artist.isEmpty() || title.isEmpty()) {
                return null;
            }
            String url = "https://api.lyrics.ovh/v1/" + URLEncoder.encode(artist, StandardCharsets.UTF_8) + "/" + URLEncoder.encode(title, StandardCharsets.UTF_8);
            String resp = LyricsManager.httpGet(url, headers = Map.of("User-Agent", "MusicSubtitles/2.2.5"), 2500);
            if (resp == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject json = JsonParser.parseString((String)resp).getAsJsonObject();
            if (json.has("lyrics") && !json.get("lyrics").isJsonNull()) {
                long dur = durationSec > 0L ? durationSec * 1000L : 180000L;
                return this.parsePlainLyrics(json.get("lyrics").getAsString(), dur, fetchId);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private FetchResult queryLrclibGet(String trackName, String artistName, long durationSec, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            List<LyricLine> lines;
            StringBuilder urlBuilder = new StringBuilder("https://lrclib.net/api/get?");
            urlBuilder.append("track_name=").append(URLEncoder.encode(trackName, StandardCharsets.UTF_8));
            urlBuilder.append("&artist_name=").append(URLEncoder.encode(artistName, StandardCharsets.UTF_8));
            if (durationSec > 0L) {
                urlBuilder.append("&duration=").append(durationSec);
            }
            Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String resp = LyricsManager.httpGet(urlBuilder.toString(), headers, 4500);
            if (resp == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonObject obj = JsonParser.parseString((String)resp).getAsJsonObject();
            if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull() && !(lines = this.parseLrc(obj.get("syncedLyrics").getAsString())).isEmpty()) {
                return new FetchResult(lines, false, true);
            }
            if (obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull()) {
                long targetDurationMs = durationSec > 0L ? durationSec * 1000L : 180000L;
                return this.parsePlainLyrics(obj.get("plainLyrics").getAsString(), targetDurationMs, fetchId);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private FetchResult queryLrclibSearchSynced(String query, long durationSec, String targetTitle, String targetArtist, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            List<LyricLine> lines;
            String url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String resp = LyricsManager.httpGet(url, headers, 4500);
            if (resp == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonArray arr = JsonParser.parseString((String)resp).getAsJsonArray();
            if (arr.isEmpty()) {
                return null;
            }
            JsonObject bestSyncedObj = null;
            int bestSyncedScore = 40;
            for (JsonElement elem : arr) {
                int score;
                JsonObject obj;
                if (!elem.isJsonObject() || !(obj = elem.getAsJsonObject()).has("syncedLyrics") || obj.get("syncedLyrics").isJsonNull() || (score = this.calculateMatchScore(obj, durationSec, targetTitle, targetArtist)) <= bestSyncedScore) continue;
                bestSyncedScore = score;
                bestSyncedObj = obj;
            }
            if (bestSyncedObj != null && !(lines = this.parseLrc(bestSyncedObj.get("syncedLyrics").getAsString())).isEmpty()) {
                boolean possibleMismatch = bestSyncedScore < 250;
                return new FetchResult(lines, possibleMismatch, true);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private FetchResult queryLrclibSearchPlain(String query, long durationSec, String targetTitle, String targetArtist, long fetchId) {
        if (fetchId != this.currentFetchId.get()) {
            return null;
        }
        try {
            String url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            Map<String, String> headers = Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String resp = LyricsManager.httpGet(url, headers, 4500);
            if (resp == null) {
                return null;
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            JsonArray arr = JsonParser.parseString((String)resp).getAsJsonArray();
            if (arr.isEmpty()) {
                return null;
            }
            JsonObject bestPlainObj = null;
            int bestPlainScore = 40;
            for (JsonElement elem : arr) {
                int score;
                JsonObject obj;
                if (!elem.isJsonObject() || !(obj = elem.getAsJsonObject()).has("plainLyrics") || obj.get("plainLyrics").isJsonNull() || (score = this.calculateMatchScore(obj, durationSec, targetTitle, targetArtist)) <= bestPlainScore) continue;
                bestPlainScore = score;
                bestPlainObj = obj;
            }
            if (bestPlainObj != null) {
                long targetDurationMs = durationSec > 0L ? durationSec * 1000L : 180000L;
                return this.parsePlainLyrics(bestPlainObj.get("plainLyrics").getAsString(), targetDurationMs, fetchId);
            }
        }
        catch (Exception exception) {

        }
        return null;
    }

    private int calculateMatchScore(JsonObject obj, long durationSec, String targetTitle, String targetArtist) {
        long itemDurSec;
        String itemTrack = obj.has("trackName") && !obj.get("trackName").isJsonNull() ? obj.get("trackName").getAsString() : "";
        String itemArtist = obj.has("artistName") && !obj.get("artistName").isJsonNull() ? obj.get("artistName").getAsString() : "";
        double titleSim = LyricsManager.computeStringSimilarity(targetTitle, itemTrack);
        double artistSim = LyricsManager.computeStringSimilarity(targetArtist, itemArtist);
        if (titleSim < 0.35) {
            return 0;
        }
        int score = 0;
        int n = titleSim >= 0.9 ? (score += 300) : (score = titleSim >= 0.5 ? (score = score + (int)(240.0 * titleSim)) : (score = score + (int)(150.0 * titleSim)));
        if (artistSim >= 0.85) {
            score += 150;
        } else if (artistSim >= 0.3) {
            score += (int)(120.0 * artistSim);
        } else if (!targetArtist.isEmpty()) {
            score -= 100;
        }
        long l = itemDurSec = obj.has("duration") && !obj.get("duration").isJsonNull() ? (long)obj.get("duration").getAsDouble() : 0L;
        if (durationSec > 0L && itemDurSec > 0L) {
            long diff = Math.abs(itemDurSec - durationSec);
            if (diff <= 2L) {
                score += 500;
            } else if (diff <= 5L) {
                score += 250;
            } else if (diff <= 10L) {
                score += 100;
            } else if (diff > 30L) {
                score -= 300;
            }
        }
        return score;
    }

    private static double computeStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        String a = LyricsManager.normalizeForComparison(s1);
        String b = LyricsManager.normalizeForComparison(s2);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.contains(b) || b.contains(a)) {
            return (double)Math.min(a.length(), b.length()) / (double)Math.max(a.length(), b.length());
        }
        HashSet<String> wordsA = new HashSet<String>(Arrays.asList(a.split("\\s+")));
        HashSet<String> wordsB = new HashSet<String>(Arrays.asList(b.split("\\s+")));
        wordsA.removeIf(w -> w.length() < 2);
        wordsB.removeIf(w -> w.length() < 2);
        if (wordsA.isEmpty() || wordsB.isEmpty()) {
            return 0.0;
        }
        HashSet<String> intersection = new HashSet<String>(wordsA);
        intersection.retainAll(wordsB);
        HashSet<String> union = new HashSet<String>(wordsA);
        union.addAll(wordsB);
        return (double)intersection.size() / (double)union.size();
    }

    private boolean isLyricsMismatched(List<LyricLine> lines, TrackState state) {
        if (lines == null || lines.isEmpty() || state == null) {
            return false;
        }
        long durationMs = state.durationMs();
        if (durationMs > 30000L) {
            boolean isSpeedUp;
            long lastLineEnd = lines.get((int)(lines.size() - 1)).endMs;
            long firstLineStart = lines.get((int)0).startMs;
            if ((double)firstLineStart > (double)durationMs * 0.55) {
                return true;
            }
            if (lastLineEnd > durationMs + 25000L) {
                return true;
            }
            boolean bl = isSpeedUp = SPEED_UP_PATTERN.matcher(state.title()).find() || SPEED_UP_PATTERN.matcher(state.artist()).find();
            if (!isSpeedUp && (double)lastLineEnd < (double)durationMs * 0.35 && lines.size() > 2) {
                return true;
            }
            if (!isSpeedUp && durationMs - lastLineEnd > Math.max(80000L, (long)((double)durationMs * 0.40)) && lines.size() > 3) {
                return true;
            }
        }
        String trackInfo = (state.artist() + " " + state.title()).toLowerCase();
        int cyrillicInTrack = LyricsManager.countCyrillicChars(trackInfo);
        int cjkInTrack = LyricsManager.countCjkChars(trackInfo);
        StringBuilder sampleLyrics = new StringBuilder();
        int sampleLimit = Math.min(10, lines.size());
        for (int i = 0; i < sampleLimit; ++i) {
            sampleLyrics.append(lines.get((int)i).text).append(" ");
        }
        String lyricsStr = sampleLyrics.toString();
        int cjkInLyrics = LyricsManager.countCjkChars(lyricsStr);
        int cyrillicInLyrics = LyricsManager.countCyrillicChars(lyricsStr);
        if (cyrillicInTrack >= 3 && cyrillicInLyrics == 0 && cjkInLyrics >= 5) {
            return true;
        }
        return cjkInTrack == 0 && cjkInLyrics > 15 && cyrillicInTrack >= 2;
    }

    private static int countCyrillicChars(String s) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ((c < '\u0400' || c > '\u04ff') && (c < '\u0500' || c > '\u052f')) continue;
            ++count;
        }
        return count;
    }

    private static int countCjkChars(String s) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ((c < '\u4e00' || c > '\u9fff') && (c < '\u3400' || c > '\u4dbf')) continue;
            ++count;
        }
        return count;
    }

    private static String normalizeForComparison(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435').replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim();
    }

    public List<LyricLine> parseLrc(String lrcContent) {
        if (lrcContent == null || lrcContent.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<RawLine> rawList = new ArrayList<RawLine>();
        String[] lines = lrcContent.split("\\r?\\n");
        long offsetMs = 0L;
        for (String raw : lines) {
            Matcher offMatcher = OFFSET_PATTERN.matcher(raw);
            if (!offMatcher.find()) continue;
            try {
                offsetMs = Long.parseLong(offMatcher.group(1).trim());
            }
            catch (Exception exception) {

            }
        }
        for (String raw : lines) {
            Matcher matcher = LRC_LINE_PATTERN.matcher(raw);
            if (!matcher.find()) continue;
            long min = Long.parseLong(matcher.group(1));
            double sec = Double.parseDouble(matcher.group(2));
            long timeMs = Math.max(0L, min * 60000L + (long)(sec * 1000.0) + offsetMs);
            String text = matcher.group(3).trim();
            rawList.add(new RawLine(timeMs, text));
        }
        if (rawList.isEmpty()) {
            return Collections.emptyList();
        }
        rawList.sort(Comparator.comparingLong(r -> r.timeMs));
        ArrayList<LyricLine> result = new ArrayList<LyricLine>();
        for (int i = 0; i < rawList.size(); ++i) {
            String plainText;
            RawLine current = (RawLine)rawList.get(i);
            if (current.text.isEmpty() || (plainText = this.stripEnhancedLrcTags(current.text)).isEmpty() || plainText.matches("(?i)^\\[?(?:instrumental|\u0441\u043e\u043b\u043e|guitar\\s*solo|\u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u0430\u043b|\u043c\u0443\u0437\u044b\u043a\u0430|intro|outro|solo)\\]?$")) continue;
            plainText = plainText.replace("\\n", " ").replace(" // ", ", ").replace(" / ", ", ").replaceAll("\\s+", " ").trim();
            if (plainText.isEmpty()) continue;
            long startMs = current.timeMs;
            long nextTimeMs = i < rawList.size() - 1 ? ((RawLine)rawList.get((int)(i + 1))).timeMs : startMs + 5000L;
            long gap = Math.max(600L, nextTimeMs - startMs);
            String cleanForMetrics = LyricLine.stripFormattingTags(plainText).trim();
            if (cleanForMetrics.isEmpty()) {
                cleanForMetrics = plainText;
            }
            String[] tokens = cleanForMetrics.split("\\s+");
            int wordCount = 0;
            for (String t : tokens) {
                if (!t.isEmpty()) ++wordCount;
            }
            if (wordCount == 0) wordCount = 1;

            long minSyllableDur = (long)StudioCascadeAligner.countSyllables(cleanForMetrics) * 140L + (long)wordCount * 80L + 200L;
            long maxVocalDur = Math.max(minSyllableDur, (long)wordCount * 450L + 500L);
            long naturalDur = Math.min(maxVocalDur, Math.max(minSyllableDur, (long)((double)gap * 0.82)));
            long estimatedVocalDur = Math.min(Math.max(400L, gap - 80L), naturalDur);
            long vocalEndMs = startMs + estimatedVocalDur;

            long endMs = Math.min(nextTimeMs, vocalEndMs + Math.min(1800L, Math.max(400L, (gap - estimatedVocalDur) / 2)));
            if (endMs <= startMs) {
                endMs = startMs + estimatedVocalDur;
            }
            List<LyricLine.Word> words = this.parseEnhancedLrcWords(current.text, startMs, vocalEndMs, offsetMs);
            if (!words.isEmpty()) {
                long lastWordEnd = words.get(words.size() - 1).endMs();
                vocalEndMs = Math.max(startMs + 100L, lastWordEnd);
                endMs = Math.max(endMs, vocalEndMs);
            } else {
                Matcher tm = ENHANCED_WORD_PATTERN.matcher(current.text);
                long trailingTagTime = -1L;
                while (tm.find()) {
                    long tMin = Long.parseLong(tm.group(1));
                    double tSec = Double.parseDouble(tm.group(2));
                    trailingTagTime = Math.max(0L, tMin * 60000L + (long)(tSec * 1000.0) + offsetMs);
                }
                if (trailingTagTime > startMs) {
                    endMs = Math.max(startMs + 100L, trailingTagTime);
                    vocalEndMs = Math.min(endMs, Math.max(startMs + 100L, vocalEndMs));
                }
                words = StudioCascadeAligner.segmentWordsBySyllables(cleanForMetrics, startMs, vocalEndMs);
            }
            List<LyricLine> splitLines = this.splitLongLineIfNeeded(startMs, endMs, vocalEndMs, gap, plainText, words, wordCount);
            result.addAll(splitLines);
        }
        return result;
    }

    private List<LyricLine> splitLongLineIfNeeded(long startMs, long endMs, long vocalEndMs, long gap, String plainText, List<LyricLine.Word> words, int wordCount) {
        if (gap < 3200L || (wordCount < 11 && plainText.length() < 54)) {
            return Collections.singletonList(new LyricLine(startMs, endMs, vocalEndMs, plainText, words));
        }
        int targetParts = 2;
        if (wordCount >= 16 || (plainText.length() >= 76 && gap >= 5500L)) {
            targetParts = 3;
        }
        if (wordCount >= 24 && gap >= 8000L) {
            targetParts = 4;
        }
        List<String> chunks = this.splitTextIntoChunks(plainText, targetParts);
        if (chunks.size() <= 1) {
            return Collections.singletonList(new LyricLine(startMs, endMs, vocalEndMs, plainText, words));
        }
        int totalChars = 0;
        for (String c : chunks) {
            totalChars += c.length();
        }
        if (totalChars <= 0) {
            return Collections.singletonList(new LyricLine(startMs, endMs, vocalEndMs, plainText, words));
        }
        ArrayList<LyricLine> result = new ArrayList<LyricLine>(chunks.size());
        long curStart = startMs;
        long totalSpan = Math.max(1000L, endMs - startMs);
        long totalVocalDur = Math.max(500L, vocalEndMs - startMs);
        int wordCursor = 0;
        boolean useExactWordSlices = words != null && words.size() >= chunks.size();
        for (int k = 0; k < chunks.size(); ++k) {
            String chunkText = chunks.get(k);
            boolean isLast = (k == chunks.size() - 1);
            List<LyricLine.Word> chunkWords = Collections.emptyList();
            long chunkStart = curStart;
            long chunkEnd;
            long chunkVocalEnd;
            if (useExactWordSlices && wordCursor < words.size()) {
                int tokenCount = 0;
                for (String tk : chunkText.trim().split("\\s+")) {
                    if (!tk.isEmpty()) ++tokenCount;
                }
                int wEnd = isLast ? words.size() : Math.min(words.size() - (chunks.size() - 1 - k), wordCursor + Math.max(1, tokenCount));
                wEnd = Math.max(wordCursor + 1, Math.min(words.size(), wEnd));
                List<LyricLine.Word> slice = words.subList(wordCursor, wEnd);
                chunkStart = (k == 0) ? startMs : Math.max(curStart, slice.get(0).startMs());
                chunkVocalEnd = Math.max(chunkStart + 150L, slice.get(slice.size() - 1).endMs());
                if (isLast || wEnd >= words.size()) {
                    chunkEnd = Math.max(chunkVocalEnd, endMs);
                } else {
                    long nextWordStart = words.get(wEnd).startMs();
                    chunkEnd = Math.max(chunkVocalEnd, Math.min(endMs, nextWordStart));
                }
                ArrayList<LyricLine.Word> matched = new ArrayList<LyricLine.Word>(slice.size());
                int cursor = 0;
                for (LyricLine.Word w : slice) {
                    int cs = cursor;
                    int ce = cs + w.text().length();
                    matched.add(new LyricLine.Word(Math.max(chunkStart, w.startMs()), Math.min(chunkEnd, Math.max(w.startMs() + 50L, w.endMs())), w.text(), cs, ce));
                    cursor = ce + 1;
                }
                chunkWords = matched;
                wordCursor = wEnd;
            } else {
                long chunkDur = isLast ? (endMs - curStart) : Math.max(800L, (long)((double)totalSpan * ((double)chunkText.length() / (double)totalChars)));
                chunkEnd = isLast ? endMs : Math.min(endMs, curStart + chunkDur);
                if (chunkEnd <= chunkStart) {
                    chunkEnd = chunkStart + 800L;
                }
                long chunkVocalDur = Math.max(400L, (long)((double)totalVocalDur * ((double)chunkText.length() / (double)totalChars)));
                chunkVocalEnd = Math.min(chunkEnd, chunkStart + chunkVocalDur);
                chunkWords = StudioCascadeAligner.segmentWordsBySyllables(chunkText, chunkStart, chunkVocalEnd);
            }

            result.add(new LyricLine(chunkStart, chunkEnd, chunkVocalEnd, chunkText, chunkWords));
            curStart = chunkEnd;
        }
        return result;
    }

    private List<String> splitTextIntoChunks(String text, int targetParts) {
        ArrayList<String> chunks = new ArrayList<String>();
        if (text == null || text.trim().isEmpty() || targetParts <= 1) {
            if (text != null && !text.trim().isEmpty()) {
                chunks.add(text.trim());
            }
            return chunks;
        }
        String[] words = text.trim().split("\\s+");
        if (words.length < targetParts * 2) {
            chunks.add(text.trim());
            return chunks;
        }
        int wordsPerChunk = (int)Math.round((double)words.length / (double)targetParts);
        int curIdx = 0;
        for (int p = 0; p < targetParts; ++p) {
            if (p == targetParts - 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = curIdx; i < words.length; ++i) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(words[i]);
                }
                String s = sb.toString().trim();
                if (!s.isEmpty()) {
                    chunks.add(s);
                }
            } else {
                int targetWordCount = wordsPerChunk;
                int bestWordEnd = curIdx + targetWordCount;
                int searchMin = Math.max(curIdx + 2, curIdx + targetWordCount - 2);
                int searchMax = Math.min(words.length - (targetParts - p - 1) * 2, curIdx + targetWordCount + 2);
                for (int i = searchMin; i <= searchMax; ++i) {
                    String prevW = words[i - 1];
                    if (prevW.endsWith(",") || prevW.endsWith(".") || prevW.endsWith("!") || prevW.endsWith("?") || prevW.endsWith(";") || prevW.endsWith(":") || prevW.endsWith("-")) {
                        bestWordEnd = i;
                        break;
                    }
                }
                bestWordEnd = Math.max(curIdx + 1, Math.min(words.length - 1, bestWordEnd));
                StringBuilder sb = new StringBuilder();
                for (int i = curIdx; i < bestWordEnd; ++i) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(words[i]);
                }
                String s = sb.toString().trim();
                if (!s.isEmpty()) {
                    chunks.add(s);
                }
                curIdx = bestWordEnd;
            }
        }
        return chunks.isEmpty() ? Collections.singletonList(text.trim()) : chunks;
    }

    private List<LyricLine.Word> parseEnhancedLrcWords(String rawText, long lineStartMs, long lineEndMs, long offsetMs) {
        ArrayList<LyricLine.Word> words = new ArrayList<LyricLine.Word>();
        ArrayList<Long> explicitEnds = new ArrayList<Long>();
        Matcher matcher = ENHANCED_WORD_PATTERN.matcher(rawText);
        int textOffset = 0;
        while (matcher.find()) {
            long min = Long.parseLong(matcher.group(1));
            double sec = Double.parseDouble(matcher.group(2));
            long wordTime = Math.max(0L, min * 60000L + (long)(sec * 1000.0) + offsetMs);
            String wordText = matcher.group(3)
                .replace("\\n", " ")
                .replace(" // ", ", ")
                .replace(" / ", ", ")
                .replaceAll("\\s+", " ")
                .trim();
            String cleanWordText = LyricLine.stripFormattingTags(wordText).trim();
            if (!words.isEmpty() && explicitEnds.get(words.size() - 1) < 0L) {
                explicitEnds.set(words.size() - 1, wordTime);
            }
            if (cleanWordText.isEmpty()) continue;
            int charStart = textOffset;
            int charEnd = charStart + cleanWordText.length();
            words.add(new LyricLine.Word(wordTime, wordTime + 500L, cleanWordText, charStart, charEnd));
            explicitEnds.add(-1L);
            textOffset = charEnd + 1;
        }
        for (int i = 0; i < words.size(); ++i) {
            LyricLine.Word w = words.get(i);
            long expEnd = explicitEnds.get(i);
            long end;
            if (expEnd >= 0L) {
                end = Math.max(w.startMs() + 50L, expEnd);
            } else if (i < words.size() - 1) {
                end = Math.max(w.startMs() + 50L, words.get(i + 1).startMs());
            } else {
                end = Math.max(w.startMs() + 100L, Math.min(Math.max(lineEndMs, w.startMs() + 250L), w.startMs() + 800L));
            }
            words.set(i, new LyricLine.Word(w.startMs(), end, w.text(), w.charStart(), w.charEnd()));
        }
        return words;
    }

    private List<LyricLine> splitLongLyricLine(long startMs, long endMs, String plainText, List<LyricLine.Word> words) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = plainText.trim().split("\\s+");
        long gap = endMs - startMs;
        long vocalEnd = startMs + Math.max(500L, (long)((double)gap * 0.9));
        return this.splitLongLineIfNeeded(startMs, endMs, vocalEnd, gap, plainText, words, tokens.length);
    }

    private String stripEnhancedLrcTags(String rawText) {
        return rawText.replaceAll("<\\d+:\\d+(?:\\.\\d+)?>", "").replaceAll("\\[\\d+:\\d+(?:\\.\\d+)?\\]", "").trim();
    }

    private FetchResult parsePlainLyrics(String plainLyrics, long totalDurationMs, long fetchId) {
        if (plainLyrics == null || plainLyrics.isEmpty()) {
            return new FetchResult(Collections.emptyList(), false);
        }
        String[] rawLines = plainLyrics.split("\\r?\\n");
        ArrayList<String> validLines = new ArrayList<String>();
        for (String l : rawLines) {
            String trimmed = l.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("[") || trimmed.endsWith("]")) continue;
            validLines.add(trimmed);
        }
        if (validLines.isEmpty()) {
            return new FetchResult(Collections.emptyList(), false);
        }
        if (fetchId != this.currentFetchId.get()) {
            return new FetchResult(Collections.emptyList(), false);
        }
        if (HudConfig.INSTANCE.audioAnalysisEnabled) {
            FetchResult aiAligned = this.tryAiAlignment(plainLyrics, validLines, totalDurationMs, fetchId);
            if (aiAligned != null && aiAligned.lines() != null && !aiAligned.lines().isEmpty()) {
                return aiAligned;
            }
        }
        List<LyricLine> cascadeLines = StudioCascadeAligner.align(plainLyrics, null, totalDurationMs);
        if (cascadeLines != null && !cascadeLines.isEmpty()) {
            return new FetchResult(cascadeLines, false);
        }
        ArrayList<LyricLine> result = new ArrayList<LyricLine>();
        long safeTotal = totalDurationMs > 15000L ? totalDurationMs : 180000L;
        long timePerLine = Math.max(1000L, safeTotal / (long)validLines.size());
        for (int i = 0; i < validLines.size(); ++i) {
            long start = (long)i * timePerLine;
            long end = Math.min(safeTotal, start + timePerLine);
            result.addAll(this.splitLongLyricLine(start, Math.max(start + 400L, end), validLines.get(i), null));
        }
        return new FetchResult(result, true);
    }

    private FetchResult tryAiAlignment(String rawPlainLyrics, List<String> plainLines, long totalDurationMs, long fetchId) {
        if (!HudConfig.INSTANCE.audioAnalysisEnabled) {
            return null;
        }
        byte[] wavBytes = null;
        try {
            double confidence;
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            int port = this.resolveHelperPort();
            if (port <= 0) {
                return null;
            }
            String helperBase = "http://127.0.0.1:" + port;
            int captureDurationMs = 4000;
            LyricsManager.httpPost(helperBase + "/capture/start", "{\"durationMs\":" + captureDurationMs + "}", 2000);
            for (int elapsed = 0; elapsed < captureDurationMs + 400; elapsed += 150) {
                if (fetchId != this.currentFetchId.get()) {
                    LyricsManager.httpPost(helperBase + "/capture/stop", "", 1000);
                    return null;
                }
                Thread.sleep(150L);
            }
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            String wavBase64 = null;
            wavBytes = LyricsManager.httpGetBytes(helperBase + "/capture/result", 3000);
            if (wavBytes != null && wavBytes.length > 44) {
                wavBase64 = Base64.getEncoder().encodeToString(wavBytes);
            }
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("{\"lines\":[");
            for (int i = 0; i < plainLines.size(); ++i) {
                if (i > 0) {
                    bodyBuilder.append(",");
                }
                bodyBuilder.append("\"").append(plainLines.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
            }
            bodyBuilder.append("],\"durationMs\":").append(totalDurationMs);
            if (wavBase64 != null) {
                bodyBuilder.append(",\"wavBase64\":\"").append(wavBase64).append("\"");
            }
            bodyBuilder.append("}");
            if (fetchId != this.currentFetchId.get()) {
                return null;
            }
            String alignResponse = LyricsManager.httpPost(helperBase + "/ai-align", bodyBuilder.toString(), 5000);
            if (alignResponse != null && !alignResponse.isEmpty() && fetchId == this.currentFetchId.get()) {
                JsonObject json = JsonParser.parseString((String)alignResponse).getAsJsonObject();
                if (json.has("success") && json.get("success").getAsBoolean() && json.has("lines") && !json.get("lines").isJsonNull()) {
                    boolean possibleMismatch = json.has("possibleMismatch") && json.get("possibleMismatch").getAsBoolean();
                    confidence = json.has("confidence") ? json.get("confidence").getAsDouble() : 1.0;
                    if (confidence < 0.35) {
                        possibleMismatch = true;
                    }
                    JsonArray linesArr = json.getAsJsonArray("lines");
                    ArrayList<LyricLine> result = new ArrayList<LyricLine>();
                    for (JsonElement lineElem : linesArr) {
                        String text;
                        if (!lineElem.isJsonObject()) continue;
                        JsonObject lineObj = lineElem.getAsJsonObject();
                        long startMs = lineObj.has("startMs") ? lineObj.get("startMs").getAsLong() : 0L;
                        long endMs = lineObj.has("endMs") ? lineObj.get("endMs").getAsLong() : startMs + 4000L;
                        text = lineObj.has("text") ? lineObj.get("text").getAsString() : "";
                        if (text.isEmpty()) continue;
                        ArrayList<LyricLine.Word> words = null;
                        if (lineObj.has("words") && lineObj.get("words").isJsonArray()) {
                            JsonArray wordsArr = lineObj.getAsJsonArray("words");
                            words = new ArrayList<LyricLine.Word>();
                            for (JsonElement wElem : wordsArr) {
                                if (!wElem.isJsonObject()) continue;
                                JsonObject wObj = wElem.getAsJsonObject();
                                long wStart = wObj.has("startMs") ? wObj.get("startMs").getAsLong() : startMs;
                                long wEnd = wObj.has("endMs") ? wObj.get("endMs").getAsLong() : wStart + 500L;
                                String wText = wObj.has("text") ? wObj.get("text").getAsString() : "";
                                int charStart = wObj.has("charStart") ? wObj.get("charStart").getAsInt() : 0;
                                int charEnd = wObj.has("charEnd") ? wObj.get("charEnd").getAsInt() : wText.length();
                                if (wText.isEmpty()) continue;
                                words.add(new LyricLine.Word(wStart, wEnd, wText, charStart, charEnd));
                            }
                            if (words.isEmpty()) {
                                words = null;
                            }
                        }
                        long vocalEnd = (words != null && !words.isEmpty()) ? words.get(words.size() - 1).endMs() : endMs;
                        result.add(new LyricLine(startMs, endMs, vocalEnd, text, words));
                    }
                    if (!result.isEmpty()) {
                        return new FetchResult(result, possibleMismatch);
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("[MusicSubtitles] AI alignment fallback to DSP: " + e.getMessage());
        }
        if (fetchId == this.currentFetchId.get()) {
            String srcLyrics = rawPlainLyrics != null && !rawPlainLyrics.isBlank() ? rawPlainLyrics : String.join("\n", plainLines);
            List<LyricLine> localCascade = StudioCascadeAligner.align(srcLyrics, null, totalDurationMs);
            if (localCascade != null && !localCascade.isEmpty()) {
                return new FetchResult(localCascade, false);
            }
        }
        return null;
    }

    public static String httpPost(String urlStr, String body, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bodyBytes.length);
            conn.getOutputStream().write(bodyBytes);
            conn.getOutputStream().flush();
            int code = conn.getResponseCode();
            if (code != 200) {
                return null;
            }
            try (InputStream is = conn.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    public static byte[] httpGetBytes(String urlStr, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            int code = conn.getResponseCode();
            if (code != 200) {
                return null;
            }
            try (InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    private String cleanForSearch(String input) {
        if (input == null) {
            return "";
        }
        String s = input.trim();
        s = BROWSER_SUFFIX_PATTERN.matcher(s).replaceAll("");
        s = PIPE_NOISE_PATTERN.matcher(s).replaceAll("");
        for (int i = 0; i < 3; ++i) {
            s = BRACKET_TAGS_PATTERN.matcher(s).replaceAll("");
        }
        s = TRAILING_NOISE_PATTERN.matcher(s).replaceAll("");
        s = s.replaceAll("[\u25ba\u25c4\"'\u00ab\u00bb\u201c\u201d\u201e]", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private String normalizeTrackTitle(String title) {
        if (title == null) {
            return "";
        }
        String s = title.trim();
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains("carmelldansen") || lower.contains("carmell dansen")) {
            return s.replaceAll("(?i)carmell\\s*dansen", "Caramelldansen");
        }
        return s;
    }

    private String extractPrimaryArtist(String artist) {
        if (artist == null) {
            return "";
        }
        String s = artist.replaceAll("(?i)\\s*(?:feat\\.?|ft\\.?|featuring)\\s+.*$", "").trim();
        String[] parts = s.split("(?i)\\s*(?:,|&|x|\\+)\\s*");
        if (parts.length > 0 && !parts[0].trim().isEmpty()) {
            String p = parts[0].trim();
            if (p.toLowerCase(Locale.ROOT).startsWith("prodby") && parts.length > 1) {
                return parts[1].trim();
            }
            return p;
        }
        return s;
    }

    public record FetchResult(List<LyricLine> lines, boolean possibleMismatch, boolean isRealSync) {
        public FetchResult(List<LyricLine> lines, boolean possibleMismatch) {
            this(lines, possibleMismatch, false);
        }
    }

    private record RawLine(long timeMs, String text) {
    }
}

