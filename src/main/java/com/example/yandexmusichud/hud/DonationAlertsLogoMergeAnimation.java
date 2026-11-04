package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.hud.MusicHudRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;

public class DonationAlertsLogoMergeAnimation {
    private static final class_2960 ICON_TEXTURE = class_2960.method_60655("yandexmusichud", "icon.png");
    private static final class_2960 DA_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/donationalerts_icon.png");

    private final long startTime;
    private boolean completed = false;
    private boolean soundAppearPlayed = false;
    private boolean soundMergePlayed = false;
    private boolean soundArrivalPlayed = false;

    private final List<AnimParticle> particles = new ArrayList<>();
    private final List<ShockwaveRing> shockwaves = new ArrayList<>();
    private final Random random = new Random();
    private long lastRenderTime = 0L;

    private static class AnimParticle {
        float x, y, vx, vy;
        int color;
        int maxAge, age;
        float size;
        String symbol;

        AnimParticle(float x, float y, float vx, float vy, int color, int maxAge, float size, String symbol) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.maxAge = maxAge;
            this.age = 0;
            this.size = size;
            this.symbol = symbol;
        }

        boolean tick(float dt60) {
            this.x += this.vx * dt60;
            this.y += this.vy * dt60;
            this.vx *= (float)Math.pow(0.93, dt60);
            this.vy *= (float)Math.pow(0.93, dt60);
            this.age = (int)((float)this.age + dt60);
            return this.age >= this.maxAge;
        }

        boolean tick() {
            return this.tick(1.0f);
        }
    }

    private static class ShockwaveRing {
        float cx, cy;
        float currentR;
        float maxR;
        int color;
        float alpha;

        ShockwaveRing(float cx, float cy, float maxR, int color) {
            this.cx = cx;
            this.cy = cy;
            this.currentR = 4.0f;
            this.maxR = maxR;
            this.color = color;
            this.alpha = 1.0f;
        }

        boolean tick(float dt60) {
            this.currentR += (this.maxR - this.currentR) * (1.0f - (float)Math.pow(0.84, dt60));
            this.alpha *= (float)Math.pow(0.88, dt60);
            return this.alpha < 0.03f || this.currentR >= this.maxR * 0.98f;
        }

        boolean tick() {
            return this.tick(1.0f);
        }
    }

    public DonationAlertsLogoMergeAnimation() {
        this.startTime = System.currentTimeMillis();
    }

    public void skip() {
        this.completed = true;
    }

    public boolean isCompleted() {
        return this.completed || (System.currentTimeMillis() - this.startTime) > 2100L;
    }

    public float getSettingsPanelAlpha() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (this.completed) return 1.0f;
        if (elapsed < 1650L) return 0.0f;
        float progress = (float)(elapsed - 1650L) / 450.0f;
        return easeOutCubic(class_3532.method_15363(progress, 0.0f, 1.0f));
    }

    public float getSettingsPanelSlideY() {
        float alpha = this.getSettingsPanelAlpha();
        return (1.0f - alpha) * 18.0f;
    }

    public void render(class_332 context, int screenW, int screenH, int targetCornerX, int targetCornerY) {
        long now = System.currentTimeMillis();
        float dt = this.lastRenderTime > 0L ? Math.min(0.08f, (float)(now - this.lastRenderTime) / 1000.0f) : 0.016f;
        this.lastRenderTime = now;
        float dt60 = Math.min(3.0f, dt * 60.0f);

        long elapsed = now - this.startTime;
        if (this.completed) elapsed = 2500L;

        class_310 mc = class_310.method_1551();
        class_327 tr = mc.field_1772;

        int cx = screenW / 2;
        int cy = screenH / 2 - 10;

        if (elapsed < 1750L) {
            float dimFade = elapsed < 200L ? (float)elapsed / 200.0f : (elapsed > 1400L ? 1.0f - (float)(elapsed - 1400L) / 350.0f : 1.0f);
            int dimAlpha = (int)(dimFade * 155.0f);
            context.method_25294(0, 0, screenW, screenH, dimAlpha << 24 | 0x0E0803);
        }

        this.shockwaves.removeIf(s -> s.tick(dt60));
        for (ShockwaveRing s : this.shockwaves) {
            int a = (int)(s.alpha * 220.0f);
            int col = a << 24 | (s.color & 0xFFFFFF);
            int r = (int)s.currentR;
            context.method_25294((int)s.cx - r, (int)s.cy - 1, (int)s.cx + r, (int)s.cy + 1, col);
            context.method_25294((int)s.cx - 1, (int)s.cy - r, (int)s.cx + 1, (int)s.cy + r, col);
            int diag = (int)((float)r * 0.707f);
            context.method_25294((int)s.cx - diag, (int)s.cy - diag, (int)s.cx - diag + 2, (int)s.cy - diag + 2, col);
            context.method_25294((int)s.cx + diag, (int)s.cy - diag, (int)s.cx + diag + 2, (int)s.cy - diag + 2, col);
            context.method_25294((int)s.cx - diag, (int)s.cy + diag, (int)s.cx - diag + 2, (int)s.cy + diag + 2, col);
            context.method_25294((int)s.cx + diag, (int)s.cy + diag, (int)s.cx + diag + 2, (int)s.cy + diag + 2, col);
        }

        this.particles.removeIf(p -> p.tick(dt60));
        for (AnimParticle p : this.particles) {
            float life = 1.0f - (float)p.age / (float)p.maxAge;
            int alpha = (int)(life * 255.0f);
            int col = alpha << 24 | (p.color & 0xFFFFFF);
            if (p.symbol != null) {
                context.method_51433(tr, p.symbol, (int)p.x, (int)p.y, col, false);
            } else {
                int s = Math.max(1, (int)(p.size * life));
                MusicHudRenderer.renderRoundedRect(context, (int)p.x, (int)p.y, s, s, s / 2, col);
            }
        }

        if (elapsed < 700L) {
            if (!this.soundAppearPlayed) {
                this.soundAppearPlayed = true;
                playSound(class_3417.field_26980, 1.25f, 0.8f);
            }

            float p = (float)elapsed / 700.0f;
            float popEase = easeOutBack(p);
            float alpha = class_3532.method_15363(p * 1.5f, 0.0f, 1.0f);
            int alphaInt = (int)(alpha * 255.0f);

            int leftCenterX = cx - 64;

            int rightCenterX = cx + 64;

            renderMusicSubtitlesBadge(context, tr, leftCenterX, cy, popEase, alphaInt);

            renderDonationAlertsBadge(context, tr, rightCenterX, cy, popEase, alphaInt);

            if (this.random.nextFloat() < 0.35f && p > 0.3f) {
                float spawnX = (float)leftCenterX + this.random.nextFloat() * 128.0f;
                float spawnY = (float)cy + (this.random.nextFloat() - 0.5f) * 16.0f;
                this.particles.add(new AnimParticle(spawnX, spawnY, (this.random.nextFloat() - 0.5f) * 0.8f, -0.6f - this.random.nextFloat() * 0.8f,
                        this.random.nextBoolean() ? 0x00E5FF : 0xFA7218, 20, 2.0f, null));
            }
        }

        else if (elapsed < 1100L) {
            float phaseTime = (float)(elapsed - 700L) / 400.0f;

            if (phaseTime < 0.55f) {

                float moveP = phaseTime / 0.55f;
                float moveEase = easeInCubic(moveP);

                int leftCenterX = (int)((float)(cx - 64) + 64.0f * moveEase);
                int rightCenterX = (int)((float)(cx + 64) - 64.0f * moveEase);

                float scale = 1.0f + 0.15f * moveEase;
                int alphaInt = 255;

                renderMusicSubtitlesBadge(context, tr, leftCenterX, cy, scale, alphaInt);
                renderDonationAlertsBadge(context, tr, rightCenterX, cy, scale, alphaInt);
            } else {

                float burstP = (phaseTime - 0.55f) / 0.45f;

                if (!this.soundMergePlayed) {
                    this.soundMergePlayed = true;
                    playSound(class_3417.field_43154, 1.15f, 0.9f);
                    playSound((class_3414)class_3417.field_14793.comp_349(), 1.1f, 0.85f);

                    this.shockwaves.add(new ShockwaveRing(cx, cy, 95.0f, 0xFA7218));
                    this.shockwaves.add(new ShockwaveRing(cx, cy, 60.0f, 0xFFCC00));

                    for (int i = 0; i < 48; ++i) {
                        float ang = (float)(this.random.nextFloat() * Math.PI * 2.0);
                        float spd = 1.8f + this.random.nextFloat() * 5.2f;
                        int col = switch (i % 3) {
                            case 0 -> 0xFA7218;
                            case 1 -> 0xFFD700;
                            default -> 0xFF8822;
                        };
                        String sym = switch (i % 6) {
                            case 0 -> "★";
                            case 1 -> "✦";
                            case 2 -> "₽";
                            case 3 -> "$";
                            case 4 -> "🪙";
                            default -> null;
                        };
                        this.particles.add(new AnimParticle(cx, cy, (float)Math.cos(ang) * spd, (float)Math.sin(ang) * spd, col, 28, 3.5f, sym));
                    }
                }

                float remainScale = 1.25f - 0.15f * burstP;
                float flashPulse = (float)Math.sin(burstP * Math.PI);
                int glowCol = (int)(flashPulse * 190.0f) << 24 | 0xFFA044;

                MusicHudRenderer.renderRoundedRect(context, cx - 24, cy - 24, 48, 48, 24, glowCol);
                renderDonationAlertsBadge(context, tr, cx, cy, remainScale, 255);
            }
        }

        else if (elapsed < 1700L) {
            float p = (float)(elapsed - 1100L) / 600.0f;
            float ease = easeInOutCubic(p);

            float p0x = cx;
            float p0y = cy;
            float p1x = (float)cx * 0.35f;
            float p1y = (float)cy * 0.25f;
            float p2x = targetCornerX + 16;
            float p2y = targetCornerY + 14;

            float curX = (1.0f - ease) * (1.0f - ease) * p0x + 2.0f * (1.0f - ease) * ease * p1x + ease * ease * p2x;
            float curY = (1.0f - ease) * (1.0f - ease) * p0y + 2.0f * (1.0f - ease) * ease * p1y + ease * ease * p2y;

            float scale = 1.25f - 0.25f * ease;

            if (this.random.nextFloat() < 0.75f) {
                int pCol = this.random.nextBoolean() ? 0xFA7218 : 0xFFD700;
                String pSym = this.random.nextInt(3) == 0 ? "✦" : (this.random.nextInt(5) == 0 ? "₽" : null);
                this.particles.add(new AnimParticle(curX, curY, (this.random.nextFloat() - 0.5f) * 1.0f, (this.random.nextFloat() - 0.5f) * 1.0f,
                        pCol, 22, 2.8f, pSym));
            }

            renderDonationAlertsBadge(context, tr, (int)curX, (int)curY, scale, 255);
        }

        else {
            if (!this.soundArrivalPlayed) {
                this.soundArrivalPlayed = true;
                playSound(class_3417.field_26980, 1.45f, 0.7f);
            }
            this.completed = true;
            renderParkedCornerBadge(context, tr, targetCornerX, targetCornerY, now);
        }
    }

    private static void renderMusicSubtitlesBadge(class_332 context, class_327 tr, int x, int y, float scale, int alphaInt) {
        if (alphaInt <= 2) return;
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)x, (float)y);
        context.method_51448().scale(scale, scale);

        int size = 34;
        int half = size / 2;

        MusicHudRenderer.renderRoundedRect(context, -half - 3, -half - 3, size + 6, size + 6, half + 2, (int)(alphaInt * 0.4f) << 24 | 0x00E5FF);

        MusicHudRenderer.renderRoundedRect(context, -half, -half, size, size, 8, (int)(alphaInt * 0.9f) << 24 | 0x0C1B2E);
        MusicHudRenderer.renderRoundedBorder(context, -half, -half, size, size, 8, (int)(alphaInt * 0.8f) << 24 | 0x00E5FF, 0x44000000);

        try {
            int iconPad = 5;
            int texSize = size - iconPad * 2;
            context.method_25291(class_10799.field_56883, ICON_TEXTURE, -half + iconPad, -half + iconPad, 0.0f, 0.0f, texSize, texSize, texSize, texSize, alphaInt << 24 | 0xFFFFFF);
        } catch (Throwable t) {
            String note = "§b♪";
            context.method_51433(tr, note, -tr.method_1727(note) / 2, -5, -1, true);
        }

        String label = "§b§lMUSIC §e§lSUBTITLES";
        int lw = tr.method_1727(label);
        context.method_51433(tr, label, -lw / 2, half + 6, alphaInt << 24 | 0xFFFFFF, true);

        context.method_51448().popMatrix();
    }

    private static void renderDonationAlertsBadge(class_332 context, class_327 tr, int x, int y, float scale, int alphaInt) {
        if (alphaInt <= 2) return;
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)x, (float)y);
        context.method_51448().scale(scale, scale);

        int size = 34;
        int half = size / 2;

        MusicHudRenderer.renderRoundedRect(context, -half - 3, -half - 3, size + 6, size + 6, half + 2, (int)(alphaInt * 0.55f) << 24 | 0xFA7218);

        MusicHudRenderer.renderRoundedRect(context, -half, -half, size, size, 8, (int)(alphaInt * 0.95f) << 24 | 0x221008);
        MusicHudRenderer.renderRoundedBorder(context, -half, -half, size, size, 8, (int)(alphaInt * 0.92f) << 24 | 0xFF8822, 0x44000000);

        int iconPad = 5;
        int texSize = size - iconPad * 2;
        try {
            context.method_25291(class_10799.field_56883, DA_ICON, -half + iconPad, -half + iconPad, 0.0f, 0.0f, texSize, texSize, texSize, texSize, alphaInt << 24 | 0xFFFFFF);
        } catch (Throwable t) {
            String glyph = "§6§lDA";
            context.method_51433(tr, glyph, -tr.method_1727(glyph) / 2, -4, -1, true);
        }

        String label = "§6§lDONATION §f§lALERTS";
        int lw = tr.method_1727(label);
        context.method_51433(tr, label, -lw / 2, half + 6, alphaInt << 24 | 0xFFFFFF, true);

        context.method_51448().popMatrix();
    }

    public static void renderParkedCornerBadge(class_332 context, class_327 textRenderer, int cornerX, int cornerY, long now) {
        int iconSize = 24;

        float pulse = (float)(Math.sin((double)now * 0.005) * 0.5 + 0.5);
        int glowAlpha = 50 + (int)(pulse * 70.0f);

        MusicHudRenderer.renderRoundedRect(context, cornerX - 2, cornerY - 2, iconSize + 4, iconSize + 4, 6, glowAlpha << 24 | 0xFA7218);
        MusicHudRenderer.renderRoundedRect(context, cornerX, cornerY, iconSize, iconSize, 5, 0xFF221008);
        MusicHudRenderer.renderRoundedBorder(context, cornerX, cornerY, iconSize, iconSize, 5, 0xFFFA7218, 0x44000000);

        int pad = 4;
        int texSize = iconSize - pad * 2;
        try {
            context.method_25291(class_10799.field_56883, DA_ICON, cornerX + pad, cornerY + pad, 0.0f, 0.0f, texSize, texSize, texSize, texSize, 0xFFFFFFFF);
        } catch (Throwable t) {
            String glyph = "§6§lD!";
            context.method_51433(textRenderer, glyph, cornerX + iconSize / 2 - textRenderer.method_1727(glyph) / 2, cornerY + 4, -1, true);
        }

        int dotColor = 0x00FF88;
        MusicHudRenderer.renderRoundedRect(context, cornerX + iconSize - 5, cornerY + iconSize - 5, 5, 5, 2, 0xFF000000 | dotColor);
    }

    private static void playSound(class_3414 sound, float pitch, float volume) {
        try {
            class_310.method_1551().method_1483().method_4873((class_1113)class_1109.method_4757(sound, pitch, volume));
        } catch (Throwable ignored) {}
    }

    private static float easeOutCubic(float x) {
        return 1.0f - (float)Math.pow(1.0f - x, 3.0);
    }

    private static float easeInCubic(float x) {
        return x * x * x;
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float)Math.pow(x - 1.0f, 3.0) + c1 * (float)Math.pow(x - 1.0f, 2.0);
    }

    private static float easeInOutCubic(float x) {
        return x < 0.5f ? 4.0f * x * x * x : 1.0f - (float)Math.pow(-2.0f * x + 2.0f, 3.0) / 2.0f;
    }
}

