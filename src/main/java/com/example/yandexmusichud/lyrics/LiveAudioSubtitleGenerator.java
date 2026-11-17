package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.network.TrackState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class LiveAudioSubtitleGenerator {
    private static final String GOOGLE_SPEECH_URL_PREFIX = "https://www.google.com/speech-api/v2/recognize?output=json&client=chromium&pFilter=0&key=";
    private static final int CAPTURE_DURATION_MS = 1800;
    private static final int SLIDING_TAIL_MS = 1400;
    private static final long INTER_CAPTURE_GAP_MS = 0L;
    private static final long MAX_RESULT_STALENESS_MS = 5600L;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern EDGE_PUNCT = Pattern.compile("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$");
    private static final Set<String> HALLUCINATION_PHRASES = Set.of(
        "thanks for watching",
        "thank you for watching",
        "thank you",
        "thanks",
        "subscribe",
        "like and subscribe",
        "subtitles by",
        "captions by",
        "translated by",
        "amara org",
        "you",
        "bye",
        "okay",
        "music",
        "music playing",
        "instrumental",
        "субтитры",
        "субтитры создавал",
        "субтитры сделал",
        "редактор субтитров",
        "автор субтитров",
        "продолжение следует",
        "подписывайтесь",
        "подпишись на канал",
        "подписывайтесь на канал",
        "не забудьте подписаться",
        "спасибо за просмотр",
        "благодарю за просмотр",
        "ставьте лайк",
        "ставь лайк",
        "музыка",
        "dimatorzok"
    );

    private record RecognitionResult(
        String text,
        double confidence,
        String lang,
        double langProb,
        String source,
        List<LocalWhisperEngine.WordSpan> wordSpans
    ) {
        RecognitionResult(String text, double confidence, String lang) {
            this(text, confidence, lang, 0.0, "Google", Collections.emptyList());
        }
    }

    public record VerificationRecognition(String text, double confidence, String lang, double activeFraction) {
    }

    private record AnalyzedSpeech(RecognitionResult result, double[] smoothActivity, double activeFraction, int tailSamples) {
    }

    private final LyricsManager lyricsManager;
    private final HttpClient httpClient;
    private final ExecutorService sttExecutor;
    private final LocalWhisperEngine whisperEngine;
    private final AtomicInteger sttInFlight = new AtomicInteger(0);
    private final AtomicLong trackGeneration = new AtomicLong(0L);
    private final Object tailLock = new Object();
    private float[] slidingTailSamples = new float[0];
    private int slidingTailSampleRate = 0;
    private String slidingTailTrackKey = "";
    private volatile boolean captureInFlight = false;
    private volatile long lastCaptureTimestampMs = 0L;
    private volatile long lastCaptureEndedMs = 0L;
    private volatile String activeTrackKey = "";
    private volatile String stickyLang = "";
    private volatile double stickyLangConfidence = 0.0;
    private volatile String lastEmittedNorm = "";
    private volatile List<String> lastEmittedWords = Collections.emptyList();
    private volatile long lastEmittedAtMs = 0L;
    private volatile long lastEmittedCaptureMs = 0L;
    private volatile String statusLabel = "Ожидание трека";

    public LiveAudioSubtitleGenerator(LyricsManager lyricsManager) {
        this.lyricsManager = lyricsManager;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.whisperEngine = new LocalWhisperEngine();
        this.sttExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MusicSubtitles-LiveSTT");
            t.setDaemon(true);
            return t;
        });
    }

    public LocalWhisperEngine getWhisperEngine() {
        return this.whisperEngine;
    }

    public void resetForTrack(String trackKey) {
        this.trackGeneration.incrementAndGet();
        this.activeTrackKey = trackKey != null ? trackKey : "";
        this.stickyLang = "";
        this.stickyLangConfidence = 0.0;
        this.lastEmittedNorm = "";
        this.lastEmittedWords = Collections.emptyList();
        this.lastEmittedAtMs = 0L;
        this.lastEmittedCaptureMs = 0L;
        this.lastCaptureTimestampMs = 0L;
        this.lastCaptureEndedMs = 0L;
        this.captureInFlight = false;
        synchronized (this.tailLock) {
            this.slidingTailSamples = new float[0];
            this.slidingTailSampleRate = 0;
            this.slidingTailTrackKey = "";
        }
        this.statusLabel = !HudConfig.INSTANCE.liveSubtitlesEnabled
            ? "Выкл"
            : (this.activeTrackKey.isEmpty() ? "Ожидание трека" : "Инициализация захвата");
    }

    public void disableAndStop() {
        boolean wasActive = this.captureInFlight || this.sttInFlight.get() > 0 || !this.activeTrackKey.isEmpty();
        this.resetForTrack("");
        this.statusLabel = "Выкл";
        this.whisperEngine.stopServer();
        if (wasActive && this.lyricsManager != null) {
            this.lyricsManager.getExecutor().execute(() -> {
                try {
                    int port = this.lyricsManager.resolveHelperPort();
                    if (port > 0) {
                        LyricsManager.httpPost("http://127.0.0.1:" + port + "/capture/stop", "", 800);
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    public boolean isCapturingOrActive() {
        if (!HudConfig.INSTANCE.liveSubtitlesEnabled) {
            return false;
        }
        long now = System.currentTimeMillis();
        return this.captureInFlight || this.sttInFlight.get() > 0 || (this.lastCaptureTimestampMs > 0L && now - this.lastCaptureTimestampMs < 6500L);
    }

    public String getStatusLabel() {
        return HudConfig.INSTANCE.liveSubtitlesEnabled ? this.statusLabel : "Выкл";
    }

    public void shutdown() {
        this.whisperEngine.shutdown();
        this.sttExecutor.shutdownNow();
    }

    public void tickIfNeeded(TrackState state, String trackKey) {
        if (!HudConfig.INSTANCE.liveSubtitlesEnabled) {
            if (this.captureInFlight || !this.activeTrackKey.isEmpty() || this.whisperEngine.isReady()) {
                this.disableAndStop();
            }
            return;
        }
        this.maybeEnsureWhisper();
        if (state == null || !state.isPlaying() || trackKey == null || trackKey.isEmpty() || this.captureInFlight) {
            return;
        }
        if (!trackKey.equals(this.activeTrackKey)) {
            this.resetForTrack(trackKey);
        }
        if (this.sttInFlight.get() >= 2) {
            return;
        }
        long now = System.currentTimeMillis();
        if (INTER_CAPTURE_GAP_MS > 0L && this.lastCaptureEndedMs > 0L && now - this.lastCaptureEndedMs < INTER_CAPTURE_GAP_MS) {
            return;
        }
        if (this.lastCaptureTimestampMs > 0L && now - this.lastCaptureTimestampMs < 300L) {
            return;
        }
        long gen = this.trackGeneration.get();
        this.captureInFlight = true;
        this.lastCaptureTimestampMs = now;
        this.lyricsManager.getExecutor().execute(() -> this.captureLoop(state, trackKey, gen));
    }

    public static boolean hasGoogleSpeechKey() {
        return HudConfig.INSTANCE != null
            && HudConfig.INSTANCE.googleSpeechApiKey != null
            && !HudConfig.INSTANCE.googleSpeechApiKey.trim().isEmpty();
    }

    private void maybeEnsureWhisper() {
        String mode = resolveSttMode();
        if (!"GOOGLE".equals(mode) && LocalWhisperEngine.isSupportedPlatform()) {
            this.whisperEngine.ensureReadyAsync();
        }
    }

    private static String resolveSttMode() {
        String raw = HudConfig.INSTANCE != null ? HudConfig.INSTANCE.liveSttEngine : "AUTO";
        if (raw == null || raw.isBlank()) {
            return "AUTO";
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT);
        if ("WHISPER".equals(norm)) {
            return norm;
        }
        if ("GOOGLE".equals(norm)) {
            return hasGoogleSpeechKey() ? "GOOGLE" : "AUTO";
        }
        return "AUTO";
    }

    private void captureLoop(TrackState state, String trackKey, long gen) {
        try {
            if (!HudConfig.INSTANCE.liveSubtitlesEnabled || gen != this.trackGeneration.get()) {
                return;
            }
            int port = this.lyricsManager.resolveHelperPort();
            if (port <= 0) {
                this.statusLabel = "Helper не запущен";
                this.lastCaptureEndedMs = System.currentTimeMillis() + 1800L;
                return;
            }
            String helperBase = "http://127.0.0.1:" + port;
            String startResp = LyricsManager.httpPost(helperBase + "/capture/start", "{\"durationMs\":" + CAPTURE_DURATION_MS + "}", 1500);
            if (startResp == null) {
                this.statusLabel = "Захват занят / недоступен";
                this.lastCaptureEndedMs = System.currentTimeMillis() + 500L;
                return;
            }
            String whisperInit = this.whisperEngine.getStatusText();
            this.statusLabel = (!this.whisperEngine.isReady() && whisperInit != null && !whisperInit.isEmpty() && whisperInit.startsWith("Загрузка"))
                ? whisperInit
                : "Слушаю аудио...";
            long captureStartPos = state.getInterpolatedPositionMs();
            for (int elapsed = 0; elapsed < CAPTURE_DURATION_MS + 35; elapsed += 60) {
                if (!HudConfig.INSTANCE.liveSubtitlesEnabled || gen != this.trackGeneration.get() || !trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
                    LyricsManager.httpPost(helperBase + "/capture/stop", "", 800);
                    this.statusLabel = HudConfig.INSTANCE.liveSubtitlesEnabled ? "Пауза / смена трека" : "Выкл";
                    return;
                }
                Thread.sleep(60L);
            }
            if (!HudConfig.INSTANCE.liveSubtitlesEnabled || gen != this.trackGeneration.get() || !trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
                return;
            }
            byte[] wav = LyricsManager.httpGetBytes(helperBase + "/capture/result", 2500);
            if ((wav == null || wav.length <= 44) && HudConfig.INSTANCE.liveSubtitlesEnabled && gen == this.trackGeneration.get()) {
                Thread.sleep(90L);
                wav = LyricsManager.httpGetBytes(helperBase + "/capture/result", 2500);
            }
            long capturedAtMs = System.currentTimeMillis();
            this.lastCaptureEndedMs = capturedAtMs;
            if (!HudConfig.INSTANCE.liveSubtitlesEnabled || gen != this.trackGeneration.get() || !trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
                return;
            }
            if (wav == null || wav.length <= 44) {
                this.statusLabel = "Пустой буфер аудио";
                return;
            }
            byte[] finalWav = wav;
            this.captureInFlight = false;
            this.sttInFlight.incrementAndGet();
            this.sttExecutor.execute(() -> {
                try {
                    this.processCapturedWav(finalWav, trackKey, state, captureStartPos, CAPTURE_DURATION_MS, gen, capturedAtMs);
                } finally {
                    this.sttInFlight.decrementAndGet();
                }
            });
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            this.statusLabel = "Ошибка захвата";
        } finally {
            this.captureInFlight = false;
        }
    }

    public VerificationRecognition verifyAudioSnippet(float[] rawSamples, int sampleRate, TrackState state) {
        if (rawSamples == null || sampleRate <= 0 || rawSamples.length < sampleRate) {
            return null;
        }
        AnalyzedSpeech analyzed = this.analyzeSpeechWindow(rawSamples, rawSamples, 0, sampleRate, state, 0.0010, 0.00055, 0.0022, 0.13, false);
        if (analyzed == null || analyzed.result() == null || analyzed.result().text() == null || analyzed.result().text().isEmpty()) {
            return null;
        }
        RecognitionResult res = analyzed.result();
        this.updateStickyLang(res);
        return new VerificationRecognition(res.text(), res.confidence(), res.lang(), analyzed.activeFraction());
    }

    private void updateStickyLang(RecognitionResult res) {
        if (res == null || res.lang() == null || res.lang().isEmpty() || "auto".equalsIgnoreCase(res.lang())) {
            return;
        }
        double score = res.langProb() > 0.0 ? res.langProb() : res.confidence();
        if (score >= 0.78 && score >= this.stickyLangConfidence - 0.05) {
            this.stickyLang = res.lang();
            this.stickyLangConfidence = Math.max(this.stickyLangConfidence, score);
        }
    }

    private float[] combineWithSlidingTail(float[] freshSamples, int sampleRate, String trackKey, int[] tailCountOut) {
        int maxTailSamples = Math.max(0, (int)Math.round((double)sampleRate * ((double)SLIDING_TAIL_MS / 1000.0)));
        float[] combined;
        synchronized (this.tailLock) {
            float[] prev = (trackKey != null && trackKey.equals(this.slidingTailTrackKey) && this.slidingTailSampleRate == sampleRate)
                ? this.slidingTailSamples
                : new float[0];
            int tailLen = prev.length;
            tailCountOut[0] = tailLen;
            combined = new float[tailLen + freshSamples.length];
            if (tailLen > 0) {
                System.arraycopy(prev, 0, combined, 0, tailLen);
            }
            System.arraycopy(freshSamples, 0, combined, tailLen, freshSamples.length);

            int nextTailLen = Math.min(freshSamples.length, maxTailSamples);
            float[] nextTail = nextTailLen > 0
                ? Arrays.copyOfRange(freshSamples, freshSamples.length - nextTailLen, freshSamples.length)
                : new float[0];
            this.slidingTailSamples = nextTail;
            this.slidingTailSampleRate = sampleRate;
            this.slidingTailTrackKey = trackKey != null ? trackKey : "";
        }
        return combined;
    }

    private AnalyzedSpeech analyzeSpeechWindow(
        float[] freshSamples,
        float[] windowSamples,
        int tailSampleCount,
        int sampleRate,
        TrackState state,
        double minRawRms,
        double minBandRms,
        double minBandPeak,
        double minActiveFraction,
        boolean updateStatusLabel
    ) {
        double rawRms = computeRms(freshSamples);
        if (rawRms < minRawRms) {
            if (updateStatusLabel) {
                this.statusLabel = "Тишина / пауза";
            }
            return null;
        }
        float[] speechBand = AcousticLagEstimator.extractSpeechBand(freshSamples, sampleRate);
        double bandRms = computeRms(speechBand);
        double bandPeak = computePeak(speechBand);
        if (bandRms < minBandRms || bandPeak < minBandPeak) {
            if (updateStatusLabel) {
                this.statusLabel = "Инструментал / низкий сигнал";
            }
            return null;
        }
        double[][] features = AcousticLagEstimator.audioFeatures(freshSamples, sampleRate);
        double[] smoothActivity = features.length > 0 ? features[0] : new double[0];
        double activeFraction = computeActiveFraction(smoothActivity, 0.14);
        if (activeFraction < minActiveFraction) {
            if (updateStatusLabel) {
                this.statusLabel = "Инструментальная секция";
            }
            return null;
        }

        if (updateStatusLabel) {
            String ws = this.whisperEngine.getStatusText();
            if (!this.whisperEngine.isReady() && ws != null && !ws.isEmpty() && ws.startsWith("Загрузка")) {
                this.statusLabel = ws;
            } else {
                this.statusLabel = this.whisperEngine.isReady() && !"GOOGLE".equals(resolveSttMode())
                    ? "Whisper распознавание..."
                    : "Распознавание речи...";
            }
        }
        RecognitionResult best = this.recognizeBest(windowSamples, sampleRate, state, rawRms, activeFraction);
        if (best == null || best.text() == null || best.text().isEmpty()) {
            if (updateStatusLabel) {
                this.statusLabel = activeFraction > 0.2 ? "Речь не разобрана (инструментал)" : "Инструментальная секция";
            }
            return null;
        }
        return new AnalyzedSpeech(best, smoothActivity, activeFraction, tailSampleCount);
    }

    private void processCapturedWav(byte[] wav, String trackKey, TrackState state, long captureStartPos, long captureDurMs, long gen, long capturedAtMs) {
        if (!HudConfig.INSTANCE.liveSubtitlesEnabled || gen != this.trackGeneration.get() || !trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
            return;
        }
        if (System.currentTimeMillis() - capturedAtMs > MAX_RESULT_STALENESS_MS) {
            this.statusLabel = "Пропуск устаревшего фрагмента";
            return;
        }
        AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wav);
        if (pcm == null || pcm.samples() == null || pcm.sampleRate() <= 0) {
            return;
        }
        float[] freshSamples = pcm.samples();
        int sampleRate = pcm.sampleRate();
        if (freshSamples.length < sampleRate / 2) {
            return;
        }
        int[] tailCountHolder = new int[1];
        float[] windowSamples = this.combineWithSlidingTail(freshSamples, sampleRate, trackKey, tailCountHolder);
        AnalyzedSpeech analyzed = this.analyzeSpeechWindow(freshSamples, windowSamples, tailCountHolder[0], sampleRate, state, 0.0010, 0.00055, 0.0022, 0.11, true);
        if (analyzed == null) {
            return;
        }
        RecognitionResult best = analyzed.result();
        long now = System.currentTimeMillis();
        if (!HudConfig.INSTANCE.liveSubtitlesEnabled || gen != this.trackGeneration.get() || !trackKey.equals(this.activeTrackKey) || now - capturedAtMs > MAX_RESULT_STALENESS_MS || capturedAtMs < this.lastEmittedCaptureMs) {
            return;
        }
        this.updateStickyLang(best);

        long tailDurMs = sampleRate > 0 ? Math.round((double)analyzed.tailSamples() * 1000.0 / (double)sampleRate) : 0L;
        List<LocalWhisperEngine.WordSpan> freshSpans = filterSpansAfterTail(best.wordSpans(), tailDurMs);
        List<String> candidateWords = !freshSpans.isEmpty()
            ? extractWordsFromSpans(freshSpans)
            : tokenizeWords(best.text());

        if (candidateWords.isEmpty()) {
            return;
        }
        int trimmedCount = 0;
        if (now - this.lastEmittedAtMs <= 4600L && !this.lastEmittedWords.isEmpty()) {
            List<String> trimmed = trimOverlappingWords(this.lastEmittedWords, candidateWords);
            trimmedCount = candidateWords.size() - trimmed.size();
            candidateWords = trimmed;
        }
        if (candidateWords.isEmpty()) {
            return;
        }
        List<LocalWhisperEngine.WordSpan> alignedSpans = (!freshSpans.isEmpty() && trimmedCount >= 0 && trimmedCount < freshSpans.size())
            ? freshSpans.subList(trimmedCount, freshSpans.size())
            : Collections.emptyList();

        String emitText = String.join(" ", candidateWords);
        String normKey = normalizeForDedupe(emitText);
        if (normKey.isEmpty() || (normKey.equals(this.lastEmittedNorm) && now - this.lastEmittedAtMs < 6500L)) {
            return;
        }
        this.lastEmittedNorm = normKey;
        this.lastEmittedWords = retainRecentContextWords(this.lastEmittedWords, candidateWords, 10);
        this.lastEmittedAtMs = now;
        this.lastEmittedCaptureMs = capturedAtMs;
        this.statusLabel = best.source() + " [" + best.lang() + "]";

        LyricLine synthesized = !alignedSpans.isEmpty() && alignedSpans.size() == candidateWords.size()
            ? this.buildTimedLineFromSpans(candidateWords, alignedSpans, tailDurMs, captureStartPos, captureDurMs)
            : this.buildTimedLine(emitText, analyzed.smoothActivity(), captureStartPos, captureDurMs);
        if (synthesized != null) {
            this.lyricsManager.appendGeneratedLiveLine(trackKey, synthesized);
        }
    }

    private static List<LocalWhisperEngine.WordSpan> filterSpansAfterTail(List<LocalWhisperEngine.WordSpan> spans, long tailDurMs) {
        if (spans == null || spans.isEmpty()) {
            return Collections.emptyList();
        }
        if (tailDurMs <= 0L) {
            return spans;
        }
        long cutoffMs = Math.max(0L, tailDurMs - 90L);
        List<LocalWhisperEngine.WordSpan> kept = new ArrayList<LocalWhisperEngine.WordSpan>(spans.size());
        for (LocalWhisperEngine.WordSpan s : spans) {
            if (s != null && s.endMs() > cutoffMs) {
                kept.add(s);
            }
        }
        return kept;
    }

    private static List<String> extractWordsFromSpans(List<LocalWhisperEngine.WordSpan> spans) {
        List<String> out = new ArrayList<String>(spans.size());
        for (LocalWhisperEngine.WordSpan s : spans) {
            if (s != null && s.word() != null && !s.word().isEmpty()) {
                out.add(s.word());
            }
        }
        return out;
    }

    private static List<String> tokenizeWords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String[] rawTokens = MULTI_SPACE.split(text.trim());
        List<String> out = new ArrayList<String>(rawTokens.length);
        for (String tok : rawTokens) {
            String cleaned = EDGE_PUNCT.matcher(tok).replaceAll("").trim();
            if (!cleaned.isEmpty()) {
                out.add(cleaned);
            }
        }
        return out;
    }

    private static List<String> retainRecentContextWords(List<String> previous, List<String> added, int maxKeep) {
        List<String> combined = new ArrayList<String>();
        if (previous != null) {
            combined.addAll(previous);
        }
        if (added != null) {
            combined.addAll(added);
        }
        if (combined.size() <= maxKeep) {
            return combined;
        }
        return new ArrayList<String>(combined.subList(combined.size() - maxKeep, combined.size()));
    }

    public static List<String> trimOverlappingWords(List<String> previousWords, List<String> newWords) {
        if (newWords == null || newWords.isEmpty()) {
            return Collections.emptyList();
        }
        if (previousWords == null || previousWords.isEmpty()) {
            return new ArrayList<String>(newWords);
        }
        int maxK = Math.min(5, Math.min(previousWords.size(), newWords.size()));
        int overlap = 0;
        for (int k = maxK; k >= 1; --k) {
            boolean allMatch = true;
            int strongCount = 0;
            for (int i = 0; i < k; ++i) {
                String p = normalizeForDedupe(previousWords.get(previousWords.size() - k + i));
                String n = normalizeForDedupe(newWords.get(i));
                if (p.isEmpty() || !p.equals(n)) {
                    allMatch = false;
                    break;
                }
                if (n.length() >= 3) {
                    ++strongCount;
                }
            }
            if (allMatch && (k >= 2 || strongCount >= 1)) {
                overlap = k;
                break;
            }
        }
        if (overlap <= 0) {
            return new ArrayList<String>(newWords);
        }
        if (overlap >= newWords.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(newWords.subList(overlap, newWords.size()));
    }

    private RecognitionResult recognizeBest(float[] rawSamples, int sampleRate, TrackState state, double rawRms, double activeFraction) {
        float[] hpfSamples = highPass75(rawSamples, sampleRate);
        byte[] cleanPcm16 = encodeCleanNormalizedPcm16(hpfSamples);
        String sttMode = resolveSttMode();
        List<String> langs = this.resolveLanguageOrder(state);
        String primaryLang = langs.get(0);
        String secondaryLang = langs.size() > 1 ? langs.get(1) : "";
        boolean nonLatinMeta = hasNonLatinScriptMetadata(state);

        RecognitionResult whisperRes = null;
        if (!"GOOGLE".equals(sttMode) && this.whisperEngine.isReady()) {
            byte[] wavBytes = wrapCanonicalWav(cleanPcm16, sampleRate);
            String whisperLang = this.stickyLangConfidence >= 0.80 && !this.stickyLang.isEmpty() ? this.stickyLang : "auto";
            String promptHint = buildPromptHint(this.lastEmittedWords);
            LocalWhisperEngine.WhisperOutput wOut = this.whisperEngine.transcribeWav(wavBytes, whisperLang, promptHint);
            if (wOut != null) {
                String cleaned = sanitizeTranscript(wOut.text(), wOut.confidence());
                if (cleaned != null) {
                    whisperRes = new RecognitionResult(cleaned, wOut.confidence(), wOut.bcp47Lang(), wOut.detectedLangProb(), "Whisper", wOut.words());
                    if (wOut.detectedLangProb() >= 0.72 && !wOut.bcp47Lang().isEmpty() && !"auto".equals(wOut.bcp47Lang())) {
                        primaryLang = wOut.bcp47Lang();
                        if (primaryLang.equals(secondaryLang)) {
                            secondaryLang = "";
                        }
                    }
                }
            }
            if ("WHISPER".equals(sttMode)) {
                return whisperRes;
            }
            if (whisperRes != null && whisperRes.confidence() >= 0.78) {
                return whisperRes;
            }
        }

        if (rawRms < 0.0009 || activeFraction < 0.09 || !hasGoogleSpeechKey()) {
            return whisperRes;
        }

        RecognitionResult googleBest = this.queryGoogleSpeechValidated(cleanPcm16, sampleRate, primaryLang);
        boolean canTrustPrimaryOnly = (this.stickyLangConfidence >= 0.84 && this.stickyLang.equals(primaryLang) && googleBest != null && googleBest.confidence() >= 0.78)
            || (nonLatinMeta && googleBest != null && googleBest.confidence() >= 0.80)
            || (googleBest != null && googleBest.confidence() >= 0.91);

        if (!canTrustPrimaryOnly && !secondaryLang.isEmpty() && !secondaryLang.equals(primaryLang)) {
            RecognitionResult secRes = this.queryGoogleSpeechValidated(cleanPcm16, sampleRate, secondaryLang);
            if (scoreCandidate(secRes) > scoreCandidate(googleBest)) {
                googleBest = secRes;
            }
        }
        if (scoreCandidate(googleBest) > scoreCandidate(whisperRes)) {
            return googleBest;
        }
        return whisperRes != null ? whisperRes : googleBest;
    }

    private static String buildPromptHint(List<String> recentWords) {
        if (recentWords == null || recentWords.isEmpty()) {
            return "";
        }
        int start = Math.max(0, recentWords.size() - 8);
        return String.join(" ", recentWords.subList(start, recentWords.size()));
    }

    private static boolean hasNonLatinScriptMetadata(TrackState state) {
        if (state == null) {
            return false;
        }
        String meta = ((state.title() != null ? state.title() : "") + " " + (state.artist() != null ? state.artist() : "")).trim();
        for (int i = 0; i < meta.length(); ++i) {
            char c = meta.charAt(i);
            if ((c >= '\u0400' && c <= '\u04FF') || (c >= '\u3040' && c <= '\u30FF') || (c >= '\uAC00' && c <= '\uD7AF') || (c >= '\u4E00' && c <= '\u9FFF')) {
                return true;
            }
        }
        return false;
    }

    private RecognitionResult queryGoogleSpeechValidated(byte[] pcm16, int sampleRate, String lang) {
        RecognitionResult raw = this.callGoogleSpeechV2(pcm16, sampleRate, lang);
        if (raw == null) {
            return null;
        }
        String cleaned = sanitizeTranscript(raw.text(), raw.confidence());
        if (cleaned == null) {
            return null;
        }
        if (raw.confidence() < 0.56 && MULTI_SPACE.split(cleaned).length <= 2) {
            return null;
        }
        return new RecognitionResult(cleaned, raw.confidence(), lang, 0.0, "Google", Collections.emptyList());
    }

    private RecognitionResult callGoogleSpeechV2(byte[] pcm16, int sampleRate, String lang) {
        String apiKey = HudConfig.INSTANCE != null && HudConfig.INSTANCE.googleSpeechApiKey != null
            ? HudConfig.INSTANCE.googleSpeechApiKey.trim()
            : "";
        if (apiKey.isEmpty()) {
            return null;
        }
        try {
            String url = GOOGLE_SPEECH_URL_PREFIX + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&lang=" + URLEncoder.encode(lang, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "audio/l16; rate=" + sampleRate)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .POST(HttpRequest.BodyPublishers.ofByteArray(pcm16))
                .build();
            HttpResponse<String> resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
                return null;
            }
            return parseGoogleSpeechNdjson(resp.body(), lang);
        } catch (Exception e) {
            return null;
        }
    }

    private static RecognitionResult parseGoogleSpeechNdjson(String body, String lang) {
        String bestText = null;
        double bestScore = -1.0;
        String[] lines = body.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line != null ? line.trim() : "";
            if (!trimmed.startsWith("{")) {
                continue;
            }
            try {
                JsonElement el = JsonParser.parseString(trimmed);
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("result") || !obj.get("result").isJsonArray()) {
                    continue;
                }
                JsonArray results = obj.getAsJsonArray("result");
                for (JsonElement resEl : results) {
                    if (!resEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject resObj = resEl.getAsJsonObject();
                    if (!resObj.has("alternative") || !resObj.get("alternative").isJsonArray()) {
                        continue;
                    }
                    JsonArray alts = resObj.getAsJsonArray("alternative");
                    for (JsonElement altEl : alts) {
                        if (!altEl.isJsonObject()) {
                            continue;
                        }
                        JsonObject altObj = altEl.getAsJsonObject();
                        if (!altObj.has("transcript") || !altObj.get("transcript").isJsonPrimitive()) {
                            continue;
                        }
                        String transcript = altObj.get("transcript").getAsString().trim();
                        if (transcript.isEmpty()) {
                            continue;
                        }
                        double conf = altObj.has("confidence") && altObj.get("confidence").isJsonPrimitive()
                            ? altObj.get("confidence").getAsDouble()
                            : 0.65;
                        if (conf > bestScore) {
                            bestScore = conf;
                            bestText = transcript;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return bestText != null ? new RecognitionResult(bestText, Math.max(0.0, bestScore), lang) : null;
    }

    private static String parseGradioCompleteSse(String sseBody) {
        boolean sawComplete = false;
        for (String line : sseBody.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.equals("event: complete")) {
                sawComplete = true;
            } else if (sawComplete && trimmed.startsWith("data:")) {
                String jsonPart = trimmed.substring(5).trim();
                try {
                    JsonElement el = JsonParser.parseString(jsonPart);
                    if (el.isJsonArray() && !el.getAsJsonArray().isEmpty()) {
                        return el.getAsJsonArray().get(0).getAsString();
                    }
                } catch (Exception ignored) {
                }
                return null;
            }
        }
        return null;
    }

    private List<String> resolveLanguageOrder(TrackState state) {
        List<String> order = new ArrayList<String>(3);
        if (this.stickyLang != null && !this.stickyLang.isEmpty()) {
            order.add(this.stickyLang);
        }
        String meta = state == null ? "" : ((state.title() != null ? state.title() : "") + " " + (state.artist() != null ? state.artist() : "") + " " + (state.album() != null ? state.album() : "")).trim();
        boolean hasUkrainian = false;
        boolean hasCyrillic = false;
        boolean hasJapanese = false;
        boolean hasKorean = false;
        boolean hasCjk = false;
        for (int i = 0; i < meta.length(); ++i) {
            char c = meta.charAt(i);
            if ("іїєґІЇЄҐ".indexOf(c) >= 0) {
                hasUkrainian = true;
                hasCyrillic = true;
            } else if (c >= '\u0400' && c <= '\u04FF') {
                hasCyrillic = true;
            } else if ((c >= '\u3040' && c <= '\u30FF') || (c >= '\u31F0' && c <= '\u31FF')) {
                hasJapanese = true;
            } else if (c >= '\uAC00' && c <= '\uD7AF') {
                hasKorean = true;
            } else if (c >= '\u4E00' && c <= '\u9FFF') {
                hasCjk = true;
            }
        }
        if (hasUkrainian) {
            addUnique(order, "uk-UA");
            addUnique(order, "ru-RU");
            addUnique(order, "en-US");
        } else if (hasCyrillic) {
            addUnique(order, "ru-RU");
            addUnique(order, "en-US");
        } else if (hasJapanese) {
            addUnique(order, "ja-JP");
            addUnique(order, "en-US");
        } else if (hasKorean) {
            addUnique(order, "ko-KR");
            addUnique(order, "en-US");
        } else if (hasCjk) {
            addUnique(order, "zh-CN");
            addUnique(order, "en-US");
        } else {
            addUnique(order, "en-US");
            addUnique(order, "ru-RU");
        }
        return order;
    }

    private static void addUnique(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    private LyricLine buildTimedLineFromSpans(List<String> words, List<LocalWhisperEngine.WordSpan> spans, long tailDurMs, long captureStartPos, long captureDurMs) {
        if (words.isEmpty() || spans.size() != words.size()) {
            return null;
        }
        String fullText = String.join(" ", words);
        List<LyricLine.Word> wordList = new ArrayList<LyricLine.Word>(words.size());
        long baseRel = Math.max(0L, spans.get(0).startMs() - tailDurMs);
        int charPos = 0;
        long maxEnd = captureStartPos;
        for (int i = 0; i < words.size(); ++i) {
            String wText = words.get(i);
            LocalWhisperEngine.WordSpan span = spans.get(i);
            long sRel = Math.max(0L, span.startMs() - tailDurMs) - baseRel;
            long eRel = Math.max(sRel + 100L, (span.endMs() - tailDurMs) - baseRel);
            long wStart = captureStartPos + Math.min(captureDurMs, sRel);
            long wEnd = captureStartPos + Math.max(sRel + 100L, Math.min(captureDurMs + 400L, eRel));
            if (!wordList.isEmpty() && wStart < wordList.get(wordList.size() - 1).endMs()) {
                wStart = wordList.get(wordList.size() - 1).endMs();
                wEnd = Math.max(wStart + 90L, wEnd);
            }
            int charStart = charPos;
            int charEnd = charStart + wText.length();
            wordList.add(new LyricLine.Word(wStart, wEnd, wText, charStart, charEnd));
            maxEnd = Math.max(maxEnd, wEnd);
            charPos = charEnd + 1;
        }
        long displayDur = Math.max(2000L, Math.max(maxEnd - captureStartPos + 500L, (long)words.size() * 360L + 750L));
        return new LyricLine(captureStartPos, captureStartPos + displayDur, fullText, wordList);
    }

    private LyricLine buildTimedLine(String text, double[] smoothActivity, long captureStartPos, long captureDurMs) {
        String[] tokens = MULTI_SPACE.split(text.trim());
        if (tokens.length == 0) {
            return null;
        }
        long activeStartRel = 0L;
        long activeEndRel = captureDurMs;
        if (smoothActivity != null && smoothActivity.length > 8) {
            int firstIdx = -1;
            int lastIdx = -1;
            for (int f = 0; f < smoothActivity.length; ++f) {
                if (smoothActivity[f] > 0.14) {
                    if (firstIdx < 0) {
                        firstIdx = f;
                    }
                    lastIdx = f;
                }
            }
            if (firstIdx >= 0 && lastIdx > firstIdx) {
                long s = Math.round((double)firstIdx / (double)smoothActivity.length * (double)captureDurMs);
                long e = Math.round((double)(lastIdx + 1) / (double)smoothActivity.length * (double)captureDurMs);
                if (e - s >= 550L) {
                    activeStartRel = s;
                    activeEndRel = e;
                }
            }
        }
        long voicedSpan = Math.max(850L, Math.min(captureDurMs, activeEndRel - activeStartRel));
        long displayDur = Math.max(2200L, Math.min(5600L, Math.max(voicedSpan + 500L, (long)tokens.length * 380L + 850L)));
        long sweepSpan = Math.min(displayDur - 150L, Math.max(850L, voicedSpan));

        int totalSyl = 0;
        int[] syls = new int[tokens.length];
        for (int i = 0; i < tokens.length; ++i) {
            syls[i] = StudioCascadeAligner.countSyllables(tokens[i]);
            totalSyl += syls[i];
        }
        totalSyl = Math.max(1, totalSyl);

        String fullText = String.join(" ", tokens);
        List<LyricLine.Word> wordList = new ArrayList<LyricLine.Word>(tokens.length);
        long cursorMs = captureStartPos;
        int charPos = 0;
        for (int i = 0; i < tokens.length; ++i) {
            String wText = tokens[i];
            long wDur = Math.max(150L, Math.round((double)sweepSpan * (double)syls[i] / (double)totalSyl));
            long wEnd = i == tokens.length - 1 ? captureStartPos + sweepSpan : Math.min(captureStartPos + sweepSpan, cursorMs + wDur);
            if (wEnd <= cursorMs) {
                wEnd = cursorMs + 130L;
            }
            int charStart = charPos;
            int charEnd = charStart + wText.length();
            wordList.add(new LyricLine.Word(cursorMs, wEnd, wText, charStart, charEnd));
            cursorMs = wEnd;
            charPos = charEnd + 1;
        }
        return new LyricLine(captureStartPos, captureStartPos + displayDur, fullText, wordList);
    }

    private static String sanitizeTranscript(String rawText, double confidence) {
        if (rawText == null) {
            return null;
        }
        String collapsed = MULTI_SPACE.matcher(rawText).replaceAll(" ").trim();
        collapsed = EDGE_PUNCT.matcher(collapsed).replaceAll("").trim();
        if (collapsed.isEmpty()) {
            return null;
        }
        int letters = 0;
        for (int i = 0; i < collapsed.length(); ++i) {
            if (Character.isLetter(collapsed.charAt(i))) {
                ++letters;
            }
        }
        if (letters < 2) {
            return null;
        }
        String lower = collapsed.toLowerCase(Locale.ROOT);
        for (String bad : HALLUCINATION_PHRASES) {
            if (lower.equals(bad) || (bad.length() >= 8 && lower.contains(bad))) {
                return null;
            }
        }
        String[] words = MULTI_SPACE.split(lower);
        if (words.length == 1 && words[0].length() <= 3 && confidence < 0.80) {
            return null;
        }
        if (words.length >= 6) {
            Map<String, Integer> freq = new HashMap<String, Integer>();
            int maxFreq = 0;
            for (String w : words) {
                int c = freq.getOrDefault(w, 0) + 1;
                freq.put(w, c);
                if (c > maxFreq) {
                    maxFreq = c;
                }
            }
            if ((double)maxFreq / (double)words.length > 0.70 && maxFreq >= 5) {
                return null;
            }
        }
        return collapsed;
    }

    private static String normalizeForDedupe(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        String lower = text.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
        for (int i = 0; i < lower.length(); ++i) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static double scoreCandidate(RecognitionResult res) {
        if (res == null || res.text() == null) {
            return -1.0;
        }
        int words = MULTI_SPACE.split(res.text().trim()).length;
        return res.confidence() + Math.min(0.22, (double)words * 0.025);
    }

    private static double computeRms(float[] samples) {
        if (samples == null || samples.length == 0) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (float s : samples) {
            sumSq += (double)s * (double)s;
        }
        return Math.sqrt(sumSq / (double)samples.length);
    }

    private static double computePeak(float[] samples) {
        double peak = 0.0;
        for (float s : samples) {
            double a = Math.abs(s);
            if (a > peak) {
                peak = a;
            }
        }
        return peak;
    }

    private static double computeActiveFraction(double[] smoothActivity, double threshold) {
        if (smoothActivity == null || smoothActivity.length == 0) {
            return 0.0;
        }
        int active = 0;
        for (double v : smoothActivity) {
            if (v > threshold) {
                ++active;
            }
        }
        return (double)active / (double)smoothActivity.length;
    }

    public static float[] highPass75(float[] samples, int sampleRate) {
        if (samples == null || samples.length == 0 || sampleRate <= 0) {
            return samples != null ? samples : new float[0];
        }
        float[] stage1 = biquadHighPass(samples, sampleRate, 80.0);
        return biquadHighPass(stage1, sampleRate, 80.0);
    }

    private static float[] biquadHighPass(float[] samples, int sampleRate, double cutoffHz) {
        double w0 = 2.0 * Math.PI * cutoffHz / (double)sampleRate;
        double alpha = Math.sin(w0) / Math.sqrt(2.0);
        double cosw = Math.cos(w0);
        double a0 = 1.0 + alpha;
        double b0 = ((1.0 + cosw) * 0.5) / a0;
        double b1 = -(1.0 + cosw) / a0;
        double b2 = ((1.0 + cosw) * 0.5) / a0;
        double a1 = (-2.0 * cosw) / a0;
        double a2 = (1.0 - alpha) / a0;
        float[] out = new float[samples.length];
        double x1 = 0.0;
        double x2 = 0.0;
        double y1 = 0.0;
        double y2 = 0.0;
        for (int i = 0; i < samples.length; ++i) {
            double x0 = samples[i];
            double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            out[i] = (float)y0;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
        }
        return out;
    }

    private static byte[] encodePreEmphPcm16(float[] samples) {
        float[] pre = new float[samples.length];
        float prev = 0.0f;
        for (int i = 0; i < samples.length; ++i) {
            float cur = samples[i];
            pre[i] = cur - 0.25f * prev;
            prev = cur;
        }
        return encodeCleanNormalizedPcm16(pre);
    }

    public static byte[] encodeCleanNormalizedPcm16(float[] samples) {
        double rms = Math.max(1.0E-5, computeRms(samples));
        double peak = Math.max(1.0E-5, computePeak(samples));
        double gain = Math.min(12.0, Math.min(0.20 / rms, 0.88 / peak));
        ByteBuffer buf = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) {
            double x = (double)s * gain;
            double abs = Math.abs(x);
            double limited = abs <= 0.85 ? x : Math.signum(x) * (0.85 + 0.13 * Math.tanh((abs - 0.85) / 0.35));
            int val = (int)Math.max(-32767.0, Math.min(32767.0, Math.round(limited * 32767.0)));
            buf.putShort((short)val);
        }
        return buf.array();
    }

    private static byte[] wrapCanonicalWav(byte[] pcm16, int sampleRate) {
        int dataLen = pcm16.length;
        ByteBuffer hdr = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        hdr.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        hdr.putInt(36 + dataLen);
        hdr.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
        hdr.putInt(16);
        hdr.putShort((short)1);
        hdr.putShort((short)1);
        hdr.putInt(sampleRate);
        hdr.putInt(sampleRate * 2);
        hdr.putShort((short)2);
        hdr.putShort((short)16);
        hdr.put("data".getBytes(StandardCharsets.US_ASCII));
        hdr.putInt(dataLen);
        hdr.put(pcm16);
        return hdr.array();
    }
}
