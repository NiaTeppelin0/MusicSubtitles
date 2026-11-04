package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
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

public class ModTutorialScreen extends class_437 {
    private final class_437 parent;
    private final Random random = new Random();
    private int currentSlide = 0;
    private static final int TOTAL_SLIDES = 4;
    private long slideStartTime = System.currentTimeMillis();
    private final List<TutParticle> particles = new ArrayList<>();

    public ModTutorialScreen(class_437 parent) {
        super(class_2561.method_43470("Обучение Music Subtitles"));
        this.parent = parent;
    }

    protected void method_25426() {
        super.method_25426();
        this.slideStartTime = System.currentTimeMillis();
        this.playSound(class_3417.field_26980, 1.2f, 0.8f);
        this.particles.clear();
        for (int i = 0; i < 30; ++i) {
            float px = this.random.nextFloat() * (float)this.field_22789;
            float py = this.random.nextFloat() * (float)this.field_22790;
            this.particles.add(new TutParticle(px, py));
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

    private void finishTutorial() {
        HudConfig.INSTANCE.hasSeenTutorial = true;
        HudConfig.save();
        this.playSound(class_3417.field_14931, 1.8f, 0.6f);
        this.playSound(class_3417.field_14706, 1.2f, 0.8f);
        if (this.field_22787 != null) {
            if (this.parent != null) {
                this.field_22787.method_1507(this.parent);
            } else if (this.field_22787.field_1687 != null) {
                this.field_22787.method_1507(null);
            } else {
                this.field_22787.method_1507(new class_442());
            }
        }
    }

    private void setSlide(int slide) {
        if (slide < 0) slide = 0;
        if (slide >= TOTAL_SLIDES) {
            this.finishTutorial();
            return;
        }
        if (slide != this.currentSlide) {
            this.currentSlide = slide;
            this.slideStartTime = System.currentTimeMillis();
            this.playSound((class_6880<class_3414>)class_3417.field_14793, 1.3f, 0.8f);
            this.playSound(class_3417.field_15015.comp_349(), 1.4f, 0.7f);
            for (int i = 0; i < 20; ++i) {
                float px = (float)this.field_22789 / 2.0f + (this.random.nextFloat() - 0.5f) * 100.0f;
                float py = (float)this.field_22790 / 2.0f + (this.random.nextFloat() - 0.5f) * 60.0f;
                this.particles.add(new TutParticle(px, py));
            }
        }
    }

    public boolean method_25404(class_11908 input) {
        int key = input.comp_4795();
        if (key == 256) {
            this.finishTutorial();
            return true;
        }
        if (key == 262 || key == 257 || key == 32) {
            this.setSlide(this.currentSlide + 1);
            return true;
        }
        if (key == 263) {
            if (this.currentSlide > 0) {
                this.setSlide(this.currentSlide - 1);
            }
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int centerX = this.field_22789 / 2;

        int cardW = Math.min(460, this.field_22789 - 24);
        int cardH = Math.min(270, this.field_22790 - 32);
        int cardX = (this.field_22789 - cardW) / 2;
        long elapsed = System.currentTimeMillis() - this.slideStartTime;
        float slideT = class_3532.method_15363((float)elapsed / 320.0f, 0.0f, 1.0f);
        float ease = (float)(1.0 - Math.pow(1.0 - slideT, 3.0));
        float slideOffsetY = (1.0f - ease) * 16.0f;
        int cardY = (int)((float)((this.field_22790 - cardH) / 2) + slideOffsetY);

        int skipW = 85;
        int skipH = 18;
        int skipX = cardX + cardW - skipW - 14;
        int skipY = cardY + 10;
        if (mx >= skipX && mx <= skipX + skipW && my >= skipY && my <= skipY + skipH) {
            this.finishTutorial();
            return true;
        }

        int navY = cardY + cardH - 30;
        int btnH = 22;

        int prevW = 100;
        int prevX = cardX + 16;
        if (this.currentSlide > 0 && mx >= prevX && mx <= prevX + prevW && my >= navY && my <= navY + btnH) {
            this.setSlide(this.currentSlide - 1);
            return true;
        }

        int nextW = this.currentSlide == TOTAL_SLIDES - 1 ? 130 : 110;
        int nextX = cardX + cardW - nextW - 16;
        if (mx >= nextX && mx <= nextX + nextW && my >= navY && my <= navY + btnH) {
            this.setSlide(this.currentSlide + 1);
            return true;
        }

        int dotSpacing = 16;
        int totalDotsW = (TOTAL_SLIDES - 1) * dotSpacing;
        int dotStartX = centerX - totalDotsW / 2;
        int dotY = navY + 11;
        for (int i = 0; i < TOTAL_SLIDES; ++i) {
            int dx = dotStartX + i * dotSpacing;
            if (mx >= dx - 6 && mx <= dx + 6 && my >= dotY - 6 && my <= dotY + 6) {
                this.setSlide(i);
                return true;
            }
        }

        return super.method_25402(click, bl);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        long elapsed = now - this.slideStartTime;
        float slideT = class_3532.method_15363((float)elapsed / 320.0f, 0.0f, 1.0f);
        float ease = (float)(1.0 - Math.pow(1.0 - slideT, 3.0));

        context.method_25294(0, 0, this.field_22789, this.field_22790, 0xEE0B0F1A);
        context.method_25296(0, 0, this.field_22789, 45, 0x5524A1DE, 0);
        context.method_25296(0, this.field_22790 - 45, this.field_22789, this.field_22790, 0, 0x66080B12);

        this.renderParticles(context);

        int centerX = this.field_22789 / 2;
        int cardW = Math.min(460, this.field_22789 - 24);
        int cardH = Math.min(270, this.field_22790 - 32);
        int cardX = (this.field_22789 - cardW) / 2;
        int baseCardY = (this.field_22790 - cardH) / 2;
        float slideOffsetY = (1.0f - ease) * 16.0f;
        int cardY = (int)((float)baseCardY + slideOffsetY);

        int alphaInt = (int)(ease * 255.0f);
        int bgCol = (int)(ease * 242.0f) << 24 | 0x0E1424;
        int borderCol = (int)(ease * 140.0f) << 24 | 0x24A1DE;

        MusicHudRenderer.renderRoundedRect(context, cardX - 2, cardY - 2, cardW + 4, cardH + 4, 12, (int)(ease * 50.0f) << 24 | 0x24A1DE);
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 10, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 10, borderCol, borderCol);
        context.method_25294(cardX + 16, cardY, cardX + cardW - 16, cardY + 1, (int)(ease * 170.0f) << 24 | 0x64C5FF);

        class_327 tr = this.field_22793;

        String stepBadge = "✦ ШАГ " + (this.currentSlide + 1) + " ИЗ " + TOTAL_SLIDES + " ✦";
        context.method_51433(tr, "§e" + stepBadge, cardX + 16, cardY + 12, alphaInt << 24 | 0xFFFFAA, false);

        int skipW = 85;
        int skipH = 18;
        int skipX = cardX + cardW - skipW - 14;
        int skipY = cardY + 10;
        boolean hoverSkip = mouseX >= skipX && mouseX <= skipX + skipW && mouseY >= skipY && mouseY <= skipY + skipH;
        int skipBg = hoverSkip ? 0x66442222 : 0x33333333;
        int skipBorder = hoverSkip ? 0xFFFFAA33 : 0x44FFFFFF;
        MusicHudRenderer.renderRoundedRect(context, skipX, skipY, skipW, skipH, 4, skipBg);
        MusicHudRenderer.renderRoundedBorder(context, skipX, skipY, skipW, skipH, 4, skipBorder, skipBorder);
        String skipText = hoverSkip ? "§6Пропустить ✕" : "§7Пропустить ✕";
        int stw = tr.method_1727(skipText);
        context.method_51433(tr, skipText, skipX + (skipW - stw) / 2, skipY + 5, alphaInt << 24 | 0xFFFFFF, true);

        this.renderSlideContent(context, cardX, cardY, cardW, cardH, ease, now);

        int navY = cardY + cardH - 30;
        int btnH = 22;

        if (this.currentSlide > 0) {
            int prevW = 100;
            int prevX = cardX + 16;
            boolean hoverPrev = mouseX >= prevX && mouseX <= prevX + prevW && mouseY >= navY && mouseY <= navY + btnH;
            int pBg = hoverPrev ? 0xDD1B2A4A : 0x88121B30;
            int pBorder = hoverPrev ? 0xFF24A1DE : 0x5524A1DE;
            MusicHudRenderer.renderRoundedRect(context, prevX, navY, prevW, btnH, 5, pBg);
            MusicHudRenderer.renderRoundedBorder(context, prevX, navY, prevW, btnH, 5, pBorder, pBorder);
            String prevText = hoverPrev ? "§b◄ Назад" : "§7◄ Назад";
            int ptw = tr.method_1727(prevText);
            context.method_51433(tr, prevText, prevX + (prevW - ptw) / 2, navY + 7, alphaInt << 24 | 0xFFFFFF, true);
        }

        int dotSpacing = 16;
        int totalDotsW = (TOTAL_SLIDES - 1) * dotSpacing;
        int dotStartX = centerX - totalDotsW / 2;
        int dotY = navY + 11;
        for (int i = 0; i < TOTAL_SLIDES; ++i) {
            int dx = dotStartX + i * dotSpacing;
            boolean active = i == this.currentSlide;
            int dotColor = active ? 0xFF24A1DE : 0x44FFFFFF;
            int dotR = active ? 4 : 2;
            if (active) {
                MusicHudRenderer.renderRoundedRect(context, dx - dotR - 2, dotY - dotR - 2, dotR * 2 + 4, dotR * 2 + 4, dotR + 2, 0x4424A1DE);
            }
            MusicHudRenderer.renderRoundedRect(context, dx - dotR, dotY - dotR, dotR * 2, dotR * 2, dotR, dotColor);
        }

        int nextW = this.currentSlide == TOTAL_SLIDES - 1 ? 130 : 110;
        int nextX = cardX + cardW - nextW - 16;
        boolean hoverNext = mouseX >= nextX && mouseX <= nextX + nextW && mouseY >= navY && mouseY <= navY + btnH;
        int nBg = hoverNext ? (this.currentSlide == TOTAL_SLIDES - 1 ? 0xDD184D35 : 0xDD1A4F7C) : (this.currentSlide == TOTAL_SLIDES - 1 ? 0x99103322 : 0x88113350);
        int nBorder = hoverNext ? (this.currentSlide == TOTAL_SLIDES - 1 ? 0xFF00FF88 : 0xFF24A1DE) : (this.currentSlide == TOTAL_SLIDES - 1 ? 0x8800AA55 : 0x8824A1DE);
        MusicHudRenderer.renderRoundedRect(context, nextX, navY, nextW, btnH, 5, nBg);
        MusicHudRenderer.renderRoundedBorder(context, nextX, navY, nextW, btnH, 5, nBorder, nBorder);
        String nextText = this.currentSlide == TOTAL_SLIDES - 1
                ? (hoverNext ? "§aВ игру! ✦" : "§fВ игру! ✦")
                : (hoverNext ? "§bДалее ➔" : "§fДалее ➔");
        int ntw = tr.method_1727(nextText);
        context.method_51433(tr, nextText, nextX + (nextW - ntw) / 2, navY + 7, alphaInt << 24 | 0xFFFFFF, true);

        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void renderSlideContent(class_332 context, int cardX, int cardY, int cardW, int cardH, float alpha, long now) {
        class_327 tr = this.field_22793;
        int alphaInt = (int)(alpha * 255.0f);

        switch (this.currentSlide) {
            case 0 -> {

                int topY = cardY + 32;
                context.method_51433(tr, "§f🌐 §bОПРЕДЕЛЕНИЕ МУЗЫКИ ИЗ БРАУЗЕРА", cardX + 16, topY, alphaInt << 24 | 0xFFFFFF, true);

                int pillW = cardW - 32;
                int pillH = 34;
                int pillX = cardX + 16;
                int pillY = topY + 16;
                MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 6, 0xBB0D1B2E);
                MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 6, 0x8824A1DE, 0x8824A1DE);

                int barX = pillX + 10;
                for (int b = 0; b < 5; ++b) {
                    float bHeight = 4.0f + (float)Math.abs(Math.sin((double)now * 0.006 + (double)b * 0.8)) * 14.0f;
                    int by = pillY + 24 - (int)bHeight;
                    context.method_25294(barX + b * 5, by, barX + b * 5 + 3, pillY + 24, 0xFF00FF88);
                }

                context.method_51433(tr, "§a▶ §fNia — Cyber Melody §7[Яндекс Музыка]", pillX + 42, pillY + 7, alphaInt << 24 | 0xFFFFFF, false);
                context.method_51433(tr, "§8Синхронизировано в реальном времени • 100% точность", pillX + 42, pillY + 19, alphaInt << 24 | 0x888899, false);

                int curY = pillY + pillH + 12;
                context.method_51433(tr, "§e✦ §fВключите музыку в §bЯндекс Музыке§f, §cYouTube§f, §aSpotify§f или браузере.", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§e✦ §fМод сам определяет трек, скачивает слова и вычисляет ритм.", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§7• Работает в §fChrome§7, §eOpera GX§7, §9Firefox§7, §fEdge§7 и приложении Windows.", cardX + 16, curY, alphaInt << 24 | 0xAAAAAA, false);
                curY += 12;
                context.method_51433(tr, "§7• При задержке Bluetooth-наушников жмите §e[ §7или §e] §7для точной синхронизации.", cardX + 16, curY, alphaInt << 24 | 0xAAAAAA, false);
            }
            case 1 -> {

                int topY = cardY + 32;
                context.method_51433(tr, "§f🎛 §bВИДЖЕТ ПЛЕЕРА HUD И КАСТОМИЗАЦИЯ", cardX + 16, topY, alphaInt << 24 | 0xFFFFFF, true);

                int hudW = cardW - 32;
                int hudH = 40;
                int hudX = cardX + 16;
                int hudY = topY + 16;
                MusicHudRenderer.renderRoundedRect(context, hudX, hudY, hudW, hudH, 6, 0xCC111726);
                MusicHudRenderer.renderRoundedBorder(context, hudX, hudY, hudW, hudH, 6, 0x88FFAA00, 0x33000000);

                MusicHudRenderer.renderRoundedRect(context, hudX + 6, hudY + 6, 28, 28, 4, 0xFF24A1DE);
                context.method_51433(tr, "♫", hudX + 16, hudY + 15, -1, true);

                context.method_51433(tr, "§fStarry Night §7— §eVirtual Symphony", hudX + 40, hudY + 8, alphaInt << 24 | 0xFFFFFF, false);

                int progW = hudW - 50;
                MusicHudRenderer.renderRoundedRect(context, hudX + 40, hudY + 22, progW, 4, 2, 0x55FFFFFF);
                float progPulse = (float)Math.abs(Math.sin((double)now * 0.0015));
                MusicHudRenderer.renderRoundedRect(context, hudX + 40, hudY + 22, (int)((float)progW * progPulse), 4, 2, 0xFFFFAA00);

                int curY = hudY + hudH + 12;
                context.method_51433(tr, "§a✔ §fКрасивый виджет плеера прямо поверх игры с обложкой трека.", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§b⚙ §fКлавиша §eO §f— удобный редактор: двигайте HUD мышкой и меняйте темы.", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§e👁 §fКлавиша §eH §f— мгновенно скрывает/показывает плеер для красивых скриншотов.", cardX + 16, curY, alphaInt << 24 | 0xAAAAAA, false);
                curY += 12;
                context.method_51433(tr, "§7• Поддерживаются кастомные картинки фона, палитры цветов и пульсация баса.", cardX + 16, curY, alphaInt << 24 | 0xAAAAAA, false);
            }
            case 2 -> {

                int topY = cardY + 32;
                context.method_51433(tr, "§f🎤 §bКИНЕМАТОГРАФИЧЕСКИЕ 3D-СУБТИТРЫ", cardX + 16, topY, alphaInt << 24 | 0xFFFFFF, true);

                int subW = cardW - 32;
                int subH = 38;
                int subX = cardX + 16;
                int subY = topY + 16;
                MusicHudRenderer.renderRoundedRect(context, subX, subY, subW, subH, 6, 0xCC131024);
                MusicHudRenderer.renderRoundedBorder(context, subX, subY, subW, subH, 6, 0x88DD55FF, 0x33000000);

                float bounce = (float)Math.sin((double)now * 0.005) * 2.0f;
                int lineY = subY + 13 + (int)bounce;
                context.method_51433(tr, "§d♪ §fWhen the stars §ealign in §6[HARMONY] §d♪", subX + 20, lineY, alphaInt << 24 | 0xFFFFFF, true);

                int curY = subY + subH + 12;
                context.method_51433(tr, "§d✦ §fТекст песни парит прямо в 3D-мире перед вами со световыми эффектами!", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§e★ §fКараоке-подсветка: слова загораются золотым точно в ритм голоса.", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§b🔀 §fКлавиша §eB §f— переключение: 3D в мире / 3D на блоках / 2D-субтитры.", cardX + 16, curY, alphaInt << 24 | 0xAAAAAA, false);
                curY += 12;
                context.method_51433(tr, "§7• Слова видны даже сквозь стены или в темноте по вашему выбору.", cardX + 16, curY, alphaInt << 24 | 0xAAAAAA, false);
            }
            case 3 -> {

                int topY = cardY + 32;
                context.method_51433(tr, "§f⚡ §bУПРАВЛЕНИЕ, КРУГОВОЕ МЕНЮ И СТРИМЫ", cardX + 16, topY, alphaInt << 24 | 0xFFFFFF, true);

                int boxW = cardW - 32;
                int boxH = 44;
                int boxX = cardX + 16;
                int boxY = topY + 14;
                MusicHudRenderer.renderRoundedRect(context, boxX, boxY, boxW, boxH, 6, 0xCC112028);
                MusicHudRenderer.renderRoundedBorder(context, boxX, boxY, boxW, boxH, 6, 0x8800FFCC, 0x33000000);

                int col1 = boxX + 12;
                int col2 = boxX + boxW / 2 + 10;
                context.method_51433(tr, "§eR §7— §fКруговое меню", col1, boxY + 7, -1, false);
                context.method_51433(tr, "§eX §7— §fПауза / Плей", col1, boxY + 22, -1, false);
                context.method_51433(tr, "§eC / Z §7— §fСлед. / Пред. трек", col2, boxY + 7, -1, false);
                context.method_51433(tr, "§eK / J §7— §fИзбранное и плейлист", col2, boxY + 22, -1, false);

                int curY = boxY + boxH + 10;
                context.method_51433(tr, "§6★ §fУправляйте музыкой прямо во время бега или битвы!", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§d💜 §fДля стримеров: уведомления Twitch и DonationAlerts прямо в HUD.", cardX + 16, curY, alphaInt << 24 | 0xEEEEEE, false);
                curY += 13;
                context.method_51433(tr, "§b✈ §fОфициальный Telegram с обновлениями: §e@TG_Nia_Teppelin0", cardX + 16, curY, alphaInt << 24 | 0x24A1DE, false);
                curY += 12;
                context.method_51433(tr, "§a✔ §fВсё готово! Нажмите кнопку ниже и наслаждайтесь музыкой!", cardX + 16, curY, alphaInt << 24 | 0x00FF88, false);
            }
        }
    }

    private void renderParticles(class_332 context) {
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            TutParticle p = this.particles.get(i);
            p.update(this.field_22789, this.field_22790);
            int pAlpha = (int)(p.alpha * 180.0f);
            context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, pAlpha << 24 | p.color, false);
        }
    }

    private static class TutParticle {
        float x, y, vy;
        String symbol;
        int color;
        float alpha;

        TutParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vy = -0.3f - (float)Math.random() * 0.6f;
            String[] syms = new String[]{"♪", "♫", "✦", "★", "·"};
            this.symbol = syms[(int)(Math.random() * syms.length)];
            int[] colors = new int[]{0x24A1DE, 0x00FF88, 0xFFAA00, 0xDD55FF, 0xFFFFFF};
            this.color = colors[(int)(Math.random() * colors.length)];
            this.alpha = 0.2f + (float)Math.random() * 0.6f;
        }

        void update(int screenW, int screenH) {
            this.y += this.vy;
            if (this.y < -10) {
                this.y = screenH + 10;
                this.x = (float)Math.random() * (float)screenW;
            }
        }
    }
}

