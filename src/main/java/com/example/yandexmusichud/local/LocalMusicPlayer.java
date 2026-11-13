package com.example.yandexmusichud.local;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.ducking.SmartDuckingManager;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class LocalMusicPlayer {
    private static final LocalMusicPlayer INSTANCE = new LocalMusicPlayer();

    private final List<LocalMusicTrack> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private volatile boolean isPlaying = false;
    private volatile boolean isPaused = false;
    private volatile boolean shouldStop = false;
    private volatile long playbackSessionId = 0L;
    private volatile float volume = 0.8f;
    private volatile boolean shuffle = false;
    private volatile boolean repeat = false;
    private volatile long currentPositionOffsetMs = 0L;
    private volatile long pausedPositionMs = 0L;
    private Thread playbackThread = null;
    private VolumeAudioDevice currentAudioDevice = null;
    private SourceDataLine currentLine = null;
    private Bitstream currentBitstream = null;
    private final Random random = new Random();
    private LocalMusicTrack lastReportedTrack = null;
    private String lastSyncedMissingKey = "";
    private long lastMissingScanTime = 0L;
    private int consecutivePlaybackErrors = 0;
    private boolean initialized = false;
    private volatile long driftAdjustmentMs = 0L;
    private volatile long lastCalculatedDriftMs = 0L;
    private volatile long lastSyncTimestamp = 0L;
    private volatile String lastSyncStatusText = "ОЖИДАНИЕ";
    private volatile int lastSyncStatusColor = 0xFFAAAAAA;

    private LocalMusicPlayer() {}

    public static LocalMusicPlayer getInstance() {
        return INSTANCE;
    }

    public long getLastCalculatedDriftMs() {
        return this.lastCalculatedDriftMs;
    }

    public long getLastSyncTimestamp() {
        return this.lastSyncTimestamp;
    }

    public String getSyncStatusText() {
        return this.lastSyncStatusText;
    }

    public int getSyncStatusColor() {
        return this.lastSyncStatusColor;
    }

    public String getStreamSourceInfo() {
        TrackState st = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getSyncedTrack();
        if (st != null && st.directUrl() != null && !st.directUrl().isEmpty()) {
            return "Прямой поток CDN (Zero-Token)";
        }
        LocalMusicTrack cur = this.getCurrentTrack();
        if (cur != null && cur.getPath() != null) {
            String p = cur.getPath().toString();
            if (p.contains("cache")) {
                return "Сетевой кэш (Host-Shared)";
            }
            return "Локальная музыка (" + cur.getFormat() + ")";
        }
        return "В ожидании";
    }

    public synchronized void init() {
        if (this.initialized) {
            return;
        }
        this.initialized = true;
        this.volume = Math.max(0.0f, Math.min(1.0f, HudConfig.INSTANCE.localMusicVolume));
        this.shuffle = HudConfig.INSTANCE.localMusicShuffle;
        this.repeat = HudConfig.INSTANCE.localMusicRepeat;
        this.reloadPlaylist();
    }

    public synchronized void reloadPlaylist() {
        LocalMusicTrack current = this.getCurrentTrack();
        List<LocalMusicTrack> scanned = LocalMusicScanner.INSTANCE.scanTracks();
        this.playlist.clear();
        this.playlist.addAll(scanned);
        if (current != null) {
            int foundIdx = -1;
            for (int i = 0; i < this.playlist.size(); i++) {
                if (this.playlist.get(i).getPath().equals(current.getPath())) {
                    foundIdx = i;
                    break;
                }
            }
            this.currentIndex = foundIdx;
        } else if (this.currentIndex >= this.playlist.size()) {
            this.currentIndex = this.playlist.isEmpty() ? -1 : 0;
        }
    }

    public synchronized List<LocalMusicTrack> getPlaylist() {
        return Collections.unmodifiableList(new ArrayList<>(this.playlist));
    }

    public synchronized LocalMusicTrack getCurrentTrack() {
        if (this.currentIndex >= 0 && this.currentIndex < this.playlist.size()) {
            return this.playlist.get(this.currentIndex);
        }
        return null;
    }

    public synchronized int getCurrentIndex() {
        return this.currentIndex;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getEffectiveVolume() {
        float vcMult = com.example.yandexmusichud.compat.VoiceChatCompat.getCategoryVolumeMultiplier();
        return Math.max(0.0f, Math.min(1.0f, this.volume * vcMult * SmartDuckingManager.INSTANCE.getCurrentDuckingFactor()));
    }

    public void setVolume(float vol) {
        this.volume = Math.max(0.0f, Math.min(1.0f, vol));
        if (this.currentAudioDevice != null) {
            this.currentAudioDevice.setVolume(this.getEffectiveVolume());
        }
        HudConfig.INSTANCE.localMusicVolume = this.volume;
        HudConfig.save();
    }

    public boolean isShuffle() {
        return this.shuffle;
    }

    public void setShuffle(boolean shuf) {
        this.shuffle = shuf;
        HudConfig.INSTANCE.localMusicShuffle = shuf;
        HudConfig.save();
    }

    public boolean isRepeat() {
        return this.repeat;
    }

    public void setRepeat(boolean rep) {
        this.repeat = rep;
        HudConfig.INSTANCE.localMusicRepeat = rep;
        HudConfig.save();
    }

    public synchronized void playTrack(LocalMusicTrack track) {
        if (track == null) {
            return;
        }
        int idx = -1;
        for (int i = 0; i < this.playlist.size(); i++) {
            if (this.playlist.get(i).getPath().equals(track.getPath())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            this.playlist.add(track);
            idx = this.playlist.size() - 1;
        }
        this.playIndex(idx);
    }

    public synchronized void playTrack(LocalMusicTrack track, long startOffsetMs) {
        if (track == null) {
            return;
        }
        int idx = -1;
        for (int i = 0; i < this.playlist.size(); i++) {
            if (this.playlist.get(i).getPath().equals(track.getPath())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            this.playlist.add(track);
            idx = this.playlist.size() - 1;
        }
        this.currentIndex = idx;
        this.startPlayback(track, startOffsetMs);
    }

    public synchronized void syncToTrack(TrackState state) {
        if (state == null || !state.isActive()) {
            if (this.isPlaying) {
                this.stop();
            }
            this.lastSyncStatusText = "ОСТАНОВЛЕНО";
            this.lastSyncStatusColor = 0xFF888888;
            return;
        }
        String targetTitle = state.title().trim();
        String targetArtist = state.artist().trim();
        boolean shouldPlay = state.isPlaying();
        long now = System.currentTimeMillis();
        this.lastSyncTimestamp = now;

        long elapsed = state.updateTime() > 0 ? (now - state.updateTime()) : 0L;
        int ping = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getRelayPingMs();
        long transitEst = ping > 0 ? (ping / 2L) : 35L;
        long targetPos = state.positionMs() + (shouldPlay ? (elapsed + transitEst) : 0L);

        LocalMusicTrack cur = this.getCurrentTrack();
        boolean sameTrack = cur != null && this.matchesTrack(cur, targetTitle, targetArtist);

        if (sameTrack) {
            if (shouldPlay && this.isPaused) {
                this.resume();
            } else if (!shouldPlay && this.isPlaying && !this.isPaused) {
                this.pause();
            }
            long curPos = this.getPositionMs();
            long drift = curPos - targetPos;
            this.lastCalculatedDriftMs = drift;

            if (Math.abs(drift) <= 45L) {
                this.lastSyncStatusText = "ИДЕАЛЬНО (±" + Math.abs(drift) + " мс)";
                this.lastSyncStatusColor = 0xFF00F5D4;
                this.driftAdjustmentMs = 0L;
            } else if (Math.abs(drift) <= 250L) {
                this.lastSyncStatusText = "ПОДСТРОЙКА (" + (drift > 0 ? "+" : "") + drift + " мс)";
                this.lastSyncStatusColor = 0xFFFFAA00;
                this.driftAdjustmentMs = drift;
            } else if (Math.abs(drift) <= 1500L) {
                this.lastSyncStatusText = "СИНХРОНИЗАЦИЯ (" + (drift > 0 ? "+" : "") + drift + " мс)";
                this.lastSyncStatusColor = 0xFFFF7744;
                this.driftAdjustmentMs = drift;
            } else {
                this.lastSyncStatusText = "ВЫРАВНИВАНИЕ (" + (drift > 0 ? "+" : "") + drift + " мс)";
                this.lastSyncStatusColor = 0xFFFF4444;
                this.seekTo(targetPos);
                this.driftAdjustmentMs = 0L;
            }
            return;
        }

        LocalMusicTrack found = this.findMatchingTrack(targetTitle, targetArtist);
        if (found != null) {
            this.lastSyncedMissingKey = "";
            this.playTrack(found, targetPos);
            if (!shouldPlay) {
                this.pause();
            }
            this.lastSyncStatusText = "ТРЕК ЗАГРУЖЕН";
            this.lastSyncStatusColor = 0xFF00F5D4;
        } else {
            String key = (targetArtist + " - " + targetTitle).toLowerCase(Locale.ROOT);
            if (!key.equals(this.lastSyncedMissingKey)) {
                this.lastSyncedMissingKey = key;
                this.lastSyncStatusText = "ПОИСК ПОТОКА...";
                this.lastSyncStatusColor = 0xFFFFAA00;
                SafeModeStreamer.INSTANCE.handleMissingSyncedTrack(state);
            }
        }
    }

    private boolean matchesTrack(LocalMusicTrack track, String title, String artist) {
        if (track == null) {
            return false;
        }
        String t = track.getTitle().trim();
        String a = track.getArtist().trim();
        if (t.equalsIgnoreCase(title) && (artist.isEmpty() || a.equalsIgnoreCase(artist) || a.isEmpty())) {
            return true;
        }
        String fn = track.getPath().getFileName().toString();
        int dot = fn.lastIndexOf('.');
        if (dot > 0) {
            fn = fn.substring(0, dot);
        }
        String normFn = fn.toLowerCase(Locale.ROOT);
        String normT = title.toLowerCase(Locale.ROOT);
        String normA = artist.toLowerCase(Locale.ROOT);
        if (!normT.isEmpty() && normFn.contains(normT)) {
            return true;
        }
        if (normFn.equalsIgnoreCase(normT) || normFn.equalsIgnoreCase(normA + " - " + normT) || normFn.equalsIgnoreCase(normT + " - " + normA)) {
            return true;
        }
        return false;
    }

    private LocalMusicTrack findMatchingTrack(String title, String artist) {
        for (LocalMusicTrack t : this.playlist) {
            if (this.matchesTrack(t, title, artist)) {
                return t;
            }
        }
        long now = System.currentTimeMillis();
        if (now - this.lastMissingScanTime < 5000L) {
            return null;
        }
        this.lastMissingScanTime = now;
        List<LocalMusicTrack> scanned = LocalMusicScanner.INSTANCE.scanTracks();
        for (LocalMusicTrack t : scanned) {
            if (!this.playlist.contains(t)) {
                this.playlist.add(t);
            }
        }
        for (LocalMusicTrack t : this.playlist) {
            if (this.matchesTrack(t, title, artist)) {
                return t;
            }
        }
        return null;
    }

    public synchronized void playIndex(int index) {
        if (this.playlist.isEmpty()) {
            return;
        }
        if (index < 0 || index >= this.playlist.size()) {
            index = 0;
        }
        this.currentIndex = index;
        LocalMusicTrack track = this.playlist.get(index);
        this.startPlayback(track, 0L);
    }

    public synchronized void togglePlayPause() {
        if (this.isPlaying) {
            if (this.isPaused) {
                this.resume();
            } else {
                this.pause();
            }
        } else if (!this.playlist.isEmpty()) {
            this.playIndex(this.currentIndex >= 0 ? this.currentIndex : 0);
        }
    }

    public synchronized void pause() {
        if (this.isPlaying && !this.isPaused) {
            if (this.currentAudioDevice != null) {
                this.currentAudioDevice.pauseLine();
            }
            if (this.currentLine != null) {
                try {
                    this.currentLine.stop();
                } catch (Exception ignored) {}
            }
            this.pausedPositionMs = this.getPositionMs();
            this.isPaused = true;
        }
    }

    public synchronized void resume() {
        if (this.isPlaying && this.isPaused) {
            if (this.currentAudioDevice != null) {
                this.currentAudioDevice.resumeLine();
            }
            if (this.currentLine != null) {
                try {
                    this.currentLine.start();
                } catch (Exception ignored) {}
            }
            this.isPaused = false;
        }
    }

    public synchronized void stop() {
        this.playbackSessionId++;
        this.stopCurrentThread();
        this.isPlaying = false;
        this.isPaused = false;
        this.currentPositionOffsetMs = 0L;
        this.pausedPositionMs = 0L;
    }

    public synchronized void next() {
        if (this.playlist.isEmpty()) {
            return;
        }
        if (this.shuffle && this.playlist.size() > 1) {
            int nextIdx = this.random.nextInt(this.playlist.size());
            if (nextIdx == this.currentIndex) {
                nextIdx = (nextIdx + 1) % this.playlist.size();
            }
            this.playIndex(nextIdx);
        } else {
            int nextIdx = (this.currentIndex + 1) % this.playlist.size();
            this.playIndex(nextIdx);
        }
    }

    public synchronized void previous() {
        if (this.playlist.isEmpty()) {
            return;
        }
        if (this.getPositionMs() > 3000L) {
            this.seekTo(0L);
        } else {
            int prevIdx = (this.currentIndex - 1 + this.playlist.size()) % this.playlist.size();
            this.playIndex(prevIdx);
        }
    }

    public synchronized void seekTo(long targetMs) {
        LocalMusicTrack track = this.getCurrentTrack();
        if (track == null) {
            return;
        }
        boolean wasPaused = this.isPaused;
        long dur = track.getDurationMs();
        if (dur > 0L) {
            targetMs = Math.max(0L, Math.min(dur - 200L, targetMs));
        } else {
            targetMs = Math.max(0L, targetMs);
        }
        this.startPlayback(track, targetMs);
        if (wasPaused) {
            this.isPaused = true;
            this.pausedPositionMs = targetMs;
        }
    }

    public long getPositionMs() {
        if (this.isPaused) {
            return this.pausedPositionMs;
        }
        VolumeAudioDevice dev = this.currentAudioDevice;
        if (dev != null) {
            return this.currentPositionOffsetMs + (long)dev.getPosition();
        }
        SourceDataLine line = this.currentLine;
        if (line != null) {
            return this.currentPositionOffsetMs + (line.getMicrosecondPosition() / 1000L);
        }
        return this.currentPositionOffsetMs;
    }

    public long getDurationMs() {
        LocalMusicTrack track = this.getCurrentTrack();
        return track != null ? track.getDurationMs() : 0L;
    }

    private synchronized void stopCurrentThread() {
        this.shouldStop = true;
        Thread targetThread = this.playbackThread;
        if (targetThread != null) {
            boolean isSelf = Thread.currentThread() == targetThread;
            if (!isSelf) {
                targetThread.interrupt();
            }
            if (this.currentAudioDevice != null) {
                try {
                    this.currentAudioDevice.close();
                } catch (Exception ignored) {}
            }
            if (this.currentBitstream != null) {
                try {
                    this.currentBitstream.close();
                } catch (Exception ignored) {}
            }
            if (this.currentLine != null) {
                try {
                    this.currentLine.stop();
                    this.currentLine.close();
                } catch (Exception ignored) {}
            }
            if (!isSelf) {
                try {
                    targetThread.join(250);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            this.playbackThread = null;
        }
        this.currentBitstream = null;
        this.currentAudioDevice = null;
        this.currentLine = null;
    }

    private synchronized void startPlayback(LocalMusicTrack track, long startOffsetMs) {
        long sessionId = ++this.playbackSessionId;
        this.stopCurrentThread();
        this.isPlaying = true;
        this.isPaused = false;
        this.shouldStop = false;
        this.currentPositionOffsetMs = startOffsetMs;
        this.pausedPositionMs = startOffsetMs;

        Thread t = new Thread(() -> this.runPlaybackLoop(track, startOffsetMs, sessionId), "MusicSubtitles-LocalPlayer");
        t.setDaemon(true);
        this.playbackThread = t;
        t.start();
    }

    private void runPlaybackLoop(LocalMusicTrack track, long startOffsetMs, long sessionId) {
        String fmt = track.getFormat();
        boolean hadError = false;
        long startNs = System.nanoTime();
        try {
            if ("MP3".equalsIgnoreCase(fmt)) {
                this.playMp3Stream(track, startOffsetMs, sessionId);
            } else if ("WAV".equalsIgnoreCase(fmt)) {
                this.playWavStream(track, startOffsetMs, sessionId);
            } else if ("OGG".equalsIgnoreCase(fmt)) {
                this.playOggStream(track, startOffsetMs, sessionId);
            }
        } catch (Exception e) {
            hadError = true;
        }

        if (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop && !Thread.currentThread().isInterrupted()) {
            long elapsedNs = System.nanoTime() - startNs;
            if (hadError && elapsedNs < 1_000_000_000L) {
                ++this.consecutivePlaybackErrors;
                if (this.consecutivePlaybackErrors >= 2 || this.repeat) {
                    CompletableFuture.runAsync(() -> {
                        if (sessionId == this.playbackSessionId) {
                            this.stop();
                        }
                    });
                    return;
                }
            } else {
                this.consecutivePlaybackErrors = 0;
            }
            CompletableFuture.runAsync(() -> {
                if (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop) {
                    this.onTrackEnded();
                }
            });
        }
    }

    private void playMp3Stream(LocalMusicTrack track, long startOffsetMs, long sessionId) throws Exception {
        Bitstream bitstream = null;
        VolumeAudioDevice device = null;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(track.getPath()), 65536)) {
            bitstream = new Bitstream(is);
            Decoder decoder = new Decoder();
            device = new VolumeAudioDevice();
            device.setVolume(this.getEffectiveVolume());
            device.open(decoder);
            this.currentBitstream = bitstream;
            this.currentAudioDevice = device;

            if (startOffsetMs > 0L) {
                double skippedMs = 0.0;
                while (skippedMs < (double)startOffsetMs && sessionId == this.playbackSessionId && !this.shouldStop) {
                    Header h = bitstream.readFrame();
                    if (h == null) {
                        return;
                    }
                    float frameMs = h.ms_per_frame();
                    skippedMs += frameMs > 0.0f ? (double)frameMs : 26.12245;
                    bitstream.closeFrame();
                }
                this.currentPositionOffsetMs = Math.round(skippedMs);
            }

            while (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop && !Thread.currentThread().isInterrupted()) {
                if (this.isPaused) {
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
                Header h = bitstream.readFrame();
                if (h == null) {
                    break;
                }
                SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
                if (output != null && sessionId == this.playbackSessionId && !this.shouldStop) {
                    device.setVolume(this.getEffectiveVolume());
                    device.write(output.getBuffer(), 0, output.getBufferLength());
                    if (this.driftAdjustmentMs > 25L) {
                        try {
                            Thread.sleep(2);
                            this.driftAdjustmentMs -= 2L;
                        } catch (InterruptedException ignored) {}
                    } else if (this.driftAdjustmentMs < -25L) {
                        Header skipH = bitstream.readFrame();
                        if (skipH != null) {
                            float fMs = skipH.ms_per_frame();
                            long frameDur = Math.round(fMs > 0.0f ? fMs : 26.0f);
                            this.currentPositionOffsetMs += frameDur;
                            this.driftAdjustmentMs += frameDur;
                            bitstream.closeFrame();
                        }
                    }
                }
                bitstream.closeFrame();
            }
            if (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop) {
                device.flush();
            }
        } finally {
            if (device != null) {
                try {
                    device.close();
                } catch (Exception ignored) {}
            }
            if (bitstream != null) {
                try {
                    bitstream.close();
                } catch (Exception ignored) {}
            }
            if (sessionId == this.playbackSessionId) {
                this.currentBitstream = null;
                this.currentAudioDevice = null;
            }
        }
    }

    private void playWavStream(LocalMusicTrack track, long startOffsetMs, long sessionId) throws Exception {
        SourceDataLine line = null;
        try (AudioInputStream rawAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(track.getPath()), 65536))) {
            AudioFormat baseFmt = rawAis.getFormat();
            AudioFormat pcmFmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFmt.getSampleRate(),
                    16,
                    baseFmt.getChannels(),
                    baseFmt.getChannels() * 2,
                    baseFmt.getSampleRate(),
                    false
            );
            try (AudioInputStream pcmAis = AudioSystem.getAudioInputStream(pcmFmt, rawAis)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmFmt);
                line = (SourceDataLine)AudioSystem.getLine(info);
                line.open(pcmFmt);
                if (!this.isPaused) {
                    line.start();
                }
                this.currentLine = line;

                if (startOffsetMs > 0L) {
                    int frameSize = Math.max(1, pcmFmt.getFrameSize());
                    long framesToSkip = (long)((startOffsetMs / 1000.0) * pcmFmt.getFrameRate());
                    long bytesRemaining = framesToSkip * (long)frameSize;
                    byte[] skipBuf = new byte[frameSize * 2048];
                    while (bytesRemaining > 0L && sessionId == this.playbackSessionId && !this.shouldStop) {
                        int toRead = (int)Math.min((long)skipBuf.length, bytesRemaining);
                        toRead = (toRead / frameSize) * frameSize;
                        if (toRead <= 0) {
                            break;
                        }
                        int r = pcmAis.read(skipBuf, 0, toRead);
                        if (r <= 0) {
                            break;
                        }
                        bytesRemaining -= r;
                    }
                }

                byte[] buffer = new byte[4096];
                int read;
                while (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop && !Thread.currentThread().isInterrupted()) {
                    if (this.isPaused) {
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            break;
                        }
                        continue;
                    }
                    read = pcmAis.read(buffer, 0, buffer.length);
                    if (read == -1) {
                        break;
                    }
                    applySoftwareVolume(buffer, read, this.getEffectiveVolume());
                    line.write(buffer, 0, read);
                }
                if (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop) {
                    line.drain();
                }
            }
        } finally {
            if (line != null) {
                try {
                    line.close();
                } catch (Exception ignored) {}
            }
            if (sessionId == this.playbackSessionId) {
                this.currentLine = null;
            }
        }
    }

    private void playOggStream(LocalMusicTrack track, long startOffsetMs, long sessionId) throws Exception {
        ByteBuffer nativeOggData = null;
        long dec = 0L;
        SourceDataLine line = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            try (SeekableByteChannel ch = Files.newByteChannel(track.getPath(), StandardOpenOption.READ)) {
                long fileSize = ch.size();
                if (fileSize <= 0L || fileSize > Integer.MAX_VALUE) {
                    return;
                }
                nativeOggData = MemoryUtil.memAlloc((int)fileSize);
                while (nativeOggData.hasRemaining()) {
                    if (ch.read(nativeOggData) <= 0) {
                        break;
                    }
                }
                nativeOggData.flip();
            }
            if (!nativeOggData.hasRemaining()) {
                return;
            }
            IntBuffer err = stack.mallocInt(1);
            dec = STBVorbis.stb_vorbis_open_memory(nativeOggData, err, null);
            if (dec == 0L) {
                return;
            }
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.nstb_vorbis_get_info(dec, info.address());
            int channels = Math.max(1, info.channels());
            int sampleRate = Math.max(8000, info.sample_rate());

            AudioFormat pcmFmt = new AudioFormat((float)sampleRate, 16, channels, true, false);
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, pcmFmt);
            line = (SourceDataLine)AudioSystem.getLine(lineInfo);
            line.open(pcmFmt);
            if (!this.isPaused) {
                line.start();
            }
            this.currentLine = line;

            if (startOffsetMs > 0L) {
                int targetSample = (int)((startOffsetMs / 1000.0) * sampleRate);
                STBVorbis.stb_vorbis_seek_frame(dec, targetSample);
            }

            short[] pcmBuffer = new short[2048 * channels];
            byte[] byteBuffer = new byte[pcmBuffer.length * 2];

            while (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop && !Thread.currentThread().isInterrupted()) {
                if (this.isPaused) {
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
                int samples = STBVorbis.stb_vorbis_get_samples_short_interleaved(dec, channels, pcmBuffer);
                if (samples <= 0) {
                    break;
                }
                int totalShorts = samples * channels;
                float vol = this.getEffectiveVolume();
                for (int i = 0; i < totalShorts; i++) {
                    int val = Math.round((float)pcmBuffer[i] * vol);
                    if (val > 32767) {
                        val = 32767;
                    } else if (val < -32768) {
                        val = -32768;
                    }
                    byteBuffer[i * 2] = (byte)(val & 0xFF);
                    byteBuffer[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
                }
                line.write(byteBuffer, 0, totalShorts * 2);
            }
            if (sessionId == this.playbackSessionId && this.isPlaying && !this.shouldStop) {
                line.drain();
            }
        } finally {
            if (dec != 0L) {
                try {
                    STBVorbis.stb_vorbis_close(dec);
                } catch (Exception ignored) {}
            }
            if (nativeOggData != null) {
                MemoryUtil.memFree(nativeOggData);
            }
            if (line != null) {
                try {
                    line.close();
                } catch (Exception ignored) {}
            }
            if (sessionId == this.playbackSessionId) {
                this.currentLine = null;
            }
        }
    }

    private static void applySoftwareVolume(byte[] buf, int len, float vol) {
        if (vol == 1.0f) {
            return;
        }
        for (int i = 0; i < len - 1; i += 2) {
            int sample = (short)((buf[i] & 0xFF) | ((buf[i + 1] & 0xFF) << 8));
            int adjusted = Math.round((float)sample * vol);
            if (adjusted > 32767) {
                adjusted = 32767;
            } else if (adjusted < -32768) {
                adjusted = -32768;
            }
            buf[i] = (byte)(adjusted & 0xFF);
            buf[i + 1] = (byte)((adjusted >> 8) & 0xFF);
        }
    }

    private synchronized void onTrackEnded() {
        if (this.repeat) {
            this.playIndex(this.currentIndex);
        } else if (this.shuffle && this.playlist.size() > 1) {
            int nextIdx = this.random.nextInt(this.playlist.size());
            if (nextIdx == this.currentIndex) {
                nextIdx = (nextIdx + 1) % this.playlist.size();
            }
            this.playIndex(nextIdx);
        } else if (this.currentIndex + 1 < this.playlist.size()) {
            this.playIndex(this.currentIndex + 1);
        } else {
            this.stop();
        }
    }

    public synchronized TrackState pollState(CoverTextureManager coverManager) {
        if ((this.isPlaying || this.isPaused) && this.getCurrentTrack() != null) {
            LocalMusicTrack tr = this.getCurrentTrack();
            if (tr != this.lastReportedTrack) {
                this.lastReportedTrack = tr;
                if (coverManager != null) {
                    if (tr.getCoverArt() != null && tr.getCoverArt().length > 0) {
                        try {
                            coverManager.updateCover(Base64.getEncoder().encodeToString(tr.getCoverArt()));
                        } catch (Exception ignored) {
                            coverManager.resetToDefault();
                        }
                    } else if (!com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.hasSyncedTrack()) {
                        coverManager.resetToDefault();
                    }
                }
            }
            long pos = this.getPositionMs();
            long dur = this.getDurationMs();
            String status = (this.isPlaying && !this.isPaused) ? "playing" : "paused";
            return new TrackState(true, tr.getTitle(), tr.getArtist(), tr.getAlbum(), status, pos, dur, null, System.currentTimeMillis(), "local", "Моя музыка", "");
        }
        return null;
    }
}
