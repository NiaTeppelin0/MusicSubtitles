package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.network.TrackState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LiveAudioSubtitleGenerator {
    private static final String[][] FAST_VOCAL_PHRASES = new String[][]{
        {"\u266a", "\u0433\u043e\u043b\u043e\u0441", "\u0432", "\u0440\u0438\u0442\u043c\u0435", "\u266a", "\u0442\u0430\u043a\u0442", "\u266b"},
        {"\u266b", "\u0432\u043e\u043a\u0430\u043b", "\u2022", "\u043f\u043e\u0442\u043e\u043a", "\u0441\u043b\u043e\u0432", "\u266a", "\u266b"},
        {"\u266a", "\u0447\u0438\u0442\u043a\u0430", "\u0432", "\u0431\u0438\u0442", "\u2022", "\u0440\u0438\u0442\u043c", "\u266b"}
    };
    private static final String[][] MELODIC_VOCAL_PHRASES = new String[][]{
        {"\u266a", "\u0436\u0438\u0432\u043e\u0439", "\u0432\u043e\u043a\u0430\u043b", "\u266b"},
        {"\u266b", "\u0432\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f", "\u043b\u0438\u043d\u0438\u044f", "\u266a"},
        {"\u266a", "\u0433\u043e\u043b\u043e\u0441", "\u0438", "\u043c\u0435\u043b\u043e\u0434\u0438\u044f", "\u266b"}
    };
    private static final String[][] BASS_DROP_PHRASES = new String[][]{
        {"\u266b", "\u0431\u0430\u0441", "\u2022", "\u0443\u0434\u0430\u0440", "\u266a", "\u0440\u0438\u0442\u043c", "\u266b"},
        {"\u266a", "\u043f\u043b\u043e\u0442\u043d\u044b\u0439", "\u0431\u0438\u0442", "\u266b"},
        {"\u266b", "\u0434\u0440\u043e\u043f", "\u0431\u0438\u0442\u0430", "\u266a"}
    };
    private static final String[][] GROOVE_PHRASES = new String[][]{
        {"\u266a", "\u0436\u0438\u0432\u043e\u0439", "\u0440\u0438\u0442\u043c", "\u266b"},
        {"\u266b", "\u043c\u0435\u043b\u043e\u0434\u0438\u044f", "\u0432", "\u0442\u0430\u043a\u0442", "\u266a"},
        {"\u266a", "\u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u0430\u043b", "\u2022", "\u0437\u0432\u0443\u043a", "\u266b"}
    };

    private final LyricsManager lyricsManager;
    private volatile boolean captureInFlight = false;
    private volatile long lastCaptureTimestampMs = 0L;
    private volatile String activeTrackKey = "";
    private volatile int phraseCounter = 0;

    public LiveAudioSubtitleGenerator(LyricsManager lyricsManager) {
        this.lyricsManager = lyricsManager;
    }

    public void resetForTrack(String trackKey) {
        this.activeTrackKey = trackKey != null ? trackKey : "";
        this.phraseCounter = 0;
        this.lastCaptureTimestampMs = 0L;
    }

    public boolean isCapturingOrActive() {
        return this.captureInFlight || (this.lastCaptureTimestampMs > 0L && System.currentTimeMillis() - this.lastCaptureTimestampMs < 6000L);
    }

    public void tickIfNeeded(TrackState state, String trackKey) {
        if (state == null || !state.isPlaying() || trackKey == null || trackKey.isEmpty() || this.captureInFlight) {
            return;
        }
        if (!trackKey.equals(this.activeTrackKey)) {
            this.resetForTrack(trackKey);
        }
        long now = System.currentTimeMillis();
        if (now - this.lastCaptureTimestampMs < 2700L) {
            return;
        }
        this.captureInFlight = true;
        this.lastCaptureTimestampMs = now;
        try {
            CompletableFuture.runAsync(() -> this.captureAndGenerate(state, trackKey), this.lyricsManager.getExecutor());
        } catch (Exception e) {
            this.captureInFlight = false;
        }
    }

    private void captureAndGenerate(TrackState state, String trackKey) {
        try {
            if (trackKey == null || !trackKey.equals(this.lyricsManager.getCurrentTrackKey()) || !state.isPlaying()) {
                return;
            }
            int port = this.lyricsManager.resolveHelperPort();
            if (port <= 0) {
                return;
            }
            String helperBase = "http://127.0.0.1:" + port;
            int captureDurMs = 2400;
            String startResp = LyricsManager.httpPost(helperBase + "/capture/start", "{\"durationMs\":" + captureDurMs + "}", 2000);
            if (startResp == null) {
                return;
            }
            long captureStartPos = state.getInterpolatedPositionMs();
            for (int elapsed = 0; elapsed < captureDurMs + 250; elapsed += 150) {
                if (!trackKey.equals(this.lyricsManager.getCurrentTrackKey()) || !state.isPlaying()) {
                    LyricsManager.httpPost(helperBase + "/capture/stop", "", 1000);
                    return;
                }
                Thread.sleep(150L);
            }
            if (!trackKey.equals(this.lyricsManager.getCurrentTrackKey()) || !state.isPlaying()) {
                return;
            }
            byte[] wav = LyricsManager.httpGetBytes(helperBase + "/capture/result", 3000);
            if (wav == null || wav.length <= 44) {
                return;
            }
            AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wav);
            if (pcm == null || pcm.samples() == null || pcm.sampleRate() <= 0) {
                return;
            }
            LyricLine synthesized = this.synthesizeLineFromPcm(pcm.samples(), pcm.sampleRate(), captureStartPos, captureDurMs);
            if (synthesized != null) {
                this.lyricsManager.appendGeneratedLiveLine(trackKey, synthesized);
            }
        } catch (Exception ignored) {
        } finally {
            this.captureInFlight = false;
            this.lastCaptureTimestampMs = System.currentTimeMillis();
        }
    }

    public LyricLine synthesizeLineFromPcm(float[] samples, int sampleRate, long startPosMs, long windowMs) {
        if (samples == null || sampleRate <= 0 || samples.length < Math.max(800, sampleRate / 4)) {
            return null;
        }
        double sumSqAll = 0.0;
        for (int i = 0; i < samples.length; ++i) {
            double v = samples[i];
            sumSqAll += v * v;
        }
        double rms = Math.sqrt(sumSqAll / (double)samples.length);
        if (rms < 0.0035) {
            return null;
        }
        long sampleDurMs = Math.round((double)samples.length * 1000.0 / (double)sampleRate);
        long actualWindowMs = windowMs > 0L ? Math.max(800L, Math.min(4500L, sampleDurMs > 0L ? sampleDurMs : windowMs)) : Math.max(800L, sampleDurMs);

        float[] vocalSamples = StudioCascadeAligner.applyVocalBandpassFilter(samples, sampleRate);
        int hop = Math.max(1, (int)((double)sampleRate * 0.02));
        int totalFrames = samples.length / hop;
        if (totalFrames < 6) {
            return null;
        }

        double[] bassEnv = new double[totalFrames];
        double[] vocalEnv = new double[totalFrames];
        double[] highEnv = new double[totalFrames];
        double lpAlpha = Math.max(0.01, Math.min(0.45, (2.0 * Math.PI * 160.0) / (double)sampleRate));
        double lpState = 0.0;
        double prevSample = samples[0];
        double bassSum = 0.0;
        double vocalSum = 0.0;
        double highSum = 0.0;
        double vocalFluxSum = 0.0;

        for (int f = 0; f < totalFrames; ++f) {
            int base = f * hop;
            double bSq = 0.0;
            double vSq = 0.0;
            double hSq = 0.0;
            for (int k = 0; k < hop; ++k) {
                double x = samples[base + k];
                lpState += lpAlpha * (x - lpState);
                bSq += lpState * lpState;
                double vx = vocalSamples[base + k];
                vSq += vx * vx;
                double hx = x - prevSample;
                prevSample = x;
                hSq += hx * hx;
            }
            double br = Math.sqrt(bSq / (double)hop);
            double vr = Math.sqrt(vSq / (double)hop);
            double hr = Math.sqrt(hSq / (double)hop);
            bassEnv[f] = br;
            vocalEnv[f] = vr;
            highEnv[f] = hr;
            bassSum += br;
            vocalSum += vr;
            highSum += hr;
            if (f > 0) {
                vocalFluxSum += Math.abs(vr - vocalEnv[f - 1]);
            }
        }

        double meanBass = bassSum / (double)totalFrames;
        double meanVocal = vocalSum / (double)totalFrames;
        double vocalFlux = vocalFluxSum / ((double)(totalFrames - 1) * Math.max(1.0E-5, meanVocal));

        double[] bSort = Arrays.copyOf(bassEnv, totalFrames);
        double[] vSort = Arrays.copyOf(vocalEnv, totalFrames);
        double[] hSort = Arrays.copyOf(highEnv, totalFrames);
        Arrays.sort(bSort);
        Arrays.sort(vSort);
        Arrays.sort(hSort);
        double bP95 = Math.max(1.0E-5, bSort[Math.min(totalFrames - 1, (int)((double)totalFrames * 0.95))]);
        double vP95 = Math.max(1.0E-5, vSort[Math.min(totalFrames - 1, (int)((double)totalFrames * 0.95))]);
        double hP95 = Math.max(1.0E-5, hSort[Math.min(totalFrames - 1, (int)((double)totalFrames * 0.95))]);

        double[] actEnv = new double[totalFrames];
        for (int f = 0; f < totalFrames; ++f) {
            double bn = Math.min(1.5, bassEnv[f] / bP95);
            double vn = Math.min(1.5, vocalEnv[f] / vP95);
            double hn = Math.min(1.5, highEnv[f] / hP95);
            actEnv[f] = 0.58 * vn + 0.25 * hn + 0.17 * bn;
        }

        double[] aSort = Arrays.copyOf(actEnv, totalFrames);
        Arrays.sort(aSort);
        double p25 = aSort[(int)((double)totalFrames * 0.25)];
        double p95 = aSort[Math.min(totalFrames - 1, (int)((double)totalFrames * 0.95))];
        double th = p25 + 0.28 * Math.max(1.0E-5, p95 - p25);

        List<Integer> peakFrames = new ArrayList<Integer>();
        int lastPeak = -14;
        for (int f = 1; f < totalFrames - 1 && peakFrames.size() < 7; ++f) {
            if (f - lastPeak >= 14 && actEnv[f] >= th && actEnv[f] >= actEnv[f - 1] && actEnv[f] >= actEnv[f + 1]) {
                peakFrames.add(f);
                lastPeak = f;
            }
        }

        List<long[]> wordSpans = new ArrayList<long[]>();
        if (peakFrames.size() >= 2) {
            for (int idx = 0; idx < peakFrames.size(); ++idx) {
                long wStart = idx == 0 ? 0L : Math.round((double)peakFrames.get(idx) * 20.0);
                long wEnd = idx + 1 < peakFrames.size()
                    ? Math.round((double)peakFrames.get(idx + 1) * 20.0)
                    : actualWindowMs;
                wStart = Math.max(0L, Math.min(actualWindowMs - 150L, wStart));
                wEnd = Math.max(wStart + 150L, Math.min(actualWindowMs, wEnd));
                wordSpans.add(new long[]{wStart, wEnd});
            }
        } else {
            int count = actualWindowMs >= 2000L ? 4 : 3;
            long step = actualWindowMs / (long)count;
            for (int idx = 0; idx < count; ++idx) {
                long wStart = (long)idx * step;
                long wEnd = idx + 1 == count ? actualWindowMs : (long)(idx + 1) * step;
                wordSpans.add(new long[]{wStart, Math.max(wStart + 150L, wEnd)});
            }
        }

        boolean vocalDom = meanVocal > meanBass * 0.58 && vocalFlux >= 0.08;
        boolean bassDom = !vocalDom && meanBass >= meanVocal * 1.10;
        String[][] palette;
        if (vocalDom && wordSpans.size() >= 5) {
            palette = FAST_VOCAL_PHRASES;
        } else if (vocalDom) {
            palette = MELODIC_VOCAL_PHRASES;
        } else if (bassDom) {
            palette = BASS_DROP_PHRASES;
        } else {
            palette = GROOVE_PHRASES;
        }

        int seq = this.phraseCounter++;
        String[] template = palette[Math.floorMod(seq, palette.length)];
        String[] chosenTokens = new String[wordSpans.size()];
        for (int i = 0; i < wordSpans.size(); ++i) {
            chosenTokens[i] = template[i % template.length];
        }

        StringBuilder sb = new StringBuilder();
        List<LyricLine.Word> words = new ArrayList<LyricLine.Word>(wordSpans.size());
        for (int i = 0; i < wordSpans.size(); ++i) {
            if (i > 0) {
                sb.append(' ');
            }
            int charStart = sb.length();
            String tok = chosenTokens[i];
            sb.append(tok);
            int charEnd = sb.length();
            long[] span = wordSpans.get(i);
            words.add(new LyricLine.Word(startPosMs + span[0], startPosMs + span[1], tok, charStart, charEnd));
        }
        String fullText = sb.toString().trim();
        if (fullText.isEmpty() || words.isEmpty()) {
            return null;
        }
        long lastWordEnd = startPosMs + wordSpans.get(wordSpans.size() - 1)[1];
        return new LyricLine(startPosMs, startPosMs + actualWindowMs, Math.max(startPosMs + 150L, lastWordEnd), fullText, words);
    }
}
