package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.twitch.TwitchManager;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;

public class TwitchStreamCommandScreen extends class_437 {
    private final class_437 parent;
    private class_342 cmdField;
    private ModernButtonWidget toggleBtn;
    private String feedbackMessage = "";
    private long feedbackTime = 0L;
    private int feedbackColor = 0x9146FF;
    private final long openTime;

    public TwitchStreamCommandScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u043f\u0440\u0438 \u0441\u0442\u0430\u0440\u0442\u0435 \u0441\u0442\u0440\u0438\u043c\u0430"));
        this.parent = parent;
        this.openTime = System.currentTimeMillis();
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {

        context.method_25294(0, 0, this.field_22789, this.field_22790, 0xBB0A0812);
    }

    protected void method_25426() {
        this.method_37068();

        int screenW = this.field_22789;
        int screenH = this.field_22790;

        int panelW = Math.min(380, screenW - 20);
        int panelH = 215;
        int panelX = (screenW - panelW) / 2;
        int panelY = (screenH - panelH) / 2;

        int rowY = panelY + 54;
        int btnH = 20;

        this.toggleBtn = new ModernButtonWidget(panelX + 14, rowY, panelW - 28, btnH,
                (class_2561)class_2561.method_43470(this.getToggleText()), b -> {
            HudConfig.INSTANCE.twitchAutoCommandEnabled = !HudConfig.INSTANCE.twitchAutoCommandEnabled;
            b.method_25355((class_2561)class_2561.method_43470(this.getToggleText()));
            b.setCustomBgColor(HudConfig.INSTANCE.twitchAutoCommandEnabled ? 0x4400FF88 : 0x3322222E);
            HudConfig.save();
            String msg = HudConfig.INSTANCE.twitchAutoCommandEnabled
                    ? "\u00a7a\u2714 \u0410\u0432\u0442\u043e\u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u044b \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u0430!"
                    : "\u00a77\u0410\u0432\u0442\u043e\u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u044b \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u0430";
            this.triggerFeedback(msg, HudConfig.INSTANCE.twitchAutoCommandEnabled ? 0x00FF88 : 0xAAAAAA);
        });
        this.toggleBtn.setCustomBgColor(HudConfig.INSTANCE.twitchAutoCommandEnabled ? 0x4400FF88 : 0x3322222E);
        this.method_37063(this.toggleBtn);

        int fieldY = rowY + btnH + 22;
        this.cmdField = new class_342(this.field_22793, panelX + 14, fieldY, panelW - 28, 18,
                (class_2561)class_2561.method_43470("Twitch Stream Command"));
        this.cmdField.method_1852(HudConfig.INSTANCE.twitchAutoCommand != null ? HudConfig.INSTANCE.twitchAutoCommand : "");
        this.cmdField.method_1863(text -> {
            HudConfig.INSTANCE.twitchAutoCommand = text.trim();
            HudConfig.save();
        });
        this.method_37063(this.cmdField);

        int presetRowY = fieldY + 28;
        String[] presets = new String[]{"/stream live", "/twitch live", "/live", "/me \u0441\u0442\u0440\u0438\u043c\u0438\u0442"};
        int pGap = 5;
        int pW = (panelW - 28 - pGap * (presets.length - 1)) / presets.length;
        for (int i = 0; i < presets.length; i++) {
            final String presetCmd = presets[i];
            int pX = panelX + 14 + i * (pW + pGap);
            ModernButtonWidget pBtn = new ModernButtonWidget(pX, presetRowY, pW, 18,
                    (class_2561)class_2561.method_43470(presetCmd), b -> {
                HudConfig.INSTANCE.twitchAutoCommand = presetCmd;
                HudConfig.save();
                if (this.cmdField != null) {
                    this.cmdField.method_1852(presetCmd);
                }
                this.triggerFeedback("\u00a7a\u2714 \u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u0430: \u00a7e" + presetCmd, 0x00FF88);
            });
            pBtn.setCustomBgColor(0x33251A38);
            this.method_37063(pBtn);
        }

        int botBtnY = panelY + panelH - 32;
        int botBtnW = (panelW - 34) / 2;

        ModernButtonWidget testBtn = new ModernButtonWidget(panelX + 14, botBtnY, botBtnW, btnH,
                (class_2561)class_2561.method_43470("\u25b6 \u0422\u0435\u0441\u0442 (\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c)"), b -> {
            String cur = this.cmdField != null ? this.cmdField.method_1882().trim() : HudConfig.INSTANCE.twitchAutoCommand;
            if (cur.isEmpty()) {
                this.triggerFeedback("\u00a7c\u2716 \u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u0443 \u043f\u0435\u0440\u0435\u0434 \u043e\u0442\u043f\u0440\u0430\u0432\u043a\u043e\u0439!", 0xFF4455);
                return;
            }
            class_310 mc = class_310.method_1551();
            if (mc == null || mc.field_1724 == null) {
                this.triggerFeedback("\u00a7e\u26a0 \u0412\u044b \u043d\u0435 \u0432 \u0438\u0433\u0440\u043e\u0432\u043e\u043c \u043c\u0438\u0440\u0435 \u0434\u043b\u044f \u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0438!", 0xFFCC00);
                return;
            }
            TwitchManager.getInstance().executeStreamCommand(cur, true);
            this.triggerFeedback("\u00a7a\u2714 \u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430 \u0432 \u0447\u0430\u0442 \u043e\u0442 \u0432\u0430\u0448\u0435\u0433\u043e \u043b\u0438\u0446\u0430!", 0x00FF88);
        });
        testBtn.setCustomBgColor(0x449146FF);
        this.method_37063(testBtn);

        ModernButtonWidget doneBtn = new ModernButtonWidget(panelX + 20 + botBtnW, botBtnY, botBtnW, btnH,
                (class_2561)class_2561.method_43470("\u2714 \u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c"), b -> {
            this.method_25419();
        });
        doneBtn.setCustomBgColor(0x3322222E);
        this.method_37063(doneBtn);

        ModernButtonWidget closeBtn = new ModernButtonWidget(panelX + panelW - 24, panelY + 8, 16, 16,
                (class_2561)class_2561.method_43470("\u00d7"), b -> this.method_25419());
        closeBtn.setCustomBgColor(0x3322222E);
        this.method_37063(closeBtn);
    }

    private String getToggleText() {
        return HudConfig.INSTANCE.twitchAutoCommandEnabled
                ? "\u2714 \u0410\u0432\u0442\u043e\u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0430: \u0412\u041a\u041b"
                : "\u2716 \u0410\u0432\u0442\u043e\u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0430: \u0412\u042b\u041a\u041b";
    }

    private void triggerFeedback(String msg, int color) {
        this.feedbackMessage = msg;
        this.feedbackTime = System.currentTimeMillis();
        this.feedbackColor = color;
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        class_327 tr = this.field_22793;
        int screenW = this.field_22789;
        int screenH = this.field_22790;

        float progress = Math.min(1.0f, (float)(now - this.openTime) / 120.0f);
        float ease = (float)Math.sin(progress * Math.PI * 0.5);
        int alphaInt = (int)(ease * 255.0f);

        int panelW = Math.min(380, screenW - 20);
        int panelH = 215;
        int panelX = (screenW - panelW) / 2;
        int panelY = (screenH - panelH) / 2;

        int bgCol = (int)((float)alphaInt * 0.94f) << 24 | 0x0E0B18;
        int borderTop = (int)((float)alphaInt * 0.85f) << 24 | 0x9146FF;
        int borderBot = (int)((float)alphaInt * 0.25f) << 24 | 0x22133D;

        MusicHudRenderer.renderRoundedRect(context, panelX, panelY, panelW, panelH, 8, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, panelX, panelY, panelW, panelH, 8, borderTop, borderBot);

        String titleText = "\u00a7d\u00a7l\u26a1 \u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u043f\u0440\u0438 \u0441\u0442\u0430\u0440\u0442\u0435 \u0441\u0442\u0440\u0438\u043c\u0430";
        context.method_51433(tr, titleText, panelX + 14, panelY + 10, alphaInt << 24 | 0xFFFFFF, true);

        context.method_51433(tr, "\u00a77\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u0432 \u0447\u0430\u0442 \u0441\u0435\u0440\u0432\u0435\u0440\u0430 \u043e\u0442 \u0432\u0430\u0448\u0435\u0433\u043e \u043b\u0438\u0446\u0430,",
                panelX + 14, panelY + 24, (int)((float)alphaInt * 0.75f) << 24 | 0xCCDDEE, false);
        context.method_51433(tr, "\u00a77\u043a\u043e\u0433\u0434\u0430 \u0432\u0430\u0448 \u0441\u0442\u0440\u0438\u043c \u043f\u0435\u0440\u0435\u0445\u043e\u0434\u0438\u0442 \u0432 \u0441\u0442\u0430\u0442\u0443\u0441 \u00a7a\u00a7lLIVE\u00a77.",
                panelX + 14, panelY + 35, (int)((float)alphaInt * 0.75f) << 24 | 0xCCDDEE, false);

        int fieldLabelY = panelY + 54 + 20 + 8;
        context.method_51433(tr, "\u00a7e\u2726 \u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0438\u043b\u0438 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435:",
                panelX + 14, fieldLabelY, alphaInt << 24 | 0xFFE088, false);

        int varHintY = fieldLabelY + 33;
        context.method_51433(tr, "\u00a78\u0428\u0430\u0431\u043b\u043e\u043d\u044b: \u00a77{channel} \u00a78- \u043a\u0430\u043d\u0430\u043b, \u00a77{name} \u00a78- \u0438\u043c\u044f",
                panelX + 14, varHintY, (int)((float)alphaInt * 0.65f) << 24 | 0x888899, false);

        super.method_25394(context, mouseX, mouseY, delta);

        if (!this.feedbackMessage.isEmpty() && now - this.feedbackTime < 3500L) {
            float fAge = (float)(now - this.feedbackTime);
            float fAlpha = fAge < 250.0f ? fAge / 250.0f : (fAge > 3000.0f ? 1.0f - (fAge - 3000.0f) / 500.0f : 1.0f);
            int fbAlphaInt = (int)(fAlpha * 255.0f);
            int msgW = tr.method_1727(this.feedbackMessage) + 16;
            int msgX = (screenW - msgW) / 2;
            int msgY = panelY + panelH + 8;
            MusicHudRenderer.renderRoundedRect(context, msgX, msgY, msgW, 20, 6, (int)((float)fbAlphaInt * 0.9f) << 24 | 0x140A1F);
            MusicHudRenderer.renderRoundedBorder(context, msgX, msgY, msgW, 20, 6, (int)((float)fbAlphaInt * 0.8f) << 24 | (this.feedbackColor & 0xFFFFFF), 0x33000000);
            context.method_51433(tr, this.feedbackMessage, msgX + 8, msgY + 6, fbAlphaInt << 24 | 0xFFFFFF, false);
        }
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256) {
            this.method_25419();
            return true;
        }
        if (this.cmdField != null && this.cmdField.method_25404(input)) {
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
}

