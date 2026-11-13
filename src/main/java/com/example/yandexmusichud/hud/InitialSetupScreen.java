package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.HudEditScreen;
import com.example.yandexmusichud.hud.KeybindSetupScreen;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_6880;

public class InitialSetupScreen extends class_437 {
    private final class_437 parent;
    private final Random random = new Random();
    private int currentStep = 0;
    private static final int TOTAL_STEPS = 4;
    private long stepStartTime = System.currentTimeMillis();
    private final List<SetupParticle> particles = new ArrayList<>();
    private long testActionTime = 0L;

    public InitialSetupScreen(class_437 parent) {
        super(class_2561.method_43470("Быстрая настройка Music Subtitles"));
        this.parent = parent;
    }

    private int getCardW() {
        return Math.min(520, this.field_22789 - 24);
    }

    private int getCardH() {
        return Math.min(320, this.field_22790 - 24);
    }

    private int getCardX() {
        return (this.field_22789 - this.getCardW()) / 2;
    }

    private int getCardY() {
        return (this.field_22790 - this.getCardH()) / 2;
    }

    protected void method_25426() {
        super.method_25426();
        this.stepStartTime = System.currentTimeMillis();
        this.playSound(class_3417.field_26980, 1.2f, 0.85f);
        this.particles.clear();
        for (int i = 0; i < 40; ++i) {
            float px = this.random.nextFloat() * (float)this.field_22789;
            float py = this.random.nextFloat() * (float)this.field_22790;
            this.particles.add(new SetupParticle(px, py));
        }
    }

    private void playSound(class_3414 sound, float pitch, float volume) {
        try {
            class_310.method_1551().method_1483().method_4873((class_1113)class_1109.method_4757(sound, pitch, volume));
        } catch (Exception ignored) {}
    }

    private void playSound(class_6880<class_3414> sound, float pitch, float volume) {
        if (sound != null && sound.comp_349() != null) {
            this.playSound((class_3414)sound.comp_349(), pitch, volume);
        }
    }

    private void setStep(int step) {
        if (step < 0) step = 0;
        if (step >= TOTAL_STEPS) {
            this.finishAndExit();
            return;
        }
        if (step != this.currentStep) {
            this.currentStep = step;
            this.stepStartTime = System.currentTimeMillis();
            this.playSound(class_3417.field_14793, 1.3f, 0.8f);
            this.playSound(class_3417.field_15015.comp_349(), 1.4f, 0.7f);
            for (int i = 0; i < 25; ++i) {
                float px = (float)this.field_22789 / 2.0f + (this.random.nextFloat() - 0.5f) * 140.0f;
                float py = (float)this.field_22790 / 2.0f + (this.random.nextFloat() - 0.5f) * 90.0f;
                this.particles.add(new SetupParticle(px, py));
            }
        }
    }

    private void finishAndExit() {
        HudConfig.INSTANCE.initialSetupCompleted = true;
        HudConfig.save();
        this.playSound(class_3417.field_14931, 1.8f, 0.6f);
        this.playSound(class_3417.field_14706, 1.2f, 0.8f);
        class_310 mc = class_310.method_1551();
        if (mc != null) {
            if (mc.field_1687 != null) {
                mc.method_1507(null);
            } else if (this.parent != null && !(this.parent instanceof InitialSetupScreen)) {
                mc.method_1507(this.parent);
            } else {
                mc.method_1507(new class_442());
            }
        }
    }

    public boolean method_25404(class_11908 input) {
        int key = input.comp_4795();
        if (key == 256) {
            if (this.currentStep > 0) {
                this.setStep(this.currentStep - 1);
            } else {
                this.finishAndExit();
            }
            return true;
        }
        if (key == 262 || key == 257 || key == 32) {
            this.setStep(this.currentStep + 1);
            return true;
        }
        if (key == 263 && this.currentStep > 0) {
            this.setStep(this.currentStep - 1);
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int cardW = this.getCardW();
        int cardH = this.getCardH();
        int cardX = this.getCardX();
        int cardY = this.getCardY();
        int centerX = cardX + cardW / 2;

        int skipW = 95;
        int skipH = 20;
        int skipX = cardX + cardW - skipW - 16;
        int skipY = cardY + 11;
        if (mx >= skipX && mx <= skipX + skipW && my >= skipY && my <= skipY + skipH) {
            this.finishAndExit();
            return true;
        }

        int navY = cardY + cardH - 32;
        int btnH = 24;

        int prevW = 95;
        int prevX = cardX + 18;
        if (this.currentStep > 0 && mx >= prevX && mx <= prevX + prevW && my >= navY && my <= navY + btnH) {
            this.setStep(this.currentStep - 1);
            return true;
        }

        int nextW = this.currentStep == TOTAL_STEPS - 1 ? 160 : 115;
        int nextX = cardX + cardW - nextW - 18;
        if (mx >= nextX && mx <= nextX + nextW && my >= navY && my <= navY + btnH) {
            this.setStep(this.currentStep + 1);
            return true;
        }

        int dotSpacing = 16;
        int totalDotsW = (TOTAL_STEPS - 1) * dotSpacing;
        int dotStartX = centerX - totalDotsW / 2;
        int dotY = navY + 12;
        for (int i = 0; i < TOTAL_STEPS; ++i) {
            int dx = dotStartX + i * dotSpacing;
            if (mx >= dx - 8 && mx <= dx + 8 && my >= dotY - 8 && my <= dotY + 8) {
                this.setStep(i);
                return true;
            }
        }

        int gridPad = 18;
        int gridX = cardX + gridPad;
        int gridY = cardY + 60;
        int gapX = 10;
        int gapY = 8;
        int colW = (cardW - gridPad * 2 - gapX) / 2;
        int rowH = cardH < 270 ? Math.max(46, (cardH - 135) / 2) : 62;

        if (this.currentStep == 0) {
            for (int i = 0; i < 4; ++i) {
                int col = i % 2;
                int row = i / 2;
                int bx = gridX + col * (colW + gapX);
                int by = gridY + row * (rowH + gapY);
                if (mx >= bx && mx <= bx + colW && my >= by && my <= by + rowH) {
                    this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                    switch (i) {
                        case 0 -> {
                            HudConfig.INSTANCE.musicSourcePreset = "ALL";
                            HudConfig.INSTANCE.pulseSyncIntegration = true;
                        }
                        case 1 -> {
                            HudConfig.INSTANCE.musicSourcePreset = "YANDEX";
                            HudConfig.INSTANCE.pulseSyncIntegration = true;
                            HudConfig.save();
                            if (this.field_22787 != null) {
                                this.field_22787.method_1507((class_437)new YandexMusicSettingsScreen(this));
                            }
                        }
                        case 2 -> {
                            HudConfig.INSTANCE.musicSourcePreset = "SPOTIFY";
                            HudConfig.INSTANCE.pulseSyncIntegration = true;
                        }
                        case 3 -> {
                            HudConfig.INSTANCE.musicSourcePreset = "LOCAL";
                            HudConfig.INSTANCE.pulseSyncIntegration = false;
                        }
                    }
                    HudConfig.save();
                    return true;
                }
            }
            int pillY = gridY + rowH * 2 + gapY + 10;
            int pillH = 26;
            int pillW = cardW - gridPad * 2;
            if (mx >= gridX && mx <= gridX + pillW && my >= pillY && my <= pillY + pillH) {
                if ("YANDEX".equalsIgnoreCase(HudConfig.INSTANCE.musicSourcePreset)) {
                    this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                    if (this.field_22787 != null) {
                        this.field_22787.method_1507((class_437)new YandexMusicSettingsScreen(this));
                    }
                    return true;
                }
            }
        } else if (this.currentStep == 1) {
            for (int i = 0; i < 4; ++i) {
                int col = i % 2;
                int row = i / 2;
                int bx = gridX + col * (colW + gapX);
                int by = gridY + row * (rowH + gapY);
                if (mx >= bx && mx <= bx + colW && my >= by && my <= by + rowH) {
                    this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                    switch (i) {
                        case 0 -> {
                            HudConfig.INSTANCE.subtitlesEnabled = true;
                            HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
                            HudConfig.INSTANCE.subtitlesAnimation = "KARAOKE_BOUNCE";
                            HudConfig.INSTANCE.sprintWordsEnabled = false;
                        }
                        case 1 -> {
                            HudConfig.INSTANCE.subtitlesEnabled = true;
                            HudConfig.INSTANCE.subtitlesMode = "HUD_BOTTOM";
                            HudConfig.INSTANCE.subtitlesAnimation = "KARAOKE_BOUNCE";
                            HudConfig.INSTANCE.sprintWordsEnabled = false;
                        }
                        case 2 -> {
                            HudConfig.INSTANCE.subtitlesEnabled = true;
                            HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
                            HudConfig.INSTANCE.subtitlesAnimation = "ELASTIC_POP";
                            HudConfig.INSTANCE.sprintWordsEnabled = true;
                        }
                        case 3 -> {
                            HudConfig.INSTANCE.subtitlesEnabled = false;
                        }
                    }
                    HudConfig.save();
                    return true;
                }
            }
        } else if (this.currentStep == 2) {
            for (int i = 0; i < 4; ++i) {
                int col = i % 2;
                int row = i / 2;
                int bx = gridX + col * (colW + gapX);
                int by = gridY + row * (rowH + gapY);
                if (mx >= bx && mx <= bx + colW && my >= by && my <= by + rowH) {
                    this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                    switch (i) {
                        case 0 -> {
                            HudConfig.INSTANCE.enabled = true;
                            HudConfig.INSTANCE.hudPositionX = 0.98;
                            HudConfig.INSTANCE.hudPositionY = 0.02;
                            HudConfig.INSTANCE.anchor = "TOP_RIGHT";
                        }
                        case 1 -> {
                            HudConfig.INSTANCE.enabled = true;
                            HudConfig.INSTANCE.hudPositionX = 0.98;
                            HudConfig.INSTANCE.hudPositionY = 0.88;
                            HudConfig.INSTANCE.anchor = "BOTTOM_RIGHT";
                        }
                        case 2 -> {
                            HudConfig.INSTANCE.enabled = true;
                            HudConfig.INSTANCE.hudPositionX = 0.02;
                            HudConfig.INSTANCE.hudPositionY = 0.02;
                            HudConfig.INSTANCE.anchor = "TOP_LEFT";
                        }
                        case 3 -> {
                            HudConfig.INSTANCE.enabled = false;
                        }
                    }
                    HudConfig.save();
                    return true;
                }
            }
            int toggleY = gridY + rowH * 2 + gapY + 10;
            int toggleW = (colW * 2 + gapX - 10) / 2;
            int toggleH = 24;
            if (mx >= gridX && mx <= gridX + toggleW && my >= toggleY && my <= toggleY + toggleH) {
                this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                HudConfig.INSTANCE.hudSize = "LARGE";
                HudConfig.save();
                return true;
            }
            int toggle2X = gridX + toggleW + 10;
            if (mx >= toggle2X && mx <= toggle2X + toggleW && my >= toggleY && my <= toggleY + toggleH) {
                this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                HudConfig.INSTANCE.hudSize = "COMPACT";
                HudConfig.save();
                return true;
            }
        } else if (this.currentStep == 3) {
            int boxY = cardY + 58;
            int boxH = cardH < 270 ? 52 : 68;
            int btnW = (cardW - gridPad * 2 - 10) / 2;
            int actBtnH = 26;
            int action1X = cardX + gridPad;
            int action2X = action1X + btnW + 10;
            int actionY = boxY + boxH + 10;

            if (mx >= action1X && mx <= action1X + btnW && my >= actionY && my <= actionY + actBtnH) {
                this.testActionTime = System.currentTimeMillis();
                this.playSound(class_3417.field_14931, 1.4f, 0.9f);
                this.playSound(class_3417.field_14725, 1.2f, 0.7f);
                if (com.example.yandexmusichud.YandexMusicHudMod.INSTANCE != null && com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getLyricsManager() != null) {
                    com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getLyricsManager().spawnTestBubble();
                }
                for (int i = 0; i < 35; ++i) {
                    float px = (float)action1X + (float)btnW / 2.0f + (this.random.nextFloat() - 0.5f) * 80.0f;
                    float py = (float)actionY + (float)actBtnH / 2.0f + (this.random.nextFloat() - 0.5f) * 40.0f;
                    this.particles.add(new SetupParticle(px, py));
                }
                return true;
            }
            if (mx >= action2X && mx <= action2X + btnW && my >= actionY && my <= actionY + actBtnH) {
                this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                HudConfig.INSTANCE.initialSetupCompleted = true;
                HudConfig.save();
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new KeybindSetupScreen(this));
                }
                return true;
            }

            int advY = actionY + actBtnH + 8;
            int advW = cardW - gridPad * 2;
            int advH = 24;
            if (mx >= action1X && mx <= action1X + advW && my >= advY && my <= advY + advH) {
                this.playSound(class_3417.field_14706, 1.2f, 0.8f);
                HudConfig.INSTANCE.initialSetupCompleted = true;
                HudConfig.save();
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new HudEditScreen(this));
                }
                return true;
            }
        }

        return super.method_25402(click, bl);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        long elapsed = now - this.stepStartTime;
        float slideT = class_3532.method_15363((float)elapsed / 260.0f, 0.0f, 1.0f);
        float ease = (float)(1.0 - Math.pow(1.0 - slideT, 3.0));

        context.method_25294(0, 0, this.field_22789, this.field_22790, 0xF6090C16);
        context.method_25296(0, 0, this.field_22789, 60, 0x6624A1DE, 0);
        context.method_25296(0, this.field_22790 - 60, this.field_22789, this.field_22790, 0, 0x77080B12);

        this.renderParticles(context);

        int cardW = this.getCardW();
        int cardH = this.getCardH();
        int cardX = this.getCardX();
        int cardY = this.getCardY();
        int centerX = cardX + cardW / 2;

        int alphaInt = (int)(ease * 255.0f);
        int bgCol = 0xEE0B1220;
        int borderCol = 0xFF24A1DE;

        MusicHudRenderer.renderRoundedRect(context, cardX - 2, cardY - 2, cardW + 4, cardH + 4, 12, 0x4424A1DE);
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 10, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 10, borderCol, borderCol);
        context.method_25294(cardX + 18, cardY, cardX + cardW - 18, cardY + 1, 0xAA64C5FF);

        class_327 tr = this.field_22793;

        String stepBadge = "✦ ШАГ " + (this.currentStep + 1) + " ИЗ " + TOTAL_STEPS + " ✦";
        context.method_51433(tr, "§e" + stepBadge, cardX + 18, cardY + 13, alphaInt << 24 | 0xFACC15, true);

        int skipW = 95;
        int skipH = 20;
        int skipX = cardX + cardW - skipW - 16;
        int skipY = cardY + 11;
        boolean hoverSkip = mouseX >= skipX && mouseX <= skipX + skipW && mouseY >= skipY && mouseY <= skipY + skipH;
        int skipBg = hoverSkip ? 0xEE331B24 : 0xBB182030;
        int skipBorder = hoverSkip ? 0xFFF87171 : 0x55475569;
        MusicHudRenderer.renderRoundedRect(context, skipX, skipY, skipW, skipH, 4, skipBg);
        MusicHudRenderer.renderRoundedBorder(context, skipX, skipY, skipW, skipH, 4, skipBorder, skipBorder);
        String skipText = hoverSkip ? "§c✕ §fПропустить" : "§7✕ Пропустить";
        int stw = tr.method_1727(skipText);
        context.method_51433(tr, skipText, skipX + (skipW - stw) / 2, skipY + 6, alphaInt << 24 | 0xFFFFFF, false);

        this.renderStepContent(context, cardX, cardY, cardW, cardH, mouseX, mouseY, ease, now);

        int navY = cardY + cardH - 32;
        int btnH = 24;

        if (this.currentStep > 0) {
            int prevW = 95;
            int prevX = cardX + 18;
            boolean hoverPrev = mouseX >= prevX && mouseX <= prevX + prevW && mouseY >= navY && mouseY <= navY + btnH;
            int pBg = hoverPrev ? 0xEE1E3A5A : 0xBB112233;
            int pBorder = hoverPrev ? 0xFF38BDF8 : 0x6624A1DE;
            MusicHudRenderer.renderRoundedRect(context, prevX, navY, prevW, btnH, 5, pBg);
            MusicHudRenderer.renderRoundedBorder(context, prevX, navY, prevW, btnH, 5, pBorder, pBorder);
            String prevText = hoverPrev ? "§b❮ Назад" : "§7❮ Назад";
            int ptw = tr.method_1727(prevText);
            context.method_51433(tr, prevText, prevX + (prevW - ptw) / 2, navY + 8, alphaInt << 24 | 0xFFFFFF, false);
        }

        int dotSpacing = 16;
        int totalDotsW = (TOTAL_STEPS - 1) * dotSpacing;
        int dotStartX = centerX - totalDotsW / 2;
        int dotY = navY + 12;
        for (int i = 0; i < TOTAL_STEPS; ++i) {
            int dx = dotStartX + i * dotSpacing;
            boolean isCur = i == this.currentStep;
            boolean isDone = i < this.currentStep;
            int dotCol = isCur ? 0xFF00FF88 : (isDone ? 0xFF24A1DE : 0x5564748B);
            int dotR = isCur ? 4 : 3;
            if (isCur) {
                MusicHudRenderer.renderRoundedRect(context, dx - dotR - 2, dotY - dotR - 2, dotR * 2 + 4, dotR * 2 + 4, dotR + 2, 0x5500FF88);
            }
            MusicHudRenderer.renderRoundedRect(context, dx - dotR, dotY - dotR, dotR * 2, dotR * 2, dotR, dotCol);
        }

        int nextW = this.currentStep == TOTAL_STEPS - 1 ? 160 : 115;
        int nextX = cardX + cardW - nextW - 18;
        boolean hoverNext = mouseX >= nextX && mouseX <= nextX + nextW && mouseY >= navY && mouseY <= navY + btnH;
        int nBg = hoverNext ? (this.currentStep == TOTAL_STEPS - 1 ? 0xFF14532D : 0xFF15548C) : (this.currentStep == TOTAL_STEPS - 1 ? 0xEE0D543A : 0xEE0E3E66);
        int nBorder = hoverNext ? (this.currentStep == TOTAL_STEPS - 1 ? 0xFF34D399 : 0xFF60A5FA) : (this.currentStep == TOTAL_STEPS - 1 ? 0xFF00FF88 : 0xFF24A1DE);
        MusicHudRenderer.renderRoundedRect(context, nextX, navY, nextW, btnH, 5, nBg);
        MusicHudRenderer.renderRoundedBorder(context, nextX, navY, nextW, btnH, 5, nBorder, nBorder);
        String nextText = this.currentStep == TOTAL_STEPS - 1
                ? (hoverNext ? "§a✔ В игру! ✦" : "§f✔ В игру! ✦")
                : (hoverNext ? "§bДалее ➔" : "§fДалее ➔");
        int ntw = tr.method_1727(nextText);
        context.method_51433(tr, nextText, nextX + (nextW - ntw) / 2, navY + 8, alphaInt << 24 | 0xFFFFFF, true);

        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void renderStepContent(class_332 context, int cardX, int cardY, int cardW, int cardH, int mouseX, int mouseY, float alpha, long now) {
        class_327 tr = this.field_22793;
        int centerX = cardX + cardW / 2;
        int alphaInt = (int)(alpha * 255.0f);
        int gridPad = 18;
        int gridX = cardX + gridPad;
        int gridY = cardY + 60;
        int gapX = 10;
        int gapY = 8;
        int colW = (cardW - gridPad * 2 - gapX) / 2;
        int rowH = cardH < 270 ? Math.max(46, (cardH - 135) / 2) : 62;

        switch (this.currentStep) {
            case 0 -> {
                context.method_51433(tr, "§f🎧 §bОТКУДА ВЫ СЛУШАЕТЕ МУЗЫКУ?", cardX + 18, cardY + 31, alphaInt << 24 | 0xFFFFFF, true);
                context.method_51433(tr, "§7Выберите основной плеер. Мод автоматически подстроится под него.", cardX + 18, cardY + 44, alphaInt << 24 | 0x94A3B8, false);

                String[] titles = new String[]{
                    "🌐 Автоопределение",
                    "🟥 Яндекс Музыка",
                    "🟩 Spotify",
                    "📁 Моя папка"
                };
                String[] subs = new String[]{
                    "Яндекс, Spotify, YouTube, VK, Браузер",
                    "Официальный веб-плеер или приложение",
                    "Клиент Spotify для Windows или веб",
                    "Встроенный плеер MP3/FLAC файлов"
                };
                String[] descs = new String[]{
                    "§e[Рекомендуется] §fМод сам перехватит звук",
                    (HudConfig.INSTANCE.yandexMusicToken != null && !HudConfig.INSTANCE.yandexMusicToken.isEmpty()) ? "§a✔ OAuth токен указан" : "§6[Клик] §fНастроить API токен",
                    "Точнейшая пословная караоке-синхронизация",
                    "Работает автономно без интернета"
                };

                String sourcePreset = HudConfig.INSTANCE.musicSourcePreset == null ? "ALL" : HudConfig.INSTANCE.musicSourcePreset;
                int selectedIdx = switch (sourcePreset.toUpperCase(java.util.Locale.ROOT)) {
                    case "YANDEX" -> 1;
                    case "SPOTIFY" -> 2;
                    case "LOCAL" -> 3;
                    default -> 0;
                };

                for (int i = 0; i < 4; ++i) {
                    int col = i % 2;
                    int row = i / 2;
                    int bx = gridX + col * (colW + gapX);
                    int by = gridY + row * (rowH + gapY);
                    boolean isSel = i == selectedIdx;
                    boolean isHover = mouseX >= bx && mouseX <= bx + colW && mouseY >= by && mouseY <= by + rowH;

                    int cBg = isSel ? 0xEE0E382B : (isHover ? 0xEE1E293B : 0xCC0E1829);
                    int cBorder = isSel ? 0xFF00FF88 : (isHover ? 0xFF38BDF8 : 0x55334155);
                    MusicHudRenderer.renderRoundedRect(context, bx, by, colW, rowH, 6, cBg);
                    MusicHudRenderer.renderRoundedBorder(context, bx, by, colW, rowH, 6, cBorder, cBorder);

                    context.method_51433(tr, titles[i], bx + 8, by + 7, alphaInt << 24 | 0xFFFFFF, isSel);
                    if (isSel) {
                        context.method_51433(tr, "§a✔", bx + colW - 16, by + 7, alphaInt << 24 | 0x00FF88, true);
                    }
                    context.method_51433(tr, "§7" + subs[i], bx + 8, by + 22, alphaInt << 24 | 0x94A3B8, false);
                    context.method_51433(tr, descs[i], bx + 8, by + 37, alphaInt << 24 | 0xCBD5E1, false);
                }

                int pillY = gridY + rowH * 2 + gapY + 10;
                int pillH = 26;
                int pillW = cardW - gridPad * 2;
                boolean isYmPreset = "YANDEX".equalsIgnoreCase(HudConfig.INSTANCE.musicSourcePreset);
                boolean hoverPill = mouseX >= gridX && mouseX <= gridX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
                int pillBg = isYmPreset ? (hoverPill ? 0xEE2A1E08 : 0xEE1E1404) : 0xEE091424;
                int pillBorder = isYmPreset ? (hoverPill ? 0xFFFFAA00 : 0xFFCC8800) : 0x6624A1DE;
                MusicHudRenderer.renderRoundedRect(context, gridX, pillY, pillW, pillH, 5, pillBg);
                MusicHudRenderer.renderRoundedBorder(context, gridX, pillY, pillW, pillH, 5, pillBorder, pillBorder);

                if (isYmPreset) {
                    boolean hasToken = HudConfig.INSTANCE.yandexMusicToken != null && !HudConfig.INSTANCE.yandexMusicToken.isEmpty();
                    String ymBtnText = hasToken ? "§a✔ OAuth токен сохранён §7[Клик для изменения настроек]" : "§6✦ Нажмите сюда для ввода API токена Яндекс Музыки ⚙";
                    context.method_51433(tr, ymBtnText, gridX + 10, pillY + 8, alphaInt << 24 | 0xFFFFFF, true);
                } else {
                    TrackState state = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : TrackState.INACTIVE;
                    if (state != null && state.isActive() && state.isPlaying()) {
                        String trackInfo = "§a● Играет: §f" + state.title() + " §7— " + state.artist() + " §e[" + state.detectPlatformName() + "]";
                        if (tr.method_1727(trackInfo) > pillW - 20) {
                            trackInfo = "§a● Играет: §f" + state.title() + " §7— " + state.artist();
                        }
                        context.method_51433(tr, trackInfo, gridX + 10, pillY + 8, alphaInt << 24 | 0xFFFFFF, false);
                    } else {
                        context.method_51433(tr, "§e○ Включите музыку в плеере или браузере для проверки связи", gridX + 10, pillY + 8, alphaInt << 24 | 0xFACC15, false);
                    }
                }
            }
            case 1 -> {
                context.method_51433(tr, "§f🎤 §bКАК ПОКАЗЫВАТЬ СУБТИТРЫ?", cardX + 18, cardY + 31, alphaInt << 24 | 0xFFFFFF, true);
                context.method_51433(tr, "§7Выберите визуальный стиль. Режим можно быстро переключать клавишей B.", cardX + 18, cardY + 44, alphaInt << 24 | 0x94A3B8, false);

                String[] titles = new String[]{
                    "🔮 3D в игровом мире",
                    "📺 2D на экране",
                    "⚡ Спринт-режим",
                    "🚫 Без субтитров"
                };
                String[] subs = new String[]{
                    "Слова парят над головой в 3D-мире",
                    "Аккуратные субтитры снизу экрана",
                    "Слова вылетают группами на бегу",
                    "Видеть только карточку плеера"
                };
                String[] descs = new String[]{
                    "§e[Стандарт] §fЗолотая караоке-подсветка",
                    "Как в фильмах и клипах, не закрывает мир",
                    "Драйвовый темп для битв и быстрого ритма",
                    "Субтитры скрыты, работает только HUD"
                };

                int selectedIdx;
                if (!HudConfig.INSTANCE.subtitlesEnabled) {
                    selectedIdx = 3;
                } else if ("SPRINT".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || HudConfig.INSTANCE.sprintWordsEnabled) {
                    selectedIdx = 2;
                } else if ("HUD_BOTTOM".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode)) {
                    selectedIdx = 1;
                } else {
                    selectedIdx = 0;
                }

                for (int i = 0; i < 4; ++i) {
                    int col = i % 2;
                    int row = i / 2;
                    int bx = gridX + col * (colW + gapX);
                    int by = gridY + row * (rowH + gapY);
                    boolean isSel = i == selectedIdx;
                    boolean isHover = mouseX >= bx && mouseX <= bx + colW && mouseY >= by && mouseY <= by + rowH;

                    int cBg = isSel ? 0xEE0E382B : (isHover ? 0xEE1E293B : 0xCC0E1829);
                    int cBorder = isSel ? 0xFF00FF88 : (isHover ? 0xFF38BDF8 : 0x55334155);
                    MusicHudRenderer.renderRoundedRect(context, bx, by, colW, rowH, 6, cBg);
                    MusicHudRenderer.renderRoundedBorder(context, bx, by, colW, rowH, 6, cBorder, cBorder);

                    context.method_51433(tr, titles[i], bx + 8, by + 7, alphaInt << 24 | 0xFFFFFF, isSel);
                    if (isSel) {
                        context.method_51433(tr, "§a✔", bx + colW - 16, by + 7, alphaInt << 24 | 0x00FF88, true);
                    }
                    context.method_51433(tr, "§7" + subs[i], bx + 8, by + 22, alphaInt << 24 | 0x94A3B8, false);
                    context.method_51433(tr, descs[i], bx + 8, by + 37, alphaInt << 24 | 0xCBD5E1, false);
                }

                int prevY = gridY + rowH * 2 + gapY + 10;
                int prevH = 26;
                int prevW = cardW - gridPad * 2;
                MusicHudRenderer.renderRoundedRect(context, gridX, prevY, prevW, prevH, 5, 0xEE101828);
                MusicHudRenderer.renderRoundedBorder(context, gridX, prevY, prevW, prevH, 5, 0x889333EA, 0x889333EA);

                String sampleText = switch (selectedIdx) {
                    case 0 -> "§d♪ §fWhen the stars §e★ align in harmony ★ §d♪";
                    case 1 -> "§b📺 §fNever gonna §b[give you up] §fnever gonna let you down";
                    case 2 -> "§e⚡ >> §6Never §f| §6gonna §f| §6give §f<<";
                    default -> "§8(Субтитры выключены — отображается только плеер)";
                };
                context.method_51433(tr, "Предпросмотр: " + sampleText, gridX + 10, prevY + 8, alphaInt << 24 | 0xFFFFFF, true);
            }
            case 2 -> {
                context.method_51433(tr, "§f🎛 §bГДЕ РАСПОЛОЖИТЬ КАРТОЧКУ ПЛЕЕРА?", cardX + 18, cardY + 31, alphaInt << 24 | 0xFFFFFF, true);
                context.method_51433(tr, "§7Выберите угол экрана. В игре можно зажать 'O' и подвинуть мышкой.", cardX + 18, cardY + 44, alphaInt << 24 | 0x94A3B8, false);

                String[] titles = new String[]{
                    "↗ Справа вверху",
                    "↘ Справа внизу",
                    "↖ Слева вверху",
                    "❌ Скрыть карточку"
                };
                String[] subs = new String[]{
                    "Классика, не мешает чату и инвентарю",
                    "Компактно над панелью предметов",
                    "Как на трансляциях и стримах",
                    "Оставить только летающие 3D субтитры"
                };
                String[] descs = new String[]{
                    "§e[Стандарт] §fИдеально для любого экрана",
                    "Удобный обзор верхней части мира",
                    "Все важные виджеты собраны слева",
                    "Экран чист, субтитры появляются в мире"
                };

                int selectedIdx;
                if (!HudConfig.INSTANCE.enabled) {
                    selectedIdx = 3;
                } else if (HudConfig.INSTANCE.hudPositionX < 0.5 && HudConfig.INSTANCE.hudPositionY < 0.5) {
                    selectedIdx = 2;
                } else if (HudConfig.INSTANCE.hudPositionY > 0.5) {
                    selectedIdx = 1;
                } else {
                    selectedIdx = 0;
                }

                for (int i = 0; i < 4; ++i) {
                    int col = i % 2;
                    int row = i / 2;
                    int bx = gridX + col * (colW + gapX);
                    int by = gridY + row * (rowH + gapY);
                    boolean isSel = i == selectedIdx;
                    boolean isHover = mouseX >= bx && mouseX <= bx + colW && mouseY >= by && mouseY <= by + rowH;

                    int cBg = isSel ? 0xEE0E382B : (isHover ? 0xEE1E293B : 0xCC0E1829);
                    int cBorder = isSel ? 0xFF00FF88 : (isHover ? 0xFF38BDF8 : 0x55334155);
                    MusicHudRenderer.renderRoundedRect(context, bx, by, colW, rowH, 6, cBg);
                    MusicHudRenderer.renderRoundedBorder(context, bx, by, colW, rowH, 6, cBorder, cBorder);

                    context.method_51433(tr, titles[i], bx + 8, by + 7, alphaInt << 24 | 0xFFFFFF, isSel);
                    if (isSel) {
                        context.method_51433(tr, "§a✔", bx + colW - 16, by + 7, alphaInt << 24 | 0x00FF88, true);
                    }
                    context.method_51433(tr, "§7" + subs[i], bx + 8, by + 22, alphaInt << 24 | 0x94A3B8, false);
                    context.method_51433(tr, descs[i], bx + 8, by + 37, alphaInt << 24 | 0xCBD5E1, false);
                }

                int toggleY = gridY + rowH * 2 + gapY + 10;
                int toggleW = (colW * 2 + gapX - 10) / 2;
                int toggleH = 24;
                boolean isLarge = !"COMPACT".equalsIgnoreCase(HudConfig.INSTANCE.hudSize);

                int bg1 = isLarge ? 0xEE0E382B : 0xBB112233;
                int bdr1 = isLarge ? 0xFF00FF88 : 0x6624A1DE;
                MusicHudRenderer.renderRoundedRect(context, gridX, toggleY, toggleW, toggleH, 5, bg1);
                MusicHudRenderer.renderRoundedBorder(context, gridX, toggleY, toggleW, toggleH, 5, bdr1, bdr1);
                String t1 = isLarge ? "§a✔ Большой плеер с обложкой" : "§7Большой плеер с обложкой";
                context.method_51433(tr, t1, gridX + (toggleW - tr.method_1727(t1)) / 2, toggleY + 8, alphaInt << 24 | 0xFFFFFF, isLarge);

                int toggle2X = gridX + toggleW + 10;
                int bg2 = !isLarge ? 0xEE0E382B : 0xBB112233;
                int bdr2 = !isLarge ? 0xFF00FF88 : 0x6624A1DE;
                MusicHudRenderer.renderRoundedRect(context, toggle2X, toggleY, toggleW, toggleH, 5, bg2);
                MusicHudRenderer.renderRoundedBorder(context, toggle2X, toggleY, toggleW, toggleH, 5, bdr2, bdr2);
                String t2 = !isLarge ? "§a✔ Компактный мини-плеер" : "§7Компактный мини-плеер";
                context.method_51433(tr, t2, toggle2X + (toggleW - tr.method_1727(t2)) / 2, toggleY + 8, alphaInt << 24 | 0xFFFFFF, !isLarge);
            }
            case 3 -> {
                context.method_51433(tr, "§a🎉 §fВСЁ ГОТОВО К ИГРЕ!", cardX + 18, cardY + 31, alphaInt << 24 | 0xFFFFFF, true);
                context.method_51433(tr, "§7Настройки сохранены. Запомните простые клавиши быстрого доступа:", cardX + 18, cardY + 44, alphaInt << 24 | 0x94A3B8, false);

                int boxY = cardY + 58;
                int boxW = cardW - gridPad * 2;
                int boxH = cardH < 270 ? 52 : 68;
                MusicHudRenderer.renderRoundedRect(context, gridX, boxY, boxW, boxH, 6, 0xEE0B1424);
                MusicHudRenderer.renderRoundedBorder(context, gridX, boxY, boxW, boxH, 6, 0x8824A1DE, 0x8824A1DE);

                int col1X = gridX + 12;
                int col2X = gridX + boxW / 2 + 6;
                int lineGap = cardH < 270 ? 13 : 18;
                int firstLineY = boxY + (cardH < 270 ? 8 : 12);

                context.method_51433(tr, "§e[ O ] §f— Меню настроек и редактор HUD", col1X, firstLineY, alphaInt << 24 | 0xFFFFFF, false);
                context.method_51433(tr, "§e[ X ] §f— Пауза / Возобновление трека", col1X, firstLineY + lineGap, alphaInt << 24 | 0xFFFFFF, false);
                context.method_51433(tr, "§e[ B ] §f— Режим субтитров (3D / 2D / Выкл)", col1X, firstLineY + lineGap * 2, alphaInt << 24 | 0xFFFFFF, false);

                context.method_51433(tr, "§b[ [ ] §f/ §b[ ] ] §f— Подгонка синхронизации", col2X, firstLineY, alphaInt << 24 | 0xFFFFFF, false);
                context.method_51433(tr, "§b[ H ] §f— Скрыть плеер для скриншотов", col2X, firstLineY + lineGap, alphaInt << 24 | 0xFFFFFF, false);
                context.method_51433(tr, "§b[ M ] §f— Меню локальных треков", col2X, firstLineY + lineGap * 2, alphaInt << 24 | 0xFFFFFF, false);

                int btnW = (cardW - gridPad * 2 - 10) / 2;
                int btnH = 26;
                int action1X = cardX + gridPad;
                int action2X = action1X + btnW + 10;
                int actionY = boxY + boxH + 10;

                boolean hoverA1 = mouseX >= action1X && mouseX <= action1X + btnW && mouseY >= actionY && mouseY <= actionY + btnH;
                int a1Bg = hoverA1 ? 0xFF15548C : 0xEE0E3E66;
                int a1Border = hoverA1 ? 0xFF60A5FA : 0xFF24A1DE;
                MusicHudRenderer.renderRoundedRect(context, action1X, actionY, btnW, btnH, 5, a1Bg);
                MusicHudRenderer.renderRoundedBorder(context, action1X, actionY, btnW, btnH, 5, a1Border, a1Border);
                String a1Text = hoverA1 ? "§b🔊 Проверить звук и караоке" : "§f🔊 Проверить звук и караоке";
                int a1tw = tr.method_1727(a1Text);
                context.method_51433(tr, a1Text, action1X + (btnW - a1tw) / 2, actionY + 8, alphaInt << 24 | 0xFFFFFF, false);

                boolean hoverA2 = mouseX >= action2X && mouseX <= action2X + btnW && mouseY >= actionY && mouseY <= actionY + btnH;
                int a2Bg = hoverA2 ? 0xFF15548C : 0xEE0E3E66;
                int a2Border = hoverA2 ? 0xFF60A5FA : 0xFF24A1DE;
                MusicHudRenderer.renderRoundedRect(context, action2X, actionY, btnW, btnH, 5, a2Bg);
                MusicHudRenderer.renderRoundedBorder(context, action2X, actionY, btnW, btnH, 5, a2Border, a2Border);
                String a2Text = hoverA2 ? "§b⌨ Настроить горячие клавиши" : "§f⌨ Настроить горячие клавиши";
                int a2tw = tr.method_1727(a2Text);
                context.method_51433(tr, a2Text, action2X + (btnW - a2tw) / 2, actionY + 8, alphaInt << 24 | 0xFFFFFF, false);

                int advY = actionY + btnH + 8;
                int advW = cardW - gridPad * 2;
                int advH = 24;
                boolean hoverAdv = mouseX >= action1X && mouseX <= action1X + advW && mouseY >= advY && mouseY <= advY + advH;
                int advBg = hoverAdv ? 0xEE2A3B52 : 0xBB141E2E;
                int advBorder = hoverAdv ? 0xFFFACC15 : 0x55475569;
                MusicHudRenderer.renderRoundedRect(context, action1X, advY, advW, advH, 5, advBg);
                MusicHudRenderer.renderRoundedBorder(context, action1X, advY, advW, advH, 5, advBorder, advBorder);
                String advText = hoverAdv ? "§e⚙ Открыть расширенный редактор HUD ➔" : "§7⚙ Открыть расширенный редактор HUD ➔";
                int atw = tr.method_1727(advText);
                context.method_51433(tr, advText, action1X + (advW - atw) / 2, advY + 7, alphaInt << 24 | 0xFFFFFF, false);

                if (now - this.testActionTime < 5000L && this.testActionTime > 0L) {
                    int statusY = Math.min(cardY + cardH - 58, advY + advH + 6);
                    int statusH = 22;
                    MusicHudRenderer.renderRoundedRect(context, action1X, statusY, advW, statusH, 4, 0xEE092C22);
                    MusicHudRenderer.renderRoundedBorder(context, action1X, statusY, advW, statusH, 4, 0xFF00FF88, 0xFF00FF88);
                    boolean inWorld = this.field_22787 != null && this.field_22787.field_1687 != null;
                    boolean is3D = "WORLD_3D".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) || "WORLD_BLOCK".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode);
                    String testStatus = (!inWorld && is3D) ? "§a✔ Звук работает! §e(3D-субтитры появятся при входе в мир)" : "§a✔ Звуковой движок и караоке-система работают безупречно!";
                    int tsw = tr.method_1727(testStatus);
                    context.method_51433(tr, testStatus, action1X + (advW - tsw) / 2, statusY + 7, alphaInt << 24 | 0x00FF88, true);
                }
            }
        }
    }

    private void renderParticles(class_332 context) {
        class_327 tr = this.field_22793;
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            SetupParticle p = this.particles.get(i);
            p.update();
            if (p.isDead()) {
                this.particles.remove(i);
                continue;
            }
            int alpha = (int)(p.alpha * 190.0f);
            if (alpha > 0) {
                context.method_51433(tr, p.symbol, (int)p.x, (int)p.y, alpha << 24 | p.color, false);
            }
        }
    }

    private static class SetupParticle {
        float x;
        float y;
        float vy;
        float alpha = 1.0f;
        int color;
        String symbol;
        int maxAge;
        int age = 0;

        SetupParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vy = -0.3f - (float)Math.random() * 0.5f;
            this.maxAge = 60 + (int)(Math.random() * 50.0);
            String[] syms = new String[]{"♪", "♫", "✦", "★", "•"};
            this.symbol = syms[(int)(Math.random() * (double)syms.length)];
            int[] cols = new int[]{0x24A1DE, 0x00FF88, 0xFACC15, 0xC084FC, 0x38BDF8};
            this.color = cols[(int)(Math.random() * (double)cols.length)];
        }

        void update() {
            this.y += this.vy;
            this.age++;
            this.alpha = 1.0f - (float)this.age / (float)this.maxAge;
        }

        boolean isDead() {
            return this.age >= this.maxAge;
        }
    }
}
