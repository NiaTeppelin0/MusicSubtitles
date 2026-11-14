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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String combined = (targetTitle != null ? targetTitle : "") + " " + (targetArtist != null ? targetArtist : "");
        boolean allowTempoScaling = VERSION_TEMPO_PATTERN.matcher(combined).find();
        return selectBest(list, l, targetTitle, targetArtist, allowTempoScaling);
    }

    public static Candidate selectBest(List<Candidate> list, long l, String targetTitle, String targetArtist, boolean allowTempoScaling) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        ArrayList<Candidate> arrayList = new ArrayList<Candidate>();
        for (Candidate candidate3 : list) {
            if (candidate3 == null || candidate3.lines.isEmpty() && candidate3.rawText.isBlank()) continue;
            if (LyricsConsensusEngine.isMeaninglessLyricsContent(candidate3.lines, candidate3.rawText, l)) continue;
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

        String safeTitle = targetTitle != null ? targetTitle.trim() : "";
        String safeArtist = targetArtist != null ? targetArtist.trim() : "";
        String targetCombined = (safeTitle + " " + safeArtist).trim();
        int targetCyrillic = countCyrillic(targetCombined);
        int targetCjk = countCjk(targetCombined);
        int targetLatin = countLatin(targetCombined);

        List<String> titleKeywords = new ArrayList<String>();
        if (!safeTitle.isEmpty()) {
            String normT = normalize(safeTitle);
            for (String w : WHITESPACE_PATTERN.split(normT)) {
                if (w.length() >= 3) {
                    titleKeywords.add(w);
                }
            }
        }

        List<Candidate> noTitleMatchCandidates = new ArrayList<Candidate>();
        for (Candidate c : arrayList) {
            if (c.titleSim < 0.0 && !c.matchedTitle.isEmpty() && !safeTitle.isEmpty()) {
                c.titleSim = LyricsConsensusEngine.computeTitleSimilarity(safeTitle, c.matchedTitle);
            }
            if (c.artistSim < 0.0 && !c.matchedArtist.isEmpty() && !safeArtist.isEmpty()) {
                c.artistSim = LyricsConsensusEngine.computeArtistSimilarity(safeArtist, c.matchedArtist);
            }
            if (!c.matchedTitle.isEmpty() && !safeTitle.isEmpty() && LyricsConsensusEngine.hasVersionConflict(safeTitle, c.matchedTitle)) {
                c.versionConflict = true;
            }

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
                c.score += 10.0;
            }

            if (c.versionConflict) {
                c.score -= 900.0;
                c.possibleMismatch = true;
            }
            if (!safeTitle.isEmpty() && c.titleSim >= 0.0) {
                if (c.titleSim < 0.50) {
                    c.score -= 800.0;
                    c.possibleMismatch = true;
                } else if (c.titleSim >= 0.85) {
                    c.score += 110.0;
                } else {
                    c.score += 70.0 * c.titleSim;
                }
            }
            long durDiffSecMeta = (l > 20L && c.sourceDurationMs > 20000L) ? Math.abs(c.sourceDurationMs / 1000L - l) : -1L;
            if (!safeArtist.isEmpty()) {
                if (c.artistSim >= 0.0) {
                    if (c.artistSim < 0.32) {
                        boolean exactDurSpecific = c.titleSim >= 0.92 && LyricsConsensusEngine.isSpecificTitle(c.matchedTitle) && durDiffSecMeta >= 0L && durDiffSecMeta <= 2L;
                        if (exactDurSpecific) {
                            c.score -= 100.0;
                        } else {
                            c.score -= 850.0;
                            c.possibleMismatch = true;
                        }
                    } else if (c.artistSim >= 0.78) {
                        c.score += 120.0;
                    } else {
                        c.score += 80.0 * c.artistSim;
                    }
                } else {
                    c.score -= 55.0;
                }
            }

            String sampleText = getSampleText(c);
            int candCyrillic = countCyrillic(sampleText);
            int candCjk = countCjk(sampleText);
            int candLatin = countLatin(sampleText);

            if (targetCyrillic >= 3 && targetLatin <= 2 && candCyrillic == 0 && (candLatin >= 15 || candCjk >= 3)) {
                c.score -= 700.0;
                c.possibleMismatch = true;
            }
            if (targetCjk == 0 && candCjk >= 12 && (targetLatin >= 3 || targetCyrillic >= 2)) {
                c.score -= 700.0;
                c.possibleMismatch = true;
            }
            if (targetCyrillic == 0 && targetCjk == 0 && targetLatin >= 4 && candCyrillic >= 25 && candLatin < candCyrillic) {
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
                    c.score += 180.0 * ((double)foundKw / (double)titleKeywords.size());
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
                if (!sameFamily && d >= 0.38) {
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
            long l4 = l * 1000L;
            for (Candidate candidate3 : arrayList) {
                long candDurMs = candidate3.sourceDurationMs;
                if (candDurMs > 20000L) {
                    long durDiffSec = Math.abs(candDurMs / 1000L - l);
                    if (durDiffSec <= 2L) {
                        candidate3.score += 120.0;
                    } else if (durDiffSec <= 5L) {
                        candidate3.score += 60.0;
                    } else if (!(allowTempoScaling && LyricsConsensusEngine.isTempoCompatibleDuration(candDurMs, l4)) && Math.abs(candDurMs - l4) > 15000L) {
                        candidate3.score -= 600.0;
                        candidate3.possibleMismatch = true;
                    }
                }
                if (!candidate3.isSynced || candidate3.lines.isEmpty()) continue;
                long l2 = candidate3.lines.get(0).startMs;
                long l3 = candidate3.lines.get(candidate3.lines.size() - 1).endMs;
                long effectiveSourceMs = candDurMs > 20000L ? candDurMs : l3;
                if (!(allowTempoScaling && LyricsConsensusEngine.isTempoCompatibleDuration(effectiveSourceMs, l4))) {
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
        if (candidate6.score < 110.0) {
            return null;
        }
        if (candidate6.versionConflict) {
            return null;
        }
        if (candidate6.possibleMismatch && !candidate6.hasCrossConsensus) {
            return null;
        }
        boolean fatalMetaMismatch = (!safeArtist.isEmpty() && candidate6.artistSim >= 0.0 && candidate6.artistSim < 0.25)
            || (!safeTitle.isEmpty() && candidate6.titleSim >= 0.0 && candidate6.titleSim < 0.45);
        if (candidate6.possibleMismatch && fatalMetaMismatch) {
            return null;
        }
        boolean isAsianFallback = "NETEASE".equalsIgnoreCase(candidate6.provider)
            || "KUGOU".equalsIgnoreCase(candidate6.provider)
            || "QQMUSIC".equalsIgnoreCase(candidate6.provider);
        if (isAsianFallback && !candidate6.hasCrossConsensus) {
            if (candidate6.score < 220.0) {
                return null;
            }
            if (!safeArtist.isEmpty() && candidate6.artistSim < 0.45) {
                return null;
            }
            if (!safeTitle.isEmpty() && candidate6.titleSim >= 0.0 && candidate6.titleSim < 0.72) {
                return null;
            }
        }
        if ("OVH".equalsIgnoreCase(candidate6.provider) && !candidate6.hasCrossConsensus) {
            if (safeArtist.isEmpty() || !LyricsConsensusEngine.isSpecificTitle(safeTitle) && arrayList.size() > 1) {
                return null;
            }
        }
        if (!candidate6.hasCrossConsensus && arrayList.size() > 1 && !candidate6.isSynced && candidate6.score < 190.0) {
            return null;
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
    private static final java.util.regex.Pattern INSTRUMENTAL_LINE_PATTERN = java.util.regex.Pattern.compile("(?iu)^\\[?(?:instrumental|\u0441\u043e\u043b\u043e|guitar\\s*solo|\u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u0430\u043b|\u043c\u0443\u0437\u044b\u043a\u0430|intro|outro|solo|music)\\]?$");
    private static final java.util.regex.Pattern CREDIT_LINE_PATTERN = Pattern.compile("(?iu)^(?:lyrics|composed|arranged|produced|vocals?|written|mix(?:ed)?|master(?:ed)?|\u0441\u043b\u043e\u0432\u0430|\u043c\u0443\u0437\u044b\u043a\u0430|\u0430\u0432\u0442\u043e\u0440|\u4f5c\u8bcd|\u4f5c\u66f2|\u7f16\u66f2|\u5236\u4f5c\u4eba|\u6f14\u5531)\\s*(?:by|:|\uff1a).*$");
    private static final Pattern VERSION_INSTRUMENTAL_PATTERN = Pattern.compile("(?iu)(?:^|[\\s(\\[\\-])(?:instrumental|\u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u0430\u043b|karaoke|\u043a\u0430\u0440\u0430\u043e\u043a\u0435|off\\s*vocal|backing\\s*track|\u043c\u0438\u043d\u0443\u0441)(?:$|[\\s)\\]\\-])");
    private static final Pattern VERSION_COVER_PATTERN = Pattern.compile("(?iu)(?:^|[\\s(\\[\\-])(?:cover|\u043a\u0430\u0432\u0435\u0440|parody|\u043f\u0430\u0440\u043e\u0434\u0438\u044f|tribute|ai\\s*cover)(?:$|[\\s)\\]\\-])");
    private static final Pattern VERSION_REMIX_PATTERN = Pattern.compile("(?iu)(?:^|[\\s(\\[\\-])(?:remix|rmx|\u0440\u0435\u043c\u0438\u043a\u0441|mashup|\u043c\u044d\u0448\u0430\u043f|vip\\s*mix|club\\s*mix|dub\\s*mix|bootleg)(?:$|[\\s)\\]\\-])");
    private static final Pattern VERSION_TEMPO_PATTERN = Pattern.compile("(?iu)(?:^|[\\s(\\[\\-])(?:sp(?:e|ed)\\s*up|nightcore|slowed(?:\\s*\\+?\\s*reverb)?)(?:$|[\\s)\\]\\-])");
    private static final Pattern ARTIST_NOISE_PATTERN = Pattern.compile("(?iu)\\s*(?:-\\s*(?:topic|\u0442\u0435\u043c\u0430)|official\\s*(?:channel|artist|music)?|vevo)\\s*$");
    private static final Pattern ARTIST_SPLIT_PATTERN = Pattern.compile("(?iu)\\s*(?:feat\\.?|ft\\.?|featuring|vs\\.?|and|&|,|/|\\u00d7|\\+)\\s*|\\s+x\\s+");
    private static final Set<String> COMMON_STOPWORDS = Set.of(
        "the", "and", "you", "for", "that", "this", "with", "from", "have", "your", "are", "was", "not", "but", "all",
        "just", "like", "know", "what", "when", "out", "can", "will", "yeah", "ooh", "aah", "hey", "baby", "love",
        "\u0447\u0442\u043e", "\u043a\u0430\u043a", "\u044d\u0442\u043e", "\u0442\u0430\u043a", "\u043c\u043d\u0435",
        "\u0442\u0435\u0431\u0435", "\u0442\u0435\u0431\u044f", "\u043c\u0435\u043d\u044f", "\u0434\u043b\u044f",
        "\u0438\u043b\u0438", "\u0433\u0434\u0435", "\u0432\u0441\u0435", "\u0432\u0441\u0451", "\u043e\u043d\u0430",
        "\u043e\u043d\u0438", "\u043d\u0430\u0434\u043e", "\u0442\u043e\u043b\u044c\u043a\u043e", "\u0430\u0433\u0430", "\u0443\u0433\u0443"
    );

    public record TranscriptMatchAssessment(
        boolean confidentSpeech,
        boolean matchedLyrics,
        boolean strongMismatch,
        int heardWordCount,
        int matchedWords,
        double bestLineCharOverlap,
        boolean scriptContradiction,
        String reason
    ) {}

    public static String transliterateRuEn(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        boolean hasCyr = false;
        boolean hasLat = false;
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if (c >= '\u0400' && c <= '\u04ff') {
                hasCyr = true;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasLat = true;
            }
        }
        String low = input.toLowerCase(Locale.ROOT);
        if (hasCyr && !hasLat) {
            StringBuilder sb = new StringBuilder(low.length() * 2);
            for (int i = 0; i < low.length(); ++i) {
                char c = low.charAt(i);
                switch (c) {
                    case '\u0430': sb.append('a'); break;
                    case '\u0431': sb.append('b'); break;
                    case '\u0432': sb.append('v'); break;
                    case '\u0433': sb.append('g'); break;
                    case '\u0434': sb.append('d'); break;
                    case '\u0435': sb.append('e'); break;
                    case '\u0451': sb.append("yo"); break;
                    case '\u0436': sb.append("zh"); break;
                    case '\u0437': sb.append('z'); break;
                    case '\u0438': sb.append('i'); break;
                    case '\u0439': sb.append('y'); break;
                    case '\u043a': sb.append('k'); break;
                    case '\u043b': sb.append('l'); break;
                    case '\u043c': sb.append('m'); break;
                    case '\u043d': sb.append('n'); break;
                    case '\u043e': sb.append('o'); break;
                    case '\u043f': sb.append('p'); break;
                    case '\u0440': sb.append('r'); break;
                    case '\u0441': sb.append('s'); break;
                    case '\u0442': sb.append('t'); break;
                    case '\u0443': sb.append('u'); break;
                    case '\u0444': sb.append('f'); break;
                    case '\u0445': sb.append("kh"); break;
                    case '\u0446': sb.append("ts"); break;
                    case '\u0447': sb.append("ch"); break;
                    case '\u0448': sb.append("sh"); break;
                    case '\u0449': sb.append("shch"); break;
                    case '\u044a': break;
                    case '\u044b': sb.append('y'); break;
                    case '\u044c': break;
                    case '\u044d': sb.append('e'); break;
                    case '\u044e': sb.append("yu"); break;
                    case '\u044f': sb.append("ya"); break;
                    default: sb.append(c); break;
                }
            }
            return sb.toString();
        }
        if (hasLat && !hasCyr) {
            String s = low.replace("shch", "\u0449")
                    .replace("ch", "\u0447")
                    .replace("sh", "\u0448")
                    .replace("zh", "\u0436")
                    .replace("kh", "\u0445")
                    .replace("ts", "\u0446")
                    .replace("yu", "\u044e")
                    .replace("ya", "\u044f")
                    .replace("yo", "\u0451")
                    .replace("ye", "\u0435");
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                switch (c) {
                    case 'a': sb.append('\u0430'); break;
                    case 'b': sb.append('\u0431'); break;
                    case 'c': sb.append('\u043a'); break;
                    case 'd': sb.append('\u0434'); break;
                    case 'e': sb.append('\u0435'); break;
                    case 'f': sb.append('\u0444'); break;
                    case 'g': sb.append('\u0433'); break;
                    case 'h': sb.append('\u0445'); break;
                    case 'i': sb.append('\u0438'); break;
                    case 'j': sb.append("\u0434\u0436"); break;
                    case 'k': sb.append('\u043a'); break;
                    case 'l': sb.append('\u043b'); break;
                    case 'm': sb.append('\u043c'); break;
                    case 'n': sb.append('\u043d'); break;
                    case 'o': sb.append('\u043e'); break;
                    case 'p': sb.append('\u043f'); break;
                    case 'q': sb.append('\u043a'); break;
                    case 'r': sb.append('\u0440'); break;
                    case 's': sb.append('\u0441'); break;
                    case 't': sb.append('\u0442'); break;
                    case 'u': sb.append('\u0443'); break;
                    case 'v': sb.append('\u0432'); break;
                    case 'w': sb.append('\u0432'); break;
                    case 'x': sb.append("\u043a\u0441"); break;
                    case 'y': sb.append('\u0439'); break;
                    case 'z': sb.append('\u0437'); break;
                    default: sb.append(c); break;
                }
            }
            return sb.toString();
        }
        return input;
    }

    private static String toCanonicalLatinPhonetic(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String low = input.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
        StringBuilder sb = new StringBuilder(low.length() * 2);
        for (int i = 0; i < low.length(); ++i) {
            char c = low.charAt(i);
            switch (c) {
                case '\u0430': sb.append('a'); break;
                case '\u0431': sb.append('b'); break;
                case '\u0432': sb.append('v'); break;
                case '\u0433': sb.append('g'); break;
                case '\u0434': sb.append('d'); break;
                case '\u0435': sb.append('e'); break;
                case '\u0436': sb.append("zh"); break;
                case '\u0437': sb.append('z'); break;
                case '\u0438': sb.append('i'); break;
                case '\u0439': sb.append('y'); break;
                case '\u043a': sb.append('k'); break;
                case '\u043b': sb.append('l'); break;
                case '\u043c': sb.append('m'); break;
                case '\u043d': sb.append('n'); break;
                case '\u043e': sb.append('o'); break;
                case '\u043f': sb.append('p'); break;
                case '\u0440': sb.append('r'); break;
                case '\u0441': sb.append('s'); break;
                case '\u0442': sb.append('t'); break;
                case '\u0443': sb.append('u'); break;
                case '\u0444': sb.append('f'); break;
                case '\u0445': sb.append('h'); break;
                case '\u0446': sb.append('c'); break;
                case '\u0447': sb.append("ch"); break;
                case '\u0448': sb.append("sh"); break;
                case '\u0449': sb.append("sh"); break;
                case '\u044a': break;
                case '\u044b': sb.append('y'); break;
                case '\u044c': break;
                case '\u044d': sb.append('e'); break;
                case '\u044e': sb.append("u"); break;
                case '\u044f': sb.append("ia"); break;
                default: sb.append(c); break;
            }
        }
        String folded = sb.toString()
            .replace("shch", "sh")
            .replace("sch", "sh")
            .replace("kh", "h")
            .replace("ts", "c")
            .replace("yo", "e")
            .replace("ye", "e")
            .replace("yu", "u")
            .replace("ju", "u")
            .replace("iu", "u")
            .replace("ya", "ia")
            .replace("ja", "ia")
            .replace("yj", "y")
            .replace("yy", "y")
            .replace("iy", "y")
            .replace("ij", "y")
            .replace('w', 'v')
            .replace('j', 'y');
        return LyricsConsensusEngine.normalize(folded);
    }

    private static double computeSingleNormalizedSimilarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        if ((a.length() >= 3 && b.length() >= 3) && (a.contains(b) || b.contains(a))) {
            return (double)Math.min(a.length(), b.length()) / (double)Math.max(a.length(), b.length());
        }
        HashSet<String> wordsA = new HashSet<String>(Arrays.asList(WHITESPACE_PATTERN.split(a)));
        HashSet<String> wordsB = new HashSet<String>(Arrays.asList(WHITESPACE_PATTERN.split(b)));
        wordsA.removeIf(w -> w.length() < 2 && (w.isEmpty() || w.codePointAt(0) <= 0x2E80));
        wordsB.removeIf(w -> w.length() < 2 && (w.isEmpty() || w.codePointAt(0) <= 0x2E80));
        if (wordsA.isEmpty() || wordsB.isEmpty()) {
            return 0.0;
        }
        HashSet<String> intersection = new HashSet<String>(wordsA);
        intersection.retainAll(wordsB);
        HashSet<String> union = new HashSet<String>(wordsA);
        union.addAll(wordsB);
        return (double)intersection.size() / (double)union.size();
    }

    public static double computeTitleSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        String a = LyricsConsensusEngine.normalize(s1);
        String b = LyricsConsensusEngine.normalize(s2);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        double direct = LyricsConsensusEngine.computeSingleNormalizedSimilarity(a, b);
        if (direct >= 0.99) {
            return direct;
        }
        String ta = LyricsConsensusEngine.normalize(LyricsConsensusEngine.transliterateRuEn(s1));
        String tb = LyricsConsensusEngine.normalize(LyricsConsensusEngine.transliterateRuEn(s2));
        double trans1 = LyricsConsensusEngine.computeSingleNormalizedSimilarity(ta, b) * 0.90;
        double trans2 = LyricsConsensusEngine.computeSingleNormalizedSimilarity(a, tb) * 0.90;
        String pa = LyricsConsensusEngine.toCanonicalLatinPhonetic(s1);
        String pb = LyricsConsensusEngine.toCanonicalLatinPhonetic(s2);
        double phon = (!pa.isEmpty() && !pb.isEmpty()) ? LyricsConsensusEngine.computeSingleNormalizedSimilarity(pa, pb) * 0.88 : 0.0;
        return Math.max(direct, Math.max(phon, Math.max(trans1, trans2)));
    }

    public static double computeArtistSimilarity(String targetArtist, String candidateArtist) {
        if (targetArtist == null || targetArtist.isBlank() || candidateArtist == null || candidateArtist.isBlank()) {
            return -1.0;
        }
        String cleanT = ARTIST_NOISE_PATTERN.matcher(targetArtist).replaceAll("").trim();
        String cleanC = ARTIST_NOISE_PATTERN.matcher(candidateArtist).replaceAll("").trim();
        double whole = LyricsConsensusEngine.computeTitleSimilarity(cleanT, cleanC);
        if (whole >= 0.95) {
            return whole;
        }
        String[] partsT = ARTIST_SPLIT_PATTERN.split(cleanT);
        String[] partsC = ARTIST_SPLIT_PATTERN.split(cleanC);
        double bestPair = 0.0;
        for (String pt : partsT) {
            String nt = LyricsConsensusEngine.normalize(pt);
            if (nt.isEmpty()) continue;
            for (String pc : partsC) {
                String nc = LyricsConsensusEngine.normalize(pc);
                if (nc.isEmpty()) continue;
                if (nt.equals(nc)) {
                    bestPair = Math.max(bestPair, partsT.length > 1 || partsC.length > 1 ? 0.90 : 1.0);
                    continue;
                }
                if (nt.length() <= 2 || nc.length() <= 2) {
                    continue;
                }
                double sim = LyricsConsensusEngine.computeTitleSimilarity(pt, pc);
                if (partsT.length > 1 || partsC.length > 1) {
                    sim = Math.min(0.90, sim);
                }
                bestPair = Math.max(bestPair, sim);
            }
        }
        return Math.max(whole, bestPair);
    }

    public static boolean hasVersionConflict(String targetTitle, String candidateTitle) {
        if (targetTitle == null || candidateTitle == null || candidateTitle.isBlank()) {
            return false;
        }
        boolean targInstr = VERSION_INSTRUMENTAL_PATTERN.matcher(targetTitle).find();
        boolean candInstr = VERSION_INSTRUMENTAL_PATTERN.matcher(candidateTitle).find();
        if (!targInstr && candInstr || targInstr && !candInstr) {
            return true;
        }
        boolean targCover = VERSION_COVER_PATTERN.matcher(targetTitle).find();
        boolean candCover = VERSION_COVER_PATTERN.matcher(candidateTitle).find();
        if (!targCover && candCover) {
            return true;
        }
        boolean targRemix = VERSION_REMIX_PATTERN.matcher(targetTitle).find();
        boolean candRemix = VERSION_REMIX_PATTERN.matcher(candidateTitle).find();
        return !targRemix && candRemix;
    }

    public static boolean isSpecificTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String norm = LyricsConsensusEngine.normalize(title);
        if (norm.replace(" ", "").length() >= 11) {
            return true;
        }
        int contentWords = 0;
        for (String w : WHITESPACE_PATTERN.split(norm)) {
            if (w.length() >= 3) {
                contentWords++;
            }
        }
        return contentWords >= 2;
    }

    public static boolean isMeaninglessLyricsContent(List<LyricLine> lines, String rawText, long trackDurationSec) {
        int totalMeaningfulWords = 0;
        int totalNonMetaLines = 0;
        if (lines != null && !lines.isEmpty()) {
            for (LyricLine line : lines) {
                if (line == null || line.text == null) continue;
                String t = line.text.trim();
                if (t.isEmpty() || INSTRUMENTAL_LINE_PATTERN.matcher(t).matches() || CREDIT_LINE_PATTERN.matcher(t).matches()) continue;
                totalNonMetaLines++;
                String norm = LyricsConsensusEngine.normalize(t);
                if (norm.isEmpty()) continue;
                for (String w : WHITESPACE_PATTERN.split(norm)) {
                    if (!w.isEmpty()) {
                        totalMeaningfulWords++;
                    }
                }
            }
        } else if (rawText != null && !rawText.isBlank()) {
            for (String rawLine : LINE_SPLIT_PATTERN.split(rawText)) {
                String t = rawLine.trim();
                if (t.isEmpty() || t.startsWith("[") && t.endsWith("]") || INSTRUMENTAL_LINE_PATTERN.matcher(t).matches() || CREDIT_LINE_PATTERN.matcher(t).matches()) continue;
                totalNonMetaLines++;
                String norm = LyricsConsensusEngine.normalize(t);
                if (norm.isEmpty()) continue;
                for (String w : WHITESPACE_PATTERN.split(norm)) {
                    if (!w.isEmpty()) {
                        totalMeaningfulWords++;
                    }
                }
            }
        }
        if (totalNonMetaLines == 0) {
            return true;
        }
        return (trackDurationSec <= 0L || trackDurationSec > 20L) && totalMeaningfulWords < 4;
    }

    private static Set<String> extractCharTrigrams(String normalized) {
        if (normalized == null) {
            return Collections.emptySet();
        }
        String compact = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
        if (compact.length() < 3) {
            return Collections.emptySet();
        }
        HashSet<String> trigrams = new HashSet<String>();
        for (int i = 0; i <= compact.length() - 3; ++i) {
            trigrams.add(compact.substring(i, i + 3));
        }
        return trigrams;
    }

    private static boolean tokensFuzzyMatch(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        if (a.length() >= 4 && b.length() >= 4) {
            int minLen = Math.min(a.length(), b.length());
            int prefix = 0;
            while (prefix < minLen && a.charAt(prefix) == b.charAt(prefix)) {
                prefix++;
            }
            if (prefix >= 4 && prefix >= minLen - 2 && Math.abs(a.length() - b.length()) <= 3) {
                return true;
            }
        }
        return false;
    }

    public static TranscriptMatchAssessment evaluateTranscriptAgainstLyrics(
        String heardText,
        double confidence,
        List<LyricLine> lines,
        long capturePosMs,
        long captureDurMs
    ) {
        if (heardText == null || heardText.isBlank() || lines == null || lines.isEmpty()) {
            return new TranscriptMatchAssessment(false, false, false, 0, 0, 0.0, false, "empty");
        }
        String normHeard = LyricsConsensusEngine.normalize(heardText);
        if (normHeard.isEmpty()) {
            return new TranscriptMatchAssessment(false, false, false, 0, 0, 0.0, false, "empty");
        }
        ArrayList<String> heardAllWords = new ArrayList<String>();
        ArrayList<String> heardMeaningfulWords = new ArrayList<String>();
        ArrayList<String> heardTranslitWords = new ArrayList<String>();
        for (String w : WHITESPACE_PATTERN.split(normHeard)) {
            if (w.length() >= 2) {
                heardAllWords.add(w);
                if (w.length() >= 3 && !COMMON_STOPWORDS.contains(w)) {
                    heardMeaningfulWords.add(w);
                    String tw = LyricsConsensusEngine.normalize(LyricsConsensusEngine.transliterateRuEn(w));
                    if (!tw.isEmpty() && !tw.equals(w)) {
                        heardTranslitWords.add(tw);
                    }
                }
            }
        }
        int alnumLen = normHeard.replace(" ", "").length();
        boolean confidentSpeech = confidence >= 0.78 && heardMeaningfulWords.size() >= 3 && alnumLen >= 13;

        Set<String> heardTrigrams = LyricsConsensusEngine.extractCharTrigrams(normHeard);
        HashSet<String> songMeaningfulTokens = new HashSet<String>();
        HashSet<String> songTranslitTokens = new HashSet<String>();
        StringBuilder allLyricsSample = new StringBuilder();
        double bestLineCharOverlap = 0.0;
        for (LyricLine line : lines) {
            if (line == null || line.text == null || line.text.isBlank()) continue;
            String normLine = LyricsConsensusEngine.normalize(line.text);
            if (normLine.isEmpty()) continue;
            allLyricsSample.append(line.text).append(" ");
            for (String lw : WHITESPACE_PATTERN.split(normLine)) {
                if (lw.length() >= 3 && !COMMON_STOPWORDS.contains(lw)) {
                    songMeaningfulTokens.add(lw);
                    String tlw = LyricsConsensusEngine.normalize(LyricsConsensusEngine.transliterateRuEn(lw));
                    if (!tlw.isEmpty() && !tlw.equals(lw)) {
                        songTranslitTokens.add(tlw);
                    }
                    String plw = LyricsConsensusEngine.toCanonicalLatinPhonetic(lw);
                    if (!plw.isEmpty() && !plw.equals(lw)) {
                        songTranslitTokens.add(plw);
                    }
                }
            }
            if (!heardTrigrams.isEmpty() && normLine.length() >= 6) {
                Set<String> lineTrigrams = LyricsConsensusEngine.extractCharTrigrams(normLine);
                if (!lineTrigrams.isEmpty()) {
                    int common = 0;
                    for (String tg : heardTrigrams) {
                        if (lineTrigrams.contains(tg)) {
                            common++;
                        }
                    }
                    int denom = Math.min(heardTrigrams.size(), lineTrigrams.size());
                    if (denom > 0) {
                        bestLineCharOverlap = Math.max(bestLineCharOverlap, (double)common / (double)denom);
                    }
                }
            }
        }

        int matchedWords = 0;
        int longWordMatches = 0;
        HashSet<String> counted = new HashSet<String>();
        for (String hw : heardMeaningfulWords) {
            String tw = LyricsConsensusEngine.normalize(LyricsConsensusEngine.transliterateRuEn(hw));
            String pw = LyricsConsensusEngine.toCanonicalLatinPhonetic(hw);
            boolean hit = songMeaningfulTokens.contains(hw) || songTranslitTokens.contains(hw)
                || (!tw.isEmpty() && (songMeaningfulTokens.contains(tw) || songTranslitTokens.contains(tw)))
                || (!pw.isEmpty() && (songMeaningfulTokens.contains(pw) || songTranslitTokens.contains(pw)));
            if (!hit) {
                for (String sw : songMeaningfulTokens) {
                    if (LyricsConsensusEngine.tokensFuzzyMatch(hw, sw)
                        || (!tw.isEmpty() && LyricsConsensusEngine.tokensFuzzyMatch(tw, sw))
                        || (!pw.isEmpty() && LyricsConsensusEngine.tokensFuzzyMatch(pw, sw))) {
                        hit = true;
                        break;
                    }
                }
            }
            if (!hit && !songTranslitTokens.isEmpty()) {
                for (String tsw : songTranslitTokens) {
                    if (LyricsConsensusEngine.tokensFuzzyMatch(hw, tsw)
                        || (!tw.isEmpty() && LyricsConsensusEngine.tokensFuzzyMatch(tw, tsw))
                        || (!pw.isEmpty() && LyricsConsensusEngine.tokensFuzzyMatch(pw, tsw))) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit && counted.add(hw)) {
                matchedWords++;
                if (hw.length() >= 5) {
                    longWordMatches++;
                }
            }
        }

        String allSampleStr = allLyricsSample.toString();
        int lyricsCyr = LyricsConsensusEngine.countCyrillic(allSampleStr);
        int lyricsLat = LyricsConsensusEngine.countLatin(allSampleStr);
        int heardCyr = LyricsConsensusEngine.countCyrillic(heardText);
        int heardLat = LyricsConsensusEngine.countLatin(heardText);
        boolean scriptContradiction = confidence >= 0.84 && heardMeaningfulWords.size() >= 3 && (
            (heardCyr >= 15 && lyricsCyr == 0 && lyricsLat >= 20) ||
            (heardLat >= 15 && lyricsLat <= 3 && lyricsCyr >= 25)
        );

        boolean matchedLyrics = matchedWords >= 2 || longWordMatches >= 1 || bestLineCharOverlap >= 0.40;
        boolean sparseCoincidentalHit = heardMeaningfulWords.size() >= 5 && matchedWords <= 1 && longWordMatches == 0 && bestLineCharOverlap < 0.24 && (double)matchedWords / (double)heardMeaningfulWords.size() <= 0.20;
        boolean strongMismatch = confidentSpeech && !matchedLyrics && (((matchedWords == 0 && bestLineCharOverlap < 0.22) || sparseCoincidentalHit) || scriptContradiction);
        String reason = scriptContradiction ? "script_contradiction" : (strongMismatch ? "no_word_overlap" : (matchedLyrics ? "matched" : "inconclusive"));
        return new TranscriptMatchAssessment(
            confidentSpeech,
            matchedLyrics,
            strongMismatch,
            heardMeaningfulWords.size(),
            matchedWords,
            bestLineCharOverlap,
            scriptContradiction,
            reason
        );
    }

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
        public String matchedTitle = "";
        public String matchedArtist = "";
        public double titleSim = -1.0;
        public double artistSim = -1.0;
        public boolean versionConflict = false;

        public Candidate(String string, List<LyricLine> list, String string2, boolean bl) {
            this(string, list, string2, bl, 0L, false, "", "");
        }

        public Candidate(String string, List<LyricLine> list, String string2, boolean bl, long sourceDurationMs, boolean hasEnhancedWords) {
            this(string, list, string2, bl, sourceDurationMs, hasEnhancedWords, "", "");
        }

        public Candidate(String string, List<LyricLine> list, String string2, boolean bl, long sourceDurationMs, boolean hasEnhancedWords, String matchedTitle, String matchedArtist) {
            this.provider = string != null ? string : "UNKNOWN";
            this.lines = list != null ? list : Collections.emptyList();
            this.rawText = string2 != null ? string2 : "";
            this.isSynced = bl;
            this.sourceDurationMs = Math.max(0L, sourceDurationMs);
            this.hasEnhancedWords = hasEnhancedWords;
            this.matchedTitle = matchedTitle != null ? matchedTitle.trim() : "";
            this.matchedArtist = matchedArtist != null ? matchedArtist.trim() : "";
            this.phrases = LyricsConsensusEngine.extractPhrases(this.lines, this.rawText);
        }
    }
}
