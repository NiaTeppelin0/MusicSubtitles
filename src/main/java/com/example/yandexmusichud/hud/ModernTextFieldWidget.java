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
    public boolean method_1851() {
        return false;
    }

    @Override
    public int method_1859() {
        return this.method_25368() - 8;
    }

    @Override
    public void method_48579(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = this.lastAnimTime > 0L ? Math.min(0.1f, (float)(now - this.lastAnimTime) / 1000.0f) : 0.016f;
        this.lastAnimTime = now;

        boolean focused = this.method_25370();
        boolean hovered = this.method_49606();

        float targetFocus = focused ? 1.0f : (hovered ? 0.35f : 0.0f);
        this.focusProgress += (targetFocus - this.focusProgress) * Math.min(1.0f, dt * 12.0f);

        int x = this.method_46426();
        int y = this.method_46427();
        int w = this.method_25368();
        int h = this.method_25364();

        int bg = focused ? 0xDD0D111E : (hovered ? 0xCC101524 : 0xAA0E1220);
        MusicHudRenderer.renderRoundedRect(context, x, y, w, h, 4, bg);

        int r = (int)(0x22 + (0x00 - 0x22) * this.focusProgress);
        int g = (int)(0x44 + (0xF5 - 0x44) * this.focusProgress);
        int b = (int)(0x66 + (0xD4 - 0x66) * this.focusProgress);
        int alpha = (int)(0x55 + 0x99 * this.focusProgress);
        int borderColor = (alpha << 24) | (r << 16) | (g << 8) | b;

        MusicHudRenderer.renderRoundedBorder(context, x, y, w, h, 4, borderColor, borderColor);

        super.method_48579(context, mouseX, mouseY, delta);

        if (!this.placeholder.isEmpty() && this.method_1882().isEmpty() && !focused) {
            class_327 tr = net.minecraft.class_310.method_1551().field_1772;
            context.method_51433(tr, this.placeholder, x + 4, y + (h - 8) / 2, 0x6694A3B8, false);
        }
    }
}
