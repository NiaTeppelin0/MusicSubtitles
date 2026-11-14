package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.lyrics.LyricLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LyricsConsensusEngine {
    public static Set<String> extractPhrases(List<LyricLine> list, String string) {
        HashSet<String> hashSet = new HashSet<String>();
        ArrayList<String> arrayList = new ArrayList<String>();
        if (list != null && !list.isEmpty()) {
            for (LyricLine object : list) {
                if (object == null || object.text == null) continue;
                arrayList.add(object.text);
            }
        } else if (string != null && !string.isBlank()) {
            String[] splitLines = LINE_SPLIT_PATTERN.split(string);
            arrayList.addAll(Arrays.asList(splitLines));
        }
        for (String string2 : arrayList) {
            String string3 = LyricsConsensusEngine.normalize(string2);
            if (string3.isEmpty()) continue;
            String[] stringArray = WHITESPACE_PATTERN.split(string3);
            if (stringArray.length >= 2) {
                hashSet.add(string3);
            } else if (string3.length() >= 3 && string3.codePoints().anyMatch(cp -> cp > 0x2E80)) {
                hashSet.add(string3);
                for (int i = 0; i <= string3.length() - 3; ++i) {
                    hashSet.add(string3.substring(i, i + 3));
                }
            }
            if (stringArray.length < 3) continue;
            for (int i = 0; i <= stringArray.length - 3; ++i) {
                hashSet.add(stringArray[i] + " " + stringArray[i + 1] + " " + stringArray[i + 2]);
            }
        }
        return hashSet;
    }

    public static Candidate selectBest(List<Candidate> list, long l) {
        return selectBest(list, l, "", "");
    }

    public static Candidate selectBest(List<Candidate> list, long l, String targetTitle, String targetArtist) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        ArrayList<Candidate> arrayList = new ArrayList<Candidate>();
        for (Candidate candidate3 : list) {
            if (candidate3 == null || candidate3.lines.isEmpty() && candidate3.rawText.isBlank()) continue;
            boolean duplicateOfSameProvider = false;
            for (Candidate existing : arrayList) {
                if (existing.provider.equalsIgnoreCase(candidate3.provider)) {
                    double sameOverlap = LyricsConsensusEngine.computePhraseOverlap(existing.phrases, candidate3.phrases);
                    if (sameOverlap >= 0.75) {
                        duplicateOfSameProvider = true;
                        break;
                    }
                }
            }
            if (!duplicateOfSameProvider) {
                arrayList.add(candidate3);
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }

        String targetCombined = ((targetTitle != null ? targetTitle : "") + " " + (targetArtist != null ? targetArtist : "")).trim();
        int targetCyrillic = countCyrillic(targetCombined);
        int targetCjk = countCjk(targetCombined);
        int targetLatin = countLatin(targetCombined);

        List<String> titleKeywords = new ArrayList<String>();
        if (targetTitle != null && !targetTitle.isBlank()) {
            String normT = normalize(targetTitle);
            for (String w : WHITESPACE_PATTERN.split(normT)) {
                if (w.length() >= 3) {
                    titleKeywords.add(w);
                }
            }
        }

        List<Candidate> noTitleMatchCandidates = new ArrayList<Candidate>();
        for (Candidate c : arrayList) {
            if (c.isSynced) {
                c.score += 150.0;
            }
            if (c.hasEnhancedWords) {
                c.score += 65.0;
            }
            if ("YANDEX_SYNCED".equalsIgnoreCase(c.provider)) {
                c.score += 130.0;
            } else if ("GENIUS".equalsIgnoreCase(c.provider)) {
                c.score += 120.0;
            } else if ("LRCLIB_GET".equalsIgnoreCase(c.provider)) {
                c.score += 115.0;
            } else if ("LRCLIB_SYNCED".equalsIgnoreCase(c.provider)) {
                c.score += 100.0;
            } else if ("LRCLIB_PLAIN".equalsIgnoreCase(c.provider)) {
                c.score += 40.0;
            } else if ("NETEASE".equalsIgnoreCase(c.provider)) {
                c.score += c.isSynced ? 85.0 : 20.0;
            } else if ("KUGOU".equalsIgnoreCase(c.provider)) {
                c.score += c.isSynced ? 90.0 : 20.0;
            } else if ("QQMUSIC".equalsIgnoreCase(c.provider)) {
                c.score += c.isSynced ? 80.0 : 20.0;
            } else if ("OVH".equalsIgnoreCase(c.provider)) {
                c.score += 20.0;
            }

            String sampleText = getSampleText(c);
            int candCyrillic = countCyrillic(sampleText);
            int candCjk = countCjk(sampleText);
            int candLatin = countLatin(sampleText);

            if (targetCyrillic >= 3 && candCyrillic == 0 && (candLatin >= 5 || candCjk >= 3)) {
                c.score -= 700.0;
                c.possibleMismatch = true;
            }
            if (targetCjk == 0 && candCjk > 10 && (targetLatin >= 4 || targetCyrillic >= 3)) {
                c.score -= 700.0;
                c.possibleMismatch = true;
            }
            if (targetCyrillic == 0 && targetCjk == 0 && targetLatin >= 4 && candCyrillic > 15) {
                c.score -= 700.0;
                c.possibleMismatch = true;
            }

            if (!titleKeywords.isEmpty() && !sampleText.isEmpty()) {
                String normSample = normalize(sampleText);
                int foundKw = 0;
                for (String kw : titleKeywords) {
                    if (normSample.contains(kw)) {
                        foundKw++;
                    }
                }
                if (foundKw > 0) {
                    c.score += 200.0 * ((double)foundKw / (double)titleKeywords.size());
                } else if (titleKeywords.size() >= 2) {
                    noTitleMatchCandidates.add(c);
                }
            }
        }

        for (int i = 0; i < arrayList.size(); ++i) {
            Candidate candidate3 = arrayList.get(i);
            for (int j = i + 1; j < arrayList.size(); ++j) {
                Candidate candidate5 = arrayList.get(j);
                if (candidate3.provider.equalsIgnoreCase(candidate5.provider)) continue;
                double d = LyricsConsensusEngine.computePhraseOverlap(candidate3.phrases, candidate5.phrases);
                boolean sameFamily = baseProviderFamily(candidate3.provider).equals(baseProviderFamily(candidate5.provider));
                if (!sameFamily && d >= 0.35) {
                    double d2 = 450.0 * d;
                    candidate3.score += d2;
                    candidate5.score += d2;
                    candidate3.hasCrossConsensus = true;
                    candidate5.hasCrossConsensus = true;
                } else if (sameFamily && d >= 0.50) {
                    candidate3.score += 60.0 * d;
                    candidate5.score += 60.0 * d;
                } else if (!sameFamily && d < 0.10) {
                    if ("YANDEX_SYNCED".equalsIgnoreCase(candidate3.provider) && "NETEASE".equalsIgnoreCase(candidate5.provider)) {
                        candidate5.score -= 450.0;
                    } else if ("YANDEX_SYNCED".equalsIgnoreCase(candidate5.provider) && "NETEASE".equalsIgnoreCase(candidate3.provider)) {
                        candidate3.score -= 450.0;
                    }
                }
            }
        }
        for (Candidate c : noTitleMatchCandidates) {
            if (!c.hasCrossConsensus) {
                c.score -= 180.0;
            }
        }

        if (l > 20L) {
            for (Candidate candidate3 : arrayList) {
                long candDurMs = candidate3.sourceDurationMs;
                if (candDurMs > 20000L) {
                    long durDiffSec = Math.abs(candDurMs / 1000L - l);
                    if (durDiffSec <= 2L) {
                        candidate3.score += 120.0;
                    } else if (durDiffSec <= 5L) {
                        candidate3.score += 60.0;
                    }
                }
                if (!candidate3.isSynced || candidate3.lines.isEmpty()) continue;
                long l2 = candidate3.lines.get(0).startMs;
                long l3 = candidate3.lines.get(candidate3.lines.size() - 1).endMs;
                long l4 = l * 1000L;
                long effectiveSourceMs = candDurMs > 20000L ? candDurMs : l3;
                if (!LyricsConsensusEngine.isTempoCompatibleDuration(effectiveSourceMs, l4)) {
                    if (l3 > l4 + 12000L) {
                        candidate3.score -= 600.0;
                        candidate3.possibleMismatch = true;
                    } else if ((double)l3 < (double)l4 * 0.45 && l4 > 50000L) {
                        candidate3.score -= 500.0;
                        candidate3.possibleMismatch = true;
                    }
                    if ((double)l2 > (double)l4 * 0.50 && l4 > 40000L) {
                        candidate3.score -= 500.0;
                        candidate3.possibleMismatch = true;
                    }
                }
            }
        }

        for (int i = 0; i < arrayList.size(); ++i) {
            Candidate c1 = arrayList.get(i);
            if (!c1.isSynced || c1.lines.isEmpty()) continue;
            for (int j = i + 1; j < arrayList.size(); ++j) {
                Candidate c2 = arrayList.get(j);
                if (!c2.isSynced || c2.lines.isEmpty()) continue;
                double overlap = LyricsConsensusEngine.computePhraseOverlap(c1.phrases, c2.phrases);
                if (overlap >= 0.55) {
                    long diff = Math.abs(c1.lines.get(0).startMs - c2.lines.get(0).startMs);
                    if (diff > 3500L) {
                        if ("YANDEX_SYNCED".equalsIgnoreCase(c1.provider)) {
                            c2.score -= 350.0;
                            c2.possibleMismatch = true;
                        } else if ("YANDEX_SYNCED".equalsIgnoreCase(c2.provider)) {
                            c1.score -= 350.0;
                            c1.possibleMismatch = true;
                        }
                    }
                }
            }
        }

        arrayList.sort((candidate, candidate2) -> Double.compare(candidate2.score, candidate.score));
        Candidate candidate6 = arrayList.get(0);
        if (candidate6.score < 80.0) {
            return null;
        }
        if (candidate6.possibleMismatch && !candidate6.hasCrossConsensus) {
            return null;
        }
        if ("NETEASE".equalsIgnoreCase(candidate6.provider) && !candidate6.hasCrossConsensus && candidate6.score < 220.0) {
            return null;
        }
        if (!candidate6.hasCrossConsensus && arrayList.size() > 1 && !candidate6.isSynced && candidate6.score < 200.0) {
            candidate6.possibleMismatch = true;
        }
        if (candidate6.isSynced && !candidate6.lines.isEmpty()) {
            candidate6.lines = LyricsConsensusEngine.fuseSyncedLines(candidate6, arrayList);
        }
        return candidate6;
    }

    private static boolean isTempoCompatibleDuration(long candidateMs, long trackMs) {
        if (candidateMs <= 20000L || trackMs <= 20000L) {
            return false;
        }
        double ratio = (double)trackMs / (double)candidateMs;
        return (ratio >= 0.65 && ratio <= 0.93) || (ratio >= 1.07 && ratio <= 1.55);
    }

    public static List<LyricLine> fuseSyncedLines(Candidate winner, List<Candidate> pool) {
        if (winner == null || !winner.isSynced || winner.lines == null || winner.lines.isEmpty() || pool == null || pool.size() <= 1) {
            return winner != null ? winner.lines : Collections.emptyList();
        }
        long winnerLastEnd = winner.lines.get(winner.lines.size() - 1).endMs;
        List<Candidate> peers = new ArrayList<Candidate>();
        for (Candidate peer : pool) {
            if (peer == null || peer == winner || !peer.isSynced || peer.possibleMismatch || peer.lines == null || peer.lines.isEmpty()) continue;
            if (LyricsConsensusEngine.computePhraseOverlap(winner.phrases, peer.phrases) < 0.60) continue;
            long peerLastEnd = peer.lines.get(peer.lines.size() - 1).endMs;
            if (Math.abs(winnerLastEnd - peerLastEnd) > 4500L) continue;
            peers.add(peer);
        }
        if (peers.isEmpty()) {
            return winner.lines;
        }
        ArrayList<LyricLine> fused = new ArrayList<LyricLine>(winner.lines.size());
        long minStart = 0L;
        for (LyricLine wLine : winner.lines) {
            if (wLine == null) continue;
            String normW = LyricsConsensusEngine.normalize(wLine.text);
            LyricLine bestPeerLine = null;
            boolean peerHasWords = false;
            double bestSim = 0.819;
            long bestTimeDist = Long.MAX_VALUE;
            for (Candidate peer : peers) {
                for (LyricLine pLine : peer.lines) {
                    if (pLine == null) continue;
                    long timeDist = Math.abs(wLine.startMs - pLine.startMs);
                    if (timeDist > 650L) continue;
                    double sim = LyricsConsensusEngine.computeNormalizedLineSimilarity(normW, LyricsConsensusEngine.normalize(pLine.text));
                    if (sim >= 0.82 && (sim > bestSim + 0.01 || (Math.abs(sim - bestSim) <= 0.01 && timeDist < bestTimeDist))) {
                        bestSim = sim;
                        bestTimeDist = timeDist;
                        bestPeerLine = pLine;
                        peerHasWords = peer.hasEnhancedWords;
                    }
                }
            }
            if (bestPeerLine != null) {
                long fusedStart = Math.max(minStart, Math.round((double)wLine.startMs * 0.65 + (double)bestPeerLine.startMs * 0.35));
                long deltaFromWinner = fusedStart - wLine.startMs;
                long dur = Math.max(60L, wLine.endMs - wLine.startMs);
                long vocalDur = Math.max(50L, wLine.vocalEndMs - wLine.startMs);
                long fusedEnd = fusedStart + dur;
                long fusedVocalEnd = Math.min(fusedEnd, fusedStart + vocalDur);
                List<LyricLine.Word> fusedWords = null;
                String lineText = wLine.rawText != null && !wLine.rawText.isEmpty() ? wLine.rawText : wLine.text;
                if (!winner.hasEnhancedWords && peerHasWords && bestPeerLine.words != null && !bestPeerLine.words.isEmpty() && normW.equals(LyricsConsensusEngine.normalize(bestPeerLine.text))) {
                    long wordDelta = fusedStart - bestPeerLine.startMs;
                    ArrayList<LyricLine.Word> shifted = new ArrayList<LyricLine.Word>(bestPeerLine.words.size());
                    for (LyricLine.Word pw : bestPeerLine.words) {
                        long ws = Math.max(0L, pw.startMs() + wordDelta);
                        long we = Math.max(ws + 40L, pw.endMs() + wordDelta);
                        shifted.add(new LyricLine.Word(ws, we, pw.text(), pw.charStart(), pw.charEnd()));
                    }
                    fusedWords = shifted;
                    long lastWordEnd = shifted.get(shifted.size() - 1).endMs();
                    fusedVocalEnd = Math.max(fusedStart + 50L, lastWordEnd);
                    fusedEnd = Math.max(fusedEnd, fusedVocalEnd);
                } else if (wLine.words != null && !wLine.words.isEmpty()) {
                    ArrayList<LyricLine.Word> shifted = new ArrayList<LyricLine.Word>(wLine.words.size());
                    for (LyricLine.Word ww : wLine.words) {
                        long ws = Math.max(0L, ww.startMs() + deltaFromWinner);
                        long we = Math.max(ws + 40L, ww.endMs() + deltaFromWinner);
                        shifted.add(new LyricLine.Word(ws, we, ww.text(), ww.charStart(), ww.charEnd()));
                    }
                    fusedWords = shifted;
                }
                fused.add(new LyricLine(fusedStart, fusedEnd, fusedVocalEnd, lineText, fusedWords));
                minStart = fusedStart;
            } else {
                long safeStart = Math.max(minStart, wLine.startMs);
                if (safeStart == wLine.startMs) {
                    fused.add(wLine);
                } else {
                    long delta = safeStart - wLine.startMs;
                    ArrayList<LyricLine.Word> shifted = null;
                    if (wLine.words != null && !wLine.words.isEmpty()) {
                        shifted = new ArrayList<LyricLine.Word>(wLine.words.size());
                        for (LyricLine.Word ww : wLine.words) {
                            long ws = Math.max(0L, ww.startMs() + delta);
                            long we = Math.max(ws + 40L, ww.endMs() + delta);
                            shifted.add(new LyricLine.Word(ws, we, ww.text(), ww.charStart(), ww.charEnd()));
                        }
                    }
                    fused.add(new LyricLine(safeStart, wLine.endMs + delta, wLine.vocalEndMs + delta, wLine.rawText != null ? wLine.rawText : wLine.text, shifted));
                }
                minStart = safeStart;
            }
        }
        return fused;
    }

    private static double computeNormalizedLineSimilarity(String normA, String normB) {
        if (normA == null || normB == null || normA.isEmpty() || normB.isEmpty()) {
            return 0.0;
        }
        if (normA.equals(normB)) {
            return 1.0;
        }
        String[] wordsA = WHITESPACE_PATTERN.split(normA);
        String[] wordsB = WHITESPACE_PATTERN.split(normB);
        HashSet<String> setA = new HashSet<String>(Arrays.asList(wordsA));
        HashSet<String> setB = new HashSet<String>(Arrays.asList(wordsB));
        int minSize = Math.min(setA.size(), setB.size());
        if (minSize == 0) {
            return 0.0;
        }
        int inter = 0;
        for (String w : setA) {
            if (setB.contains(w)) {
                ++inter;
            }
        }
        return (double)inter / (double)Math.max(setA.size(), setB.size());
    }

    private static String baseProviderFamily(String provider) {
        if (provider == null) return "";
        String upper = provider.toUpperCase(Locale.ROOT);
        if (upper.startsWith("LRCLIB")) return "LRCLIB";
        if (upper.startsWith("YANDEX")) return "YANDEX";
        return upper;
    }

    private static String getSampleText(Candidate c) {
        if (c.rawText != null && !c.rawText.isBlank()) {
            return c.rawText;
        }
        if (c.lines != null && !c.lines.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(25, c.lines.size());
            for (int i = 0; i < limit; i++) {
                LyricLine line = c.lines.get(i);
                if (line != null && line.text != null) {
                    sb.append(line.text).append(" ");
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static int countCyrillic(String s) {
        if (s == null) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ((c >= '\u0400' && c <= '\u04ff') || (c >= '\u0500' && c <= '\u052f')) {
                ++count;
            }
        }
        return count;
    }

    private static int countCjk(String s) {
        if (s == null) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ((c >= '\u4e00' && c <= '\u9fff') || (c >= '\u3400' && c <= '\u4dbf')) {
                ++count;
            }
        }
        return count;
    }

    private static int countLatin(String s) {
        if (s == null) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                ++count;
            }
        }
        return count;
    }

    public static double computePhraseOverlap(Set<String> set, Set<String> set2) {
        if (set == null || set2 == null || set.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }
        int n = Math.min(set.size(), set2.size());
        if (n == 0) {
            return 0.0;
        }
        int n2 = 0;
        for (String string : set) {
            if (!set2.contains(string)) continue;
            ++n2;
        }
        return (double)n2 / (double)n;
    }

    private static final java.util.regex.Pattern LINE_SPLIT_PATTERN = java.util.regex.Pattern.compile("\\r?\\n");
    private static final java.util.regex.Pattern WHITESPACE_PATTERN = java.util.regex.Pattern.compile("\\s+");
    private static final java.util.regex.Pattern NON_ALNUM_PATTERN = java.util.regex.Pattern.compile("[^\\p{L}\\p{N}\\s]");

    private static String normalize(String string) {
        if (string == null) {
            return "";
        }
        String noPunct = NON_ALNUM_PATTERN.matcher(string.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435')).replaceAll("");
        return WHITESPACE_PATTERN.matcher(noPunct).replaceAll(" ").trim();
    }

    public static class Candidate {
        public final String provider;
        public List<LyricLine> lines;
        public final String rawText;
        public final boolean isSynced;
        public final Set<String> phrases;
        public long sourceDurationMs = 0L;
        public boolean hasEnhancedWords = false;
        public double score = 0.0;
        public boolean hasCrossConsensus = false;
        public boolean possibleMismatch = false;

        public Candidate(String string, List<LyricLine> list, String string2, boolean bl) {
            this(string, list, string2, bl, 0L, false);
        }

        public Candidate(String string, List<LyricLine> list, String string2, boolean bl, long sourceDurationMs, boolean hasEnhancedWords) {
            this.provider = string != null ? string : "UNKNOWN";
            this.lines = list != null ? list : Collections.emptyList();
            this.rawText = string2 != null ? string2 : "";
            this.isSynced = bl;
            this.sourceDurationMs = Math.max(0L, sourceDurationMs);
            this.hasEnhancedWords = hasEnhancedWords;
            this.phrases = LyricsConsensusEngine.extractPhrases(this.lines, this.rawText);
        }
    }
}
