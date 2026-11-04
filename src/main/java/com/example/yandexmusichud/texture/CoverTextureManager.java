package com.example.yandexmusichud.texture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;

public class CoverTextureManager {
    public static final class_2960 DEFAULT_COVER = class_2960.method_60655((String)"yandexmusichud", (String)"textures/gui/default_cover.png");
    private static final class_2960 COVER_TEXTURE_ID = class_2960.method_60655((String)"yandexmusichud", (String)"dynamic/album_cover");
    private static final class_2960 COVER_TEXTURE_A = class_2960.method_60655((String)"yandexmusichud", (String)"dynamic/album_cover_a");
    private static final class_2960 COVER_TEXTURE_B = class_2960.method_60655((String)"yandexmusichud", (String)"dynamic/album_cover_b");
    private final ExecutorService decoder = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicSubtitles-CoverDecoder");
        t.setDaemon(true);
        return t;
    });
    private volatile class_2960 currentCoverId = DEFAULT_COVER;
    private volatile class_2960 previousCoverId = null;
    private volatile long coverChangeTimestamp = 0L;
    private volatile boolean useTextureA = false;
    private volatile boolean hasCustomCover = false;
    private volatile String lastBase64 = null;
    private volatile int dominantColor = 7101671;
    private volatile float smoothDomR = 108.0f;
    private volatile float smoothDomG = 94.0f;
    private volatile float smoothDomB = 103.0f;
    private volatile long lastColorUpdateTime = 0L;
    private static final class_2960 ROUNDED_DEFAULT_COVER = class_2960.method_60655((String)"yandexmusichud", (String)"dynamic/rounded_default_cover");
    private volatile boolean defaultCoverInitialized = false;

    private class_2960 getDefaultCover() {
        if (this.defaultCoverInitialized) return ROUNDED_DEFAULT_COVER;
        class_310 mc = class_310.method_1551();
        if (mc == null || !mc.method_18854()) {
            return DEFAULT_COVER;
        }
        try (InputStream stream = CoverTextureManager.class.getResourceAsStream("/assets/yandexmusichud/textures/gui/default_cover.png");){
            if (stream == null) return DEFAULT_COVER;
            class_1011 img = class_1011.method_4309((InputStream)stream);
            CoverTextureManager.roundImageCorners(img, (int)((float)Math.min(img.method_4307(), img.method_4323()) * 0.12f));
            class_1043 texture = new class_1043(() -> "default_cover", img);
            mc.method_1531().method_4616(ROUNDED_DEFAULT_COVER, (class_1044)texture);
            this.defaultCoverInitialized = true;
            return ROUNDED_DEFAULT_COVER;
        }
        catch (Exception exception) {

        }
        return DEFAULT_COVER;
    }

    public class_2960 getCurrentCoverId() {
        if (this.currentCoverId == DEFAULT_COVER) {
            return this.getDefaultCover();
        }
        return this.currentCoverId;
    }

    public String getLastBase64() {
        return this.lastBase64;
    }

    public int getDominantColor() {
        return this.dominantColor;
    }

    public int getSmoothDominantColor() {
        long now = System.currentTimeMillis();
        float dt = this.lastColorUpdateTime > 0L ? Math.min(0.1f, (float)(now - this.lastColorUpdateTime) / 1000.0f) : 0.016f;
        this.lastColorUpdateTime = now;
        int target = this.dominantColor;
        float tr = (float)(target >> 16 & 0xFF);
        float tg = (float)(target >> 8 & 0xFF);
        float tb = (float)(target & 0xFF);
        float factor = Math.min(1.0f, dt * 7.5f);
        this.smoothDomR += (tr - this.smoothDomR) * factor;
        this.smoothDomG += (tg - this.smoothDomG) * factor;
        this.smoothDomB += (tb - this.smoothDomB) * factor;
        int r = Math.max(0, Math.min(255, Math.round(this.smoothDomR)));
        int g = Math.max(0, Math.min(255, Math.round(this.smoothDomG)));
        int b = Math.max(0, Math.min(255, Math.round(this.smoothDomB)));
        return r << 16 | g << 8 | b;
    }

    public class_2960 getPreviousCoverId() {
        if (this.previousCoverId == null || this.coverChangeTimestamp <= 0L) {
            return null;
        }
        long elapsed = System.currentTimeMillis() - this.coverChangeTimestamp;
        if (elapsed >= 240L) {
            this.previousCoverId = null;
            return null;
        }
        return this.previousCoverId;
    }

    public float getCoverFadeProgress() {
        if (this.coverChangeTimestamp <= 0L) {
            return 1.0f;
        }
        long elapsed = System.currentTimeMillis() - this.coverChangeTimestamp;
        if (elapsed >= 240L) {
            return 1.0f;
        }
        float t = (float)elapsed / 240.0f;
        return t * t * (3.0f - 2.0f * t);
    }

    public void updateCover(String base64Data) {
        this.lastBase64 = base64Data;
        this.decoder.submit(() -> {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                class_1011 image = class_1011.method_4309((InputStream)new ByteArrayInputStream(imageBytes));
                this.dominantColor = CoverTextureManager.extractDominantColor(image);
                CoverTextureManager.roundImageCorners(image, (int)((float)Math.min(image.method_4307(), image.method_4323()) * 0.12f));
                class_2960 targetId = this.useTextureA ? COVER_TEXTURE_B : COVER_TEXTURE_A;
                this.useTextureA = !this.useTextureA;
                class_310.method_1551().execute(() -> {
                    try {
                        class_1043 texture = new class_1043(() -> "album_cover", image);
                        class_310.method_1551().method_1531().method_4616(targetId, (class_1044)texture);
                        class_2960 oldId = this.currentCoverId != null ? this.currentCoverId : this.getDefaultCover();
                        if (oldId != targetId) {
                            this.previousCoverId = oldId;
                            this.coverChangeTimestamp = System.currentTimeMillis();
                        }
                        this.currentCoverId = targetId;
                        this.hasCustomCover = true;
                    }
                    catch (Exception e) {
                        this.currentCoverId = this.getDefaultCover();
                    }
                });
            }
            catch (Exception e) {
                this.currentCoverId = DEFAULT_COVER;
            }
        });
    }

    public static void roundImageCorners(class_1011 img, int radius) {
        if (img == null || radius <= 0) {
            return;
        }
        try {
            int w = img.method_4307();
            int h = img.method_4323();
            radius = Math.min(radius, Math.min(w / 2, h / 2));
            double rSq = (double)radius * (double)radius;
            for (int y = 0; y < radius; ++y) {
                double dy = (double)(radius - y) - 0.5;
                for (int x = 0; x < radius; ++x) {
                    double dx = (double)(radius - x) - 0.5;
                    double distSq = dx * dx + dy * dy;
                    if (!(distSq > rSq)) continue;
                    double dist = Math.sqrt(distSq);
                    float edgeAlpha = Math.max(0.0f, Math.min(1.0f, (float)((double)radius + 0.5 - dist)));
                    CoverTextureManager.setAntialiasedPixel(img, x, y, edgeAlpha);
                    CoverTextureManager.setAntialiasedPixel(img, w - 1 - x, y, edgeAlpha);
                    CoverTextureManager.setAntialiasedPixel(img, x, h - 1 - y, edgeAlpha);
                    CoverTextureManager.setAntialiasedPixel(img, w - 1 - x, h - 1 - y, edgeAlpha);
                }
            }
        }
        catch (Exception exception) {

        }
    }

    private static void setAntialiasedPixel(class_1011 img, int x, int y, float factor) {
        if (factor <= 0.001f) {
            img.method_61941(x, y, 0);
        } else {
            int color = img.method_61940(x, y);
            int a = (int)((float)(color >> 24 & 0xFF) * factor);
            img.method_61941(x, y, a << 24 | color & 0xFFFFFF);
        }
    }

    private static int extractDominantColor(class_1011 img) {
        if (img == null) {
            return 7101671;
        }
        try {
            int w = img.method_4307();
            int h = img.method_4323();
            long rSum = 0L;
            long gSum = 0L;
            long bSum = 0L;
            long count = 0L;
            float maxSaturation = -1.0f;
            int mostVibrantColor = 7101671;
            int step = Math.max(1, w / 16);
            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    float sat;
                    int b;
                    int g;
                    int argb = img.method_61940(x, y);
                    int r = argb >> 16 & 0xFF;
                    int brightness = (r + (g = argb >> 8 & 0xFF) + (b = argb & 0xFF)) / 3;
                    if (brightness < 30 || brightness > 235) continue;
                    int max = Math.max(r, Math.max(g, b));
                    int min = Math.min(r, Math.min(g, b));
                    float f = sat = max == 0 ? 0.0f : (float)(max - min) / (float)max;
                    if (sat > maxSaturation) {
                        maxSaturation = sat;
                        mostVibrantColor = r << 16 | g << 8 | b;
                    }
                    rSum += (long)r;
                    gSum += (long)g;
                    bSum += (long)b;
                    ++count;
                }
            }
            if (maxSaturation > 0.2f) {
                return mostVibrantColor;
            }
            if (count > 0L) {
                return (int)(rSum / count) << 16 | (int)(gSum / count) << 8 | (int)(bSum / count);
            }
        }
        catch (Exception exception) {

        }
        return 7101671;
    }

    public void resetToDefault() {
        this.dominantColor = 7101671;
        if (this.hasCustomCover) {
            class_310.method_1551().execute(() -> {
                this.previousCoverId = this.currentCoverId;
                this.coverChangeTimestamp = System.currentTimeMillis();
                this.hasCustomCover = false;
                this.currentCoverId = this.getDefaultCover();
            });
        }
    }

    public void shutdown() {
        this.decoder.shutdownNow();
        this.resetToDefault();
    }
}

