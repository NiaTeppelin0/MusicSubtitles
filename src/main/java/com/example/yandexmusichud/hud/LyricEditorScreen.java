package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.YandexMusicHudMod;
import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.ModernButtonWidget;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.lyrics.CustomFontManager;
import com.example.yandexmusichud.lyrics.LyricLine;
import com.example.yandexmusichud.lyrics.LyricsManager;
import com.example.yandexmusichud.lyrics.StudioCascadeAligner;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_342;
import net.minecraft.class_437;
import net.minecraft.class_6880;

public class LyricEditorScreen extends class_437 {
    private static final class_2960 TEX_GLOW = class_2960.method_60655("yandexmusichud", "textures/gui/radial/wheel_ambient_glow.png");
    private static final class_2960 TEX_VINYL = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_record.png");
    private static final class_2960 ICON_PLAY = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_play.png");
    private static final class_2960 ICON_PAUSE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_pause.png");


    private static final class_2960 TEX_DRUM_DEPTH = class_2960.method_60655("yandexmusichud", "textures/gui/editor/drum_depth.png");
    private static final class_2960 TEX_DRUM_DEPTH_BOTTOM = class_2960.method_60655("yandexmusichud", "textures/gui/editor/drum_depth_bottom.png");
    private static final class_2960 TEX_SOUNDWAVE = class_2960.method_60655("yandexmusichud", "textures/gui/editor/soundwave.png");

    private static final String[][] FONT_PRESETS = {
        {"minecraft:default", "По умолч."},
        {"minecraft:alt", "Руны (SGA)"},
        {"minecraft:uniform", "Юникод"},
        {"minecraft:illageralt", "Илладжер"}
    };

    private final class_437 parent;
    private final List<EditableLine> lines = new ArrayList<EditableLine>();
    private String trackArtist = "";
    private String trackTitle = "";
    private float currentScroll = 0.0f;
    private float targetScroll = 0.0f;
    private int selectedIndex = 0;
    private int selectedWordIndex = -1;
    private class_342 editField;
    private class_342 hexField;
    private class_342 fontField;
    private int currentHexColor = 0x00E5FF;
    private String customFont = "minecraft:default";
    private int fontPresetIdx = 0;
    private boolean isDraggingReel = false;
    private double lastDragMouseY = 0.0;
    private String feedbackToast = null;
    private long feedbackToastTime = 0L;

    private int customColor = -1;
    private int customActiveColor = 0xFFA726;
    private String customMode = "DEFAULT";
    private float customScale = 1.0f;

    private final List<WordChip> currentWordChips = new ArrayList<WordChip>();
    private final List<LyricLine> initialSnapshotLines = new ArrayList<LyricLine>();
    private String initialStyleRaw = null;
    private boolean savedChanges = false;
    private boolean previewDirty = false;
    private float wordChipScrollX = 0.0f;
    private int chipLineIndex = -1;
    private boolean updatingFieldText = false;

    public LyricEditorScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("Редактор субтитр"));
        this.parent = parent;
        this.initTrackData();
    }

    private void initTrackData() {
        TrackState state = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : null;
        if (state != null && state.isActive() && !state.title().isEmpty()) {
            this.trackArtist = state.artist();
            this.trackTitle = state.title();
        } else {
            this.trackArtist = "Неизвестный автор";
            this.trackTitle = "Текущий трек";
        }

        this.customColor = HudConfig.INSTANCE.getTrackColor(this.trackArtist, this.trackTitle, HudConfig.INSTANCE.subtitlesColor);
        this.customActiveColor = HudConfig.INSTANCE.getTrackActiveColor(this.trackArtist, this.trackTitle, HudConfig.INSTANCE.subtitlesActiveColor);
        this.customMode = HudConfig.INSTANCE.getTrackMode(this.trackArtist, this.trackTitle, "DEFAULT");
        this.customScale = HudConfig.INSTANCE.getTrackScale(this.trackArtist, this.trackTitle, 1.0f);
        this.customFont = HudConfig.INSTANCE.getTrackFont(this.trackArtist, this.trackTitle, "minecraft:default");
        if (this.customFont == null || this.customFont.isEmpty() || "minecraft".equalsIgnoreCase(this.customFont.trim())) {
            this.customFont = "minecraft:default";
        }

        if (this.customColor != -1) {
            this.currentHexColor = this.customColor & 0xFFFFFF;
        }

        for (int p = 0; p < FONT_PRESETS.length; p++) {
            if (FONT_PRESETS[p][0].equalsIgnoreCase(this.customFont)) {
                this.fontPresetIdx = p;
                break;
            }
        }

        this.lines.clear();
        LyricsManager lm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getLyricsManager() : null;
        List<LyricLine> activeLyrics = lm != null ? lm.getCurrentTrackLyrics() : null;

        this.initialSnapshotLines.clear();
        if (HudConfig.INSTANCE.trackStyles != null) {
            this.initialStyleRaw = HudConfig.INSTANCE.trackStyles.get((this.trackArtist + "___" + this.trackTitle).toLowerCase());
        }
        if (activeLyrics != null && !activeLyrics.isEmpty()) {
            this.initialSnapshotLines.addAll(activeLyrics);
            for (LyricLine ll : activeLyrics) {
                String src = ll.rawText != null ? ll.rawText : ll.text;
                this.lines.add(new EditableLine(ll.startMs, ll.endMs, ll.vocalEndMs, src.replace('\u00a7', '&'), ll.words));
            }
        } else {
            this.lines.add(new EditableLine(0L, 4000L, "Текст первой строки..."));
            this.lines.add(new EditableLine(4000L, 8000L, "Текст второй строки..."));
            this.lines.add(new EditableLine(8000L, 12000L, "Текст третьей строки..."));
        }
        this.selectedIndex = 0;
        this.targetScroll = 0.0f;
        this.currentScroll = 0.0f;
        this.selectedWordIndex = -1;
    }

    protected void method_25426() {
        super.method_25426();
        int screenW = this.field_22789;
        int screenH = this.field_22790;

        int leftW = 118;
        int leftX = 6;
        int rightW = 118;
        int rightX = screenW - rightW - 6;

        int centerX = leftX + leftW + 8;
        int centerW = rightX - centerX - 8;

        int animColW = (leftW - 16) / 2;

        ModernButtonWidget bMiside = new ModernButtonWidget(leftX + 6, 32, animColW, 14, (class_2561)class_2561.method_43470("Мисайд"), b -> this.applyAnimToSelected("miside"));
        bMiside.setCustomBgColor(0xDD3A2A1A);
        this.method_37063(bMiside);

        ModernButtonWidget bBounce = new ModernButtonWidget(leftX + 6 + animColW + 4, 32, animColW, 14, (class_2561)class_2561.method_43470("Прыжок"), b -> this.applyAnimToSelected("bounce"));
        bBounce.setCustomBgColor(0xDD1B3348);
        this.method_37063(bBounce);

        ModernButtonWidget bWave = new ModernButtonWidget(leftX + 6, 48, animColW, 14, (class_2561)class_2561.method_43470("Волна"), b -> this.applyAnimToSelected("wave"));
        bWave.setCustomBgColor(0xDD114455);
        this.method_37063(bWave);

        ModernButtonWidget bPop = new ModernButtonWidget(leftX + 6 + animColW + 4, 48, animColW, 14, (class_2561)class_2561.method_43470("Поп"), b -> this.applyAnimToSelected("pop"));
        bPop.setCustomBgColor(0xDD441838);
        this.method_37063(bPop);

        ModernButtonWidget bPulse = new ModernButtonWidget(leftX + 6, 64, animColW, 14, (class_2561)class_2561.method_43470("Пульс"), b -> this.applyAnimToSelected("pulse"));
        bPulse.setCustomBgColor(0xDD163828);
        this.method_37063(bPulse);

        ModernButtonWidget bSlide = new ModernButtonWidget(leftX + 6 + animColW + 4, 64, animColW, 14, (class_2561)class_2561.method_43470("Слайд"), b -> this.applyAnimToSelected("slide"));
        bSlide.setCustomBgColor(0xDD282E3E);
        this.method_37063(bSlide);

        this.hexField = new class_342(this.field_22793, leftX + 6, 94, 72, 14, (class_2561)class_2561.method_43470("#RRGGBB"));
        this.hexField.method_1880(7);
        this.hexField.method_1852(String.format(Locale.US, "#%06X", this.currentHexColor));
        this.hexField.method_1863(text -> {
            String cleanHex = text.trim();
            if (cleanHex.startsWith("#")) {
                cleanHex = cleanHex.substring(1);
            }
            if (cleanHex.length() == 6) {
                try {
                    this.currentHexColor = Integer.parseInt(cleanHex, 16);
                } catch (Exception ignored) {}
            }
        });
        this.method_37063(this.hexField);

        int fullLeftW = leftW - 12;

        ModernButtonWidget bApplyHex = new ModernButtonWidget(leftX + 6, 111, animColW, 14, (class_2561)class_2561.method_43470("Применить"), b -> this.applyHexToSelected(this.hexField != null ? this.hexField.method_1882() : String.format(Locale.US, "%06X", this.currentHexColor)));
        bApplyHex.setCustomBgColor(0xDD114455);
        this.method_37063(bApplyHex);

        ModernButtonWidget bResetColor = new ModernButtonWidget(leftX + 6 + animColW + 4, 111, animColW, 14, (class_2561)class_2561.method_43470("Сброс"), b -> this.stripColorFromSelected());
        bResetColor.setCustomBgColor(0xDD332228);
        this.method_37063(bResetColor);

        ModernButtonWidget bTrackColor = new ModernButtonWidget(leftX + 6, 127, animColW, 14, (class_2561)class_2561.method_43470("Текст"), b -> {
            this.customColor = this.currentHexColor;
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14709, 1.4f, 0.7f);
        });
        bTrackColor.setCustomBgColor(0xDD243348);
        this.method_37063(bTrackColor);

        ModernButtonWidget bActiveColor = new ModernButtonWidget(leftX + 6 + animColW + 4, 127, animColW, 14, (class_2561)class_2561.method_43470("Караоке"), b -> {
            this.customActiveColor = this.currentHexColor;
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14709, 1.4f, 0.7f);
        });
        bActiveColor.setCustomBgColor(0xDD3A2E1A);
        this.method_37063(bActiveColor);

        ModernButtonWidget bFontPreset = new ModernButtonWidget(leftX + 6, 157, fullLeftW, 14, (class_2561)class_2561.method_43470("Шрифт: " + CustomFontManager.getDisplayName(this.customFont)), b -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507(new FontSelectorScreen(this, this.customFont, selectedFont -> {
                    this.customFont = selectedFont;
                    if (this.fontField != null) {
                        this.fontField.method_1852(this.customFont);
                    }
                    b.method_25355((class_2561)class_2561.method_43470("Шрифт: " + CustomFontManager.getDisplayName(this.customFont)));
                    if (this.selectedWordIndex >= 0 && this.selectedWordIndex < this.currentWordChips.size()) {
                        this.applyFontToSelected(this.customFont);
                        this.feedbackToast = "Шрифт применён к слову!";
                    } else if (this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
                        this.applyFontToLine(this.customFont);
                        this.feedbackToast = "Шрифт применён к строке!";
                    } else {
                        this.feedbackToast = "Шрифт трека: " + CustomFontManager.getDisplayName(this.customFont);
                    }
                    this.feedbackToastTime = System.currentTimeMillis();
                    this.syncRealtimeLyrics();
                }));
            }
        });
        bFontPreset.setCustomBgColor(0xDD1B3A2C);
        this.method_37063(bFontPreset);

        this.fontField = new class_342(this.field_22793, leftX + 6, 173, fullLeftW, 14, (class_2561)class_2561.method_43470("namespace:font"));
        this.fontField.method_1880(120);
        this.fontField.method_1852(this.customFont);
        this.fontField.method_1863(newFont -> {
            this.customFont = newFont.trim();
            bFontPreset.method_25355((class_2561)class_2561.method_43470("Шрифт: " + CustomFontManager.getDisplayName(this.customFont)));
            this.syncRealtimeLyrics();
        });
        this.method_37063(this.fontField);

        ModernButtonWidget bWordFont = new ModernButtonWidget(leftX + 6, 189, animColW, 14, (class_2561)class_2561.method_43470("К слову"), b -> this.applyFontToSelected(this.customFont));
        bWordFont.setCustomBgColor(0xDD193848);
        this.method_37063(bWordFont);

        ModernButtonWidget bLineFont = new ModernButtonWidget(leftX + 6 + animColW + 4, 189, animColW, 14, (class_2561)class_2561.method_43470("К строке"), b -> this.applyFontToLine(this.customFont));
        bLineFont.setCustomBgColor(0xDD283448);
        this.method_37063(bLineFont);

        ModernButtonWidget bTrackFont = new ModernButtonWidget(leftX + 6, 205, animColW, 14, (class_2561)class_2561.method_43470("Весь трек"), b -> {
            this.syncRealtimeLyrics();
            this.feedbackToast = "Шрифт трека установлен: " + this.customFont;
            this.feedbackToastTime = System.currentTimeMillis();
            this.playSound(class_3417.field_14709, 1.5f, 0.8f);
        });
        bTrackFont.setCustomBgColor(0xDD224433);
        this.method_37063(bTrackFont);

        ModernButtonWidget bResetFont = new ModernButtonWidget(leftX + 6 + animColW + 4, 205, animColW, 14, (class_2561)class_2561.method_43470("Сброс"), b -> this.stripFontFromSelected());
        bResetFont.setCustomBgColor(0xDD382228);
        this.method_37063(bResetFont);

        int timeColW = (rightW - 16) / 2;

        ModernButtonWidget bM05 = new ModernButtonWidget(rightX + 6, 32, timeColW, 14, (class_2561)class_2561.method_43470("-0.5 сек"), b -> this.adjustSelectedTime(-500));
        bM05.setCustomBgColor(0xDD283448);
        this.method_37063(bM05);

        ModernButtonWidget bP05 = new ModernButtonWidget(rightX + 6 + timeColW + 4, 32, timeColW, 14, (class_2561)class_2561.method_43470("+0.5 сек"), b -> this.adjustSelectedTime(500));
        bP05.setCustomBgColor(0xDD283448);
        this.method_37063(bP05);

        ModernButtonWidget bM01 = new ModernButtonWidget(rightX + 6, 48, timeColW, 14, (class_2561)class_2561.method_43470("-0.1 сек"), b -> this.adjustSelectedTime(-100));
        bM01.setCustomBgColor(0xDD223344);
        this.method_37063(bM01);

        ModernButtonWidget bP01 = new ModernButtonWidget(rightX + 6 + timeColW + 4, 48, timeColW, 14, (class_2561)class_2561.method_43470("+0.1 сек"), b -> this.adjustSelectedTime(100));
        bP01.setCustomBgColor(0xDD223344);
        this.method_37063(bP01);

        int fullRightW = rightW - 12;

        ModernButtonWidget bAddLine = new ModernButtonWidget(rightX + 6, 80, fullRightW, 14, (class_2561)class_2561.method_43470("+ Новая строка"), b -> this.addNewLine());
        bAddLine.setCustomBgColor(0xDD1E4428);
        this.method_37063(bAddLine);

        ModernButtonWidget bDelLine = new ModernButtonWidget(rightX + 6, 96, fullRightW, 14, (class_2561)class_2561.method_43470("✕ Удалить строку"), b -> this.deleteSelectedLine());
        bDelLine.setCustomBgColor(0xDD442228);
        this.method_37063(bDelLine);

        ModernButtonWidget btnMode = new ModernButtonWidget(rightX + 6, 128, fullRightW, 14, (class_2561)class_2561.method_43470(this.getModeDisplay()), b -> {
            this.cycleMode();
            b.method_25355((class_2561)class_2561.method_43470(this.getModeDisplay()));
            this.playSound(class_3417.field_14709, 1.2f, 0.7f);
            this.syncRealtimeLyrics();
        });
        btnMode.setCustomBgColor(0xDD243348);
        this.method_37063(btnMode);

        ModernButtonWidget btnScale = new ModernButtonWidget(rightX + 6, 144, fullRightW, 14, (class_2561)class_2561.method_43470("Размер: " + String.format(Locale.US, "%.1fx", Float.valueOf(this.customScale))), b -> {
            this.customScale = this.customScale >= 1.6f ? 0.8f : this.customScale + 0.2f;
            b.method_25355((class_2561)class_2561.method_43470("Размер: " + String.format(Locale.US, "%.1fx", Float.valueOf(this.customScale))));
            this.playSound(class_3417.field_14709, 1.2f, 0.7f);
            this.syncRealtimeLyrics();
        });
        btnScale.setCustomBgColor(0xDD283448);
        this.method_37063(btnScale);

        int drumH = Math.max(65, screenH - 146);
        int drumY = 36;

        int wordStripY = drumY + drumH + 3;
        int editFieldY = wordStripY + 16;

        String curText = (this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) ? this.lines.get(this.selectedIndex).text : "";
        this.editField = new class_342(this.field_22793, centerX + 4, editFieldY, centerW - 8, 15, (class_2561)class_2561.method_43470("Текст строки..."));
        this.editField.method_1880(1000);
        this.editField.method_1852(curText);
        this.editField.method_47404((class_2561)class_2561.method_43470("Текст выбранной строки..."));
        this.editField.method_1863(newText -> {
            if (this.updatingFieldText) {
                return;
            }
            if (this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
                this.lines.get(this.selectedIndex).text = newText;
                this.rebuildWordChips();
                this.syncRealtimeLyrics();
            }
        });
        this.method_37063(this.editField);

        int bottomY = screenH - 22;
        int saveBtnW = 120;
        int closeBtnW = 80;
        int btnsTotalW = saveBtnW + closeBtnW + 8;
        int btnsStartX = centerX + (centerW - btnsTotalW) / 2;

        ModernButtonWidget bSave = new ModernButtonWidget(btnsStartX, bottomY, saveBtnW, 16, (class_2561)class_2561.method_43470("Сохранить"), b -> this.saveLyrics());
        bSave.setPrimary(true);
        this.method_37063(bSave);

        ModernButtonWidget bClose = new ModernButtonWidget(btnsStartX + saveBtnW + 8, bottomY, closeBtnW, 16, (class_2561)class_2561.method_43470("Закрыть"), b -> this.method_25419());
        bClose.setCustomBgColor(0xDD333344);
        this.method_37063(bClose);

        ModernButtonWidget bTopClose = new ModernButtonWidget(screenW - 22, 4, 16, 14, (class_2561)class_2561.method_43470("✕"), b -> this.method_25419());
        bTopClose.setCustomBgColor(0xDD442228);
        this.method_37063(bTopClose);

        this.rebuildWordChips();
    }

    private void rebuildWordChips() {
        this.currentWordChips.clear();
        if (this.chipLineIndex != this.selectedIndex) {
            this.chipLineIndex = this.selectedIndex;
            this.wordChipScrollX = 0.0f;
        }
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        String text = this.lines.get(this.selectedIndex).text;
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Pattern pattern = Pattern.compile("(\\S+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            this.currentWordChips.add(new WordChip(matcher.start(), matcher.end(), matcher.group(1)));
        }
    }

    private void applyHexToSelected(String hex) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        if (hex == null || hex.isEmpty()) {
            return;
        }
        hex = hex.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return;
        }
        EditableLine line = this.lines.get(this.selectedIndex);
        String colorTag = "&#" + hex.toUpperCase(Locale.ROOT);
        if (this.selectedWordIndex >= 0 && this.selectedWordIndex < this.currentWordChips.size()) {
            WordChip chip = this.currentWordChips.get(this.selectedWordIndex);
            String wordText = line.text.substring(chip.start, chip.end);
            String cleanWord = stripColorFormatting(wordText);
            String replaced = colorTag + cleanWord + "&r";
            line.text = line.text.substring(0, chip.start) + replaced + line.text.substring(chip.end);
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14709, 1.5f, 0.7f);
        } else {
            this.insertTag(colorTag, "&r");
        }
    }

    private void applyFontToSelected(String fontId) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        if (fontId == null || fontId.trim().isEmpty()) {
            fontId = "minecraft:default";
        }
        fontId = fontId.trim();
        EditableLine line = this.lines.get(this.selectedIndex);
        if (this.selectedWordIndex >= 0 && this.selectedWordIndex < this.currentWordChips.size()) {
            WordChip chip = this.currentWordChips.get(this.selectedWordIndex);
            String wordText = line.text.substring(chip.start, chip.end);
            String cleanWord = stripFontFormatting(wordText);
            String replaced = "<font:" + fontId + ">" + cleanWord + "</font>";
            line.text = line.text.substring(0, chip.start) + replaced + line.text.substring(chip.end);
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14709, 1.5f, 0.7f);
        } else {
            this.insertTag("<font:" + fontId + ">", "</font>");
        }
    }

    private void applyFontToLine(String fontId) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        if (fontId == null || fontId.trim().isEmpty()) {
            fontId = "minecraft:default";
        }
        fontId = fontId.trim();
        EditableLine line = this.lines.get(this.selectedIndex);
        String cleaned = stripFontFormatting(line.text);
        line.text = "<font:" + fontId + ">" + cleaned + "</font>";
        this.updateFieldText();
        this.rebuildWordChips();
        this.syncRealtimeLyrics();
        this.playSound(class_3417.field_14709, 1.5f, 0.7f);
    }

    private void applyAnimToSelected(String animName) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        EditableLine line = this.lines.get(this.selectedIndex);
        if (this.selectedWordIndex >= 0 && this.selectedWordIndex < this.currentWordChips.size()) {
            WordChip chip = this.currentWordChips.get(this.selectedWordIndex);
            String wordText = line.text.substring(chip.start, chip.end);
            String cleanWord = wordText.replaceAll("<anim:[^>]+>", "").replaceAll("</anim>", "");
            String replaced = "<anim:" + animName + ">" + cleanWord + "</anim>";
            line.text = line.text.substring(0, chip.start) + replaced + line.text.substring(chip.end);
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14709, 1.5f, 0.7f);
        } else {
            this.insertTag("<anim:" + animName + ">", "</anim>");
        }
    }

    private void stripColorFromSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        EditableLine line = this.lines.get(this.selectedIndex);
        if (this.selectedWordIndex >= 0 && this.selectedWordIndex < this.currentWordChips.size()) {
            WordChip chip = this.currentWordChips.get(this.selectedWordIndex);
            String wordText = line.text.substring(chip.start, chip.end);
            String cleaned = stripColorFormatting(wordText);
            line.text = line.text.substring(0, chip.start) + cleaned + line.text.substring(chip.end);
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14624, 0.9f, 0.8f);
        } else {
            String cleaned = stripColorFormatting(line.text);
            line.text = cleaned;
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14624, 0.9f, 0.8f);
        }
    }

    private void stripFontFromSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        EditableLine line = this.lines.get(this.selectedIndex);
        if (this.selectedWordIndex >= 0 && this.selectedWordIndex < this.currentWordChips.size()) {
            WordChip chip = this.currentWordChips.get(this.selectedWordIndex);
            String wordText = line.text.substring(chip.start, chip.end);
            String cleaned = stripFontFormatting(wordText);
            line.text = line.text.substring(0, chip.start) + cleaned + line.text.substring(chip.end);
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14624, 0.9f, 0.8f);
        } else {
            String cleaned = stripFontFormatting(line.text);
            line.text = cleaned;
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14624, 0.9f, 0.8f);
        }
    }

    private static String stripColorFormatting(String str) {
        if (str == null) return "";
        return str.replaceAll("&#[0-9a-fA-F]{6}", "")
                  .replaceAll("<#[0-9a-fA-F]{6}>", "")
                  .replaceAll("</#>", "")
                  .replaceAll("&[0-9a-fA-FklmnorKLMNOR]", "")
                  .replaceAll("\u00a7[0-9a-fA-FklmnorKLMNOR]", "");
    }

    private static String stripFontFormatting(String str) {
        if (str == null) return "";
        return str.replaceAll("<font:[^>]+>", "")
                  .replaceAll("</font>", "")
                  .replaceAll("\\[font=[^\\]]+\\]", "")
                  .replaceAll("\\[/font\\]", "");
    }

    public static String stripAllFormatting(String str) {
        if (str == null) return "";
        return stripFontFormatting(stripColorFormatting(str))
            .replaceAll("<anim:[^>]+>", "")
            .replaceAll("</anim>", "");
    }

    private void insertTag(String prefix, String suffix) {
        if (this.editField == null || this.selectedIndex < 0 || this.selectedIndex >= this.lines.size()) {
            return;
        }
        String cur = this.editField.method_1882();
        String nextText = cur + prefix + suffix;
        this.updatingFieldText = true;
        try {
            this.editField.method_1852(nextText);
        } finally {
            this.updatingFieldText = false;
        }
        this.lines.get(this.selectedIndex).text = nextText;
        this.rebuildWordChips();
        this.syncRealtimeLyrics();
        this.playSound(class_3417.field_14709, 1.4f, 0.7f);
    }

    private void updateFieldText() {
        if (this.editField != null && this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
            this.updatingFieldText = true;
            try {
                this.editField.method_1852(this.lines.get(this.selectedIndex).text);
            } finally {
                this.updatingFieldText = false;
            }
        }
    }

    private void adjustSelectedTime(long deltaMs) {
        if (this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
            EditableLine line = this.lines.get(this.selectedIndex);
            long newStart = Math.max(0L, line.startMs + deltaMs);
            long appliedDelta = newStart - line.startMs;
            if (appliedDelta == 0L) {
                return;
            }
            line.startMs = newStart;
            line.endMs = Math.max(line.startMs + 60L, line.endMs + appliedDelta);
            line.vocalEndMs = Math.max(line.startMs + 50L, line.vocalEndMs + appliedDelta);
            if (!line.words.isEmpty()) {
                ArrayList<LyricLine.Word> shifted = new ArrayList<LyricLine.Word>(line.words.size());
                for (LyricLine.Word w : line.words) {
                    shifted.add(new LyricLine.Word(Math.max(0L, w.startMs() + appliedDelta), Math.max(0L, w.endMs() + appliedDelta), w.text(), w.charStart(), w.charEnd()));
                }
                line.words = shifted;
            }
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14709, 1.6f, 0.6f);
        }
    }

    private void addNewLine() {
        long newStart = 0L;
        if (!this.lines.isEmpty()) {
            EditableLine last = this.lines.get(this.lines.size() - 1);
            newStart = last.endMs + 500L;
        }
        EditableLine newLine = new EditableLine(newStart, newStart + 3500L, "Новая строка");
        this.lines.add(newLine);
        this.selectedIndex = this.lines.size() - 1;
        this.selectedWordIndex = -1;
        this.targetScroll = (float)this.selectedIndex;
        this.updateFieldText();
        this.rebuildWordChips();
        this.syncRealtimeLyrics();
        this.playSound(class_3417.field_14709, 1.3f, 0.8f);
    }

    private void deleteSelectedLine() {
        if (this.lines.size() > 1 && this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
            this.lines.remove(this.selectedIndex);
            if (this.selectedIndex >= this.lines.size()) {
                this.selectedIndex = this.lines.size() - 1;
            }
            this.selectedWordIndex = -1;
            this.targetScroll = (float)this.selectedIndex;
            this.updateFieldText();
            this.rebuildWordChips();
            this.syncRealtimeLyrics();
            this.playSound(class_3417.field_14624, 1.0f, 0.8f);
        }
    }

    private String getModeDisplay() {
        String effective = "DEFAULT".equalsIgnoreCase(this.customMode) ? HudConfig.INSTANCE.subtitlesMode : this.customMode;
        if (effective == null) effective = "WORLD_3D";
        return switch (effective.toUpperCase(Locale.ROOT)) {
            case "WORLD_3D" -> "3D мир";
            case "WORLD_BLOCK", "WORLD_SURFACE" -> "3D блоки";
            case "HUD_BOTTOM" -> "Снизу 2D";
            case "SCREEN_RANDOM" -> "Случайно 2D";
            case "CHAT" -> "В чате";
            default -> "3D мир";
        };
    }

    private void cycleMode() {
        String effective = "DEFAULT".equalsIgnoreCase(this.customMode) ? HudConfig.INSTANCE.subtitlesMode : this.customMode;
        if (effective == null) effective = "WORLD_3D";
        this.customMode = switch (effective.toUpperCase(Locale.ROOT)) {
            case "WORLD_3D" -> "WORLD_BLOCK";
            case "WORLD_BLOCK", "WORLD_SURFACE" -> "HUD_BOTTOM";
            case "HUD_BOTTOM" -> "SCREEN_RANDOM";
            case "SCREEN_RANDOM" -> "CHAT";
            case "CHAT" -> "WORLD_3D";
            default -> "WORLD_3D";
        };
    }

    private void syncRealtimeLyrics() {
        LyricsManager lm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getLyricsManager() : null;
        if (lm == null) {
            return;
        }
        this.previewDirty = true;
        lm.updateLyricsForCurrentTrack(this.trackArtist, this.trackTitle, this.buildInMemoryLines(), null);
        HudConfig.INSTANCE.setTrackStyle(this.trackArtist, this.trackTitle, this.customColor, this.customActiveColor, this.customMode, this.customScale, this.customFont);
    }

    private static String formatLrcTime(long ms) {
        long safe = Math.max(0L, ms);
        long min = safe / 60000L;
        long sec = (safe % 60000L) / 1000L;
        long cs = (safe % 1000L) / 10L;
        return String.format(Locale.US, "%02d:%02d.%02d", min, sec, cs);
    }

    private static String[] splitRawTokens(String text) {
        if (text == null) {
            return new String[0];
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s+");
    }

    private static String plainToken(String rawToken) {
        return stripAllFormatting(LyricLine.stripFormattingTags(rawToken)).trim();
    }

    private long[][] resolveWordTimes(EditableLine el, String[] rawTokens) {
        int n = rawTokens.length;
        long[][] times = new long[n][];
        if (n == 0) {
            return times;
        }
        List<LyricLine.Word> orig = el.words;
        if (orig.isEmpty()) {
            String plainLine = stripAllFormatting(LyricLine.stripFormattingTags(el.text));
            List<LyricLine.Word> seg = StudioCascadeAligner.segmentWordsBySyllables(plainLine, el.startMs, Math.max(el.startMs + 100L, el.vocalEndMs));
            for (int i = 0; i < n; i++) {
                if (i < seg.size()) {
                    times[i] = new long[]{seg.get(i).startMs(), seg.get(i).endMs()};
                }
            }
        } else {
            int cursor = 0;
            for (int i = 0; i < n; i++) {
                String plain = plainToken(rawTokens[i]);
                if (plain.isEmpty()) continue;
                for (int j = cursor; j < orig.size() && j < cursor + 4; j++) {
                    if (plain.equalsIgnoreCase(orig.get(j).text().trim())) {
                        times[i] = new long[]{orig.get(j).startMs(), orig.get(j).endMs()};
                        cursor = j + 1;
                        break;
                    }
                }
            }
        }
        long lineEnd = Math.max(el.startMs + 100L, el.vocalEndMs);
        int i = 0;
        while (i < n) {
            if (times[i] != null) {
                i++;
                continue;
            }
            int gapStart = i;
            while (i < n && times[i] == null) {
                i++;
            }
            int gapEnd = i;
            long from = gapStart > 0 ? times[gapStart - 1][1] : el.startMs;
            long to = gapEnd < n ? times[gapEnd][0] : lineEnd;
            if (to <= from) {
                to = from + 80L * (gapEnd - gapStart);
            }
            long step = Math.max(1L, (to - from) / (long)(gapEnd - gapStart));
            for (int k = gapStart; k < gapEnd; k++) {
                long s = from + step * (long)(k - gapStart);
                times[k] = new long[]{s, Math.max(s + 1L, s + step)};
            }
        }
        return times;
    }

    private List<LyricLine> buildInMemoryLines() {
        ArrayList<EditableLine> sorted = new ArrayList<EditableLine>(this.lines);
        Collections.sort(sorted, Comparator.comparingLong(a -> a.startMs));
        ArrayList<LyricLine> result = new ArrayList<LyricLine>(sorted.size());
        for (EditableLine el : sorted) {
            String[] rawTokens = splitRawTokens(el.text);
            ArrayList<LyricLine.Word> words = new ArrayList<LyricLine.Word>();
            if (!el.words.isEmpty()) {
                long[][] times = this.resolveWordTimes(el, rawTokens);
                for (int i = 0; i < rawTokens.length; i++) {
                    String plain = plainToken(rawTokens[i]);
                    if (plain.isEmpty() || times[i] == null) continue;
                    words.add(new LyricLine.Word(times[i][0], times[i][1], plain, 0, 0));
                }
            }
            result.add(new LyricLine(el.startMs, el.endMs, el.vocalEndMs, el.text, words));
        }
        return result;
    }

    private String buildLrcContent() {
        ArrayList<EditableLine> sorted = new ArrayList<EditableLine>(this.lines);
        Collections.sort(sorted, Comparator.comparingLong(a -> a.startMs));
        StringBuilder sb = new StringBuilder();
        for (EditableLine el : sorted) {
            sb.append('[').append(formatLrcTime(el.startMs)).append(']');
            String[] rawTokens = splitRawTokens(el.text);
            if (!el.words.isEmpty() && rawTokens.length > 0) {
                long[][] times = this.resolveWordTimes(el, rawTokens);
                long lastEnd = Math.max(el.vocalEndMs, el.endMs);
                for (int i = 0; i < rawTokens.length; i++) {
                    if (i > 0) sb.append(' ');
                    if (times[i] != null) {
                        sb.append('<').append(formatLrcTime(times[i][0])).append('>');
                        lastEnd = Math.max(lastEnd, times[i][1]);
                    }
                    sb.append(rawTokens[i]);
                }
                sb.append('<').append(formatLrcTime(Math.max(el.startMs + 50L, lastEnd))).append('>');
            } else {
                sb.append(el.text);
                if (el.endMs > el.startMs) {
                    sb.append('<').append(formatLrcTime(el.endMs)).append('>');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public boolean method_25421() {
        return false;
    }

    private void saveLyrics() {
        if (this.lines.isEmpty()) {
            return;
        }
        if (this.editField != null && this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
            this.lines.get(this.selectedIndex).text = this.editField.method_1882();
        }
        Collections.sort(this.lines, Comparator.comparingLong(a -> a.startMs));

        LyricsManager lm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getLyricsManager() : null;
        if (lm != null) {
            List<LyricLine> inMemory = this.buildInMemoryLines();
            lm.updateLyricsForCurrentTrack(this.trackArtist, this.trackTitle, inMemory, this.buildLrcContent());
            this.initialSnapshotLines.clear();
            this.initialSnapshotLines.addAll(inMemory);
        }
        this.savedChanges = true;
        this.previewDirty = false;
        this.rebuildWordChips();

        HudConfig.INSTANCE.setTrackStyle(this.trackArtist, this.trackTitle, this.customColor, this.customActiveColor, this.customMode, this.customScale, this.customFont);
        HudConfig.save();
        this.initialStyleRaw = HudConfig.INSTANCE.trackStyles != null ? HudConfig.INSTANCE.trackStyles.get((this.trackArtist + "___" + this.trackTitle).toLowerCase()) : null;

        this.feedbackToast = "Субтитры, шрифты и стили успешно сохранены!";
        this.feedbackToastTime = System.currentTimeMillis();
        this.playSound(class_3417.field_15015.comp_349(), 1.5f, 0.9f);
    }

    private void playSound(class_6880<class_3414> sound, float pitch, float volume) {
        if (sound != null && sound.comp_349() != null) {
            this.playSound((class_3414)sound.comp_349(), pitch, volume);
        }
    }

    private void playSound(class_3414 sound, float pitch, float volume) {
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.method_1483() != null && sound != null) {
            mc.method_1483().method_4873((class_1113)class_1109.method_4757(sound, pitch, volume));
        }
    }

    public void method_25419() {
        if (this.previewDirty) {
            this.revertUnsavedPreview();
        }
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    private void revertUnsavedPreview() {
        this.previewDirty = false;
        String styleKey = (this.trackArtist + "___" + this.trackTitle).toLowerCase();
        if (HudConfig.INSTANCE.trackStyles != null) {
            if (this.initialStyleRaw != null) {
                HudConfig.INSTANCE.trackStyles.put(styleKey, this.initialStyleRaw);
            } else {
                HudConfig.INSTANCE.trackStyles.remove(styleKey);
            }
        }
        LyricsManager lm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getLyricsManager() : null;
        if (lm == null) {
            return;
        }
        if (!this.initialSnapshotLines.isEmpty()) {
            lm.updateLyricsForCurrentTrack(this.trackArtist, this.trackTitle, new ArrayList<LyricLine>(this.initialSnapshotLines), null);
        } else if (!this.savedChanges) {
            lm.reloadCurrentTrack();
        }
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 265) {
            if (this.selectedIndex > 0) {
                this.selectedIndex--;
                this.selectedWordIndex = -1;
                this.targetScroll = (float)this.selectedIndex;
                this.updateFieldText();
                this.rebuildWordChips();
                this.playSound(class_3417.field_14709, 1.6f, 0.5f);
                return true;
            }
        } else if (input.comp_4795() == 264) {
            if (this.selectedIndex < this.lines.size() - 1) {
                this.selectedIndex++;
                this.selectedWordIndex = -1;
                this.targetScroll = (float)this.selectedIndex;
                this.updateFieldText();
                this.rebuildWordChips();
                this.playSound(class_3417.field_14709, 1.6f, 0.5f);
                return true;
            }
        }
        return super.method_25404(input);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int screenW = this.field_22789;
        int screenH = this.field_22790;
        int leftW = 118;
        int leftX = 6;
        int rightW = 118;
        int rightX = screenW - rightW - 6;
        int centerX = leftX + leftW + 8;
        int centerW = rightX - centerX - 8;
        int drumY = 36;
        int drumH = Math.max(65, screenH - 146);

        int wordStripYScroll = drumY + drumH + 3;
        if (mouseX >= (double)centerX && mouseX <= (double)(centerX + centerW) && mouseY >= (double)(wordStripYScroll - 3) && mouseY <= (double)(wordStripYScroll + 17)) {
            double amount = horizontalAmount != 0.0 ? horizontalAmount : verticalAmount;
            float maxScroll = Math.max(0.0f, (float)(this.computeChipsTotalWidth() - (centerW - 8)));
            this.wordChipScrollX = Math.max(0.0f, Math.min(maxScroll, this.wordChipScrollX - (float)(amount * 24.0)));
            return true;
        }

        if (mouseX >= (double)centerX && mouseX <= (double)(centerX + centerW) && mouseY >= (double)drumY && mouseY <= (double)(drumY + drumH)) {
            this.targetScroll = (float)((double)this.targetScroll - verticalAmount);
            this.clampScroll();
            int newIdx = Math.round(this.targetScroll);
            if (newIdx != this.selectedIndex && newIdx >= 0 && newIdx < this.lines.size()) {
                this.selectedIndex = newIdx;
                this.selectedWordIndex = -1;
                this.updateFieldText();
                this.rebuildWordChips();
                this.playSound(class_3417.field_14709, 1.8f, 0.4f);
            }
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        int screenW = this.field_22789;
        int screenH = this.field_22790;
        int leftW = 118;
        int leftX = 6;
        int rightW = 118;
        int rightX = screenW - rightW - 6;
        int centerX = leftX + leftW + 8;
        int centerW = rightX - centerX - 8;
        int drumY = 36;
        int drumH = Math.max(65, screenH - 146);
        int drumCenterY = drumY + drumH / 2;

        if (click.comp_4800() != null && click.comp_4800().comp_4801() == 0) {
            int swatchX = leftX + 82;
            int swatchY = 94;
            if (mx >= (double)swatchX && mx <= (double)(swatchX + 30) && my >= (double)swatchY && my <= (double)(swatchY + 15)) {
                this.applyHexToSelected(String.format(Locale.US, "%06X", this.currentHexColor));
                return true;
            }

            if (mx >= (double)centerX && mx <= (double)(centerX + centerW) && my >= (double)drumY && my <= (double)(drumY + drumH)) {
                this.isDraggingReel = true;
                this.lastDragMouseY = my;
                float drumRadius = (float)drumH * 0.48f;
                float relY = (float)(my - (double)drumCenterY);
                float sinVal = Math.max(-0.95f, Math.min(0.95f, relY / drumRadius));
                float angle = (float)Math.asin((double)sinVal);
                int clickedIdx = Math.round(this.currentScroll + angle / 0.25f);
                if (clickedIdx >= 0 && clickedIdx < this.lines.size() && clickedIdx != this.selectedIndex) {
                    this.selectedIndex = clickedIdx;
                    this.selectedWordIndex = -1;
                    this.targetScroll = (float)clickedIdx;
                    this.updateFieldText();
                    this.rebuildWordChips();
                    this.playSound(class_3417.field_14709, 1.6f, 0.5f);
                }
                return true;
            }

            int wordStripY = drumY + drumH + 3;
            if (my >= (double)(wordStripY - 3) && my <= (double)(wordStripY + 17) && mx >= (double)(centerX + 2) && mx <= (double)(centerX + centerW - 2)) {
                class_327 tr = this.field_22793;
                int curChipX = centerX + 4 - Math.round(this.wordChipScrollX);
                int wholeLineBtnW = tr.method_1727("Вся строка") + 10;
                if (mx >= (double)curChipX && mx <= (double)(curChipX + wholeLineBtnW)) {
                    this.selectedWordIndex = -1;
                    this.playSound(class_3417.field_14709, 1.6f, 0.6f);
                    return true;
                }
                curChipX += wholeLineBtnW + 4;

                for (int wIdx = 0; wIdx < this.currentWordChips.size(); ++wIdx) {
                    WordChip chip = this.currentWordChips.get(wIdx);
                    String clean = stripAllFormatting(chip.text);
                    int chipW = tr.method_1727(clean) + 12;
                    if (mx >= (double)curChipX && mx <= (double)(curChipX + chipW)) {
                        this.selectedWordIndex = (this.selectedWordIndex == wIdx) ? -1 : wIdx;
                        this.playSound(class_3417.field_14709, 1.6f, 0.6f);
                        return true;
                    }
                    curChipX += chipW + 4;
                }
            }
        }
        return super.method_25402(click, bl);
    }

    public boolean method_25406(class_11909 click) {
        this.isDraggingReel = false;
        return super.method_25406(click);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        if (this.isDraggingReel) {
            this.targetScroll = (float)((double)this.targetScroll - deltaY / 24.0);
            this.clampScroll();
            int newIdx = Math.round(this.targetScroll);
            if (newIdx != this.selectedIndex && newIdx >= 0 && newIdx < this.lines.size()) {
                this.selectedIndex = newIdx;
                this.selectedWordIndex = -1;
                this.updateFieldText();
                this.rebuildWordChips();
                this.playSound(class_3417.field_14709, 1.8f, 0.35f);
            }
            return true;
        }
        return super.method_25403(click, deltaX, deltaY);
    }

    private int computeChipsTotalWidth() {
        class_327 tr = this.field_22793;
        if (tr == null) {
            return 0;
        }
        int total = tr.method_1727("Вся строка") + 14;
        for (WordChip chip : this.currentWordChips) {
            total += tr.method_1727(stripAllFormatting(chip.text)) + 16;
        }
        return total;
    }

    private void clampScroll() {
        int max = Math.max(0, this.lines.size() - 1);
        if (this.targetScroll < 0.0f) {
            this.targetScroll = 0.0f;
        } else if (this.targetScroll > (float)max) {
            this.targetScroll = (float)max;
        }
    }

    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        class_327 tr = this.field_22793;
        int screenW = this.field_22789;
        int screenH = this.field_22790;

        this.currentScroll += (this.targetScroll - this.currentScroll) * 0.28f;

        int leftW = 118;
        int leftX = 6;
        int rightW = 118;
        int rightX = screenW - rightW - 6;

        int centerX = leftX + leftW + 8;
        int centerW = rightX - centerX - 8;

        int panelH = screenH - 30;

        MusicHudRenderer.renderRoundedRect(context, leftX, 16, leftW, panelH, 7, 0x880B101C);
        MusicHudRenderer.renderRoundedBorder(context, leftX, 16, leftW, panelH, 7, 0x4400E5FF, 0x22112233);

        context.method_51433(tr, "АНИМАЦИИ", leftX + 8, 21, 0xFFFFD54F, true);

        context.method_51433(tr, "ЦВЕТ (HEX)", leftX + 8, 83, 0xFF00E5FF, true);

        int swatchX = leftX + 82;
        int swatchY = 94;
        MusicHudRenderer.renderRoundedRect(context, swatchX, swatchY, 30, 14, 3, 0xFF000000 | this.currentHexColor);
        MusicHudRenderer.renderRoundedBorder(context, swatchX, swatchY, 30, 14, 3, 0xFFFFFFFF, 0x8800E5FF);

        context.method_51433(tr, "ШРИФТЫ", leftX + 8, 146, 0xFF00FF88, true);

        MusicHudRenderer.renderRoundedRect(context, rightX, 16, rightW, panelH, 7, 0x880B101C);
        MusicHudRenderer.renderRoundedBorder(context, rightX, 16, rightW, panelH, 7, 0x4400E5FF, 0x22112233);

        context.method_51433(tr, "ТАЙМИНГ", rightX + 8, 21, 0xFF00E5FF, true);
        context.method_51433(tr, "СТРОКИ", rightX + 8, 69, 0xFFFFD54F, true);
        context.method_51433(tr, "ПАРАМЕТРЫ", rightX + 8, 117, 0xFF00FF88, true);

        TrackState liveState = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : null;
        boolean hasTrack = liveState != null && liveState.isActive() && !liveState.title().isEmpty();
        boolean isPlaying = hasTrack && liveState.isPlaying();
        CoverTextureManager cm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getCoverManager() : null;

        MusicHudRenderer.renderRoundedRect(context, centerX, 4, centerW, 18, 5, 0x88090F1C);
        MusicHudRenderer.renderRoundedBorder(context, centerX, 4, centerW, 18, 5, 0x5500E5FF, 0x2200E5FF);

        long now = System.currentTimeMillis();
        float vinylSpeed = isPlaying ? 0.05f : 0.008f;
        float vinylAngle = (float)(now * vinylSpeed) % 360.0f;

        context.method_51448().pushMatrix();
        context.method_51448().translate((float)centerX + 11.0f, 13.0f);
        context.method_51448().rotate((float)Math.toRadians((double)vinylAngle));
        context.method_25291(class_10799.field_56883, TEX_VINYL, -6, -6, 0.0f, 0.0f, 12, 12, 14, 14, -1);
        context.method_51448().popMatrix();

        int textStartX = centerX + 22;
        if (cm != null) {
            class_2960 cov = cm.getCurrentCoverId();
            if (cov != null) {
                context.method_25291(class_10799.field_56883, cov, textStartX, 6, 0.0f, 0.0f, 13, 13, 14, 14, -1);
                MusicHudRenderer.renderRoundedBorder(context, textStartX, 6, 13, 13, 3, 0x8800E5FF, 0x4400E5FF);
                textStartX += 16;
            }
        }

        class_2960 playIcon = isPlaying ? ICON_PAUSE : ICON_PLAY;
        context.method_25291(class_10799.field_56883, playIcon, textStartX, 7, 0.0f, 0.0f, 10, 10, 64, 64, -1);
        textStartX += 13;

        int maxTitleW = centerW - (textStartX - centerX) - 8;
        String titleStr = "§bРедактор субтитров §7| §f" + this.trackArtist + " §7— §e" + this.trackTitle;
        String trimmedTitle = tr.method_27523(titleStr, maxTitleW);
        context.method_51433(tr, trimmedTitle, textStartX, 9, 0xFFFFFFFF, true);

        context.method_51433(tr, "СПИСОК СТРОК", centerX + 8, 25, 0xFF00E5FF, true);

        int drumH = Math.max(65, screenH - 146);
        int drumY = 36;
        int drumCenterY = drumY + drumH / 2;

        context.method_51448().pushMatrix();
        context.method_51448().translate((float)centerX + (float)centerW / 2.0f, (float)drumCenterY);
        int glowSize = Math.min(screenW, (int)((float)drumH * 2.2f));
        int halfGlow = glowSize / 2;
        context.method_25291(class_10799.field_56883, TEX_GLOW, -halfGlow, -halfGlow, 0.0f, 0.0f, glowSize, glowSize, glowSize, glowSize, -1);
        context.method_51448().popMatrix();

        MusicHudRenderer.renderRoundedRect(context, centerX, drumY, centerW, drumH, 8, 0x88080D18);
        MusicHudRenderer.renderRoundedBorder(context, centerX, drumY, centerW, drumH, 8, 0x5500E5FF, 0x22334466);

        int focusH = 22;
        int focusY = drumCenterY - focusH / 2;
        MusicHudRenderer.renderRoundedRect(context, centerX + 4, focusY, centerW - 8, focusH, 5, 0x2800E5FF);
        MusicHudRenderer.renderRoundedBorder(context, centerX + 4, focusY, centerW - 8, focusH, 5, 0xAA00E5FF, 0x4400E5FF);

        context.method_51433(tr, "◀", centerX + 8, drumCenterY - 4, 0xFF00E5FF, true);
        context.method_51433(tr, "▶", centerX + centerW - 14, drumCenterY - 4, 0xFF00E5FF, true);

        context.method_44379(centerX, drumY + 2, centerX + centerW, drumY + drumH - 2);

        float drumRadius = (float)drumH * 0.48f;
        long livePos = (liveState != null && liveState.isActive()) ? liveState.getInterpolatedPositionMs() : -1L;

        for (int i = 0; i < this.lines.size(); ++i) {
            EditableLine el = this.lines.get(i);
            float dist = (float)i - this.currentScroll;
            if (Math.abs(dist) > 5.5f) {
                continue;
            }
            float angle = dist * 0.25f;
            float lineY = (float)drumCenterY + (float)Math.sin((double)angle) * drumRadius;
            float depth = (float)Math.cos((double)angle);
            if (depth <= 0.05f) {
                continue;
            }

            float alphaF = Math.max(0.06f, depth * depth);
            int alphaInt = (int)(alphaF * 255.0f);
            if (alphaInt <= 4) {
                continue;
            }

            long min = el.startMs / 60000L;
            long sec = (el.startMs % 60000L) / 1000L;
            long cs = (el.startMs % 1000L) / 10L;
            String timeTag = String.format(Locale.US, "[%02d:%02d.%02d]", min, sec, cs);

            boolean isCurrentInTrack = (livePos >= el.startMs && livePos < el.endMs);
            if (isCurrentInTrack) {
                timeTag = "♫ " + timeTag;
            }

            int tagW = tr.method_1727(timeTag);
            int maxTxtW = centerW - tagW - 48;
            String cleanReel = LyricLine.toMinecraftFormatting(stripFontFormatting(el.text).replaceAll("<anim:[^>]+>", "").replaceAll("</anim>", ""));
            String trimmedTxt = tr.method_27523(cleanReel, maxTxtW);

            int itemDrawY = (int)lineY - 4;
            boolean isSel = (i == this.selectedIndex);
            int tagCol = alphaInt << 24 | (isCurrentInTrack ? 0x00FF88 : (isSel ? 0x00E5FF : 0x667788));
            int lineCol = alphaInt << 24 | (isSel ? 0xFFFFF0 : 0x99AABB);

            context.method_51433(tr, timeTag, centerX + 20, itemDrawY, tagCol, isSel || isCurrentInTrack);
            context.method_51433(tr, trimmedTxt, centerX + 26 + tagW, itemDrawY, lineCol, isSel);
        }
        context.method_44380();

        context.method_25291(class_10799.field_56883, TEX_DRUM_DEPTH, centerX + 1, drumY + 1, 0.0f, 0.0f, centerW - 2, 24, 32, 128, -1);
        context.method_25291(class_10799.field_56883, TEX_DRUM_DEPTH_BOTTOM, centerX + 1, drumY + drumH - 25, 0.0f, 0.0f, centerW - 2, 24, 32, 128, -1);

        int wordStripY = drumY + drumH + 3;
        context.method_51433(tr, "СЛОВА СТРОКИ:", centerX + 4, wordStripY - 10, 0xFFFFD54F, true);

        int curChipX = centerX + 4 - Math.round(this.wordChipScrollX);
        context.method_44379(centerX + 2, wordStripY - 1, centerX + centerW - 2, wordStripY + 15);
        int wholeLineBtnW = tr.method_1727("Вся строка") + 10;
        int wholeLineBg = (this.selectedWordIndex == -1) ? 0xDD00E5FF : 0x44223344;
        int wholeLineBorder = (this.selectedWordIndex == -1) ? 0xFF00E5FF : 0x88446688;
        int wholeLineText = (this.selectedWordIndex == -1) ? 0x05060A : 0x88CCEE;
        MusicHudRenderer.renderRoundedRect(context, curChipX, wordStripY, wholeLineBtnW, 14, 4, wholeLineBg);
        MusicHudRenderer.renderRoundedBorder(context, curChipX, wordStripY, wholeLineBtnW, 14, 4, wholeLineBorder, wholeLineBorder);
        context.method_51433(tr, "Вся строка", curChipX + 5, wordStripY + 3, wholeLineText, this.selectedWordIndex != -1);
        curChipX += wholeLineBtnW + 4;

        for (int wIdx = 0; wIdx < this.currentWordChips.size(); ++wIdx) {
            WordChip chip = this.currentWordChips.get(wIdx);
            String clean = stripAllFormatting(chip.text);
            int chipW = tr.method_1727(clean) + 12;
            if (curChipX > centerX + centerW) {
                break;
            }
            boolean isSel = (this.selectedWordIndex == wIdx);
            int chipBg = isSel ? 0xEE00E5FF : 0x441A2536;
            int chipBorder = isSel ? 0xFFFFFFFF : 0x77335577;
            int chipTextCol = isSel ? 0x05060A : 0xDDFFFF;

            MusicHudRenderer.renderRoundedRect(context, curChipX, wordStripY, chipW, 14, 4, chipBg);
            MusicHudRenderer.renderRoundedBorder(context, curChipX, wordStripY, chipW, 14, 4, chipBorder, chipBorder);
            context.method_51433(tr, clean, curChipX + 6, wordStripY + 3, chipTextCol, !isSel);
            curChipX += chipW + 4;
        }
        context.method_44380();

        int editFieldY = wordStripY + 16;
        int previewPanelY = editFieldY + 20;
        this.renderPreview(context, centerX, previewPanelY, centerW, 22);

        if (this.feedbackToast != null) {
            long age = System.currentTimeMillis() - this.feedbackToastTime;
            if (age < 3000L) {
                float toastAlpha = age < 2500L ? 1.0f : 1.0f - (float)(age - 2500L) / 500.0f;
                int toastA = (int)(toastAlpha * 255.0f);
                int tW = tr.method_1727(this.feedbackToast) + 24;
                int tX = (screenW - tW) / 2;
                int tY = screenH - 42;
                MusicHudRenderer.renderRoundedRect(context, tX, tY, tW, 16, 5, toastA << 24 | 0x1B3828);
                MusicHudRenderer.renderRoundedBorder(context, tX, tY, tW, 16, 5, toastA << 24 | 0x00FFAA, toastA << 24 | 0x00FFAA);
                context.method_51433(tr, this.feedbackToast, tX + 12, tY + 4, toastA << 24 | 0x00FFAA, true);
            } else {
                this.feedbackToast = null;
            }
        }
        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void renderPreview(class_332 context, int px, int py, int pw, int ph) {
        class_327 tr = this.field_22793;
        MusicHudRenderer.renderRoundedRect(context, px, py, pw, ph, 5, 0x88090E18);
        MusicHudRenderer.renderRoundedBorder(context, px, py, pw, ph, 5, 0x4400E5FF, 0x2200E5FF);

        CoverTextureManager cm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getCoverManager() : null;
        TrackState liveState = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : null;
        boolean isTrackActive = (liveState != null && liveState.isActive());

        int curX = px + 6;
        if (cm != null) {
            class_2960 cov = cm.getCurrentCoverId();
            if (cov != null) {
                int cs = ph - 6;
                int cy = py + 3;
                context.method_25291(class_10799.field_56883, cov, curX, cy, 0.0f, 0.0f, cs, cs, cs, cs, -1);
                MusicHudRenderer.renderRoundedBorder(context, curX, cy, cs, cs, 3, 0x8800E5FF, 0x4400E5FF);
                curX += cs + 6;
            }
        }

        int waveW = 28;
        int waveH = 8;
        int waveY = py + (ph - waveH) / 2;
        context.method_25291(class_10799.field_56883, TEX_SOUNDWAVE, curX, waveY, 0.0f, 0.0f, waveW, waveH, 64, 16, -1);
        curX += waveW + 6;

        context.method_51433(tr, "ПРЕДПРОСМОТР:", curX, py + ph / 2 - 4, 0xFF00E5FF, true);
        curX += tr.method_1727("ПРЕДПРОСМОТР:") + 6;

        if (this.selectedIndex >= 0 && this.selectedIndex < this.lines.size()) {
            EditableLine line = this.lines.get(this.selectedIndex);
            LyricLine dummy = new LyricLine(line.startMs, line.endMs, line.text, null);
            int textColor = this.customColor != -1 ? (this.customColor & 0xFFFFFF) : 0xFFFFFF;
            int textY = py + ph / 2 - 4;
            String text = dummy.text;
            if (text != null && !text.isEmpty()) {
                int drawX = curX;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    String charFont = dummy.getCharFont(i);
                    if (charFont == null || charFont.isEmpty()) {
                        charFont = this.customFont;
                    }
                    int rawCol = dummy.getRawCharColor(i);
                    int charCol = rawCol != -1 ? (rawCol & 0xFFFFFF) : textColor;
                    int finalCol = 0xFF000000 | charCol;

                    if (CustomFontManager.isCustomFont(charFont)) {
                        CustomFontManager.CustomGlyph glyph = CustomFontManager.getGlyph(charFont, c);
                        int charW = glyph != null ? Math.max(1, Math.round(glyph.advance)) : 6;
                        if (drawX + charW > px + pw - 60) break;
                        CustomFontManager.drawGlyph2D(context, glyph, drawX, textY, finalCol);
                        drawX += charW;
                    } else {
                        String s = String.valueOf(c);
                        int charW = tr.method_1727(s);
                        if (drawX + charW > px + pw - 60) break;
                        context.method_51433(tr, s, drawX, textY, finalCol, true);
                        drawX += charW;
                    }
                }
            }
        }

        if (isTrackActive) {
            String syncStr = "[В ЭФИРЕ]";
            int syncW = tr.method_1727(syncStr);
            context.method_51433(tr, syncStr, px + pw - syncW - 8, py + ph / 2 - 4, 0xFF00FF88, true);
        }
    }

    private static class EditableLine {
        long startMs;
        long endMs;
        long vocalEndMs;
        String text;
        List<LyricLine.Word> words;

        EditableLine(long startMs, long endMs, String text) {
            this(startMs, endMs, endMs, text, null);
        }

        EditableLine(long startMs, long endMs, long vocalEndMs, String text, List<LyricLine.Word> words) {
            this.startMs = startMs;
            this.endMs = Math.max(endMs, startMs + 60L);
            this.vocalEndMs = Math.max(startMs + 50L, Math.min(this.endMs, vocalEndMs));
            this.text = text != null ? text : "";
            this.words = words != null ? new ArrayList<LyricLine.Word>(words) : new ArrayList<LyricLine.Word>();
        }
    }

    private static class WordChip {
        final int start;
        final int end;
        final String text;

        WordChip(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }
}
