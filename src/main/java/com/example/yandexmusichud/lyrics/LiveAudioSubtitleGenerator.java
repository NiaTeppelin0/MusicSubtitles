package com.example.yandexmusichud.lyrics;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveAudioSubtitleGenerator {
    private static final String GOOGLE_SPEECH_BASE = "https://www.google.com/speech-api/v2/recognize?output=json&client=chromium&pFilter=0";
    private static final String HF_WHISPER_BASE = "https://hf-audio-whisper-large-v3.hf.space";
    private static final int CAPTURE_DURATION_MS = 3800;
    private static final long INTER_CAPTURE_GAP_MS = 120L;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern EDGE_PUNCT = Pattern.compile("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$");
    private static final Set<String> HALLUCINATION_PHRASES = Set.of(
        "thanks for watching",
        "thank you for watching",
        "subscribe",
        "like and subscribe",
        "subtitles by",
        "captions by",
        "translated by",
        "amara org",
        "you",
        "thank you",
        "bye",
        "music",
        "субтитры",
        "субтитры создавал",
        "редактор субтитров",
        "продолжение следует",
        "подписывайтесь",
        "спасибо за просмотр",
        "ставьте лайк",
        "музыка",
        "dimatorzok"
    );

    private record RecognitionResult(String text, double confidence, String lang) {
    }

    public record VerificationRecognition(String text, double confidence, String lang, double activeFraction) {
    }

    private final LyricsManager lyricsManager;
    private final HttpClient httpClient;
    private final ExecutorService sttExecutor;
    private final AtomicInteger sttInFlight = new AtomicInteger(0);
    private volatile boolean captureInFlight = false;
    private volatile long lastCaptureTimestampMs = 0L;
    private volatile long lastCaptureEndedMs = 0L;
    private volatile String activeTrackKey = "";
    private volatile String stickyLang = "";
    private volatile String lastEmittedNorm = "";
    private volatile long lastEmittedAtMs = 0L;
    private volatile long whisperCooldownUntilMs = 0L;
    private volatile String statusLabel = "Ожидание трека";

    public LiveAudioSubtitleGenerator(LyricsManager lyricsManager) {
        this.lyricsManager = lyricsManager;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        this.sttExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MusicSubtitles-LiveSTT");
            t.setDaemon(true);
            return t;
        });
    }

    public void resetForTrack(String trackKey) {
        this.activeTrackKey = trackKey != null ? trackKey : "";
        this.stickyLang = "";
        this.lastEmittedNorm = "";
        this.lastEmittedAtMs = 0L;
        this.lastCaptureTimestampMs = 0L;
        this.lastCaptureEndedMs = 0L;
        this.captureInFlight = false;
        this.statusLabel = this.activeTrackKey.isEmpty() ? "Ожидание трека" : "Инициализация захвата";
    }

    public boolean isCapturingOrActive() {
        long now = System.currentTimeMillis();
        return this.captureInFlight || this.sttInFlight.get() > 0 || (this.lastCaptureTimestampMs > 0L && now - this.lastCaptureTimestampMs < 6500L);
    }

    public String getStatusLabel() {
        return this.statusLabel;
    }

    public void shutdown() {
        this.sttExecutor.shutdownNow();
    }

    public void tickIfNeeded(TrackState state, String trackKey) {
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
        if (this.lastCaptureEndedMs > 0L && now - this.lastCaptureEndedMs < INTER_CAPTURE_GAP_MS) {
            return;
        }
        if (this.lastCaptureTimestampMs > 0L && now - this.lastCaptureTimestampMs < 600L) {
            return;
        }
        this.captureInFlight = true;
        this.lastCaptureTimestampMs = now;
        this.lyricsManager.getExecutor().execute(() -> this.captureLoop(state, trackKey));
    }

    private void captureLoop(TrackState state, String trackKey) {
        try {
            int port = this.lyricsManager.resolveHelperPort();
            if (port <= 0) {
                this.statusLabel = "Helper не запущен";
                this.lastCaptureEndedMs = System.currentTimeMillis() + 1800L;
                return;
            }
            String helperBase = "http://127.0.0.1:" + port;
            String startResp = LyricsManager.httpPost(helperBase + "/capture/start", "{\"durationMs\":" + CAPTURE_DURATION_MS + "}", 2000);
            if (startResp == null) {
                this.statusLabel = "Захват занят / недоступен";
                this.lastCaptureEndedMs = System.currentTimeMillis() + 900L;
                return;
            }
            this.statusLabel = "Слушаю аудио...";
            long captureStartPos = state.getInterpolatedPositionMs();
            for (int elapsed = 0; elapsed < CAPTURE_DURATION_MS + 220; elapsed += 120) {
                if (!trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
                    LyricsManager.httpPost(helperBase + "/capture/stop", "", 1000);
                    this.statusLabel = "Пауза / смена трека";
                    return;
                }
                Thread.sleep(120L);
            }
            if (!trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
                return;
            }
            byte[] wav = LyricsManager.httpGetBytes(helperBase + "/capture/result", 3000);
            this.lastCaptureEndedMs = System.currentTimeMillis();
            if (wav == null || wav.length <= 44) {
                this.statusLabel = "Пустой буфер аудио";
                return;
            }
            this.captureInFlight = false;
            this.sttInFlight.incrementAndGet();
            this.sttExecutor.execute(() -> {
                try {
                    this.processCapturedWav(wav, trackKey, state, captureStartPos, CAPTURE_DURATION_MS);
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

    private record AnalyzedSpeech(RecognitionResult result, double[] smoothActivity, double activeFraction) {
    }

    public VerificationRecognition verifyAudioSnippet(float[] rawSamples, int sampleRate, TrackState state) {
        if (rawSamples == null || sampleRate <= 0 || rawSamples.length < sampleRate) {
            return null;
        }
        AnalyzedSpeech analyzed = this.analyzeSpeechSamples(rawSamples, sampleRate, state, 0.0010, 0.00055, 0.0020, 0.14, false);
        if (analyzed == null || analyzed.result() == null || analyzed.result().text() == null || analyzed.result().text().isEmpty()) {
            return null;
        }
        RecognitionResult res = analyzed.result();
        if (res.confidence() >= 0.75 && res.lang() != null && !res.lang().isEmpty()) {
            this.stickyLang = res.lang();
        }
        return new VerificationRecognition(res.text(), res.confidence(), res.lang(), analyzed.activeFraction());
    }

    private AnalyzedSpeech analyzeSpeechSamples(
        float[] rawSamples,
        int sampleRate,
        TrackState state,
        double minRawRms,
        double minBandRms,
        double minBandPeak,
        double minActiveFraction,
        boolean updateStatusLabel
    ) {
        double rawRms = LiveAudioSubtitleGenerator.computeRms(rawSamples);
        if (rawRms < minRawRms) {
            if (updateStatusLabel) {
                this.statusLabel = "Тишина / пауза";
            }
            return null;
        }
        float[] speechBand = AcousticLagEstimator.extractSpeechBand(rawSamples, sampleRate);
        double bandRms = LiveAudioSubtitleGenerator.computeRms(speechBand);
        double bandPeak = LiveAudioSubtitleGenerator.computePeak(speechBand);
        if (bandRms < minBandRms || bandPeak < minBandPeak) {
            if (updateStatusLabel) {
                this.statusLabel = "Инструментал / низкий сигнал";
            }
            return null;
        }
        double[][] features = AcousticLagEstimator.audioFeatures(rawSamples, sampleRate);
        double[] smoothActivity = features.length > 0 ? features[0] : new double[0];
        double activeFraction = LiveAudioSubtitleGenerator.computeActiveFraction(smoothActivity, 0.14);
        if (activeFraction < minActiveFraction) {
            return null;
        }

        byte[] speechPcm16 = LiveAudioSubtitleGenerator.encodePreEmphPcm16(speechBand);
        List<String> langs = this.resolveLanguageOrder(state);
        String primaryLang = langs.get(0);
        String secondaryLang = langs.size() > 1 ? langs.get(1) : "";

        if (updateStatusLabel) {
            this.statusLabel = "Распознавание речи (" + primaryLang + ")...";
        }
        RecognitionResult best = this.recognizeBest(speechPcm16, rawSamples, sampleRate, primaryLang, secondaryLang, rawRms, activeFraction);
        if (best == null || best.text() == null || best.text().isEmpty()) {
            if (updateStatusLabel) {
                this.statusLabel = activeFraction > 0.2 ? "Речь не разобрана (инструментал/шум)" : "Инструментальная секция";
            }
            return null;
        }
        return new AnalyzedSpeech(best, smoothActivity, activeFraction);
    }

    private void processCapturedWav(byte[] wav, String trackKey, TrackState state, long captureStartPos, long captureDurMs) {
        if (!trackKey.equals(this.activeTrackKey) || !state.isPlaying()) {
            return;
        }
        AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wav);
        if (pcm == null || pcm.samples() == null || pcm.sampleRate() <= 0) {
            return;
        }
        float[] rawSamples = pcm.samples();
        int sampleRate = pcm.sampleRate();
        if (rawSamples.length < sampleRate) {
            return;
        }
        AnalyzedSpeech analyzed = this.analyzeSpeechSamples(rawSamples, sampleRate, state, 0.0007, 0.00045, 0.0018, 0.0, true);
        if (analyzed == null) {
            return;
        }
        RecognitionResult best = analyzed.result();
        if (!trackKey.equals(this.activeTrackKey)) {
            return;
        }
        if (best.confidence() >= 0.75 && best.lang() != null && !best.lang().isEmpty()) {
            this.stickyLang = best.lang();
        }
        String normKey = LiveAudioSubtitleGenerator.normalizeForDedupe(best.text());
        long now = System.currentTimeMillis();
        if (normKey.isEmpty() || (normKey.equals(this.lastEmittedNorm) && now - this.lastEmittedAtMs < 6500L)) {
            return;
        }
        this.lastEmittedNorm = normKey;
        this.lastEmittedAtMs = now;
        this.statusLabel = "STT OK [" + best.lang() + "]";

        LyricLine synthesized = this.buildTimedLine(best.text(), analyzed.smoothActivity(), captureStartPos, captureDurMs);
        if (synthesized != null) {
            this.lyricsManager.appendGeneratedLiveLine(trackKey, synthesized);
        }
    }

    private RecognitionResult recognizeBest(byte[] speechPcm16, float[] rawSamples, int sampleRate, String primaryLang, String secondaryLang, double rawRms, double activeFraction) {
        RecognitionResult primaryRes = this.queryGoogleSpeechValidated(speechPcm16, sampleRate, primaryLang);
        if (primaryRes != null && primaryRes.confidence() >= 0.72) {
            return primaryRes;
        }
        RecognitionResult best = primaryRes;
        boolean skipSecondary = !this.stickyLang.isEmpty() && this.stickyLang.equals(primaryLang) && primaryRes != null;
        if (!skipSecondary && !secondaryLang.isEmpty() && !secondaryLang.equals(primaryLang)) {
            RecognitionResult secRes = this.queryGoogleSpeechValidated(speechPcm16, sampleRate, secondaryLang);
            if (LiveAudioSubtitleGenerator.scoreCandidate(secRes) > LiveAudioSubtitleGenerator.scoreCandidate(best)) {
                best = secRes;
            }
        }
        if (best != null) {
            return best;
        }
        if (rawRms > 0.0025 && activeFraction >= 0.12) {
            byte[] fullBandPcm16 = LiveAudioSubtitleGenerator.encodeNormalizedPcm16(rawSamples);
            RecognitionResult rawRes = this.queryGoogleSpeechValidated(fullBandPcm16, sampleRate, primaryLang);
            if (rawRes != null) {
                return rawRes;
            }
        }
        long now = System.currentTimeMillis();
        if (activeFraction >= 0.25 && rawRms > 0.003 && now >= this.whisperCooldownUntilMs) {
            RecognitionResult whisperRes = this.queryHfWhisper(speechPcm16, sampleRate);
            if (whisperRes != null) {
                return whisperRes;
            }
        }
        return null;
    }

    private RecognitionResult queryGoogleSpeechValidated(byte[] pcm16, int sampleRate, String lang) {
        RecognitionResult raw = this.callGoogleSpeechV2(pcm16, sampleRate, lang);
        if (raw == null) {
            return null;
        }
        String cleaned = LiveAudioSubtitleGenerator.sanitizeTranscript(raw.text(), raw.confidence());
        if (cleaned == null) {
            return null;
        }
        return new RecognitionResult(cleaned, raw.confidence(), lang);
    }

    private RecognitionResult callGoogleSpeechV2(byte[] pcm16, int sampleRate, String lang) {
        try {
            String url = GOOGLE_SPEECH_BASE + "&lang=" + URLEncoder.encode(lang, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(7))
                .header("Content-Type", "audio/l16; rate=" + sampleRate)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .POST(HttpRequest.BodyPublishers.ofByteArray(pcm16))
                .build();
            HttpResponse<String> resp = this.httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
                return null;
            }
            return LiveAudioSubtitleGenerator.parseGoogleSpeechNdjson(resp.body(), lang);
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
        return bestText != null ? new RecognitionResult(bestText, bestScore, lang) : null;
    }

    private RecognitionResult queryHfWhisper(byte[] pcm16, int sampleRate) {
        try {
            byte[] wavBytes = LiveAudioSubtitleGenerator.wrapCanonicalWav(pcm16, sampleRate);
            String boundary = "----MusicSubtitlesBoundary" + System.nanoTime();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files\"; filename=\"clip.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n";
            baos.write(header.getBytes(StandardCharsets.UTF_8));
            baos.write(wavBytes);
            baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest upReq = HttpRequest.newBuilder()
                .uri(URI.create(HF_WHISPER_BASE + "/gradio_api/upload"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();
            HttpResponse<String> upResp = this.httpClient.send(upReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (upResp.statusCode() != 200 || upResp.body() == null) {
                this.whisperCooldownUntilMs = System.currentTimeMillis() + 600000L;
                return null;
            }
            JsonArray arr = JsonParser.parseString(upResp.body()).getAsJsonArray();
            if (arr.isEmpty()) {
                this.whisperCooldownUntilMs = System.currentTimeMillis() + 600000L;
                return null;
            }
            String serverPath = arr.get(0).getAsString();
            JsonObject fileData = new JsonObject();
            fileData.addProperty("path", serverPath);
            JsonObject meta = new JsonObject();
            meta.addProperty("_type", "gradio.FileData");
            fileData.add("meta", meta);
            JsonArray dataArr = new JsonArray();
            dataArr.add(fileData);
            dataArr.add("transcribe");
            JsonObject callBody = new JsonObject();
            callBody.add("data", dataArr);

            HttpRequest callReq = HttpRequest.newBuilder()
                .uri(URI.create(HF_WHISPER_BASE + "/gradio_api/call/transcribe"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(callBody.toString(), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> callResp = this.httpClient.send(callReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (callResp.statusCode() != 200 || callResp.body() == null) {
                this.whisperCooldownUntilMs = System.currentTimeMillis() + 600000L;
                return null;
            }
            JsonObject evObj = JsonParser.parseString(callResp.body()).getAsJsonObject();
            String eventId = evObj.has("event_id") ? evObj.get("event_id").getAsString() : "";
            if (eventId.isEmpty()) {
                this.whisperCooldownUntilMs = System.currentTimeMillis() + 600000L;
                return null;
            }

            HttpRequest sseReq = HttpRequest.newBuilder()
                .uri(URI.create(HF_WHISPER_BASE + "/gradio_api/call/transcribe/" + eventId))
                .timeout(Duration.ofSeconds(14))
                .GET()
                .build();
            HttpResponse<String> sseResp = this.httpClient.send(sseReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (sseResp.statusCode() != 200 || sseResp.body() == null || sseResp.body().contains("event: error")) {
                this.whisperCooldownUntilMs = System.currentTimeMillis() + 600000L;
                return null;
            }
            String text = LiveAudioSubtitleGenerator.parseGradioCompleteSse(sseResp.body());
            String cleaned = LiveAudioSubtitleGenerator.sanitizeTranscript(text, 0.78);
            return cleaned != null ? new RecognitionResult(cleaned, 0.78, "auto") : null;
        } catch (Exception e) {
            this.whisperCooldownUntilMs = System.currentTimeMillis() + 600000L;
            return null;
        }
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
        String meta = ((state.title() != null ? state.title() : "") + " " + (state.artist() != null ? state.artist() : "") + " " + (state.album() != null ? state.album() : "")).trim();
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
            LiveAudioSubtitleGenerator.addUnique(order, "uk-UA");
            LiveAudioSubtitleGenerator.addUnique(order, "ru-RU");
            LiveAudioSubtitleGenerator.addUnique(order, "en-US");
        } else if (hasCyrillic) {
            LiveAudioSubtitleGenerator.addUnique(order, "ru-RU");
            LiveAudioSubtitleGenerator.addUnique(order, "en-US");
        } else if (hasJapanese) {
            LiveAudioSubtitleGenerator.addUnique(order, "ja-JP");
            LiveAudioSubtitleGenerator.addUnique(order, "en-US");
        } else if (hasKorean) {
            LiveAudioSubtitleGenerator.addUnique(order, "ko-KR");
            LiveAudioSubtitleGenerator.addUnique(order, "en-US");
        } else if (hasCjk) {
            LiveAudioSubtitleGenerator.addUnique(order, "zh-CN");
            LiveAudioSubtitleGenerator.addUnique(order, "en-US");
        } else {
            LiveAudioSubtitleGenerator.addUnique(order, "en-US");
            LiveAudioSubtitleGenerator.addUnique(order, "ru-RU");
        }
        return order;
    }

    private static void addUnique(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
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
                if (e - s >= 650L) {
                    activeStartRel = s;
                    activeEndRel = e;
                }
            }
        }
        long voicedSpan = Math.max(900L, Math.min(captureDurMs, activeEndRel - activeStartRel));
        long displayDur = Math.max(2400L, Math.min(5600L, Math.max(voicedSpan + 550L, (long)tokens.length * 420L + 900L)));
        long sweepSpan = Math.min(displayDur - 150L, Math.max(900L, voicedSpan));

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
            long wDur = Math.max(160L, Math.round((double)sweepSpan * (double)syls[i] / (double)totalSyl));
            long wEnd = i == tokens.length - 1 ? captureStartPos + sweepSpan : Math.min(captureStartPos + sweepSpan, cursorMs + wDur);
            if (wEnd <= cursorMs) {
                wEnd = cursorMs + 140L;
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
        String lower = text.toLowerCase(Locale.ROOT);
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
        return res.confidence() + Math.min(0.25, (double)words * 0.03);
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

    private static byte[] encodePreEmphPcm16(float[] samples) {
        float[] pre = new float[samples.length];
        float prev = 0.0f;
        for (int i = 0; i < samples.length; ++i) {
            float cur = samples[i];
            pre[i] = cur - 0.45f * prev;
            prev = cur;
        }
        return LiveAudioSubtitleGenerator.encodeNormalizedPcm16(pre);
    }

    private static byte[] encodeNormalizedPcm16(float[] samples) {
        double rms = Math.max(1.0E-5, LiveAudioSubtitleGenerator.computeRms(samples));
        double peak = Math.max(1.0E-5, LiveAudioSubtitleGenerator.computePeak(samples));
        double gain = Math.min(28.0, Math.min(0.21 / rms, 0.90 / peak));
        ByteBuffer buf = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) {
            double shaped = Math.tanh((double)s * gain) * 0.88;
            int val = (int)Math.max(-32767.0, Math.min(32767.0, Math.round(shaped * 32767.0)));
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
