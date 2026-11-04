package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.donationalerts.DonationAlertBanner;
import com.example.yandexmusichud.donationalerts.DonationAlertsManager;
import com.example.yandexmusichud.hud.ModernButtonWidget;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.class_10799;
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

public class DonationAlertsSettingsScreen extends class_437 {
    private static final class_2960 DA_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/donationalerts_icon.png");
    private final class_437 parent;
    private final DonationAlertsLogoMergeAnimation introAnim;
    private class_342 tokenField;
    private class_342 minAmountField;
    private String feedbackMessage = "";
    private long feedbackTime = 0L;
    private int feedbackColor = 0xFA7218;

    private final List<AmbientParticle> bgParticles = new ArrayList<>();
    private final Random random = new Random();
    private boolean particlesInit = false;

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
        float wavePhase;
        int color;
        String symbol;
        float alpha;

        AmbientParticle(float x, float y, float vy, int color, String symbol) {
            this.x = x;
            this.y = y;
            this.vy = vy;
            this.color = color;
            this.symbol = symbol;
            this.wavePhase = (float)(Math.random() * Math.PI * 2.0);
            this.alpha = 0.3f + (float)Math.random() * 0.55f;
        }

        void update(int screenW, int screenH) {
            this.wavePhase += 0.025f;
            this.x += (float)Math.sin(this.wavePhase) * 0.6f;
            this.y += this.vy;
            if (this.y < -15.0f) {
                this.y = screenH + 10.0f;
                this.x = (float)Math.random() * (float)screenW;
            }
        }
    }

    public DonationAlertsSettingsScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("DonationAlerts"));
        this.parent = parent;
        this.introAnim = new DonationAlertsLogoMergeAnimation();
    }

    private void initParticles(int screenW, int screenH) {
        if (this.particlesInit) return;
        this.particlesInit = true;
        String[] symbols = new String[]{"✦", "★", "₽", "$", "🪙", "•", "❤"};
        int[] colors = new int[]{0xFA7218, 0xFFD700, 0xFFAA00, 0xFF6600, 0x00FF88, 0xFFCC33};
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

        DonationAlertsManager.getInstance().start();

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

        int fieldY = card1Top + 48;
        int fieldH = 18;
        this.tokenField = new class_342(this.field_22793, col1X + 12, fieldY, colW - 24, fieldH,
                (class_2561)class_2561.method_43470("DonationAlerts Token"));
        this.tokenField.method_1852(HudConfig.INSTANCE.daToken != null ? HudConfig.INSTANCE.daToken : "");
        this.tokenField.method_1863(text -> {
            String clean = DonationAlertsManager.sanitizeToken(text);
            HudConfig.INSTANCE.daToken = clean;
            HudConfig.save();
        });
        this.method_37063(this.tokenField);

        int btnRow1Y = fieldY + fieldH + 8;
        int col1BtnW = (colW - 28) / 2;
        int btnH = 20;

        ModernButtonWidget pasteBtn = new ModernButtonWidget(col1X + 12, btnRow1Y, col1BtnW, btnH,
                (class_2561)class_2561.method_43470("📋 Вставить"), b -> {
            String clip = "";
            try {
                clip = GLFW.glfwGetClipboardString(class_310.method_1551().method_22683().method_4490());
            } catch (Throwable ignored) {}
            if (clip != null && !clip.trim().isEmpty()) {
                if (clip.contains("/r/")) {
                    this.triggerFeedback("\u00a7e\u26a0 \u042d\u0442\u043e \u0441\u0441\u044b\u043b\u043a\u0430 \u0434\u043b\u044f \u0434\u043e\u043d\u0430\u0442\u0430! \u041d\u0443\u0436\u0435\u043d \u0442\u043e\u043a\u0435\u043d \u0438\u0437 '\u041e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u044f'", 0xFFAA00);
                }
                String clean = DonationAlertsManager.sanitizeToken(clip);
                this.tokenField.method_1852(clean);
                HudConfig.INSTANCE.daToken = clean;
                HudConfig.save();
                DonationAlertsManager.getInstance().start();
                this.triggerFeedback("\u00a7a\u2714 \u0422\u043e\u043a\u0435\u043d \u0432\u0441\u0442\u0430\u0432\u043b\u0435\u043d \u0438 \u043f\u0440\u043e\u0432\u0435\u0440\u044f\u0435\u0442\u0441\u044f...", 0x00FF88);
            } else {
                this.triggerFeedback("§c✖ Буфер обмена пуст", 0xFF4455);
            }
        });
        pasteBtn.setCustomBgColor(0x3322222E);
        this.method_37063(pasteBtn);

        ModernButtonWidget connectBtn = new ModernButtonWidget(col1X + 16 + col1BtnW, btnRow1Y, col1BtnW, btnH,
                (class_2561)class_2561.method_43470("\u21bb \u041f\u0440\u043e\u0432\u0435\u0448\u0438\u0442\u044c".replace("\u0448", "\u0440")), b -> {
            String cur = this.tokenField != null ? this.tokenField.method_1882() : HudConfig.INSTANCE.daToken;
            String clean = DonationAlertsManager.sanitizeToken(cur);
            if (this.tokenField != null && !clean.equals(cur)) {
                this.tokenField.method_1852(clean);
            }
            HudConfig.INSTANCE.daToken = clean;
            HudConfig.save();
            DonationAlertsManager.getInstance().start();
            this.triggerFeedback("\u00a76\u21bb \u0417\u0430\u043f\u0440\u043e\u0441 \u0434\u0430\u043d\u043d\u044b\u0445 DonationAlerts...", 0xFA7218);
        });
        connectBtn.setCustomBgColor(0x44FA7218);
        this.method_37063(connectBtn);

        int btnRow2Y = btnRow1Y + btnH + 6;
        ModernButtonWidget enableBtn = new ModernButtonWidget(col1X + 12, btnRow2Y, colW - 24, btnH,
                (class_2561)class_2561.method_43470(this.getEnableText()), b -> {
            HudConfig.INSTANCE.daEnabled = !HudConfig.INSTANCE.daEnabled;
            b.method_25355((class_2561)class_2561.method_43470(this.getEnableText()));
            b.setCustomBgColor(HudConfig.INSTANCE.daEnabled ? 0x4400FF88 : 0x3322222E);
            HudConfig.save();
            if (HudConfig.INSTANCE.daEnabled) {
                DonationAlertsManager.getInstance().start();
                this.triggerFeedback("§a✔ DonationAlerts интеграция включена!", 0x00FF88);
            } else {
                DonationAlertsManager.getInstance().disconnect();
                this.triggerFeedback("§7DonationAlerts отключен", 0xAAAAAA);
            }
        });
        enableBtn.setCustomBgColor(HudConfig.INSTANCE.daEnabled ? 0x4400FF88 : 0x3322222E);
        this.method_37063(enableBtn);

        int card2Top = panelY;
        int toggleW = (colW - 28) / 2;
        int tRow1Y = card2Top + 24;

        ModernButtonWidget alertsBtn = new ModernButtonWidget(col2X + 12, tRow1Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getAlertsText()), b -> {
            HudConfig.INSTANCE.daAlertsInHud = !HudConfig.INSTANCE.daAlertsInHud;
            b.method_25355((class_2561)class_2561.method_43470(this.getAlertsText()));
            b.setCustomBgColor(HudConfig.INSTANCE.daAlertsInHud ? 0x44FA7218 : 0x3322222E);
            HudConfig.save();
        });
        alertsBtn.setCustomBgColor(HudConfig.INSTANCE.daAlertsInHud ? 0x44FA7218 : 0x3322222E);
        this.method_37063(alertsBtn);

        ModernButtonWidget soundBtn = new ModernButtonWidget(col2X + 16 + toggleW, tRow1Y, toggleW, btnH,
                (class_2561)class_2561.method_43470(this.getSoundText()), b -> {
            HudConfig.INSTANCE.daSoundEnabled = !HudConfig.INSTANCE.daSoundEnabled;
            b.method_25355((class_2561)class_2561.method_43470(this.getSoundText()));
            b.setCustomBgColor(HudConfig.INSTANCE.daSoundEnabled ? 0x4400FF88 : 0x3322222E);
            HudConfig.save();
        });
        soundBtn.setCustomBgColor(HudConfig.INSTANCE.daSoundEnabled ? 0x4400FF88 : 0x3322222E);
        this.method_37063(soundBtn);

        int minRowY = tRow1Y + btnH + 6;
        int fieldW = 54;
        int fieldX = col2X + colW - 12 - fieldW - 14;
        this.minAmountField = new class_342(this.field_22793, fieldX, minRowY, fieldW, 18,
                (class_2561)class_2561.method_43470("Min Amount"));
        this.minAmountField.method_1852(String.valueOf(HudConfig.INSTANCE.daMinAmountAlert));
        this.minAmountField.method_1863(text -> {
            String digits = text.replaceAll("[^0-9]", "");
            int val = 0;
            if (!digits.isEmpty()) {
                try {
                    val = Math.min(1000000, Integer.parseInt(digits));
                } catch (Exception ignored) {}
            }
            HudConfig.INSTANCE.daMinAmountAlert = val;
            HudConfig.save();
        });
        this.method_37063(this.minAmountField);

        int presetRowY = minRowY + 22;
        int[] presets = new int[]{0, 50, 100, 500, 1000};
        String[] presetLabels = new String[]{"\u0412\u0441\u0435", "50 \u20bd", "100 \u20bd", "500 \u20bd", "1k \u20bd"};
        int pGap = 4;
        int pBtnW = (colW - 24 - pGap * (presets.length - 1)) / presets.length;
        for (int pIdx = 0; pIdx < presets.length; pIdx++) {
            final int pVal = presets[pIdx];
            String pLabel = presetLabels[pIdx];
            int pX = col2X + 12 + pIdx * (pBtnW + pGap);
            ModernButtonWidget pBtn = new ModernButtonWidget(pX, presetRowY, pBtnW, 18,
                    (class_2561)class_2561.method_43470(pLabel), b -> {
                HudConfig.INSTANCE.daMinAmountAlert = pVal;
                HudConfig.save();
                if (this.minAmountField != null) {
                    this.minAmountField.method_1852(String.valueOf(pVal));
                }
                String msg = pVal == 0 ? "\u00a7a\u2714 \u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u044e\u0442\u0441\u044f \u0432\u0441\u0435 \u0434\u043e\u043d\u0430\u0442\u044b (\u043e\u0442 0 \u20bd)" : "\u00a76\u2714 \u041c\u0438\u043d. \u0441\u0443\u043c\u043c\u0430 \u0434\u043e\u043d\u0430\u0442\u0430 \u0434\u043b\u044f \u043e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u044f: " + pVal + " \u20bd";
                this.triggerFeedback(msg, pVal == 0 ? 0x00FF88 : 0xFA7218);
            });
            pBtn.setCustomBgColor(0x3322222E);
            this.method_37063(pBtn);
        }

        int testCardY = card1Top + cardH + 6;
        int testBtnCount = 4;
        int testGap = 6;
        int testBtnW = (panelW - 24 - testGap * (testBtnCount - 1)) / testBtnCount;
        int testBtnY = testCardY + 14;

        ModernButtonWidget test100Btn = new ModernButtonWidget(panelX + 12, testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("🔥 Тест 100 ₽"), b -> {
            DonationAlertsManager.getInstance().sendTestDonation("Алексей", 100.0, "RUB", "Спасибо за стрим! ❤");
            this.triggerFeedback("§6🔥 Тестовый донат 100 ₽ отправлен в HUD!", 0xFA7218);
        });
        this.method_37063(test100Btn);

        ModernButtonWidget test500Btn = new ModernButtonWidget(panelX + 12 + (testBtnW + testGap), testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("⚡ Тест 500 ₽"), b -> {
            DonationAlertsManager.getInstance().sendTestDonation("Мария", 500.0, "RUB", "На кофе и вкусняшки к чаю!");
            this.triggerFeedback("§e⚡ Тестовый донат 500 ₽ отправлен в HUD!", 0xFFD700);
        });
        this.method_37063(test500Btn);

        ModernButtonWidget test1000Btn = new ModernButtonWidget(panelX + 12 + (testBtnW + testGap) * 2, testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("👑 Тест 1000 ₽"), b -> {
            DonationAlertsManager.getInstance().sendTestDonation("Меценат", 1000.0, "RUB", "Топовый контент, так держать!");
            this.triggerFeedback("§6👑 Тестовый донат 1000 ₽ отправлен в HUD!", 0xFFAA00);
        });
        this.method_37063(test1000Btn);

        ModernButtonWidget test5000Btn = new ModernButtonWidget(panelX + 12 + (testBtnW + testGap) * 3, testBtnY, testBtnW, btnH,
                (class_2561)class_2561.method_43470("💎 Тест 5000 ₽"), b -> {
            DonationAlertsManager.getInstance().sendTestDonation("VIP Спонсор", 5000.0, "RUB", "Лучший стример, продолжай в том же духе!");
            this.triggerFeedback("§a💎 Тестовый донат 5000 ₽ отправлен в HUD!", 0x00FF88);
        });
        this.method_37063(test5000Btn);

        int closeBtnW = 20;
        int closeBtnH = 20;
        int closeBtnX = screenW - 28;
        int closeBtnY = 16;

        int twitchBtnW = 96;
        int twitchBtnH = 20;
        int twitchBtnX = closeBtnX - twitchBtnW - 8;
        int twitchBtnY = 16;
        ModernButtonWidget twitchNavBtn = new ModernButtonWidget(twitchBtnX, twitchBtnY, twitchBtnW, twitchBtnH,
                (class_2561)class_2561.method_43470("§d✦ §fTwitch Live"), b -> {
            class_310.method_1551().method_1507(new TwitchSettingsScreen(this.parent));
        });
        twitchNavBtn.setCustomBgColor(0x332A153D);
        this.method_37063(twitchNavBtn);

        ModernButtonWidget closeBtn = new ModernButtonWidget(closeBtnX, closeBtnY, closeBtnW, closeBtnH,
                (class_2561)class_2561.method_43470("×"), b -> this.method_25419());
        closeBtn.setCustomBgColor(0x3322222E);
        this.method_37063(closeBtn);
    }

    private String getEnableText() {
        return HudConfig.INSTANCE.daEnabled ? "✔ Интеграция: ВКЛ" : "✖ Интеграция: ВЫКЛ";
    }

    private String getAlertsText() {
        return HudConfig.INSTANCE.daAlertsInHud ? "Алерты: ВКЛ" : "Алерты: ВЫКЛ";
    }

    private String getSoundText() {
        return HudConfig.INSTANCE.daSoundEnabled ? "Звук: ВКЛ" : "Звук: ВЫКЛ";
    }

    private String getMinAmtText() {
        int amt = HudConfig.INSTANCE.daMinAmountAlert;
        return amt == 0 ? "Мин: 0 ₽ (Все)" : "Мин: " + amt + " ₽";
    }

    private void triggerFeedback(String msg, int color) {
        this.feedbackMessage = msg;
        this.feedbackTime = System.currentTimeMillis();
        this.feedbackColor = color;
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        int screenW = this.field_22789;
        int screenH = this.field_22790;

        context.method_25294(0, 0, screenW, screenH, 0xFF0D0A0E);

        int topGradient = -1157627899;
        int botGradient = -587202555;
        context.method_25296(0, 0, screenW, 55, topGradient, 0);
        context.method_25296(0, screenH - 55, screenW, screenH, 0, botGradient);

        for (AmbientParticle p : this.bgParticles) {
            p.update(screenW, screenH);
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

            DonationAlertsManager dam = DonationAlertsManager.getInstance();
            DonationAlertsManager.Status status = dam.getStatus();
            String token = HudConfig.INSTANCE.daToken;

            String statusBadge;
            int badgeBorder;
            int badgeBg;

            if (!HudConfig.INSTANCE.daEnabled || token == null || token.trim().isEmpty()) {
                statusBadge = "\u00a77\u25cb \u041e\u0422\u041a\u041b\u042e\u0427\u0415\u041d\u041e";
                badgeBorder = 0x555566;
                badgeBg = 0x14141E;
            } else if (status == DonationAlertsManager.Status.CONNECTING) {
                statusBadge = "\u00a7e\u21bb \u041f\u041e\u0414\u041a\u041b\u042e\u0427\u0415\u041d\u0418\u0415...";
                badgeBorder = 0xFFCC00;
                badgeBg = 0x241F0A;
            } else if (status == DonationAlertsManager.Status.ERROR) {
                String err = dam.getLastErrorMessage();
                if (err != null && !err.isEmpty() && err.length() <= 24) {
                    statusBadge = "\u00a7c\u25cf " + err.toUpperCase();
                } else {
                    statusBadge = "\u00a7c\u25cf \u041e\u0428\u0418\u0411\u041a\u0410";
                }
                badgeBorder = 0xFF4455;
                badgeBg = 0x240A10;
            } else {
                statusBadge = "\u00a7a\u25cf \u0412 \u0421\u0415\u0422\u0418";
                badgeBorder = 0x00FF88;
                badgeBg = 0x0A2414;
            }

            String titleText = "\u00a76\u00a7lDONATION \u00a7f\u00a7lALERTS";
            int titleW = this.field_22793.method_1727(titleText);
            int bw = this.field_22793.method_1727(statusBadge) + 10;
            int bh = 12;
            int bx = titleX + titleW + 8;
            int by = titleY - 1;

            if (alphaInt > 10) {
                context.method_51433(this.field_22793, titleText, titleX, titleY, alphaInt << 24 | 0xFFFFFF, true);

                MusicHudRenderer.renderRoundedRect(context, bx, by, bw, bh, 3, (int)((float)alphaInt * 0.85f) << 24 | (badgeBg & 0xFFFFFF));
                MusicHudRenderer.renderRoundedBorder(context, bx, by, bw, bh, 3, (int)((float)alphaInt * 0.9f) << 24 | (badgeBorder & 0xFFFFFF), 0x33000000);
                context.method_51433(this.field_22793, statusBadge, bx + 5, by + 2, alphaInt << 24 | 0xFFFFFF, false);

                String subtitle = "\u0418\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u044f \u043e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u0439 \u043e \u0434\u043e\u043d\u0430\u0442\u0430\u0445 \u0432 HUD";
                int maxSubW = screenW - 150 - titleX;
                if (maxSubW > 50) {
                    subtitle = trimText(this.field_22793, subtitle, maxSubW);
                }
                context.method_51433(this.field_22793, "\u00a77" + subtitle, titleX, titleY + 12, (int)((float)alphaInt * 0.75f) << 24 | 0xDDDDDD, false);
            }
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
            int bgCol = (int)((float)alphaInt * 0.85f) << 24 | 0x140D18;
            int borderTop = (int)((float)alphaInt * 0.45f) << 24 | 0xFA7218;
            int borderBot = (int)((float)alphaInt * 0.20f) << 24 | 0x0A060C;

            MusicHudRenderer.renderRoundedRect(context, col1X, panelY, colW, cardH, 8, bgCol);
            MusicHudRenderer.renderRoundedBorder(context, col1X, panelY, colW, cardH, 8, borderTop, borderBot);
            context.method_51433(this.field_22793, "\u00a76\u00a7l\u2726 \u0410\u043a\u043a\u0430\u0443\u043d\u0442 \u0438 API-\u0422\u043e\u043a\u0435\u043d", col1X + 12, panelY + 8, alphaInt << 24 | 0xFFA044, true);

            DonationAlertsManager dam = DonationAlertsManager.getInstance();
            String username = dam.getStreamerUsername();
            int avatarSize = 20;
            int avX = col1X + 12;
            int avY = panelY + 22;

            if (dam.hasCustomAvatar() && dam.getAvatarTextureId() != null) {
                try {
                    context.method_25291(class_10799.field_56883, dam.getAvatarTextureId(), avX, avY, 0.0f, 0.0f, avatarSize, avatarSize, avatarSize, avatarSize, alphaInt << 24 | 0xFFFFFF);
                } catch (Throwable ignored) {
                    context.method_25291(class_10799.field_56883, DA_ICON, avX + 2, avY + 2, 0.0f, 0.0f, 16, 16, 16, 16, alphaInt << 24 | 0xFFFFFF);
                }
            } else {
                MusicHudRenderer.renderRoundedRect(context, avX, avY, avatarSize, avatarSize, 4, (int)((float)alphaInt * 0.27f) << 24 | 0xFA7218);
                try {
                    context.method_25291(class_10799.field_56883, DA_ICON, avX + 2, avY + 2, 0.0f, 0.0f, 16, 16, 16, 16, alphaInt << 24 | 0xFFFFFF);
                } catch (Throwable ignored) {}
            }
            MusicHudRenderer.renderRoundedBorder(context, avX, avY, avatarSize, avatarSize, 4, 0x66FA7218, 0x22000000);
            String subText;
            int subColor;
            if (dam.getStatus() == DonationAlertsManager.Status.CONNECTED) {
                subText = "\u00a7a\u2714 \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e \u043a DonationAlerts";
                subColor = 0x00FF88;
            } else if (dam.getStatus() == DonationAlertsManager.Status.ERROR) {
                subText = "\u00a7c\u26a0 " + dam.getLastErrorMessage();
                subColor = 0xFF4455;
            } else if (dam.getStatus() == DonationAlertsManager.Status.CONNECTING) {
                subText = "\u00a7e\u21bb \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a DA...";
                subColor = 0xFFCC00;
            } else {
                subText = "\u00a77DonationAlerts \u043f\u0440\u043e\u0444\u0438\u043b\u044c";
                subColor = 0xAAAAAA;
            }
            context.method_51433(this.field_22793, "\u00a7f" + username, avX + avatarSize + 6, avY + 2, alphaInt << 24 | 0xFFFFFF, false);
            context.method_51433(this.field_22793, subText, avX + avatarSize + 6, avY + 11, (int)((float)alphaInt * 0.75f) << 24 | (subColor & 0xFFFFFF), false);

            int helpY = panelY + cardH - 14;
            context.method_51433(this.field_22793, "\u00a77\u0421\u0441\u044b\u043b\u043a\u0430 \u0438\u043b\u0438 \u0442\u043e\u043a\u0435\u043d: DA -> \u041e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u044f", col1X + 12, helpY, (int)((float)alphaInt * 0.6f) << 24 | 0x888899, false);

            MusicHudRenderer.renderRoundedRect(context, col2X, panelY, colW, cardH, 8, bgCol);
            MusicHudRenderer.renderRoundedBorder(context, col2X, panelY, colW, cardH, 8, borderTop, borderBot);
            context.method_51433(this.field_22793, "\u00a7e\u00a7l\u2699 \u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u041e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u0439", col2X + 12, panelY + 8, alphaInt << 24 | 0xFFD700, true);

            int minLabelY = panelY + 24 + 20 + 6;
            int fieldW = 54;
            int fieldX = col2X + colW - 12 - fieldW - 14;
            context.method_51433(this.field_22793, "\u00a7e\u2726 \u041c\u0438\u043d. \u0441\u0443\u043c\u043c\u0430 \u0434\u043e\u043d\u0430\u0442\u0430:", col2X + 12, minLabelY + 5, alphaInt << 24 | 0xFFFFFF, false);
            context.method_51433(this.field_22793, "\u00a76\u20bd", fieldX + fieldW + 4, minLabelY + 5, alphaInt << 24 | 0xFA7218, true);

            int infoBoxY = minLabelY + 44;
            int infoBoxH = cardH - (infoBoxY - panelY) - 10;
            MusicHudRenderer.renderRoundedRect(context, col2X + 12, infoBoxY, colW - 24, infoBoxH, 6, 0x22000000);
            MusicHudRenderer.renderRoundedBorder(context, col2X + 12, infoBoxY, colW - 24, infoBoxH, 6, 0x22FA7218, 0x11000000);
            context.method_51433(this.field_22793, "\u00a76\u2726 \u041f\u043e\u0437\u0438\u0446\u0438\u044f: \u00a7f\u0426\u0435\u043d\u0442\u0440 \u0412\u0435\u0440\u0445", col2X + 18, infoBoxY + 6, alphaInt << 24 | 0xFFFFFF, false);
            context.method_51433(this.field_22793, "\u00a77\u041e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u044f \u043d\u0438\u0436\u0435 \u0443\u043a\u0430\u0437\u0430\u043d\u043d\u043e\u0439 \u0441\u0443\u043c\u043c\u044b", col2X + 18, infoBoxY + 17, (int)((float)alphaInt * 0.7f) << 24 | 0xCCCCCC, false);
            context.method_51433(this.field_22793, "\u00a77\u043d\u0435 \u0431\u0443\u0434\u0443\u0442 \u0432\u044b\u0432\u043e\u0434\u0438\u0442\u044c\u0441\u044f \u0432 HUD.", col2X + 18, infoBoxY + 27, (int)((float)alphaInt * 0.7f) << 24 | 0xCCCCCC, false);
            context.method_51433(this.field_22793, "\u00a78\u041f\u0440\u0438 0 \u20bd \u043f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u044e\u0442\u0441\u044f \u0432\u0441\u0435 \u0434\u043e\u043d\u0430\u0442\u044b.", col2X + 18, infoBoxY + 37, (int)((float)alphaInt * 0.6f) << 24 | 0x999999, false);

            int testCardY = panelY + cardH + 6;
            int testCardH = 40;
            MusicHudRenderer.renderRoundedRect(context, panelX, testCardY, panelW, testCardH, 8, bgCol);
            MusicHudRenderer.renderRoundedBorder(context, panelX, testCardY, panelW, testCardH, 8, borderTop, borderBot);
            context.method_51433(this.field_22793, "\u00a76\u00a7l\u26a1 \u041c\u0433\u043d\u043e\u0432\u0435\u043d\u043d\u044b\u0439 \u0422\u0435\u0441\u0442-\u0414\u0440\u0430\u0439\u0432 \u041e\u043f\u043e\u0432\u0435\u0449\u0435\u043d\u0438\u0439", panelX + 12, testCardY + 4, alphaInt << 24 | 0xFFA044, true);

            super.method_25394(context, mouseX, (int)((float)mouseY - slideY), delta);

            context.method_51448().popMatrix();
        }

        if (panelAlpha > 0.3f) {
            int alphaInt = (int)(panelAlpha * 180.0f);
            context.method_51433(this.field_22793, "\u00a78\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0432 \u0447\u0430\u0442\u0435: \u00a77/musicsubtitles da  \u00a78|  ESC \u0434\u043b\u044f \u0432\u044b\u0445\u043e\u0434\u0430",
                    24, screenH - 14, alphaInt << 24 | 0x888899, false);
        }

        if (!this.feedbackMessage.isEmpty() && now - this.feedbackTime < 3500L) {
            float fAge = (float)(now - this.feedbackTime);
            float fAlpha = fAge < 250.0f ? fAge / 250.0f : (fAge > 3000.0f ? 1.0f - (fAge - 3000.0f) / 500.0f : 1.0f);
            int fbAlphaInt = (int)(fAlpha * 255.0f);
            int msgW = this.field_22793.method_1727(this.feedbackMessage) + 16;
            int msgX = (screenW - msgW) / 2;
            int msgY = screenH - 36;
            MusicHudRenderer.renderRoundedRect(context, msgX, msgY, msgW, 20, 6, (int)((float)fbAlphaInt * 0.9f) << 24 | 0x140A05);
            MusicHudRenderer.renderRoundedBorder(context, msgX, msgY, msgW, 20, 6, (int)((float)fbAlphaInt * 0.8f) << 24 | (this.feedbackColor & 0xFFFFFF), 0x33000000);
            context.method_51433(this.field_22793, this.feedbackMessage, msgX + 8, msgY + 6, fbAlphaInt << 24 | 0xFFFFFF, false);
        }

        this.renderJsabSquad(context, screenW, screenH, now, panelAlpha);

        DonationAlertBanner.INSTANCE.renderInScreen(context, screenW, screenH, now);
    }

    private void renderJsabSquad(class_332 context, int screenW, int screenH, long now, float panelAlpha) {
        if (panelAlpha < 0.05f) return;

        if (this.jsabLastTime == 0L) {
            this.jsabLastTime = now;
            this.jsabNextDash = now + 4000L + (long)this.random.nextInt(3000);
        }
        float dt = class_3532.method_15363((float)(now - this.jsabLastTime) / 1000.0f, 0.0f, 0.1f);
        this.jsabLastTime = now;

        boolean isDashing = now < this.jsabDashEnd;
        if (!isDashing && now >= this.jsabNextDash) {
            this.jsabDashEnd = now + 450L;
            this.jsabNextDash = now + 5000L + (long)this.random.nextInt(4000);
            for (JsabRunner runner : this.jsabSquad) {
                runner.jumpVelocity = -3.2f - this.random.nextFloat() * 1.8f;
            }
        }

        float speed = (isDashing ? 190.0f : 65.0f) * dt;
        if (this.jsabMovingRight) {
            this.jsabSquadX += speed;
            if (this.jsabSquadX > (float)(screenW - 90)) {
                this.jsabMovingRight = false;
            }
        } else {
            this.jsabSquadX -= speed;
            if (this.jsabSquadX < 24.0f) {
                this.jsabMovingRight = true;
            }
        }

        int groundY = screenH - 12;
        context.method_25294(0, groundY + 4, screenW, groundY + 5, 0x22FA7218);

        float dt60 = Math.min(3.0f, dt * 60.0f);
        for (int i = 0; i < this.jsabSquad.length; ++i) {
            JsabRunner r = this.jsabSquad[i];
            float targetX = this.jsabSquadX + (float)i * 18.0f;
            r.x += (targetX - r.x) * (1.0f - (float)Math.pow(0.75, dt60));

            r.jumpVelocity += 9.8f * dt;
            r.yOffset += r.jumpVelocity * dt60;
            if (r.yOffset >= 0.0f) {
                r.yOffset = 0.0f;
                r.jumpVelocity = 0.0f;
            }

            int rx = (int)r.x;
            int ry = groundY + (int)r.yOffset;
            int rCol = r.color;

            switch (r.type) {
                case 0 -> {
                    int ts = 7;
                    for (int row = 0; row < ts; ++row) {
                        int rowW = row * 2 + 1;
                        int rowX = rx - row;
                        int rowY = ry - ts + row;
                        context.method_25294(rowX, rowY, rowX + rowW, rowY + 1, rCol);
                    }
                }
                case 1 -> {
                    int ss = 6;
                    context.method_25294(rx - ss / 2, ry - ss, rx + ss / 2, ry, rCol);
                }
                case 2 -> {
                    int cs = 6;
                    MusicHudRenderer.renderRoundedRect(context, rx - cs / 2, ry - cs, cs, cs, cs / 2, rCol);
                }
                case 3 -> {
                    int ps = 6;
                    MusicHudRenderer.renderRoundedRect(context, rx - ps / 2, ry - ps, ps, ps, 2, rCol);
                }
            }
        }
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        if (!this.introAnim.isCompleted()) {
            this.introAnim.skip();
            return true;
        }
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int groundY = this.field_22790 - 18;
        for (int i = 0; i < this.jsabSquad.length; i++) {
            JsabRunner r = this.jsabSquad[i];
            int runnerY = groundY - 3 + Math.round(r.yOffset);
            if (Math.abs(mx - Math.round(r.x)) < 12 && Math.abs(my - runnerY) < 16) {
                r.jumpVelocity = -6.5f;
                return true;
            }
        }
        return super.method_25402(click, bl);
    }

    public boolean method_25404(net.minecraft.class_11908 input) {
        if (input.comp_4795() == 256) {
            this.method_25419();
            return true;
        }
        if (this.tokenField != null && this.tokenField.method_25404(input)) {
            return true;
        }
        if (this.minAmountField != null && this.minAmountField.method_25404(input)) {
            return true;
        }
        return super.method_25404(input);
    }

    public void method_25419() {
        if (this.parent != null) {
            class_310.method_1551().method_1507(this.parent);
        } else {
            super.method_25419();
        }
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

