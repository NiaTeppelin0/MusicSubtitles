package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.lyrics.LyricLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudioCascadeAligner {
    private static final Pattern RU_VOWELS = Pattern.compile("[\u0430\u0435\u0451\u0438\u043e\u0443\u044b\u044d\u044e\u044f\u0410\u0415\u0401\u0418\u041e\u0423\u042b\u042d\u042e\u042f]");
    private static final Pattern EN_VOWELS = Pattern.compile("[aeiouyAEIOUY]");
    private static final Pattern SECTION_HEADER = Pattern.compile("^\\[.*?\\]$");

    public static List<LyricLine> align(String plainLyrics, byte[] wavBytes, long totalDurationMs) {
        if (plainLyrics == null || plainLyrics.isBlank()) {
            return Collections.emptyList();
        }
        long durationMs = totalDurationMs > 30000L ? totalDurationMs : 180000L;
        List<Stanza> stanzas = StudioCascadeAligner.parseStanzas(plainLyrics);
        if (stanzas.isEmpty()) {
            return Collections.emptyList();
        }
        List<VocalRegion> vocalRegions = null;
        if (wavBytes != null && wavBytes.length > 44) {
            try {
                AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wavBytes);
                long wavDurationMs = pcm != null && pcm.sampleRate() > 0 ? (long)((double)pcm.samples().length * 1000.0 / (double)pcm.sampleRate()) : 0L;
                if (pcm != null && pcm.samples() != null && pcm.sampleRate() > 0 && wavDurationMs >= Math.min(45000L, durationMs / 2L)) {
                    float[] filtered = StudioCascadeAligner.applyVocalBandpassFilter(pcm.samples(), pcm.sampleRate());
                    vocalRegions = StudioCascadeAligner.detectVocalRegions(filtered, pcm.sampleRate(), durationMs);
                }
            }
            catch (Exception exception) {

            }
        }
        if (vocalRegions == null || vocalRegions.isEmpty()) {
            vocalRegions = StudioCascadeAligner.generateAcousticStructure(stanzas.size(), durationMs);
        }
        return StudioCascadeAligner.alignStanzasToVocalRegions(stanzas, vocalRegions, durationMs);
    }

    public static List<Stanza> parseStanzas(String text) {
        ArrayList<Stanza> result = new ArrayList<Stanza>();
        String[] rawLines = text.split("\\r?\\n");
        Stanza current = new Stanza("");
        for (String raw : rawLines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                if (current.lines.isEmpty()) continue;
                result.add(current);
                current = new Stanza("");
                continue;
            }
            if (SECTION_HEADER.matcher(trimmed).matches()) {
                if (!current.lines.isEmpty()) {
                    result.add(current);
                }
                current = new Stanza(trimmed);
                continue;
            }
            current.lines.add(trimmed);
        }
        if (!current.lines.isEmpty()) {
            result.add(current);
        }
        return result;
    }

    public static float[] applyVocalBandpassFilter(float[] input, int sampleRate) {
        double alpha;
        if (input == null || input.length == 0) {
            return input;
        }
        double f0 = 1200.0 / (double)sampleRate;
        double Q = 0.9;
        double omega = Math.PI * 2 * f0;
        double b0 = alpha = Math.sin(omega) / (2.0 * Q);
        double b1 = 0.0;
        double b2 = -alpha;
        double a0 = 1.0 + alpha;
        double a1 = -2.0 * Math.cos(omega);
        double a2 = 1.0 - alpha;
        float[] output = new float[input.length];
        double x1 = 0.0;
        double x2 = 0.0;
        double y1 = 0.0;
        double y2 = 0.0;
        for (int i = 0; i < input.length; ++i) {
            double x0 = input[i];
            double y0 = b0 / a0 * x0 + b1 / a0 * x1 + b2 / a0 * x2 - a1 / a0 * y1 - a2 / a0 * y2;
            output[i] = (float)y0;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
        }
        return output;
    }

    public static List<VocalRegion> detectVocalRegions(float[] samples, int sampleRate, long durationMs) {
        int f;
        ArrayList<VocalRegion> regions = new ArrayList<VocalRegion>();
        if (samples == null || sampleRate <= 0) {
            return regions;
        }
        int frameSize = Math.max(1, (int)((double)sampleRate * 0.02));
        int totalFrames = samples.length / frameSize;
        if (totalFrames <= 0) {
            return regions;
        }
        double[] frameEnergy = new double[totalFrames];
        double maxE = 1.0E-5;
        for (f = 0; f < totalFrames; ++f) {
            double rms;
            int offset = f * frameSize;
            double sumSq = 0.0;
            for (int i = 0; i < frameSize; ++i) {
                double s = samples[offset + i];
                sumSq += s * s;
            }
            frameEnergy[f] = rms = Math.sqrt(sumSq / (double)frameSize);
            if (!(rms > maxE)) continue;
            maxE = rms;
        }
        f = 0;
        while (f < totalFrames) {
            int n = f++;
            frameEnergy[n] = frameEnergy[n] / maxE;
        }
        double threshold = 0.16;
        boolean inVoice = false;
        long regionStart = 0L;
        for (int f2 = 0; f2 < totalFrames; ++f2) {
            long timeMs = (long)((double)f2 * (double)frameSize * 1000.0 / (double)sampleRate);
            boolean isVoiced = frameEnergy[f2] > threshold;
            if (isVoiced && !inVoice) {
                inVoice = true;
                regionStart = timeMs;
                continue;
            }
            if (isVoiced || !inVoice) continue;
            int lookahead = Math.min(totalFrames, f2 + 15);
            boolean resumes = false;
            for (int k = f2 + 1; k < lookahead; ++k) {
                if (!(frameEnergy[k] > threshold)) continue;
                resumes = true;
                break;
            }
            if (resumes) continue;
            inVoice = false;
            long regionEnd = Math.min(durationMs, timeMs);
            if (regionEnd - regionStart < 1500L) continue;
            regions.add(new VocalRegion(regionStart, regionEnd));
        }
        if (inVoice) {
            long endMs = Math.min(durationMs, (long)((double)totalFrames * (double)frameSize * 1000.0 / (double)sampleRate));
            if (endMs - regionStart >= 1500L) {
                regions.add(new VocalRegion(regionStart, endMs));
            }
        }
        return regions;
    }

    public static List<VocalRegion> generateAcousticStructure(int stanzaCount, long durationMs) {
        ArrayList<VocalRegion> regions = new ArrayList<VocalRegion>();
        stanzaCount = Math.max(1, stanzaCount);
        long introMs = Math.min(22000L, Math.max(9000L, (long)((double)durationMs * 0.085)));
        long outroMs = Math.min(15000L, Math.max(4000L, (long)((double)durationMs * 0.05)));
        long singingDuration = durationMs - introMs - outroMs;
        if (stanzaCount == 1) {
            regions.add(new VocalRegion(introMs, durationMs - outroMs));
            return regions;
        }
        long soloDurationMs = durationMs > 120000L ? 6000L : 3000L;
        long netSinging = Math.max(20000L, singingDuration - soloDurationMs);
        long blockDuration = netSinging / (long)stanzaCount;
        int halfPoint = stanzaCount / 2;
        long curPos = introMs;
        for (int s = 0; s < stanzaCount; ++s) {
            if (s == halfPoint) {
                curPos += soloDurationMs;
            }
            long end = Math.min(durationMs - outroMs, curPos + blockDuration);
            regions.add(new VocalRegion(curPos, end));
            curPos = end + 1500L;
        }
        return regions;
    }

    public static List<LyricLine> alignStanzasToVocalRegions(List<Stanza> stanzas, List<VocalRegion> regions, long durationMs) {
        ArrayList<LyricLine> result = new ArrayList<LyricLine>();
        ArrayList<Stanza> nonEmptyStanzas = new ArrayList<Stanza>();
        for (Stanza st : stanzas) {
            if (st == null || st.lines.isEmpty()) continue;
            ArrayList<String> cleanLines = new ArrayList<String>();
            for (String ln : st.lines) {
                String tr = ln != null ? ln.trim() : "";
                if (!tr.isEmpty() && !SECTION_HEADER.matcher(tr).matches()) {
                    cleanLines.add(tr);
                }
            }
            if (!cleanLines.isEmpty()) {
                Stanza cleaned = new Stanza(st.header);
                cleaned.lines.addAll(cleanLines);
                nonEmptyStanzas.add(cleaned);
            }
        }
        if (nonEmptyStanzas.isEmpty()) {
            return result;
        }
        long safeDuration = Math.max(10000L, durationMs);
        long maxEndLimit = Math.max(2500L, safeDuration - 250L);
        int stanzaCount = nonEmptyStanzas.size();

        ArrayList<VocalRegion> cleanRegions = new ArrayList<VocalRegion>();
        if (regions != null) {
            for (VocalRegion r : regions) {
                if (r == null) continue;
                long rs = Math.max(0L, Math.min(maxEndLimit - 500L, r.startMs));
                long re = Math.max(rs, Math.min(maxEndLimit, r.endMs));
                if (re - rs < 800L) continue;
                if (!cleanRegions.isEmpty() && rs <= cleanRegions.get(cleanRegions.size() - 1).endMs + 250L) {
                    VocalRegion prev = cleanRegions.remove(cleanRegions.size() - 1);
                    cleanRegions.add(new VocalRegion(prev.startMs, Math.max(prev.endMs, re)));
                } else {
                    cleanRegions.add(new VocalRegion(rs, re));
                }
            }
        }
        long totalRegionSpan = 0L;
        for (VocalRegion r : cleanRegions) {
            totalRegionSpan += r.getDuration();
        }
        if (cleanRegions.isEmpty() || totalRegionSpan < 4000L) {
            cleanRegions.clear();
            long startWindow = Math.min(2500L, Math.max(500L, safeDuration / 60L));
            long endWindow = Math.max(startWindow + 3000L, Math.min(maxEndLimit, safeDuration - Math.min(4000L, safeDuration / 30L)));
            cleanRegions.add(new VocalRegion(startWindow, endWindow));
        }
        while (cleanRegions.size() > stanzaCount) {
            int bestIdx = -1;
            long bestGap = Long.MAX_VALUE;
            for (int i = 0; i < cleanRegions.size() - 1; ++i) {
                long gap = cleanRegions.get(i + 1).startMs - cleanRegions.get(i).endMs;
                if (gap < bestGap) {
                    bestGap = gap;
                    bestIdx = i;
                }
            }
            if (bestIdx < 0 || bestGap >= 6000L) break;
            VocalRegion a = cleanRegions.get(bestIdx);
            VocalRegion b = cleanRegions.get(bestIdx + 1);
            cleanRegions.set(bestIdx, new VocalRegion(a.startMs, b.endMs));
            cleanRegions.remove(bestIdx + 1);
        }

        int[] stanzaWeights = new int[stanzaCount];
        int totalWeight = 0;
        for (int s = 0; s < stanzaCount; ++s) {
            Stanza stanza = nonEmptyStanzas.get(s);
            int stWeight = 0;
            for (String lineText : stanza.lines) {
                int syllables = StudioCascadeAligner.countSyllables(lineText);
                stWeight += Math.max(4, syllables * 2 + lineText.length() / 4);
            }
            stanzaWeights[s] = Math.max(1, stWeight);
            totalWeight += stanzaWeights[s];
        }

        int regionCount = cleanRegions.size();
        long[] regCumEnd = new long[regionCount];
        long totalRegDur = 0L;
        for (int r = 0; r < regionCount; ++r) {
            totalRegDur += Math.max(800L, cleanRegions.get(r).getDuration());
            regCumEnd[r] = totalRegDur;
        }

        List<List<Integer>> regionStanzaIndices = new ArrayList<List<Integer>>(regionCount);
        for (int r = 0; r < regionCount; ++r) {
            regionStanzaIndices.add(new ArrayList<Integer>());
        }
        int curRegIdx = 0;
        long cumWeight = 0L;
        for (int s = 0; s < stanzaCount; ++s) {
            double centerRatio = totalWeight > 0 ? ((double)cumWeight + 0.5 * (double)stanzaWeights[s]) / (double)totalWeight : ((double)s + 0.5) / (double)stanzaCount;
            cumWeight += stanzaWeights[s];
            while (curRegIdx < regionCount - 1 && (double)regCumEnd[curRegIdx] / (double)totalRegDur < centerRatio) {
                ++curRegIdx;
            }
            regionStanzaIndices.get(curRegIdx).add(s);
        }

        long prevLineEnd = 0L;
        for (int r = 0; r < regionCount; ++r) {
            List<Integer> assigned = regionStanzaIndices.get(r);
            if (assigned.isEmpty()) continue;
            VocalRegion reg = cleanRegions.get(r);
            long regStart = Math.max(prevLineEnd, reg.startMs);
            long regEnd = Math.min(maxEndLimit, Math.max(regStart + (long)assigned.size() * 400L, reg.endMs));
            long regSpan = Math.max(400L, regEnd - regStart);
            int groupSize = assigned.size();
            long interStanzaPauseMs = groupSize > 1 ? Math.min(1800L, Math.max(300L, regSpan / (long)(groupSize * 10))) : 0L;
            if (interStanzaPauseMs * (long)(groupSize - 1) > regSpan / 4L) {
                interStanzaPauseMs = (regSpan / 4L) / (long)Math.max(1, groupSize - 1);
            }
            long availVocalMs = Math.max((long)groupSize * 300L, regSpan - interStanzaPauseMs * (long)(groupSize - 1));
            int groupWeightSum = 0;
            for (int stIdx : assigned) {
                groupWeightSum += stanzaWeights[stIdx];
            }
            long curTime = regStart;
            for (int g = 0; g < groupSize; ++g) {
                int stIdx = assigned.get(g);
                Stanza stanza = nonEmptyStanzas.get(stIdx);
                double stRatio = groupWeightSum > 0 ? (double)stanzaWeights[stIdx] / (double)groupWeightSum : 1.0 / (double)groupSize;
                long stanzaEndLimit = g == groupSize - 1 ? regEnd : Math.min(regEnd, curTime + Math.max(300L, (long)((double)availVocalMs * stRatio)));
                long stanzaDur = Math.max(250L, stanzaEndLimit - curTime);
                int lineCount = stanza.lines.size();
                int[] lineWeights = new int[lineCount];
                int lineWeightSum = 0;
                for (int l = 0; l < lineCount; ++l) {
                    String lt = stanza.lines.get(l);
                    int lw = Math.max(4, StudioCascadeAligner.countSyllables(lt) * 2 + lt.length() / 4);
                    lineWeights[l] = lw;
                    lineWeightSum += lw;
                }
                for (int l = 0; l < lineCount; ++l) {
                    String lineText = stanza.lines.get(l);
                    double lRatio = lineWeightSum > 0 ? (double)lineWeights[l] / (double)lineWeightSum : 1.0 / (double)lineCount;
                    long remainingLines = lineCount - l;
                    long maxSlot = Math.max(160L, (stanzaEndLimit - curTime) - (remainingLines - 1L) * 120L);
                    long slotDur = l == lineCount - 1 ? Math.max(160L, stanzaEndLimit - curTime) : Math.min(maxSlot, Math.max(160L, (long)((double)stanzaDur * lRatio)));
                    long lineStart = Math.min(maxEndLimit - 120L, Math.max(prevLineEnd, curTime));
                    long vocalDur = Math.max(100L, (long)((double)slotDur * 0.90));
                    long lineEnd = Math.min(maxEndLimit, Math.max(lineStart + 100L, lineStart + vocalDur));
                    List<LyricLine.Word> words = StudioCascadeAligner.segmentWordsBySyllables(lineText, lineStart, lineEnd);
                    result.add(new LyricLine(lineStart, lineEnd, lineText, words));
                    prevLineEnd = lineEnd;
                    curTime = Math.max(lineEnd, curTime + slotDur);
                }
                curTime = Math.min(regEnd, curTime + interStanzaPauseMs);
            }
        }
        return result;
    }

    public static List<LyricLine.Word> segmentWordsBySyllables(String lineText, long lineStartMs, long lineEndMs) {
        ArrayList<LyricLine.Word> words = new ArrayList<LyricLine.Word>();
        if (lineText == null || lineText.isBlank()) {
            return words;
        }
        String[] tokens = lineText.trim().split("\\s+");
        if (tokens.length == 0) {
            return words;
        }
        int totalSyllables = 0;
        int[] weights = new int[tokens.length];
        int totalWeight = 0;
        for (int i = 0; i < tokens.length; ++i) {
            String tok = tokens[i];
            int syllables = StudioCascadeAligner.countSyllables(tok);
            totalSyllables += syllables;
            int weight = syllables * 3 + Math.min(5, tok.length());
            char lastChar = tok.charAt(tok.length() - 1);
            if (lastChar == ',' || lastChar == ';') {
                weight += 2;
            } else if (lastChar == '.' || lastChar == '!' || lastChar == '?' || tok.endsWith("...")) {
                weight += 3;
            }
            weights[i] = weight;
            totalWeight += weight;
        }
        long lineSlot = Math.max(250L, lineEndMs - lineStartMs);
        long syllableBoundMs = (long)totalSyllables * 230L + (long)tokens.length * 90L + 160L;
        long totalDur = Math.min(lineSlot, Math.max(350L, syllableBoundMs));
        int charOffset = 0;
        long curStart = lineStartMs;
        for (int i = 0; i < tokens.length; ++i) {
            int charEnd;
            String token = tokens[i];
            int charStart = lineText.indexOf(token, charOffset);
            if (charStart < 0) {
                charStart = charOffset;
            }
            charOffset = charEnd = charStart + token.length();
            double ratio = totalWeight > 0 ? (double)weights[i] / (double)totalWeight : 1.0 / (double)tokens.length;
            long wordDur = Math.max(80L, (long)((double)totalDur * ratio));
            long wordEnd = i == tokens.length - 1 ? curStart + wordDur : Math.min(lineEndMs, curStart + (long)((double)wordDur * 0.90));
            words.add(new LyricLine.Word(curStart, wordEnd, token, charStart, charEnd));
            curStart += wordDur;
        }
        return words;
    }

    public static int countSyllables(String word) {
        if (word == null || word.isEmpty()) {
            return 1;
        }
        int count = 0;
        Matcher mRu = RU_VOWELS.matcher(word);
        while (mRu.find()) {
            ++count;
        }
        Matcher mEn = EN_VOWELS.matcher(word);
        while (mEn.find()) {
            ++count;
        }
        return Math.max(1, count);
    }

    public static float[] extractPcmSamples(byte[] wavBytes) {
        if (wavBytes == null || wavBytes.length <= 44) {
            return new float[0];
        }
        AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wavBytes);
        if (pcm != null && pcm.samples() != null && pcm.samples().length > 0) {
            return pcm.samples();
        }
        return new float[0];
    }

    public static AcousticEnergyProfile analyzeAcousticEnergy(byte[] wavBytes) {
        if (wavBytes == null || wavBytes.length <= 44) {
            return new AcousticEnergyProfile(0.0, 0.0, 0.0, 0.0, false, false);
        }
        AcousticLagEstimator.Pcm pcm = AcousticLagEstimator.decodeWav(wavBytes);
        if (pcm == null || pcm.samples() == null || pcm.sampleRate() <= 0) {
            return new AcousticEnergyProfile(0.0, 0.0, 0.0, 0.0, false, false);
        }
        float[] samples = pcm.samples();
        int sampleRate = pcm.sampleRate();
        int minSamples = Math.max(160, sampleRate / 10);
        if (samples.length < minSamples) {
            return new AcousticEnergyProfile(0.0, 0.0, 0.0, 0.0, false, false);
        }
        double sumSqTotal = 0.0;
        for (float s : samples) {
            sumSqTotal += (double)s * (double)s;
        }
        double rmsTotal = Math.sqrt(sumSqTotal / (double)samples.length);
        float[] filtered = StudioCascadeAligner.applyVocalBandpassFilter(samples, sampleRate);
        double sumSqVocal = 0.0;
        for (float f : filtered) {
            sumSqVocal += (double)f * (double)f;
        }
        double rmsVocal = Math.sqrt(sumSqVocal / (double)filtered.length);
        double vocalRatio = rmsVocal / Math.max(1.0E-4, rmsTotal);
        int frameSize = Math.max(1, (int)((double)sampleRate * 0.02));
        int totalFrames = filtered.length / frameSize;
        int voicedFrames = 0;
        for (int f = 0; f < totalFrames; ++f) {
            int offset = f * frameSize;
            double frameSum = 0.0;
            for (int i = 0; i < frameSize; ++i) {
                double val = filtered[offset + i];
                frameSum += val * val;
            }
            double frameRms = Math.sqrt(frameSum / (double)frameSize);
            if (!(frameRms > 0.012)) continue;
            ++voicedFrames;
        }
        double voicedFraction = totalFrames > 0 ? (double)voicedFrames / (double)totalFrames : 0.0;
        boolean isPlayingSound = rmsTotal > 0.005;
        boolean hasVocalPresence = isPlayingSound && (rmsVocal > 0.014 && voicedFraction > 0.12 || vocalRatio > 0.2 && rmsVocal > 0.008 || voicedFraction > 0.25);
        return new AcousticEnergyProfile(rmsTotal, rmsVocal, vocalRatio, voicedFraction, isPlayingSound, hasVocalPresence);
    }

    public static class Stanza {
        public final String header;
        public final List<String> lines = new ArrayList<String>();

        public Stanza(String header) {
            this.header = header;
        }
    }

    public static class VocalRegion {
        public final long startMs;
        public final long endMs;

        public VocalRegion(long startMs, long endMs) {
            this.startMs = startMs;
            this.endMs = endMs;
        }

        public long getDuration() {
            return this.endMs - this.startMs;
        }
    }

    public record AcousticEnergyProfile(double rmsTotal, double rmsVocal, double vocalRatio, double voicedFrameFraction, boolean isPlayingSound, boolean hasVocalPresence) {
    }
}

