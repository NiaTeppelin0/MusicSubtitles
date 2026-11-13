package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.keybinds.MusicControlKeybinds;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_6880;

public class KeybindSetupScreen
extends class_437 {
    private final class_437 parent;
    private final Random random = new Random();
    private class_304 selectedBinding = null;
    private int hoveredIndex = -1;
    private final List<KeyParticle> particles = new ArrayList<KeyParticle>();
    private final List<KeyShockwave> shockwaves = new ArrayList<KeyShockwave>();
    private final List<KeybindItem> items = new ArrayList<KeybindItem>();

    public KeybindSetupScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"\u0413\u043e\u0440\u044f\u0447\u0438\u0435 \u043a\u043b\u0430\u0432\u0438\u0448\u0438"));
        this.parent = parent;
        this.initItems();
    }

    private void initItems() {
        this.items.clear();
        this.items.add(new KeybindItem(MusicControlKeybinds.playPauseKey, "\u23ef", "\u041f\u0430\u0443\u0437\u0430 / \u041f\u043b\u0435\u0439", "\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u0438\u043b\u0438 \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0438\u0442\u044c \u0432\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u0435.", "\u0423\u0434\u043e\u0431\u043d\u043e: X \u0438\u043b\u0438 Numpad 5.", 88, -16711800));
        this.items.add(new KeybindItem(MusicControlKeybinds.prevTrackKey, "\u23ee", "\u041f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0439 \u0442\u0440\u0435\u043a", "\u0412\u0435\u0440\u043d\u0443\u0442\u044c\u0441\u044f \u043a \u043f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0435\u043c\u0443 \u0442\u0440\u0435\u043a\u0443.", "\u0423\u0434\u043e\u0431\u043d\u043e: Z \u0438\u043b\u0438 Numpad 4.", 90, -13388315));
        this.items.add(new KeybindItem(MusicControlKeybinds.nextTrackKey, "\u23ed", "\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439 \u0442\u0440\u0435\u043a", "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u043d\u0430 \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439 \u0442\u0440\u0435\u043a \u0432 \u043f\u043b\u0435\u0439\u043b\u0438\u0441\u0442\u0435.", "\u0423\u0434\u043e\u0431\u043d\u043e: C \u0438\u043b\u0438 Numpad 6.", 67, -13388315));
        this.items.add(new KeybindItem(MusicControlKeybinds.radialMenuKey, "\u25ce", "\u041a\u0440\u0443\u0433\u043e\u0432\u043e\u0435 \u043c\u0435\u043d\u044e", "\u041a\u0440\u0443\u0433\u043e\u0432\u043e\u0439 \u0434\u0438\u0441\u043a \u0432\u044b\u0431\u043e\u0440\u0430 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0439 \u043a\u043e\u043b\u0451\u0441\u0438\u043a\u043e\u043c \u043c\u044b\u0448\u0438.", "\u0423\u0434\u043e\u0431\u043d\u043e: R (\u0437\u0430\u0436\u0430\u0442\u044c \u0438 \u043a\u0440\u0443\u0442\u0438\u0442\u044c \u043a\u043e\u043b\u0451\u0441\u0438\u043a\u043e).", 82, -16718337));
        this.items.add(new KeybindItem(MusicControlKeybinds.toggleHudKey, "\ud83d\udc41", "\u0412\u0438\u0434\u0436\u0435\u0442 HUD", "\u0421\u043a\u0440\u044b\u0442\u044c \u0438\u043b\u0438 \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u043a\u0430\u0440\u0442\u043e\u0447\u043a\u0443 \u043f\u043b\u0435\u0435\u0440\u0430.", "\u0423\u0434\u043e\u0431\u043d\u043e: H (\u0441\u043a\u0440\u044b\u0442\u044c \u0434\u043b\u044f \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u0430).", 72, -10929));
        this.items.add(new KeybindItem(MusicControlKeybinds.editHudKey, "\u2699", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043e\u043a\u043d\u043e \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a \u043c\u043e\u0434\u0430.", "\u0423\u0434\u043e\u0431\u043d\u043e: O.", 79, -16718337));
        this.items.add(new KeybindItem(MusicControlKeybinds.toggleSubtitlesKey, "\ud83d\udcac", "\u0421\u0443\u0431\u0442\u0438\u0442\u0440\u044b", "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0440\u0435\u0436\u0438\u043c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432 (3D / 2D / \u0412\u042b\u041a\u041b).", "\u0423\u0434\u043e\u0431\u043d\u043e: B.", 66, -4487428));
        this.items.add(new KeybindItem(MusicControlKeybinds.syncEarlierKey, "\u23ea", "\u0420\u0430\u043d\u044c\u0448\u0435 \u043d\u0430 0.5\u0441", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b \u0447\u0443\u0442\u044c \u0440\u0430\u043d\u044c\u0448\u0435 \u0437\u0432\u0443\u043a\u0430.", "\u0423\u0434\u043e\u0431\u043d\u043e: [ (\u043b\u0435\u0432\u0430\u044f \u0441\u043a\u043e\u0431\u043a\u0430).", 91, -16718337));
        this.items.add(new KeybindItem(MusicControlKeybinds.syncLaterKey, "\u23e9", "\u041f\u043e\u0437\u0436\u0435 \u043d\u0430 0.5\u0441", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b \u0447\u0443\u0442\u044c \u043f\u043e\u0437\u0436\u0435 (\u0434\u043b\u044f Bluetooth).", "\u0423\u0434\u043e\u0431\u043d\u043e: ] (\u043f\u0440\u0430\u0432\u0430\u044f \u0441\u043a\u043e\u0431\u043a\u0430).", 93, -16718337));
        this.items.add(new KeybindItem(MusicControlKeybinds.toggleFavoriteKey, "\u2b50", "\u0412 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435", "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0442\u0435\u043a\u0443\u0449\u0438\u0439 \u0442\u0440\u0435\u043a \u0432 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435.", "\u0423\u0434\u043e\u0431\u043d\u043e: K.", 75, -16121));
        this.items.add(new KeybindItem(MusicControlKeybinds.openFavoritesKey, "\ud83d\udcc1", "\u0411\u0438\u0431\u043b\u0438\u043e\u0442\u0435\u043a\u0430", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0445 \u0442\u0440\u0435\u043a\u043e\u0432.", "\u0423\u0434\u043e\u0431\u043d\u043e: J.", 74, -49023));
        this.items.add(new KeybindItem(MusicControlKeybinds.localMusicKey, "\ud83c\udfb5", "\u041c\u043e\u044f \u043c\u0443\u0437\u044b\u043a\u0430", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0432\u0441\u0442\u0440\u043e\u0435\u043d\u043d\u044b\u0439 \u043f\u043b\u0435\u0435\u0440 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0445 \u0442\u0440\u0435\u043a\u043e\u0432.", "\u0423\u0434\u043e\u0431\u043d\u043e: M.", 77, -16718337));
    }

    protected void method_25426() {
        super.method_25426();
        this.playSound(class_3417.field_26980, 1.15f, 0.85f);
        this.playSound((class_6880<class_3414>)class_3417.field_14725, 1.2f, 0.5f);
        for (int i = 0; i < 35; ++i) {
            this.spawnAmbientParticle(true);
        }
    }

    private void spawnAmbientParticle(boolean randomY) {
        String[] symbols = new String[]{"\u266a", "\u266b", "\u2726", "\u2605", "\u2727", "\u25cf"};
        String sym = symbols[this.random.nextInt(symbols.length)];
        float x = this.random.nextFloat() * (float)this.field_22789;
        float y = randomY ? this.random.nextFloat() * (float)this.field_22790 : (float)(this.field_22790 + 10);
        float vy = -0.35f - this.random.nextFloat() * 0.65f;
        int[] colors = new int[]{58879, 0xFFCC00, 65416, 0xAA77FF, 0xFFFFFF};
        int col = colors[this.random.nextInt(colors.length)];
        this.particles.add(new KeyParticle(x, y, 0.0f, vy, col, sym, 120 + this.random.nextInt(80), true));
    }

    private void spawnBurstParticles(float originX, float originY, int color) {
        this.shockwaves.add(new KeyShockwave(originX, originY, 40.0f, color, 18));
        for (int i = 0; i < 45; ++i) {
            float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
            float speed = 1.5f + this.random.nextFloat() * 4.5f;
            float vx = (float)Math.cos(angle) * speed;
            float vy = (float)Math.sin(angle) * speed - 0.8f;
            this.particles.add(new KeyParticle(originX, originY, vx, vy, color, null, 30 + this.random.nextInt(25), false));
        }
    }

    private void playSound(class_3414 sound, float pitch, float volume) {
        try {
            class_310.method_1551().method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)sound, (float)pitch, (float)volume));
        }
        catch (Exception exception) {

        }
    }

    private void playSound(class_6880<class_3414> sound, float pitch, float volume) {
        if (sound != null && sound.comp_349() != null) {
            this.playSound((class_3414)sound.comp_349(), pitch, volume);
        }
    }

    public boolean method_25404(class_11908 input) {
        if (this.selectedBinding != null) {
            int key = input.comp_4795();
            if (key == 256) {
                this.bindKey(this.selectedBinding, class_3675.field_16237);
                this.selectedBinding = null;
                this.playSound((class_6880<class_3414>)class_3417.field_14624, 0.8f, 0.8f);
                return true;
            }

            class_3675.class_306 boundKey = class_3675.method_15985((class_11908)input);
            this.bindKey(this.selectedBinding, boundKey);
            this.selectedBinding = null;
            this.playSound(class_3417.field_14709, 1.6f, 0.7f);
            this.playSound((class_6880<class_3414>)class_3417.field_14622, 2.0f, 0.9f);
            return true;
        }
        if (input.comp_4795() == 256) {
            this.finishAndExit();
            return true;
        }
        return super.method_25404(input);
    }

    private void bindKey(class_304 binding, class_3675.class_306 key) {
        binding.method_1422(key);
        class_304.method_1426();
        if (this.field_22787 != null) {
            this.field_22787.field_1690.method_1640();
        }
    }

    private void applyRecommendedKeys() {
        for (KeybindItem item : this.items) {
            class_3675.class_306 key = item.defaultKeyCode != -1 ? class_3675.class_307.field_1668.method_1447(item.defaultKeyCode) : class_3675.field_16237;
            this.bindKey(item.binding, key);
        }
        this.playSound(class_3417.field_15195, 1.2f, 0.9f);
        this.playSound(class_3417.field_19167, 1.1f, 0.8f);
        this.spawnBurstParticles((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f, 58879);
        this.spawnBurstParticles((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f, -13312);
    }

    private void clearAllKeys() {
        for (KeybindItem item : this.items) {
            this.bindKey(item.binding, class_3675.field_16237);
        }
        this.playSound((class_6880<class_3414>)class_3417.field_14624, 0.7f, 0.9f);
        this.spawnBurstParticles((float)this.field_22789 / 2.0f, (float)this.field_22790 / 2.0f, -44462);
    }

    private void finishAndExit() {
        this.playSound(class_3417.field_14931, 1.8f, 0.5f);
        this.playSound(class_3417.field_14706, 1.1f, 0.7f);
        if (this.field_22787 != null) {
            if (!HudConfig.INSTANCE.hasSeenTutorial) {
                HudConfig.INSTANCE.hasSeenTutorial = true;
                HudConfig.save();
                this.field_22787.method_1507((class_437)new ModTutorialScreen(this.parent));
                return;
            }
            if (this.parent != null) {
                this.field_22787.method_1507(this.parent);
            } else if (this.field_22787.field_1687 != null) {
                this.field_22787.method_1507(null);
            } else {
                this.field_22787.method_1507((class_437)new class_442());
            }
        }
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        int centerX = this.field_22789 / 2;
        int colW = Math.min(270, (this.field_22789 - 40) / 2);
        int perCol = (this.items.size() + 1) / 2;
        int rowH = this.field_22790 < 300 ? 19 : (this.field_22790 < 350 ? 23 : 24);
        int rowGap = this.field_22790 < 300 ? 2 : (this.field_22790 < 350 ? 3 : 4);
        int startY = this.field_22790 < 300 ? 52 : (this.field_22790 < 350 ? 58 : 72);
        for (int i = 0; i < this.items.size(); ++i) {
            KeybindItem item = this.items.get(i);
            boolean col = i >= perCol;
            int row = i < perCol ? i : i - perCol;
            int rowX = !col ? centerX - colW - 6 : centerX + 6;
            int rowY = startY + row * (rowH + rowGap);
            int btnW = 84;
            int btnH = 20;
            int btnX = rowX + colW - btnW - 4;
            int btnY = rowY + (rowH - btnH) / 2;
            if (mx < btnX || mx > btnX + btnW || my < btnY || my > btnY + btnH) continue;
            if (this.selectedBinding == item.binding) {
                this.selectedBinding = null;
                this.playSound((class_6880<class_3414>)class_3417.field_14624, 1.0f, 0.6f);
            } else {
                this.selectedBinding = item.binding;
                this.playSound((class_6880<class_3414>)class_3417.field_14793, 1.4f, 0.8f);
                this.spawnBurstParticles((float)btnX + (float)btnW / 2.0f, (float)btnY + (float)btnH / 2.0f, 58879);
            }
            return true;
        }
        int footerY = this.field_22790 - 32;

        int totalBtnW = Math.min(480, this.field_22789 - 16);
        int btnRecW = (int)((float)totalBtnW * 0.42f);
        int btnClearW = (int)((float)totalBtnW * 0.23f);
        int btnDoneW = totalBtnW - btnRecW - btnClearW - 12;
        int btnRecX = centerX - totalBtnW / 2;
        int btnClearX = btnRecX + btnRecW + 6;
        int btnDoneX = btnClearX + btnClearW + 6;

        if (mx >= btnRecX && mx <= btnRecX + btnRecW && my >= footerY && my <= footerY + 22) {
            this.applyRecommendedKeys();
            return true;
        }
        if (mx >= btnClearX && mx <= btnClearX + btnClearW && my >= footerY && my <= footerY + 22) {
            this.clearAllKeys();
            return true;
        }
        if (mx >= btnDoneX && mx <= btnDoneX + btnDoneW && my >= footerY && my <= footerY + 22) {
            this.finishAndExit();
            return true;
        }
        return super.method_25402(click, bl);
    }

    private void renderParticles(class_332 context) {
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            KeyParticle p = this.particles.get(i);
            p.update();
            if (p.isDead()) {
                this.particles.remove(i);
                continue;
            }
            int alphaInt = (int)(p.getAlpha() * 255.0f);
            if (alphaInt <= 2) continue;
            int col = alphaInt << 24 | p.color & 0xFFFFFF;
            if (p.symbol != null) {
                context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, col, false);
                continue;
            }
            context.method_25294((int)p.x, (int)p.y, (int)(p.x + 2.0f), (int)(p.y + 2.0f), col);
        }
    }

    private void renderShockwaves(class_332 context) {
        for (int i = this.shockwaves.size() - 1; i >= 0; --i) {
            KeyShockwave s = this.shockwaves.get(i);
            s.update();
            if (s.isDead()) {
                this.shockwaves.remove(i);
                continue;
            }
            int alphaInt = (int)(s.getAlpha() * 200.0f);
            if (alphaInt <= 2) continue;
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

    private void renderAudioVisualizer(class_332 context, int centerX, int baseY, boolean isPlaying, float beatEnergy, long nowMs) {
        int specBars = 26;
        int specBarW = 5;
        int specGap = 4;
        int specTotalW = specBars * (specBarW + specGap) - specGap;
        int specStartX = centerX - specTotalW / 2;
        for (int b = 0; b < specBars; ++b) {
            float wave = (float)Math.sin((double)nowMs * 0.006 + (double)b * 0.44);
            float heightFactor = (isPlaying ? beatEnergy * 0.65f + (wave + 1.0f) * 0.35f : (wave + 1.0f) * 0.5f) * (1.0f - Math.abs((float)b - (float)specBars / 2.0f) / ((float)specBars / 1.5f));
            int barH = (int)Math.max(2.0f, heightFactor * 16.0f);
            int rgb = b < 6 ? 58879 : (b < 20 ? 0xFFCC00 : 65416);
            int barColor = 0x2A000000 | rgb;
            int bx = specStartX + b * (specBarW + specGap);
            context.method_25294(bx, baseY - barH, bx + specBarW, baseY, barColor);
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        List<String> recLines;
        List<String> descLines;
        Object headerBadge;
        context.method_25296(0, 0, this.field_22789, this.field_22790, -100070892, -50068215);
        context.method_25296(0, 0, 150, 100, 352380415, 0);
        context.method_25296(this.field_22789 - 150, 0, this.field_22789, 100, 369085440, 0);
        int headerY = this.field_22790 < 350 ? 6 : 10;
        int headerH = this.field_22790 < 350 ? 42 : 48;
        int headerW = Math.min(520, this.field_22789 - 16);
        int headerX = this.field_22789 / 2 - headerW / 2;
        int cornerR = Math.min(24, headerH / 2);
        MusicHudRenderer.renderRoundedRect(context, headerX, headerY, headerW, headerH, cornerR, 402712063);
        MusicHudRenderer.renderRoundedBorder(context, headerX, headerY, headerW, headerH, cornerR, 855696895);
        this.renderParticles(context);
        this.renderShockwaves(context);
        if (this.random.nextFloat() < 0.25f) {
            this.spawnAmbientParticle(false);
        }
        class_327 tr = this.field_22793;
        int centerX = this.field_22789 / 2;
        TrackState trackState = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : TrackState.INACTIVE;
        boolean isPlaying = trackState.isActive() && !"stopped".equalsIgnoreCase(trackState.status()) && !"paused".equalsIgnoreCase(trackState.status());
        long nowMs = System.currentTimeMillis();
        float beatCycle = isPlaying ? (float)((double)(trackState.getInterpolatedPositionMs() % 500L) / 500.0) : (float)((double)(nowMs % 600L) / 600.0);
        float beatEnergy = isPlaying ? (float)Math.pow(Math.max(0.0, 1.0 - (double)beatCycle * 1.8), 2.2) : (float)Math.sin((double)nowMs * 0.005) * 0.25f + 0.25f;
        float iconBounce = isPlaying ? -beatEnergy * 3.0f : (float)Math.sin((double)nowMs * 0.004) * 1.2f;
        int titleY = this.field_22790 < 350 ? 8 : 12;
        int subTitleY = this.field_22790 < 350 ? 19 : 24;
        int helpY = this.field_22790 < 350 ? 30 : 38;
        int headerTitleW = tr.method_1727("MUSIC SUBTITLES");
        context.method_51433(tr, "\u266a", centerX - headerTitleW / 2 - 14, (int)((float)titleY + iconBounce), -16718337, true);
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)"\u00a7bMUSIC SUBTITLES"), centerX, titleY, -1);
        context.method_51433(tr, "\u266b", centerX + headerTitleW / 2 + 6, (int)((float)titleY - iconBounce), -13312, true);
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)"\u00a7f\u00a7l\u0413\u043e\u0440\u044f\u0447\u0438\u0435 \u043a\u043b\u0430\u0432\u0438\u0448\u0438"), centerX, subTitleY, -1);
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)"\u00a77\u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u043a\u043b\u0430\u0432\u0438\u0448\u0435 \u0434\u043b\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f"), centerX, helpY, -5592406);
        this.renderAudioVisualizer(context, centerX, this.field_22790 - 36, isPlaying, beatEnergy, nowMs);
        int colW = Math.min(270, (this.field_22789 - 40) / 2);
        int perCol = (this.items.size() + 1) / 2;
        int rowH = this.field_22790 < 300 ? 19 : (this.field_22790 < 350 ? 23 : 24);
        int rowGap = this.field_22790 < 300 ? 2 : (this.field_22790 < 350 ? 3 : 4);
        int startY = this.field_22790 < 300 ? 52 : (this.field_22790 < 350 ? 58 : 72);
        this.hoveredIndex = -1;
        for (int i = 0; i < this.items.size(); ++i) {
            int keycapBorder;
            int keycapBg;
            int keyColor;
            Object keyText;
            boolean hoverBtn;
            int cardBg;
            boolean isSelected;
            boolean isHovered;
            KeybindItem item = this.items.get(i);
            boolean col = i >= perCol;
            int row = i < perCol ? i : i - perCol;
            int rowX = !col ? centerX - colW - 6 : centerX + 6;
            int rowY = startY + row * (rowH + rowGap);
            boolean bl = isHovered = mouseX >= rowX && mouseX <= rowX + colW && mouseY >= rowY && mouseY <= rowY + rowH;
            if (isHovered) {
                this.hoveredIndex = i;
            }
            boolean bl2 = isSelected = this.selectedBinding == item.binding;
            cardBg = isSelected ? -300016336 : (isHovered ? -585622740 : -1441787362);
            int cardBorder = isSelected ? -16718337 : (isHovered ? -2013207041 : 0x2EFFFFFF);
            MusicHudRenderer.renderRoundedRect(context, rowX, rowY, colW, rowH, 6, cardBg);
            MusicHudRenderer.renderRoundedBorder(context, rowX, rowY, colW, rowH, 6, cardBorder, cardBorder);
            int iconSize = 20;
            int iconY = rowY + (rowH - iconSize) / 2;
            int iconTintBg = 0x22000000 | item.iconColor & 0xFFFFFF;
            MusicHudRenderer.renderRoundedRect(context, rowX + 4, iconY, iconSize, iconSize, 4, iconTintBg);
            MusicHudRenderer.renderRoundedBorder(context, rowX + 4, iconY, iconSize, iconSize, 4, 0x44000000 | item.iconColor & 0xFFFFFF);
            context.method_27534(tr, (class_2561)class_2561.method_43470((String)item.icon), rowX + 14, iconY + 6, item.iconColor);
            int textY = rowY + (rowH - 9) / 2;
            context.method_51433(tr, item.name, rowX + 28, textY, isHovered ? -1 : -2236963, true);
            int btnW = 84;
            int btnH = 20;
            int btnX = rowX + colW - btnW - 4;
            int btnY = rowY + (rowH - btnH) / 2;
            boolean bl3 = hoverBtn = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            if (isSelected) {
                float pulse = (float)Math.sin((double)nowMs * 0.009) * 0.5f + 0.5f;
                int glowAlpha = (int)(pulse * 120.0f + 40.0f);
                MusicHudRenderer.renderRoundedRect(context, btnX - 3, btnY - 3, btnW + 6, btnH + 6, 7, glowAlpha << 24 | 0xE5FF);
                MusicHudRenderer.renderRoundedRect(context, btnX - 1, btnY - 1, btnW + 2, btnH + 2, 5, -1426076672);
                if (this.random.nextFloat() < 0.35f) {
                    float px = (float)btnX + this.random.nextFloat() * (float)btnW;
                    float py = (float)btnY + this.random.nextFloat() * (float)btnH;
                    this.particles.add(new KeyParticle(px, py, (this.random.nextFloat() - 0.5f) * 0.6f, -0.6f - this.random.nextFloat() * 0.8f, -13312, "\u2727", 20 + this.random.nextInt(15), false));
                }
                keyText = "\u00a7e\u25cf \u041d\u0430\u0436\u043c\u0438\u0442\u0435...";
                keyColor = -8960;
                keycapBg = -298638848;
                keycapBorder = -16718337;
            } else {
                String rawName = item.binding.method_16007().getString();
                boolean isUnbound = rawName.equalsIgnoreCase("none") || rawName.equalsIgnoreCase("\u043d\u0435 \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0430");
                if (isUnbound) {
                    keyText = "\u00a78[-]";
                    keyColor = -8947832;
                } else {
                    keyText = "\u00a7f" + rawName;
                    keyColor = -1;
                }
                keycapBg = hoverBtn ? -584306620 : -1155653070;
                keycapBorder = hoverBtn ? -872356353 : 0x44FFFFFF;
            }
            MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 4, keycapBg);
            MusicHudRenderer.renderRoundedBorder(context, btnX, btnY, btnW, btnH, 4, keycapBorder, keycapBorder);
            context.method_25294(btnX + 3, btnY, btnX + btnW - 3, btnY + 1, 0x55FFFFFF);
            context.method_25294(btnX + 3, btnY + btnH - 1, btnX + btnW - 3, btnY + btnH, 0x44000000);
            context.method_27534(tr, (class_2561)class_2561.method_43470((String)keyText), btnX + btnW / 2, btnY + 6, keyColor);
        }
        int footerY = this.field_22790 - 32;
        int infoCardW = Math.min(this.field_22789 - 24, Math.max(colW * 2 + 12, 440));
        int textMaxW = infoCardW - 20;
        int infoCardX = centerX - infoCardW / 2;
        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.items.size()) {
            KeybindItem hovered = this.items.get(this.hoveredIndex);
            String boundKey = hovered.binding.method_16007().getString();
            headerBadge = "\u00a7b\u2726 " + hovered.name + " \u00a77(\u041a\u043b\u0430\u0432\u0438\u0448\u0430: \u00a7e" + boundKey + "\u00a77)";
            descLines = this.wrapText("\u00a7e\ud83d\udca1 \u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435: \u00a7f" + hovered.description, textMaxW);
            recLines = this.wrapText("\u00a7a\u26a1 \u0421\u043e\u0432\u0435\u0442: \u00a77" + hovered.recommendation, textMaxW);
        } else {
            headerBadge = "\u00a7b\u2139 \u041f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0430:";
            descLines = this.wrapText("\u00a7f\u2022 \u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u043a\u043d\u043e\u043f\u043a\u0435 \u0438 \u043d\u0430\u0436\u043c\u0438\u0442\u0435 \u0436\u0435\u043b\u0430\u0435\u043c\u0443\u044e \u043a\u043b\u0430\u0432\u0438\u0448\u0443.", textMaxW);
            recLines = this.wrapText("\u00a77\u2022 \u041d\u0430\u0436\u043c\u0438\u0442\u0435 ESC \u0432\u043e \u0432\u0440\u0435\u043c\u044f \u0432\u044b\u0431\u043e\u0440\u0430, \u0447\u0442\u043e\u0431\u044b \u0441\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0443.", textMaxW);
        }
        int totalContentLines = 1 + descLines.size() + recLines.size();
        int cardsBottomY = startY + perCol * (rowH + rowGap);
        int maxInfoH = Math.max(26, footerY - cardsBottomY - 6);
        int infoCardH = Math.min(maxInfoH, Math.max(48, 14 + totalContentLines * 11 + 4));
        int infoCardY = Math.min(footerY - infoCardH - 4, cardsBottomY + 4);
        if (infoCardY < cardsBottomY + 2) {
            infoCardY = cardsBottomY + 2;
        }
        MusicHudRenderer.renderRoundedRect(context, infoCardX, infoCardY, infoCardW, infoCardH, 8, -267250654);
        MusicHudRenderer.renderRoundedBorder(context, infoCardX, infoCardY, infoCardW, infoCardH, 8, 0x33FFFFFF);
        context.method_25294(infoCardX + 10, infoCardY, infoCardX + infoCardW - 10, infoCardY + 1, 0x44FFFFFF);

        int curY = infoCardY + 6;
        context.method_51433(tr, (String)headerBadge, infoCardX + 10, curY, -1, false);
        curY += 13;
        for (String dl : descLines) {
            if (curY + 9 > infoCardY + infoCardH - 2) break;
            context.method_51433(tr, dl, infoCardX + 10, curY, -1, false);
            curY += 11;
        }
        for (String rl : recLines) {
            if (curY + 9 > infoCardY + infoCardH - 2) break;
            context.method_51433(tr, rl, infoCardX + 10, curY, -1, false);
            curY += 11;
        }
        int totalBtnW = Math.min(480, this.field_22789 - 16);
        int btnRecW = (int)((float)totalBtnW * 0.42f);
        int btnClearW = (int)((float)totalBtnW * 0.23f);
        int btnDoneW = totalBtnW - btnRecW - btnClearW - 12;
        int btnRecX = centerX - totalBtnW / 2;
        int btnClearX = btnRecX + btnRecW + 6;
        int btnDoneX = btnClearX + btnClearW + 6;

        boolean hoverRec = mouseX >= btnRecX && mouseX <= btnRecX + btnRecW && mouseY >= footerY && mouseY <= footerY + 22;
        MusicHudRenderer.renderRoundedRect(context, btnRecX, footerY, btnRecW, 22, 5, hoverRec ? -587171073 : -1442818629);
        MusicHudRenderer.renderRoundedBorder(context, btnRecX, footerY, btnRecW, 22, 5, hoverRec ? -16718337 : 0x44FFFFFF, hoverRec ? -16718337 : 0x22FFFFFF);
        String recBtnText = btnRecW < 155 ? "\u26a1 \u041f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e" : "\u26a1 \u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u043e\u0432\u0430\u043d\u043d\u044b\u0439 \u0441\u0435\u0442\u0430\u043f";
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)recBtnText), btnRecX + btnRecW / 2, footerY + 7, -1);

        boolean hoverClear = mouseX >= btnClearX && mouseX <= btnClearX + btnClearW && mouseY >= footerY && mouseY <= footerY + 22;
        MusicHudRenderer.renderRoundedRect(context, btnClearX, footerY, btnClearW, 22, 5, hoverClear ? -582737374 : -2010507240);
        MusicHudRenderer.renderRoundedBorder(context, btnClearX, footerY, btnClearW, 22, 5, hoverClear ? -44462 : 0x44FF4444, hoverClear ? -44462 : 0x22FF4444);
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)"\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u0432\u0441\u0451"), btnClearX + btnClearW / 2, footerY + 7, hoverClear ? -1 : -21846);

        boolean hoverDone = mouseX >= btnDoneX && mouseX <= btnDoneX + btnDoneW && mouseY >= footerY && mouseY <= footerY + 22;
        MusicHudRenderer.renderRoundedRect(context, btnDoneX, footerY, btnDoneW, 22, 5, hoverDone ? -587155308 : -1442806424);
        MusicHudRenderer.renderRoundedBorder(context, btnDoneX, footerY, btnDoneW, 22, 5, hoverDone ? -16711732 : 0x6600FFCC, hoverDone ? -16711732 : 0x3300FFCC);
        int arrowMove = hoverDone ? (int)(Math.sin((double)nowMs * 0.01) * 2.0) : 0;
        String doneLabel = !HudConfig.INSTANCE.hasSeenTutorial ? "\u0414\u0430\u043b\u0435\u0435: \u041e\u0431\u0443\u0447\u0435\u043d\u0438\u0435 " : "\u0413\u043e\u0442\u043e\u0432\u043e / \u0412 \u0438\u0433\u0440\u0443 ";
        context.method_27534(tr, (class_2561)class_2561.method_43470((String)(doneLabel + (arrowMove > 0 ? " \u2794" : "\u2794"))), btnDoneX + btnDoneW / 2 + arrowMove, footerY + 7, -1);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    private List<String> wrapText(String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (this.field_22793.method_1727(String.valueOf(current) + " " + word) > maxWidth) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                    continue;
                }
                lines.add(word);
                current = new StringBuilder();
                continue;
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(word);
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    public boolean method_25421() {
        return true;
    }

    public static class KeybindItem {
        public final class_304 binding;
        public final String icon;
        public final String name;
        public final String description;
        public final String recommendation;
        public final int defaultKeyCode;
        public final int iconColor;

        public KeybindItem(class_304 binding, String icon, String name, String description, String recommendation, int defaultKeyCode, int iconColor) {
            this.binding = binding;
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.recommendation = recommendation;
            this.defaultKeyCode = defaultKeyCode;
            this.iconColor = iconColor;
        }
    }

    private static class KeyParticle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        String symbol;
        int maxAge;
        int age = 0;
        boolean isAmbient;

        KeyParticle(float x, float y, float vx, float vy, int color, String symbol, int maxAge, boolean isAmbient) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.symbol = symbol;
            this.maxAge = maxAge;
            this.isAmbient = isAmbient;
        }

        void update() {
            this.x += this.vx;
            this.y += this.vy;
            if (this.isAmbient) {
                this.x += (float)Math.sin((double)this.age * 0.08) * 0.35f;
            }
            ++this.age;
        }

        boolean isDead() {
            return this.age >= this.maxAge;
        }

        float getAlpha() {
            float progress = (float)this.age / (float)this.maxAge;
            if (progress < 0.15f) {
                return progress / 0.15f;
            }
            return Math.max(0.0f, 1.0f - (progress - 0.15f) / 0.85f);
        }
    }

    private static class KeyShockwave {
        float cx;
        float cy;
        float radius = 2.0f;
        float maxRadius;
        int color;
        int age = 0;
        int maxAge;

        KeyShockwave(float cx, float cy, float maxRadius, int color, int maxAge) {
            this.cx = cx;
            this.cy = cy;
            this.maxRadius = maxRadius;
            this.color = color;
            this.maxAge = maxAge;
        }

        void update() {
            ++this.age;
            float t = (float)this.age / (float)this.maxAge;
            this.radius = 2.0f + (this.maxRadius - 2.0f) * (float)Math.sin((double)t * Math.PI / 2.0);
        }

        boolean isDead() {
            return this.age >= this.maxAge;
        }

        float getAlpha() {
            return Math.max(0.0f, 1.0f - (float)this.age / (float)this.maxAge);
        }
    }
}

