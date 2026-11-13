package com.example.yandexmusichud.donationalerts;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import net.minecraft.class_10799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3532;

public class DonationAlertBanner {
    public static final DonationAlertBanner INSTANCE = new DonationAlertBanner();
    private static final class_2960 DA_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/donationalerts_icon.png");

    private DonationAlertBanner() {}

    public void render(class_332 context, int screenW, int screenH, long now) {
        if (!HudConfig.INSTANCE.daEnabled || !HudConfig.INSTANCE.daAlertsInHud) {
            return;
        }
        renderInternal(context, screenW, screenH, now, getTopMargin(), false);
    }

    public void renderInScreen(class_332 context, int screenW, int screenH, long now) {
        renderInternal(context, screenW, screenH, now, getTopMargin(), true);
    }

    private static int getTopMargin() {
        boolean twitchActiveAtTop = com.example.yandexmusichud.twitch.TwitchAlertBanner.INSTANCE.isShowing() && !"BOTTOM".equalsIgnoreCase(HudConfig.INSTANCE.twitchToastPosition);
        return twitchActiveAtTop ? 46 : 14;
    }

    private static void renderInternal(class_332 context, int screenW, int screenH, long now, int baseMarginY, boolean tickQueue) {
        DonationAlertQueue queue = DonationAlertQueue.INSTANCE;
        if (tickQueue) {
            queue.update();
        }

        if (!queue.isShowing()) {
            return;
        }

        DonationAlertEvent alert = queue.getActiveAlert();
        if (alert == null) return;

        float animProgress = queue.getAnimProgress();
        if (animProgress <= 0.01f) return;

        class_310 mc = class_310.method_1551();
        class_327 textRenderer = mc.field_1772;

        String title = alert.getTitle();
        String subtitle = alert.getSubtitle();
        int titleW = textRenderer.method_1727(title);
        int subW = subtitle.isEmpty() ? 0 : textRenderer.method_1727(subtitle);
        int textNeededW = Math.max(titleW, subW);

        String badge = alert.getBadgeText();
        int iconW = 11;
        int badgeW = textRenderer.method_1727(badge) + iconW + 9;

        int bannerH = subtitle.isEmpty() ? 22 : 25;
        int neededW = badgeW + textNeededW + 36;
        int bannerW = Math.max(240, Math.min(Math.min(400, screenW - 32), neededW));

        float slideOffset = (1.0f - animProgress) * -28.0f;
        int bannerX = (screenW - bannerW) / 2;
        int bannerY = baseMarginY + (int)slideOffset;

        int finalAlpha = (int)(animProgress * 255.0f);
        if (finalAlpha <= 5) return;

        renderAlertBody(context, textRenderer, alert, bannerX, bannerY, bannerW, bannerH, finalAlpha, now);
    }

    private static void renderAlertBody(class_332 context, class_327 textRenderer, DonationAlertEvent alert,
                                        int bannerX, int bannerY, int bannerW, int bannerH, int finalAlpha, long now) {
        int r = class_3532.method_15340((int)HudConfig.INSTANCE.cornerRadius, (int)0, (int)8);
        int accentColor = alert.getAccentColor();

        context.method_51448().pushMatrix();

        int bgRgb = 0x1A0E08;
        int bgColor = (int)((float)finalAlpha * 0.92f) << 24 | bgRgb;
        int accentGlow = (int)((float)finalAlpha * 0.35f) << 24 | (accentColor & 0xFFFFFF);

        MusicHudRenderer.renderRoundedRect(context, bannerX - 2, bannerY - 2, bannerW + 4, bannerH + 4, r + 2, accentGlow);
        MusicHudRenderer.renderRoundedRect(context, bannerX, bannerY, bannerW, bannerH, r, bgColor);

        int topBorder = (int)((float)finalAlpha * 0.90f) << 24 | (accentColor & 0xFFFFFF);
        int botBorder = (int)((float)finalAlpha * 0.35f) << 24 | 0x100804;
        MusicHudRenderer.renderRoundedBorder(context, bannerX, bannerY, bannerW, bannerH, r, topBorder, botBorder);

        String badge = alert.getBadgeText();
        int iconW = 11;
        int badgeW = textRenderer.method_1727(badge) + iconW + 9;
        int badgeX = bannerX + 6;
        int badgeY = bannerY + (bannerH - 13) / 2;
        int badgeBg = (int)((float)finalAlpha * 0.55f) << 24 | (accentColor & 0xFFFFFF);
        MusicHudRenderer.renderRoundedRect(context, badgeX, badgeY, badgeW, 13, 3, badgeBg);
        try {
            int iconTint = finalAlpha << 24 | 0xFFFFFF;
            context.method_25291(class_10799.field_56883, DA_ICON, badgeX + 3, badgeY + 1, 0.0f, 0.0f, iconW, iconW, iconW, iconW, iconTint);
        } catch (Throwable ignored) {}
        context.method_51433(textRenderer, badge, badgeX + iconW + 5, badgeY + 3, finalAlpha << 24 | 0xFFFFFF, false);

        int textX = badgeX + badgeW + 6;
        int maxTextW = bannerW - (badgeW + 24);
        if (maxTextW < 20) maxTextW = 20;

        String title = alert.getTitle();
        String subtitle = alert.getSubtitle();
        int titleW = textRenderer.method_1727(title);
        int subW = subtitle.isEmpty() ? 0 : textRenderer.method_1727(subtitle);

        float scrollOffsetTitle = 0.0f;
        if (titleW > maxTextW) {
            float diff = (float)(titleW - maxTextW);
            long cycle = 4000L;
            long t = (now + 500L) % cycle;
            if (t < 1200L) {
                scrollOffsetTitle = 0.0f;
            } else if (t < 2800L) {
                float p = (float)(t - 1200L) / 1600.0f;
                scrollOffsetTitle = p * diff;
            } else {
                scrollOffsetTitle = diff;
            }
        }

        float scrollOffsetSub = 0.0f;
        if (subW > maxTextW) {
            float diff = (float)(subW - maxTextW);
            long cycle = 4200L;
            long t = now % cycle;
            if (t < 1200L) {
                scrollOffsetSub = 0.0f;
            } else if (t < 3000L) {
                float p = (float)(t - 1200L) / 1800.0f;
                scrollOffsetSub = p * diff;
            } else {
                scrollOffsetSub = diff;
            }
        }

        context.method_44379(textX, bannerY + 1, textX + maxTextW, bannerY + bannerH - 1);
        if (!subtitle.isEmpty()) {
            int titleY = bannerY + 4;
            int subY = bannerY + 14;
            context.method_51433(textRenderer, title, textX - (int)scrollOffsetTitle, titleY, finalAlpha << 24 | 0xFFFFFF, false);
            context.method_51433(textRenderer, subtitle, textX - (int)scrollOffsetSub, subY, (int)((float)finalAlpha * 0.85f) << 24 | 0xFFE0BD, false);
        } else {
            context.method_51433(textRenderer, title, textX - (int)scrollOffsetTitle, bannerY + (bannerH - 8) / 2, finalAlpha << 24 | 0xFFFFFF, false);
        }
        context.method_44380();

        for (int i = 0; i < 4; ++i) {
            long cycle = 1600L + (long)i * 350L;
            float pNorm = (float)((now + (long)i * 450L) % cycle) / (float)cycle;
            float pAlpha = (float)Math.sin((double)pNorm * Math.PI);
            int partAlpha = (int)(pAlpha * ((float)finalAlpha / 255.0f) * 180.0f);
            if (partAlpha > 10) {
                float px = (float)(bannerX + bannerW - 10 - i * 8) + (float)Math.sin((double)i * 1.5 + (double)now * 0.003) * 3.0f;
                float py = (float)(bannerY + bannerH - 4) - pNorm * 18.0f;
                String sym = i % 2 == 0 ? "★" : (i % 3 == 0 ? "₽" : "✦");
                int pCol = partAlpha << 24 | (i % 2 == 0 ? (accentColor & 0xFFFFFF) : 0xFFD700);
                context.method_51433(textRenderer, sym, (int)px, (int)py, pCol, false);
            }
        }

        context.method_51448().popMatrix();
    }
}

