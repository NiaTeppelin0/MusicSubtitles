package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.hud.NiaFoxMaidModel;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11909;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_591;

public class NiaAuthorScreen
extends class_437 {
    private static final class_2960 SKIN_TEXTURE = class_2960.method_60655((String)"yandexmusichud", (String)"textures/gui/nia_foxmaid_skin.png");
    private static final String TELEGRAM_URL = "https://t.me/TG_Nia_Teppelin0";
    private static final String DISCORD_URL = "https://discord.gg/4C2AtagDR9";
    private static final String TWITCH_URL = "https://twitch.tv/nia_teppelin0";
    private final class_437 parent;
    private final Random random = new Random();
    private class_591 niaModel;
    private float modelRotY = 30.0f;
    private float modelRotX = -5.0f;
    private boolean draggingModel = false;
    private float modelScale = 1.0f;
    private String toastMessage = null;
    private long toastTime = 0L;
    private int toastColor = 7124206;
    private final List<ScreenParticle> particles = new ArrayList<ScreenParticle>();

    public NiaAuthorScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"\u0421\u043e\u0437\u0434\u0430\u0442\u0435\u043b\u044c \u2014 \u041d\u0438\u044f (Nia_Teppelin_0)"));
        this.parent = parent;
    }

    protected void method_25426() {
        super.method_25426();
        try {
            if (this.field_22787 != null) {
                this.niaModel = NiaFoxMaidModel.createPlayerModel();
                this.niaModel.method_2805(true);
                this.niaModel.field_27433.field_3654 = -0.06f;
                this.niaModel.field_3401.field_3654 = -0.06f;
                if (this.field_22787.method_1483() != null) {
                    this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)class_3417.field_26980, (float)1.25f, (float)0.9f));
                    this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)((class_3414)class_3417.field_14793.comp_349()), (float)1.1f, (float)0.8f));
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void openUrl(String url) {
        com.example.yandexmusichud.favorites.FavoritesManager.openUrlSafely(url);
        this.playClickSound();
    }

    private void playClickSound() {
        try {
            if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)((class_3414)class_3417.field_15015.comp_349()), (float)1.0f, (float)0.6f));
            }
        }
        catch (Exception exception) {

        }
    }

    private void triggerToast(String msg, int color) {
        this.toastMessage = msg;
        this.toastTime = System.currentTimeMillis();
        this.toastColor = color;
    }

    private void spawnSparks(int cx, int cy, int count) {
        for (int i = 0; i < count; ++i) {
            float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
            float speed = 1.2f + this.random.nextFloat() * 3.2f;
            float vx = (float)Math.cos(angle) * speed;
            float vy = (float)Math.sin(angle) * speed - 0.8f;
            String sym = switch (this.random.nextInt(5)) {
                case 0 -> "\u2726";
                case 1 -> "\u266a";
                case 2 -> "\u266b";
                case 3 -> "\u00b7";
                default -> "\u2605";
            };
            int col = switch (this.random.nextInt(4)) {
                case 0 -> -9653010;
                case 1 -> -6587670;
                case 2 -> -3874567;
                default -> -1;
            };
            this.particles.add(new ScreenParticle(cx, cy, vx, vy, col, sym, 30 + this.random.nextInt(25)));
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        context.method_25294(0, 0, this.field_22789, this.field_22790, -670232809);
        if (!this.draggingModel) {
            this.modelRotY += delta * 1.5f;
            if (this.modelRotY >= 360.0f) {
                this.modelRotY -= 360.0f;
            }
        }
        int cardW = Math.min(520, this.field_22789 - 24);
        int cardH = Math.min(270, this.field_22790 - 24);
        int cardX = (this.field_22789 - cardW) / 2;
        int cardY = (this.field_22790 - cardH) / 2;
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 12, -267183578);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 12, 810576002, 810576002);
        context.method_25294(cardX + 16, cardY, cardX + cardW - 16, cardY + 1, 0x44FFFFFF);
        int leftW = 190;
        int sepX = cardX + leftW;
        context.method_25294(sepX, cardY + 12, sepX + 1, cardY + cardH - 12, 0x18FFFFFF);
        this.renderModelSection(context, mouseX, mouseY, cardX, cardY, leftW, cardH);
        int rightX = sepX + 14;
        int rightW = cardW - leftW - 28;
        this.renderAuthorSection(context, mouseX, mouseY, rightX, cardY, rightW, cardH);
        float dt60 = Math.min(3.0f, delta);
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            ScreenParticle p = this.particles.get(i);
            if (!p.update(dt60)) {
                this.particles.remove(i);
                continue;
            }
            float alpha = 1.0f - (float)p.age / (float)p.maxLife;
            int col = (int)(alpha * 255.0f) << 24 | p.color & 0xFFFFFF;
            context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, col, true);
        }
        this.renderToast(context);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void renderModelSection(class_332 context, int mouseX, int mouseY, int cardX, int cardY, int leftW, int cardH) {
        int centerX = cardX + leftW / 2;
        int pedestalY = cardY + cardH - 58;
        int pedW = 126;
        int pedH = 20;
        int pedX = centerX - pedW / 2;
        MusicHudRenderer.renderRoundedRect(context, pedX, pedestalY, pedW, pedH, 10, 1142433848);
        MusicHudRenderer.renderRoundedBorder(context, pedX, pedestalY, pedW, pedH, 10, 1246396330, 1246396330);
        MusicHudRenderer.renderRoundedRect(context, pedX + 12, pedestalY + 4, pedW - 24, pedH - 8, 6, 1433187566);
        int shadowW = (int)(74.0f * this.modelScale);
        int shadowH = (int)(12.0f * this.modelScale);
        MusicHudRenderer.renderRoundedRect(context, centerX - shadowW / 2, pedestalY + 3, shadowW, shadowH, 6, 0x44000000);
        if (this.niaModel != null) {
            int vpX1 = cardX + 5;
            int vpY1 = cardY + 25;
            int vpX2 = cardX + leftW - 5;
            int vpY2 = pedestalY + 5;
            int vpH = vpY2 - vpY1;
            float scale = 0.97f * (float)vpH * this.modelScale / 2.125f;
            try {
                context.method_70854(this.niaModel, SKIN_TEXTURE, scale, this.modelRotX, this.modelRotY, -1.0625f, vpX1, vpY1, vpX2, vpY2);
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
        int titleY = cardY + 12;
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)"\u00a7b\u2726 Nia_FoxMaid \u2726"), centerX, titleY, -1);
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)"\u00a783D \u043c\u043e\u0434\u0435\u043b\u044c \u0430\u0432\u0442\u043e\u0440\u0430"), centerX, titleY + 10, -5592406);
        boolean hoverModel = mouseX >= cardX + 10 && mouseX <= cardX + leftW - 10 && mouseY >= cardY + 10 && mouseY <= cardY + cardH - 15;
        int hintY = cardY + cardH - 22;
        String hint = hoverModel ? "\u00a7e\u041f\u043e\u0442\u044f\u043d\u0438\u0442\u0435 \u041b\u041a\u041c \u0434\u043b\u044f \u0432\u0440\u0430\u0449\u0435\u043d\u0438\u044f" : "\u00a78\u041b\u041a\u041c: \u0432\u0440\u0430\u0449\u0435\u043d\u0438\u0435 \u2022 \u041a\u043e\u043b\u0435\u0441\u043e: \u0437\u0443\u043c";
        int hw = this.field_22793.method_1727(hint);
        context.method_51433(this.field_22793, hint, centerX - hw / 2, hintY, -1, false);
    }

    private void renderAuthorSection(class_332 context, int mouseX, int mouseY, int x, int cardY, int w, int cardH) {
        int curY = cardY + 14;
        int badgeW = 90;
        int badgeH = 15;
        MusicHudRenderer.renderRoundedRect(context, x, curY, badgeW, badgeH, 4, 982217423);
        MusicHudRenderer.renderRoundedBorder(context, x, curY, badgeW, badgeH, 4, 1721465578, 1721465578);
        context.method_51433(this.field_22793, "\u00a7b\u2726 \u0421\u041e\u0417\u0414\u0410\u0422\u0415\u041b\u042c \u2726", x + 8, curY + 4, -1, false);
        context.method_51433(this.field_22793, "\u00a7f\u041d\u0438\u044f \u00a77(\u00a7b@Nia_Teppelin_0\u00a77)", x, curY += 20, -1, false);
        curY += 12;
        String desc = "\u0421\u043e\u0437\u0434\u0430\u0442\u0435\u043b\u044c \u043c\u043e\u0434\u0430 Music Subtitles";
        for (String line : this.wrapText(desc, w)) {
            context.method_51433(this.field_22793, "\u00a77" + line, x, curY, -2236963, false);
            curY += 10;
        }
        context.method_25294(x, curY += 6, x + w, curY + 1, 0x18FFFFFF);
        int btnW = w;
        int btnH = 26;
        int btnTgY = curY += 8;
        boolean hoverTg = mouseX >= x && mouseX <= x + btnW && mouseY >= btnTgY && mouseY <= btnTgY + btnH;
        int bgTg = hoverTg ? 1140885708 : 570460364;
        int borderTg = hoverTg ? -16725761 : 1426098380;
        MusicHudRenderer.renderRoundedRect(context, x, btnTgY, btnW, btnH, 6, bgTg);
        MusicHudRenderer.renderRoundedBorder(context, x, btnTgY, btnW, btnH, 6, borderTg, borderTg);
        context.method_51433(this.field_22793, "\u00a7b\ud83d\udcac Telegram", x + 10, btnTgY + 5, -1, true);
        context.method_51433(this.field_22793, "\u00a7f@TG_Nia_Teppelin0 \u00a77(\u041a\u0430\u043d\u0430\u043b \u0438 \u043d\u043e\u0432\u043e\u0441\u0442\u0438)", x + 10, btnTgY + 15, -2236963, true);
        context.method_51433(this.field_22793, hoverTg ? "\u00a7b\u2794" : "\u00a77\u2794", x + btnW - 18, btnTgY + 9, -1, true);
        int btnDcY = curY += btnH + 6;
        boolean hoverDc = mouseX >= x && mouseX <= x + btnW && mouseY >= btnDcY && mouseY <= btnDcY + btnH;
        int bgDc = hoverDc ? 1146643954 : 576218610;
        int borderDc = hoverDc ? -9270822 : 1431856626;
        MusicHudRenderer.renderRoundedRect(context, x, btnDcY, btnW, btnH, 6, bgDc);
        MusicHudRenderer.renderRoundedBorder(context, x, btnDcY, btnW, btnH, 6, borderDc, borderDc);
        context.method_51433(this.field_22793, "\u00a79\ud83c\udfae Discord", x + 10, btnDcY + 5, -1, true);
        context.method_51433(this.field_22793, "\u00a7f\u041c\u043e\u0439 \u0441\u0435\u0440\u0432\u0435\u0440 Minecraft \u00a77(discord.gg/4C2AtagDR9)", x + 10, btnDcY + 15, -2236963, true);
        context.method_51433(this.field_22793, hoverDc ? "\u00a79\u2794" : "\u00a77\u2794", x + btnW - 18, btnDcY + 9, -1, true);
        int btnTwY = curY += btnH + 6;
        boolean hoverTw = mouseX >= x && mouseX <= x + btnW && mouseY >= btnTwY && mouseY <= btnTwY + btnH;
        int bgTw = hoverTw ? 1150371583 : 579946239;
        int borderTw = hoverTw ? -4221697 : 1435584255;
        MusicHudRenderer.renderRoundedRect(context, x, btnTwY, btnW, btnH, 6, bgTw);
        MusicHudRenderer.renderRoundedBorder(context, x, btnTwY, btnW, btnH, 6, borderTw, borderTw);
        context.method_51433(this.field_22793, "\u00a7d\ud83d\udfe3 Twitch", x + 10, btnTwY + 5, -1, true);
        context.method_51433(this.field_22793, "\u00a7f@nia_teppelin0 \u00a77(\u0422\u0440\u0430\u043d\u0441\u043b\u044f\u0446\u0438\u0438 \u0438 \u0441\u0442\u0440\u0438\u043c\u044b)", x + 10, btnTwY + 15, -2236963, true);
        context.method_51433(this.field_22793, hoverTw ? "\u00a7d\u2794" : "\u00a77\u2794", x + btnW - 18, btnTwY + 9, -1, true);
        int bottomBtnH = 20;
        int copyW = w - 85;
        int closeW = 75;
        int copyX = x;
        int copyY = cardY + cardH - 28;
        boolean hoverCopy = mouseX >= copyX && mouseX <= copyX + copyW && mouseY >= copyY && mouseY <= copyY + bottomBtnH;
        int bgCopy = hoverCopy ? 862762222 : 577549550;
        int borderCopy = hoverCopy ? -9653010 : 1433187566;
        MusicHudRenderer.renderRoundedRect(context, copyX, copyY, copyW, bottomBtnH, 5, bgCopy);
        MusicHudRenderer.renderRoundedBorder(context, copyX, copyY, copyW, bottomBtnH, 5, borderCopy, borderCopy);
        String copyText = hoverCopy ? "\u00a7b\ud83d\udccb \u0421\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u044b" : "\u00a7f\ud83d\udccb \u0421\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u044b";
        int cw = this.field_22793.method_1727(copyText);
        context.method_51433(this.field_22793, copyText, copyX + (copyW - cw) / 2, copyY + 6, -1, true);
        int closeX = copyX + copyW + 10;
        int closeY = copyY;
        boolean hoverClose = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + bottomBtnH;
        int bgClose = hoverClose ? 869024853 : 583812181;
        int borderClose = hoverClose ? -3390379 : 0x55CC4455;
        MusicHudRenderer.renderRoundedRect(context, closeX, closeY, closeW, bottomBtnH, 5, bgClose);
        MusicHudRenderer.renderRoundedBorder(context, closeX, closeY, closeW, bottomBtnH, 5, borderClose, borderClose);
        String closeText = hoverClose ? "\u00a7c\u00d7 \u0417\u0430\u043a\u0440\u044b\u0442\u044c" : "\u00a77\u00d7 \u0417\u0430\u043a\u0440\u044b\u0442\u044c";
        int clw = this.field_22793.method_1727(closeText);
        context.method_51433(this.field_22793, closeText, closeX + (closeW - clw) / 2, closeY + 6, -1, true);
    }

    private void renderToast(class_332 context) {
        if (this.toastMessage == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.toastTime;
        if (elapsed > 3500L) {
            this.toastMessage = null;
            return;
        }
        float alpha = 1.0f;
        if (elapsed > 2800L) {
            alpha = Math.max(0.0f, (float)(3500L - elapsed) / 700.0f);
        }
        int tw = this.field_22793.method_1727(this.toastMessage);
        int pillW = tw + 24;
        int pillH = 20;
        int pillX = (this.field_22789 - pillW) / 2;
        int pillY = this.field_22790 - 34;
        int alphaInt = (int)(alpha * 255.0f);
        int bgCol = (int)(alpha * 230.0f) << 24 | 0x131A26;
        int borderCol = alphaInt << 24 | this.toastColor & 0xFFFFFF;
        MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 6, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 6, borderCol, borderCol);
        context.method_51433(this.field_22793, this.toastMessage, pillX + 12, pillY + 6, alphaInt << 24 | 0xFFFFFF, false);
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

    public boolean method_25402(class_11909 click, boolean bl) {
        int curY;
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int cardW = Math.min(520, this.field_22789 - 24);
        int cardH = Math.min(270, this.field_22790 - 24);
        int cardX = (this.field_22789 - cardW) / 2;
        int cardY = (this.field_22790 - cardH) / 2;
        int leftW = 190;
        int rightX = cardX + leftW + 14;
        int rightW = cardW - leftW - 28;
        if (mx >= cardX + 10 && mx <= cardX + leftW - 10 && my >= cardY + 10 && my <= cardY + cardH - 30) {
            this.draggingModel = true;
            this.spawnSparks(mx, my, 8);
            return true;
        }
        int btnW = rightW;
        int btnH = 26;
        String desc = "\u0421\u043e\u0437\u0434\u0430\u0442\u0435\u043b\u044c \u043c\u043e\u0434\u0430 Music Subtitles";
        int descLines = this.wrapText(desc, rightW).size();
        int btnTgY = curY = cardY + 14 + 20 + 12 + (descLines * 10) + 6 + 8;
        if (mx >= rightX && mx <= rightX + btnW && my >= btnTgY && my <= btnTgY + btnH) {
            this.spawnSparks(mx, my, 20);
            this.openUrl(TELEGRAM_URL);
            this.triggerToast("\u00a7b\u2726 \u0421\u0441\u044b\u043b\u043a\u0430 Telegram \u043e\u0442\u043a\u0440\u044b\u0442\u0430 \u0432 \u0431\u0440\u0430\u0443\u0437\u0435\u0440\u0435! \ud83d\udcac", 51455);
            return true;
        }
        int btnDcY = curY += btnH + 6;
        if (mx >= rightX && mx <= rightX + btnW && my >= btnDcY && my <= btnDcY + btnH) {
            this.spawnSparks(mx, my, 20);
            this.openUrl(DISCORD_URL);
            this.triggerToast("\u00a79\u2726 \u041f\u0440\u0438\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 Discord \u043e\u0442\u043a\u0440\u044b\u0442\u043e \u0432 \u0431\u0440\u0430\u0443\u0437\u0435\u0440\u0435! \ud83c\udfae", 5793266);
            return true;
        }
        int btnTwY = curY += btnH + 6;
        if (mx >= rightX && mx <= rightX + btnW && my >= btnTwY && my <= btnTwY + btnH) {
            this.spawnSparks(mx, my, 20);
            this.openUrl(TWITCH_URL);
            this.triggerToast("\u00a7d\u2726 \u041a\u0430\u043d\u0430\u043b Twitch \u043e\u0442\u043a\u0440\u044b\u0442 \u0432 \u0431\u0440\u0430\u0443\u0437\u0435\u0440\u0435! \ud83d\udfe3", 9520895);
            return true;
        }
        int bottomBtnH = 20;
        int copyW = rightW - 85;
        int closeW = 75;
        int copyX = rightX;
        int copyY = cardY + cardH - 28;
        if (mx >= copyX && mx <= copyX + copyW && my >= copyY && my <= copyY + bottomBtnH) {
            String allContacts = "\u041d\u0438\u044f (Nia_Teppelin_0)\nTelegram: https://t.me/TG_Nia_Teppelin0\nDiscord (\u0421\u0435\u0440\u0432\u0435\u0440 Minecraft): https://discord.gg/4C2AtagDR9\nTwitch: https://twitch.tv/nia_teppelin0";
            if (this.field_22787 != null && this.field_22787.field_1774 != null) {
                this.field_22787.field_1774.method_1455(allContacts);
            }
            try {
                if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                    this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)class_3417.field_14709, (float)1.2f, (float)0.8f));
                }
            }
            catch (Exception exception) {

            }
            this.spawnSparks(copyX + copyW / 2, copyY + 10, 26);
            this.triggerToast("\u00a7a\u2726 \u0412\u0441\u0435 \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u044b \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u044b \u0432 \u0431\u0443\u0444\u0435\u0440 \u043e\u0431\u043c\u0435\u043d\u0430! \ud83d\udccb", 65416);
            return true;
        }
        int closeX = copyX + copyW + 10;
        int closeY = copyY;
        if (mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + bottomBtnH) {
            this.playClickSound();
            this.method_25419();
            return true;
        }
        return super.method_25402(click, bl);
    }

    public boolean method_25406(class_11909 click) {
        this.draggingModel = false;
        return super.method_25406(click);
    }

    public boolean method_25403(class_11909 click, double offsetX, double offsetY) {
        if (this.draggingModel) {
            this.modelRotY += (float)offsetX * 1.6f;
            this.modelRotX = class_3532.method_15363((float)(this.modelRotX - (float)offsetY * 1.2f), (float)-50.0f, (float)50.0f);
            return true;
        }
        return super.method_25403(click, offsetX, offsetY);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int cardW = Math.min(520, this.field_22789 - 24);
        int cardX = (this.field_22789 - cardW) / 2;
        int leftW = 190;
        if (mouseX >= (double)cardX && mouseX <= (double)(cardX + leftW)) {
            this.modelScale = class_3532.method_15363((float)(this.modelScale + (float)verticalAmount * 0.08f), (float)0.65f, (float)1.85f);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void method_25419() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }

    public boolean method_25422() {
        return true;
    }

    private static class ScreenParticle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        String symbol;
        int age = 0;
        int maxLife;

        ScreenParticle(float x, float y, float vx, float vy, int color, String symbol, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.symbol = symbol;
            this.maxLife = maxLife;
        }

        boolean update(float dt60) {
            this.x += this.vx * dt60;
            this.y += this.vy * dt60;
            this.vy += 0.03f * dt60;
            this.vx *= (float)Math.pow(0.98, dt60);
            return ++this.age < this.maxLife;
        }
    }
}

