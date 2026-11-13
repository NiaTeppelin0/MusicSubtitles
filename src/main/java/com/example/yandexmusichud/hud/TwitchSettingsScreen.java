package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.ModernButtonWidget;
import com.example.yandexmusichud.hud.ModernSliderWidget;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.hud.TwitchLogoMergeAnimation;
import com.example.yandexmusichud.twitch.TwitchAlertBanner;
import com.example.yandexmusichud.twitch.TwitchAlertEvent;
import com.example.yandexmusichud.twitch.TwitchAlertQueue;
import com.example.yandexmusichud.twitch.TwitchManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.class_10799;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

public class TwitchSettingsScreen extends class_437 {
    private static final class_2960 TWITCH_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/twitch_icon.png");
    private final class_437 parent;
    private final TwitchLogoMergeAnimation introAnim;
    private class_342 channelField;
    private String feedbackMessage = "";
    private long feedbackTime = 0L;
    private int feedbackColor = 0x00E5FF;

    private final List<AmbientParticle> bgParticles = new ArrayList<>();
    private final Random random = new Random();
    private boolean particlesInit = false;
    private long lastLiveCheckTime = 0L;

    private static class JsabRunner {
        float x;
        float yOffset;
        float jumpVelocity;
        final int type;
        final int color;

        JsabRunner(int type, int color, float initX) {
            this.type = type;
            this.color = color;
            this.x = initX;
        }
    }

    private final JsabRunner[] jsabSquad = new JsabRunner[]{
        new JsabRunner(0, 0xFFFFEE00, 24.0f),
        new JsabRunner(1, 0xFF00E5FF, 40.0f),
        new JsabRunner(2, 0xFF00FF88, 56.0f),
        new JsabRunner(3, 0xFFFF7700, 72.0f)
    };
    private float jsabSquadX = 24.0f;
    private boolean jsabMovingRight = true;
    private long jsabLastTime = 0L;
    private long jsabNextDash = 0L;
    private long jsabDashEnd = 0L;

    private static class AmbientParticle {
        float x, y;
        float vx, vy;
        float baseSpeed;
        float wavePhase;
        int color;
        String symbol;
        float alpha;

        AmbientParticle(float x, float y, float vy, int color, String symbol) {
            this.x = x;
            this.y = y;
            this.vy = vy;
            this.baseSpeed = vy;
            this.color = color;
            this.symbol = symbol;
            this.wavePhase = (float)(Math.random() * Math.PI * 2.0);
            this.alpha = 0.3f + (float)Math.random() * 0.55f;
        }

        void update(int screenW, int screenH, float dt60) {
            this.wavePhase += 0.025f * dt60;
            this.x += (float)Math.sin(this.wavePhase) * 0.6f * dt60;
            this.y += this.vy * dt60;
            if (this.y < -15.0f) {
                this.y = screenH + 10.0f;
                this.x = (float)Math.random() * (float)screenW;
            }
        }

        void update(int screenW, int screenH) {
            this.update(screenW, screenH, 1.0f);
        }
    }

    public TwitchSettingsScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"Twitch Live"));
        this.parent = parent;
        this.introAnim = new TwitchLogoMergeAnimation();
    }

    private void initParticles(int screenW, int screenH) {
        if (this.particlesInit) return;
        this.particlesInit = true;
        String[] symbols = new String[]{"♪", "♫", "★", "✦", "💜", "◆", "•"};
        int[] colors = new int[]{0x9146FF, 0xB57BFF, 0x00E5FF, 0xFFCC00, 0xFF66CC, 0x77DDFF};
        for (int i = 0; i < 35; ++i) {
            float px = this.random.nextFloat() * (float)screenW;
            float py = this.random.nextFloat() * (float)screenH;
            float vy = -0.3f - this.random.nextFloat() * 0.7f;
            int col = colors[this.random.nextInt(colors.length)];
            String sym = symbols[this.random.nextInt(symbols.length)];
            this.bgParticles.add(new AmbientParticle(px, py, vy, col, sym));
        }
    }

    protected void method_25426() {
        this.method_37068();
        int screenW = this.field_22789;
        int screenH = this.field_22790;
        this.initParticles(screenW, screenH);
        TwitchManager.getInstance().checkLiveStatusAsync();

        int panelW = Math.min(520, screenW - 24);
        int panelH = Math.min(265, screenH - 56);
        int panelX = (screenW - panelW) / 2;
        int panelY = 46 + Math.max(0, (screenH - 56 - panelH) / 2);

        int colGap = 10;
        int colW = (panelW - colGap) / 2;
        int col1X = panelX;
        int col2X = panelX + colW + colGap;

        int card1Top = panelY;
        int cardH = panelH - 52;

        int fieldY = card1Top + 24;
        int fieldH = 18;
        this.channelField = new class_342(this.field_22793, col1X + 12, fieldY, colW - 24, fieldH,
                (class_2561)class_2561.method_43470((String)"Twitch Channel"));
        this.channelField.method_1852(HudConfig.INSTANCE.twitchChannel != null ? HudConfig.INSTANCE.twitchChannel : "");
        this.channelField.method_1863(text -> {
            HudConfig.INSTANCE.twitchChannel = text.trim();
            HudConfig.save();
            TwitchManager.getInstance().checkLiveStatusAsync();
        });
        this.method_37063(this.channelField);

        int btnRow1Y = fieldY + fieldH + 16;
        int col1BtnW = (colW - 28) / 2;
        int btnH = 20;

        ModernButtonWidget enableBtn = new ModernButtonWidget(col1X + 12, btnRow1Y, col1BtnW, btnH,
                (class_2561)class_2561.method_43470(this.getEnableText()), b -> {
            HudConfig.INSTANCE.twitchEnabled = !HudConfig.INSTANCE.twitchEnabled;
            b.method_25355((class_2561)class_2561.method_43470(this.getEnableText()));
            b.setCustomBgColor(HudConfig.INSTANCE.twitchEnabled ? 0x4400FF88 : 0x3322222E);
            HudConfig.save();
            if (HudConfig.INSTANCE.twitchEnabled) {
                TwitchManager.getInstance().start();
                this.triggerFeedback("\u00a7a✔ Twitch интеграция включена!", 0x00FF88);
            } else {
                TwitchManager.getInstance().disconnect();
                this.triggerFeedback("\u00a77Twitch отключен", 0xAAAAAA);
            }
        });
        enableBtn.setCustomBgColor(HudConfig.INSTANCE.twitchEnabled ? 0x4400FF88 : 0x3322222E);
        this.method_37063(enableBtn);

        ModernButtonWidget reconnectBtn = new ModernButtonWidget(col1X + 16 + col1BtnW, btnRow1Y, col1BtnW, btnH,
                (class_2561)class_2561.method_43470("\u21bb \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c"), b -> {
            TwitchManager.getInstance().disconnect();
            TwitchManager.getInstance().start();
            this.triggerFeedback("\u00a7b⟳ Переподключение к IRC Twitch...", 0x00E5FF);
        });
        this.method_37063(reconnectBtn);

        int autoCmdBtnY = card1Top + cardH - 26;
        ModernButtonWidget autoCmdBtn = new ModernButtonWidget(col1X + 10, autoCmdBtnY, colW - 20, btnH,
                (class_2561)class_2561.method_43470(this.getAutoCmdButtonText()), b -> {
            class_310.method_1551().method_1507(new TwitchStreamCommandScreen(this));
        });
        autoCmdBtn.setCustomBgColor(HudConfig.INSTANCE.twitchAutoCommandEnabled ? 0x449146FF : 0x3322222E);
        this.method_37063(autoCmdBtn);

        int card2Top = panelY;
        int toggleW = (colW - 28) / 2;
        int tRow1Y = card2Top + 24;

        ModernButtonWidget subsBtn = new ModernButtonWidget(col2X + 12, tRow1Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getSubsText()), b -> {
            HudConfig.INSTANCE.twitchShowSubs = !HudConfig.INSTANCE.twitchShowSubs;
            b.method_25355((class_2561)class_2561.method_43470(this.getSubsText()));
            b.setCustomBgColor(HudConfig.INSTANCE.twitchShowSubs ? 0x449146FF : 0x3322222E);
            HudConfig.save();
        });
        subsBtn.setCustomBgColor(HudConfig.INSTANCE.twitchShowSubs ? 0x449146FF : 0x3322222E);
        this.method_37063(subsBtn);

        ModernButtonWidget rewardsBtn = new ModernButtonWidget(col2X + 16 + toggleW, tRow1Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getRewardsText()), b -> {
            HudConfig.INSTANCE.twitchShowRewards = !HudConfig.INSTANCE.twitchShowRewards;
            b.method_25355((class_2561)class_2561.method_43470(this.getRewardsText()));
            b.setCustomBgColor(HudConfig.INSTANCE.twitchShowRewards ? 0x4400E5FF : 0x3322222E);
            HudConfig.save();
        });
        rewardsBtn.setCustomBgColor(HudConfig.INSTANCE.twitchShowRewards ? 0x4400E5FF : 0x3322222E);
        this.method_37063(rewardsBtn);

        int tRow2Y = tRow1Y + 23;
        ModernButtonWidget bitsBtn = new ModernButtonWidget(col2X + 12, tRow2Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getBitsText()), b -> {
            HudConfig.INSTANCE.twitchShowBits = !HudConfig.INSTANCE.twitchShowBits;
            b.method_25355((class_2561)class_2561.method_43470(this.getBitsText()));
            b.setCustomBgColor(HudConfig.INSTANCE.twitchShowBits ? 0x44FFAA00 : 0x3322222E);
            HudConfig.save();
        });
        bitsBtn.setCustomBgColor(HudConfig.INSTANCE.twitchShowBits ? 0x44FFAA00 : 0x3322222E);
        this.method_37063(bitsBtn);

        ModernButtonWidget soundBtn = new ModernButtonWidget(col2X + 16 + toggleW, tRow2Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getSoundText()), b -> {
            HudConfig.INSTANCE.twitchPlaySound = !HudConfig.INSTANCE.twitchPlaySound;
            b.method_25355((class_2561)class_2561.method_43470(this.getSoundText()));
            b.setCustomBgColor(HudConfig.INSTANCE.twitchPlaySound ? 0x4400FF88 : 0x3322222E);
            HudConfig.save();
        });
        soundBtn.setCustomBgColor(HudConfig.INSTANCE.twitchPlaySound ? 0x4400FF88 : 0x3322222E);
        this.method_37063(soundBtn);

        int tRow3Y = tRow2Y + 23;
        ModernButtonWidget posBtn = new ModernButtonWidget(col2X + 12, tRow3Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getPositionText()), b -> {
            boolean isBottom = "BOTTOM".equalsIgnoreCase(HudConfig.INSTANCE.twitchToastPosition);
            HudConfig.INSTANCE.twitchToastPosition = isBottom ? "TOP" : "BOTTOM";
            b.method_25355((class_2561)class_2561.method_43470(this.getPositionText()));
            HudConfig.save();
        });
        this.method_37063(posBtn);

        double initDuration = class_3532.method_15350((double)(HudConfig.INSTANCE.twitchToastDurationSeconds - 2) / 13.0, 0.0, 1.0);
        ModernSliderWidget durationSlider = new ModernSliderWidget(col2X + 16 + toggleW, tRow3Y, toggleW, btnH,
                "\u041f\u043e\u043a\u0430\u0437: ", initDuration, val -> String.format("%d \u0441\u0435\u043a", 2 + (int)(val * 13.0)), val -> {
            HudConfig.INSTANCE.twitchToastDurationSeconds = 2 + (int)(val * 13.0);
            HudConfig.save();
        });
        this.method_37063(durationSlider);

        int testCardY = card1Top + cardH + 6;
        int testBtnCount = 4;
        int testGap = 6;
        int testBtnW = (panelW - 24 - testGap * (testBtnCount - 1)) / testBtnCount;
        int testBtnY = testCardY + 14;

        ModernButtonWidget testSubBtn = new ModernButtonWidget(panelX + 12, testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("\u2728 \u0422\u0435\u0441\u0442 Sub"), b -> {
            TwitchManager.getInstance().triggerTestAlert(TwitchAlertEvent.Type.SUB);
            this.triggerFeedback("\u00a7d\u2728 \u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439 \u0441\u0430\u0431 T3 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d \u0432 HUD!", 0xB57BFF);
        });
        this.method_37063(testSubBtn);

        ModernButtonWidget testGiftBtn = new ModernButtonWidget(panelX + 12 + (testBtnW + testGap), testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("\ud83c\udf81 \u0422\u0435\u0441\u0442 \u0413\u0438\u0444\u0442"), b -> {
            TwitchManager.getInstance().triggerTestAlert(TwitchAlertEvent.Type.SUB_GIFT);
            this.triggerFeedback("\u00a7d\ud83c\udf81 \u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439 \u0433\u0438\u0444\u0442 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d \u0432 HUD!", 0xFF66CC);
        });
        this.method_37063(testGiftBtn);

        ModernButtonWidget testRewardBtn = new ModernButtonWidget(panelX + 12 + (testBtnW + testGap) * 2, testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("\ud83d\udd2e \u0422\u0435\u0441\u0442 \u0411\u0430\u043b\u043b\u044b"), b -> {
            TwitchManager.getInstance().triggerTestAlert(TwitchAlertEvent.Type.REWARD);
            this.triggerFeedback("\u00a7b\ud83d\udd2e \u0422\u0435\u0441\u0442\u043e\u0432\u0430\u044f \u043d\u0430\u0433\u0440\u0430\u0434\u0430 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430 \u0432 HUD!", 0x00E5FF);
        });
        this.method_37063(testRewardBtn);

        ModernButtonWidget testBitsBtn = new ModernButtonWidget(panelX + 12 + (testBtnW + testGap) * 3, testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("\ud83d\udc8e \u0422\u0435\u0441\u0442 Bits"), b -> {
            TwitchManager.getInstance().triggerTestAlert(TwitchAlertEvent.Type.BITS);
            this.triggerFeedback("\u00a7e\ud83d\udc8e \u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0435 500 Bits \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u044b \u0432 HUD!", 0xFFAA00);
        });
        this.method_37063(testBitsBtn);

        int closeBtnW = 20;
        int closeBtnH = 20;
        int closeBtnX = screenW - 28;
        int closeBtnY = 16;
        ModernButtonWidget closeBtn = new ModernButtonWidget(closeBtnX, closeBtnY, closeBtnW, closeBtnH,
                (class_2561)class_2561.method_43470("\u00d7"), b -> this.method_25419());
        closeBtn.setCustomBgColor(0x3322222E);
        this.method_37063(closeBtn);

        int daBtnW = 106;
        int daBtnH = 20;
        int daBtnX = closeBtnX - daBtnW - 8;
        int daBtnY = 16;
        ModernButtonWidget daNavBtn = new ModernButtonWidget(daBtnX, daBtnY, daBtnW, daBtnH,
                (class_2561)class_2561.method_43470("§6✦ §fDonationAlerts"), b -> {
            class_310.method_1551().method_1507(new DonationAlertsSettingsScreen(this.parent));
        });
        daNavBtn.setCustomBgColor(0x333A1F0D);
        this.method_37063(daNavBtn);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        int screenW = this.field_22789;
        int screenH = this.field_22790;

        if (now - this.lastLiveCheckTime > 20000L) {
            this.lastLiveCheckTime = now;
            TwitchManager.getInstance().checkLiveStatusAsync();
        }

        context.method_25294(0, 0, screenW, screenH, 0xFF0A0A12);

        int topGradient = -1157627899;
        int botGradient = -587202555;
        context.method_25296(0, 0, screenW, 55, topGradient, 0);
        context.method_25296(0, screenH - 55, screenW, screenH, 0, botGradient);

        float dt60 = Math.min(3.0f, delta);
        for (AmbientParticle p : this.bgParticles) {
            p.update(screenW, screenH, dt60);
            int a = (int)(p.alpha * 255.0f);
            int col = a << 24 | (p.color & 0xFFFFFF);
            context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, col, false);
        }

        int targetCornerX = 24;
        int targetCornerY = 16;
        this.introAnim.render(context, screenW, screenH, targetCornerX, targetCornerY);

        float panelAlpha = this.introAnim.getSettingsPanelAlpha();
        float slideY = this.introAnim.getSettingsPanelSlideY();

        if (panelAlpha > 0.02f) {
            int alphaInt = (int)(panelAlpha * 255.0f);

            int titleX = targetCornerX + 32;
            int titleY = targetCornerY + 2 - (int)(slideY * 0.5f);

            TwitchManager tm = TwitchManager.getInstance();
            TwitchManager.Status status = tm.getStatus();
            boolean isLive = tm.isLive();
            String curChan = HudConfig.INSTANCE.twitchChannel;

            String statusBadge;
            int badgeBorder;
            int badgeBg;

            if (!HudConfig.INSTANCE.twitchEnabled || curChan == null || curChan.trim().isEmpty()) {
                statusBadge = "\u00a77\u25cb \u041e\u0422\u041a\u041b\u042e\u0427\u0415\u041d\u041e";
                badgeBorder = 0x555566;
                badgeBg = 0x14141E;
            } else if (status == TwitchManager.Status.CONNECTING) {
                statusBadge = "\u00a7e\u21bb \u041f\u041e\u0414\u041a\u041b\u042e\u0427\u0415\u041d\u0418\u0415...";
                badgeBorder = 0xFFCC00;
                badgeBg = 0x241F0A;
            } else if (status == TwitchManager.Status.ERROR) {
                statusBadge = "\u00a7c\u25cf \u041e\u0428\u0418\u0411\u041a\u0410";
                badgeBorder = 0xFF4455;
                badgeBg = 0x240A10;
            } else if (isLive) {
                statusBadge = "\u00a7a\u25cf \u0412 \u0421\u0415\u0422\u0418";
                badgeBorder = 0x00FF88;
                badgeBg = 0x0A2418;
            } else {
                statusBadge = "\u00a77\u25cb \u041e\u0424\u0424\u041b\u0410\u0419\u041d";
                badgeBorder = 0x555566;
                badgeBg = 0x14141E;
            }

            int titleW = this.field_22793.method_1727("\u00a7d\u00a7lTWITCH \u00a7f\u00a7lLIVE");
            int badgeW = this.field_22793.method_1727(statusBadge) + 12;
            int badgeH = 14;
            int badgeX = titleX + titleW + 8;
            int badgeY = titleY + 1;

            context.method_51433(this.field_22793, "\u00a7d\u00a7lTWITCH \u00a7f\u00a7lLIVE", titleX, titleY, alphaInt << 24 | 0xFFFFFF, true);

            MusicHudRenderer.renderRoundedRect(context, badgeX, badgeY, badgeW, badgeH, 4, (int)(alphaInt * 0.7f) << 24 | badgeBg);
            MusicHudRenderer.renderRoundedBorder(context, badgeX, badgeY, badgeW, badgeH, 4, (int)(alphaInt * 0.8f) << 24 | badgeBorder, 0x44000000);
            context.method_51433(this.field_22793, statusBadge, badgeX + 6, badgeY + 3, alphaInt << 24 | 0xFFFFFF, false);

            String subtitle = "\u0418\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u044f \u0441 \u0442\u0432\u0438\u0447\u0451\u043c! \u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f \u043e \u043f\u0440\u043e\u0444\u0438\u043b\u0435, \u043d\u0430\u0433\u0440\u0430\u0434\u0430\u0445, \u0431\u0430\u043b\u043b\u0430\u0445 \u0438 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0430\u0445!";
            int maxSubW = screenW - 150 - titleX;
            if (maxSubW > 50) {
                subtitle = trimText(this.field_22793, subtitle, maxSubW);
            }
            context.method_51433(this.field_22793, "\u00a77" + subtitle, titleX, titleY + 11, alphaInt << 24 | 0xCCDDEE, false);
        }

        if (panelAlpha > 0.05f) {
            context.method_51448().pushMatrix();
            context.method_51448().translate(0.0f, slideY);

            int panelW = Math.min(520, screenW - 24);
            int panelH = Math.min(265, screenH - 56);
            int panelX = (screenW - panelW) / 2;
            int panelY = 46 + Math.max(0, (screenH - 56 - panelH) / 2);

            int colGap = 10;
            int colW = (panelW - colGap) / 2;
            int col1X = panelX;
            int col2X = panelX + colW + colGap;
            int cardH = panelH - 52;

            int alphaInt = (int)(panelAlpha * 255.0f);
            int cardBg = (int)(panelAlpha * 230.0f) << 24 | 0x120E22;
            int cardBorder1 = (int)(panelAlpha * 140.0f) << 24 | 0x9146FF;
            int cardBorder2 = (int)(panelAlpha * 140.0f) << 24 | 0x00E5FF;

            MusicHudRenderer.renderRoundedRect(context, col1X, panelY, colW, cardH, 8, cardBg);
            MusicHudRenderer.renderRoundedBorder(context, col1X, panelY, colW, cardH, 8, cardBorder1, 0x33000000);
            context.method_51433(this.field_22793, "\u00a7d\u25C8 \u041f\u0440\u043e\u0444\u0438\u043b\u044c \u0438 \u041a\u0430\u043d\u0430\u043b", col1X + 12, panelY + 9, alphaInt << 24 | 0xFFFFFF, true);

            String curChan = HudConfig.INSTANCE.twitchChannel;
            TwitchManager tm = TwitchManager.getInstance();
            String chanHint;
            if (curChan == null || curChan.trim().isEmpty()) {
                chanHint = "\u00a7e\u26a0 \u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0438\u043a \u0434\u043b\u044f \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f";
            } else if (tm.isLive()) {
                chanHint = "\u00a7a\u25cf \u0421\u0442\u0440\u0438\u043c \u0438\u0434\u0451\u0442! \u00a77(#" + curChan + (tm.getViewersCount() > 0 ? " \u2022 \u00a7f" + tm.getViewersCount() + " \u0437\u0440\u0438\u0442." : "") + ")";
            } else {
                chanHint = "\u00a77\u25cb \u041e\u0444\u0444\u043b\u0430\u0439\u043d \u00a78(#" + curChan + ")";
            }
            context.method_51433(this.field_22793, chanHint, col1X + 12, panelY + 45, alphaInt << 24 | 0xFFFFFF, false);

            int statBoxY = panelY + 86;
            int statBoxH = cardH - 120;
            MusicHudRenderer.renderRoundedRect(context, col1X + 10, statBoxY, colW - 20, statBoxH, 6, (int)(panelAlpha * 120.0f) << 24 | 0x080612);
            MusicHudRenderer.renderRoundedBorder(context, col1X + 10, statBoxY, colW - 20, statBoxH, 6, (int)(panelAlpha * 80.0f) << 24 | 0x553388, 0x22000000);

            int avSize = 30;
            int avX = col1X + 16;
            int avY = statBoxY + 8;
            class_2960 avTex = tm.hasCustomAvatar() ? tm.getAvatarTextureId() : TWITCH_ICON;
            int ringColor = tm.isLive() ? 0xFF00FF88 : 0xFF555566;

            MusicHudRenderer.renderRoundedRect(context, avX - 2, avY - 2, avSize + 4, avSize + 4, 6, (int)(panelAlpha * 180.0f) << 24 | 0x1A0E2E);
            MusicHudRenderer.renderRoundedBorder(context, avX - 2, avY - 2, avSize + 4, avSize + 4, 6, (int)(panelAlpha * 255.0f) << 24 | ringColor, 0x44000000);

            try {
                context.method_25291(class_10799.field_56883, avTex, avX, avY, 0.0f, 0.0f, avSize, avSize, avSize, avSize, alphaInt << 24 | 0xFFFFFF);
            } catch (Throwable ignored) {}

            int infoX = avX + avSize + 8;
            int maxNameW = colW - 20 - (infoX - (col1X + 10)) - 6;
            String dispName = tm.getChannelDisplayName();
            if (dispName == null || dispName.isEmpty()) dispName = (curChan != null && !curChan.isEmpty()) ? curChan : "Twitch";
            dispName = trimText(this.field_22793, dispName, maxNameW);
            context.method_51433(this.field_22793, "\u00a7f\u00a7l" + dispName, infoX, avY + 1, alphaInt << 24 | 0xFFFFFF, false);

            if (tm.isLive()) {
                String liveLabel = tm.getViewersCount() > 0
                        ? "\u00a7a\u25cf \u0412 \u044d\u0444\u0438\u0440\u0435 \u00a77(\u00a7f" + tm.getViewersCount() + "\u00a77)"
                        : "\u00a7a\u25cf \u0412 \u044d\u0444\u0438\u0440\u0435";
                context.method_51433(this.field_22793, liveLabel, infoX, avY + 11, alphaInt << 24 | 0x00FF88, false);
            } else {
                context.method_51433(this.field_22793, "\u00a77\u25cb \u041e\u0444\u0444\u043b\u0430\u0439\u043d", infoX, avY + 11, alphaInt << 24 | 0x888899, false);
            }

            String descLine = tm.isLive() && !tm.getStreamTitle().isEmpty()
                    ? "\u00a7e" + tm.getStreamTitle()
                    : "\u00a78#" + (curChan != null ? curChan.toLowerCase(java.util.Locale.ROOT) : "");
            descLine = trimText(this.field_22793, descLine, maxNameW);
            context.method_51433(this.field_22793, descLine, infoX, avY + 21, alphaInt << 24 | 0xCCDDEE, false);

            int divY = avY + avSize + 6;
            context.method_25294(col1X + 16, divY, col1X + colW - 16, divY + 1, (int)(panelAlpha * 50.0f) << 24 | 0x443366);

            int statsY = divY + 5;
            context.method_51433(this.field_22793, "\u00a77\u2022 \u041e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u0439 \u0432 HUD: \u00a7f" + TwitchAlertQueue.INSTANCE.getTotalHandledCount(), col1X + 16, statsY, alphaInt << 24 | 0xDDDDDD, false);
            context.method_51433(this.field_22793, "\u00a77\u2022 \u0412 \u043e\u0447\u0435\u0440\u0435\u0434\u0438: \u00a7f" + TwitchAlertQueue.INSTANCE.getQueuedCount(), col1X + 16, statsY + 11, alphaInt << 24 | 0xDDDDDD, false);

            MusicHudRenderer.renderRoundedRect(context, col2X, panelY, colW, cardH, 8, cardBg);
            MusicHudRenderer.renderRoundedBorder(context, col2X, panelY, colW, cardH, 8, cardBorder2, 0x33000000);
            context.method_51433(this.field_22793, "\u00a7b\u2605 \u0421\u043e\u0431\u044b\u0442\u0438\u044f \u0438 \u041f\u043b\u0430\u0448\u043a\u0430 HUD", col2X + 12, panelY + 9, alphaInt << 24 | 0xFFFFFF, true);

            int prevY = panelY + 86;
            int prevH = cardH - 94;
            MusicHudRenderer.renderRoundedRect(context, col2X + 10, prevY, colW - 20, prevH, 6, (int)(panelAlpha * 120.0f) << 24 | 0x080612);
            MusicHudRenderer.renderRoundedBorder(context, col2X + 10, prevY, colW - 20, prevH, 6, (int)(panelAlpha * 80.0f) << 24 | 0x226688, 0x22000000);

            context.method_51433(this.field_22793, "\u00a7e\u2726 \u041c\u0430\u043a\u0435\u0442 \u043f\u043b\u0430\u0448\u043a\u0438 \u0432 HUD:", col2X + 16, prevY + 8, alphaInt << 24 | 0xFFFFFF, false);

            TwitchAlertEvent liveAlert = TwitchAlertQueue.INSTANCE.getActiveAlert();
            int ribX = col2X + 16;
            int ribY = prevY + 22;
            int ribW = colW - 32;
            int ribH = 22;

            class_2960 ribbonIcon = tm.hasCustomAvatar() ? tm.getAvatarTextureId() : TWITCH_ICON;

            if (liveAlert != null) {
                int accentCol = liveAlert.getAccentColor();
                MusicHudRenderer.renderRoundedRect(context, ribX, ribY, ribW, ribH, 5, 0xFF1A0E2E);
                MusicHudRenderer.renderRoundedBorder(context, ribX, ribY, ribW, ribH, 5, 0xFF000000 | accentCol, 0x44000000);
                try {
                    context.method_25291(class_10799.field_56883, ribbonIcon, ribX + 5, ribY + 4, 0.0f, 0.0f, 13, 13, 13, 13, alphaInt << 24 | 0xFFFFFF);
                } catch (Throwable ignored) {}
                String pTitle = trimText(this.field_22793, liveAlert.getTitle(), ribW - 28);
                context.method_51433(this.field_22793, pTitle, ribX + 22, ribY + 3, -1, false);
                String pSub = liveAlert.getSubtitle();
                if (!pSub.isEmpty()) {
                    pSub = trimText(this.field_22793, pSub, ribW - 28);
                    context.method_51433(this.field_22793, pSub, ribX + 22, ribY + 12, 0xFFCCDDEE, false);
                }
            } else {
                MusicHudRenderer.renderRoundedRect(context, ribX, ribY, ribW, ribH, 5, 0xFF1A0E2E);
                MusicHudRenderer.renderRoundedBorder(context, ribX, ribY, ribW, ribH, 5, 0xFF9146FF, 0x44000000);
                try {
                    context.method_25291(class_10799.field_56883, ribbonIcon, ribX + 5, ribY + 4, 0.0f, 0.0f, 13, 13, 13, 13, alphaInt << 24 | 0xFFFFFF);
                } catch (Throwable ignored) {}
                context.method_51433(this.field_22793, "\u00a7d\u041f\u041e\u0414\u041f\u0418\u0421\u041a\u0410 \u00a7f(Tier 1)", ribX + 22, ribY + 4, -1, true);
                context.method_51433(this.field_22793, "\u00a7fTwitchUser \u00a77\u2022 3 \u043c\u0435\u0441\u044f\u0446\u0430!", ribX + 22, ribY + 12, 0xFFCCDDEE, false);
            }

            int testCardY = panelY + cardH + 6;
            int testCardH = 38;
            MusicHudRenderer.renderRoundedRect(context, panelX, testCardY, panelW, testCardH, 8, cardBg);
            MusicHudRenderer.renderRoundedBorder(context, panelX, testCardY, panelW, testCardH, 8, (int)(panelAlpha * 120.0f) << 24 | 0xFFAA00, 0x33000000);
            context.method_51433(this.field_22793, "\u00a7e\u26a1 \u0422\u0435\u0441\u0442 \u0441\u043e\u0431\u044b\u0442\u0438\u0439 \u0432 HUD:", panelX + 12, testCardY + 4, alphaInt << 24 | 0xFFFFFF, false);

            super.method_25394(context, mouseX, (int)((float)mouseY - slideY), delta);

            context.method_51448().popMatrix();
        }

        if (panelAlpha > 0.3f) {
            int alphaInt = (int)(panelAlpha * 180.0f);
            context.method_51433(this.field_22793, "\u00a78\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0432 \u0447\u0430\u0442\u0435: \u00a77/musicsubtitles twitch  \u00a78|  ESC \u0434\u043b\u044f \u0432\u044b\u0445\u043e\u0434\u0430",
                    24, screenH - 14, alphaInt << 24 | 0x888899, false);
        }

        this.renderJsabEasterEgg(context, screenW, screenH, mouseX, mouseY, now, panelAlpha);

        if (now - this.feedbackTime < 2800L && !this.feedbackMessage.isEmpty()) {
            float t = (float)(now - this.feedbackTime) / 2800.0f;
            float alpha = t < 0.15f ? t / 0.15f : (t > 0.8f ? 1.0f - (t - 0.8f) / 0.2f : 1.0f);
            int toastAlpha = (int)(alpha * 240.0f);
            int tw = this.field_22793.method_1727(this.feedbackMessage) + 20;
            int tx = (screenW - tw) / 2;
            int ty = screenH - 36;
            MusicHudRenderer.renderRoundedRect(context, tx, ty, tw, 20, 6, toastAlpha << 24 | 0x11111E);
            MusicHudRenderer.renderRoundedBorder(context, tx, ty, tw, 20, 6, toastAlpha << 24 | (this.feedbackColor & 0xFFFFFF), 0x44000000);
            context.method_51433(this.field_22793, this.feedbackMessage, tx + 10, ty + 6, toastAlpha << 24 | 0xFFFFFF, false);
        }

        TwitchAlertBanner.INSTANCE.renderInScreen(context, screenW, screenH, now);
    }

    private void renderJsabEasterEgg(class_332 context, int screenW, int screenH, int mouseX, int mouseY, long now, float panelAlpha) {
        if (panelAlpha < 0.05f) return;

        if (this.jsabLastTime == 0L) {
            this.jsabLastTime = now;
            this.jsabNextDash = now + 4000L;
            return;
        }
        float dt = Math.min(0.05f, (float)(now - this.jsabLastTime) / 1000.0f);
        this.jsabLastTime = now;

        if (now > this.jsabNextDash) {
            this.jsabNextDash = now + 5000L + (long)this.random.nextInt(6000);
            this.jsabDashEnd = now + 300L;
        }
        boolean isDashing = now < this.jsabDashEnd;
        float speed = isDashing ? 130.0f : 55.0f;

        float minX = 20.0f;
        float squadWidth = 3 * 16.0f;
        float maxX = (float)screenW - 24.0f - squadWidth - 10.0f;
        if (maxX < minX) maxX = minX;

        if (this.jsabMovingRight) {
            this.jsabSquadX += speed * dt;
            if (this.jsabSquadX >= maxX) {
                this.jsabSquadX = maxX;
                this.jsabMovingRight = false;
                for (JsabRunner r : this.jsabSquad) {
                    if (r.jumpVelocity <= 0.0f) r.jumpVelocity = 2.5f;
                }
            }
        } else {
            this.jsabSquadX -= speed * dt;
            if (this.jsabSquadX <= minX) {
                this.jsabSquadX = minX;
                this.jsabMovingRight = true;
                for (JsabRunner r : this.jsabSquad) {
                    if (r.jumpVelocity <= 0.0f) r.jumpVelocity = 2.5f;
                }
            }
        }

        int groundY = screenH - 18;
        int runnerH = 10;
        int alphaInt = (int)(panelAlpha * 255.0f);

        context.method_25294(16, groundY + 1, screenW - 16, groundY + 2, (int)(panelAlpha * 35.0f) << 24 | 0x334466);

        for (int i = 0; i < 4; i++) {
            JsabRunner runner = this.jsabSquad[i];
            float targetX = this.jsabSquadX + (float)i * 16.0f;
            runner.x += (targetX - runner.x) * Math.min(1.0f, dt * 14.0f);

            if (runner.jumpVelocity > 0.0f || runner.yOffset < 0.0f) {
                runner.yOffset -= runner.jumpVelocity * dt * 45.0f;
                runner.jumpVelocity -= dt * 65.0f;
                if (runner.yOffset > 0.0f) {
                    runner.yOffset = 0.0f;
                    runner.jumpVelocity = 0.0f;
                }
            }

            int rx = Math.round(runner.x);
            if (Math.abs(mouseX - (rx + 5)) < 10 && Math.abs(mouseY - (groundY - 5)) < 12) {
                if (runner.jumpVelocity <= 0.0f && runner.yOffset == 0.0f) {
                    runner.jumpVelocity = 3.5f;
                }
            }

            float hopWave = (float)Math.abs(Math.sin((now / 140.0) + (double)i * 0.75)) * 3.5f;
            int drawY = groundY - runnerH - Math.round(hopWave) + Math.round(runner.yOffset);

            int ghostDist1 = this.jsabMovingRight ? -4 : 4;
            int ghostDist2 = this.jsabMovingRight ? -8 : 8;
            int ghostDist3 = this.jsabMovingRight ? -12 : 12;
            int gAlpha1 = (int)(alphaInt * (isDashing ? 0.45f : 0.22f));
            int gAlpha2 = (int)(alphaInt * (isDashing ? 0.28f : 0.10f));
            renderJsabShape(context, runner.type, rx + ghostDist2, drawY, runner.color, gAlpha2, this.jsabMovingRight, true);
            renderJsabShape(context, runner.type, rx + ghostDist1, drawY, runner.color, gAlpha1, this.jsabMovingRight, true);
            if (isDashing) {
                int gAlpha3 = (int)(alphaInt * 0.16f);
                renderJsabShape(context, runner.type, rx + ghostDist3, drawY, runner.color, gAlpha3, this.jsabMovingRight, true);
            }

            renderJsabShape(context, runner.type, rx, drawY, runner.color, alphaInt, this.jsabMovingRight, false);
        }
    }

    private static void renderJsabShape(class_332 context, int type, int drawX, int drawY, int baseColor, int alpha, boolean movingRight, boolean isGhost) {
        if (alpha <= 0) return;
        int color = (alpha << 24) | (baseColor & 0xFFFFFF);

        switch (type) {
            case 0 -> {
                for (int row = 0; row < 10; row++) {
                    int halfW = (row * 5) / 9;
                    context.method_25294(drawX + 5 - halfW, drawY + row, drawX + 5 + halfW + 1, drawY + row + 1, color);
                }
                if (!isGhost) {
                    int eyeColor = (alpha << 24) | 0x1A1500;
                    if (movingRight) {
                        context.method_25294(drawX + 4, drawY + 5, drawX + 6, drawY + 7, eyeColor);
                        context.method_25294(drawX + 7, drawY + 6, drawX + 8, drawY + 7, eyeColor);
                    } else {
                        context.method_25294(drawX + 2, drawY + 6, drawX + 3, drawY + 7, eyeColor);
                        context.method_25294(drawX + 4, drawY + 5, drawX + 6, drawY + 7, eyeColor);
                    }
                }
            }
            case 1 -> {
                MusicHudRenderer.renderRoundedRect(context, drawX, drawY, 10, 10, 2, color);
                if (!isGhost) {
                    int eyeColor = (alpha << 24) | 0x051C2C;
                    if (movingRight) {
                        context.method_25294(drawX + 5, drawY + 3, drawX + 7, drawY + 5, eyeColor);
                        context.method_25294(drawX + 8, drawY + 3, drawX + 9, drawY + 5, eyeColor);
                    } else {
                        context.method_25294(drawX + 1, drawY + 3, drawX + 2, drawY + 5, eyeColor);
                        context.method_25294(drawX + 3, drawY + 3, drawX + 5, drawY + 5, eyeColor);
                    }
                }
            }
            case 2 -> {
                MusicHudRenderer.renderRoundedRect(context, drawX, drawY, 10, 10, 5, color);
                if (!isGhost) {
                    int eyeColor = (alpha << 24) | 0x042010;
                    if (movingRight) {
                        context.method_25294(drawX + 5, drawY + 3, drawX + 7, drawY + 5, eyeColor);
                        context.method_25294(drawX + 8, drawY + 4, drawX + 9, drawY + 5, eyeColor);
                    } else {
                        context.method_25294(drawX + 1, drawY + 4, drawX + 2, drawY + 5, eyeColor);
                        context.method_25294(drawX + 3, drawY + 3, drawX + 5, drawY + 5, eyeColor);
                    }
                }
            }
            case 3 -> {

                context.method_25294(drawX + 4, drawY + 0, drawX + 6, drawY + 1, color);
                context.method_25294(drawX + 3, drawY + 1, drawX + 7, drawY + 2, color);
                context.method_25294(drawX + 2, drawY + 2, drawX + 8, drawY + 3, color);
                context.method_25294(drawX + 1, drawY + 3, drawX + 9, drawY + 4, color);
                context.method_25294(drawX,     drawY + 4, drawX + 10, drawY + 5, color);

                context.method_25294(drawX,     drawY + 5, drawX + 10, drawY + 7, color);

                context.method_25294(drawX + 1, drawY + 7, drawX + 9, drawY + 9, color);

                context.method_25294(drawX + 2, drawY + 9, drawX + 8, drawY + 10, color);
                if (!isGhost) {
                    int eyeColor = (alpha << 24) | 0x281000;
                    if (movingRight) {
                        context.method_25294(drawX + 5, drawY + 4, drawX + 7, drawY + 6, eyeColor);
                        context.method_25294(drawX + 8, drawY + 4, drawX + 9, drawY + 6, eyeColor);
                    } else {
                        context.method_25294(drawX + 1, drawY + 4, drawX + 2, drawY + 6, eyeColor);
                        context.method_25294(drawX + 3, drawY + 4, drawX + 5, drawY + 6, eyeColor);
                    }
                }
            }
        }
    }

    private void triggerFeedback(String msg, int color) {
        this.feedbackMessage = msg;
        this.feedbackColor = color;
        this.feedbackTime = System.currentTimeMillis();
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256) {
            this.method_25419();
            return true;
        }
        if (this.channelField != null && this.channelField.method_25404(input)) {
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        if (!this.introAnim.isCompleted()) {
            this.introAnim.skip();
            return true;
        }
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int groundY = this.field_22790 - 18;
        for (int i = 0; i < 4; i++) {
            JsabRunner r = this.jsabSquad[i];
            int runnerY = groundY - 5 + Math.round(r.yOffset);
            if (Math.abs(mx - (Math.round(r.x) + 5)) < 12 && Math.abs(my - runnerY) < 16) {
                r.jumpVelocity = 8.0f;
                return true;
            }
        }
        return super.method_25402(click, bl);
    }

    public void method_25419() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    private String getEnableText() {
        return HudConfig.INSTANCE.twitchEnabled ? "\u00a7a\u25cf \u0418\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u044f: \u0412\u041a\u041b" : "\u00a7c\u25cb \u0418\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u044f: \u0412\u042b\u041a\u041b";
    }

    private String getSubsText() {
        return HudConfig.INSTANCE.twitchShowSubs ? "\u00a7d\u2605 \u0421\u0430\u0431\u044b: \u0412\u041a\u041b" : "\u00a77\u2605 \u0421\u0430\u0431\u044b: \u0412\u042b\u041a\u041b";
    }

    private String getRewardsText() {
        return HudConfig.INSTANCE.twitchShowRewards ? "\u00a7b\ud83d\udd2e \u0411\u0430\u043b\u043b\u044b: \u0412\u041a\u041b" : "\u00a77\ud83d\udd2e \u0411\u0430\u043b\u043b\u044b: \u0412\u042b\u041a\u041b";
    }

    private String getBitsText() {
        return HudConfig.INSTANCE.twitchShowBits ? "\u00a7e\ud83d\udc8e Bits: \u0412\u041a\u041b" : "\u00a77\ud83d\udc8e Bits: \u0412\u042b\u041a\u041b";
    }

    private String getSoundText() {
        return HudConfig.INSTANCE.twitchPlaySound ? "\u00a7a\ud83d\udd14 \u0417\u0432\u0443\u043a: \u0412\u041a\u041b" : "\u00a77\ud83d\udd15 \u0417\u0432\u0443\u043a: \u0412\u042b\u041a\u041b";
    }

    private String getPositionText() {
        boolean isBottom = "BOTTOM".equalsIgnoreCase(HudConfig.INSTANCE.twitchToastPosition);
        return isBottom ? "\u00a7f\ud83d\udccc \u041f\u043e\u0437\u0438\u0446\u0438\u044f: \u00a7b\u0421\u043d\u0438\u0437\u0443" : "\u00a7f\ud83d\udccc \u041f\u043e\u0437\u0438\u0446\u0438\u044f: \u00a7e\u0421\u0432\u0435\u0440\u0445\u0443";
    }

    private String getAutoCmdButtonText() {
        if (!HudConfig.INSTANCE.twitchAutoCommandEnabled) {
            return "\u00a77\u26a1 \u0410\u0432\u0442\u043e-\u043a\u043e\u043c\u0430\u043d\u0434\u0430: \u0412\u042b\u041a\u041b";
        }
        String cmd = HudConfig.INSTANCE.twitchAutoCommand != null ? HudConfig.INSTANCE.twitchAutoCommand.trim() : "";
        if (cmd.isEmpty()) {
            return "\u00a7d\u26a1 \u0410\u0432\u0442\u043e-\u043a\u043e\u043c\u0430\u043d\u0434\u0430: [\u041d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c]";
        }
        if (cmd.length() > 18) {
            cmd = cmd.substring(0, 15) + "...";
        }
        return "\u00a7d\u26a1 \u041a\u043e\u043c\u0430\u043d\u0434\u0430: \u00a7f" + cmd;
    }

    private static String trimText(class_327 tr, String text, int maxW) {
        if (tr == null || text == null || tr.method_1727(text) <= maxW) {
            return text;
        }
        while (text.length() > 3 && tr.method_1727(text + "...") > maxW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }
}

