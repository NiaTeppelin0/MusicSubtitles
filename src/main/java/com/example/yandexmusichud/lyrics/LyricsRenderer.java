package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.lyrics.LyricLine;
import com.example.yandexmusichud.lyrics.LyricsManager;
import com.example.yandexmusichud.lyrics.SubtitleBubble;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_12249;
import net.minecraft.class_1297;
import net.minecraft.class_1921;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4184;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_465;
import net.minecraft.class_481;
import net.minecraft.class_9779;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LyricsRenderer {
    private static final String[] ASCII_CHAR_STRINGS = new String[128];
    private static final java.util.concurrent.ConcurrentHashMap<String, net.minecraft.class_11719.class_11721> FONT_DESC_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, net.minecraft.class_2583> FONT_STYLE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, net.minecraft.class_5250> STYLED_CHAR_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        for (int i = 0; i < 128; ++i) {
            ASCII_CHAR_STRINGS[i] = String.valueOf((char)i);
        }
    }

    public static String singleCharString(char c) {
        return c < 128 ? ASCII_CHAR_STRINGS[c] : String.valueOf(c);
    }

    public static net.minecraft.class_11719.class_11721 getCachedFontDesc(String fontName) {
        return FONT_DESC_CACHE.computeIfAbsent(fontName, name -> {
            class_2960 fontId = name.contains(":") ? class_2960.method_60654(name) : class_2960.method_60655("minecraft", name);
            return new net.minecraft.class_11719.class_11721(fontId);
        });
    }

    public static net.minecraft.class_2583 getCachedFontStyle(String fontName) {
        return FONT_STYLE_CACHE.computeIfAbsent(fontName, name -> net.minecraft.class_2583.field_24360.method_27704(getCachedFontDesc(name)));
    }

    public static net.minecraft.class_5250 getCachedStyledChar(String fontName, String renderStr) {
        String key = fontName + '\u0000' + renderStr;
        net.minecraft.class_5250 cached = STYLED_CHAR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        net.minecraft.class_5250 comp = net.minecraft.class_2561.method_43470(renderStr);
        comp.method_10862(getCachedFontStyle(fontName));
        if (STYLED_CHAR_CACHE.size() > 2048) {
            STYLED_CHAR_CACHE.clear();
        }
        STYLED_CHAR_CACHE.putIfAbsent(key, comp);
        return comp;
    }

    private final LyricsManager lyricsManager;
    private final NowPlayingClient client;
    private long last3DRenderNanoTime = 0L;
    private long last2DFrameTimeNs = 0L;
    private static Boolean irisAvailable = null;
    private static java.lang.reflect.Method irisIsShadowPassMethod = null;
    private static Object irisApiInstance = null;

    private static boolean isIrisShadowPass() {
        if (irisAvailable == null) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                java.lang.reflect.Method getInstance = irisApiClass.getMethod("getInstance");
                irisApiInstance = getInstance.invoke(null);
                irisIsShadowPassMethod = irisApiClass.getMethod("isRenderingShadowPass");
                irisAvailable = true;
            } catch (Throwable t) {
                irisAvailable = false;
            }
        }
        if (irisAvailable && irisApiInstance != null && irisIsShadowPassMethod != null) {
            try {
                return (boolean)irisIsShadowPassMethod.invoke(irisApiInstance);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static java.lang.reflect.Field handledScreenXField = null;
    private static java.lang.reflect.Field handledScreenYField = null;
    private static java.lang.reflect.Field handledScreenWField = null;
    private static java.lang.reflect.Field handledScreenHField = null;
    private static boolean handledScreenFieldsInit = false;
    private double lastMouseX = -1.0;
    private double lastMouseY = -1.0;
    private long lastMouseTime = 0L;
    private float mouseVelX = 0.0f;
    private float mouseVelY = 0.0f;
    private SubtitleBubble draggedBubble = null;
    private int draggedCharIndex = -1;
    private float dragGrabOffsetX = 0.0f;
    private float dragGrabOffsetY = 0.0f;

    private static void initHandledScreenFields() {
        if (handledScreenFieldsInit) {
            return;
        }
        try {
            handledScreenXField = class_465.class.getDeclaredField("field_2776");
            handledScreenXField.setAccessible(true);
            handledScreenYField = class_465.class.getDeclaredField("field_2800");
            handledScreenYField.setAccessible(true);
            handledScreenWField = class_465.class.getDeclaredField("field_2792");
            handledScreenWField.setAccessible(true);
            handledScreenHField = class_465.class.getDeclaredField("field_2779");
            handledScreenHField.setAccessible(true);
        } catch (Throwable t) {
            try {
                for (java.lang.reflect.Field f : class_465.class.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        if ("x".equals(f.getName())) {
                            f.setAccessible(true);
                            handledScreenXField = f;
                        } else if ("y".equals(f.getName())) {
                            f.setAccessible(true);
                            handledScreenYField = f;
                        } else if ("backgroundWidth".equals(f.getName())) {
                            f.setAccessible(true);
                            handledScreenWField = f;
                        } else if ("backgroundHeight".equals(f.getName())) {
                            f.setAccessible(true);
                            handledScreenHField = f;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        handledScreenFieldsInit = true;
    }

    public static float[] getScreenObstacleBounds(class_437 screen, int screenW, int screenH) {
        if (screen instanceof class_465) {
            initHandledScreenFields();
            int gx = (screenW - 176) / 2;
            int gy = (screenH - 166) / 2;
            int gw = 176;
            int gh = 166;
            if (handledScreenXField != null && handledScreenYField != null && handledScreenWField != null && handledScreenHField != null) {
                try {
                    gx = handledScreenXField.getInt(screen);
                    gy = handledScreenYField.getInt(screen);
                    gw = handledScreenWField.getInt(screen);
                    gh = handledScreenHField.getInt(screen);
                } catch (Throwable ignored) {
                }
            }
            if (screen instanceof class_481) {
                gy -= 28;
                gh += 28;
            }
            return new float[]{ (float)gx, (float)gy, (float)(gx + gw), (float)(gy + gh) };
        }
        return null;
    }

    public LyricsRenderer(LyricsManager lyricsManager, NowPlayingClient client) {
        this.lyricsManager = lyricsManager;
        this.client = client;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::render2D);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!HudConfig.INSTANCE.subtitlesSeeThrough) {
                this.render3D(context);
            }
        });
        WorldRenderEvents.END_MAIN.register(context -> {
            if (HudConfig.INSTANCE.subtitlesSeeThrough) {
                this.render3D(context);
            }
        });
    }

    private void render3D(WorldRenderContext context) {
        if (isIrisShadowPass()) {
            return;
        }
        long currentNano = System.nanoTime();
        if (currentNano == this.last3DRenderNanoTime) {
            return;
        }
        this.last3DRenderNanoTime = currentNano;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1773 == null) {
            return;
        }
        if (mc.field_1690.field_1842 && !HudConfig.INSTANCE.subtitlesVisibleInF1) {
            return;
        }
        TrackState state = this.client.getState();
        String effectiveMode = this.lyricsManager.resolveEffectiveMode(state);
        if (!HudConfig.INSTANCE.subtitlesEnabled || "CHAT".equalsIgnoreCase(effectiveMode) || !"WORLD_3D".equalsIgnoreCase(effectiveMode) && !"WORLD_BLOCK".equalsIgnoreCase(effectiveMode) && !"WORLD_SURFACE".equalsIgnoreCase(effectiveMode)) {
            return;
        }
        List<SubtitleBubble> bubbles = this.lyricsManager.getActiveBubbles();
        if (bubbles.isEmpty()) {
            return;
        }
        int trackOffset = state.isActive() ? HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title()) : 0;
        long currentPosMs = state.isActive() ? Math.max(0L, state.getInterpolatedPositionMs() + (long)HudConfig.INSTANCE.subtitlesOffsetMs + (long)trackOffset + this.lyricsManager.getLiveAcousticOffsetMs() + LyricsManager.PERCEPTUAL_LEAD_MS) : 0L;
        long now = System.currentTimeMillis();
        class_4587 matrices = context.matrices() != null ? context.matrices() : new class_4587();
        class_4597 consumers = context.consumers() != null ? context.consumers() : mc.method_22940().method_23000();
        if (matrices == null || consumers == null) {
            return;
        }
        class_4184 camera = mc.field_1773.method_19418();
        class_243 cameraPos = camera.method_71156();
        class_327 textRenderer = mc.field_1772;
        for (SubtitleBubble bubble : bubbles) {
            if (bubble.worldPos == null) continue;
            float overallAlpha = bubble.getOverallAlpha(currentPosMs, now);
            if (overallAlpha <= 0.005f) continue;
            float trackScale = state.isActive() ? HudConfig.INSTANCE.getTrackScale(state.artist(), state.title(), 1.0f) : 1.0f;
            float baseScale = (bubble.isSurfaceAttached ? 0.03f : 0.035f) * HudConfig.INSTANCE.subtitlesScale * trackScale;
            float animScale = bubble.getScale(now);
            matrices.method_22903();
            matrices.method_46416((float)(bubble.worldPos.field_1352 - cameraPos.field_1352), (float)(bubble.worldPos.field_1351 - cameraPos.field_1351) + (bubble.isSurfaceAttached ? 0.0f : bubble.getExitOffsetY(currentPosMs) * 0.02f), (float)(bubble.worldPos.field_1350 - cameraPos.field_1350));
            if (bubble.worldRot != null) {
                matrices.method_22907((Quaternionfc)bubble.worldRot);
            }
            matrices.method_22905(baseScale, -baseScale, baseScale);
            matrices.method_22905(animScale, animScale, animScale);
            double distSq = bubble.worldPos.method_1025(cameraPos);
            float proxFade = distSq < 0.64 ? (float)Math.sqrt(distSq) / 0.8f : 1.0f;
            float finalOverallAlpha = overallAlpha * proxFade;
            this.render3DSubtitleDoubleSided(matrices, consumers, textRenderer, bubble, currentPosMs, finalOverallAlpha);
            matrices.method_22909();
        }
    }

    private void render3DSubtitleDoubleSided(class_4587 matrices, class_4597 consumers, class_327 textRenderer, SubtitleBubble bubble, long currentPosMs, float overallAlpha) {
        Vector4f o = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).mul((Matrix4fc)matrices.method_23760().method_23761());
        Vector4f f = new Vector4f(0.0f, 0.0f, 1.0f, 1.0f).mul((Matrix4fc)matrices.method_23760().method_23761());
        float dOriginSq = o.x * o.x + o.y * o.y + o.z * o.z;
        float dFrontSq = f.x * f.x + f.y * f.y + f.z * f.z;
        boolean isFront = dFrontSq < dOriginSq;
        if (isFront) {
            this.draw3DFace(matrices, consumers, textRenderer, bubble, currentPosMs, overallAlpha);
        } else {
            matrices.method_22903();
            matrices.method_22907((Quaternionfc)new Quaternionf().rotationY((float)Math.PI));
            this.draw3DFace(matrices, consumers, textRenderer, bubble, currentPosMs, overallAlpha);
            matrices.method_22909();
        }
    }

    private void draw3DFace(class_4587 matrices, class_4597 consumers, class_327 textRenderer, SubtitleBubble bubble, long currentPosMs, float overallAlpha) {
        LyricLine line = bubble.line;
        String text = line.text;
        TrackState state = this.client.getState();
        String trackFont = state.isActive() ? HudConfig.INSTANCE.getTrackFont(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesFont) : HudConfig.INSTANCE.subtitlesFont;
        int max3DWidth = LyricLine.getMaxAllowedWidth3D();
        List<LyricLine.SubLine> subLines = line.getSubLines(textRenderer, max3DWidth, trackFont);
        Objects.requireNonNull(textRenderer);
        int fontHeight = 9;
        int lineHeight = fontHeight + 3;
        int glyphHeight = (subLines.size() - 1) * lineHeight + fontHeight;
        int maxWidth = 0;
        for (LyricLine.SubLine sl : subLines) {
            if (sl.width() <= maxWidth) continue;
            maxWidth = sl.width();
        }
        float padX = 5.0f;
        float padY = 2.5f;
        float startY = -((float)glyphHeight / 2.0f);
        float panelX = (float)(-maxWidth) / 2.0f - padX;
        float panelY = startY - padY;
        float panelW = (float)maxWidth + padX * 2.0f;
        float panelH = (float)glyphHeight + padY * 2.0f;
        String animMode = HudConfig.INSTANCE.subtitlesAnimation;
        long now = System.currentTimeMillis();
        long effectivePosMs = bubble.isLiveCaption ? (now - bubble.spawnSystemTimeMs) : currentPosMs;
        float trackScale = state.isActive() ? HudConfig.INSTANCE.getTrackScale(state.artist(), state.title(), 1.0f) : 1.0f;
        float baseScale = (bubble.isSurfaceAttached ? 0.03f : 0.035f) * HudConfig.INSTANCE.subtitlesScale * trackScale;
        class_310 mc = class_310.method_1551();
        if (!bubble.isSurfaceAttached && (bubble.charGroundTargetY == null || bubble.charGroundTargetY.length != text.length())) {
            bubble.charGroundTargetY = new float[text.length()];
            float defaultGroundY = 28.0f;
            if (("MISIDE_FALL".equalsIgnoreCase(animMode) || line.hasMisideAnim()) && bubble.worldPos != null && mc.field_1687 != null && mc.field_1724 != null) {
                for (int lineIdx = 0; lineIdx < subLines.size(); ++lineIdx) {
                    LyricLine.SubLine sl = subLines.get(lineIdx);
                    float y = startY + (float)lineIdx * (float)lineHeight;
                    float estimatedFall = Math.max(10.0f, (defaultGroundY - 1.2f) - (float)fontHeight - y);
                    int charX = -sl.width() / 2;
                    int start = Math.max(0, sl.charStart());
                    int end = Math.min(text.length(), sl.charEnd());
                    for (int i = start; i < end; ++i) {
                        char c = text.charAt(i);
                        String charFont = line.getCharFont(i);
                        if (charFont == null || charFont.isEmpty()) {
                            charFont = trackFont;
                        }
                        int charW = Math.round(LyricLine.measureCharWidth(textRenderer, c, charFont));
                        int charPosInLine = i - start;
                        int lineLength = Math.max(1, end - start);
                        float scatterX = line.getCharScatterX(i, line.endMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, estimatedFall);
                        float scatterZ = line.getCharScatterZ(i, line.endMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, estimatedFall);
                        float localCharX = (float)charX + (float)charW / 2.0f + scatterX;
                        float localCharZ = scatterZ;

                        Vector3f offset = new Vector3f(localCharX * baseScale, 0.0f, localCharZ * baseScale);
                        if (bubble.worldRot != null) {
                            bubble.worldRot.transform(offset);
                        }
                        double charWorldX = bubble.worldPos.field_1352 + (double)offset.x;
                        double charWorldY = bubble.worldPos.field_1351 + (double)offset.y;
                        double charWorldZ = bubble.worldPos.field_1350 + (double)offset.z;

                        class_243 startVec = new class_243(charWorldX, charWorldY + 0.8, charWorldZ);
                        class_243 downVec = new class_243(charWorldX, charWorldY - 32.0, charWorldZ);
                        class_3959 rayContext = new class_3959(startVec, downVec, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, (class_1297)mc.field_1724);
                        class_3965 hit = mc.field_1687.method_17742(rayContext);
                        double groundWorldY;
                        if (hit != null && hit.method_17783() == class_239.class_240.field_1332) {
                            groundWorldY = hit.method_17784().field_1351;
                        } else if (mc.field_1724 != null) {
                            class_243 playerStartVec = new class_243(charWorldX, mc.field_1724.method_23318() + 2.0, charWorldZ);
                            class_243 playerDownVec = new class_243(charWorldX, mc.field_1724.method_23318() - 32.0, charWorldZ);
                            class_3965 hit2 = mc.field_1687.method_17742(new class_3959(playerStartVec, playerDownVec, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, (class_1297)mc.field_1724));
                            if (hit2 != null && hit2.method_17783() == class_239.class_240.field_1332) {
                                groundWorldY = hit2.method_17784().field_1351;
                            } else {
                                groundWorldY = mc.field_1724.method_23318();
                            }
                        } else {
                            groundWorldY = charWorldY - 2.0;
                        }
                        double dropBlocks = Math.max(0.15, charWorldY - groundWorldY);
                        float charTargetGroundY = baseScale > 0.001f ? (float)(dropBlocks / (double)baseScale) : defaultGroundY;
                        int hash = ((i * 197 + 53) ^ (text.length() * 37)) & 0x7FFFFFFF;
                        float pileOffset = (float)(hash % 6) * 0.45f;
                        bubble.charGroundTargetY[i] = charTargetGroundY - pileOffset;
                        charX += charW;
                    }
                }
            } else {
                for (int i = 0; i < text.length(); ++i) {
                    bubble.charGroundTargetY[i] = defaultGroundY;
                }
            }
        }
        float avgTargetGroundY = 28.0f;
        if (!bubble.isSurfaceAttached && bubble.charGroundTargetY != null && bubble.charGroundTargetY.length > 0) {
            float sum = 0.0f;
            for (float g : bubble.charGroundTargetY) {
                sum += g;
            }
            avgTargetGroundY = sum / (float)bubble.charGroundTargetY.length;
        }
        float maxBgFall = bubble.isSurfaceAttached ? 0.0f : Math.max(0.0f, (avgTargetGroundY - 1.2f) - panelH - panelY);
        boolean seeThrough = HudConfig.INSTANCE.subtitlesSeeThrough;
        int bgAlpha = (int)(HudConfig.INSTANCE.subtitlesBgAlpha * overallAlpha * 230.0f);
        class_327.class_6415 layerType = seeThrough ? class_327.class_6415.field_33994 : class_327.class_6415.field_33993;
        int light = 0xF000F0;
        int textRgb = (state.isActive() ? HudConfig.INSTANCE.getTrackColor(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesColor) : HudConfig.INSTANCE.subtitlesColor) & 0xFFFFFF;
        boolean hasShadow = HudConfig.INSTANCE.subtitlesTextShadow;
        int activeColor = state.isActive() ? HudConfig.INSTANCE.getTrackActiveColor(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesActiveColor) : HudConfig.INSTANCE.subtitlesActiveColor;

        matrices.method_22903();
        matrices.method_46416(0.0f, 0.0f, 0.06f);
        Matrix4f textMatrix = new Matrix4f(matrices.method_23760().method_23761());
        for (int lineIdx = 0; lineIdx < subLines.size(); ++lineIdx) {
            LyricLine.SubLine sl = subLines.get(lineIdx);
            float y = startY + (float)lineIdx * (float)lineHeight;
            int charX = -sl.width() / 2;
            int start = Math.max(0, sl.charStart());
            int end = Math.min(text.length(), sl.charEnd());
            for (int i = start; i < end; ++i) {
                char c = text.charAt(i);
                String charFont = line.getCharFont(i);
                if (charFont == null || charFont.isEmpty()) {
                    charFont = trackFont;
                }
                boolean isCustom = CustomFontManager.isCustomFont(charFont);
                boolean isDefaultFont = !isCustom && (charFont == null || charFont.isEmpty() || "default".equalsIgnoreCase(charFont) || "minecraft:default".equalsIgnoreCase(charFont) || "minecraft".equalsIgnoreCase(charFont));
                boolean isRuneFont = !isCustom && (charFont != null && (charFont.contains("alt") || charFont.contains("illager")));
                String renderStr = isRuneFont ? transliterateForRune(c) : singleCharString(c);
                if (renderStr.isEmpty()) {
                    renderStr = " ";
                }
                CustomFontManager.CustomGlyph customGlyph = isCustom ? CustomFontManager.getGlyph(charFont, c) : null;
                net.minecraft.class_5250 charComp = null;
                int charW;
                if (isCustom) {
                    charW = customGlyph != null ? Math.max(1, Math.round(customGlyph.advance)) : 6;
                } else if (isDefaultFont) {
                    charW = textRenderer.method_1727(renderStr);
                } else {
                    String fontName = charFont.trim();
                    charComp = getCachedStyledChar(fontName, renderStr);
                    charW = textRenderer.method_27525(charComp);
                }
                float charAlpha = line.getCharAlpha(i, effectivePosMs);
                float finalAlpha = charAlpha * overallAlpha;
                if (finalAlpha > 0.005f) {
                    float charAnimY;
                    float tiltX;
                    float scatterX;
                    float scatterZ;
                    float randomYaw;

                    if (bubble.isSurfaceAttached) {
                        charAnimY = 0.0f;
                        tiltX = 0.0f;
                        scatterX = 0.0f;
                        scatterZ = 0.0f;
                        randomYaw = 0.0f;
                    } else {
                        float charTargetGround = bubble.charGroundTargetY != null && i < bubble.charGroundTargetY.length ? bubble.charGroundTargetY[i] : avgTargetGroundY;
                        float maxCharFall = Math.max(0.0f, (charTargetGround - 1.2f) - (float)fontHeight - y);
                        charAnimY = line.getCharAnimationOffsetY(i, effectivePosMs, now, animMode, maxCharFall);
                        tiltX = line.getCharAnimationTiltX(i, effectivePosMs, now, animMode, maxCharFall);
                        int charPosInLine = i - start;
                        int lineLength = Math.max(1, end - start);
                        scatterX = line.getCharScatterX(i, effectivePosMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, maxCharFall);
                        scatterZ = line.getCharScatterZ(i, effectivePosMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, maxCharFall);
                        randomYaw = line.getCharRandomYaw(i, effectivePosMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, maxCharFall);
                    }

                    int color = line.getCharColor(i, effectivePosMs, now, animMode, textRgb, finalAlpha, activeColor);
                    float drawY = y + charAnimY;
                    float drawX = (float)charX + scatterX;
                    int shadowAlpha = (int)(finalAlpha * 160.0f);
                    int shadowColor = shadowAlpha << 24 | 0x05060A;

                    boolean hasTransform = Math.abs(tiltX) > 0.01f || Math.abs(randomYaw) > 0.01f || Math.abs(scatterZ) > 0.01f;
                    if (hasTransform) {
                        matrices.method_22903();
                        matrices.method_46416(0.0f, 0.0f, scatterZ);
                        float pivotX = drawX + (float)charW / 2.0f;
                        float pivotY = drawY + (float)fontHeight;
                        matrices.method_46416(pivotX, pivotY, 0.0f);
                        if (randomYaw != 0.0f) {
                            matrices.method_22907((Quaternionfc)new Quaternionf().rotationY((float)Math.toRadians(randomYaw)));
                        }
                        if (tiltX != 0.0f) {
                            matrices.method_22907((Quaternionfc)new Quaternionf().rotationX((float)Math.toRadians(tiltX)));
                        }
                        matrices.method_46416(-pivotX, -pivotY, 0.0f);
                        if (hasShadow && shadowAlpha > 0) {
                            matrices.method_22903();
                            matrices.method_46416(0.0f, 0.0f, -0.015f);
                            if (isCustom) {
                                CustomFontManager.drawGlyph3D(matrices.method_23760().method_23761(), consumers, customGlyph, drawX + 0.8f, drawY + 0.8f, 0.0f, shadowColor, seeThrough, light);
                            } else if (isDefaultFont) {
                                textRenderer.method_27521(renderStr, drawX + 0.8f, drawY + 0.8f, shadowColor, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                            } else {
                                textRenderer.method_27522(charComp, drawX + 0.8f, drawY + 0.8f, shadowColor, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                            }
                            matrices.method_22909();
                        }
                        if (isCustom) {
                            CustomFontManager.drawGlyph3D(matrices.method_23760().method_23761(), consumers, customGlyph, drawX, drawY, 0.0f, color, seeThrough, light);
                        } else if (isDefaultFont) {
                            textRenderer.method_27521(renderStr, drawX, drawY, color, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                        } else {
                            textRenderer.method_27522(charComp, drawX, drawY, color, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                        }
                        matrices.method_46416(pivotX, pivotY, 0.0f);
                        matrices.method_22907((Quaternionfc)new Quaternionf().rotationY((float)Math.PI));
                        matrices.method_46416(-pivotX, -pivotY, 0.0f);
                        if (hasShadow && shadowAlpha > 0) {
                            matrices.method_22903();
                            matrices.method_46416(0.0f, 0.0f, -0.015f);
                            if (isCustom) {
                                CustomFontManager.drawGlyph3D(matrices.method_23760().method_23761(), consumers, customGlyph, drawX + 0.8f, drawY + 0.8f, 0.0f, shadowColor, seeThrough, light);
                            } else if (isDefaultFont) {
                                textRenderer.method_27521(renderStr, drawX + 0.8f, drawY + 0.8f, shadowColor, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                            } else {
                                textRenderer.method_27522(charComp, drawX + 0.8f, drawY + 0.8f, shadowColor, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                            }
                            matrices.method_22909();
                        }
                        if (isCustom) {
                            CustomFontManager.drawGlyph3D(matrices.method_23760().method_23761(), consumers, customGlyph, drawX, drawY, 0.0f, color, seeThrough, light);
                        } else if (isDefaultFont) {
                            textRenderer.method_27521(renderStr, drawX, drawY, color, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                        } else {
                            textRenderer.method_27522(charComp, drawX, drawY, color, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                        }
                        matrices.method_22909();
                    } else {
                        if (hasShadow && shadowAlpha > 0) {
                            matrices.method_22903();
                            matrices.method_46416(0.0f, 0.0f, -0.015f);
                            if (isCustom) {
                                CustomFontManager.drawGlyph3D(matrices.method_23760().method_23761(), consumers, customGlyph, (float)charX + 0.8f, drawY + 0.8f, 0.0f, shadowColor, seeThrough, light);
                            } else if (isDefaultFont) {
                                textRenderer.method_27521(renderStr, (float)charX + 0.8f, drawY + 0.8f, shadowColor, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                            } else {
                                textRenderer.method_27522(charComp, (float)charX + 0.8f, drawY + 0.8f, shadowColor, false, matrices.method_23760().method_23761(), consumers, layerType, 0, light);
                            }
                            matrices.method_22909();
                        }
                        if (isCustom) {
                            CustomFontManager.drawGlyph3D(textMatrix, consumers, customGlyph, (float)charX, drawY, 0.0f, color, seeThrough, light);
                        } else if (isDefaultFont) {
                            textRenderer.method_27521(renderStr, (float)charX, drawY, color, false, textMatrix, consumers, layerType, 0, light);
                        } else {
                            textRenderer.method_27522(charComp, (float)charX, drawY, color, false, textMatrix, consumers, layerType, 0, light);
                        }
                    }
                }
                charX += charW;
            }
        }
        matrices.method_22909();
    }

    public void render2D(class_332 context, class_9779 tickCounter) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1755 != null) {
            return;
        }
        this.render2DInternal(context, -1, -1);
    }

    public void render2DOverScreen(class_332 context, int mouseX, int mouseY) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1755 instanceof com.example.yandexmusichud.hud.HudEditScreen || mc.field_1755 instanceof com.example.yandexmusichud.hud.LyricEditorScreen) {
            return;
        }
        this.render2DInternal(context, mouseX, mouseY);
    }

    private void render2DInternal(class_332 context, int mouseX, int mouseY) {
        class_310 mc = class_310.method_1551();
        List<SubtitleBubble> activeBubbles = this.lyricsManager.getActiveBubbles();
        boolean hasLive2DBubbles = false;
        for (SubtitleBubble b : activeBubbles) {
            if (b.isLiveCaption && b.worldPos == null) {
                hasLive2DBubbles = true;
                break;
            }
        }
        if (mc.field_1724 == null && !hasLive2DBubbles) {
            return;
        }
        if (mc.field_1724 != null && mc.field_1690.field_1842 && !HudConfig.INSTANCE.subtitlesVisibleInF1) {
            return;
        }
        TrackState state = this.client.getState();
        int screenWidth = mc.method_22683().method_4486();
        int screenHeight = mc.method_22683().method_4502();
        String effectiveMode = this.lyricsManager.resolveEffectiveMode(state);
        boolean is2DMode = !"CHAT".equalsIgnoreCase(effectiveMode) && !"WORLD_3D".equalsIgnoreCase(effectiveMode) && !"WORLD_BLOCK".equalsIgnoreCase(effectiveMode) && !"WORLD_SURFACE".equalsIgnoreCase(effectiveMode);
        if (!HudConfig.INSTANCE.subtitlesEnabled || (!is2DMode && !hasLive2DBubbles)) {
            return;
        }
        if ((!state.isActive() || "stopped".equalsIgnoreCase(state.status())) && !hasLive2DBubbles) {
            return;
        }
        long now = System.currentTimeMillis();
        long nowNs = System.nanoTime();
        float dt = this.last2DFrameTimeNs == 0L ? (1.0f / 60.0f) : Math.max(0.001f, Math.min(0.1f, (float)(nowNs - this.last2DFrameTimeNs) / 1.0E9f));
        this.last2DFrameTimeNs = nowNs;
        float dt60 = Math.min(3.0f, dt * 60.0f);

        if (mouseX >= 0 && mouseY >= 0) {
            if (this.lastMouseX >= 0.0 && this.lastMouseY >= 0.0 && (now - this.lastMouseTime) < 200L && (now - this.lastMouseTime) > 0L) {
                float rawVx = (float)(mouseX - this.lastMouseX);
                float rawVy = (float)(mouseY - this.lastMouseY);
                this.mouseVelX = this.mouseVelX * 0.4f + rawVx * 0.6f;
                this.mouseVelY = this.mouseVelY * 0.4f + rawVy * 0.6f;
            } else {
                this.mouseVelX *= (float)Math.pow(0.85, dt60);
                this.mouseVelY *= (float)Math.pow(0.85, dt60);
            }
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.lastMouseTime = now;
        } else {
            this.lastMouseX = -1.0;
            this.lastMouseY = -1.0;
            this.mouseVelX *= (float)Math.pow(0.85, dt60);
            this.mouseVelY *= (float)Math.pow(0.85, dt60);
        }
        int trackOffset = state.isActive() ? HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title()) : 0;
        long currentPosMs = state.isActive() ? Math.max(0L, state.getInterpolatedPositionMs() + (long)HudConfig.INSTANCE.subtitlesOffsetMs + (long)trackOffset + this.lyricsManager.getLiveAcousticOffsetMs() + LyricsManager.PERCEPTUAL_LEAD_MS) : 0L;
        boolean isLmbDown = false;
        if (mc.method_22683() != null) {
            long handle = mc.method_22683().method_4490();
            if (handle != 0L) {
                isLmbDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
        }
        if (!isLmbDown && this.draggedBubble != null) {
            this.handleMouseRelease();
        }
        if (this.draggedBubble != null && !activeBubbles.contains(this.draggedBubble)) {
            this.handleMouseRelease();
        }
        class_327 textRenderer = mc.field_1772;
        float[] obstacleBounds = getScreenObstacleBounds(mc.field_1755, screenWidth, screenHeight);
        for (SubtitleBubble bubble : activeBubbles) {
            if (bubble.worldPos != null) continue;
            float overallAlpha = bubble.getOverallAlpha(currentPosMs, now);
            if (overallAlpha <= 0.005f) continue;
            float trackScale = state.isActive() ? HudConfig.INSTANCE.getTrackScale(state.artist(), state.title(), 1.0f) : 1.0f;
            float scale = Math.max(0.2f, bubble.getScale(now) * HudConfig.INSTANCE.subtitlesScale * trackScale);
            float offX = bubble.getOffsetX(now);
            float offY = bubble.getOffsetY(now);
            context.method_51448().pushMatrix();
            context.method_51448().translate(bubble.screenX + offX, bubble.screenY + offY + bubble.getExitOffsetY(currentPosMs));
            context.method_51448().scale(scale, scale);
            this.render2DBubble(context, textRenderer, bubble, currentPosMs, overallAlpha, screenWidth, screenHeight, now, scale, offX, offY, obstacleBounds, mouseX, mouseY, dt60);
            context.method_51448().popMatrix();
        }
    }

    private void render2DBubble(class_332 context, class_327 textRenderer, SubtitleBubble bubble, long currentPosMs, float overallAlpha, int screenWidth, int screenHeight, long now, float scale, float offX, float offY, float[] obstacleBounds, int mouseX, int mouseY, float dt60) {
        LyricLine line = bubble.line;
        String text = line.text;
        TrackState state = this.client.getState();
        String trackFont = state.isActive() ? HudConfig.INSTANCE.getTrackFont(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesFont) : HudConfig.INSTANCE.subtitlesFont;
        int maxAllowedWidth = LyricLine.getMaxAllowedWidth2D(screenWidth);
        List<LyricLine.SubLine> subLines = line.getSubLines(textRenderer, maxAllowedWidth, trackFont);
        Objects.requireNonNull(textRenderer);
        int fontHeight = 9;
        int lineHeight = fontHeight + 3;
        int glyphHeight = (subLines.size() - 1) * lineHeight + fontHeight;
        int maxWidth = 0;
        for (LyricLine.SubLine sl : subLines) {
            if (sl.width() <= maxWidth) continue;
            maxWidth = sl.width();
        }
        int padX = 8;
        int padY = 4;
        String animMode = HudConfig.INSTANCE.subtitlesAnimation;
        long effectivePosMs = bubble.isLiveCaption ? (now - bubble.spawnSystemTimeMs) : currentPosMs;
        int screenH = screenHeight;
        float defaultFloorScreenY = (float)screenH - 4.0f;
        float defaultTargetGroundY = Math.max(10.0f, (defaultFloorScreenY - bubble.screenY) / scale);
        boolean screenPresent = (obstacleBounds != null);
        if (bubble.charGroundTargetY == null || bubble.charGroundTargetY.length != text.length() || bubble.hasAdaptedToScreen != screenPresent) {
            bubble.charGroundTargetY = new float[text.length()];
            bubble.hasAdaptedToScreen = screenPresent;
            for (int l = 0; l < subLines.size(); ++l) {
                LyricLine.SubLine sl = subLines.get(l);
                int slCharX = (maxWidth - sl.width()) / 2;
                int start = Math.max(0, sl.charStart());
                int end = Math.min(text.length(), sl.charEnd());
                for (int k = start; k < end; ++k) {
                    char c = text.charAt(k);
                    String charFont = line.getCharFont(k);
                    if (charFont == null || charFont.isEmpty()) {
                        charFont = trackFont;
                    }
                    int charW = Math.round(LyricLine.measureCharWidth(textRenderer, c, charFont));
                    float approxCharScreenX = (bubble.screenX + offX) + ((float)slCharX + (float)charW / 2.0f) * scale;
                    float charFloorScreenY = defaultFloorScreenY;
                    if (obstacleBounds != null && bubble.screenY < obstacleBounds[1] + 24.0f) {
                        if (approxCharScreenX >= obstacleBounds[0] - 2.0f && approxCharScreenX <= obstacleBounds[2] + 2.0f) {
                            charFloorScreenY = obstacleBounds[1] - 2.0f;
                        }
                    }
                    int hash = ((k * 197 + 53) ^ (text.length() * 37)) & 0x7FFFFFFF;
                    float pileOffset = (float)(hash % 5) * 0.6f;
                    float charGroundY = Math.max(10.0f, (charFloorScreenY - (bubble.screenY + offY)) / scale);
                    bubble.charGroundTargetY[k] = charGroundY - pileOffset;
                    slCharX += charW;
                }
            }
        }
        if (bubble.charPhysics == null || bubble.charPhysics.length != text.length()) {
            bubble.charPhysics = new SubtitleBubble.CharPhysics[text.length()];
            for (int k = 0; k < text.length(); ++k) {
                bubble.charPhysics[k] = new SubtitleBubble.CharPhysics();
            }
        }
        float targetGroundY = defaultTargetGroundY;
        if (bubble.charGroundTargetY.length > 0) {
            float sum = 0.0f;
            for (float g : bubble.charGroundTargetY) {
                sum += g;
            }
            targetGroundY = sum / (float)bubble.charGroundTargetY.length;
        }
        float maxBgFall = Math.max(0.0f, targetGroundY - (float)(glyphHeight + padY * 2));
        int textRgb = (state.isActive() ? HudConfig.INSTANCE.getTrackColor(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesColor) : HudConfig.INSTANCE.subtitlesColor) & 0xFFFFFF;
        boolean hasShadow = HudConfig.INSTANCE.subtitlesTextShadow;
        int activeColor = state.isActive() ? HudConfig.INSTANCE.getTrackActiveColor(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesActiveColor) : HudConfig.INSTANCE.subtitlesActiveColor;
        for (int lineIdx = 0; lineIdx < subLines.size(); ++lineIdx) {
            LyricLine.SubLine sl = subLines.get(lineIdx);
            int yOffset = lineIdx * lineHeight;
            int charX = (maxWidth - sl.width()) / 2;
            int start = Math.max(0, sl.charStart());
            int end = Math.min(text.length(), sl.charEnd());
            for (int i = start; i < end; ++i) {
                char c = text.charAt(i);
                String charFont = line.getCharFont(i);
                if (charFont == null || charFont.isEmpty()) {
                    charFont = trackFont;
                }
                boolean isCustom = CustomFontManager.isCustomFont(charFont);
                boolean isDefaultFont = !isCustom && (charFont == null || charFont.isEmpty() || "default".equalsIgnoreCase(charFont) || "minecraft:default".equalsIgnoreCase(charFont) || "minecraft".equalsIgnoreCase(charFont));
                boolean isRuneFont = !isCustom && (charFont != null && (charFont.contains("alt") || charFont.contains("illager")));
                String renderStr = isRuneFont ? transliterateForRune(c) : singleCharString(c);
                if (renderStr.isEmpty()) {
                    renderStr = " ";
                }
                CustomFontManager.CustomGlyph customGlyph = isCustom ? CustomFontManager.getGlyph(charFont, c) : null;
                net.minecraft.class_5250 charComp = null;
                int charW;
                if (isCustom) {
                    charW = customGlyph != null ? Math.max(1, Math.round(customGlyph.advance)) : 6;
                } else if (isDefaultFont) {
                    charW = textRenderer.method_1727(renderStr);
                } else {
                    String fontName = charFont.trim();
                    charComp = getCachedStyledChar(fontName, renderStr);
                    charW = textRenderer.method_27525(charComp);
                }
                float charAlpha = line.getCharAlpha(i, effectivePosMs);
                float finalAlpha = charAlpha * overallAlpha;
                if (finalAlpha > 0.005f) {
                    int charPosInLine = i - start;
                    int lineLength = Math.max(1, end - start);
                    float line2DGroundOffset = (float)(subLines.size() - 1 - lineIdx) * 11.0f;
                    float charTargetGround = (bubble.charGroundTargetY != null && i < bubble.charGroundTargetY.length ? bubble.charGroundTargetY[i] : targetGroundY) - line2DGroundOffset;
                    float maxCharFall = Math.max(0.0f, charTargetGround - (float)fontHeight - (float)yOffset);
                    float charAnimY = line.getCharAnimationOffsetY(i, effectivePosMs, now, animMode, maxCharFall);
                    float tiltX = 0.0f;
                    float scatterX = line.getCharScatterX(i, effectivePosMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, maxCharFall);
                    int color = line.getCharColor(i, effectivePosMs, now, animMode, textRgb, finalAlpha, activeColor);
                    int baseY = (int)((float)yOffset + charAnimY);
                    int drawCharX = charX + (int)scatterX;

                    SubtitleBubble.CharPhysics cp = bubble.charPhysics[i];
                    float baseCharCenterX = (bubble.screenX + offX) + ((float)drawCharX + (float)charW / 2.0f) * scale;
                    float baseCharCenterY = (bubble.screenY + offY + bubble.getExitOffsetY(currentPosMs)) + ((float)baseY + (float)fontHeight / 2.0f) * scale;
                    float curCharScreenX = baseCharCenterX + cp.extraX * scale;
                    float curCharScreenY = baseCharCenterY + cp.extraY * scale;

                    boolean isDraggingThis = (this.draggedBubble == bubble && this.draggedCharIndex == i);
                    if (isDraggingThis) {
                        cp.isDragging = true;
                        cp.touched = true;
                        float targetScreenX = (float)mouseX - this.dragGrabOffsetX;
                        float targetScreenY = (float)mouseY - this.dragGrabOffsetY;
                        cp.extraX = (targetScreenX - baseCharCenterX) / scale;
                        cp.extraY = (targetScreenY - baseCharCenterY) / scale;
                        cp.velX = Math.max(-14.0f, Math.min(14.0f, this.mouseVelX));
                        cp.velY = Math.max(-14.0f, Math.min(14.0f, this.mouseVelY));
                        cp.rot = 0.0f;
                        cp.rotVel = 0.0f;
                        bubble.lastInteractionTimeMs = now;
                        if (obstacleBounds != null) {
                            if (targetScreenY < obstacleBounds[1] + 16.0f && targetScreenX >= obstacleBounds[0] - 2.0f && targetScreenX <= obstacleBounds[2] + 2.0f) {
                                cp.fellOff = false;
                            } else {
                                cp.fellOff = true;
                            }
                        }
                    } else if (cp.touched || cp.velX != 0.0f || cp.velY != 0.0f || cp.extraX != 0.0f || cp.extraY != 0.0f) {
                        cp.extraX += (cp.velX / scale) * dt60;
                        cp.extraY += (cp.velY / scale) * dt60;
                        cp.velY += 0.42f * dt60;
                        cp.velX *= (float)Math.pow(0.88, dt60);
                        cp.rot = 0.0f;
                        cp.rotVel = 0.0f;

                        curCharScreenX = baseCharCenterX + cp.extraX * scale;
                        float targetScreenFloor = (float)screenH - 4.0f;
                        if (obstacleBounds != null && bubble.screenY < obstacleBounds[1] + 24.0f) {
                            if (!cp.fellOff && curCharScreenX >= obstacleBounds[0] - 2.0f && curCharScreenX <= obstacleBounds[2] + 2.0f) {
                                targetScreenFloor = obstacleBounds[1] - 2.0f;
                            } else {
                                cp.fellOff = true;
                            }
                        }

                        float localFloorY = (targetScreenFloor - (bubble.screenY + offY)) / scale - line2DGroundOffset;
                        float maxAllowedLocalY = localFloorY - (float)fontHeight;
                        float currentLocalY = (float)yOffset + charAnimY + cp.extraY;
                        if (currentLocalY >= maxAllowedLocalY) {
                            float pen = currentLocalY - maxAllowedLocalY;
                            cp.extraY -= pen;
                            if (cp.velY > 0.0f) {
                                if (cp.velY > 1.4f) {
                                    cp.velY = -cp.velY * 0.25f;
                                } else {
                                    cp.velY = 0.0f;
                                }
                            }
                            cp.velX *= (float)Math.pow(0.82, dt60);
                            if (Math.abs(cp.velX) < 0.03f) cp.velX = 0.0f;
                            if (Math.abs(cp.velY) < 0.03f) cp.velY = 0.0f;
                        }

                        if (curCharScreenX < 4.0f) {
                            cp.extraX += (4.0f - curCharScreenX) / scale;
                            cp.velX = Math.abs(cp.velX) * 0.5f;
                        } else if (curCharScreenX > (float)screenWidth - 4.0f) {
                            cp.extraX -= (curCharScreenX - ((float)screenWidth - 4.0f)) / scale;
                            cp.velX = -Math.abs(cp.velX) * 0.5f;
                        }
                    }

                    float renderCharX = (float)drawCharX + cp.extraX;
                    float renderCharY = (float)baseY + cp.extraY;
                    int shadowAlpha = (int)(finalAlpha * 155.0f);
                    int shadowColor = shadowAlpha << 24 | 0x05060A;

                    int rx = Math.round(renderCharX);
                    int ry = Math.round(renderCharY);
                    if (hasShadow && shadowAlpha > 0) {
                        if (isCustom) {
                            CustomFontManager.drawGlyph2D(context, customGlyph, rx + 1, ry + 1, shadowColor);
                        } else if (isDefaultFont) {
                            context.method_51433(textRenderer, renderStr, rx + 1, ry + 1, shadowColor, false);
                        } else {
                            context.method_51439(textRenderer, charComp, rx + 1, ry + 1, shadowColor, false);
                        }
                    }
                    if (isCustom) {
                        CustomFontManager.drawGlyph2D(context, customGlyph, rx, ry, color);
                    } else if (isDefaultFont) {
                        context.method_51433(textRenderer, renderStr, rx, ry, color, false);
                    } else {
                        context.method_51439(textRenderer, charComp, rx, ry, color, false);
                    }
                }
                charX += charW;
            }
        }
    }

    public boolean handleMousePress(double mouseX, double mouseY) {
        long now = System.currentTimeMillis();
        class_310 mc = class_310.method_1551();
        if (mc == null) return false;
        TrackState state = this.client.getState();
        int trackOffset = state.isActive() ? HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title()) : 0;
        long currentPosMs = state.isActive() ? Math.max(0L, state.getInterpolatedPositionMs() + (long)HudConfig.INSTANCE.subtitlesOffsetMs + (long)trackOffset + this.lyricsManager.getLiveAcousticOffsetMs() + LyricsManager.PERCEPTUAL_LEAD_MS) : 0L;
        class_327 textRenderer = mc.field_1772;
        if (textRenderer == null) return false;
        int screenWidth = mc.method_22683().method_4486();
        String animMode = HudConfig.INSTANCE.subtitlesAnimation;

        SubtitleBubble bestBubble = null;
        int bestCharIndex = -1;
        float bestDistSq = Float.MAX_VALUE;
        float bestGrabOffsetX = 0.0f;
        float bestGrabOffsetY = 0.0f;

        for (SubtitleBubble bubble : this.lyricsManager.getActiveBubbles()) {
            if (bubble.worldPos != null || bubble.charPhysics == null) continue;
            float overallAlpha = bubble.getOverallAlpha(currentPosMs, now);
            if (overallAlpha <= 0.005f) continue;
            float trackScale = state.isActive() ? HudConfig.INSTANCE.getTrackScale(state.artist(), state.title(), 1.0f) : 1.0f;
            float scale = Math.max(0.2f, bubble.getScale(now) * HudConfig.INSTANCE.subtitlesScale * trackScale);
            float offX = bubble.getOffsetX(now);
            float offY = bubble.getOffsetY(now);
            LyricLine line = bubble.line;
            if (line == null || line.text == null) continue;
            String trackFont = state.isActive() ? HudConfig.INSTANCE.getTrackFont(state.artist(), state.title(), HudConfig.INSTANCE.subtitlesFont) : HudConfig.INSTANCE.subtitlesFont;
            int maxAllowedWidth = LyricLine.getMaxAllowedWidth2D(screenWidth);
            List<LyricLine.SubLine> subLines = line.getSubLines(textRenderer, maxAllowedWidth, trackFont);
            int fontHeight = 9;
            int lineHeight = fontHeight + 3;
            int maxWidth = 0;
            for (LyricLine.SubLine sl : subLines) {
                if (sl.width() > maxWidth) maxWidth = sl.width();
            }
            long effectivePosMs = bubble.isLiveCaption ? (now - bubble.spawnSystemTimeMs) : currentPosMs;

            for (int lineIdx = 0; lineIdx < subLines.size(); ++lineIdx) {
                LyricLine.SubLine sl = subLines.get(lineIdx);
                int yOffset = lineIdx * lineHeight;
                int charX = (maxWidth - sl.width()) / 2;
                int start = Math.max(0, sl.charStart());
                int end = Math.min(line.text.length(), sl.charEnd());
                for (int i = start; i < end; ++i) {
                    if (i >= bubble.charPhysics.length) break;
                    char c = line.text.charAt(i);
                    String charFont = line.getCharFont(i);
                    if (charFont == null || charFont.isEmpty()) {
                        charFont = trackFont;
                    }
                    int charW = Math.round(LyricLine.measureCharWidth(textRenderer, c, charFont));
                    SubtitleBubble.CharPhysics cp = bubble.charPhysics[i];
                    int charPosInLine = i - start;
                    int lineLength = Math.max(1, end - start);
                    float line2DGroundOffset = (float)(subLines.size() - 1 - lineIdx) * 11.0f;
                    float charTargetGround = (bubble.charGroundTargetY != null && i < bubble.charGroundTargetY.length ? bubble.charGroundTargetY[i] : 200.0f) - line2DGroundOffset;
                    float maxCharFall = Math.max(0.0f, charTargetGround - (float)fontHeight - (float)yOffset);
                    float charAnimY = line.getCharAnimationOffsetY(i, effectivePosMs, now, animMode, maxCharFall);
                    float scatterX = line.getCharScatterX(i, effectivePosMs, animMode, lineIdx, subLines.size(), charPosInLine, lineLength, maxCharFall);

                    float baseCharCenterX = (bubble.screenX + offX) + ((float)charX + scatterX + (float)charW / 2.0f) * scale;
                    float baseCharCenterY = (bubble.screenY + offY + bubble.getExitOffsetY(currentPosMs)) + ((float)yOffset + charAnimY + (float)fontHeight / 2.0f) * scale;
                    float curCharScreenX = baseCharCenterX + cp.extraX * scale;
                    float curCharScreenY = baseCharCenterY + cp.extraY * scale;

                    float dx = (float)mouseX - curCharScreenX;
                    float dy = (float)mouseY - curCharScreenY;
                    float distSq = dx * dx + dy * dy;
                    float hitRadius = Math.max(14.0f, ((float)charW + 6.0f) * scale);
                    if (distSq <= hitRadius * hitRadius && distSq < bestDistSq) {
                        if (line.isCharPronounced(i, effectivePosMs)) {
                            bestDistSq = distSq;
                            bestBubble = bubble;
                            bestCharIndex = i;
                            bestGrabOffsetX = dx;
                            bestGrabOffsetY = dy;
                        }
                    }
                    charX += charW;
                }
            }
        }

        if (bestBubble != null && bestCharIndex >= 0) {
            this.draggedBubble = bestBubble;
            this.draggedCharIndex = bestCharIndex;
            this.dragGrabOffsetX = bestGrabOffsetX;
            this.dragGrabOffsetY = bestGrabOffsetY;
            SubtitleBubble.CharPhysics cp = bestBubble.charPhysics[bestCharIndex];
            cp.isDragging = true;
            cp.touched = true;
            cp.velX = 0.0f;
            cp.velY = 0.0f;
            cp.rot = 0.0f;
            cp.rotVel = 0.0f;
            bestBubble.lastInteractionTimeMs = now;
            return true;
        }
        return false;
    }

    public void handleMouseRelease() {
        if (this.draggedBubble != null && this.draggedCharIndex >= 0) {
            if (this.draggedBubble.charPhysics != null && this.draggedCharIndex < this.draggedBubble.charPhysics.length) {
                SubtitleBubble.CharPhysics cp = this.draggedBubble.charPhysics[this.draggedCharIndex];
                if (cp != null) {
                    cp.isDragging = false;
                    cp.velX = Math.max(-14.0f, Math.min(14.0f, this.mouseVelX));
                    cp.velY = Math.max(-14.0f, Math.min(14.0f, this.mouseVelY));
                    cp.rot = 0.0f;
                    cp.rotVel = 0.0f;
                }
            }
        }
        this.draggedBubble = null;
        this.draggedCharIndex = -1;
    }

    public void handleMouseClick(double mouseX, double mouseY) {
        this.handleMousePress(mouseX, mouseY);
    }

    public static String transliterateForRune(char c) {
        return switch (c) {
            case '\u0430', '\u0410' -> "a";
            case '\u0431', '\u0411' -> "b";
            case '\u0432', '\u0412' -> "v";
            case '\u0433', '\u0413' -> "g";
            case '\u0434', '\u0414' -> "d";
            case '\u0435', '\u0415', '\u0451', '\u0401', '\u044d', '\u042d' -> "e";
            case '\u0436', '\u0416' -> "zh";
            case '\u0437', '\u0417' -> "z";
            case '\u0438', '\u0418', '\u0439', '\u0419' -> "i";
            case '\u043a', '\u041a' -> "k";
            case '\u043b', '\u041b' -> "l";
            case '\u043c', '\u041c' -> "m";
            case '\u043d', '\u041d' -> "n";
            case '\u043e', '\u041e' -> "o";
            case '\u043f', '\u041f' -> "p";
            case '\u0440', '\u0420' -> "r";
            case '\u0441', '\u0421' -> "s";
            case '\u0442', '\u0422' -> "t";
            case '\u0443', '\u0423' -> "u";
            case '\u0444', '\u0424' -> "f";
            case '\u0445', '\u0425' -> "kh";
            case '\u0446', '\u0426' -> "ts";
            case '\u0447', '\u0427' -> "ch";
            case '\u0448', '\u0428' -> "sh";
            case '\u0449', '\u0429' -> "sch";
            case '\u044a', '\u042a', '\u044c', '\u042c' -> "";
            case '\u044b', '\u042b' -> "y";
            case '\u044e', '\u042e' -> "yu";
            case '\u044f', '\u042f' -> "ya";
            default -> String.valueOf(c);
        };
    }
}

