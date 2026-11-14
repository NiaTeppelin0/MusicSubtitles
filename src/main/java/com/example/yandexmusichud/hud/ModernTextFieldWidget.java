package com.example.yandexmusichud.hud;

import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;

public class ModernTextFieldWidget extends class_342 {
    private String placeholder = "";
    private float focusProgress = 0.0f;
    private long lastAnimTime = 0L;

    public ModernTextFieldWidget(class_327 textRenderer, int x, int y, int width, int height, class_2561 text) {
        super(textRenderer, x, y, width, height, text);
    }

    public ModernTextFieldWidget setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    @Override
    public void method_47404(class_2561 placeholder) {
        this.placeholder = placeholder != null ? placeholder.getString() : "";
    }

    @Override
    public boolean method_1851() {
        return false;
    }

    @Override
    public int method_1859() {
        return Math.max(8, this.method_25368() - 12);
    }

    @Override
    public void method_48579(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = this.lastAnimTime > 0L ? Math.min(0.1f, (float)(now - this.lastAnimTime) / 1000.0f) : 0.016f;
        this.lastAnimTime = now;

        boolean focused = this.method_25370();
        boolean hovered = this.method_49606();

        float targetFocus = focused ? 1.0f : (hovered ? 0.38f : 0.0f);
        this.focusProgress += (targetFocus - this.focusProgress) * Math.min(1.0f, dt * 12.0f);

        int x = this.method_46426();
        int y = this.method_46427();
        int w = this.method_25368();
        int h = this.method_25364();

        if (this.focusProgress > 0.04f) {
            int haloAlpha = (int)(this.focusProgress * 36.0f);
            MusicHudRenderer.renderRoundedRect(context, x - 1, y - 1, w + 2, h + 2, 5, haloAlpha << 24 | 0x38BDF8);
        }

        int bg = focused ? 0xE60D1422 : (hovered ? 0xD8111828 : 0xBA0C101C);
        MusicHudRenderer.renderRoundedRect(context, x, y, w, h, 4, bg);
        if (w > 10 && h >= 12) {
            context.method_25294(x + 4, y + 1, x + w - 4, y + 2, 0x18FFFFFF);
        }

        int r = (int)(0x33 + (0x38 - 0x33) * this.focusProgress);
        int g = (int)(0x41 + (0xBD - 0x41) * this.focusProgress);
        int b = (int)(0x55 + (0xF8 - 0x55) * this.focusProgress);
        int alpha = (int)(0x68 + 0x8C * this.focusProgress);
        int topBorder = (alpha << 24) | (r << 16) | (g << 8) | b;
        int botBorder = ((alpha * 2 / 3) << 24) | (r << 16) | (g << 8) | b;
        MusicHudRenderer.renderRoundedBorder(context, x, y, w, h, 4, topBorder, botBorder);

        if (this.focusProgress > 0.05f && h >= 10) {
            int barH = Math.max(4, Math.round((float)(h - 6) * (0.4f + 0.6f * this.focusProgress)));
            int barY = y + (h - barH) / 2;
            int barAlpha = Math.min(255, (int)(this.focusProgress * 235.0f));
            MusicHudRenderer.renderRoundedRect(context, x + 2, barY, 2, barH, 1, barAlpha << 24 | 0x38BDF8);
        }

        int padX = 6;
        int padY = Math.max(0, (h - 8) / 2);
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)padX, (float)padY);
        super.method_48579(context, mouseX, mouseY, delta);
        context.method_51448().popMatrix();

        if (!this.placeholder.isEmpty() && this.method_1882().isEmpty() && !focused) {
            class_327 tr = net.minecraft.class_310.method_1551().field_1772;
            String clipPlaceholder = tr.method_1727(this.placeholder) > w - 12 ? tr.method_27523(this.placeholder, Math.max(8, w - 12)) : this.placeholder;
            context.method_51433(tr, clipPlaceholder, x + padX, y + padY, 0x7794A3B8, false);
        }
    }
}
