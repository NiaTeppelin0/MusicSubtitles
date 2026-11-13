package com.example.yandexmusichud.hud;

import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_10799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3532;

public class ColorWheelWidget {
    private static final class_2960 COLOR_WHEEL_TEXTURE = class_2960.method_60655((String)"yandexmusichud", (String)"dynamic/color_wheel");
    private static volatile boolean textureInitialized = false;
    private static volatile boolean textureGenerating = false;
    public final int x;
    public final int y;
    public final int size;
    private final float radius;
    private final float centerX;
    private final float centerY;
    private float currentHue = 0.0f;
    private float currentSaturation = 1.0f;
    private float currentBrightness = 1.0f;
    private boolean isDragging = false;
    private Consumer<Integer> onColorChanged;

    public ColorWheelWidget(int x, int y, int size, int initialRgb, Consumer<Integer> onColorChanged) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.radius = (float)size / 2.0f;
        this.centerX = (float)x + this.radius;
        this.centerY = (float)y + this.radius;
        this.onColorChanged = onColorChanged;
        this.setColorFromRgb(initialRgb);
        ColorWheelWidget.ensureTextureCreated();
    }

    public void setOnColorChanged(Consumer<Integer> callback) {
        this.onColorChanged = callback;
    }

    public void setColorFromRgb(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        this.currentHue = hsb[0];
        this.currentSaturation = hsb[1];
        this.currentBrightness = Math.max(0.05f, hsb[2]);
    }

    public int getRgb() {
        return Color.HSBtoRGB(this.currentHue, this.currentSaturation, this.currentBrightness) & 0xFFFFFF;
    }

    public float getBrightness() {
        return this.currentBrightness;
    }

    public void setBrightness(float b) {
        this.currentBrightness = class_3532.method_15363((float)b, (float)0.0f, (float)1.0f);
        if (this.onColorChanged != null) {
            this.onColorChanged.accept(this.getRgb());
        }
    }

    private static synchronized void ensureTextureCreated() {
        if (textureInitialized || textureGenerating) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        textureGenerating = true;
        class_1011 image = null;
        boolean handedOff = false;
        try {
            int texSize = 128;
            float texRadius = (float)texSize / 2.0f;
            image = new class_1011(texSize, texSize, false);
            for (int py = 0; py < texSize; ++py) {
                for (int px = 0; px < texSize; ++px) {
                    float dx = (float)px - texRadius + 0.5f;
                    float dy = (float)py - texRadius + 0.5f;
                    float dist = (float)Math.sqrt(dx * dx + dy * dy);
                    if (dist <= texRadius - 1.0f) {
                        float angle = (float)Math.atan2(dy, dx);
                        float hue = (angle + (float)Math.PI) / ((float)Math.PI * 2);
                        float sat = dist / (texRadius - 1.0f);
                        int rgb = Color.HSBtoRGB(hue, sat, 1.0f) & 0xFFFFFF;
                        int argb = 0xFF000000 | rgb;
                        image.method_61941(px, py, argb);
                        continue;
                    }
                    if (dist <= texRadius) {
                        float alphaFraction = 1.0f - (dist - (texRadius - 1.0f));
                        int alpha = (int)(alphaFraction * 255.0f);
                        float angle = (float)Math.atan2(dy, dx);
                        float hue = (angle + (float)Math.PI) / ((float)Math.PI * 2);
                        int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;
                        image.method_61941(px, py, alpha << 24 | rgb);
                        continue;
                    }
                    image.method_61941(px, py, 0);
                }
            }
            if (mc.method_18854()) {
                class_1043 texture = new class_1043(() -> "color_wheel", image);
                handedOff = true;
                mc.method_1531().method_4616(COLOR_WHEEL_TEXTURE, (class_1044)texture);
                textureInitialized = true;
                textureGenerating = false;
            } else {
                final class_1011 finalImage = image;
                handedOff = true;
                mc.execute(() -> {
                    boolean registered = false;
                    try {
                        class_1043 texture = new class_1043(() -> "color_wheel", finalImage);
                        registered = true;
                        mc.method_1531().method_4616(COLOR_WHEEL_TEXTURE, (class_1044)texture);
                        textureInitialized = true;
                    } catch (Exception ignored) {
                        if (!registered) {
                            try {
                                finalImage.close();
                            } catch (Exception ignored2) {}
                        }
                    } finally {
                        textureGenerating = false;
                    }
                });
            }
        } catch (Exception ignored) {
            textureGenerating = false;
        } finally {
            if (!handedOff && image != null) {
                try {
                    image.close();
                } catch (Exception ignored) {}
                textureGenerating = false;
            }
        }
    }

    public void render(class_332 context, int mouseX, int mouseY) {
        int brInt = Math.max(20, Math.min(255, (int)(this.currentBrightness * 255.0f)));
        int tint = 0xFF000000 | (brInt << 16) | (brInt << 8) | brInt;
        context.method_25291(class_10799.field_56883, COLOR_WHEEL_TEXTURE, this.x, this.y, 0.0f, 0.0f, this.size, this.size, this.size, this.size, tint);
        float angle = this.currentHue * 2.0f * (float)Math.PI - (float)Math.PI;
        float dist = this.currentSaturation * (this.radius - 1.0f);
        int selX = Math.round(this.centerX + (float)Math.cos(angle) * dist);
        int selY = Math.round(this.centerY + (float)Math.sin(angle) * dist);
        context.method_25294(selX - 3, selY - 3, selX + 4, selY + 4, -1);
        context.method_25294(selX - 2, selY - 2, selX + 3, selY + 3, -16777216);
        int curRgb = this.getRgb();
        context.method_25294(selX - 1, selY - 1, selX + 2, selY + 2, 0xFF000000 | curRgb);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        float dx = (float)mouseX - this.centerX;
        float dy = (float)mouseY - this.centerY;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist <= this.radius + 2.0f) {
            this.isDragging = true;
            this.updateFromMouse(mouseX, mouseY);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (this.isDragging) {
            this.updateFromMouse(mouseX, mouseY);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        this.isDragging = false;
    }

    public boolean isDragging() {
        return this.isDragging;
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        float dx = (float)mouseX - this.centerX;
        float dy = (float)mouseY - this.centerY;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        float angle = (float)Math.atan2(dy, dx);
        this.currentHue = (angle + (float)Math.PI) / ((float)Math.PI * 2);
        this.currentSaturation = class_3532.method_15363((float)(dist / (this.radius - 1.0f)), (float)0.0f, (float)1.0f);
        if (this.onColorChanged != null) {
            this.onColorChanged.accept(this.getRgb());
        }
    }
}

