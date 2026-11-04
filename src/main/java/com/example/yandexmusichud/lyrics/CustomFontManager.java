package com.example.yandexmusichud.lyrics;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_10799;
import net.minecraft.class_12249;
import net.minecraft.class_156;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class CustomFontManager {
    private static final List<FontInfo> ALL_FONTS = new ArrayList<FontInfo>();
    private static final Map<String, FontInfo> FONTS_BY_ID = new LinkedHashMap<String, FontInfo>();
    private static final Map<String, class_2960> PREVIEW_CACHE = new ConcurrentHashMap<String, class_2960>();
    private static final Map<String, CustomGlyph> GLYPH_CACHE = new ConcurrentHashMap<String, CustomGlyph>();
    private static Font FALLBACK_CYRILLIC = null;
    private static boolean initialized = false;

    public static class FontInfo {
        public final String id;
        public final String name;
        public final String category;
        public final boolean isMinecraft;
        public final boolean supportsCyrillic;
        public final boolean supportsLatin;
        public final Font awtFont;

        public FontInfo(String id, String name, String category, boolean isMinecraft, boolean supportsCyrillic, boolean supportsLatin, Font awtFont) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.isMinecraft = isMinecraft;
            this.supportsCyrillic = supportsCyrillic;
            this.supportsLatin = supportsLatin;
            this.awtFont = awtFont;
        }
    }

    public static class CustomGlyph {
        public final class_2960 textureId;
        public final float advance;
        public final float quadW;
        public final float quadH;
        public final float offX;
        public final float offY;
        public final int imgWidth;
        public final int imgHeight;

        public CustomGlyph(class_2960 textureId, float advance, float quadW, float quadH, float offX, float offY, int imgWidth, int imgHeight) {
            this.textureId = textureId;
            this.advance = advance;
            this.quadW = quadW;
            this.quadH = quadH;
            this.offX = offX;
            this.offY = offY;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
        }
    }

    public static synchronized void init() {
        if (!initialized) {
            reload();
            initialized = true;
        }
    }

    public static boolean checkCyrillic(Font f) {
        if (f == null) return false;
        try {
            return f.canDisplay('А') && f.canDisplay('я') && f.canDisplay('ё') && f.canDisplay('Ш');
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkLatin(Font f) {
        if (f == null) return false;
        try {
            return f.canDisplay('A') && f.canDisplay('z') && f.canDisplay('M');
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized void reload() {
        List<class_2960> texturesToRelease = new ArrayList<class_2960>(PREVIEW_CACHE.values());
        for (CustomGlyph g : GLYPH_CACHE.values()) {
            if (g != null && g.textureId != null) {
                texturesToRelease.add(g.textureId);
            }
        }
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.method_1531() != null && !texturesToRelease.isEmpty()) {
            Runnable releaseAction = () -> {
                for (class_2960 id : texturesToRelease) {
                    try {
                        mc.method_1531().method_4615(id);
                    } catch (Throwable ignored) {}
                }
            };
            if (mc.method_18854()) {
                releaseAction.run();
            } else {
                mc.execute(releaseAction);
            }
        }

        ALL_FONTS.clear();
        FONTS_BY_ID.clear();
        PREVIEW_CACHE.clear();
        GLYPH_CACHE.clear();

        List<FontInfo> mcFonts = new ArrayList<FontInfo>();
        mcFonts.add(new FontInfo("minecraft:default", "По умолчанию (MC)", "MINECRAFT", true, true, true, null));
        mcFonts.add(new FontInfo("minecraft:uniform", "Юникод (Uniform)", "MINECRAFT", true, true, true, null));
        mcFonts.add(new FontInfo("minecraft:alt", "Руны зачарования (SGA)", "MINECRAFT", true, false, true, null));
        mcFonts.add(new FontInfo("minecraft:illageralt", "Илладжер руны", "MINECRAFT", true, false, true, null));

        for (FontInfo fi : mcFonts) {
            addFont(fi);
        }

        List<FontInfo> fileFonts = new ArrayList<FontInfo>();
        File fontsDir = getFontsDirectory();
        if (!fontsDir.exists()) {
            fontsDir.mkdirs();
        }
        File[] files = fontsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".ttf") || lower.endsWith(".otf");
        });
        if (files != null) {
            for (File f : files) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, f);
                    String id = "file:" + f.getName();
                    String name = font.getFamily(Locale.ROOT);
                    if (name == null || name.isEmpty() || name.equalsIgnoreCase("dialog")) {
                        name = f.getName();
                    }
                    boolean cyr = checkCyrillic(font);
                    boolean lat = checkLatin(font);
                    fileFonts.add(new FontInfo(id, name, "ФАЙЛ", false, cyr, lat, font));
                } catch (Exception ignored) {}
            }
        }
        for (FontInfo fi : fileFonts) {
            addFont(fi);
        }

        String[] popular = {
            "Impact", "Comic Sans MS", "Arial", "Arial Black", "Calibri", "Consolas",
            "Segoe UI", "Verdana", "Times New Roman", "Trebuchet MS", "Georgia", "Courier New", "Bahnschrift"
        };
        Set<String> added = new HashSet<String>();
        List<FontInfo> popCyr = new ArrayList<FontInfo>();
        List<FontInfo> popOther = new ArrayList<FontInfo>();
        for (String p : popular) {
            try {
                Font f = new Font(p, Font.PLAIN, 24);
                if (f.getFamily().equalsIgnoreCase(p) || f.getName().equalsIgnoreCase(p)) {
                    boolean cyr = checkCyrillic(f);
                    boolean lat = checkLatin(f);
                    if (cyr || lat) {
                        FontInfo fi = new FontInfo("sys:" + p, p, "СИСТЕМА", false, cyr, lat, f);
                        if (cyr) {
                            popCyr.add(fi);
                        } else {
                            popOther.add(fi);
                        }
                        added.add(p.toLowerCase(Locale.ROOT));
                    }
                }
            } catch (Exception ignored) {}
        }
        for (FontInfo fi : popCyr) {
            addFont(fi);
        }

        List<FontInfo> sysCyr = new ArrayList<FontInfo>();
        List<FontInfo> sysOther = new ArrayList<FontInfo>();
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
            if (available != null) {
                for (String fam : available) {
                    if (added.contains(fam.toLowerCase(Locale.ROOT))) continue;
                    try {
                        Font f = new Font(fam, Font.PLAIN, 24);
                        boolean cyr = checkCyrillic(f);
                        boolean lat = checkLatin(f);
                        if (!cyr && !lat) {
                            continue;
                        }
                        FontInfo fi = new FontInfo("sys:" + fam, fam, "СИСТЕМА", false, cyr, lat, f);
                        if (cyr) {
                            sysCyr.add(fi);
                        } else {
                            sysOther.add(fi);
                        }
                        added.add(fam.toLowerCase(Locale.ROOT));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        for (FontInfo fi : sysCyr) {
            addFont(fi);
        }
        for (FontInfo fi : popOther) {
            addFont(fi);
        }
        for (FontInfo fi : sysOther) {
            addFont(fi);
        }
    }

    private static void addFont(FontInfo info) {
        ALL_FONTS.add(info);
        FONTS_BY_ID.put(info.id.toLowerCase(Locale.ROOT), info);
    }

    public static List<FontInfo> getAllFonts() {
        if (!initialized) {
            init();
        }
        return Collections.unmodifiableList(ALL_FONTS);
    }

    public static FontInfo getFontInfo(String fontId) {
        if (!initialized) {
            init();
        }
        if (fontId == null) {
            return FONTS_BY_ID.get("minecraft:default");
        }
        return FONTS_BY_ID.get(fontId.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isCustomFont(String fontId) {
        if (fontId == null || fontId.isEmpty()) {
            return false;
        }
        String lower = fontId.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("sys:") || lower.startsWith("file:");
    }

    public static String getDisplayName(String fontId) {
        if (fontId == null || fontId.isEmpty() || "minecraft:default".equalsIgnoreCase(fontId) || "default".equalsIgnoreCase(fontId)) {
            return "По умолч.";
        }
        if ("minecraft:alt".equalsIgnoreCase(fontId)) {
            return "Руны (SGA)";
        }
        if ("minecraft:illageralt".equalsIgnoreCase(fontId)) {
            return "Илладжер";
        }
        if ("minecraft:uniform".equalsIgnoreCase(fontId)) {
            return "Юникод";
        }
        if (fontId.startsWith("sys:")) {
            return fontId.substring(4);
        }
        if (fontId.startsWith("file:")) {
            return fontId.substring(5);
        }
        FontInfo info = getFontInfo(fontId);
        if (info != null) {
            return info.name;
        }
        return fontId;
    }

    public static class_2960 getPreviewTexture(FontInfo info) {
        if (info == null || info.isMinecraft || info.awtFont == null) {
            return null;
        }
        class_2960 cached = PREVIEW_CACHE.get(info.id);
        if (cached != null) {
            return cached;
        }
        try {
            int w = 1200;
            int h = 64;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            Font f = info.awtFont.deriveFont(Font.PLAIN, 26f);
            g.setFont(f);
            g.setColor(new Color(242, 246, 255));

            String sample;
            if (info.supportsCyrillic) {
                sample = "Съешь ещё этих мягких булок - 123";
                if (f.canDisplay('A') && f.canDisplay('a')) {
                    sample += " Aa";
                } else if (f.canDisplay('A')) {
                    sample += " A";
                }
            } else {
                sample = "The quick brown fox jumps - 123";
                if (f.canDisplay('A') && f.canDisplay('a')) {
                    sample += " Aa";
                }
            }

            StringBuilder safeSample = new StringBuilder();
            for (int i = 0; i < sample.length(); i++) {
                char ch = sample.charAt(i);
                if (f.canDisplay(ch)) {
                    safeSample.append(ch);
                } else if (Character.isLowerCase(ch) && f.canDisplay(Character.toUpperCase(ch))) {
                    safeSample.append(Character.toUpperCase(ch));
                } else if (ch == '•' || ch == '—' || ch == '–') {
                    safeSample.append('-');
                }
            }
            sample = safeSample.toString();

            FontMetrics fm = g.getFontMetrics();
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(sample, 8, baseline);
            g.dispose();

            class_1011 nativeImg = new class_1011(w, h, false);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (green << 8) | r;
                    nativeImg.method_61941(x, y, abgr);
                }
            }
            class_1043 tex = new class_1043(() -> "preview_" + info.id, nativeImg);
            class_2960 texUri = class_2960.method_60655("yandexmusichud", "font_prev_" + Integer.toUnsignedString(info.id.hashCode(), 16));
            class_310.method_1551().method_1531().method_4616(texUri, (class_1044)tex);
            PREVIEW_CACHE.put(info.id, texUri);
            return texUri;
        } catch (Exception e) {
            return null;
        }
    }

    public static CustomGlyph getGlyph(String fontId, char c) {
        if (fontId == null || !isCustomFont(fontId)) {
            return null;
        }
        String cacheKey = fontId + ":" + (int)c;
        CustomGlyph cached = GLYPH_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Font baseFont = resolveAwtFont(fontId);
        if (baseFont == null) {
            return null;
        }
        if (c == ' ') {
            try {
                Font font = baseFont.deriveFont(Font.PLAIN, 36f);
                BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gTemp = temp.createGraphics();
                gTemp.setFont(font);
                FontMetrics fm = gTemp.getFontMetrics();
                int spW = fm.charWidth(' ');
                int totalH = Math.max(10, fm.getAscent() + fm.getDescent());
                gTemp.dispose();
                float scale = 9.0f / (float)totalH;
                float advance = Math.max(3.0f, (float)spW * scale);
                CustomGlyph spGlyph = new CustomGlyph(null, advance, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0);
                GLYPH_CACHE.put(cacheKey, spGlyph);
                return spGlyph;
            } catch (Exception e) {
                return new CustomGlyph(null, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0);
            }
        }
        try {
            Font fontToUse = baseFont;
            if (!fontToUse.canDisplay(c)) {
                Font fallback = getFallbackFont(c);
                if (fallback != null) {
                    fontToUse = fallback;
                }
            }
            Font font = fontToUse.deriveFont(Font.PLAIN, 36f);
            BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gTemp = temp.createGraphics();
            gTemp.setFont(font);
            FontMetrics fm = gTemp.getFontMetrics();
            int charW = Math.max(2, fm.charWidth(c));
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            int totalH = Math.max(10, ascent + descent);
            gTemp.dispose();

            int pad = 3;
            int imgW = charW + pad * 2;
            int imgH = totalH + pad * 2;
            BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(font);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(c), pad, pad + ascent);
            g.dispose();

            class_1011 nativeImg = new class_1011(imgW, imgH, false);
            for (int y = 0; y < imgH; y++) {
                for (int x = 0; x < imgW; x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (green << 8) | r;
                    nativeImg.method_61941(x, y, abgr);
                }
            }

            class_1043 tex = new class_1043(() -> "glyph_" + cacheKey, nativeImg);
            class_2960 texUri = class_2960.method_60655("yandexmusichud", "glyph_" + Integer.toUnsignedString(cacheKey.hashCode(), 16));
            class_310.method_1551().method_1531().method_4616(texUri, (class_1044)tex);

            float scale = 9.0f / (float)totalH;
            float advance = Math.max(1.0f, (float)charW * scale);
            float quadW = (float)imgW * scale;
            float quadH = (float)imgH * scale;
            float offX = -((float)pad * scale);
            float offY = -((float)pad * scale);

            CustomGlyph glyph = new CustomGlyph(texUri, advance, quadW, quadH, offX, offY, imgW, imgH);
            GLYPH_CACHE.put(cacheKey, glyph);
            return glyph;
        } catch (Exception e) {
            return null;
        }
    }

    public static float getCharWidth(String fontId, char c) {
        if (!isCustomFont(fontId)) {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.field_1772 != null) {
                return (float)mc.field_1772.method_1727(String.valueOf(c));
            }
            return 6.0f;
        }
        CustomGlyph g = getGlyph(fontId, c);
        if (g != null) {
            return g.advance;
        }
        return 6.0f;
    }

    public static int getStringWidth(String fontId, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (!isCustomFont(fontId)) {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.field_1772 != null) {
                return mc.field_1772.method_1727(text);
            }
            return text.length() * 6;
        }
        float total = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            total += getCharWidth(fontId, text.charAt(i));
        }
        return Math.round(total);
    }

    public static void drawGlyph2D(class_332 context, CustomGlyph glyph, int x, int y, int color) {
        if (glyph == null || glyph.textureId == null) {
            return;
        }
        int qx = x + Math.round(glyph.offX);
        int qy = y + Math.round(glyph.offY);
        int qw = Math.max(1, Math.round(glyph.quadW));
        int qh = Math.max(1, Math.round(glyph.quadH));
        context.method_25293(class_10799.field_56883, glyph.textureId, qx, qy, 0.0f, 0.0f, qw, qh, glyph.imgWidth, glyph.imgHeight, glyph.imgWidth, glyph.imgHeight, color);
    }

    public static void drawGlyph3D(Matrix4f matrix, class_4597 consumers, CustomGlyph glyph, float x, float y, float z, int color, boolean seeThrough, int light) {
        if (glyph == null || glyph.textureId == null) {
            return;
        }
        class_1921 textLayer = class_12249.method_75992(glyph.textureId, seeThrough);
        class_4588 buffer = consumers.method_73477(textLayer);
        float x0 = x + glyph.offX;
        float y0 = y + glyph.offY;
        float x1 = x0 + glyph.quadW;
        float y1 = y0 + glyph.quadH;

        buffer.method_22918((Matrix4fc)matrix, x0, y0, z).method_39415(color).method_22913(0.0f, 0.0f).method_60803(light);
        buffer.method_22918((Matrix4fc)matrix, x0, y1, z).method_39415(color).method_22913(0.0f, 1.0f).method_60803(light);
        buffer.method_22918((Matrix4fc)matrix, x1, y1, z).method_39415(color).method_22913(1.0f, 1.0f).method_60803(light);
        buffer.method_22918((Matrix4fc)matrix, x1, y0, z).method_39415(color).method_22913(1.0f, 0.0f).method_60803(light);

        buffer.method_22918((Matrix4fc)matrix, x1, y0, z).method_39415(color).method_22913(1.0f, 0.0f).method_60803(light);
        buffer.method_22918((Matrix4fc)matrix, x1, y1, z).method_39415(color).method_22913(1.0f, 1.0f).method_60803(light);
        buffer.method_22918((Matrix4fc)matrix, x0, y1, z).method_39415(color).method_22913(0.0f, 1.0f).method_60803(light);
        buffer.method_22918((Matrix4fc)matrix, x0, y0, z).method_39415(color).method_22913(0.0f, 0.0f).method_60803(light);
    }

    private static Font getFallbackFont(char c) {
        if (FALLBACK_CYRILLIC == null) {
            String[] fallbacks = {"Segoe UI", "Arial", "Calibri", "Verdana", "Tahoma"};
            for (String fName : fallbacks) {
                try {
                    Font f = new Font(fName, Font.PLAIN, 36);
                    if (f.canDisplay('А') && f.canDisplay('я')) {
                        FALLBACK_CYRILLIC = f;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (FALLBACK_CYRILLIC == null) {
                FALLBACK_CYRILLIC = new Font(Font.SANS_SERIF, Font.PLAIN, 36);
            }
        }
        if (FALLBACK_CYRILLIC.canDisplay(c)) {
            return FALLBACK_CYRILLIC;
        }
        return null;
    }

    private static Font resolveAwtFont(String fontId) {
        if (fontId == null) {
            return null;
        }
        FontInfo info = getFontInfo(fontId);
        if (info != null && info.awtFont != null) {
            return info.awtFont;
        }
        if (fontId.startsWith("sys:")) {
            String name = fontId.substring(4);
            return new Font(name, Font.PLAIN, 36);
        }
        if (fontId.startsWith("file:")) {
            String name = fontId.substring(5);
            File f = new File(getFontsDirectory(), name);
            if (f.exists()) {
                try {
                    return Font.createFont(Font.TRUETYPE_FONT, f);
                } catch (Exception ignored) {}
            }
        }
        return new Font("Arial", Font.PLAIN, 36);
    }

    public static File getFontsDirectory() {
        class_310 mc = class_310.method_1551();
        File base = (mc != null && mc.field_1697 != null) ? mc.field_1697 : new File(".");
        File configDir = new File(base, "config");
        File modDir = new File(configDir, "yandexmusichud");
        return new File(modDir, "fonts");
    }

    public static void openFontsFolder() {
        File dir = getFontsDirectory();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        com.example.yandexmusichud.favorites.FavoritesManager.openFolderSafely(dir.toPath());
    }
}
