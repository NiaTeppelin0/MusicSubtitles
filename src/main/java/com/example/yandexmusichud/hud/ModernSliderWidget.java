package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.hud.MusicHudRenderer;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_357;

public class ModernSliderWidget
extends class_357 {
    private final String label;
    private final String cleanLabel;
    private final Formatter formatter;
    private final Consumer<Double> onValueChange;
    private Runnable onRelease;
    private float hoverProgress = 0.0f;
    private long lastRenderTime = 0L;
    private double cachedVal = Double.NaN;
    private String cachedValText = "";
    private class_2561 cachedValComponent = class_2561.method_43473();
    private int cachedMaxLblW = -1;
    private String cachedDrawLbl = "";

    public ModernSliderWidget(int x, int y, int width, int height, String label, double initialValue, Formatter formatter, Consumer<Double> onValueChange) {
        super(x, y, width, height, (class_2561)class_2561.method_43473(), class_3532.method_15350((double)initialValue, (double)0.0, (double)1.0));
        this.label = label;
        String cl = label != null ? label.trim() : "";
        if (cl.endsWith(":")) {
            cl = cl.substring(0, cl.length() - 1).trim();
        }
        this.cleanLabel = cl;
        this.formatter = formatter;
        this.onValueChange = onValueChange;
        this.method_25346();
    }

    private String getFormattedValText() {
        double v = this.field_22753;
        if (Double.compare(v, this.cachedVal) != 0) {
            String formatted = this.formatter != null ? this.formatter.format(v) : String.format(Locale.ROOT, "%.2f", v);
            formatted = formatted != null ? formatted.trim() : "";
            if (formatted.startsWith(":")) {
                formatted = formatted.substring(1).trim();
            }
            this.cachedVal = v;
            this.cachedValText = formatted;
            this.cachedValComponent = class_2561.method_43470(formatted);
        }
        return this.cachedValText;
    }

    public ModernSliderWidget setOnRelease(Runnable onRelease) {
        this.onRelease = onRelease;
        return this;
    }

    public void setValue(double val) {
        this.field_22753 = class_3532.method_15350(val, 0.0, 1.0);
        this.method_25346();
    }

    public boolean method_25406(net.minecraft.class_11909 click) {
        if (this.onRelease != null) {
            this.onRelease.run();
        }
        return super.method_25406(click);
    }

    protected void method_25346() {
        String formatted = this.formatter != null ? this.formatter.format(this.field_22753) : String.format(Locale.ROOT, "%.2f", this.field_22753);
        this.method_25355((class_2561)class_2561.method_43470((String)(this.label + formatted)));
    }

    protected void method_25344() {
        if (this.onValueChange != null) {
            this.onValueChange.accept(this.field_22753);
        }
    }

    public void method_48579(class_332 context, int mouseX, int mouseY, float delta) {
        class_310 mc = class_310.method_1551();
        class_327 textRenderer = mc.field_1772;
        long now = System.currentTimeMillis();
        float dt = this.lastRenderTime > 0L ? Math.min(0.1f, (float)(now - this.lastRenderTime) / 1000.0f) : 0.016f;
        this.lastRenderTime = now;
        boolean activeOrHovered = this.method_49606() || this.method_25370();
        float target = activeOrHovered ? 1.0f : 0.0f;
        this.hoverProgress += (target - this.hoverProgress) * Math.min(1.0f, dt * 14.0f);
        int sx = this.method_46426();
        int sy = this.method_46427();
        int sw = this.method_25368();
        int sh = this.method_25364();
        int r = 5;
        if (this.hoverProgress > 0.04f) {
            int haloAlpha = (int)(this.hoverProgress * 30.0f);
            MusicHudRenderer.renderRoundedRect(context, sx - 1, sy - 1, sw + 2, sh + 2, r + 1, haloAlpha << 24 | 0x38BDF8);
        }
        int cardBg = ModernSliderWidget.lerpColor(0xEE0C0F17, 0xF4141B28, this.hoverProgress);
        MusicHudRenderer.renderRoundedRect(context, sx, sy, sw, sh, r, cardBg);
        context.method_25294(sx + 4, sy + 1, sx + sw - 4, sy + 2, 0x16FFFFFF);

        int idleBorderAlpha = 64;
        int borderAlpha = Math.min(240, (int)((float)idleBorderAlpha + this.hoverProgress * 155.0f));
        int borderRgb = ModernSliderWidget.lerpColor(0xFF334155, 0xFF38BDF8, this.hoverProgress) & 0xFFFFFF;
        int borderColTop = borderAlpha << 24 | borderRgb;
        int borderColBot = (borderAlpha * 2 / 3) << 24 | borderRgb;
        MusicHudRenderer.renderRoundedBorder(context, sx, sy, sw, sh, r, borderColTop, borderColBot);

        int trackPad = 7;
        int trackW = Math.max(8, sw - trackPad * 2);
        int trackH = 4;
        int trackX = sx + trackPad;
        int trackY = sy + sh - 6;
        MusicHudRenderer.renderRoundedRect(context, trackX, trackY, trackW, trackH, 2, 0xDD1E2636);

        int filledWidth = (int)Math.round((double)trackW * this.field_22753);
        if (filledWidth > 0) {
            int fillCol = ModernSliderWidget.lerpColor(0xEE0EA5E9, 0xFF38BDF8, this.hoverProgress);
            MusicHudRenderer.renderRoundedRect(context, trackX, trackY, filledWidth, trackH, 2, fillCol);
        }

        int knobX = trackX + filledWidth;
        int knobY = trackY + trackH / 2;
        int knobR = this.hoverProgress > 0.25f ? 4 : 3;
        if (this.hoverProgress > 0.05f) {
            int glowA = (int)(this.hoverProgress * 85.0f);
            MusicHudRenderer.renderRoundedRect(context, knobX - knobR - 2, knobY - knobR - 2, (knobR + 2) * 2, (knobR + 2) * 2, knobR + 2, glowA << 24 | 0x38BDF8);
        }
        int knobCol = ModernSliderWidget.lerpColor(0xFF38BDF8, 0xFFFFFFFF, this.hoverProgress);
        MusicHudRenderer.renderRoundedRect(context, knobX - knobR, knobY - knobR, knobR * 2, knobR * 2, knobR, knobCol);
        MusicHudRenderer.renderRoundedBorder(context, knobX - knobR, knobY - knobR, knobR * 2, knobR * 2, knobR, 0xFFFFFFFF, 0x8838BDF8);

        String valText = this.getFormattedValText();
        int textY = sy + 3;
        if (!this.cleanLabel.isEmpty() && sw >= 64) {
            int valW = textRenderer.method_1727(valText);
            int maxLblW = Math.max(16, sw - valW - 14);
            if (maxLblW != this.cachedMaxLblW) {
                this.cachedMaxLblW = maxLblW;
                this.cachedDrawLbl = textRenderer.method_1727(this.cleanLabel) > maxLblW ? textRenderer.method_27523(this.cleanLabel, maxLblW) : this.cleanLabel;
            }
            int lblColor = activeOrHovered ? 0xFFF8FAFC : 0xFFCBD5E1;
            int valColor = activeOrHovered ? 0xFF38BDF8 : 0xFF94A3B8;
            context.method_51433(textRenderer, this.cachedDrawLbl, sx + 7, textY, lblColor, false);
            context.method_51433(textRenderer, valText, sx + sw - valW - 7, textY, valColor, false);
        } else {
            int textColor = activeOrHovered ? -1 : -1710619;
            context.method_27534(textRenderer, this.method_25369(), sx + sw / 2, textY, textColor);
        }

        if (this.hoverProgress > 0.05f) {
            int bubbleTextW = textRenderer.method_1727(valText);
            int bubbleW = bubbleTextW + 10;
            int bubbleH = 13;
            int bubbleX = Math.max(sx, Math.min(sx + sw - bubbleW, knobX - bubbleW / 2));
            int bubbleY = sy - 14 - (int)(this.hoverProgress * 2.0f);
            int bAlpha = (int)(this.hoverProgress * 245.0f);
            if (bAlpha > 5) {
                MusicHudRenderer.renderRoundedRect(context, bubbleX - 1, bubbleY - 1, bubbleW + 2, bubbleH + 2, 5, (bAlpha / 4) << 24 | 0x38BDF8);
                MusicHudRenderer.renderRoundedRect(context, bubbleX, bubbleY, bubbleW, bubbleH, 4, bAlpha << 24 | 0x0A0D14);
                MusicHudRenderer.renderRoundedBorder(context, bubbleX, bubbleY, bubbleW, bubbleH, 4, (int)(this.hoverProgress * 200.0f) << 24 | 0x38BDF8, (int)(this.hoverProgress * 120.0f) << 24 | 0x334155);
                context.method_27534(textRenderer, this.cachedValComponent, bubbleX + bubbleW / 2, bubbleY + 2, bAlpha << 24 | 0xFFFFFF);
            }
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        if (t <= 0.0f) {
            return c1;
        }
        if (t >= 1.0f) {
            return c2;
        }
        int a1 = c1 >> 24 & 0xFF;
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = c2 >> 24 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int)((float)a1 + (float)(a2 - a1) * t);
        int r = (int)((float)r1 + (float)(r2 - r1) * t);
        int g = (int)((float)g1 + (float)(g2 - g1) * t);
        int b = (int)((float)b1 + (float)(b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static interface Formatter {
        public String format(double var1);
    }
}
