package com.example.yandexmusichud.hud;

import net.minecraft.class_10799;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_6382;

public class ModernButtonWidget
extends class_339 {
    private final PressAction onPress;
    private boolean isPrimary = false;
    private int customBgColor = 0;
    private long lastClickTime = 0L;
    private float hoverProgress = 0.0f;
    private float toggleAnim = -1.0f;
    private long lastRenderTime = 0L;
    private float clickX = -1.0f;
    private float clickY = -1.0f;
    private class_2960 iconTexture = null;
    private int iconSize = 0;

    public ModernButtonWidget(int x, int y, int width, int height, class_2561 message, PressAction onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public ModernButtonWidget setIconTexture(class_2960 texture, int size) {
        this.iconTexture = texture;
        this.iconSize = size;
        return this;
    }

    public ModernButtonWidget setPrimary(boolean primary) {
        this.isPrimary = primary;
        return this;
    }

    public ModernButtonWidget setCustomBgColor(int argb) {
        this.customBgColor = argb;
        return this;
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        if (this.field_22763 && this.field_22764 && this.method_49606()) {
            this.lastClickTime = System.currentTimeMillis();
            this.clickX = (float)click.comp_4798();
            this.clickY = (float)click.comp_4799();
            this.method_25354(class_310.method_1551().method_1483());
            if (this.onPress != null) {
                this.onPress.onPress(this);
            }
            return true;
        }
        return false;
    }

    public void method_48579(class_332 context, int mouseX, int mouseY, float delta) {
        class_310 mc = class_310.method_1551();
        class_327 textRenderer = mc.field_1772;
        long now = System.currentTimeMillis();
        float dt = this.lastRenderTime > 0L ? Math.min(0.1f, (float)(now - this.lastRenderTime) / 1000.0f) : 0.016f;
        this.lastRenderTime = now;
        boolean isTargetHovered = this.method_49606() && this.field_22763;
        float target = isTargetHovered ? 1.0f : 0.0f;
        this.hoverProgress += (target - this.hoverProgress) * Math.min(1.0f, dt * 14.0f);
        long timeSinceClick = now - this.lastClickTime;
        float pressCompression = 0.0f;
        if (timeSinceClick < 160L) {
            float p = (float)timeSinceClick / 160.0f;
            pressCompression = 0.035f * (1.0f - p);
        }
        float easedHover = ModernButtonWidget.easeOutCubic(this.hoverProgress);
        float totalScale = 1.0f + 0.018f * easedHover - pressCompression;
        float liftY = -0.8f * easedHover;
        context.method_51448().pushMatrix();
        float cx = (float)this.method_46426() + (float)this.method_25368() * 0.5f;
        float cy = (float)this.method_46427() + (float)this.method_25364() * 0.5f;
        context.method_51448().translate(cx, cy + liftY);
        context.method_51448().scale(totalScale, totalScale);
        context.method_51448().translate(-cx, -cy);
        int r = 5;
        int bx = this.method_46426();
        int by = this.method_46427();
        int bw = this.method_25368();
        int bh = this.method_25364();
        String rawMsg = this.method_25369() != null ? this.method_25369().getString() : "";
        boolean hasOnToken = rawMsg.contains("§aВКЛ") || rawMsg.contains("ВКЛ") || rawMsg.contains("§a●");
        boolean hasOffToken = rawMsg.contains("§cВЫКЛ") || rawMsg.contains("§7ВЫКЛ") || rawMsg.contains("ВЫКЛ") || rawMsg.contains("§c○");
        boolean isPillToggle = (hasOnToken || hasOffToken) && rawMsg.contains(":") && bw >= 68 && bh >= 15;
        boolean isToggleOn = hasOnToken || rawMsg.contains("§aВсегда") || rawMsg.contains("§aСкрывать") || rawMsg.contains("§a1-2");
        boolean isToggleOff = hasOffToken;

        if (isPillToggle) {
            float targetToggle = hasOnToken ? 1.0f : 0.0f;
            if (this.toggleAnim < 0.0f) {
                this.toggleAnim = targetToggle;
            } else {
                this.toggleAnim += (targetToggle - this.toggleAnim) * Math.min(1.0f, dt * 16.0f);
            }
            int idleBg = ModernButtonWidget.lerpColor(0xEE0C0F17, 0xEE0E1520, this.toggleAnim);
            int hovBg = ModernButtonWidget.lerpColor(0xF6141B28, 0xF6142230, this.toggleAnim);
            int cardBg = ModernButtonWidget.lerpColor(idleBg, hovBg, this.hoverProgress);
            if (this.field_22763 && this.hoverProgress > 0.04f) {
                int haloAlpha = (int)(this.hoverProgress * 32.0f);
                int haloRgb = ModernButtonWidget.lerpColor(0xFF38BDF8, 0xFF10B981, this.toggleAnim) & 0xFFFFFF;
                MusicHudRenderer.renderRoundedRect(context, bx - 1, by - 1, bw + 2, bh + 2, r + 1, haloAlpha << 24 | haloRgb);
            }
            MusicHudRenderer.renderRoundedRect(context, bx, by, bw, bh, r, cardBg);
            context.method_25294(bx + 4, by + 1, bx + bw - 4, by + 2, 0x18FFFFFF);

            int barH = Math.max(6, bh - 8);
            int barY = by + (bh - barH) / 2;
            int barCol = ModernButtonWidget.lerpColor(0x66475569, 0xFF10B981, this.toggleAnim);
            MusicHudRenderer.renderRoundedRect(context, bx + 3, barY, 2, barH, 1, barCol);

            int idleBorder = ModernButtonWidget.lerpColor(0x38334155, 0x6610B981, this.toggleAnim);
            int hovBorder = ModernButtonWidget.lerpColor(0xAA38BDF8, 0xCC34D399, this.toggleAnim);
            int borderColTop = ModernButtonWidget.lerpColor(idleBorder, hovBorder, this.hoverProgress);
            int borderAlphaBot = ((borderColTop >>> 24) & 0xFF) * 2 / 3;
            int borderColBot = borderAlphaBot << 24 | (borderColTop & 0xFFFFFF);
            MusicHudRenderer.renderRoundedBorder(context, bx, by, bw, bh, r, borderColTop, borderColBot);

            int pillW = 22;
            int pillH = 11;
            int pillX = bx + bw - pillW - 5;
            int pillY = by + (bh - pillH) / 2;
            int pillTrackBg = ModernButtonWidget.lerpColor(0xFF1A202C, 0xFF10B981, this.toggleAnim);
            int pillTrackBorder = ModernButtonWidget.lerpColor(0x55475569, 0xAA34D399, this.toggleAnim);
            MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 5, pillTrackBg);
            MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 5, pillTrackBorder, pillTrackBorder);

            int knobSize = 7;
            int knobX = pillX + 2 + Math.round(this.toggleAnim * (float)(pillW - knobSize - 4));
            int knobY = pillY + 2;
            MusicHudRenderer.renderRoundedRect(context, knobX, knobY + 1, knobSize, knobSize, 3, 0x55000000);
            MusicHudRenderer.renderRoundedRect(context, knobX, knobY, knobSize, knobSize, 3, 0xFFFFFFFF);

            int colonIdx = rawMsg.lastIndexOf(':');
            String cleanLabel = colonIdx > 0 ? rawMsg.substring(0, colonIdx).trim() : rawMsg;
            int maxLabelW = Math.max(20, pillX - (bx + 8) - 4);
            if (textRenderer.method_1727(cleanLabel) > maxLabelW) {
                cleanLabel = textRenderer.method_27523(cleanLabel, maxLabelW);
            }
            int baseTextCol = ModernButtonWidget.lerpColor(0xFF94A3B8, 0xFFF1F5F9, this.toggleAnim);
            int finalTextCol = ModernButtonWidget.lerpColor(baseTextCol, 0xFFFFFFFF, this.hoverProgress);
            context.method_51433(textRenderer, cleanLabel, bx + 8, by + (bh - 8) / 2, finalTextCol, false);
            context.method_51448().popMatrix();
            return;
        }

        int normalBg;
        int hoverBg;
        int baseBorderRgb = 0x334155;
        int hoverBorderRgb = 0x38BDF8;
        int accentBarCol = 0;
        if (this.customBgColor != 0) {
            int col = this.customBgColor;
            int a = col >>> 24 & 0xFF;
            if (a <= 0x66) {
                col = (col & 0x00FFFFFF) | 0xDD000000;
            }
            normalBg = col;
            int hr = Math.min(255, (normalBg >> 16 & 0xFF) + 22);
            int hg = Math.min(255, (normalBg >> 8 & 0xFF) + 22);
            int hb = Math.min(255, (normalBg & 0xFF) + 22);
            int ha = Math.min(255, (normalBg >>> 24 & 0xFF) + 20);
            hoverBg = ha << 24 | hr << 16 | hg << 8 | hb;
            if (isToggleOn) {
                baseBorderRgb = 0x10B981;
                hoverBorderRgb = 0x34D399;
            } else if (isToggleOff) {
                baseBorderRgb = 0xEF4444;
                hoverBorderRgb = 0xF87171;
            }
        } else if (!this.field_22763) {
            normalBg = 0x77090C12;
            hoverBg = 0x77090C12;
        } else if (this.isPrimary) {
            normalBg = 0xF2132032;
            hoverBg = 0xFF1A2B42;
            baseBorderRgb = 0x38BDF8;
            hoverBorderRgb = 0x7DD3FC;
        } else if (isToggleOn) {
            normalBg = 0xEE0D221D;
            hoverBg = 0xF413322A;
            baseBorderRgb = 0x10B981;
            hoverBorderRgb = 0x34D399;
            accentBarCol = 0xCC10B981;
        } else if (isToggleOff) {
            normalBg = 0xEE191117;
            hoverBg = 0xF4251822;
            baseBorderRgb = 0x475569;
            hoverBorderRgb = 0xF43F5E;
            accentBarCol = 0x88F43F5E;
        } else {
            normalBg = 0xEE0C0F17;
            hoverBg = 0xF4151C28;
            baseBorderRgb = 0x334155;
            hoverBorderRgb = 0x38BDF8;
            if (bw >= 48) {
                accentBarCol = 0x6638BDF8;
            }
        }
        if (this.field_22763 && (this.hoverProgress > 0.04f || this.isPrimary)) {
            int haloAlpha = (int)((this.isPrimary ? 20.0f : 0.0f) + this.hoverProgress * 34.0f);
            int haloCol = haloAlpha << 24 | hoverBorderRgb;
            MusicHudRenderer.renderRoundedRect(context, bx - 1, by - 1, bw + 2, bh + 2, r + 1, haloCol);
        }
        int bg = ModernButtonWidget.lerpColor(normalBg, hoverBg, this.hoverProgress);
        MusicHudRenderer.renderRoundedRect(context, bx, by, bw, bh, r, bg);
        if (this.field_22763 && bw > 14 && bh >= 14) {
            int topShineAlpha = (int)((this.isPrimary ? 48.0f : 22.0f) + this.hoverProgress * 36.0f);
            context.method_25294(bx + 4, by + 1, bx + bw - 4, by + 2, topShineAlpha << 24 | 0xFFFFFF);
            context.method_25294(bx + 4, by + bh - 2, bx + bw - 4, by + bh - 1, 0x44000000);
        }
        if (accentBarCol != 0 && bw >= 48 && bh >= 16) {
            int barH = Math.max(6, bh - 8);
            int barY = by + (bh - barH) / 2;
            MusicHudRenderer.renderRoundedRect(context, bx + 3, barY, 2, barH, 1, accentBarCol);
        }
        if (this.field_22763) {
            int idleBorderAlpha = this.isPrimary ? 175 : (isToggleOn ? 135 : 78);
            int borderAlpha = Math.min(255, (int)((float)idleBorderAlpha + this.hoverProgress * (235 - idleBorderAlpha)));
            int borderRgb = ModernButtonWidget.lerpColor(0xFF000000 | baseBorderRgb, 0xFF000000 | hoverBorderRgb, this.hoverProgress) & 0xFFFFFF;
            int borderColTop = borderAlpha << 24 | borderRgb;
            int borderColBot = (borderAlpha * 2 / 3) << 24 | borderRgb;
            MusicHudRenderer.renderRoundedBorder(context, bx, by, bw, bh, r, borderColTop, borderColBot);
        } else {
            MusicHudRenderer.renderRoundedBorder(context, bx, by, bw, bh, r, 0x22334155, 0x221E293B);
        }
        if (timeSinceClick < 350L && this.clickX >= 0.0f) {
            float p = (float)timeSinceClick / 350.0f;
            float maxR = (float)Math.max(this.method_25368(), this.method_25364()) * 0.9f;
            float currentR = maxR * ModernButtonWidget.easeOutCubic(p);
            int rippleAlpha = (int)((1.0f - p) * 65.0f);
            if (rippleAlpha > 0) {
                int rippleColor = rippleAlpha << 24 | 0xE5FF;
                context.method_44379(this.method_46426(), this.method_46427(), this.method_46426() + this.method_25368(), this.method_46427() + this.method_25364());
                MusicHudRenderer.renderRoundedRect(context, (int)(this.clickX - currentR), (int)(this.clickY - currentR), (int)(currentR * 2.0f), (int)(currentR * 2.0f), (int)currentR, rippleColor);
                context.method_44380();
            }
        }
        int normalText = !this.field_22763 ? -7829368 : (this.isPrimary ? -1 : -2039584);
        int hoverText = !this.field_22763 ? -7829368 : -1;
        int textColor = ModernButtonWidget.lerpColor(normalText, hoverText, this.hoverProgress);
        if (this.iconTexture != null && this.iconSize > 0) {
            int ix = this.method_46426() + (this.method_25368() - this.iconSize) / 2;
            int iy = this.method_46427() + (this.method_25364() - this.iconSize) / 2;
            try {
                int iconAlpha = !this.field_22763 ? 0x88 : 0xFF;
                context.method_25291(class_10799.field_56883, this.iconTexture, ix, iy, 0.0f, 0.0f, this.iconSize, this.iconSize, this.iconSize, this.iconSize, iconAlpha << 24 | 0xFFFFFF);
            } catch (Throwable t) {
                context.method_27534(textRenderer, this.method_25369(), this.method_46426() + this.method_25368() / 2, this.method_46427() + (this.method_25364() - 8) / 2, textColor);
            }
        } else {
            context.method_27534(textRenderer, this.method_25369(), this.method_46426() + this.method_25368() / 2, this.method_46427() + (this.method_25364() - 8) / 2, textColor);
        }
        context.method_51448().popMatrix();
    }

    private static float easeOutCubic(float t) {
        float f = t - 1.0f;
        return f * f * f + 1.0f;
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

    protected void method_47399(class_6382 builder) {
        this.method_37021(builder);
    }

    public static interface PressAction {
        public void onPress(ModernButtonWidget var1);
    }
}
