package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.InitialSetupScreen;
import com.example.yandexmusichud.hud.KeybindSetupScreen;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.hud.SystemDiagnosticsScreen;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.class_1068;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_5599;
import net.minecraft.class_5601;
import net.minecraft.class_591;
import net.minecraft.class_630;
import net.minecraft.class_6880;
import net.minecraft.class_7920;
import net.minecraft.class_8685;

public class IntroScreen
extends class_437 {
    public static boolean hasShownIntro = false;
    private static final class_2960 ICON_TEXTURE = class_2960.method_60655((String)"yandexmusichud", (String)"icon.png");
    private static final class_2960 YANDEX_BROWSER_ICON = class_2960.method_60655((String)"yandexmusichud", (String)"textures/gui/yandex_browser_icon.png");
    private static final class_2960 TELEGRAM_LOGO = class_2960.method_60655((String)"yandexmusichud", (String)"textures/gui/telegram_logo.png");
    private static final String TITLE_TEXT = "MUSIC SUBTITLES";
    private static final String TAGLINE_TEXT = "CINEMATIC 3D LYRICS & REAL-TIME AUDIO SYNC";
    private static final String HINT_TEXT = "\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043b\u044e\u0431\u0443\u044e \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u0434\u043b\u044f \u043f\u0440\u043e\u043f\u0443\u0441\u043a\u0430";
    private static final int COLOR_GOLD = -13312;
    private static final int COLOR_CYAN = -16718337;
    private static final int COLOR_WHITE = -1;
    private final class_437 parent;
    private final Random random = new Random();
    private long startTimestamp = -1L;
    private boolean soundWhooshPlayed = false;
    private boolean soundImpactPlayed = false;
    private boolean soundShimmerPlayed = false;
    private boolean versionSoundPlayed = false;
    private float versionHitFlash = 0.0f;
    private boolean inTelegramSplash = false;
    private long telegramSplashStartTime = -1L;
    private boolean telegramSplashSoundPlayed = false;
    private boolean inBrowserWarning = false;
    private long browserWarningStartTime = -1L;
    private boolean browserWarningSoundPlayed = false;
    private boolean isExiting = false;
    private long exitStartTime = -1L;
    private class_591 runnerModel;
    private Supplier<class_8685> skinTexturesSupplier;
    private class_2960 playerSkinTexture;
    private boolean isPlayerSlim = false;
    private float runnerStartTime = 1.3f;
    private long lastFootstepSoundMs = 0L;
    private final List<Letter> letters = new ArrayList<Letter>();
    private final List<TrailPoint> iconTrails = new ArrayList<TrailPoint>();
    private final List<IntroParticle> particles = new ArrayList<IntroParticle>();
    private final List<Shockwave> shockwaves = new ArrayList<Shockwave>();
    private long lastIntroRenderTime = 0L;
    private float iconX;
    private float iconY;
    private float iconScale = 1.0f;
    private float iconRotation = 0.0f;
    private float iconAlpha = 0.0f;
    private float collisionTargetX;
    private float collisionTargetY;
    private float iconRestX;
    private float iconRestY;
    private float shakeMagnitude = 0.0f;

    public IntroScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"Music Subtitles Intro"));
        this.parent = parent;
    }

    protected void method_25426() {
        boolean isFirstInit;
        super.method_25426();
        boolean bl = isFirstInit = this.startTimestamp <= 0L;
        if (isFirstInit) {
            this.startTimestamp = -1L;
            this.soundWhooshPlayed = false;
            this.soundImpactPlayed = false;
            this.soundShimmerPlayed = false;
            this.versionSoundPlayed = false;
            this.versionHitFlash = 0.0f;
            this.inTelegramSplash = false;
            this.telegramSplashStartTime = -1L;
            this.telegramSplashSoundPlayed = false;
            this.inBrowserWarning = false;
            this.browserWarningStartTime = -1L;
            this.browserWarningSoundPlayed = false;
            this.isExiting = false;
            this.exitStartTime = -1L;
            this.runnerStartTime = 1.3f;
            this.lastFootstepSoundMs = 0L;
            this.particles.clear();
            this.shockwaves.clear();
        }
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.method_31974() != null) {
                class_8685 textures;
                GameProfile profile = mc.method_53462();
                this.skinTexturesSupplier = mc.method_1582().method_73544(profile, true);
                class_8685 class_86852 = textures = this.skinTexturesSupplier != null ? this.skinTexturesSupplier.get() : null;
                if (textures == null) {
                    textures = class_1068.method_52854((GameProfile)profile);
                }
                if (textures != null) {
                    this.playerSkinTexture = textures.comp_1626().comp_3627();
                    this.isPlayerSlim = textures.comp_1629() == class_7920.field_41122;
                } else {
                    this.playerSkinTexture = class_1068.method_52854((GameProfile)profile).comp_1626().comp_3627();
                    this.isPlayerSlim = false;
                }
                class_5599 loadedModels = mc.method_31974();
                class_5601 playerLayer = new class_5601(class_2960.method_60655((String)"minecraft", (String)(this.isPlayerSlim ? "player_slim" : "player")), "main");
                class_630 modelPart = loadedModels.method_32072(playerLayer);
                this.runnerModel = new class_591(modelPart, this.isPlayerSlim);
                this.runnerModel.method_2805(true);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        this.letters.clear();
        this.iconTrails.clear();
        class_327 tr = this.field_22793;
        int totalWidth = tr.method_1727(TITLE_TEXT);
        int startX = (this.field_22789 - totalWidth) / 2 + 18;
        int centerY = this.field_22790 / 2 - 14;
        int curX = startX;
        for (int i = 0; i < TITLE_TEXT.length(); ++i) {
            char ch = TITLE_TEXT.charAt(i);
            int charWidth = tr.method_1727(String.valueOf(ch));
            if (ch == ' ') {
                curX += charWidth;
                continue;
            }
            int color = i < 5 ? -13312 : -16718337;
            this.letters.add(new Letter(ch, curX, centerY, color, i));
            curX += charWidth;
        }
        this.iconRestX = startX - 38;
        this.iconRestY = centerY - 12;
        this.collisionTargetX = this.iconRestX;
        this.collisionTargetY = this.iconRestY;
        if (isFirstInit) {
            this.iconX = -120.0f;
            this.iconY = centerY - 160;
            for (int p = 0; p < 45; ++p) {
                this.particles.add(new IntroParticle(this.random.nextFloat() * (float)this.field_22789, this.random.nextFloat() * (float)this.field_22790, (this.random.nextFloat() - 0.5f) * 0.4f, -0.3f - this.random.nextFloat() * 0.5f, this.random.nextBoolean() ? -13312 : -16718337, this.random.nextFloat() * 1.5f + 1.0f, this.random.nextInt(300) + 300, true));
            }
        }
    }

    private void transitionToTelegramSplash() {
        if (this.inTelegramSplash || this.inBrowserWarning) {
            return;
        }
        this.inTelegramSplash = true;
        this.telegramSplashStartTime = System.currentTimeMillis();
        if (!this.telegramSplashSoundPlayed) {
            this.telegramSplashSoundPlayed = true;
            this.playSound(class_3417.field_26980, 1.25f, 0.85f);
            this.playSound((class_3414)class_3417.field_14793.comp_349(), 1.15f, 0.7f);
        }
        for (int i = 0; i < 35; ++i) {
            float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
            float spd = 1.2f + this.random.nextFloat() * 3.5f;
            int col = this.random.nextBoolean() ? 0xFF24A1DE : (this.random.nextBoolean() ? -16718337 : -1);
            this.particles.add(new IntroParticle((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f - 30.0f, (float)Math.cos(angle) * spd, (float)Math.sin(angle) * spd - 1.0f, col, this.random.nextFloat() * 2.2f + 1.2f, 45, false));
        }
        this.shockwaves.add(new Shockwave((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f - 30.0f, 160.0f, 0xFF24A1DE));
    }

    private void transitionToBrowserWarning() {
        if (this.inBrowserWarning) {
            return;
        }
        this.inTelegramSplash = false;
        this.inBrowserWarning = true;
        this.browserWarningStartTime = System.currentTimeMillis();
        if (!this.browserWarningSoundPlayed) {
            this.browserWarningSoundPlayed = true;
            this.playSound((class_3414)class_3417.field_14793.comp_349(), 0.85f, 1.0f);
            this.playSound(class_3417.field_43154, 1.05f, 0.75f);
        }
        for (int i = 0; i < 40; ++i) {
            float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
            float spd = 1.4f + this.random.nextFloat() * 4.2f;
            int col = this.random.nextBoolean() ? -52429 : (this.random.nextBoolean() ? -35038 : -13312);
            this.particles.add(new IntroParticle((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f - 30.0f, (float)Math.cos(angle) * spd, (float)Math.sin(angle) * spd - 1.2f, col, this.random.nextFloat() * 2.2f + 1.2f, 50, false));
        }
        this.shockwaves.add(new Shockwave((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f - 30.0f, 180.0f, -52429));
        this.shockwaves.add(new Shockwave((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f - 30.0f, 120.0f, -35038));
    }

    private void startExit() {
        if (this.isExiting) {
            return;
        }
        this.isExiting = true;
        this.exitStartTime = System.currentTimeMillis();
        this.playSound((class_3414)class_3417.field_15015.comp_349(), 1.2f, 0.6f);
    }

    public static void openUrl(String url) {
        com.example.yandexmusichud.favorites.FavoritesManager.openUrlSafely(url);
    }

    private List<String> wrapText(String text, int maxWidth) {
        ArrayList<String> result = new ArrayList<String>();
        if (text == null || text.isBlank()) {
            return result;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
                continue;
            }
            if (this.field_22793.method_1727(String.valueOf(current) + " " + word) <= maxWidth) {
                current.append(" ").append(word);
                continue;
            }
            result.add(current.toString());
            current = new StringBuilder(word);
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float introDt = this.lastIntroRenderTime > 0L ? Math.min(0.08f, (float)(now - this.lastIntroRenderTime) / 1000.0f) : 0.016f;
        this.lastIntroRenderTime = now;
        float dt60 = Math.min(3.0f, introDt * 60.0f);

        if (this.startTimestamp <= 0L) {
            this.startTimestamp = now;
        }
        long elapsed = now - this.startTimestamp;
        float timeSec = (float)elapsed / 1000.0f;
        if (!this.inTelegramSplash && !this.inBrowserWarning && timeSec >= 6.2f) {
            this.transitionToTelegramSplash();
        }
        context.method_25294(0, 0, this.field_22789, this.field_22790, -16119278);
        this.renderVignette(context);
        if (!this.inTelegramSplash && !this.inBrowserWarning) {
            TrackState trackState = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : null;
            boolean hasLiveTrack = trackState != null && trackState.isActive() && !trackState.title().isEmpty();
            long beatInterval = 500L;
            long currentMusicPos = hasLiveTrack && trackState.getInterpolatedPositionMs() > 0L ? trackState.getInterpolatedPositionMs() : elapsed;
            float beatCycle = (float)(currentMusicPos % beatInterval) / (float)beatInterval;
            float beatEnergy = (float)Math.pow(Math.max(0.0, 1.0 - (double)beatCycle * 1.8), 2.2);
            float shakeX = 0.0f;
            float shakeY = 0.0f;
            float totalShake = this.shakeMagnitude + (timeSec > 2.6f ? beatEnergy * 1.2f : 0.0f);
            if (totalShake > 0.05f) {
                shakeX = (this.random.nextFloat() - 0.5f) * 2.0f * totalShake;
                shakeY = (this.random.nextFloat() - 0.5f) * 2.0f * totalShake;
                this.shakeMagnitude *= (float)Math.pow(0.88, dt60);
            }
            context.method_51448().pushMatrix();
            context.method_51448().translate(shakeX, shakeY);
            this.renderParticles(context, dt60);
            this.renderShockwaves(context, dt60);
            this.updateAndRenderIcon(context, timeSec, elapsed, beatEnergy);
            this.renderLetters(context, timeSec, elapsed, beatEnergy, beatCycle);
            this.renderVersionBadge(context, timeSec, elapsed, beatEnergy, beatCycle);
            this.renderBeatEqualizer(context, this.field_22789 / 2, this.field_22790 / 2 + 50, beatEnergy, elapsed, timeSec);
            this.renderTaglineAndShimmer(context, timeSec, elapsed);
            context.method_51448().popMatrix();
            this.updateAndRenderRunner(context, timeSec, elapsed, beatEnergy);
            if (hasLiveTrack) {
                this.renderNowPlayingPill(context, trackState, beatEnergy, timeSec);
            }
            if (timeSec > 2.5f && timeSec < 6.2f && (beatEnergy > 0.8f || this.random.nextInt(6) == 0)) {
                class_327 tr = this.field_22793;
                int titleW = tr.method_1727(TITLE_TEXT);
                int sx = (this.field_22789 - titleW) / 2 + this.random.nextInt(Math.max(1, titleW));
                int sy = this.field_22790 / 2 + 10;
                String note = this.random.nextBoolean() ? "\u266a" : (this.random.nextBoolean() ? "\u266b" : (this.random.nextBoolean() ? "\u266c" : "\u2726"));
                int col = this.random.nextBoolean() ? -13312 : (this.random.nextBoolean() ? -16718337 : -43657);
                this.particles.add(new IntroParticle(sx, sy, (this.random.nextFloat() - 0.5f) * 0.9f, -0.8f - this.random.nextFloat() * 1.2f, col, 8.5f, 75, true, note));
            }
            if (timeSec > 0.8f) {
                float hintAlpha = Math.min(1.0f, (timeSec - 0.8f) * 1.5f);
                float pulse = 0.6f + 0.4f * class_3532.method_15374((double)(timeSec * 3.0f));
                int alphaInt = (int)(hintAlpha * pulse * 140.0f);
                int hintColor = alphaInt << 24 | 0x888899;
                int hintX = (this.field_22789 - this.field_22793.method_1727(HINT_TEXT)) / 2;
                int hintY = hasLiveTrack ? this.field_22790 - 18 : this.field_22790 - 24;
                context.method_51433(this.field_22793, HINT_TEXT, hintX, hintY, hintColor, false);
            }
            if (timeSec < 0.6f) {
                float inAlpha = 1.0f - timeSec / 0.6f;
                int blackAlpha = (int)(class_3532.method_15363((float)inAlpha, (float)0.0f, (float)1.0f) * 255.0f);
                context.method_25294(0, 0, this.field_22789, this.field_22790, blackAlpha << 24);
            }
        } else if (this.inTelegramSplash) {
            this.renderParticles(context, dt60);
            this.renderShockwaves(context, dt60);
            float tgT = class_3532.method_15363((float)((float)(now - this.telegramSplashStartTime) / 400.0f), (float)0.0f, (float)1.0f);
            float tgEase = IntroScreen.easeOutCubic(tgT);
            int cardW = Math.min(420, this.field_22789 - 24);
            int cardH = Math.min(235, this.field_22790 - 36);
            int cardX = (this.field_22789 - cardW) / 2;
            int baseCardY = Math.max(16, (this.field_22790 - cardH - 32) / 2 + 6);
            float slideY = (1.0f - tgEase) * 22.0f;
            int cardY = (int)((float)baseCardY + slideY);
            int headerY = Math.max(10, cardY - 20);
            this.renderTelegramSplashHeader(context, tgEase, headerY);
            this.renderTelegramSplashCard(context, mouseX, mouseY, cardX, cardY, cardW, cardH, tgEase, now);
            this.renderTabBadgeBottomBar(context, mouseX, mouseY, tgEase);
            long tgElapsed = now - this.telegramSplashStartTime;
            if (tgElapsed > 14000L) {
                this.transitionToBrowserWarning();
            }
        } else {
            this.renderParticles(context, dt60);
            this.renderShockwaves(context, dt60);
            float warnT = class_3532.method_15363((float)((float)(now - this.browserWarningStartTime) / 450.0f), (float)0.0f, (float)1.0f);
            float warnEase = IntroScreen.easeOutCubic(warnT);
            int cardW = Math.min(420, this.field_22789 - 24);
            int cardH = Math.min(230, this.field_22790 - 36);
            int cardX = (this.field_22789 - cardW) / 2;
            int baseCardY = Math.max(16, (this.field_22790 - cardH - 32) / 2 + 6);
            float slideY = (1.0f - warnEase) * 25.0f;
            int cardY = (int)((float)baseCardY + slideY);
            int headerY = Math.max(10, cardY - 18);
            this.renderBrowserWarningHeader(context, warnEase, headerY);
            this.renderBrowserWarningCard(context, mouseX, mouseY, cardX, cardY, cardW, cardH, warnEase, now);
            this.renderTabBadgeBottomBar(context, mouseX, mouseY, warnEase);
            long warnElapsed = now - this.browserWarningStartTime;
            if (warnElapsed > 16000L && !this.isExiting) {
                this.startExit();
            }
        }
        if (this.isExiting) {
            float exitT = class_3532.method_15363((float)((float)(now - this.exitStartTime) / 260.0f), (float)0.0f, (float)1.0f);
            int blackAlpha = (int)(exitT * 255.0f);
            context.method_25294(0, 0, this.field_22789, this.field_22790, blackAlpha << 24);
            if (exitT >= 1.0f) {
                this.method_25419();
            }
        }
    }

    private void renderTelegramSplashHeader(class_332 context, float alpha, int centerY) {
        int alphaInt = (int)(alpha * 255.0f);
        if (alphaInt <= 2) {
            return;
        }
        class_327 tr = this.field_22793;
        String title = "\u2726 \u041e\u0424\u0418\u0426\u0418\u0410\u041b\u042c\u041d\u042b\u0419 \u0418\u0421\u0422\u041e\u0427\u041d\u0418\u041a \u2726";
        int tw = tr.method_1727(title);
        int iconSize = 16;
        int totalW = iconSize + 8 + tw;
        int startX = (this.field_22789 - totalW) / 2;
        int iconY = centerY - iconSize / 2;
        int textY = centerY - 4;
        context.method_25291(class_10799.field_56883, TELEGRAM_LOGO, startX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, alphaInt << 24 | 0xFFFFFF);
        context.method_51433(tr, "\u00a7b\u2726 \u00a7f\u041e\u0424\u0418\u0426\u0418\u0410\u041b\u042c\u041d\u042b\u0419 \u00a7b\u0418\u0421\u0422\u041e\u0427\u041d\u0418\u041a \u00a7b\u2726", startX + iconSize + 8, textY, alphaInt << 24 | 0xFFFFFF, true);
    }

    private void renderTelegramSplashCard(class_332 context, int mouseX, int mouseY, int cardX, int cardY, int cardW, int cardH, float alpha, long now) {
        int alphaInt = (int)(alpha * 255.0f);
        if (alphaInt <= 2) {
            return;
        }
        class_327 tr = this.field_22793;
        int bgCol = (int)(alpha * 242.0f) << 24 | 0x0C1626;
        int borderCol = (int)(alpha * 140.0f) << 24 | 0x24A1DE;
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 10, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 10, borderCol, borderCol);
        context.method_25294(cardX + 16, cardY, cardX + cardW - 16, cardY + 1, (int)(alpha * 160.0f) << 24 | 0x64C5FF);

        int bannerPad = 12;
        int bannerW = cardW - bannerPad * 2;
        int bannerH = 17;
        int bannerX = cardX + bannerPad;
        int bannerY = cardY + 10;
        int bannerBg = (int)(alpha * 160.0f) << 24 | 0x0F2E4D;
        int bannerBorder = (int)(alpha * 190.0f) << 24 | 0x24A1DE;
        MusicHudRenderer.renderRoundedRect(context, bannerX, bannerY, bannerW, bannerH, 4, bannerBg);
        MusicHudRenderer.renderRoundedBorder(context, bannerX, bannerY, bannerW, bannerH, 4, bannerBorder, bannerBorder);
        int dotCol = (int)(alpha * 255.0f) << 24 | 0x24A1DE;
        MusicHudRenderer.renderRoundedRect(context, bannerX + 8, bannerY + 5, 6, 6, 3, dotCol);
        String bannerText = "\u2708 TELEGRAM-\u041a\u0410\u041d\u0410\u041b  |  \u041e\u0424\u0418\u0426\u0418\u0410\u041b\u042c\u041d\u042b\u0419 \u0420\u0415\u041b\u0418\u0417 \u041c\u041e\u0414\u0410";
        context.method_51433(tr, "\u00a7b" + bannerText, bannerX + 20, bannerY + 4, alphaInt << 24 | 0xFFFFFF, false);

        int logoSize = 48;
        int logoX = cardX + 16;
        float floatY = (float)Math.sin((double)now * 0.0035) * 2.0f;
        float pulse = (float)Math.sin((double)now * 0.005) * 0.05f + 1.0f;
        int logoY = cardY + 36;
        MusicHudRenderer.renderRoundedRect(context, logoX - 3, (int)((float)logoY + floatY - 3.0f), logoSize + 6, logoSize + 6, (logoSize + 6) / 2, (int)(alpha * 75.0f) << 24 | 0x24A1DE);

        context.method_51448().pushMatrix();
        context.method_51448().translate((float)logoX + (float)logoSize / 2.0f, (float)logoY + floatY + (float)logoSize / 2.0f);
        context.method_51448().scale(pulse, pulse);
        context.method_25291(class_10799.field_56883, TELEGRAM_LOGO, -logoSize / 2, -logoSize / 2, 0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize, alphaInt << 24 | 0xFFFFFF);
        context.method_51448().popMatrix();

        int textX = cardX + 74;
        int lineY = cardY + 36;
        context.method_51433(tr, "§fМод скачан с §bTelegram-канала", textX, lineY, alphaInt << 24 | 0xFFFFFF, false);
        lineY += 13;
        context.method_51433(tr, "§b✈ @TG_Nia_Teppelin0", textX, lineY, alphaInt << 24 | 0x24A1DE, true);
        lineY += 13;
        context.method_51433(tr, "§7Версия: §e2.8.1 §7| Автор: §dНия (Nia_Teppelin_0)", textX, lineY, alphaInt << 24 | 0xAAAAAA, false);

        int listY = cardY + 94;
        context.method_51433(tr, "§a✔ §fОригинальная версия без изменений и угроз", cardX + 16, listY, alphaInt << 24 | 0xFFFFFF, false);
        listY += 12;
        context.method_51433(tr, "§e⚡ §fСвежие обновления, ранние патчи и поддержка", cardX + 16, listY, alphaInt << 24 | 0xFFFFFF, false);
        listY += 12;
        context.method_51433(tr, "§d✦ §f3D-караоке в мире, Яндекс Музыка, Twitch и DA", cardX + 16, listY, alphaInt << 24 | 0xFFFFFF, false);

        int btnY = cardY + cardH - 28;
        int btnH = 20;
        int gap = 10;
        int btnW = (cardW - 32 - gap) / 2;
        int btnTgX = cardX + 16;
        int btnNextX = btnTgX + btnW + gap;

        boolean hoverTg = mouseX >= btnTgX && mouseX <= btnTgX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnTgBg = hoverTg ? (int)(alpha * 240.0f) << 24 | 0x1A4F7C : (int)(alpha * 180.0f) << 24 | 0x113350;
        int btnTgBorder = hoverTg ? (int)(alpha * 255.0f) << 24 | 0x24A1DE : (int)(alpha * 180.0f) << 24 | 0x1C6A9E;
        MusicHudRenderer.renderRoundedRect(context, btnTgX, btnY, btnW, btnH, 5, btnTgBg);
        MusicHudRenderer.renderRoundedBorder(context, btnTgX, btnY, btnW, btnH, 5, btnTgBorder, btnTgBorder);
        String tgBtnText = hoverTg ? "\u00a7b\u2708 \u041e\u0442\u043a\u0440\u044b\u0442\u044c Telegram" : "\u00a7f\u2708 \u041e\u0442\u043a\u0440\u044b\u0442\u044c Telegram";
        int tw1 = tr.method_1727(tgBtnText);
        context.method_51433(tr, tgBtnText, btnTgX + (btnW - tw1) / 2, btnY + 6, alphaInt << 24 | 0xFFFFFF, true);

        boolean hoverNext = mouseX >= btnNextX && mouseX <= btnNextX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnNextBg = hoverNext ? (int)(alpha * 240.0f) << 24 | 0x184D35 : (int)(alpha * 180.0f) << 24 | 0x103322;
        int btnNextBorder = hoverNext ? (int)(alpha * 255.0f) << 24 | 0x00FF88 : (int)(alpha * 180.0f) << 24 | 0x00AA55;
        MusicHudRenderer.renderRoundedRect(context, btnNextX, btnY, btnW, btnH, 5, btnNextBg);
        MusicHudRenderer.renderRoundedBorder(context, btnNextX, btnY, btnW, btnH, 5, btnNextBorder, btnNextBorder);
        String nextBtnText = hoverNext ? "\u00a7a\u041f\u043e\u043d\u044f\u0442\u043d\u043e, \u0434\u0430\u043b\u0435\u0435 \u00a7f\u2794" : "\u00a7f\u041f\u043e\u043d\u044f\u0442\u043d\u043e, \u0434\u0430\u043b\u0435\u0435 \u00a7a\u2794";
        int tw2 = tr.method_1727(nextBtnText);
        context.method_51433(tr, nextBtnText, btnNextX + (btnW - tw2) / 2, btnY + 6, alphaInt << 24 | 0xFFFFFF, true);

        long tgElapsed = now - this.telegramSplashStartTime;
        float timerNorm = Math.max(0.0f, 1.0f - (float)tgElapsed / 14000.0f);
        int timerW = (int)((float)(cardW - 20) * timerNorm);
        if (timerW > 0) {
            context.method_25294(cardX + 10, cardY + cardH - 3, cardX + 10 + timerW, cardY + cardH - 2, (int)(alpha * 160.0f) << 24 | 0x24A1DE);
        }
        int hintY = Math.min(this.field_22790 - 14, cardY + cardH + 6);
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)"\u00a78\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043b\u044e\u0431\u0443\u044e \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u0438\u043b\u0438 \u043a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u041b\u041a\u041c \u0434\u043b\u044f \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0435\u043d\u0438\u044f"), this.field_22789 / 2, hintY, (int)(alpha * 160.0f) << 24 | 0x888899);
    }

    private void renderBrowserWarningHeader(class_332 context, float alpha, int centerY) {
        int alphaInt = (int)(alpha * 255.0f);
        if (alphaInt <= 2) {
            return;
        }
        class_327 tr = this.field_22793;
        String title = "\u0421\u041e\u0412\u041c\u0415\u0421\u0422\u0418\u041c\u041e\u0421\u0422\u042c \u0411\u0420\u0410\u0423\u0417\u0415\u0420\u041e\u0412";
        int tw = tr.method_1727(title);
        int iconSize = 14;
        int totalW = iconSize + 6 + tw;
        int startX = (this.field_22789 - totalW) / 2;
        int iconY = centerY - iconSize / 2;
        int textY = centerY - 4;
        context.method_25291(class_10799.field_56883, YANDEX_BROWSER_ICON, startX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, alphaInt << 24 | 0xFFFFFF);
        context.method_51433(tr, "\u00a7c\u26a0 \u00a7f\u0421\u041e\u0412\u041c\u0415\u0421\u0422\u0418\u041c\u041e\u0421\u0422\u042c \u00a7c\u0411\u0420\u0410\u0423\u0417\u0415\u0420\u041e\u0412 \u00a7c\u26a0", startX + iconSize + 6, textY, alphaInt << 24 | 0xFFFFFF, true);
    }

    private void renderBrowserWarningCard(class_332 context, int mouseX, int mouseY, int cardX, int cardY, int cardW, int cardH, float alpha, long now) {
        int alphaInt = (int)(alpha * 255.0f);
        if (alphaInt <= 2) {
            return;
        }
        int bgCol = (int)(alpha * 246.0f) << 24 | 0x130F15;
        int borderCol = (int)(alpha * 150.0f) << 24 | 0xFF3D2E;
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 10, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 10, borderCol, borderCol);
        context.method_25294(cardX + 16, cardY, cardX + cardW - 16, cardY + 1, (int)(alpha * 160.0f) << 24 | 0xFF7755);
        long warnElapsed = now - this.browserWarningStartTime;
        float popT = class_3532.method_15363((float)((float)warnElapsed / 480.0f), (float)0.0f, (float)1.0f);
        float popScale = IntroScreen.easeOutBack(popT);
        float floatBob = (float)Math.sin((double)warnElapsed * 0.0035) * 2.5f;
        int iconBaseSize = cardH < 225 ? 36 : 42;
        int iconRenderSize = Math.max(1, (int)((float)iconBaseSize * popScale));
        int iconCenterX = cardX + cardW / 2;
        int iconY = (int)((float)(cardY + 12) + floatBob);
        float glowPulse = 0.5f + 0.5f * (float)Math.sin((double)warnElapsed * 0.0055);
        int glowRadius = (int)((float)iconRenderSize * 0.72f + glowPulse * 5.0f);
        int glowAlpha = (int)(alpha * (0.35f + glowPulse * 0.3f) * 160.0f);
        MusicHudRenderer.renderRoundedRect(context, iconCenterX - glowRadius, iconY + iconRenderSize / 2 - glowRadius, glowRadius * 2, glowRadius * 2, glowRadius, glowAlpha << 24 | 0xFF2200);
        context.method_25291(class_10799.field_56883, YANDEX_BROWSER_ICON, iconCenterX - iconRenderSize / 2, iconY, 0.0f, 0.0f, iconRenderSize, iconRenderSize, iconRenderSize, iconRenderSize, alphaInt << 24 | 0xFFFFFF);
        int badgeR = 6;
        int badgeX = iconCenterX + iconRenderSize / 2 - 3;
        int badgeY = iconY + iconRenderSize - 5;
        MusicHudRenderer.renderRoundedRect(context, badgeX - badgeR, badgeY - badgeR, badgeR * 2, badgeR * 2, badgeR, (int)(alpha * 255.0f) << 24 | 0xEE1122);
        MusicHudRenderer.renderRoundedBorder(context, badgeX - badgeR, badgeY - badgeR, badgeR * 2, badgeR * 2, badgeR, (int)(alpha * 255.0f) << 24 | 0xFFFFFF);
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)"\u00a7f\u2715"), badgeX, badgeY - 4, alphaInt << 24 | 0xFFFFFF);
        float textT1 = class_3532.method_15363((float)((float)(warnElapsed - 70L) / 380.0f), (float)0.0f, (float)1.0f);
        float ease1 = IntroScreen.easeOutCubic(textT1);
        float slideY1 = (1.0f - ease1) * 12.0f;
        int alpha1 = (int)(alpha * ease1 * 255.0f);
        int curY = iconY + iconRenderSize + 6 + (int)slideY1;
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)"\u00a7c\u00a7l\u042f\u043d\u0434\u0435\u043a\u0441 \u0411\u0440\u0430\u0443\u0437\u0435\u0440 \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442\u0441\u044f"), iconCenterX, curY, alpha1 << 24 | 0xFFFFFF);
        curY += 12;
        float textT2 = class_3532.method_15363((float)((float)(warnElapsed - 140L) / 380.0f), (float)0.0f, (float)1.0f);
        float ease2 = IntroScreen.easeOutCubic(textT2);
        float slideY2 = (1.0f - ease2) * 12.0f;
        int alpha2 = (int)(alpha * ease2 * 255.0f);
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)"\u00a7e\u041c\u043e\u0434 \u043d\u0435 \u043c\u043e\u0436\u0435\u0442 \u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0442\u044c \u0442\u0440\u0435\u043a\u0438 \u0438 \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b \u0438\u0437 \u044d\u0442\u043e\u0433\u043e \u0431\u0440\u0430\u0443\u0437\u0435\u0440\u0430"), iconCenterX, curY += (int)slideY2, alpha2 << 24 | 0xFFFFFF);
        curY += 13;
        float textT3 = class_3532.method_15363((float)((float)(warnElapsed - 210L) / 380.0f), (float)0.0f, (float)1.0f);
        float ease3 = IntroScreen.easeOutCubic(textT3);
        float slideY3 = (1.0f - ease3) * 12.0f;
        int alpha3 = (int)(alpha * ease3 * 255.0f);
        int boxPad = 12;
        int boxW = cardW - boxPad * 2;
        int boxH = cardH < 225 ? 62 : 68;
        int boxX = cardX + boxPad;
        int boxY = curY + (int)slideY3;
        int boxBg = (int)(alpha * ease3 * 190.0f) << 24 | 0x1A1422;
        int boxBorder = (int)(alpha * ease3 * 160.0f) << 24 | 0x664433;
        MusicHudRenderer.renderRoundedRect(context, boxX, boxY, boxW, boxH, 6, boxBg);
        MusicHudRenderer.renderRoundedBorder(context, boxX, boxY, boxW, boxH, 6, boxBorder, boxBorder);
        int inX = boxX + 8;
        int inY = boxY + 5;
        context.method_51433(this.field_22793, "\u00a76\u2022 \u041f\u0440\u0438\u0447\u0438\u043d\u0430: \u00a77\u042f\u043d\u0434\u0435\u043a\u0441 \u0411\u0440\u0430\u0443\u0437\u0435\u0440 \u0431\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u0442 \u0441\u0435\u0441\u0441\u0438\u0438 Windows Media", inX, inY, alpha3 << 24 | 0xFFFFFF, false);
        context.method_51433(this.field_22793, "\u00a7a\u2714 \u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u0435\u043c\u044b\u0435 \u0441\u043f\u043e\u0441\u043e\u0431\u044b \u043f\u0440\u043e\u0441\u043b\u0443\u0448\u0438\u0432\u0430\u043d\u0438\u044f:", inX, inY += 11, alpha3 << 24 | 0xFFFFFF, false);
        context.method_51433(this.field_22793, "  \u00a7f\u2022 \u0411\u0440\u0430\u0443\u0437\u0435\u0440\u044b: \u00a7bGoogle Chrome\u00a77, \u00a7eOpera GX\u00a77, \u00a79Firefox\u00a77, \u00a7fEdge", inX, inY += 11, alpha3 << 24 | 0xDDDDDD, false);
        context.method_51433(this.field_22793, "  \u00a7f\u2022 \u041f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u0435: \u00a76\u041e\u0444\u0438\u0446\u0438\u0430\u043b\u044c\u043d\u0430\u044f \u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430 \u0434\u043b\u044f Windows", inX, inY += 11, alpha3 << 24 | 0xDDDDDD, false);
        float textT4 = class_3532.method_15363((float)((float)(warnElapsed - 280L) / 380.0f), (float)0.0f, (float)1.0f);
        float ease4 = IntroScreen.easeOutCubic(textT4);
        int alpha4 = (int)(alpha * ease4 * 255.0f);
        int btnW = 210;
        int btnH = 20;
        int btnX = cardX + (cardW - btnW) / 2;
        int btnY = cardY + cardH - 26;
        boolean isBtnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnBg = isBtnHovered ? (int)(alpha * ease4 * 240.0f) << 24 | 0x1E4D2B : (int)(alpha * ease4 * 180.0f) << 24 | 0x14331C;
        int btnBorderCol = isBtnHovered ? (int)(alpha * ease4 * 255.0f) << 24 | 0xFF88 : (int)(alpha * ease4 * 180.0f) << 24 | 0x22C55E;
        MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 5, btnBg);
        MusicHudRenderer.renderRoundedBorder(context, btnX, btnY, btnW, btnH, 5, btnBorderCol, btnBorderCol);
        String btnText = isBtnHovered ? "\u00a7a\u2714 \u0414\u0430\u043b\u0435\u0435: \u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u043a\u043b\u0430\u0432\u0438\u0448 \u00a7f\u2794" : "\u00a7f\u0414\u0430\u043b\u0435\u0435: \u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u043a\u043b\u0430\u0432\u0438\u0448 \u00a7a\u2794";
        int bTextW = this.field_22793.method_1727(btnText);
        context.method_51433(this.field_22793, btnText, btnX + (btnW - bTextW) / 2, btnY + 6, alpha4 << 24 | 0xFFFFFF, true);
        float timerNorm = Math.max(0.0f, 1.0f - (float)warnElapsed / 16000.0f);
        int timerW = (int)((float)(cardW - 20) * timerNorm);
        if (timerW > 0) {
            context.method_25294(cardX + 10, cardY + cardH - 3, cardX + 10 + timerW, cardY + cardH - 2, (int)(alpha * 150.0f) << 24 | 0xFF3D2E);
        }
        int hintY = Math.min(this.field_22790 - 14, cardY + cardH + 6);
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)"\u00a78\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043b\u044e\u0431\u0443\u044e \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u0438\u043b\u0438 \u043a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u041b\u041a\u041c \u0434\u043b\u044f \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0435\u043d\u0438\u044f"), this.field_22789 / 2, hintY, (int)(alpha * 160.0f) << 24 | 0x888899);
    }

    private void updateAndRenderRunner(class_332 context, float timeSec, long elapsed, float beatEnergy) {
        class_310 mc2;
        if (timeSec < this.runnerStartTime) {
            return;
        }
        float runElapsed = timeSec - this.runnerStartTime;
        float runDuration = 3.6f;
        if (runElapsed < 0.0f || runElapsed > runDuration) {
            return;
        }
        if (this.runnerModel == null) {
            try {
                mc2 = class_310.method_1551();
                if (mc2 != null && mc2.method_31974() != null) {
                    class_5599 loadedModels = mc2.method_31974();
                    class_5601 playerLayer = new class_5601(class_2960.method_60655((String)"minecraft", (String)(this.isPlayerSlim ? "player_slim" : "player")), "main");
                    class_630 modelPart = loadedModels.method_32072(playerLayer);
                    this.runnerModel = new class_591(modelPart, this.isPlayerSlim);
                    this.runnerModel.method_2805(true);
                }
            }
            catch (Throwable ignored) {

            }
        }
        if (this.runnerModel == null) {
            return;
        }
        if (this.skinTexturesSupplier != null) {
            try {
                class_8685 curTextures = this.skinTexturesSupplier.get();
                if (curTextures != null && curTextures.comp_1626() != null && curTextures.comp_1626().comp_3627() != null) {
                    this.playerSkinTexture = curTextures.comp_1626().comp_3627();
                    this.isPlayerSlim = curTextures.comp_1629() == class_7920.field_41122;
                }
            }
            catch (Throwable curTextures) {

            }
        }
        if (this.playerSkinTexture == null) {
            try {
                GameProfile profile;
                class_8685 def;
                mc2 = class_310.method_1551();
                if (mc2 != null && (def = class_1068.method_52854((GameProfile)(profile = mc2.method_53462()))) != null && def.comp_1626() != null) {
                    this.playerSkinTexture = def.comp_1626().comp_3627();
                }
            }
            catch (Throwable mc3) {

            }
        }
        if (this.playerSkinTexture == null) {
            return;
        }
        float progress = runElapsed / runDuration;
        float runnerX = class_3532.method_16439((float)progress, (float)-50.0f, (float)((float)this.field_22789 + 50.0f));
        float groundY = (float)this.field_22790 - 32.0f;
        float runSpeed = 16.5f;
        float phase = runElapsed * runSpeed;
        float sinP = class_3532.method_15374((double)phase);
        float cosP = class_3532.method_15362((double)phase);
        float jumpStart = 0.95f;
        float jumpEnd = 1.65f;
        boolean isJumping = runElapsed >= jumpStart && runElapsed <= jumpEnd;
        float jumpArc = 0.0f;
        float jumpY = 0.0f;
        if (isJumping) {
            float jumpT = (runElapsed - jumpStart) / (jumpEnd - jumpStart);
            jumpArc = class_3532.method_15374((double)(jumpT * (float)Math.PI));
            jumpY = jumpArc * 26.0f;
        }
        float normalBounce = Math.abs(sinP) * 3.8f * (1.0f - jumpArc);
        float currentY = groundY - normalBounce - jumpY;
        long nowMs = System.currentTimeMillis();
        if (!isJumping && nowMs - this.lastFootstepSoundMs > 280L && runnerX > 0.0f && runnerX < (float)this.field_22789) {
            this.lastFootstepSoundMs = nowMs;
            this.playSound(class_3417.field_15053, 1.25f, 0.35f);
        }
        if (runnerX > -20.0f && runnerX < (float)this.field_22789 + 20.0f) {
            int shadowW = (int)(28.0f - jumpArc * 10.0f);
            int shadowH = (int)(7.0f - jumpArc * 3.0f);
            int shadowAlpha = (int)((0.45f - jumpArc * 0.22f) * 255.0f);
            MusicHudRenderer.renderRoundedRect(context, (int)(runnerX - (float)shadowW / 2.0f), (int)(groundY - 2.0f), shadowW, shadowH, 4, shadowAlpha << 24);
        }
        this.runnerModel.method_2805(true);
        this.runnerModel.field_3398.method_41923();
        this.runnerModel.field_3394.method_41923();
        this.runnerModel.field_3391.method_41923();
        this.runnerModel.field_3483.method_41923();
        this.runnerModel.field_27433.method_41923();
        this.runnerModel.field_3484.method_41923();
        this.runnerModel.field_3401.method_41923();
        this.runnerModel.field_3486.method_41923();
        this.runnerModel.field_3397.method_41923();
        this.runnerModel.field_3482.method_41923();
        this.runnerModel.field_3392.method_41923();
        this.runnerModel.field_3479.method_41923();
        float baseRightLeg = sinP * 1.15f;
        float baseLeftLeg = -sinP * 1.15f;
        float baseRightArm = -sinP * 1.1f;
        float baseLeftArm = sinP * 1.1f;
        if (isJumping) {
            this.runnerModel.field_3392.field_3654 = class_3532.method_16439((float)jumpArc, (float)baseRightLeg, (float)(-0.75f - jumpArc * 0.35f));
            this.runnerModel.field_3397.field_3654 = class_3532.method_16439((float)jumpArc, (float)baseLeftLeg, (float)(0.85f + jumpArc * 0.4f));
            this.runnerModel.field_3401.field_3654 = class_3532.method_16439((float)jumpArc, (float)baseRightArm, (float)(-0.95f - jumpArc * 0.25f));
            this.runnerModel.field_27433.field_3654 = class_3532.method_16439((float)jumpArc, (float)baseLeftArm, (float)(0.7f + jumpArc * 0.3f));
            this.runnerModel.field_3401.field_3674 = class_3532.method_16439((float)jumpArc, (float)0.16f, (float)0.4f);
            this.runnerModel.field_27433.field_3674 = class_3532.method_16439((float)jumpArc, (float)-0.16f, (float)-0.4f);
            this.runnerModel.field_3391.field_3654 = class_3532.method_16439((float)jumpArc, (float)0.22f, (float)0.34f);
        } else {
            this.runnerModel.field_3392.field_3654 = baseRightLeg;
            this.runnerModel.field_3397.field_3654 = baseLeftLeg;
            this.runnerModel.field_3401.field_3654 = baseRightArm;
            this.runnerModel.field_27433.field_3654 = baseLeftArm;
            this.runnerModel.field_3401.field_3674 = 0.16f;
            this.runnerModel.field_27433.field_3674 = -0.16f;
            this.runnerModel.field_3391.field_3654 = 0.22f;
        }
        this.runnerModel.field_3392.field_3674 = 0.03f;
        this.runnerModel.field_3397.field_3674 = -0.03f;
        this.runnerModel.field_3392.field_3675 = 0.0f;
        this.runnerModel.field_3397.field_3675 = 0.0f;
        this.runnerModel.field_3401.field_3675 = 0.06f;
        this.runnerModel.field_27433.field_3675 = -0.06f;
        this.runnerModel.field_3391.field_3675 = sinP * 0.12f;
        this.runnerModel.field_3391.field_3674 = cosP * 0.04f;
        this.runnerModel.field_3398.field_3654 = -0.16f + class_3532.method_15362((double)(phase * 2.0f)) * 0.03f;
        this.runnerModel.field_3398.field_3675 = 0.08f;
        this.runnerModel.field_3398.field_3674 = -cosP * 0.03f;
        int boxW = 54;
        int boxH = 76;
        int vpX1 = (int)(runnerX - (float)boxW / 2.0f);
        int vpX2 = (int)(runnerX + (float)boxW / 2.0f);
        int vpY1 = (int)(currentY - (float)boxH);
        int vpY2 = (int)currentY;
        float scale = 0.97f * (float)boxH / 2.125f;
        try {
            context.method_70854(this.runnerModel, this.playerSkinTexture, scale, -2.0f, 72.0f, -1.0625f, vpX1, vpY1, vpX2, vpY2);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        if (runnerX > -10.0f && runnerX < (float)this.field_22789 + 10.0f) {
            String username = this.field_22787 != null && this.field_22787.method_1548() != null ? this.field_22787.method_1548().method_1676() : "Player";
            String tag = "\u00a7f" + username;
            int nw = this.field_22793.method_1727(tag);
            int tagX = (int)(runnerX - (float)nw / 2.0f);
            int tagY = (int)(currentY - (float)boxH - 14.0f);
            MusicHudRenderer.renderRoundedRect(context, tagX - 4, tagY - 2, nw + 8, 11, 4, 0x77000000);
            context.method_51433(this.field_22793, tag, tagX, tagY, -1, true);
        }
        if (runnerX > -10.0f && runnerX < (float)this.field_22789 + 10.0f) {
            if (!isJumping && Math.abs(cosP) > 0.85f && this.random.nextInt(2) == 0) {
                this.particles.add(new IntroParticle(runnerX - 10.0f + (this.random.nextFloat() - 0.5f) * 6.0f, groundY - 1.0f, -0.7f - this.random.nextFloat() * 0.7f, -0.2f - this.random.nextFloat() * 0.4f, -1999844148, 1.8f, 22, false));
            }
            if (isJumping || beatEnergy > 0.85f || this.random.nextInt(12) == 0) {
                String sym = this.random.nextBoolean() ? "\u266a" : (this.random.nextBoolean() ? "\u266b" : "\u2726");
                int col = this.random.nextBoolean() ? -16718337 : -13312;
                this.particles.add(new IntroParticle(runnerX - 8.0f, currentY - 14.0f - this.random.nextFloat() * 12.0f, -0.5f - this.random.nextFloat() * 0.6f, -0.4f - this.random.nextFloat() * 0.6f, col, 6.5f, 30, true, sym));
            }
        }
    }

    private void updateAndRenderIcon(class_332 context, float timeSec, long elapsed, float beatEnergy) {
        if (timeSec < 1.1f) {
            this.iconAlpha = 0.0f;
        } else if (timeSec < 2.3f) {
            float t = class_3532.method_15363((float)((timeSec - 1.1f) / 1.2f), (float)0.0f, (float)1.0f);
            if (t > 0.05f && !this.soundWhooshPlayed) {
                this.soundWhooshPlayed = true;
                this.playSound(class_3417.field_14706, 1.15f, 0.95f);
                this.playSound(class_3417.field_14702, 1.35f, 0.6f);
            }
            float ease = t * t * t;
            this.iconX = class_3532.method_16439((float)ease, (float)-160.0f, (float)this.collisionTargetX);
            this.iconY = class_3532.method_16439((float)ease, (float)(this.collisionTargetY - 160.0f), (float)this.collisionTargetY);
            this.iconScale = class_3532.method_16439((float)ease, (float)3.2f, (float)1.25f);
            this.iconRotation = ease * 360.0f * 1.5f;
            this.iconAlpha = Math.min(1.0f, t * 2.5f);
            if (t > 0.05f) {
                for (int k = 0; k < 2; ++k) {
                    float px = this.iconX + 16.0f + (this.random.nextFloat() - 0.5f) * 10.0f;
                    float py = this.iconY + 16.0f + (this.random.nextFloat() - 0.5f) * 10.0f;
                    int pColor = this.random.nextBoolean() ? -13312 : (this.random.nextBoolean() ? -16718337 : -1);
                    this.particles.add(new IntroParticle(px, py, (this.random.nextFloat() - 0.5f) * 0.8f, (this.random.nextFloat() - 0.5f) * 0.8f, pColor, this.random.nextFloat() * 2.2f + 1.2f, 18, false));
                }
            }
        } else if (timeSec >= 2.3f && timeSec < 2.45f) {
            if (!this.soundImpactPlayed) {
                this.soundImpactPlayed = true;
                this.triggerImpact(this.collisionTargetX, this.collisionTargetY);
            }
            this.iconX = this.collisionTargetX;
            this.iconY = this.collisionTargetY;
            this.iconScale = 1.35f;
            this.iconRotation = 0.0f;
            this.iconAlpha = 1.0f;
        } else {
            float t = class_3532.method_15363((float)((timeSec - 2.45f) / 1.2f), (float)0.0f, (float)1.0f);
            float spring = (float)(Math.sin((double)t * Math.PI * 2.5) * Math.exp((double)(-t) * 3.5));
            float beatScale = timeSec > 2.6f ? 1.0f + beatEnergy * 0.12f : 1.0f;
            this.iconX = this.iconRestX - spring * 8.0f;
            this.iconY = this.iconRestY + (float)Math.sin(timeSec * 2.8f) * 2.5f - (timeSec > 2.6f ? beatEnergy * 3.5f : 0.0f);
            this.iconScale = (1.0f + spring * 0.3f) * beatScale;
            this.iconRotation = spring * 10.0f;
            this.iconAlpha = 1.0f;
        }
        if (this.iconAlpha > 0.02f) {
            if (timeSec > 2.6f) {
                int glowAlpha = (int)(this.iconAlpha * (0.2f + beatEnergy * 0.35f) * 120.0f);
                int glowCol = glowAlpha << 24 | 0xE5FF;
                int gSize = (int)(32.0f * this.iconScale);
                int gx = (int)(this.iconX + 16.0f - (float)(gSize / 2));
                int gy = (int)(this.iconY + 16.0f - (float)(gSize / 2));
                MusicHudRenderer.renderRoundedRect(context, gx - 2, gy - 2, gSize + 4, gSize + 4, 8, glowCol);
            }
            this.renderIconAt(context, this.iconX, this.iconY, this.iconScale, this.iconRotation, this.iconAlpha);
        }
    }

    private void triggerImpact(float hitX, float hitY) {
        this.shakeMagnitude = 9.0f;
        this.iconTrails.clear();
        this.playSound(class_3417.field_14833, 0.8f, 1.0f);
        this.playSound(class_3417.field_15016, 0.9f, 1.0f);
        this.playSound((class_6880<class_3414>)class_3417.field_15152, 1.65f, 0.6f);
        this.shockwaves.add(new Shockwave(hitX + 16.0f, hitY + 16.0f, 210.0f, -13312));
        this.shockwaves.add(new Shockwave(hitX + 16.0f, hitY + 16.0f, 150.0f, -16718337));
        for (int i = 0; i < 60; ++i) {
            float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
            float speed = 2.5f + this.random.nextFloat() * 8.5f;
            int col = this.random.nextBoolean() ? -13312 : (this.random.nextBoolean() ? -16718337 : -1);
            this.particles.add(new IntroParticle(hitX + 16.0f, hitY + 16.0f, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed - 1.8f, col, this.random.nextFloat() * 2.5f + 1.2f, this.random.nextInt(40) + 35, false));
        }
        for (Letter l : this.letters) {
            l.scheduleImpulse(l.index * 55);
        }
    }

    private void renderLetters(class_332 context, float timeSec, long elapsed, float beatEnergy, float beatCycle) {
        class_327 tr = this.field_22793;
        for (Letter l : this.letters) {
            l.update(elapsed, this);
            float drawX = l.targetX;
            float beatBounce = 0.0f;
            if (timeSec > 2.6f) {
                beatBounce = -beatEnergy * 5.0f * (float)Math.sin((double)beatCycle * Math.PI + (double)((float)l.index * 0.22f));
            }
            float drawY = l.targetY + l.offsetY + beatBounce;
            context.method_51448().pushMatrix();
            context.method_51448().translate(drawX + (float)tr.method_1727(String.valueOf(l.ch)) / 2.0f, drawY + 4.0f);
            float scaleY = l.scaleY * (timeSec > 2.6f ? 1.0f + beatEnergy * 0.1f : 1.0f);
            float scaleX = l.scaleX * (timeSec > 2.6f ? 1.0f - beatEnergy * 0.05f : 1.0f);
            context.method_51448().scale(scaleX, scaleY);
            if (Math.abs(l.rotation) > 0.05f) {
                context.method_51448().rotate((float)Math.toRadians(l.rotation));
            }
            int alpha = (int)(l.alpha * 255.0f);
            int baseCol = l.color & 0xFFFFFF;
            int drawColor = alpha << 24 | baseCol;
            if (l.hitFlash > 0.05f) {
                int flashAlpha = (int)(l.hitFlash * 255.0f);
                drawColor = flashAlpha << 24 | 0xFFFFFF;
            } else if (timeSec > 2.6f && beatEnergy > 0.85f && l.index % 2 == 0) {
                drawColor = alpha << 24 | 0xFFFFAA;
            }
            String chStr = String.valueOf(l.ch);
            int charHalfW = tr.method_1727(chStr) / 2;
            context.method_51433(tr, chStr, -charHalfW + 1, -3, -1442840576, false);
            context.method_51433(tr, chStr, -charHalfW, -4, drawColor, false);
            context.method_51448().popMatrix();
        }
    }

    private void renderVersionBadge(class_332 context, float timeSec, long elapsed, float beatEnergy, float beatCycle) {
        if (timeSec < 3.3f) {
            return;
        }
        class_327 tr = this.field_22793;
        String verText = "2.8.1";
        int verW = tr.method_1727(verText);
        int verH = 9;

        int totalWidth = tr.method_1727(TITLE_TEXT);
        int startX = (this.field_22789 - totalWidth) / 2 + 18;
        int centerY = this.field_22790 / 2 - 14;

        float startXPos = (float)(startX + totalWidth - 8);
        float startYPos = (float)centerY;
        float targetXPos = (float)(startX + totalWidth + 4);
        float targetYPos = (float)(centerY - 7);

        float animT = class_3532.method_15363((timeSec - 3.3f) / 0.45f, 0.0f, 1.0f);
        float popEase = IntroScreen.easeOutBack(animT);

        if (!this.versionSoundPlayed) {
            this.versionSoundPlayed = true;
            this.playSound((class_6880<class_3414>)class_3417.field_14622, 1.85f, 0.85f);
            this.playSound(class_3417.field_26980, 1.4f, 0.65f);
            for (int i = 0; i < 14; ++i) {
                float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
                float spd = 1.0f + this.random.nextFloat() * 2.5f;
                int pCol = this.random.nextBoolean() ? COLOR_GOLD : (this.random.nextBoolean() ? -35038 : -1);
                this.particles.add(new IntroParticle(targetXPos + 8.0f, targetYPos + 4.0f, (float)Math.cos(angle) * spd, (float)Math.sin(angle) * spd - 0.8f, pCol, this.random.nextFloat() * 2.0f + 1.0f, 32, false));
            }
        }

        float curX = startXPos + (targetXPos - startXPos) * Math.min(1.0f, animT * 1.25f);
        float curY = startYPos + (targetYPos - startYPos) * popEase;
        float curScale = 0.25f + 0.75f * popEase;
        float curRot = (1.0f - Math.min(1.0f, animT * 1.2f)) * -22.0f;
        float curAlpha = Math.min(1.0f, animT * 2.5f);

        float beatBounce = timeSec > 3.6f ? -beatEnergy * 3.0f * (float)Math.sin((double)beatCycle * Math.PI + 0.5) : 0.0f;

        if (curAlpha > 0.05f) {
            int bgGlowAlpha = (int)(curAlpha * (0.18f + beatEnergy * 0.25f) * 120.0f);
            if (bgGlowAlpha > 2) {
                MusicHudRenderer.renderRoundedRect(context, (int)curX - 2, (int)(curY - 1 + beatBounce), verW + 4, verH + 2, 3, bgGlowAlpha << 24 | (COLOR_GOLD & 0xFFFFFF));
            }
        }

        int verBaseCol = (this.versionHitFlash > 0.05f) ? 0xFFFFFF : (COLOR_GOLD & 0xFFFFFF);
        int alphaInt = (int)(curAlpha * 255.0f);
        int verCol = alphaInt << 24 | verBaseCol;

        context.method_51448().pushMatrix();
        context.method_51448().translate(curX + (float)verW / 2.0f, curY + 4.0f + beatBounce);
        context.method_51448().scale(curScale, curScale);
        if (Math.abs(curRot) > 0.05f) {
            context.method_51448().rotate((float)Math.toRadians(curRot));
        }
        int halfW = verW / 2;
        context.method_51433(tr, verText, -halfW + 1, -3, (int)(curAlpha * 180.0f) << 24 | 0x2A1A00, false);
        context.method_51433(tr, verText, -halfW, -4, verCol, false);
        context.method_51448().popMatrix();

        this.versionHitFlash = Math.max(0.0f, this.versionHitFlash - 0.06f);
    }

    private void renderBeatEqualizer(class_332 context, int centerX, int y, float beatEnergy, long elapsed, float timeSec) {
        int alphaInt;
        if (timeSec < 2.8f) {
            return;
        }
        float alpha = Math.min(1.0f, (timeSec - 2.8f) * 1.5f);
        if (timeSec > 6.4f) {
            alpha *= Math.max(0.0f, (7.0f - timeSec) / 0.6f);
        }
        if ((alphaInt = (int)(alpha * 255.0f)) <= 2) {
            return;
        }
        int barCount = 18;
        int barW = 5;
        int barGap = 3;
        int totalW = barCount * (barW + barGap) - barGap;
        int startX = centerX - totalW / 2;
        for (int b = 0; b < barCount; ++b) {
            float wave = (float)Math.sin((double)elapsed * 0.011 + (double)b * 0.48);
            float centerBias = 1.0f - Math.abs((float)b - (float)barCount / 2.0f) / ((float)barCount / 2.0f) * 0.35f;
            float barNorm = (beatEnergy * 0.7f + (wave + 1.0f) * 0.3f) * centerBias;
            int barH = (int)(3.0f + barNorm * 18.0f);
            int rgb = b < 4 ? -49023 : (b < 12 ? -13312 : -16718337);
            int color = alphaInt << 24 | rgb;
            int bx = startX + b * (barW + barGap);
            context.method_25294(bx, y - barH, bx + barW, y, color);
            context.method_25294(bx, y - barH, bx + barW, y - barH + 1, alphaInt << 24 | 0xFFFFFF);
        }
    }

    private void renderNowPlayingPill(class_332 context, TrackState state, float beatEnergy, float timeSec) {
        int alphaInt;
        if (state == null || !state.isActive() || timeSec < 2.2f) {
            return;
        }
        float alpha = Math.min(1.0f, (timeSec - 2.2f) * 1.5f);
        if (timeSec > 6.4f) {
            alpha *= Math.max(0.0f, (7.0f - timeSec) / 0.6f);
        }
        if ((alphaInt = (int)(alpha * 255.0f)) <= 2) {
            return;
        }
        class_327 tr = this.field_22793;
        String trackTitle = state.title();
        String trackArtist = state.artist();
        String fullText = (String)(!trackArtist.isEmpty() ? trackArtist + " - " : "") + trackTitle;
        if (tr.method_1727(fullText) > 260) {
            fullText = fullText.substring(0, Math.min(fullText.length(), 36)) + "...";
        }
        String platform = state.detectPlatformName();
        String label = "\u266a " + platform + " | " + fullText;
        int textW = tr.method_1727(label);
        int pillW = textW + 28;
        int pillH = 18;
        int pillX = (this.field_22789 - pillW) / 2;
        int pillY = this.field_22790 - 46;
        int bgAlpha = (int)(alpha * 200.0f);
        int borderColor = (int)(alpha * (140.0f + beatEnergy * 115.0f)) << 24 | 0xE5FF;
        MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 9, bgAlpha << 24 | 0xA0B14);
        MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 9, borderColor, borderColor);
        int dotColor = alphaInt << 24 | (beatEnergy > 0.6f ? 65416 : 43724);
        context.method_25294(pillX + 8, pillY + 6, pillX + 13, pillY + 11, dotColor);
        int textCol = alphaInt << 24 | 0xFFFFFF;
        context.method_51433(tr, label, pillX + 18, pillY + 5, textCol, false);
    }

    private void renderTaglineAndShimmer(class_332 context, float timeSec, long elapsed) {
        class_327 tr = this.field_22793;
        if (timeSec > 3.6f) {
            float tagAlpha = Math.min(1.0f, (timeSec - 3.6f) * 1.5f);
            int alphaInt = (int)(tagAlpha * 220.0f);
            int tagColor = alphaInt << 24 | 0xCCDDEE;
            int tagX = (this.field_22789 - tr.method_1727(TAGLINE_TEXT)) / 2;
            int tagY = this.field_22790 / 2 + 18;
            context.method_51433(tr, TAGLINE_TEXT, tagX + 1, tagY + 1, -2013265920, false);
            context.method_51433(tr, TAGLINE_TEXT, tagX, tagY, tagColor, false);
        }
        if (timeSec > 4.8f && timeSec < 6.0f) {
            if (!this.soundShimmerPlayed) {
                this.soundShimmerPlayed = true;
                this.playSound(class_3417.field_26980, 1.35f, 0.85f);
                this.playSound((class_6880<class_3414>)class_3417.field_15103, 1.15f, 0.65f);
            }
            float shimmerProgress = (timeSec - 4.8f) / 1.2f;
            int totalWidth = tr.method_1727(TITLE_TEXT) + 60;
            int startX = (this.field_22789 - tr.method_1727(TITLE_TEXT)) / 2 - 10;
            float glintX = (float)startX + shimmerProgress * (float)totalWidth;
            int glintY = this.field_22790 / 2 - 12;
            for (Letter l : this.letters) {
                float letterCenter = l.targetX + (float)tr.method_1727(String.valueOf(l.ch)) / 2.0f;
                if (!(Math.abs(glintX - letterCenter) < 18.0f)) continue;
                l.hitFlash = Math.max(l.hitFlash, 0.9f);
            }
            int titleTotalW = tr.method_1727(TITLE_TEXT);
            int titleStartX = (this.field_22789 - titleTotalW) / 2 + 18;
            float verTargetX = (float)(titleStartX + titleTotalW + 4);
            if (Math.abs(glintX - (verTargetX + 10.0f)) < 18.0f) {
                this.versionHitFlash = Math.max(this.versionHitFlash, 0.9f);
            }
            context.method_25296((int)glintX - 16, glintY - 8, (int)glintX + 16, glintY + 26, 0xFFFFFF, 0x55FFFFFF);
            context.method_25294((int)glintX - 2, glintY - 10, (int)glintX + 2, glintY + 28, -285212673);
        }
    }

    private void renderIconAt(class_332 context, float x, float y, float scale, float rotDeg, float alpha) {
        int iconSize = 32;
        int alphaInt = (int)(class_3532.method_15363((float)alpha, (float)0.0f, (float)1.0f) * 255.0f);
        if (alphaInt <= 0) {
            return;
        }
        context.method_51448().pushMatrix();
        context.method_51448().translate(x + (float)iconSize / 2.0f, y + (float)iconSize / 2.0f);
        context.method_51448().scale(scale, scale);
        if (Math.abs(rotDeg) > 0.05f) {
            context.method_51448().rotate((float)Math.toRadians(rotDeg));
        }
        context.method_25291(class_10799.field_56883, ICON_TEXTURE, -iconSize / 2, -iconSize / 2, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, alphaInt << 24 | 0xFFFFFF);
        context.method_51448().popMatrix();
    }

    private void renderVignette(class_332 context) {
        int topGradient = -1157627899;
        int botGradient = -587202555;
        context.method_25296(0, 0, this.field_22789, 40, topGradient, 0);
        context.method_25296(0, this.field_22790 - 40, this.field_22789, this.field_22790, 0, botGradient);
    }

    private void renderParticles(class_332 context, float dt60) {
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            IntroParticle p = this.particles.get(i);
            p.update(dt60);
            if (p.isDead()) {
                this.particles.remove(i);
                continue;
            }
            int alphaInt = (int)(p.alpha * 255.0f);
            int col = alphaInt << 24 | p.color & 0xFFFFFF;
            if (p.symbol != null) {
                context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, col, false);
                continue;
            }
            context.method_25294((int)p.x, (int)p.y, (int)(p.x + p.size), (int)(p.y + p.size), col);
        }
    }

    private void renderShockwaves(class_332 context, float dt60) {
        for (int i = this.shockwaves.size() - 1; i >= 0; --i) {
            Shockwave s = this.shockwaves.get(i);
            s.update(dt60);
            if (s.isDead()) {
                this.shockwaves.remove(i);
                continue;
            }
            int alphaInt = (int)(s.alpha * 180.0f);
            int col = alphaInt << 24 | s.color & 0xFFFFFF;
            int r = (int)s.radius;
            int cx = (int)s.cx;
            int cy = (int)s.cy;
            context.method_25294(cx - r, cy - 1, cx + r, cy + 1, col);
            context.method_25294(cx - 1, cy - r, cx + 1, cy + r, col);
            int diag = (int)((float)r * 0.707f);
            context.method_25294(cx - diag, cy - diag, cx - diag + 2, cy - diag + 2, col);
            context.method_25294(cx + diag, cy - diag, cx + diag + 2, cy - diag + 2, col);
            context.method_25294(cx - diag, cy + diag, cx - diag + 2, cy + diag + 2, col);
            context.method_25294(cx + diag, cy + diag, cx + diag + 2, cy + diag + 2, col);
        }
    }

    public void playLetterSound(int index) {
        float pitch = 0.7f + (float)index * 0.075f;
        this.playSound((class_6880<class_3414>)class_3417.field_14622, pitch, 0.75f);
    }

    public void playSound(class_3414 sound, float pitch, float volume) {
        try {
            class_310.method_1551().method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)sound, (float)pitch, (float)volume));
        }
        catch (Exception exception) {

        }
    }

    public void playSound(class_6880<class_3414> sound, float pitch, float volume) {
        if (sound != null && sound.comp_349() != null) {
            this.playSound((class_3414)sound.comp_349(), pitch, volume);
        }
    }

    public boolean method_25404(class_11908 input) {
        if (!this.inTelegramSplash && !this.inBrowserWarning) {
            this.transitionToTelegramSplash();
        } else if (this.inTelegramSplash) {
            this.transitionToBrowserWarning();
        } else {
            this.startExit();
        }
        return true;
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        if (!this.inTelegramSplash && !this.inBrowserWarning) {
            float dist = (float)Math.hypot(click.comp_4798() - (double)(this.iconX + 16.0f), click.comp_4799() - (double)(this.iconY + 16.0f));
            if (dist <= 28.0f) {
                this.playSound(class_3417.field_26980, 1.4f, 1.0f);
                this.playSound((class_3414)class_3417.field_14793.comp_349(), 1.25f, 0.9f);
                for (int i = 0; i < 28; ++i) {
                    float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
                    float spd = 1.6f + this.random.nextFloat() * 3.5f;
                    String sym = switch (this.random.nextInt(5)) {
                        case 0 -> "\u2764";
                        case 1 -> "\u2726";
                        case 2 -> "\ud83c\udf38";
                        case 3 -> "\u2605";
                        default -> "\u266b";
                    };
                    int col = this.random.nextBoolean() ? -49023 : -10496;
                    this.particles.add(new IntroParticle(this.iconX + 16.0f, this.iconY + 16.0f, (float)Math.cos(angle) * spd, (float)Math.sin(angle) * spd - 1.2f, col, 9.0f, 60, true, sym));
                }
                return true;
            }
            if (click.comp_4799() > (double)(this.field_22790 - 70)) {
                this.runnerStartTime = (float)(System.currentTimeMillis() - this.startTimestamp) / 1000.0f;
                this.playSound(class_3417.field_14627, 1.2f, 0.8f);
                for (int i = 0; i < 16; ++i) {
                    this.particles.add(new IntroParticle((float)click.comp_4798() + (this.random.nextFloat() - 0.5f) * 16.0f, (float)click.comp_4799() + (this.random.nextFloat() - 0.5f) * 8.0f, (this.random.nextFloat() - 0.5f) * 1.5f, -0.8f - this.random.nextFloat() * 1.5f, -16718337, 2.5f, 30, false));
                }
                return true;
            }
            this.transitionToTelegramSplash();
            return true;
        }
        if (this.inTelegramSplash) {
            int mx = (int)click.comp_4798();
            int my = (int)click.comp_4799();
            int barW = Math.min(this.field_22789 - 24, 420);
            int barH = 22;
            int barX = (this.field_22789 - barW) / 2;
            int barY = this.field_22790 - barH - 4;
            if (mx >= barX && mx <= barX + barW && my >= barY && my <= barY + barH) {
                HudConfig.INSTANCE.tabBadgeEnabled = !HudConfig.INSTANCE.tabBadgeEnabled;
                HudConfig.save();
                this.playSound((class_3414)class_3417.field_15234, 1.2f, 0.8f);
                return true;
            }
            int cardW = Math.min(420, this.field_22789 - 24);
            int cardH = Math.min(235, this.field_22790 - 36);
            int cardX = (this.field_22789 - cardW) / 2;
            int baseCardY = Math.max(16, (this.field_22790 - cardH - 32) / 2 + 6);
            int btnY = baseCardY + cardH - 28;
            int btnH = 20;
            int gap = 10;
            int btnW = (cardW - 32 - gap) / 2;
            int btnTgX = cardX + 16;
            int btnNextX = btnTgX + btnW + gap;

            if (mx >= btnTgX && mx <= btnTgX + btnW && my >= btnY && my <= btnY + btnH) {
                this.playSound((class_3414)class_3417.field_15015.comp_349(), 1.4f, 0.8f);
                this.playSound(class_3417.field_26980, 1.2f, 0.9f);
                for (int i = 0; i < 30; ++i) {
                    float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
                    float spd = 1.5f + this.random.nextFloat() * 4.0f;
                    this.particles.add(new IntroParticle(mx, my, (float)Math.cos(angle) * spd, (float)Math.sin(angle) * spd - 1.2f, 0xFF24A1DE, 2.5f, 40, false));
                }
                openUrl("https://t.me/TG_Nia_Teppelin0");
                return true;
            }
            this.playSound((class_3414)class_3417.field_15015.comp_349(), 1.2f, 0.7f);
            this.transitionToBrowserWarning();
            return true;
        }
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int barW = Math.min(this.field_22789 - 24, 420);
        int barH = 22;
        int barX = (this.field_22789 - barW) / 2;
        int barY = this.field_22790 - barH - 4;
        if (mx >= barX && mx <= barX + barW && my >= barY && my <= barY + barH) {
            HudConfig.INSTANCE.tabBadgeEnabled = !HudConfig.INSTANCE.tabBadgeEnabled;
            HudConfig.save();
            this.playSound((class_3414)class_3417.field_15234, 1.2f, 0.8f);
            return true;
        }
        int cardW = Math.min(420, this.field_22789 - 24);
        int cardH = Math.min(230, this.field_22790 - 36);
        int cardX = (this.field_22789 - cardW) / 2;
        int baseCardY = Math.max(16, (this.field_22790 - cardH - 32) / 2 + 6);
        int btnW = 210;
        int btnH = 20;
        int btnX = cardX + (cardW - btnW) / 2;
        int btnY = baseCardY + cardH - 26;
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            this.playSound((class_3414)class_3417.field_15015.comp_349(), 1.2f, 0.7f);
            this.startExit();
            return true;
        }
        this.startExit();
        return true;
    }

    private void renderTabBadgeBottomBar(class_332 context, int mouseX, int mouseY, float alpha) {
        int alphaInt = (int)(alpha * 255.0f);
        if (alphaInt <= 2) {
            return;
        }
        class_327 tr = this.field_22793;
        int barW = Math.min(this.field_22789 - 24, 420);
        int barH = 22;
        int barX = (this.field_22789 - barW) / 2;
        int barY = this.field_22790 - barH - 4;

        int bgCol = (int)(alpha * 245.0f) << 24 | 0x0B0F19;
        int borderCol = (int)(alpha * 170.0f) << 24 | 0x00F5D4;
        MusicHudRenderer.renderRoundedRect(context, barX, barY, barW, barH, 6, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, barX, barY, barW, barH, 6, borderCol, borderCol);

        context.method_51433(tr, "§b👥 Отображение мода у других в Tab:", barX + 10, barY + 7, alphaInt << 24 | 0xFFFFFF, false);

        int toggleW = 84;
        int toggleH = 16;
        int toggleX = barX + barW - toggleW - 4;
        int toggleY = barY + 3;
        boolean hoverToggle = mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH;

        boolean enabled = HudConfig.INSTANCE.tabBadgeEnabled;
        int tBg = enabled
            ? (hoverToggle ? ((int)(alpha * 255.0f) << 24 | 0x1E5C3B) : ((int)(alpha * 200.0f) << 24 | 0x144029))
            : (hoverToggle ? ((int)(alpha * 255.0f) << 24 | 0x5C1E1E) : ((int)(alpha * 200.0f) << 24 | 0x401414));
        int tBorder = enabled ? ((int)(alpha * 255.0f) << 24 | 0x00FF88) : ((int)(alpha * 255.0f) << 24 | 0xFF4444);

        MusicHudRenderer.renderRoundedRect(context, toggleX, toggleY, toggleW, toggleH, 4, tBg);
        MusicHudRenderer.renderRoundedBorder(context, toggleX, toggleY, toggleW, toggleH, 4, tBorder, tBorder);

        String tText = enabled ? "§a✔ ВКЛ" : "§c✕ ВЫКЛ";
        int ttw = tr.method_1727(tText);
        context.method_51433(tr, tText, toggleX + (toggleW - ttw) / 2, toggleY + 4, alphaInt << 24 | 0xFFFFFF, true);
    }

    public void method_25419() {
        hasShownIntro = true;
        if (this.field_22787 != null) {
            if (HudConfig.INSTANCE.promptYandexMusicOnStartup) {
                YandexMusicSettingsScreen.hasPromptedStartup = true;
                this.field_22787.method_1507((class_437)new YandexMusicSettingsScreen(this.parent));
            } else if (!HudConfig.INSTANCE.initialSetupCompleted) {
                this.field_22787.method_1507((class_437)new InitialSetupScreen(this.parent));
            } else {
                this.field_22787.method_1507((class_437)new KeybindSetupScreen(this.parent));
            }
        }
    }

    public boolean method_25421() {
        return true;
    }

    public boolean method_25422() {
        return true;
    }

    private static float easeOutCubic(float t) {
        float f = t - 1.0f;
        return f * f * f + 1.0f;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float)Math.pow((double)t - 1.0, 3.0) + c1 * (float)Math.pow((double)t - 1.0, 2.0);
    }

    private static class Letter {
        final char ch;
        final float targetX;
        final float targetY;
        final int color;
        final int index;
        float offsetY = 0.0f;
        float velocityY = 0.0f;
        float rotation = 0.0f;
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float hitFlash = 0.0f;
        float alpha = 0.95f;
        long impulseScheduledAt = -1L;
        boolean impulseApplied = false;

        Letter(char ch, float targetX, float targetY, int color, int index) {
            this.ch = ch;
            this.targetX = targetX;
            this.targetY = targetY;
            this.color = color;
            this.index = index;
        }

        void scheduleImpulse(long delayMs) {
            this.impulseScheduledAt = System.currentTimeMillis() + delayMs;
            this.impulseApplied = false;
        }

        void update(long elapsed, IntroScreen screen) {
            long now = System.currentTimeMillis();
            if (this.impulseScheduledAt > 0L && now >= this.impulseScheduledAt && !this.impulseApplied) {
                this.impulseApplied = true;
                this.velocityY = -42.0f;
                this.rotation = (this.index % 2 == 0 ? -1.0f : 1.0f) * (8.0f + (float)(this.index % 3) * 3.5f);
                this.scaleY = 1.45f;
                this.scaleX = 0.78f;
                this.hitFlash = 1.0f;
                screen.playLetterSound(this.index);
            }
            float k = 0.28f;
            float damping = 0.78f;
            float forceY = -k * this.offsetY;
            this.velocityY += forceY;
            this.velocityY *= damping;
            this.offsetY += this.velocityY;
            this.rotation *= 0.86f;
            this.scaleX += (1.0f - this.scaleX) * 0.22f;
            this.scaleY += (1.0f - this.scaleY) * 0.22f;
            this.hitFlash = Math.max(0.0f, this.hitFlash - 0.06f);
        }
    }

    private static class IntroParticle {
        float x;
        float y;
        float vx;
        float vy;
        final int color;
        final float size;
        final int maxLife;
        final boolean isAmbient;
        final String symbol;
        int age = 0;
        float alpha = 1.0f;

        IntroParticle(float x, float y, float vx, float vy, int color, float size, int maxLife, boolean isAmbient, String symbol) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.maxLife = maxLife;
            this.isAmbient = isAmbient;
            this.symbol = symbol;
        }

        IntroParticle(float x, float y, float vx, float vy, int color, float size, int maxLife, boolean isAmbient) {
            this(x, y, vx, vy, color, size, maxLife, isAmbient, null);
        }

        void update(float dt60) {
            if (this.symbol != null) {
                this.x += (float)Math.sin((double)this.age * 0.12 + (double)this.y * 0.05) * 0.45f * dt60;
                this.y += this.vy * dt60;
            } else {
                this.x += this.vx * dt60;
                this.y += this.vy * dt60;
                if (!this.isAmbient) {
                    this.vy += 0.22f * dt60;
                    this.vx *= (float)Math.pow(0.94, dt60);
                }
            }
            this.age = (int)((float)this.age + dt60);
            this.alpha = 1.0f - (float)this.age / (float)this.maxLife;
        }

        void update() {
            this.update(1.0f);
        }

        boolean isDead() {
            return this.age >= this.maxLife;
        }
    }

    private static class Shockwave {
        final float cx;
        final float cy;
        final float maxRadius;
        final int color;
        float radius = 0.0f;
        float alpha = 1.0f;

        Shockwave(float cx, float cy, float maxRadius, int color) {
            this.cx = cx;
            this.cy = cy;
            this.maxRadius = maxRadius;
            this.color = color;
        }

        void update(float dt60) {
            this.radius += 9.5f * dt60;
            this.alpha = Math.max(0.0f, 1.0f - this.radius / this.maxRadius);
        }

        void update() {
            this.update(1.0f);
        }

        boolean isDead() {
            return this.alpha <= 0.0f || this.radius >= this.maxRadius;
        }
    }

    private static class TrailPoint {
        final float x;
        final float y;
        final float scale;
        final float rot;
        float alpha;

        TrailPoint(float x, float y, float scale, float rot, float alpha) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rot = rot;
            this.alpha = alpha;
        }
    }
}

