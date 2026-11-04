package com.example.yandexmusichud.lyrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_3532;

public class LyricLine {
    public final long startMs;
    public final long endMs;
    public final long vocalEndMs;
    public final String text;
    public final String rawText;
    public final List<Word> words;
    private final int[] charColors;
    private final String[] charAnims;
    private final String[] charFonts;
    private volatile List<SubLine> cachedSubLines = null;
    private volatile int cachedSubLinesMaxWidth = -1;
    private volatile String cachedSubLinesFont = null;

    public LyricLine(long startMs, long endMs, String text, List<Word> words) {
        this(startMs, endMs, endMs, text, words);
    }

    public LyricLine(long startMs, long endMs, long vocalEndMs, String text, List<Word> words) {
        this.startMs = startMs;
        this.endMs = Math.max(endMs, startMs + 60L);
        this.vocalEndMs = Math.max(vocalEndMs, startMs + 50L);
        String input = text != null ? text.trim().replace('\u2014', '-').replace('\u2013', '-') : "";
        this.rawText = input;

        StringBuilder sb = new StringBuilder();
        List<Integer> colors = new ArrayList<Integer>();
        List<String> anims = new ArrayList<String>();
        List<String> fonts = new ArrayList<String>();

        int currentColor = -1;
        String currentAnim = null;
        String currentFont = null;

        int len = input.length();
        int i = 0;
        while (i < len) {
            if (input.startsWith("<font:", i)) {
                int closeIdx = input.indexOf('>', i);
                if (closeIdx != -1) {
                    currentFont = input.substring(i + 6, closeIdx).trim().toLowerCase(Locale.ROOT);
                    i = closeIdx + 1;
                    continue;
                }
            } else if (input.startsWith("</font>", i)) {
                currentFont = null;
                i += 7;
                continue;
            } else if (input.startsWith("[font=", i)) {
                int closeIdx = input.indexOf(']', i);
                if (closeIdx != -1) {
                    currentFont = input.substring(i + 6, closeIdx).trim().toLowerCase(Locale.ROOT);
                    i = closeIdx + 1;
                    continue;
                }
            } else if (input.startsWith("[/font]", i)) {
                currentFont = null;
                i += 7;
                continue;
            } else if (input.startsWith("<anim:", i)) {
                int closeIdx = input.indexOf('>', i);
                if (closeIdx != -1) {
                    String tagContent = input.substring(i + 6, closeIdx).trim().toLowerCase(Locale.ROOT);
                    currentAnim = switch (tagContent) {
                        case "bounce" -> "KARAOKE_BOUNCE";
                        case "wave" -> "WAVE_FLOAT";
                        case "miside" -> "MISIDE_FALL";
                        case "pop" -> "ELASTIC_POP";
                        case "pulse" -> "PULSE_GLOW";
                        case "slide" -> "SLIDE_FADE";
                        case "classic" -> "CLASSIC";
                        default -> tagContent.toUpperCase(Locale.ROOT);
                    };
                    i = closeIdx + 1;
                    continue;
                }
            } else if (input.startsWith("</anim>", i)) {
                currentAnim = null;
                i += 7;
                continue;
            } else if (input.startsWith("<#", i) && i + 8 < len && input.charAt(i + 8) == '>') {
                String hex = input.substring(i + 2, i + 8);
                try {
                    currentColor = Integer.parseInt(hex, 16);
                    i += 9;
                    continue;
                } catch (Exception ignored) {}
            } else if (input.startsWith("</#>", i)) {
                currentColor = -1;
                i += 4;
                continue;
            } else if ((input.charAt(i) == '&' || input.charAt(i) == '\u00a7') && i + 7 < len && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                try {
                    currentColor = Integer.parseInt(hex, 16);
                    i += 8;
                    continue;
                } catch (Exception ignored) {}
            } else if ((input.charAt(i) == '&' || input.charAt(i) == '\u00a7') && i + 13 < len && Character.toLowerCase(input.charAt(i + 1)) == 'x') {
                StringBuilder hexSb = new StringBuilder();
                boolean valid = true;
                for (int h = 0; h < 6; h++) {
                    if ((input.charAt(i + 2 + h * 2) == '&' || input.charAt(i + 2 + h * 2) == '\u00a7') && "0123456789abcdefABCDEF".indexOf(input.charAt(i + 3 + h * 2)) != -1) {
                        hexSb.append(input.charAt(i + 3 + h * 2));
                    } else {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    try {
                        currentColor = Integer.parseInt(hexSb.toString(), 16);
                        i += 14;
                        continue;
                    } catch (Exception ignored) {}
                }
            } else if ((input.charAt(i) == '&' || input.charAt(i) == '\u00a7') && i + 1 < len) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                if ("0123456789abcdefkr".indexOf(code) != -1) {
                    currentColor = switch (code) {
                        case '0' -> 0x000000;
                        case '1' -> 0x0000AA;
                        case '2' -> 0x00AA00;
                        case '3' -> 0x00AAAA;
                        case '4' -> 0xAA0000;
                        case '5' -> 0xAA00AA;
                        case '6' -> 0xFFAA00;
                        case '7' -> 0xAAAAAA;
                        case '8' -> 0x555555;
                        case '9' -> 0x5555FF;
                        case 'a' -> 0x55FF55;
                        case 'b' -> 0x55FFFF;
                        case 'c' -> 0xFF5555;
                        case 'd' -> 0xFF55FF;
                        case 'e' -> 0xFFFF55;
                        case 'f' -> 0xFFFFFF;
                        default -> -1;
                    };
                    i += 2;
                    continue;
                }
            }
            char c = input.charAt(i);
            sb.append(c);
            colors.add(currentColor);
            anims.add(currentAnim);
            fonts.add(currentFont);
            i++;
        }

        this.text = sb.toString();
        this.charColors = new int[colors.size()];
        for (int k = 0; k < colors.size(); k++) {
            this.charColors[k] = colors.get(k);
        }
        this.charAnims = anims.toArray(new String[0]);
        this.charFonts = fonts.toArray(new String[0]);
        this.words = LyricLine.realignWordsToPlainText(words, this.text);
    }

    private static List<Word> realignWordsToPlainText(List<Word> rawWords, String plainText) {
        if (rawWords == null || rawWords.isEmpty() || plainText == null || plainText.isEmpty()) {
            return List.of();
        }
        ArrayList<Word> aligned = new ArrayList<Word>(rawWords.size());
        int cursor = 0;
        String lowerPlain = plainText.toLowerCase(Locale.ROOT);
        for (Word w : rawWords) {
            if (w == null) continue;
            String cleanToken = LyricLine.stripFormattingTags(w.text())
                .replace("\\n", " ")
                .replace(" // ", ", ")
                .replace(" / ", ", ")
                .replaceAll("\\s+", " ")
                .trim();
            if (cleanToken.isEmpty()) continue;
            int found = plainText.indexOf(cleanToken, cursor);
            if (found < 0) {
                found = lowerPlain.indexOf(cleanToken.toLowerCase(Locale.ROOT), cursor);
            }
            int cs;
            int ce;
            if (found >= 0) {
                cs = found;
                ce = Math.min(plainText.length(), found + cleanToken.length());
                cursor = ce;
            } else {
                cs = Math.min(plainText.length(), Math.max(0, w.charStart()));
                ce = Math.min(plainText.length(), Math.max(cs, w.charEnd()));
            }
            aligned.add(new Word(w.startMs(), w.endMs(), cleanToken, cs, ce));
        }
        return aligned;
    }

    public static String stripFormattingTags(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String input = text.replace('\u2014', '-').replace('\u2013', '-');
        StringBuilder sb = new StringBuilder();
        int len = input.length();
        int i = 0;
        while (i < len) {
            if (input.startsWith("<font:", i) || input.startsWith("<anim:", i)) {
                int closeIdx = input.indexOf('>', i);
                if (closeIdx != -1) {
                    i = closeIdx + 1;
                    continue;
                }
            } else if (input.startsWith("</font>", i) || input.startsWith("</anim>", i) || input.startsWith("[/font]", i)) {
                i += 7;
                continue;
            } else if (input.startsWith("[font=", i)) {
                int closeIdx = input.indexOf(']', i);
                if (closeIdx != -1) {
                    i = closeIdx + 1;
                    continue;
                }
            } else if (input.startsWith("<#", i) && i + 8 < len && input.charAt(i + 8) == '>') {
                String hex = input.substring(i + 2, i + 8);
                try {
                    Integer.parseInt(hex, 16);
                    i += 9;
                    continue;
                } catch (Exception ignored) {}
            } else if (input.startsWith("</#>", i)) {
                i += 4;
                continue;
            } else if ((input.charAt(i) == '&' || input.charAt(i) == '\u00a7') && i + 7 < len && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                try {
                    Integer.parseInt(hex, 16);
                    i += 8;
                    continue;
                } catch (Exception ignored) {}
            } else if ((input.charAt(i) == '&' || input.charAt(i) == '\u00a7') && i + 13 < len && Character.toLowerCase(input.charAt(i + 1)) == 'x') {
                boolean valid = true;
                for (int h = 0; h < 6; h++) {
                    if (!((input.charAt(i + 2 + h * 2) == '&' || input.charAt(i + 2 + h * 2) == '\u00a7') && "0123456789abcdefABCDEF".indexOf(input.charAt(i + 3 + h * 2)) != -1)) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    i += 14;
                    continue;
                }
            } else if ((input.charAt(i) == '&' || input.charAt(i) == '\u00a7') && i + 1 < len) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(code) != -1) {
                    i += 2;
                    continue;
                }
            }
            sb.append(input.charAt(i));
            i++;
        }
        return sb.toString();
    }

    public String getCharAnim(int charIndex) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length) {
            return this.charAnims[charIndex];
        }
        return null;
    }

    public String getCharFont(int charIndex) {
        if (this.charFonts != null && charIndex >= 0 && charIndex < this.charFonts.length) {
            return this.charFonts[charIndex];
        }
        return null;
    }

    public int getRawCharColor(int charIndex) {
        if (this.charColors != null && charIndex >= 0 && charIndex < this.charColors.length) {
            return this.charColors[charIndex];
        }
        return -1;
    }

    public String getLineFont() {
        if (this.charFonts != null) {
            for (String f : this.charFonts) {
                if (f != null && !f.isEmpty()) {
                    return f;
                }
            }
        }
        return null;
    }

    public net.minecraft.class_2561 toFormattedText(int defaultColor, String defaultFont) {
        if (this.text == null || this.text.isEmpty()) {
            return net.minecraft.class_2561.method_43473();
        }
        net.minecraft.class_5250 root = net.minecraft.class_2561.method_43473();
        int n = this.text.length();
        int segStart = 0;
        while (segStart < n) {
            int cColor = (this.charColors != null && segStart < this.charColors.length && this.charColors[segStart] != -1)
                ? this.charColors[segStart] : (defaultColor & 0xFFFFFF);
            String cFont = (this.charFonts != null && segStart < this.charFonts.length && this.charFonts[segStart] != null)
                ? this.charFonts[segStart] : defaultFont;

            int segEnd = segStart + 1;
            while (segEnd < n) {
                int nextCol = (this.charColors != null && segEnd < this.charColors.length && this.charColors[segEnd] != -1)
                    ? this.charColors[segEnd] : (defaultColor & 0xFFFFFF);
                String nextFnt = (this.charFonts != null && segEnd < this.charFonts.length && this.charFonts[segEnd] != null)
                    ? this.charFonts[segEnd] : defaultFont;
                if (nextCol != cColor || !java.util.Objects.equals(nextFnt, cFont)) {
                    break;
                }
                segEnd++;
            }

            String segStr = this.text.substring(segStart, segEnd);
            net.minecraft.class_5250 segComp = net.minecraft.class_2561.method_43470(segStr);
            net.minecraft.class_2583 style = net.minecraft.class_2583.field_24360.method_36139(cColor);
            if (cFont != null && !cFont.isEmpty() && !"default".equalsIgnoreCase(cFont) && !"minecraft:default".equalsIgnoreCase(cFont) && !"minecraft".equalsIgnoreCase(cFont) && !cFont.startsWith("sys:") && !cFont.startsWith("file:")) {
                try {
                    String cleanF = cFont.trim().toLowerCase(java.util.Locale.ROOT);
                    net.minecraft.class_2960 fontId = cleanF.contains(":") ? net.minecraft.class_2960.method_60654(cleanF) : net.minecraft.class_2960.method_60655("minecraft", cleanF);
                    style = style.method_27704(new net.minecraft.class_11719.class_11721(fontId));
                } catch (Exception ignored) {}
            }
            segComp.method_10862(style);
            root.method_10852(segComp);
            segStart = segEnd;
        }
        return root;
    }

    public LyricLine scaled(double factor) {
        long newStart = Math.max(0L, (long)((double)this.startMs * factor));
        long newEnd = Math.max(newStart + 60L, (long)((double)this.endMs * factor));
        long newVocalEnd = Math.max(newStart + 50L, (long)((double)this.vocalEndMs * factor));
        ArrayList<Word> newWords = null;
        if (!this.words.isEmpty()) {
            newWords = new ArrayList<Word>(this.words.size());
            for (Word w : this.words) {
                long ws = Math.max(0L, (long)((double)w.startMs() * factor));
                long we = Math.max(ws + 50L, (long)((double)w.endMs() * factor));
                newWords.add(new Word(ws, we, w.text(), w.charStart(), w.charEnd()));
            }
        }
        return new LyricLine(newStart, newEnd, newVocalEnd, this.rawText != null ? this.rawText : this.text, newWords);
    }

    public float getCharAlpha(int charIndex, long trackPosMs) {
        if (this.words.isEmpty()) {
            long totalDur = this.vocalEndMs - this.startMs;
            if (totalDur <= 0L) {
                return 1.0f;
            }
            int len = this.text.length();
            if (len <= 0) {
                return 1.0f;
            }
            long activeSingingDur = (long)((float)totalDur * 0.95f);
            long charStartMs = this.startMs + (long)((float)charIndex / (float)len * (float)activeSingingDur);
            long charFadeDur = Math.max(25L, Math.min(65L, activeSingingDur / (long)len + 20L));
            long charEndMs = charStartMs + charFadeDur;
            if (trackPosMs < charStartMs) {
                return 0.6f;
            }
            if (trackPosMs >= charEndMs) {
                return 1.0f;
            }
            float progress = (float)(trackPosMs - charStartMs) / (float)charFadeDur;
            return 0.6f + 0.4f * progress;
        }
        Word charWord = null;
        int offsetInWord = 0;
        for (Word w : this.words) {
            if (charIndex < w.charStart() || charIndex >= w.charEnd()) continue;
            charWord = w;
            offsetInWord = charIndex - w.charStart();
            break;
        }
        if (charWord == null) {
            return 1.0f;
        }
        long wordDur = Math.max(60L, charWord.endMs() - charWord.startMs());
        int wordLen = Math.max(1, charWord.text().length());
        long charStartMs = charWord.startMs() + (long)((float)offsetInWord / (float)wordLen * (float)wordDur);
        long charFadeDur = Math.max(25L, Math.min(70L, wordDur / (long)wordLen + 15L));
        long charEndMs = charStartMs + charFadeDur;
        if (trackPosMs < charStartMs) {
            return 0.6f;
        }
        if (trackPosMs >= charEndMs) {
            return 1.0f;
        }
        float progress = (float)(trackPosMs - charStartMs) / (float)charFadeDur;
        return 0.6f + 0.4f * progress;
    }

    public boolean isCharActive(int charIndex, long trackPosMs) {
        if (!this.words.isEmpty()) {
            for (Word w : this.words) {
                if (charIndex < w.charStart() || charIndex >= w.charEnd()) continue;
                return trackPosMs >= w.startMs() && trackPosMs <= w.endMs();
            }
            return false;
        }
        long totalDur = this.vocalEndMs - this.startMs;
        if (totalDur <= 0L || this.text.isEmpty()) {
            return false;
        }
        long activeSingingDur = (long)((float)totalDur * 0.95f);
        int len = this.text.length();
        long charStartMs = this.startMs + (long)((float)charIndex / (float)len * (float)activeSingingDur);
        long charEndMs = charStartMs + Math.max(35L, Math.min(85L, activeSingingDur / (long)len + 25L));
        return trackPosMs >= charStartMs && trackPosMs <= charEndMs;
    }

    public float getCharActiveProgress(int charIndex, long trackPosMs) {
        if (!this.words.isEmpty()) {
            for (Word w : this.words) {
                if (charIndex < w.charStart() || charIndex >= w.charEnd()) continue;
                long dur = w.endMs() - w.startMs();
                if (dur <= 0L) {
                    return 1.0f;
                }
                if (trackPosMs < w.startMs()) {
                    return -1.0f;
                }
                if (trackPosMs > w.endMs()) {
                    return 2.0f;
                }
                long activeBounceDur = Math.min(dur, 160L);
                long elapsed = trackPosMs - w.startMs();
                if (elapsed <= activeBounceDur) {
                    return (float)elapsed / (float)activeBounceDur;
                }
                return 1.0f;
            }
            return -1.0f;
        }
        long totalDur = this.vocalEndMs - this.startMs;
        if (totalDur <= 0L || this.text.isEmpty()) {
            return -1.0f;
        }
        long activeSingingDur = (long)((float)totalDur * 0.95f);
        int len = this.text.length();
        long charStartMs = this.startMs + (long)((float)charIndex / (float)len * (float)activeSingingDur);
        long charDur = Math.max(25L, Math.min(65L, activeSingingDur / (long)len + 15L));
        if (trackPosMs < charStartMs) {
            return -1.0f;
        }
        if (trackPosMs > charStartMs + charDur) {
            return 2.0f;
        }
        return (float)(trackPosMs - charStartMs) / (float)charDur;
    }

    public boolean isCharPronounced(int charIndex, long trackPosMs) {
        if (trackPosMs >= this.endMs) {
            return true;
        }
        if (this.getTimeSinceCharPassed(charIndex, trackPosMs) >= 0L) {
            return true;
        }
        if (!this.words.isEmpty()) {
            for (Word w : this.words) {
                if (charIndex >= w.charStart() && charIndex < w.charEnd()) {
                    int offsetInWord = charIndex - w.charStart();
                    int wordLen = Math.max(1, w.text().length());
                    long wordDur = Math.max(30L, w.endMs() - w.startMs());
                    long charStartMs = w.startMs() + (long)((float)offsetInWord / (float)wordLen * (float)wordDur);
                    return trackPosMs >= charStartMs;
                }
            }
            long prevEnd = this.startMs;
            for (Word w : this.words) {
                if (w.charEnd() <= charIndex && w.endMs() > prevEnd) {
                    prevEnd = w.endMs();
                }
            }
            return trackPosMs >= prevEnd;
        }
        long totalDur = this.vocalEndMs - this.startMs;
        if (totalDur <= 0L || this.text.isEmpty()) {
            return trackPosMs >= this.startMs;
        }
        long activeSingingDur = (long)((float)totalDur * 0.95f);
        int len = this.text.length();
        long charStartMs = this.startMs + (long)((float)charIndex / (float)len * (float)activeSingingDur);
        return trackPosMs >= charStartMs;
    }

    public float getCharAnimationOffsetY(int charIndex, long trackPosMs, long systemTimeMs, String animMode) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null) {
            animMode = "KARAOKE_BOUNCE";
        }
        switch (animMode.toUpperCase()) {
            case "KARAOKE_BOUNCE": {
                float p = this.getCharActiveProgress(charIndex, trackPosMs);
                if (p >= 0.0f && p <= 1.0f) {
                    return -4.5f * (float)Math.sin((double)p * Math.PI);
                }
                return 0.0f;
            }
            case "WAVE_FLOAT": {
                return (float)Math.sin((double)systemTimeMs / 220.0 + (double)charIndex * 0.42) * 3.2f;
            }
            case "ELASTIC_POP": {
                long age = Math.max(0L, trackPosMs - this.startMs);
                long charDelay = (long)charIndex * 6L;
                long popDur = Math.min(130L, Math.max(65L, (this.vocalEndMs - this.startMs) / Math.max(1, this.text.length()) * 2));
                if (age >= charDelay && age < charDelay + popDur) {
                    float t = (float)(age - charDelay) / (float)popDur;
                    return -6.0f * (float)(Math.sin((double)t * Math.PI * 2.2) * Math.exp((double)(-t) * 3.2));
                }
                return 0.0f;
            }
            case "PULSE_GLOW": {
                float p = this.getCharActiveProgress(charIndex, trackPosMs);
                if (p >= 0.0f && p <= 1.0f) {
                    return -2.2f * (float)Math.sin((double)p * Math.PI);
                }
                return 0.0f;
            }
            case "SLIDE_FADE": {
                long age = Math.max(0L, trackPosMs - this.startMs);
                long slideDur = Math.min(110L, Math.max(55L, this.vocalEndMs - this.startMs));
                if (age < slideDur) {
                    float t = (float)age / (float)slideDur;
                    return 4.0f * (1.0f - t * t);
                }
                return 0.0f;
            }
            case "MISIDE_FALL": {
                return this.getCharAnimationOffsetY(charIndex, trackPosMs, systemTimeMs, animMode, 28.0f);
            }
        }
        return 0.0f;
    }

    public float getCharAnimationOffsetY(int charIndex, long trackPosMs, long systemTimeMs, String animMode, float maxFallDist) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return this.getCharAnimationOffsetY(charIndex, trackPosMs, systemTimeMs, animMode);
        }
        long timePassed = this.getTimeSinceCharPassed(charIndex, trackPosMs);
        if (timePassed <= 0L) {
            return 0.0f;
        }
        long fallTime = timePassed;
        int hash = ((charIndex * 197 + 53) ^ (this.text.length() * 37)) & 0x7FFFFFFF;
        float dt = (float)fallTime / 1000.0f;
        float letterGravity = 210.0f + (float)((hash % 9) - 4) * 8.0f;
        float velocity = letterGravity * dt * (1.0f - dt * 0.12f);
        float fallDist = 0.5f * velocity * dt;
        if (fallDist >= maxFallDist) {
            fallDist = maxFallDist;
            float impactTime = (float)Math.sqrt((2.0f * maxFallDist) / letterGravity);
            float bounceTime = dt - impactTime;
            float bounceDur = 0.16f + (float)(hash % 5) * 0.02f;
            if (bounceTime > 0.0f && bounceTime < bounceDur) {
                float bounceH = Math.min(2.5f, maxFallDist * 0.06f);
                fallDist -= bounceH * (float)Math.sin(bounceTime / bounceDur * Math.PI) * (1.0f - bounceTime / bounceDur);
            }
        }
        return fallDist;
    }

    public float getLineAnimationOffsetY(long trackPosMs, String animMode) {
        return this.getLineAnimationOffsetY(trackPosMs, animMode, 28.0f);
    }

    public float getLineAnimationOffsetY(long trackPosMs, String animMode, float maxFallDist) {
        if (this.rawText != null && this.rawText.contains("<anim:miside>")) {
            animMode = "MISIDE_FALL";
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return 0.0f;
        }
        long passed = trackPosMs - (this.vocalEndMs - 80L);
        if (passed <= 0L) {
            return 0.0f;
        }
        float dt = (float)passed / 1000.0f;
        float gravity = 210.0f;
        float velocity = gravity * dt * (1.0f - dt * 0.12f);
        float y = 0.5f * velocity * dt;
        return Math.min(y, maxFallDist);
    }

    public float getLineAnimationTiltX(long trackPosMs, String animMode) {
        if (this.rawText != null && this.rawText.contains("<anim:miside>")) {
            animMode = "MISIDE_FALL";
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return 0.0f;
        }
        long passed = trackPosMs - (this.vocalEndMs - 80L);
        if (passed <= 0L) {
            return 0.0f;
        }
        float progressToGround = Math.min(1.0f, (float)passed / 320.0f);
        return progressToGround * 88.0f;
    }

    public float getCharAnimationTiltX(int charIndex, long trackPosMs, long systemTimeMs, String animMode) {
        return this.getCharAnimationTiltX(charIndex, trackPosMs, systemTimeMs, animMode, 28.0f);
    }

    public float getCharAnimationTiltX(int charIndex, long trackPosMs, long systemTimeMs, String animMode, float maxFallDist) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return 0.0f;
        }
        long timePassed = this.getTimeSinceCharPassed(charIndex, trackPosMs);
        if (timePassed <= 0L) {
            return 0.0f;
        }
        int hash = ((charIndex * 197 + 53) ^ (this.text.length() * 37)) & 0x7FFFFFFF;
        long fallTime = timePassed;
        float dt = (float)fallTime / 1000.0f;
        float letterGravity = 210.0f + (float)((hash % 9) - 4) * 8.0f;
        float impactTime = Math.max(0.18f, (float)Math.sqrt((2.0f * maxFallDist) / letterGravity));
        float progressToGround = Math.min(1.0f, dt / impactTime);
        return progressToGround * 88.0f;
    }

    public float getCharScatterX(int charIndex, long trackPosMs, String animMode) {
        return this.getCharScatterX(charIndex, trackPosMs, animMode, 0, 1, charIndex, Math.max(1, this.text.length()), 28.0f);
    }

    public float getCharScatterX(int charIndex, long trackPosMs, String animMode, int lineIdx, int lineCount, int charPosInLine, int lineLength) {
        return this.getCharScatterX(charIndex, trackPosMs, animMode, lineIdx, lineCount, charPosInLine, lineLength, 28.0f);
    }

    public float getCharScatterX(int charIndex, long trackPosMs, String animMode, int lineIdx, int lineCount, int charPosInLine, int lineLength, float maxFallDist) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return 0.0f;
        }
        long timePassed = this.getTimeSinceCharPassed(charIndex, trackPosMs);
        if (timePassed <= 0L) {
            return 0.0f;
        }
        int hash = ((charIndex * 197 + lineIdx * 389 + 53) ^ (this.text.length() * 37)) & 0x7FFFFFFF;
        float dt = (float)timePassed / 1000.0f;
        float letterGravity = 210.0f + (float)((hash % 9) - 4) * 8.0f;
        float impactTime = Math.max(0.18f, (float)Math.sqrt((2.0f * maxFallDist) / letterGravity));
        float progressToGround = Math.min(1.0f, dt / impactTime);
        float scatterProgress = (float)Math.pow(progressToGround, 2.5);

        float heightFactor = Math.max(0.35f, Math.min(2.5f, (float)Math.sqrt(Math.max(4.0f, maxFallDist) / 28.0f)));
        float multiLineBoost = 1.0f + (float)(lineCount - 1) * 0.35f;

        float baseScatterX = (float)((hash % 11) - 5) * 0.55f * multiLineBoost;
        float normPos = lineLength > 1 ? ((float)charPosInLine / (float)(lineLength - 1) - 0.5f) * 2.0f : 0.0f;
        float lateralFan = lineCount >= 2 ? normPos * 4.0f * (float)(lineCount - 1) : 0.0f;
        float lineXOffset = lineCount >= 2 ? ((float)lineIdx - (float)(lineCount - 1) * 0.5f) * 3.5f * ((charIndex & 1) == 0 ? 1.0f : -1.0f) : 0.0f;

        float totalScatterX = (baseScatterX * heightFactor + lineXOffset + lateralFan);
        return totalScatterX * scatterProgress;
    }

    public float getCharScatterZ(int charIndex, long trackPosMs, String animMode) {
        return this.getCharScatterZ(charIndex, trackPosMs, animMode, 0, 1, charIndex, Math.max(1, this.text.length()), 28.0f);
    }

    public float getCharScatterZ(int charIndex, long trackPosMs, String animMode, int lineIdx, int lineCount, int charPosInLine, int lineLength) {
        return this.getCharScatterZ(charIndex, trackPosMs, animMode, lineIdx, lineCount, charPosInLine, lineLength, 28.0f);
    }

    public float getCharScatterZ(int charIndex, long trackPosMs, String animMode, int lineIdx, int lineCount, int charPosInLine, int lineLength, float maxFallDist) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return 0.0f;
        }
        long timePassed = this.getTimeSinceCharPassed(charIndex, trackPosMs);
        if (timePassed <= 0L) {
            return 0.0f;
        }
        int hash = ((charIndex * 197 + lineIdx * 389 + 53) ^ (this.text.length() * 37)) & 0x7FFFFFFF;
        float dt = (float)timePassed / 1000.0f;
        float letterGravity = 210.0f + (float)((hash % 9) - 4) * 8.0f;
        float impactTime = Math.max(0.18f, (float)Math.sqrt((2.0f * maxFallDist) / letterGravity));
        float progressToGround = Math.min(1.0f, dt / impactTime);
        float scatterProgress = (float)Math.pow(progressToGround, 2.5);

        float heightFactor = Math.max(0.35f, Math.min(2.5f, (float)Math.sqrt(Math.max(4.0f, maxFallDist) / 28.0f)));
        float multiLineBoost = 1.0f + (float)(lineCount - 1) * 0.35f;

        float baseScatterZ = (float)(((hash / 11) % 11) - 5) * 0.45f * multiLineBoost;
        float lineDepthBase = lineCount >= 2 ? ((float)lineIdx - (float)(lineCount - 1) * 0.5f) * 16.0f : 0.0f;

        float totalScatterZ = (baseScatterZ * heightFactor + lineDepthBase * heightFactor);
        return totalScatterZ * scatterProgress;
    }

    public float getCharRandomYaw(int charIndex, long trackPosMs, String animMode) {
        return this.getCharRandomYaw(charIndex, trackPosMs, animMode, 0, 1, charIndex, Math.max(1, this.text.length()), 28.0f);
    }

    public float getCharRandomYaw(int charIndex, long trackPosMs, String animMode, int lineIdx, int lineCount, int charPosInLine, int lineLength) {
        return this.getCharRandomYaw(charIndex, trackPosMs, animMode, lineIdx, lineCount, charPosInLine, lineLength, 28.0f);
    }

    public float getCharRandomYaw(int charIndex, long trackPosMs, String animMode, int lineIdx, int lineCount, int charPosInLine, int lineLength, float maxFallDist) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null || !"MISIDE_FALL".equalsIgnoreCase(animMode)) {
            return 0.0f;
        }
        long timePassed = this.getTimeSinceCharPassed(charIndex, trackPosMs);
        if (timePassed <= 0L) {
            return 0.0f;
        }
        int hash = ((charIndex * 197 + lineIdx * 389 + 53) ^ (this.text.length() * 37)) & 0x7FFFFFFF;
        float dt = (float)timePassed / 1000.0f;
        float letterGravity = 210.0f + (float)((hash % 9) - 4) * 8.0f;
        float impactTime = Math.max(0.18f, (float)Math.sqrt((2.0f * maxFallDist) / letterGravity));
        float progressToGround = Math.min(1.0f, dt / impactTime);
        float scatterProgress = (float)Math.pow(progressToGround, 2.5);

        float heightFactor = Math.max(0.35f, Math.min(2.5f, (float)Math.sqrt(Math.max(4.0f, maxFallDist) / 28.0f)));
        int yawHash = ((charIndex * 151 + lineIdx * 311 + 71) ^ (this.text.length() * 23)) & 0x7FFFFFFF;
        float maxAngle = (float)(((yawHash / 121) % 41) - 20) * 1.5f;
        return maxAngle * heightFactor * scatterProgress;
    }

    public long getTimeSinceCharPassed(int charIndex, long trackPosMs) {
        long pauseAfterWord = 120L;
        if (!this.words.isEmpty()) {
            for (Word w : this.words) {
                if (charIndex < w.charStart() || charIndex >= w.charEnd()) continue;
                int charInWord = charIndex - w.charStart();
                long charStep = (long)charInWord * 35L;
                long triggerTime = w.endMs() + pauseAfterWord + charStep;
                if (trackPosMs >= triggerTime) {
                    return trackPosMs - triggerTime;
                }
                return -1L;
            }
            long prevEnd = this.startMs;
            for (Word w : this.words) {
                if (w.charEnd() <= charIndex && w.endMs() > prevEnd) {
                    prevEnd = w.endMs();
                }
            }
            long charStep = (long)charIndex * 25L;
            if (trackPosMs >= prevEnd + pauseAfterWord + charStep) {
                return trackPosMs - (prevEnd + pauseAfterWord + charStep);
            }
            return -1L;
        }
        long totalDur = this.vocalEndMs - this.startMs;
        if (totalDur <= 0L || this.text.isEmpty()) {
            long charStep = (long)charIndex * 35L;
            return trackPosMs >= this.endMs + pauseAfterWord + charStep ? trackPosMs - (this.endMs + pauseAfterWord + charStep) : -1L;
        }
        long activeSingingDur = (long)((float)totalDur * 0.90f);
        int len = Math.max(1, this.text.length());
        long wordEndMs = this.startMs + (long)((float)charIndex / (float)len * (float)activeSingingDur);
        if (trackPosMs >= wordEndMs + pauseAfterWord) {
            return trackPosMs - (wordEndMs + pauseAfterWord);
        }
        return -1L;
    }

    public int getCharColor(int charIndex, long trackPosMs, long systemTimeMs, String animMode, int baseRgb, float finalAlpha, int activeColorRgb) {
        if (this.charAnims != null && charIndex >= 0 && charIndex < this.charAnims.length && this.charAnims[charIndex] != null) {
            animMode = this.charAnims[charIndex];
        }
        if (animMode == null) {
            animMode = "KARAOKE_BOUNCE";
        }
        if (this.charColors != null && charIndex >= 0 && charIndex < this.charColors.length && this.charColors[charIndex] != -1) {
            baseRgb = this.charColors[charIndex];
        }
        int alphaInt = Math.max(0, Math.min(255, (int)(finalAlpha * 255.0f)));
        switch (animMode.toUpperCase()) {
            case "KARAOKE_BOUNCE": {
                float p = this.getCharActiveProgress(charIndex, trackPosMs);
                if (p >= 0.0f && p <= 1.0f) {
                    return alphaInt << 24 | activeColorRgb & 0xFFFFFF;
                }
                if (p > 1.0f) {
                    return alphaInt << 24 | baseRgb & 0xFFFFFF;
                }
                int dimAlpha = (int)((float)alphaInt * 0.75f);
                return dimAlpha << 24 | baseRgb & 0xFFFFFF;
            }
            case "WAVE_FLOAT": {
                float hue = ((float)systemTimeMs / 2800.0f + (float)charIndex * 0.045f) % 1.0f;
                int rgb = class_3532.method_15369((float)hue, (float)0.62f, (float)1.0f);
                return alphaInt << 24 | rgb & 0xFFFFFF;
            }
            case "PULSE_GLOW": {
                float p = this.getCharActiveProgress(charIndex, trackPosMs);
                if (p >= 0.0f && p <= 1.0f) {
                    float pulse = (float)Math.sin((double)systemTimeMs / 130.0) * 0.5f + 0.5f;
                    int r = (int)(0.0f + pulse * 255.0f);
                    int g = (int)(229.0f + pulse * 26.0f);
                    int b = (int)(255.0f - pulse * 187.0f);
                    return alphaInt << 24 | r << 16 | g << 8 | b;
                }
                return alphaInt << 24 | baseRgb & 0xFFFFFF;
            }
            case "ELASTIC_POP": {
                if (this.isCharActive(charIndex, trackPosMs)) {
                    return alphaInt << 24 | 0xFF4081;
                }
                return alphaInt << 24 | baseRgb & 0xFFFFFF;
            }
            case "MISIDE_FALL": {
                long timePassed = this.getTimeSinceCharPassed(charIndex, trackPosMs);
                if (timePassed > 0L) {
                    return alphaInt << 24 | baseRgb & 0xFFFFFF;
                }
                float p = this.getCharActiveProgress(charIndex, trackPosMs);
                if (p >= 0.0f && p <= 1.0f) {
                    return alphaInt << 24 | activeColorRgb & 0xFFFFFF;
                }
                return alphaInt << 24 | baseRgb & 0xFFFFFF;
            }
        }
        return alphaInt << 24 | baseRgb & 0xFFFFFF;
    }

    public boolean hasCustomFonts() {
        if (this.charFonts != null) {
            for (String f : this.charFonts) {
                if (CustomFontManager.isCustomFont(f)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float measureCharWidth(class_327 textRenderer, char c, String font) {
        if (font != null && CustomFontManager.isCustomFont(font)) {
            return CustomFontManager.getCharWidth(font, c);
        }
        boolean isRuneFont = font != null && (font.contains("alt") || font.contains("illager"));
        if (isRuneFont) {
            String renderStr = LyricsRenderer.transliterateForRune(c);
            return (float)textRenderer.method_1727(renderStr);
        }
        boolean isDefaultFont = font == null || font.isEmpty() || "default".equalsIgnoreCase(font) || "minecraft:default".equalsIgnoreCase(font) || "minecraft".equalsIgnoreCase(font);
        if (isDefaultFont) {
            return (float)textRenderer.method_1727(String.valueOf(c));
        }
        String fontName = font.trim();
        class_2960 fontId = fontName.contains(":") ? class_2960.method_60654(fontName) : class_2960.method_60655("minecraft", fontName);
        net.minecraft.class_5250 comp = net.minecraft.class_2561.method_43470(String.valueOf(c));
        comp.method_10862(net.minecraft.class_2583.field_24360.method_27704(new net.minecraft.class_11719.class_11721(fontId)));
        return (float)textRenderer.method_27525(comp);
    }

    public int measureSubstringWidth(class_327 textRenderer, int start, int end) {
        return this.measureSubstringWidth(textRenderer, start, end, null);
    }

    public int measureSubstringWidth(class_327 textRenderer, int start, int end, String defaultFont) {
        if (start >= end || this.text == null || this.text.isEmpty()) {
            return 0;
        }
        start = Math.max(0, start);
        end = Math.min(this.text.length(), end);
        float total = 0.0f;
        for (int i = start; i < end; i++) {
            char c = this.text.charAt(i);
            String f = getCharFont(i);
            if (f == null || f.isEmpty()) {
                f = defaultFont;
            }
            total += measureCharWidth(textRenderer, c, f);
        }
        return Math.round(total);
    }

    public static int getMaxAllowedWidth2D(int screenWidth) {
        return Math.min(220, Math.max(140, (int)((float)screenWidth * 0.50f)));
    }

    public static int getMaxAllowedWidth3D() {
        return 190;
    }

    public List<SubLine> getSubLines(class_327 textRenderer, int maxAllowedWidth) {
        return this.getSubLines(textRenderer, maxAllowedWidth, null);
    }

    public List<SubLine> getSubLines(class_327 textRenderer, int maxAllowedWidth, String defaultFont) {
        if (this.cachedSubLines != null && this.cachedSubLinesMaxWidth == maxAllowedWidth && Objects.equals(this.cachedSubLinesFont, defaultFont)) {
            return this.cachedSubLines;
        }
        ArrayList<SubLine> result = new ArrayList<SubLine>();
        if (this.text == null || this.text.isEmpty()) {
            return result;
        }
        int totalWidth = this.measureSubstringWidth(textRenderer, 0, this.text.length(), defaultFont);
        if (totalWidth <= maxAllowedWidth) {
            result.add(new SubLine(0, this.text.length(), totalWidth));
            this.cachedSubLines = result;
            this.cachedSubLinesMaxWidth = maxAllowedWidth;
            this.cachedSubLinesFont = defaultFont;
            return result;
        }

        List<int[]> words = new ArrayList<int[]>();
        int len = this.text.length();
        int wStart = -1;
        for (int i = 0; i < len; i++) {
            char c = this.text.charAt(i);
            if (!Character.isWhitespace(c)) {
                if (wStart == -1) {
                    wStart = i;
                }
            } else {
                if (wStart != -1) {
                    words.add(new int[]{wStart, i});
                    wStart = -1;
                }
            }
        }
        if (wStart != -1) {
            words.add(new int[]{wStart, len});
        }

        if (words.isEmpty()) {
            this.cachedSubLines = result;
            this.cachedSubLinesMaxWidth = maxAllowedWidth;
            this.cachedSubLinesFont = defaultFont;
            return result;
        }

        int targetLines = Math.max(2, (int)Math.ceil((double)totalWidth / (double)maxAllowedWidth));
        int targetWidth = Math.max(50, totalWidth / targetLines);

        int lineWordStart = 0;
        while (lineWordStart < words.size()) {
            int charStart = words.get(lineWordStart)[0];
            int remainingWords = words.size() - lineWordStart;
            if (remainingWords == 1) {
                int charEnd = words.get(lineWordStart)[1];
                int w = this.measureSubstringWidth(textRenderer, charStart, charEnd, defaultFont);
                if (w > maxAllowedWidth) {
                    int curStart = charStart;
                    while (curStart < charEnd) {
                        int curEnd = curStart + 1;
                        while (curEnd <= charEnd && this.measureSubstringWidth(textRenderer, curStart, curEnd, defaultFont) <= maxAllowedWidth) {
                            curEnd++;
                        }
                        if (curEnd > charEnd) {
                            curEnd = charEnd;
                        } else if (curEnd > curStart + 1) {
                            curEnd--;
                        }
                        curEnd = Math.min(charEnd, Math.max(curStart + 1, curEnd));
                        int partW = this.measureSubstringWidth(textRenderer, curStart, curEnd, defaultFont);
                        result.add(new SubLine(curStart, curEnd, partW));
                        curStart = curEnd;
                    }
                } else {
                    result.add(new SubLine(charStart, charEnd, w));
                }
                break;
            }

            int bestWordIdx = lineWordStart;
            double bestCost = Double.MAX_VALUE;

            for (int k = lineWordStart; k < words.size(); k++) {
                int candEnd = words.get(k)[1];
                int candWidth = this.measureSubstringWidth(textRenderer, charStart, candEnd, defaultFont);

                if (candWidth > maxAllowedWidth) {
                    if (k == lineWordStart) {
                        bestWordIdx = lineWordStart;
                    }
                    break;
                }

                int remWidth = this.measureSubstringWidth(textRenderer, candEnd, this.text.length(), defaultFont);
                int remLines = targetLines - result.size() - 1;
                if (remLines > 0 && remWidth > maxAllowedWidth * remLines + 35) {
                    continue;
                }

                double cost = Math.abs((double)(candWidth - targetWidth));

                char lastChar = (candEnd > 0 && candEnd <= this.text.length()) ? this.text.charAt(candEnd - 1) : ' ';
                if (lastChar == ',' || lastChar == ';' || lastChar == ':' || lastChar == '!' || lastChar == '?' || lastChar == '\u2014' || lastChar == '-' || lastChar == ')' || lastChar == '\u00bb' || lastChar == '"') {
                    cost -= 40.0;
                }

                if (k + 1 < words.size()) {
                    int nwStart = words.get(k + 1)[0];
                    int nwEnd = words.get(k + 1)[1];
                    if (nwStart >= 0 && nwEnd <= this.text.length() && nwStart < nwEnd) {
                        String nextW = this.text.substring(nwStart, nwEnd).toLowerCase(Locale.ROOT);
                        if (nextW.equals("\u0438") || nextW.equals("\u0430") || nextW.equals("\u043d\u043e") || nextW.equals("\u0447\u0442\u043e") || nextW.equals("\u043a\u0430\u043a") || nextW.equals("\u0433\u0434\u0435") || nextW.equals("\u0434\u0430") || nextW.equals("\u0438\u043b\u0438") || nextW.equals("and") || nextW.equals("or") || nextW.equals("but") || nextW.equals("that")) {
                            cost -= 22.0;
                        }
                    }
                }

                if (cost < bestCost) {
                    bestCost = cost;
                    bestWordIdx = k;
                }
            }

            int lineEnd = words.get(bestWordIdx)[1];
            int finalWidth = this.measureSubstringWidth(textRenderer, charStart, lineEnd, defaultFont);
            if (finalWidth > maxAllowedWidth && lineWordStart == bestWordIdx) {
                int curStart = charStart;
                int curEnd = curStart + 1;
                while (curEnd <= lineEnd && this.measureSubstringWidth(textRenderer, curStart, curEnd, defaultFont) <= maxAllowedWidth) {
                    curEnd++;
                }
                if (curEnd > lineEnd) {
                    curEnd = lineEnd;
                } else if (curEnd > curStart + 1) {
                    curEnd--;
                }
                curEnd = Math.min(lineEnd, Math.max(curStart + 1, curEnd));
                int partW = this.measureSubstringWidth(textRenderer, curStart, curEnd, defaultFont);
                result.add(new SubLine(curStart, curEnd, partW));
                if (curEnd >= lineEnd) {
                    lineWordStart = bestWordIdx + 1;
                } else {
                    words.get(lineWordStart)[0] = curEnd;
                }
                continue;
            }

            result.add(new SubLine(charStart, lineEnd, finalWidth));
            lineWordStart = bestWordIdx + 1;
        }

        this.cachedSubLines = result;
        this.cachedSubLinesMaxWidth = maxAllowedWidth;
        this.cachedSubLinesFont = defaultFont;
        return result;
    }

    public static String toMinecraftFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = text.replaceAll("<font:[^>]+>", "").replaceAll("</font>", "");
        text = text.replaceAll("\\[font=[^\\]]+\\]", "").replaceAll("\\[/font\\]", "");
        StringBuilder sb = new StringBuilder();
        int len = text.length();
        int i = 0;
        while (i < len) {
            if ((text.charAt(i) == '&' || text.charAt(i) == '\u00a7') && i + 7 < len && text.charAt(i + 1) == '#') {
                String hex = text.substring(i + 2, i + 8);
                boolean isHex = true;
                for (int c = 0; c < 6; c++) {
                    if ("0123456789abcdefABCDEF".indexOf(hex.charAt(c)) == -1) {
                        isHex = false;
                        break;
                    }
                }
                if (isHex) {
                    sb.append("\u00a7x");
                    for (int c = 0; c < 6; c++) {
                        sb.append("\u00a7").append(hex.charAt(c));
                    }
                    i += 8;
                    continue;
                }
            } else if (text.startsWith("<#", i) && i + 8 < len && text.charAt(i + 8) == '>') {
                String hex = text.substring(i + 2, i + 8);
                boolean isHex = true;
                for (int c = 0; c < 6; c++) {
                    if ("0123456789abcdefABCDEF".indexOf(hex.charAt(c)) == -1) {
                        isHex = false;
                        break;
                    }
                }
                if (isHex) {
                    sb.append("\u00a7x");
                    for (int c = 0; c < 6; c++) {
                        sb.append("\u00a7").append(hex.charAt(c));
                    }
                    i += 9;
                    continue;
                }
            } else if (text.startsWith("</#>", i)) {
                sb.append("\u00a7r");
                i += 4;
                continue;
            } else if (text.charAt(i) == '&' && i + 1 < len) {
                char code = text.charAt(i + 1);
                if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(code) != -1) {
                    sb.append("\u00a7").append(code);
                    i += 2;
                    continue;
                }
            }
            sb.append(text.charAt(i));
            i++;
        }
        return sb.toString();
    }

    public record Word(long startMs, long endMs, String text, int charStart, int charEnd) {
        public Word {
            text = text != null ? text.replace('\u2014', '-').replace('\u2013', '-') : "";
        }
    }

    public record SubLine(int charStart, int charEnd, int width) {
        public SubLine {
            charStart = Math.max(0, charStart);
            charEnd = Math.max(charStart, charEnd);
            width = Math.max(0, width);
        }
    }
}

