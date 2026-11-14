package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.donationalerts.DonationAlertBanner;
import com.example.yandexmusichud.donationalerts.DonationAlertQueue;
import com.example.yandexmusichud.favorites.FavoritesManager;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.example.yandexmusichud.texture.CustomBackgroundManager;
import com.example.yandexmusichud.twitch.TwitchAlertBanner;
import com.example.yandexmusichud.twitch.TwitchAlertQueue;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_9779;

public class MusicHudRenderer {
    private static final int MAX_PRECOMPUTED_RADIUS = 64;
    private static final int[][] ROUND_DX = new int[MAX_PRECOMPUTED_RADIUS + 1][];

    static {
        for (int r = 1; r <= MAX_PRECOMPUTED_RADIUS; ++r) {
            int[] row = new int[r];
            double rr = (double)r * (double)r;
            for (int i = 0; i < r; ++i) {
                double d = (double)r - (double)i - 0.5;
                row[i] = (int)Math.round((double)r - Math.sqrt(Math.max(0.0, rr - d * d)));
            }
            ROUND_DX[r] = row;
        }
    }

    private static int getRoundDx(int radius, int index) {
        if (radius <= 0 || index < 0 || index >= radius) {
            return 0;
        }
        if (radius <= MAX_PRECOMPUTED_RADIUS) {
            return ROUND_DX[radius][index];
        }
        double d = (double)radius - (double)index - 0.5;
        return (int)Math.round((double)radius - Math.sqrt(Math.max(0.0, (double)radius * (double)radius - d * d)));
    }

    private static String formatSpeed(float speed, boolean withSpace) {
        int hundredths = Math.round(speed * 100.0f);
        int whole = hundredths / 100;
        int frac = Math.abs(hundredths % 100);
        return (withSpace ? "⚡ " : "⚡") + whole + (frac < 10 ? ".0" : ".") + frac + "x";
    }

    private final NowPlayingClient client;
    private final CoverTextureManager coverManager;
    private static final class_2960 TWITCH_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/twitch_icon.png");
    private static final class_2960 TEX_VINYL = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_record.png");
    private static final class_2960 TEX_SPINDLE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_spindle.png");
    private float currentAlpha = 0.0f;
    private float currentSlide = 0.0f;
    private float vinylSlideProgress = 0.0f;
    private float vinylRotationAngle = 0.0f;
    private float hoverControlsAlpha = 0.0f;
    private float lastFrameDt = 0.016f;
    private long lastActiveTime = 0L;
    private long marqueeStartTime = 0L;
    private long popupStartTime = 0L;
    private String lastTitle = "";
    private String lastArtist = "";
    private boolean lastWasPlaying = false;
    private long lastTimePosSec = -1L;
    private long lastTimeDurSec = -1L;
    private String lastTimePairStr = "0:00 / 0:00";
    private long lastSingleTimeSec = -1L;
    private String lastSingleTimeStr = "0:00";
    private int cachedX;
    private int cachedY;
    public static int lastCancelBtnScreenX = 0;
    public static int lastCancelBtnScreenY = 0;
    public static int lastCancelBtnScreenW = 0;
    public static int lastCancelBtnScreenH = 0;
    public static boolean lastCancelBtnVisible = false;
    public static boolean lastMicroControlsVisible = false;
    public static final int[] lastMicroBtnScreenX = new int[5];
    public static final int[] lastMicroBtnScreenY = new int[5];
    public static final int[] lastMicroBtnScreenW = new int[5];
    public static final int[] lastMicroBtnScreenH = new int[5];
    private long lastRenderTime = 0L;
    private long lastEqUpdateTime = 0L;
    private final float[] eqBarHeights = new float[12];
    private final float[] eqPeakHeights = new float[12];
    private long trackChangeTimestamp = 0L;
    private float karaokeScrollX = 0.0f;
    private com.example.yandexmusichud.lyrics.LyricLine lastKaraokeLine = null;

    public MusicHudRenderer(NowPlayingClient client, CoverTextureManager coverManager) {
        this.client = client;
        this.coverManager = coverManager;
    }

    public boolean handleMouseClick(double mouseX, double mouseY) {
        if (!lastMicroControlsVisible || this.client == null) {
            return false;
        }
        TrackState state = this.client.getState();
        if (state == null || !state.isActive()) {
            return false;
        }
        for (int i = 0; i < 5; ++i) {
            if (mouseX >= (double)lastMicroBtnScreenX[i] && mouseX <= (double)(lastMicroBtnScreenX[i] + lastMicroBtnScreenW[i]) &&
                mouseY >= (double)lastMicroBtnScreenY[i] && mouseY <= (double)(lastMicroBtnScreenY[i] + lastMicroBtnScreenH[i])) {
                class_310 mc = class_310.method_1551();
                if (mc != null && mc.method_1483() != null) {
                    mc.method_1483().method_4873((class_1113)class_1109.method_4757(class_3417.field_14709, 1.25f, 0.8f));
                }
                switch (i) {
                    case 0 -> this.client.sendPreviousAsync();
                    case 1 -> this.client.sendPlayPauseAsync();
                    case 2 -> this.client.sendNextAsync();
                    case 3 -> {
                        if (!state.title().isEmpty()) {
                            boolean isFav = FavoritesManager.INSTANCE.toggleFavorite(state, this.coverManager);
                            if (mc != null && mc.field_1705 != null) {
                                String msg = isFav
                                    ? "§b[Музыка] §eДобавлено в избранное: §f" + state.artist() + " - " + state.title()
                                    : "§b[Музыка] §7Удалено из избранного: §f" + state.artist() + " - " + state.title();
                                mc.field_1705.method_1758((class_2561)class_2561.method_43470(msg), false);
                            }
                        }
                    }
                    case 4 -> {
                        HudConfig.INSTANCE.hudMiniKaraoke = !HudConfig.INSTANCE.hudMiniKaraoke;
                        HudConfig.save();
                        if (mc != null && mc.field_1705 != null) {
                            String msg = HudConfig.INSTANCE.hudMiniKaraoke
                                ? "§b[Музыка] §aМини-караоке в карточке: ВКЛ"
                                : "§b[Музыка] §7Мини-караоке в карточке: ВЫКЛ";
                            mc.field_1705.method_1758((class_2561)class_2561.method_43470(msg), false);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void render(class_332 context, class_9779 tickCounter) {
        float targetSlide;
        float targetAlpha;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1690.field_1842) {
            lastMicroControlsVisible = false;
            lastCancelBtnVisible = false;
            return;
        }
        TwitchAlertQueue.INSTANCE.update();
        DonationAlertQueue.INSTANCE.update();
        boolean hasTwitchAlert = TwitchAlertQueue.INSTANCE.isShowing();
        boolean hasDaAlert = DonationAlertQueue.INSTANCE.isShowing();

        int screenWidth = mc.method_22683().method_4486();
        int screenHeight = mc.method_22683().method_4502();
        long now = System.currentTimeMillis();

        if (hasTwitchAlert) {
            TwitchAlertBanner.INSTANCE.render(context, screenWidth, screenHeight, now);
        }
        if (hasDaAlert) {
            DonationAlertBanner.INSTANCE.render(context, screenWidth, screenHeight, now);
        }

        if (!HudConfig.INSTANCE.enabled) {
            lastMicroControlsVisible = false;
            lastCancelBtnVisible = false;
            return;
        }
        TrackState state = this.client.getState();
        boolean isNewTrack = !state.title().equals(this.lastTitle) || !state.artist().equals(this.lastArtist);
        boolean justStarted = !this.lastWasPlaying && state.isPlaying();
        if (state.isActive() && (isNewTrack || justStarted)) {
            this.popupStartTime = now;
            this.trackChangeTimestamp = now;
        }
        this.lastWasPlaying = state.isPlaying();
        boolean isPopupMode = "POPUP".equalsIgnoreCase(HudConfig.INSTANCE.hudDisplayType);
        if (state.isActive() && state.isPlaying()) {
            this.lastActiveTime = now;
            if (isPopupMode) {
                long popupAge = now - this.popupStartTime;
                long maxPopupDuration = (long)HudConfig.INSTANCE.popupDurationSeconds * 1000L;
                if (popupAge < maxPopupDuration) {
                    targetAlpha = 1.0f;
                    targetSlide = 1.0f;
                } else {
                    targetAlpha = 0.0f;
                    targetSlide = 0.0f;
                }
            } else {
                targetAlpha = 1.0f;
                targetSlide = 1.0f;
            }
        } else if (state.isActive() && state.isPaused()) {
            long pausedFor = now - this.lastActiveTime;
            if (pausedFor < (long)HudConfig.INSTANCE.hideDelaySeconds * 1000L) {
                targetAlpha = 0.72f;
                targetSlide = 1.0f;
            } else {
                targetAlpha = 0.0f;
                targetSlide = 0.0f;
            }
        } else {
            long inactiveFor = now - this.lastActiveTime;
            if (this.lastActiveTime > 0L && inactiveFor < 1500L) {
                targetAlpha = 0.3f;
                targetSlide = 1.0f;
            } else {
                targetAlpha = 0.0f;
                targetSlide = 0.0f;
            }
        }
        if (hasTwitchAlert) {
            targetAlpha = 1.0f;
            targetSlide = 1.0f;
            if (this.currentAlpha < 0.25f) {
                this.currentAlpha = 0.25f;
            }
            if (this.currentSlide < 0.25f) {
                this.currentSlide = 0.25f;
            }
        }
        float dt = this.lastRenderTime > 0L ? Math.min(0.08f, (float)(now - this.lastRenderTime) / 1000.0f) : 0.016f;
        this.lastFrameDt = dt;
        this.lastRenderTime = now;
        float lerpFactor = Math.min(1.0f, dt * (hasTwitchAlert ? 14.0f : 8.5f));
        this.currentAlpha += (targetAlpha - this.currentAlpha) * lerpFactor;
        this.currentSlide += (targetSlide - this.currentSlide) * lerpFactor;

        float targetVinylSlide = state.isActive() && state.isPlaying() ? 1.0f : (state.isActive() ? 0.22f : 0.0f);
        this.vinylSlideProgress += (targetVinylSlide - this.vinylSlideProgress) * Math.min(1.0f, dt * 7.5f);
        float rotSpeed = state.isActive() && state.isPlaying() ? 54.0f : 4.0f;
        this.vinylRotationAngle = (this.vinylRotationAngle + dt * rotSpeed) % 360.0f;

        if (this.currentAlpha < 0.005f && this.currentSlide < 0.005f && !hasTwitchAlert) {
            lastCancelBtnVisible = false;
            lastMicroControlsVisible = false;
            return;
        }
        int baseW = HudConfig.INSTANCE.getHudBaseWidth();
        int baseH = HudConfig.INSTANCE.getHudBaseHeight();
        float scale = class_3532.method_15363((float)HudConfig.INSTANCE.hudScale, (float)0.2f, (float)4.0f);
        int scaledWidth = (int)((float)baseW * scale);
        int scaledHeight = (int)((float)baseH * scale);
        int baseX = HudConfig.INSTANCE.getAbsoluteX(screenWidth, scaledWidth);
        int baseY = HudConfig.INSTANCE.getAbsoluteY(screenHeight, scaledHeight);
        baseX = class_3532.method_15340((int)baseX, (int)0, (int)Math.max(0, screenWidth - scaledWidth));
        baseY = class_3532.method_15340((int)baseY, (int)0, (int)Math.max(0, screenHeight - scaledHeight));
        float slideOffsetX = 0.0f;
        float slideOffsetY = 0.0f;
        if (isPopupMode) {
            float s = Math.max(0.0f, Math.min(1.0f, 1.0f - this.currentSlide));
            float hiddenFraction = s * s * (3.0f - 2.0f * s);
            int distToLeft = baseX;
            int distToRight = screenWidth - (baseX + scaledWidth);
            int distToTop = baseY;
            int distToBottom = screenHeight - (baseY + scaledHeight);
            int minDist = Math.min(Math.min(distToLeft, distToRight), Math.min(distToTop, distToBottom));
            if (minDist < 60) {
                if (minDist == distToRight) {
                    slideOffsetX = (float)(scaledWidth + 20) * hiddenFraction;
                } else if (minDist == distToLeft) {
                    slideOffsetX = (float)(-(scaledWidth + 20)) * hiddenFraction;
                } else {
                    slideOffsetY = minDist == distToTop ? (float)(-(scaledHeight + 20)) * hiddenFraction : (float)(scaledHeight + 20) * hiddenFraction;
                }
            }
        }
        long trackAge = now - this.trackChangeTimestamp;
        float trackGlide = 0.0f;
        if (trackAge < 260L) {
            float tp = (float)trackAge / 260.0f;
            trackGlide = (float)Math.sin(tp * Math.PI) * 1.8f;
        }
        float beatBounce = 0.0f;
        if (HudConfig.INSTANCE.beatBounce && state.isActive() && state.isPlaying()) {
            long trackPos = state.getInterpolatedPositionMs();
            float beatCycle = (float)(trackPos % 500L) / 500.0f;
            beatBounce = (float)Math.pow(Math.max(0.0, 1.0 - (double)beatCycle * 1.9), 2.4) * 1.4f;
        }
        int finalX = Math.round((float)baseX + slideOffsetX);
        int finalY = Math.round((float)baseY + slideOffsetY - (beatBounce + trackGlide) * scale);
        this.cachedX = finalX;
        this.cachedY = finalY;
        HudConfig.INSTANCE.hudWidth = scaledWidth;
        HudConfig.INSTANCE.hudHeight = scaledHeight;

        int winW = Math.max(1, mc.method_22683().method_4489());
        int winH = Math.max(1, mc.method_22683().method_4506());
        double mouseScreenX = mc.field_1729.method_1603() * (double)screenWidth / (double)winW;
        double mouseScreenY = mc.field_1729.method_1604() * (double)screenHeight / (double)winH;
        boolean canInteract = mc.field_1755 != null && !(mc.field_1755 instanceof HudEditScreen);
        boolean cardHovered = canInteract && state.isActive() &&
            mouseScreenX >= (double)finalX && mouseScreenX <= (double)(finalX + scaledWidth) &&
            mouseScreenY >= (double)finalY && mouseScreenY <= (double)(finalY + scaledHeight);
        float targetHoverAlpha = cardHovered ? 1.0f : 0.0f;
        this.hoverControlsAlpha += (targetHoverAlpha - this.hoverControlsAlpha) * Math.min(1.0f, dt * 14.0f);

        boolean mirrorLayout = HudConfig.INSTANCE.autoAdaptLayout && baseX < screenWidth / 2;

        context.method_51448().pushMatrix();
        context.method_51448().translate((float)finalX, (float)finalY);
        context.method_51448().scale(scale, scale);
        int alpha = (int)(this.currentAlpha * 255.0f);
        this.renderBackground(context, alpha, baseW, baseH, mirrorLayout);
        if (state.isActive()) {
            this.renderContent(context, state, mc, alpha, now, mirrorLayout, baseW, baseH, beatBounce, finalX, finalY, scale, mouseScreenX, mouseScreenY);
            boolean isMismatched = com.example.yandexmusichud.YandexMusicHudMod.INSTANCE != null &&
                com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getLyricsManager() != null &&
                com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getLyricsManager().isCurrentTrackMismatched() &&
                !com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getLyricsManager().isCurrentTrackDisabled();
            boolean showCancelBtn = state.isPlaying() && HudConfig.INSTANCE.subtitlesEnabled && isMismatched && alpha > 30;
            if (showCancelBtn) {
                String fullText = "§e⚠ §fСубтитры не сходятся §c✕";
                int textW = mc.field_1772.method_1727(fullText);
                int btnW = textW + 14;
                int btnH = 15;
                int btnX = mirrorLayout ? 4 : baseW - 4 - btnW;
                int btnY = baseH + 4;
                if (finalY + (int)((float)(btnY + btnH) * scale) > screenHeight - 2 && finalY - (int)((float)(btnH + 4) * scale) >= 2) {
                    btnY = -btnH - 4;
                }
                int scrX = finalX + (int)((float)btnX * scale);
                int scrY = finalY + (int)((float)btnY * scale);
                int scrW = (int)((float)btnW * scale);
                int scrH = (int)((float)btnH * scale);
                lastCancelBtnScreenX = scrX;
                lastCancelBtnScreenY = scrY;
                lastCancelBtnScreenW = scrW;
                lastCancelBtnScreenH = scrH;
                lastCancelBtnVisible = true;
                boolean isHovered = (mc.field_1755 != null) && mouseScreenX >= (double)scrX && mouseScreenX <= (double)(scrX + scrW) && mouseScreenY >= (double)scrY && mouseScreenY <= (double)(scrY + scrH);
                if (isHovered) {
                    fullText = "§c✕ §fОтменить субтитры §e⚠";
                }
                float pulse = (float)Math.sin((double)now / 180.0) * 0.5f + 0.5f;
                int pulseAlpha = (int)((float)alpha * (0.82f + pulse * 0.18f));
                int btnBg = isHovered ? (pulseAlpha << 24 | 0x3A1219) : ((int)((float)alpha * 0.85f) << 24 | 0x120A0E);
                int btnBorder = isHovered ? (pulseAlpha << 24 | 0xFF3B30) : ((int)((float)alpha * (0.50f + pulse * 0.40f)) << 24 | 0xEE5253);
                MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 5, btnBg);
                MusicHudRenderer.renderRoundedBorder(context, btnX, btnY, btnW, btnH, 5, btnBorder, btnBorder);
                int curTextW = mc.field_1772.method_1727(fullText);
                int textStartX = btnX + (btnW - curTextW) / 2;
                int textStartY = btnY + (btnH - 9) / 2;
                context.method_51433(mc.field_1772, fullText, textStartX, textStartY, alpha << 24 | 0xFFFFFF, true);
            } else {
                lastCancelBtnVisible = false;
            }
        } else if (hasTwitchAlert) {
            lastCancelBtnVisible = false;
            lastMicroControlsVisible = false;
            this.renderStandbyContent(context, mc, alpha, baseW, baseH);
        } else {
            lastCancelBtnVisible = false;
            lastMicroControlsVisible = false;
        }
        context.method_51448().popMatrix();
    }

    private void renderStandbyContent(class_332 context, class_310 mc, int alpha, int baseW, int baseH) {
        class_327 tr = mc.field_1772;
        int iconSize = 24;
        int iconX = 10;
        int iconY = (baseH - iconSize) / 2;
        try {
            int iconAlpha = (int)((float)alpha * 1.0f);
            context.method_25291(class_10799.field_56883, TWITCH_ICON, iconX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, iconAlpha << 24 | 0xFFFFFF);
        } catch (Throwable ignored) {}
        int textX = iconX + iconSize + 8;
        String title = "§d§lTwitch Live";
        String sub = "§7Интеграция активна";
        context.method_51433(tr, title, textX, baseH / 2 - 9, alpha << 24 | 0xFFFFFF, true);
        context.method_51433(tr, sub, textX, baseH / 2 + 2, (int)((float)alpha * 0.8f) << 24 | 0xCCCCCC, false);
    }

    private static int getEffectiveRadius(String style, int baseH) {
        if ("DYNAMIC_ISLAND".equalsIgnoreCase(style)) {
            return baseH / 2;
        }
        return class_3532.method_15340((int)HudConfig.INSTANCE.cornerRadius, (int)0, (int)20);
    }

    private void renderBackground(class_332 context, int alpha, int baseW, int baseH, boolean mirrorLayout) {
        String style = HudConfig.INSTANCE.hudStyle != null ? HudConfig.INSTANCE.hudStyle.toUpperCase(java.util.Locale.ROOT) : "MINIMAL_DARK";
        class_2960 customBg = CustomBackgroundManager.INSTANCE.getBackgroundTexture();
        int r = MusicHudRenderer.getEffectiveRadius(style, baseH);
        int rawDom = this.coverManager.getSmoothDominantColor() & 0xFFFFFF;
        int domColor = rawDom != 0 ? rawDom : 0x38BDF8;
        if (customBg != null) {
            float bgAlpha = class_3532.method_15363((float)HudConfig.INSTANCE.customBackgroundAlpha, (float)0.0f, (float)1.0f) * ((float)alpha / 255.0f);
            int bgAlphaInt = (int)(bgAlpha * 255.0f);
            if (bgAlphaInt > 2) {
                if (bgAlphaInt > 20) {
                    int auraR = Math.min(28, r + 3);
                    int auraColorOuter = (int)(bgAlpha * 38.0f) << 24 | 0x000000;
                    int auraColorInner = (int)(bgAlpha * 28.0f) << 24 | domColor;
                    MusicHudRenderer.renderRoundedRect(context, -3, -2, baseW + 6, baseH + 5, auraR, auraColorOuter);
                    MusicHudRenderer.renderRoundedRect(context, -1, -1, baseW + 2, baseH + 2, r + 1, auraColorInner);
                }
                context.method_25291(class_10799.field_56883, customBg, 0, 0, 0.0f, 0.0f, baseW, baseH, baseW, baseH, bgAlphaInt << 24 | 0xFFFFFF);
                int shineColor = (int)(bgAlpha * 68.0f) << 24 | 0xFFFFFF;
                int shadowColor = (int)(bgAlpha * 80.0f) << 24 | 0x000000;
                MusicHudRenderer.renderRoundedBorder(context, 0, 0, baseW, baseH, r, shineColor, shadowColor);
            }
            return;
        }

        float bgAlpha = class_3532.method_15363((float)HudConfig.INSTANCE.hudBackgroundAlpha, (float)0.0f, (float)1.0f) * ((float)alpha / 255.0f);
        int bgAlphaInt = (int)(bgAlpha * 255.0f);
        if (bgAlphaInt <= 2) {
            return;
        }

        boolean isGlass = "GLASS_AMBIENT".equals(style);
        boolean isIsland = "DYNAMIC_ISLAND".equals(style);

        if (bgAlphaInt > 20) {
            int auraR = Math.min(30, r + 3);
            if (isGlass) {
                int auraOuter = (int)(bgAlpha * 24.0f) << 24 | domColor;
                int auraInner = (int)(bgAlpha * 50.0f) << 24 | domColor;
                MusicHudRenderer.renderRoundedRect(context, -4, -4, baseW + 8, baseH + 8, auraR + 1, auraOuter);
                MusicHudRenderer.renderRoundedRect(context, -2, -2, baseW + 4, baseH + 4, r + 2, auraInner);
            } else {
                int shadowOuter = (int)(bgAlpha * 52.0f) << 24 | 0x000000;
                int shadowInner = (int)(bgAlpha * 75.0f) << 24 | 0x000000;
                MusicHudRenderer.renderRoundedRect(context, -2, -1, baseW + 4, baseH + 4, auraR, shadowOuter);
                MusicHudRenderer.renderRoundedRect(context, -1, 0, baseW + 2, baseH + 2, r + 1, shadowInner);
            }
        }

        int baseRgb = HudConfig.INSTANCE.hudBackgroundColor & 0xFFFFFF;
        if (isIsland && baseRgb == 0x0A0D14) {
            baseRgb = 0x06080D;
        }
        MusicHudRenderer.renderRoundedRect(context, 0, 0, baseW, baseH, r, bgAlphaInt << 24 | baseRgb);

        if (isGlass) {
            int glowSpan = (int)((float)baseW * 0.72f);
            int maxGlowAlpha = (int)(bgAlpha * 95.0f);
            MusicHudRenderer.renderHorizontalAmbientGlow(context, 0, 0, baseW, baseH, r, domColor, maxGlowAlpha, glowSpan, mirrorLayout);
            int topShine = (int)(bgAlpha * 88.0f) << 24 | MusicHudRenderer.blendRgb(0xFFFFFF, domColor, 0.35f);
            int botBorder = (int)(bgAlpha * 56.0f) << 24 | MusicHudRenderer.blendRgb(0x1E293B, domColor, 0.45f);
            MusicHudRenderer.renderRoundedBorder(context, 0, 0, baseW, baseH, r, topShine, botBorder);
        } else if (isIsland) {
            int glowSpan = (int)((float)baseW * 0.42f);
            int maxGlowAlpha = (int)(bgAlpha * 32.0f);
            MusicHudRenderer.renderHorizontalAmbientGlow(context, 0, 0, baseW, baseH, r, domColor, maxGlowAlpha, glowSpan, mirrorLayout);
            int topShine = (int)(bgAlpha * 62.0f) << 24 | MusicHudRenderer.blendRgb(0x64748B, domColor, 0.30f);
            int botBorder = (int)(bgAlpha * 44.0f) << 24 | 0x1E293B;
            MusicHudRenderer.renderRoundedBorder(context, 0, 0, baseW, baseH, r, topShine, botBorder);
        } else {
            int glowSpan = (int)((float)baseW * 0.48f);
            int maxGlowAlpha = (int)(bgAlpha * 34.0f);
            MusicHudRenderer.renderHorizontalAmbientGlow(context, 0, 0, baseW, baseH, r, domColor, maxGlowAlpha, glowSpan, mirrorLayout);
            int topShine = (int)(bgAlpha * 54.0f) << 24 | 0x94A3B8;
            int botBorder = (int)(bgAlpha * 42.0f) << 24 | 0x1E293B;
            MusicHudRenderer.renderRoundedBorder(context, 0, 0, baseW, baseH, r, topShine, botBorder);

            int accentH = Math.max(12, baseH - Math.max(r * 2, 20));
            int accentY = (baseH - accentH) / 2;
            int accentX = mirrorLayout ? baseW - 3 : 1;
            int accentBarCol = (int)(bgAlpha * 195.0f) << 24 | domColor;
            MusicHudRenderer.renderRoundedRect(context, accentX, accentY, 2, accentH, 1, accentBarCol);
        }
    }

    public static void renderHorizontalAmbientGlow(class_332 context, int x, int y, int width, int height, int radius, int rgbColor, int maxAlpha, int spanWidth, boolean fromRight) {
        if (maxAlpha <= 2 || spanWidth <= 0 || width <= 0 || height <= 0) {
            return;
        }
        int steps = 14;
        float stepW = (float)spanWidth / (float)steps;
        int safeR = Math.min(radius, Math.min(width / 2, height / 2));
        for (int i = 0; i < steps; ++i) {
            float t = (float)i / (float)steps;
            float falloff = (1.0f - t) * (1.0f - t);
            int a = (int)((float)maxAlpha * falloff);
            if (a <= 1) continue;
            int sx1;
            int sx2;
            if (fromRight) {
                sx2 = x + width - Math.round((float)i * stepW);
                sx1 = x + width - Math.round((float)(i + 1) * stepW);
            } else {
                sx1 = x + Math.round((float)i * stepW);
                sx2 = x + Math.round((float)(i + 1) * stepW);
            }
            sx1 = Math.max(x + 1, sx1);
            sx2 = Math.min(x + width - 1, sx2);
            if (sx2 <= sx1) continue;

            int topPad = 1;
            int distEdge = fromRight ? (x + width - sx2) : (sx1 - x);
            if (safeR > 0 && distEdge < safeR) {
                topPad = Math.max(1, MusicHudRenderer.getRoundDx(safeR, distEdge));
            }
            int sy1 = y + topPad;
            int sy2 = y + height - topPad;
            if (sy2 > sy1) {
                context.method_25294(sx1, sy1, sx2, sy2, a << 24 | (rgbColor & 0xFFFFFF));
            }
        }
    }

    private static int blendRgb(int c1, int c2, float ratio) {
        float t = Math.max(0.0f, Math.min(1.0f, ratio));
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int r = (int)((float)r1 + (float)(r2 - r1) * t);
        int g = (int)((float)g1 + (float)(g2 - g1) * t);
        int b = (int)((float)b1 + (float)(b2 - b1) * t);
        return r << 16 | g << 8 | b;
    }

    public static void renderRoundedRect(class_332 context, int x, int y, int width, int height, int radius, int argbColor) {
        int a = argbColor >> 24 & 0xFF;
        if (a == 0) {
            return;
        }
        if (radius <= 0) {
            context.method_25294(x, y, x + width, y + height, argbColor);
            return;
        }
        radius = Math.min(radius, Math.min(width / 2, height / 2));
        context.method_25294(x, y + radius, x + width, y + height - radius, argbColor);
        for (int i = 0; i < radius; ++i) {
            int dx = MusicHudRenderer.getRoundDx(radius, i);
            context.method_25294(x + dx, y + i, x + width - dx, y + i + 1, argbColor);
            context.method_25294(x + dx, y + height - 1 - i, x + width - dx, y + height - i, argbColor);
        }
    }

    public static void renderRoundedBorder(class_332 context, int x, int y, int width, int height, int radius, int color) {
        renderRoundedBorder(context, x, y, width, height, radius, color, color);
    }

    public static void renderRoundedBorder(class_332 context, int x, int y, int width, int height, int radius, int topShine, int bottomDark) {
        if (radius <= 0) {
            context.method_25294(x, y, x + width, y + 1, topShine);
            context.method_25294(x, y, x + 1, y + height, topShine);
            context.method_25294(x, y + height - 1, x + width, y + height, bottomDark);
            context.method_25294(x + width - 1, y, x + width, y + height, bottomDark);
            return;
        }
        radius = Math.min(radius, Math.min(width / 2, height / 2));
        context.method_25294(x + radius, y, x + width - radius, y + 1, topShine);
        context.method_25294(x, y + radius, x + 1, y + height - radius, topShine);
        context.method_25294(x + radius, y + height - 1, x + width - radius, y + height, bottomDark);
        context.method_25294(x + width - 1, y + radius, x + width, y + height - radius, bottomDark);

        int prevDx = radius;
        for (int i = 0; i < radius; ++i) {
            int dx = MusicHudRenderer.getRoundDx(radius, i);
            int span = Math.max(1, prevDx - dx);
            context.method_25294(x + dx, y + i, x + dx + span, y + i + 1, topShine);
            context.method_25294(x + width - dx - span, y + i, x + width - dx, y + i + 1, topShine);
            context.method_25294(x + dx, y + height - 1 - i, x + dx + span, y + height - i, bottomDark);
            context.method_25294(x + width - dx - span, y + height - 1 - i, x + width - dx, y + height - i, bottomDark);
            prevDx = dx;
        }
    }

    public static void renderBottomRoundedRect(class_332 context, int x, int y, int width, int height, int radius, int argbColor) {
        int a = argbColor >> 24 & 0xFF;
        if (a == 0 || height <= 0 || width <= 0) {
            return;
        }
        if (radius <= 0) {
            context.method_25294(x, y, x + width, y + height, argbColor);
            return;
        }
        radius = Math.min(radius, Math.min(width / 2, height));
        if (height > radius) {
            context.method_25294(x, y, x + width, y + height - radius, argbColor);
        }
        for (int i = 0; i < radius; ++i) {
            int dx = MusicHudRenderer.getRoundDx(radius, i);
            int lineY = y + height - radius + i;
            if (lineY >= y && lineY < y + height) {
                context.method_25294(x + dx, lineY, x + width - dx, lineY + 1, argbColor);
            }
        }
    }

    public static void renderBottomRoundedBorder(class_332 context, int x, int y, int width, int height, int radius, int sideShine, int bottomDark) {
        if (height <= 0 || width <= 0) {
            return;
        }
        if (radius <= 0) {
            context.method_25294(x, y, x + 1, y + height, sideShine);
            context.method_25294(x + width - 1, y, x + width, y + height, bottomDark);
            context.method_25294(x, y + height - 1, x + width, y + height, bottomDark);
            return;
        }
        radius = Math.min(radius, Math.min(width / 2, height));
        if (height > radius) {
            context.method_25294(x, y, x + 1, y + height - radius, sideShine);
            context.method_25294(x + width - 1, y, x + width, y + height - radius, bottomDark);
        }
        for (int i = 0; i < radius; ++i) {
            int dx = MusicHudRenderer.getRoundDx(radius, i);
            int lineY = y + height - radius + i;
            if (lineY >= y && lineY < y + height) {
                context.method_25294(x + dx, lineY, x + dx + 1, lineY + 1, sideShine);
                context.method_25294(x + width - dx - 1, lineY, x + width - dx, lineY + 1, bottomDark);
            }
        }
        context.method_25294(x + radius, y + height - 1, x + width - radius, y + height, bottomDark);
    }

    private static void renderSpinningVinylDisc(class_332 context, float centerX, float centerY, int discSize, float angleDeg, int alpha, int domColor) {
        int half = discSize / 2;
        int tint = alpha << 24 | 0xFFFFFF;
        context.method_51448().pushMatrix();
        context.method_51448().translate(centerX, centerY);
        context.method_51448().rotate((float)Math.toRadians(angleDeg));
        try {
            context.method_25291(class_10799.field_56883, TEX_VINYL, -half, -half, 0.0f, 0.0f, discSize, discSize, discSize, discSize, tint);
        } catch (Throwable t) {
            MusicHudRenderer.renderRoundedRect(context, -half, -half, discSize, discSize, half, alpha << 24 | 0x141720);
        }
        int labelR = Math.max(4, discSize / 6);
        int labelCol = (int)((float)alpha * 0.90f) << 24 | (domColor != 0 ? domColor : 0x38BDF8);
        MusicHudRenderer.renderRoundedRect(context, -labelR, -labelR, labelR * 2, labelR * 2, labelR, labelCol);
        int spindleSize = Math.max(6, discSize / 4);
        int spindleHalf = spindleSize / 2;
        try {
            context.method_25291(class_10799.field_56883, TEX_SPINDLE, -spindleHalf, -spindleHalf, 0.0f, 0.0f, spindleSize, spindleSize, spindleSize, spindleSize, tint);
        } catch (Throwable t) {
            MusicHudRenderer.renderRoundedRect(context, -2, -2, 4, 4, 2, alpha << 24 | 0x0A0D14);
        }
        context.method_51448().popMatrix();
    }

    private void renderContent(class_332 context, TrackState state, class_310 mc, int alpha, long now, boolean mirrorLayout, int baseW, int baseH, float beatBounce, int finalX, int finalY, float scale, double mouseScreenX, double mouseScreenY) {
        class_327 textRenderer = mc.field_1772;
        String style = HudConfig.INSTANCE.hudStyle != null ? HudConfig.INSTANCE.hudStyle.toUpperCase(java.util.Locale.ROOT) : "MINIMAL_DARK";
        boolean isIsland = "DYNAMIC_ISLAND".equals(style);
        boolean isVinylDeck = "VINYL_DECK".equals(style);
        boolean isGlass = "GLASS_AMBIENT".equals(style);
        int rawDom = this.coverManager.getSmoothDominantColor() & 0xFFFFFF;
        int domColor = rawDom != 0 ? rawDom : 0x38BDF8;
        int r = MusicHudRenderer.getEffectiveRadius(style, baseH);
        boolean isLarge = "LARGE".equalsIgnoreCase(HudConfig.INSTANCE.hudSize);

        String title = state.title();
        String artist = state.artist();
        if (!title.equals(this.lastTitle) || !artist.equals(this.lastArtist)) {
            this.lastTitle = title;
            this.lastArtist = artist;
            this.marqueeStartTime = now + 2000L;
        }
        boolean isFav = FavoritesManager.INSTANCE.isFavorite(state.artist(), state.title());
        String starIcon = isFav ? "★" : "☆";
        int starColor = isFav ? alpha << 24 | 0xFFD700 : (int)((float)alpha * 0.7f) << 24 | 0x94A3B8;
        String timeText = this.formatTimePair(state.getInterpolatedPositionMs(), state.durationMs());
        int timeColor = alpha << 24 | 0x94A3B8;
        int timeWidth = textRenderer.method_1727(timeText);
        String platformStr = state.platformName() != null && !state.platformName().isEmpty() ? state.platformName() : "Музыка";
        int platColor = MusicHudRenderer.getPlatformColor(state.platform());
        com.example.yandexmusichud.lyrics.LyricsManager lm = com.example.yandexmusichud.YandexMusicHudMod.INSTANCE != null ? com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getLyricsManager() : null;
        int trackOffset = HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title());
        long currentPosMs = Math.max(0L, state.getInterpolatedPositionMs() + (long)HudConfig.INSTANCE.subtitlesOffsetMs + (long)trackOffset + (lm != null ? lm.getLiveAcousticOffsetMs() : 0L) + com.example.yandexmusichud.lyrics.LyricsManager.PERCEPTUAL_LEAD_MS);
        com.example.yandexmusichud.lyrics.LyricLine activeLine = HudConfig.INSTANCE.hudMiniKaraoke && lm != null ? lm.getCurrentActiveLine(state, currentPosMs) : null;
        com.example.yandexmusichud.lyrics.LyricLine upcomingLine = (activeLine == null && HudConfig.INSTANCE.hudMiniKaraoke && lm != null) ? lm.getUpcomingLine(state, currentPosMs) : null;
        java.util.List<com.example.yandexmusichud.lyrics.LyricLine> trackLyrics = lm != null && !lm.isCurrentTrackDisabled() ? lm.getLyricsForTrack(state) : null;
        boolean hasTrackLyrics = trackLyrics != null && !trackLyrics.isEmpty();
        boolean isLiveGen = lm != null && (lm.isTrackOnTheFly(state) || lm.isLiveAudioGenerating());
        boolean showKaraokeStrip = HudConfig.INSTANCE.hudMiniKaraoke && (activeLine != null || upcomingLine != null || hasTrackLyrics);

        if (isIsland) {
            int coverSize = isLarge ? 38 : 30;
            int coverMargin = 8;
            int coverX = mirrorLayout ? baseW - coverSize - coverMargin : coverMargin;
            int coverY = (baseH - coverSize) / 2 - (HudConfig.INSTANCE.showProgressBar ? 1 : 0);
            float discCenterX = (float)coverX + (float)coverSize * 0.5f;
            float discCenterY = (float)coverY + (float)coverSize * 0.5f;
            MusicHudRenderer.renderSpinningVinylDisc(context, discCenterX, discCenterY, coverSize, this.vinylRotationAngle, alpha, domColor);
            class_2960 coverId = this.coverManager.getCurrentCoverId();
            if (coverId != null) {
                int innerCover = coverSize - 8;
                int icX = coverX + 4;
                int icY = coverY + 4;
                context.method_25291(class_10799.field_56883, coverId, icX, icY, 0.0f, 0.0f, innerCover, innerCover, innerCover, innerCover, alpha << 24 | 0xFFFFFF);
                MusicHudRenderer.renderRoundedBorder(context, icX, icY, innerCover, innerCover, innerCover / 2, (int)((float)alpha * 0.65f) << 24 | domColor);
            }
            int waveBars = isLarge ? 9 : 6;
            int barW = 2;
            int barGap = 2;
            int totalWaveW = waveBars * (barW + barGap) - barGap;
            int waveX = mirrorLayout ? coverMargin + 2 : baseW - coverMargin - totalWaveW - 2;
            int waveCenterY = baseH / 2 - (HudConfig.INSTANCE.showProgressBar ? 1 : 0);
            int maxWaveH = isLarge ? 22 : 16;
            if (HudConfig.INSTANCE.showEqualizer) {
                float eqDt = this.lastEqUpdateTime > 0L ? Math.min(0.08f, (float)(now - this.lastEqUpdateTime) / 1000.0f) : 0.016f;
                this.lastEqUpdateTime = now;
                for (int b = 0; b < waveBars; ++b) {
                    float freq = 0.007f + (float)b * 0.0024f;
                    float phase = (float)b * 0.72f;
                    float env = 1.0f - Math.abs((float)b - (float)(waveBars - 1) * 0.5f) / ((float)waveBars * 0.6f);
                    float targetH;
                    if (state.isPlaying()) {
                        double s = Math.sin((double)now * (double)freq + (double)phase) * 0.45 + Math.cos((double)now * ((double)freq * 0.7) - (double)phase) * 0.35 + 0.50;
                        targetH = (float)Math.max(0.15, Math.min(1.0, s)) * env;
                    } else {
                        targetH = 0.14f * env;
                    }
                    float curH = this.eqBarHeights[b];
                    curH += (targetH - curH) * Math.min(1.0f, eqDt * 16.0f);
                    this.eqBarHeights[b] = curH;
                    int bh = Math.max(2, (int)(curH * (float)maxWaveH));
                    int bx = waveX + b * (barW + barGap);
                    int by = waveCenterY - bh / 2;
                    float ratio = (float)b / (float)Math.max(1, waveBars - 1);
                    int bCol = (int)((float)alpha * 0.90f) << 24 | MusicHudRenderer.blendRgb(domColor, 0x38BDF8, ratio);
                    MusicHudRenderer.renderRoundedRect(context, bx, by, barW, bh, 1, bCol);
                }
            }
            int textX = mirrorLayout ? waveX + totalWaveW + 8 : coverX + coverSize + 8;
            int maxTextW = Math.max(40, baseW - coverSize - totalWaveW - coverMargin * 2 - 20);
            int titleX = (mirrorLayout && this.hoverControlsAlpha > 0.1f) ? Math.max(textX, 90) : textX;
            int titleMaxW = this.hoverControlsAlpha > 0.1f
                ? (mirrorLayout ? Math.max(36, coverX - 8 - titleX) : Math.max(36, Math.min(maxTextW, (baseW - 90) - titleX)))
                : maxTextW;
            int titleY = isLarge ? 10 : 7;
            this.renderMarqueeText(context, textRenderer, title, titleX, titleY, titleMaxW, alpha << 24 | 0xFFFFFF, now, true);
            if (activeLine != null && activeLine.text != null && !activeLine.text.isEmpty()) {
                this.renderMiniKaraokeCapsule(context, textRenderer, activeLine, null, currentPosMs, textX, titleY + (isLarge ? 12 : 10), maxTextW, 12, alpha, domColor, now, true);
            } else {
                String subLine = artist + " · " + this.formatTime(state.getInterpolatedPositionMs());
                this.renderMarqueeText(context, textRenderer, subLine, textX, titleY + (isLarge ? 13 : 11), maxTextW, alpha << 24 | 0x94A3B8, now, false);
            }
            if (HudConfig.INSTANCE.showProgressBar) {
                int barHeight = class_3532.method_15340((int)HudConfig.INSTANCE.progressBarHeight, (int)2, (int)6);
                int barPad = Math.max(16, r - 4);
                int barW2 = baseW - barPad * 2;
                int barY = baseH - barHeight - 3;
                int fillCol = (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF) != 0xFFCC00
                    ? (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF)
                    : domColor;
                MusicHudRenderer.renderProgressBarWithPlayhead(context, barPad, barY, barW2, barHeight, state.getProgress(), alpha, fillCol, now);
            }
            this.renderHoverMicroControls(context, textRenderer, state, isFav, alpha, mirrorLayout, baseW, finalX, finalY, scale, mouseScreenX, mouseScreenY);
            return;
        }

        int coverSize = isLarge ? 52 : 42;
        int coverMargin = Math.max(8, r - 1);
        int coverX = mirrorLayout ? baseW - coverSize - coverMargin : coverMargin;
        int coverY = (baseH - coverSize) / 2 - (HudConfig.INSTANCE.showProgressBar ? 2 : 0);
        int coverRadius = isLarge ? 6 : 5;
        class_2960 coverId = this.coverManager.getCurrentCoverId();
        class_2960 prevCoverId = this.coverManager.getPreviousCoverId();
        float coverFade = this.coverManager.getCoverFadeProgress();

        int vinylExtraOffset = 0;
        if (isVinylDeck || isLarge) {
            float maxSlide = isVinylDeck ? (isLarge ? 20.0f : 15.0f) : 10.0f;
            float curSlidePx = maxSlide * this.vinylSlideProgress;
            int discSize = coverSize - 4;
            float discCenterX = (float)coverX + (float)coverSize * 0.5f + (mirrorLayout ? -curSlidePx : curSlidePx);
            float discCenterY = (float)coverY + (float)coverSize * 0.5f;
            int vinylAlpha = (int)((float)alpha * (isVinylDeck ? 0.96f : 0.82f));
            MusicHudRenderer.renderSpinningVinylDisc(context, discCenterX, discCenterY, discSize, this.vinylRotationAngle, vinylAlpha, domColor);
            if (isVinylDeck) {
                vinylExtraOffset = Math.round(curSlidePx * 0.85f);
            }
        }

        MusicHudRenderer.renderRoundedRect(context, coverX + 1, coverY + 2, coverSize, coverSize, coverRadius, (int)((float)alpha * 0.48f) << 24 | 0);
        MusicHudRenderer.renderRoundedBorder(context, coverX - 1, coverY - 1, coverSize + 2, coverSize + 2, coverRadius + 1, (int)((float)alpha * 0.25f) << 24 | domColor);
        float hudAlpha = (float)alpha / 255.0f;
        if (prevCoverId != null && coverFade < 1.0f) {
            int prevAlpha = (int)((1.0f - coverFade) * hudAlpha * 255.0f);
            if (prevAlpha > 2) {
                context.method_25291(class_10799.field_56883, prevCoverId, coverX, coverY, 0.0f, 0.0f, coverSize, coverSize, coverSize, coverSize, prevAlpha << 24 | 0xFFFFFF);
            }
            int currAlpha = (int)(coverFade * hudAlpha * 255.0f);
            if (currAlpha > 2 && coverId != null) {
                context.method_25291(class_10799.field_56883, coverId, coverX, coverY, 0.0f, 0.0f, coverSize, coverSize, coverSize, coverSize, currAlpha << 24 | 0xFFFFFF);
            }
        } else if (coverId != null) {
            context.method_25291(class_10799.field_56883, coverId, coverX, coverY, 0.0f, 0.0f, coverSize, coverSize, coverSize, coverSize, alpha << 24 | 0xFFFFFF);
        }
        int coverShine = (int)((float)alpha * 0.36f) << 24 | 0xFFFFFF;
        int coverDark = (int)((float)alpha * 0.50f) << 24 | 0;
        MusicHudRenderer.renderRoundedBorder(context, coverX, coverY, coverSize, coverSize, coverRadius, coverShine, coverDark);

        if (state.isPlaying() && (isGlass || isVinylDeck)) {
            for (int pIdx = 0; pIdx < 4; ++pIdx) {
                float pCycle = 2400.0f + (float)pIdx * 500.0f;
                float pNorm = (float)((now + (long)pIdx * 650L) % (long)pCycle) / pCycle;
                float pAlphaNorm = (float)Math.sin((double)pNorm * Math.PI);
                int pAlpha = (int)(pAlphaNorm * hudAlpha * 165.0f);
                if (pAlpha <= 10) continue;
                float px = (float)coverX + (float)coverSize * 0.5f + (float)Math.sin((double)pIdx * 1.8 + (double)now * 0.0012) * ((float)coverSize * 0.6f);
                float py = (float)coverY + (float)coverSize * 0.5f - pNorm * 26.0f - (beatBounce > 1.0f ? 3.0f : 0.0f);
                String pSym = pIdx % 2 == 0 ? "♪" : "♫";
                int pColor = pIdx % 2 == 0 ? domColor : 0x38BDF8;
                context.method_51433(textRenderer, pSym, (int)px, (int)py, pAlpha << 24 | pColor & 0xFFFFFF, false);
            }
        }

        int textX = mirrorLayout ? coverMargin : coverX + coverSize + 10 + vinylExtraOffset;
        int textY = showKaraokeStrip ? (isLarge ? 6 : 5) : (isLarge ? 10 : 8);
        int maxTextWidth = Math.max(48, baseW - (coverMargin * 2 + coverSize + 10 + vinylExtraOffset));
        int titleColor = alpha << 24 | 0xF8FAFC;
        int artistColor = alpha << 24 | 0x94A3B8;
        int titleX = (mirrorLayout && this.hoverControlsAlpha > 0.1f) ? Math.max(textX, 90) : textX;
        int titleMaxW = this.hoverControlsAlpha > 0.1f
            ? (mirrorLayout ? Math.max(36, maxTextWidth - (titleX - textX)) : Math.max(36, Math.min(maxTextWidth, (baseW - 90) - titleX)))
            : maxTextWidth;
        this.renderMarqueeText(context, textRenderer, title, titleX, textY, titleMaxW, titleColor, now, true);
        this.renderMarqueeText(context, textRenderer, artist, textX, textY + (showKaraokeStrip ? (isLarge ? 12 : 11) : 13), maxTextWidth, artistColor, now, false);

        int metaRowY = textY + (showKaraokeStrip ? (isLarge ? 24 : 22) : 27);
        if (isLarge && HudConfig.INSTANCE.showEqualizer) {
            int eqY = metaRowY;
            int eqHeight = showKaraokeStrip ? 12 : 13;
            int numBars = isVinylDeck ? 10 : 12;
            int barW = 3;
            int barGap = 2;
            int totalEqW = numBars * (barW + barGap) - barGap;
            int eqX = textX;
            float eqDt = this.lastEqUpdateTime > 0L ? Math.min(0.08f, (float)(now - this.lastEqUpdateTime) / 1000.0f) : 0.016f;
            this.lastEqUpdateTime = now;
            for (int b = 0; b < numBars; ++b) {
                float targetH;
                float freq = 0.0068f + (float)b * 0.0022f;
                float phase = (float)b * 0.68f;
                if (state.isPlaying()) {
                    double s = Math.sin((double)now * (double)freq + (double)phase) * 0.42 + Math.cos((double)now * ((double)freq * 0.72) - (double)phase) * 0.38 + 0.52;
                    targetH = (float)Math.max(0.12, Math.min(1.0, s));
                    if (b < 4) {
                        targetH = Math.min(1.0f, targetH + beatBounce * 0.35f);
                    }
                } else {
                    targetH = 0.12f;
                }
                float curH = this.eqBarHeights[b];
                if (targetH > curH) {
                    curH += (targetH - curH) * Math.min(1.0f, eqDt * 18.0f);
                } else {
                    curH += (targetH - curH) * Math.min(1.0f, eqDt * 8.0f);
                }
                this.eqBarHeights[b] = curH;

                float peakH = this.eqPeakHeights[b];
                if (curH >= peakH) {
                    peakH = curH;
                } else {
                    peakH -= eqDt * 1.5f;
                    if (peakH < curH) peakH = curH;
                }
                this.eqPeakHeights[b] = peakH;

                int barH = Math.max(2, (int)(curH * (float)eqHeight));
                int bx = eqX + b * (barW + barGap);
                int by = eqY + eqHeight - barH;
                float ratio = (float)b / (float)Math.max(1, numBars - 1);
                int barRgb = MusicHudRenderer.blendRgb(domColor, 0x38BDF8, ratio);
                int barColor = (int)((float)alpha * 0.88f) << 24 | barRgb;
                MusicHudRenderer.renderRoundedRect(context, bx, by, barW, barH, 1, barColor);
                if (state.isPlaying() && barH > 3) {
                    int peakY = eqY + eqHeight - Math.max(2, (int)(peakH * (float)eqHeight));
                    int peakAlpha = (int)((float)alpha * 0.85f);
                    context.method_25294(bx, peakY, bx + barW, peakY + 1, peakAlpha << 24 | 0xF8FAFC);
                }
            }
            int badgeX = eqX + totalEqW + 7;
            int badgeY = eqY + 2;
            int trimmedPlatMax = maxTextWidth - totalEqW - timeWidth - 26;
            if (trimmedPlatMax > 15) {
                String trimmedPlatform = textRenderer.method_27523(platformStr, trimmedPlatMax);
                int platW = textRenderer.method_1727(trimmedPlatform);
                MusicHudRenderer.renderRoundedRect(context, badgeX - 2, badgeY - 1, platW + 4, 10, 3, (int)((float)alpha * 0.20f) << 24 | platColor & 0xFFFFFF);
                context.method_51433(textRenderer, trimmedPlatform, badgeX, badgeY, alpha << 24 | platColor & 0xFFFFFF, false);
                badgeX += platW + 6;
            }
            int rightLimitX = textX + maxTextWidth - timeWidth - 14;
            if (Math.abs(state.playbackSpeed() - 1.0f) >= 0.05f) {
                String spdText = MusicHudRenderer.formatSpeed(state.playbackSpeed(), true);
                int spdW = textRenderer.method_1727(spdText);
                if (badgeX + spdW + 4 <= rightLimitX) {
                    MusicHudRenderer.renderRoundedRect(context, badgeX - 2, badgeY - 1, spdW + 4, 10, 3, (int)((float)alpha * 0.22f) << 24 | 0xFFA500);
                    context.method_51433(textRenderer, spdText, badgeX, badgeY, alpha << 24 | 0xFFB732, false);
                    badgeX += spdW + 6;
                }
            }
            if (isLiveGen) {
                String liveText = "● LIVE";
                int liveW = textRenderer.method_1727(liveText);
                if (badgeX + liveW + 4 <= rightLimitX) {
                    float livePulse = (float)Math.sin((double)now * 0.008) * 0.35f + 0.65f;
                    MusicHudRenderer.renderRoundedRect(context, badgeX - 2, badgeY - 1, liveW + 4, 10, 3, (int)((float)alpha * 0.26f * livePulse) << 24 | 0x10B981);
                    context.method_51433(textRenderer, liveText, badgeX, badgeY, alpha << 24 | 0x34D399, false);
                    badgeX += liveW + 6;
                }
            }
            int starDrawX = Math.min(badgeX, textX + maxTextWidth - timeWidth - 12);
            if (isFav) {
                MusicHudRenderer.renderRoundedRect(context, starDrawX - 2, badgeY - 1, 12, 10, 3, (int)((float)alpha * 0.22f) << 24 | 0xFFD700);
            }
            context.method_51433(textRenderer, starIcon, starDrawX, badgeY, starColor, isFav);
            context.method_51433(textRenderer, timeText, textX + maxTextWidth - timeWidth, badgeY, timeColor, false);
        } else {
            if (HudConfig.INSTANCE.showEqualizer) {
                int eqX = coverX + coverSize - 15;
                int eqY = coverY + coverSize - 12;
                MusicHudRenderer.renderRoundedRect(context, eqX - 1, eqY - 1, 15, 12, 3, (int)((float)alpha * 0.85f) << 24 | 0x0A0D14);
                for (int b = 0; b < 4; ++b) {
                    float phase = (float)b * 1.35f;
                    float speed = 0.008f + (float)b * 0.003f;
                    float barH = state.isPlaying() ? (float)(Math.sin((double)now * (double)speed + (double)phase) * 0.45 + 0.55) * 7.5f + 2.0f : 2.0f;
                    int bx = eqX + b * 3 + 1;
                    int by = eqY + 9 - (int)barH;
                    int bColor = alpha << 24 | (state.isPlaying() ? domColor : 0x64748B);
                    context.method_25294(bx, by, bx + 2, eqY + 9, bColor);
                }
            }
            if (isFav) {
                MusicHudRenderer.renderRoundedRect(context, textX - 2, metaRowY - 1, 13, 11, 3, (int)((float)alpha * 0.22f) << 24 | 0xFFD700);
            }
            context.method_51433(textRenderer, starIcon, textX, metaRowY, starColor, isFav);
            int starW = textRenderer.method_1727(starIcon) + 4;
            int maxPlatformW = maxTextWidth - timeWidth - starW - 6;
            int platX = textX + starW;
            int platY = metaRowY;
            if (maxPlatformW > 15) {
                String trimmedPlatform = textRenderer.method_27523(platformStr, maxPlatformW);
                int platW = textRenderer.method_1727(trimmedPlatform);
                MusicHudRenderer.renderRoundedRect(context, platX - 2, platY - 1, platW + 4, 10, 3, (int)((float)alpha * 0.20f) << 24 | platColor & 0xFFFFFF);
                context.method_51433(textRenderer, trimmedPlatform, platX, platY, alpha << 24 | platColor & 0xFFFFFF, false);
                platX += platW + 5;
            }
            if (Math.abs(state.playbackSpeed() - 1.0f) >= 0.05f && textX + maxTextWidth - timeWidth - platX > 25) {
                String spdText = MusicHudRenderer.formatSpeed(state.playbackSpeed(), false);
                int spdW = textRenderer.method_1727(spdText);
                MusicHudRenderer.renderRoundedRect(context, platX - 2, platY - 1, spdW + 4, 10, 3, (int)((float)alpha * 0.22f) << 24 | 0xFFA500);
                context.method_51433(textRenderer, spdText, platX, platY, alpha << 24 | 0xFFB732, false);
                platX += spdW + 5;
            }
            if (isLiveGen) {
                String liveText = "LIVE";
                int liveW = textRenderer.method_1727(liveText);
                if (platX + liveW + 5 <= textX + maxTextWidth - timeWidth - 4) {
                    float livePulse = (float)Math.sin((double)now * 0.008) * 0.35f + 0.65f;
                    MusicHudRenderer.renderRoundedRect(context, platX - 2, platY - 1, liveW + 4, 10, 3, (int)((float)alpha * 0.26f * livePulse) << 24 | 0x10B981);
                    context.method_51433(textRenderer, liveText, platX, platY, alpha << 24 | 0x34D399, false);
                }
            }
            context.method_51433(textRenderer, timeText, textX + maxTextWidth - timeWidth, metaRowY, timeColor, false);
        }

        if (showKaraokeStrip) {
            int stripY = isLarge ? 45 : 38;
            int stripH = 13;
            this.renderMiniKaraokeCapsule(context, textRenderer, activeLine, upcomingLine, currentPosMs, textX, stripY, maxTextWidth, stripH, alpha, domColor, now, false);
        }

        if (HudConfig.INSTANCE.showProgressBar) {
            int barHeight = class_3532.method_15340((int)HudConfig.INSTANCE.progressBarHeight, (int)2, (int)6);
            int barPad = Math.max(r + 2, 8);
            int barW = baseW - barPad * 2;
            int barY = baseH - barHeight - (isLarge ? 4 : 3);
            int fillCol = (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF) != 0xFFCC00
                ? (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF)
                : domColor;
            MusicHudRenderer.renderProgressBarWithPlayhead(context, barPad, barY, barW, barHeight, state.getProgress(), alpha, fillCol, now);
        }

        this.renderHoverMicroControls(context, textRenderer, state, isFav, alpha, mirrorLayout, baseW, finalX, finalY, scale, mouseScreenX, mouseScreenY);
    }

    public static void renderProgressBarWithPlayhead(class_332 context, int barX, int barY, int barW, int barHeight, float progress, int alpha, int fillRgb, long now) {
        alpha = class_3532.method_15340(alpha, 0, 255);
        int barBgColor = (int)((float)alpha * 0.55f) << 24 | 0x161B26;
        int barFillColor = alpha << 24 | (fillRgb & 0xFFFFFF);
        MusicHudRenderer.renderRoundedRect(context, barX, barY, barW, barHeight, Math.max(1, barHeight / 2), barBgColor);
        int progressWidth = Math.max(0, Math.min(barW, (int)((float)barW * progress)));
        if (progressWidth > 0) {
            MusicHudRenderer.renderRoundedRect(context, barX, barY, progressWidth, barHeight, Math.max(1, barHeight / 2), barFillColor);
            context.method_25294(barX, barY, barX + progressWidth, barY + 1, (int)((float)alpha * 0.35f) << 24 | 0xFFFFFF);
            long cycle = 2800L;
            float shimmerCycle = (float)(now % cycle) / (float)cycle;
            if (shimmerCycle < 0.65f) {
                float st = shimmerCycle / 0.65f;
                float waveCenterX = (float)barX + (float)progressWidth * st;
                float waveHalfW = 10.0f;
                int wx1 = (int)Math.max((float)barX, waveCenterX - waveHalfW);
                int wx2 = (int)Math.min((float)(barX + progressWidth), waveCenterX + waveHalfW);
                if (wx2 > wx1) {
                    int shimmerAlpha = (int)((float)alpha * 0.35f * (1.0f - Math.abs(st - 0.5f) * 0.5f));
                    context.method_25294(wx1, barY, wx2, barY + barHeight, shimmerAlpha << 24 | 0xFFFFFF);
                }
            }
            int knobX = barX + progressWidth;
            int knobR = Math.max(2, (barHeight + 2) / 2);
            int glowAlpha = (int)((float)alpha * 0.38f);
            MusicHudRenderer.renderRoundedRect(context, knobX - knobR - 1, barY + barHeight / 2 - knobR - 1, (knobR + 1) * 2, (knobR + 1) * 2, knobR + 1, glowAlpha << 24 | (fillRgb & 0xFFFFFF));
            MusicHudRenderer.renderRoundedRect(context, knobX - knobR, barY + barHeight / 2 - knobR, knobR * 2, knobR * 2, knobR, alpha << 24 | 0xFFFFFF);
        }
    }

    private void renderHoverMicroControls(class_332 context, class_327 tr, TrackState state, boolean isFav, int hudAlpha, boolean mirrorLayout, int baseW, int finalX, int finalY, float scale, double mouseScreenX, double mouseScreenY) {
        if (this.hoverControlsAlpha <= 0.02f) {
            lastMicroControlsVisible = false;
            return;
        }
        int cAlpha = (int)((float)hudAlpha * this.hoverControlsAlpha);
        if (cAlpha <= 6) {
            lastMicroControlsVisible = false;
            return;
        }
        lastMicroControlsVisible = true;
        int btnW = 14;
        int btnH = 12;
        int gap = 2;
        int pillPad = 2;
        int pillW = 5 * btnW + 4 * gap + pillPad * 2;
        int pillH = btnH + pillPad * 2;
        int pillX = mirrorLayout ? 6 : baseW - pillW - 6;
        int pillY = 5;

        MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 5, (int)((float)cAlpha * 0.94f) << 24 | 0x080B11);
        MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 5, (int)((float)cAlpha * 0.55f) << 24 | 0x38BDF8, (int)((float)cAlpha * 0.35f) << 24 | 0x334155);

        String[] icons = new String[]{
            "⏮",
            state.isPlaying() ? "⏸" : "▶",
            "⏭",
            isFav ? "★" : "☆",
            "♪"
        };
        for (int i = 0; i < 5; ++i) {
            int bx = pillX + pillPad + i * (btnW + gap);
            int by = pillY + pillPad;
            int scrX = finalX + Math.round((float)bx * scale);
            int scrY = finalY + Math.round((float)by * scale);
            int scrW = Math.max(4, Math.round((float)btnW * scale));
            int scrH = Math.max(4, Math.round((float)btnH * scale));
            lastMicroBtnScreenX[i] = scrX;
            lastMicroBtnScreenY[i] = scrY;
            lastMicroBtnScreenW[i] = scrW;
            lastMicroBtnScreenH[i] = scrH;
            boolean btnHov = mouseScreenX >= (double)scrX && mouseScreenX <= (double)(scrX + scrW) &&
                             mouseScreenY >= (double)scrY && mouseScreenY <= (double)(scrY + scrH);
            if (btnHov) {
                int hovCol = i == 3 ? 0xFFD700 : 0x38BDF8;
                MusicHudRenderer.renderRoundedRect(context, bx, by, btnW, btnH, 3, (int)((float)cAlpha * 0.35f) << 24 | hovCol);
            } else if (i == 4 && HudConfig.INSTANCE.hudMiniKaraoke) {
                MusicHudRenderer.renderRoundedRect(context, bx, by, btnW, btnH, 3, (int)((float)cAlpha * 0.22f) << 24 | 0x38BDF8);
            }
            int iconCol = i == 3 && isFav
                ? (cAlpha << 24 | 0xFFD700)
                : (i == 4 && HudConfig.INSTANCE.hudMiniKaraoke && !btnHov ? (cAlpha << 24 | 0x38BDF8) : (btnHov ? (cAlpha << 24 | 0xFFFFFF) : (cAlpha << 24 | 0xCBD5E1)));
            int iw = tr.method_1727(icons[i]);
            context.method_51433(tr, icons[i], bx + (btnW - iw) / 2, by + 2, iconCol, false);
        }
    }

    private void renderMiniKaraokeCapsule(class_332 context, class_327 tr, com.example.yandexmusichud.lyrics.LyricLine activeLine, com.example.yandexmusichud.lyrics.LyricLine upcomingLine, long currentPosMs, int stripX, int stripY, int stripW, int stripH, int alpha, int domColor, long now, boolean inlineMode) {
        if (stripW < 24 || alpha <= 6) {
            return;
        }
        int textAreaX;
        int textAreaW;
        if (!inlineMode) {
            MusicHudRenderer.renderRoundedRect(context, stripX, stripY, stripW, stripH, 4, (int)((float)alpha * 0.78f) << 24 | 0x06080D);
            int topBorder = (int)((float)alpha * 0.42f) << 24 | MusicHudRenderer.blendRgb(0x334155, domColor, 0.38f);
            int botBorder = (int)((float)alpha * 0.30f) << 24 | 0x1E293B;
            MusicHudRenderer.renderRoundedBorder(context, stripX, stripY, stripW, stripH, 4, topBorder, botBorder);
            float notePulse = (float)Math.sin((double)now * 0.008) * 0.15f + 0.85f;
            int noteAlpha = (int)((float)alpha * notePulse);
            context.method_51433(tr, "♪", stripX + 4, stripY + 2, noteAlpha << 24 | domColor, false);
            textAreaX = stripX + 13;
            textAreaW = Math.max(16, stripW - 17);
        } else {
            context.method_51433(tr, "♪", stripX, stripY + 1, alpha << 24 | domColor, false);
            textAreaX = stripX + 9;
            textAreaW = Math.max(16, stripW - 10);
        }

        if (activeLine != null && activeLine.text != null && !activeLine.text.isEmpty()) {
            String plain = activeLine.text;
            if (activeLine != this.lastKaraokeLine) {
                this.lastKaraokeLine = activeLine;
                this.karaokeScrollX = 0.0f;
            }
            int activeStart = -1;
            int activeEnd = -1;
            int lastPronounced = -1;
            int len = plain.length();
            for (int i = 0; i < len; ++i) {
                if (activeLine.isCharActive(i, currentPosMs)) {
                    if (activeStart < 0) activeStart = i;
                    activeEnd = i;
                }
                if (activeLine.isCharPronounced(i, currentPosMs)) {
                    lastPronounced = i;
                }
            }
            if (activeStart >= 0) {
                while (activeStart > 0 && plain.charAt(activeStart - 1) != ' ') {
                    --activeStart;
                }
                while (activeEnd + 1 < len && plain.charAt(activeEnd + 1) != ' ') {
                    ++activeEnd;
                }
            }
            int activeCenterPx = 0;
            if (activeStart >= 0) {
                int sx = tr.method_1727(plain.substring(0, activeStart));
                int ex = tr.method_1727(plain.substring(0, activeEnd + 1));
                activeCenterPx = (sx + ex) / 2;
            } else if (lastPronounced >= 0) {
                activeCenterPx = tr.method_1727(plain.substring(0, lastPronounced + 1));
            }
            int totalW = tr.method_1727(plain);
            float targetScroll = totalW <= textAreaW ? 0.0f : class_3532.method_15363((float)activeCenterPx - (float)textAreaW * 0.38f, 0.0f, (float)(totalW - textAreaW + 8));
            this.karaokeScrollX += (targetScroll - this.karaokeScrollX) * Math.min(1.0f, this.lastFrameDt * 12.0f);

            int drawX = textAreaX - Math.round(this.karaokeScrollX);
            int drawY = stripY + (inlineMode ? 1 : 2);
            context.method_44379(textAreaX, stripY, textAreaX + textAreaW, stripY + stripH);
            if (activeStart >= 0) {
                String beforeStr = plain.substring(0, activeStart);
                String activeStr = plain.substring(activeStart, activeEnd + 1);
                String afterStr = activeEnd + 1 < len ? plain.substring(activeEnd + 1) : "";
                int beforeW = tr.method_1727(beforeStr);
                int activeW = tr.method_1727(activeStr);
                MusicHudRenderer.renderRoundedRect(context, drawX + beforeW - 2, stripY + 1, activeW + 4, stripH - 2, 3, (int)((float)alpha * 0.28f) << 24 | domColor);
                if (!beforeStr.isEmpty()) {
                    context.method_51433(tr, beforeStr, drawX, drawY, alpha << 24 | 0xE2E8F0, false);
                }
                int activeTextCol = alpha << 24 | MusicHudRenderer.blendRgb(domColor, 0xFFFFFF, 0.38f);
                context.method_51433(tr, activeStr, drawX + beforeW, drawY, activeTextCol, true);
                if (!afterStr.isEmpty()) {
                    context.method_51433(tr, afterStr, drawX + beforeW + activeW, drawY, (int)((float)alpha * 0.85f) << 24 | 0x64748B, false);
                }
            } else {
                int splitIdx = Math.max(0, Math.min(len, lastPronounced + 1));
                String sungPart = plain.substring(0, splitIdx);
                String remPart = plain.substring(splitIdx);
                int sungW = tr.method_1727(sungPart);
                if (!sungPart.isEmpty()) {
                    context.method_51433(tr, sungPart, drawX, drawY, alpha << 24 | 0xE2E8F0, false);
                }
                if (!remPart.isEmpty()) {
                    context.method_51433(tr, remPart, drawX + sungW, drawY, (int)((float)alpha * 0.85f) << 24 | 0x64748B, false);
                }
            }
            context.method_44380();

            if (!inlineMode) {
                long lineDur = Math.max(100L, activeLine.vocalEndMs - activeLine.startMs);
                float lineProg = class_3532.method_15363((float)(currentPosMs - activeLine.startMs) / (float)lineDur, 0.0f, 1.0f);
                int progMaxW = stripW - 6;
                int progW = Math.round((float)progMaxW * lineProg);
                if (progW > 0) {
                    context.method_25294(stripX + 3, stripY + stripH - 1, stripX + 3 + progW, stripY + stripH, (int)((float)alpha * 0.85f) << 24 | domColor);
                }
            }
        } else if (upcomingLine != null && upcomingLine.text != null && !upcomingLine.text.isEmpty()) {
            this.lastKaraokeLine = null;
            this.karaokeScrollX = 0.0f;
            String upText = tr.method_27523("Далее: " + upcomingLine.text, textAreaW);
            context.method_51433(tr, upText, textAreaX, stripY + 2, (int)((float)alpha * 0.72f) << 24 | 0x64748B, false);
        } else {
            this.lastKaraokeLine = null;
            this.karaokeScrollX = 0.0f;
            int activeDot = (int)((now / 420L) % 3L);
            for (int d = 0; d < 3; ++d) {
                int dotCol = d == activeDot ? (alpha << 24 | domColor) : ((int)((float)alpha * 0.45f) << 24 | 0x64748B);
                MusicHudRenderer.renderRoundedRect(context, textAreaX + d * 7, stripY + 5, 3, 3, 1, dotCol);
            }
            if (textAreaW > 55) {
                context.method_51433(tr, "Проигрыш", textAreaX + 24, stripY + 2, (int)((float)alpha * 0.60f) << 24 | 0x475569, false);
            }
        }
    }

    public static void renderStylePreview(class_332 context, class_327 tr, int baseW, int baseH, boolean mirror) {
        String style = HudConfig.INSTANCE.hudStyle != null ? HudConfig.INSTANCE.hudStyle.toUpperCase(java.util.Locale.ROOT) : "MINIMAL_DARK";
        boolean isIsland = "DYNAMIC_ISLAND".equals(style);
        boolean isVinylDeck = "VINYL_DECK".equals(style);
        boolean isGlass = "GLASS_AMBIENT".equals(style);
        boolean isLarge = "LARGE".equalsIgnoreCase(HudConfig.INSTANCE.hudSize);
        int r = MusicHudRenderer.getEffectiveRadius(style, baseH);
        int domColor = 0x38BDF8;
        if (com.example.yandexmusichud.YandexMusicHudMod.INSTANCE != null && com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getCoverManager() != null) {
            int c = com.example.yandexmusichud.YandexMusicHudMod.INSTANCE.getCoverManager().getSmoothDominantColor() & 0xFFFFFF;
            if (c != 0) domColor = c;
        }
        long now = System.currentTimeMillis();
        float rotAngle = (float)(now % 7200L) / 7200.0f * 360.0f;

        class_2960 customBg = CustomBackgroundManager.INSTANCE.getBackgroundTexture();
        if (customBg != null) {
            int customBgAlpha = (int)(class_3532.method_15363(HudConfig.INSTANCE.customBackgroundAlpha, 0.0f, 1.0f) * 255.0f);
            context.method_25291(class_10799.field_56883, customBg, 0, 0, 0.0f, 0.0f, baseW, baseH, baseW, baseH, customBgAlpha << 24 | 0xFFFFFF);
            int shineColor = (int)(customBgAlpha * 0.35f) << 24 | 0xFFFFFF;
            int shadowColor = (int)(customBgAlpha * 0.45f) << 24 | 0x000000;
            MusicHudRenderer.renderRoundedBorder(context, 0, 0, baseW, baseH, r, shineColor, shadowColor);
        } else {
            float bgAlphaF = class_3532.method_15363(HudConfig.INSTANCE.hudBackgroundAlpha, 0.0f, 1.0f);
            int bgAlpha = (int)(bgAlphaF * 255.0f);
            int baseRgb = HudConfig.INSTANCE.hudBackgroundColor & 0xFFFFFF;
            if (isIsland && baseRgb == 0x0A0D14) {
                baseRgb = 0x06080D;
            }
            MusicHudRenderer.renderRoundedRect(context, 0, 0, baseW, baseH, r, bgAlpha << 24 | baseRgb);
            int glowSpan = (int)((float)baseW * (isGlass ? 0.72f : 0.46f));
            int maxGlowAlpha = (int)(bgAlphaF * (isGlass ? 95.0f : 34.0f));
            MusicHudRenderer.renderHorizontalAmbientGlow(context, 0, 0, baseW, baseH, r, domColor, maxGlowAlpha, glowSpan, mirror);
            int topBorder = (int)(bgAlphaF * (isGlass ? 88.0f : 56.0f)) << 24 | (isGlass ? MusicHudRenderer.blendRgb(0xFFFFFF, domColor, 0.35f) : 0x94A3B8);
            int botBorder = (int)(bgAlphaF * 44.0f) << 24 | 0x1E293B;
            MusicHudRenderer.renderRoundedBorder(context, 0, 0, baseW, baseH, r, topBorder, botBorder);
            if (!isGlass && !isIsland) {
                int accentH = Math.max(12, baseH - Math.max(r * 2, 20));
                int accentY = (baseH - accentH) / 2;
                int accentX = mirror ? baseW - 3 : 1;
                MusicHudRenderer.renderRoundedRect(context, accentX, accentY, 2, accentH, 1, (int)(bgAlphaF * 195.0f) << 24 | domColor);
            }
        }

        if (isIsland) {
            int coverSize = isLarge ? 38 : 30;
            int coverMargin = 8;
            int coverX = mirror ? baseW - coverSize - coverMargin : coverMargin;
            int coverY = (baseH - coverSize) / 2 - (HudConfig.INSTANCE.showProgressBar ? 1 : 0);
            MusicHudRenderer.renderSpinningVinylDisc(context, (float)coverX + (float)coverSize * 0.5f, (float)coverY + (float)coverSize * 0.5f, coverSize, rotAngle, 255, domColor);
            int waveBars = isLarge ? 9 : 6;
            int totalWaveW = waveBars * 4 - 2;
            int waveX = mirror ? coverMargin + 2 : baseW - coverMargin - totalWaveW - 2;
            int waveCenterY = baseH / 2 - 1;
            if (HudConfig.INSTANCE.showEqualizer) {
                for (int b = 0; b < waveBars; ++b) {
                    float wave = (float)Math.sin((double)now * 0.008 + (double)b * 0.7);
                    int bh = 4 + (int)((wave + 1.0f) * 6.0f);
                    int bx = waveX + b * 4;
                    int bCol = 0xFF000000 | MusicHudRenderer.blendRgb(domColor, 0x38BDF8, (float)b / (float)waveBars);
                    MusicHudRenderer.renderRoundedRect(context, bx, waveCenterY - bh / 2, 2, bh, 1, bCol);
                }
            }
            int textX = mirror ? waveX + totalWaveW + 8 : coverX + coverSize + 8;
            int titleY = isLarge ? 10 : 7;
            context.method_51433(tr, "Предпросмотр трека", textX, titleY, -1, true);
            context.method_51433(tr, "♪ Музыка в ночи", textX, titleY + (isLarge ? 13 : 11), 0xFF000000 | domColor, false);
            if (HudConfig.INSTANCE.showProgressBar) {
                int barHeight = class_3532.method_15340(HudConfig.INSTANCE.progressBarHeight, 2, 6);
                int barPad = Math.max(16, r - 4);
                int fillCol = (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF) != 0xFFCC00 ? (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF) : domColor;
                MusicHudRenderer.renderProgressBarWithPlayhead(context, barPad, baseH - barHeight - 3, baseW - barPad * 2, barHeight, 0.45f, 255, fillCol, now);
            }
            return;
        }

        int coverMargin = Math.max(8, r - 1);
        int coverSize = isLarge ? 52 : 42;
        int coverX = mirror ? baseW - coverSize - coverMargin : coverMargin;
        int coverY = (baseH - coverSize) / 2 - 2;
        int vinylExtra = 0;
        if (isVinylDeck || isLarge) {
            float slidePx = isVinylDeck ? (isLarge ? 20.0f : 15.0f) : 10.0f;
            float discCX = (float)coverX + (float)coverSize * 0.5f + (mirror ? -slidePx : slidePx);
            float discCY = (float)coverY + (float)coverSize * 0.5f;
            MusicHudRenderer.renderSpinningVinylDisc(context, discCX, discCY, coverSize - 4, rotAngle, 240, domColor);
            if (isVinylDeck) {
                vinylExtra = Math.round(slidePx * 0.85f);
            }
        }
        MusicHudRenderer.renderRoundedRect(context, coverX, coverY, coverSize, coverSize, 6, 0xFF141924);
        MusicHudRenderer.renderRoundedBorder(context, coverX, coverY, coverSize, coverSize, 6, 0x66FFFFFF, 0x88000000);
        context.method_27534(tr, (class_2561)class_2561.method_43470("♪"), coverX + coverSize / 2, coverY + coverSize / 2 - 4, 0xFF000000 | domColor);
        int textX = mirror ? coverMargin : coverX + coverSize + 10 + vinylExtra;
        int maxTextW = Math.max(48, baseW - (coverMargin * 2 + coverSize + 10 + vinylExtra));
        boolean showKaraoke = HudConfig.INSTANCE.hudMiniKaraoke;
        int titleY = showKaraoke ? (isLarge ? 6 : 5) : 8;
        context.method_51433(tr, "Предпросмотр трека", textX, titleY, 0xFFF8FAFC, true);
        context.method_51433(tr, "Исполнитель", textX, titleY + (showKaraoke ? (isLarge ? 12 : 11) : 12), 0xFF94A3B8, false);
        if (HudConfig.INSTANCE.showEqualizer) {
            int eqX = textX;
            int eqY = titleY + (showKaraoke ? (isLarge ? 24 : 22) : 26);
            int numBars = isVinylDeck ? 9 : 11;
            for (int b = 0; b < numBars; ++b) {
                float wave = (float)Math.sin((double)now * 0.008 + (double)b * 0.7);
                int barH = 3 + (int)((wave + 1.0f) * 4.0f);
                int barColor = 0xFF000000 | MusicHudRenderer.blendRgb(domColor, 0x38BDF8, (float)b / (float)numBars);
                MusicHudRenderer.renderRoundedRect(context, eqX + b * 5, eqY + (11 - barH), 3, barH, 1, barColor);
            }
        }
        if (showKaraoke) {
            int stripY = isLarge ? 45 : 38;
            int stripH = 13;
            MusicHudRenderer.renderRoundedRect(context, textX, stripY, maxTextW, stripH, 4, 0xCC06080D);
            MusicHudRenderer.renderRoundedBorder(context, textX, stripY, maxTextW, stripH, 4, 0x6638BDF8, 0x441E293B);
            context.method_51433(tr, "♪", textX + 4, stripY + 2, 0xFF000000 | domColor, false);
            String[] words = new String[]{"Музыка", "звучит", "в", "ночи"};
            int activeWordIdx = (int)((now / 650L) % (long)words.length);
            int wx = textX + 13;
            context.method_44379(textX + 12, stripY, textX + maxTextW - 3, stripY + stripH);
            for (int w = 0; w < words.length; ++w) {
                int ww = tr.method_1727(words[w]);
                if (w == activeWordIdx) {
                    MusicHudRenderer.renderRoundedRect(context, wx - 2, stripY + 1, ww + 4, stripH - 2, 3, 0x44000000 | domColor);
                    context.method_51433(tr, words[w], wx, stripY + 2, 0xFF000000 | MusicHudRenderer.blendRgb(domColor, 0xFFFFFF, 0.38f), true);
                } else {
                    int wCol = w < activeWordIdx ? 0xFFE2E8F0 : 0xFF64748B;
                    context.method_51433(tr, words[w], wx, stripY + 2, wCol, false);
                }
                wx += ww + 4;
            }
            context.method_44380();
            float lProg = (float)(now % 2600L) / 2600.0f;
            int pW = Math.round((float)(maxTextW - 6) * lProg);
            if (pW > 0) {
                context.method_25294(textX + 3, stripY + stripH - 1, textX + 3 + pW, stripY + stripH, 0xDD000000 | domColor);
            }
        }
        if (HudConfig.INSTANCE.showProgressBar) {
            int barHeight = class_3532.method_15340(HudConfig.INSTANCE.progressBarHeight, 2, 6);
            int barPad = Math.max(r + 2, 8);
            int barW = baseW - barPad * 2;
            int barY = baseH - barHeight - (isLarge ? 4 : 3);
            int fillCol = (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF) != 0xFFCC00
                ? (HudConfig.INSTANCE.progressBarColor & 0xFFFFFF)
                : domColor;
            MusicHudRenderer.renderProgressBarWithPlayhead(context, barPad, barY, barW, barHeight, 0.45f, 255, fillCol, now);
        }
    }

    public static int getPlatformColor(String platform) {
        if (platform == null) {
            return -16718337;
        }
        return switch (platform.toLowerCase(java.util.Locale.ROOT)) {
            case "local" -> -16718081;
            case "spotify" -> -14829228;
            case "youtube" -> -56798;
            case "soundcloud" -> -39424;
            case "vk" -> -16746497;
            case "apple" -> -246716;
            case "yandex" -> -13312;
            case "browser" -> -16723201;
            default -> -16718337;
        };
    }

    private void renderMarqueeText(class_332 context, class_327 textRenderer, String text, int x, int y, int maxWidth, int color, long now, boolean isTitle) {
        int textWidth = textRenderer.method_1727(text);
        if (textWidth <= maxWidth) {
            context.method_51433(textRenderer, text, x, y, color, isTitle);
            return;
        }
        long scrollDelay = isTitle ? this.marqueeStartTime : this.marqueeStartTime + 500L;
        if (now < scrollDelay) {
            this.drawClippedText(context, textRenderer, text, x, y, maxWidth, color, 0, isTitle);
            return;
        }
        long elapsed = now - scrollDelay;
        int totalScroll = textWidth - maxWidth + 18;
        long oneWayMs = Math.max(1400L, (long)((float)totalScroll / 26.0f * 1000.0f));
        long holdEndMs = 1100L;
        long holdStartMs = 1400L;
        long cycleMs = oneWayMs * 2L + holdEndMs + holdStartMs;
        long posInCycle = elapsed % cycleMs;
        float frac;
        if (posInCycle < oneWayMs) {
            float t = (float)posInCycle / (float)oneWayMs;
            frac = 0.5f - 0.5f * (float)Math.cos((double)t * Math.PI);
        } else if (posInCycle < oneWayMs + holdEndMs) {
            frac = 1.0f;
        } else if (posInCycle < oneWayMs * 2L + holdEndMs) {
            float t = (float)(posInCycle - oneWayMs - holdEndMs) / (float)oneWayMs;
            frac = 0.5f + 0.5f * (float)Math.cos((double)t * Math.PI);
        } else {
            frac = 0.0f;
        }
        int scrollOffset = Math.round(frac * (float)totalScroll);
        this.drawClippedText(context, textRenderer, text, x, y, maxWidth, color, scrollOffset, isTitle);
    }

    private void drawClippedText(class_332 context, class_327 textRenderer, String text, int x, int y, int maxWidth, int color, int scrollOffset, boolean shadow) {
        context.method_44379(x, y - 2, x + maxWidth, y + 14);
        context.method_51433(textRenderer, text, x - scrollOffset, y, color, shadow);
        context.method_44380();
    }

    private String formatTime(long ms) {
        if (ms <= 0L) {
            return "0:00";
        }
        long totalSeconds = ms / 1000L;
        if (totalSeconds != this.lastSingleTimeSec) {
            this.lastSingleTimeSec = totalSeconds;
            this.lastSingleTimeStr = formatSecondsColon(totalSeconds);
        }
        return this.lastSingleTimeStr;
    }

    private String formatTimePair(long posMs, long durMs) {
        long posSec = Math.max(0L, posMs) / 1000L;
        long durSec = Math.max(0L, durMs) / 1000L;
        if (posSec != this.lastTimePosSec || durSec != this.lastTimeDurSec) {
            this.lastTimePosSec = posSec;
            this.lastTimeDurSec = durSec;
            this.lastTimePairStr = formatSecondsColon(posSec) + " / " + formatSecondsColon(durSec);
        }
        return this.lastTimePairStr;
    }

    private static String formatSecondsColon(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return seconds < 10L ? minutes + ":0" + seconds : minutes + ":" + seconds;
    }
}
