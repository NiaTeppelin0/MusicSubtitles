package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.favorites.FavoritesManager;
import com.example.yandexmusichud.hud.FavoritesScreen;
import com.example.yandexmusichud.hud.HudEditScreen;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.keybinds.MusicControlKeybinds;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_10185;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_241;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import net.minecraft.class_6880;
import net.minecraft.class_744;
import org.lwjgl.glfw.GLFW;

public class RadialMenuScreen extends class_437 {
    private static final int SECTOR_COUNT = 8;
    private static final float STEP_ANGLE = 0.7853982f;

    private static final class_2960 TEX_RING = class_2960.method_60655("yandexmusichud", "textures/gui/radial/radial_ring.png");
    private static final class_2960 TEX_VINYL = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_record.png");
    private static final class_2960 TEX_SPINDLE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_spindle.png");
    private static final class_2960 TEX_SELECTOR = class_2960.method_60655("yandexmusichud", "textures/gui/radial/radial_selector.png");
    private static final class_2960 TEX_POD_NORMAL = class_2960.method_60655("yandexmusichud", "textures/gui/radial/pod_normal.png");
    private static final class_2960 TEX_POD_HOVER = class_2960.method_60655("yandexmusichud", "textures/gui/radial/pod_hover.png");
    private static final class_2960 TEX_AMBIENT_GLOW = class_2960.method_60655("yandexmusichud", "textures/gui/radial/wheel_ambient_glow.png");

    private static final class_2960 ICON_PLAY = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_play.png");
    private static final class_2960 ICON_PAUSE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_pause.png");
    private static final class_2960 ICON_NEXT = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_next.png");
    private static final class_2960 ICON_PREV = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_prev.png");
    private static final class_2960 ICON_FAVORITE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_favorite.png");
    private static final class_2960 ICON_LIBRARY = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_library.png");
    private static final class_2960 ICON_SETTINGS = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_settings.png");
    private static final class_2960 ICON_HUD = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_hud.png");
    private static final class_2960 ICON_SUBTITLES = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_subtitles.png");

    private final NowPlayingClient musicClient;
    private final CoverTextureManager coverManager;
    private int selectedIndex = 0;
    private float currentAngle = 0.0f;
    private float targetAngle = 0.0f;
    private long openTimestamp = -1L;
    private boolean isExiting = false;
    private long exitTimestamp = -1L;
    private int actionToExecuteOnExit = -1;
    private final float[] podPops = new float[8];
    private long lastRadialRenderTime = 0L;
    private final List<DialItem> items = new ArrayList<DialItem>();

    public RadialMenuScreen(NowPlayingClient musicClient, CoverTextureManager coverManager) {
        super((class_2561)class_2561.method_43470((String)"Music Subtitles Radial Menu"));
        this.musicClient = musicClient;
        this.coverManager = coverManager;
        this.initItems();
        this.selectedIndex = class_3532.method_15340((int)HudConfig.INSTANCE.radialLastSelectedIndex, (int)0, (int)7);
        this.currentAngle = this.targetAngle = (float)this.selectedIndex * STEP_ANGLE;
    }

    private void initItems() {
        this.items.clear();
        this.items.add(new DialItem(0, ICON_PLAY, "\u041f\u043b\u0435\u0439 / \u041f\u0430\u0443\u0437\u0430", "\u0412\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u0435 \u0438 \u043f\u0430\u0443\u0437\u0430", 0x00FF88));
        this.items.add(new DialItem(1, ICON_NEXT, "\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439", "\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439 \u0442\u0440\u0435\u043a \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u0438", 0x00E5FF));
        this.items.add(new DialItem(2, ICON_FAVORITE, "\u0412 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0442\u0435\u043a\u0443\u0449\u0438\u0439 \u0442\u0440\u0435\u043a", 0xFFCC00));
        this.items.add(new DialItem(3, ICON_LIBRARY, "\u0411\u0438\u0431\u043b\u0438\u043e\u0442\u0435\u043a\u0430", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435 \u0442\u0440\u0435\u043a\u0438", 0x38BDF8));
        this.items.add(new DialItem(4, ICON_SETTINGS, "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043c\u0435\u043d\u044e \u043a\u043e\u043d\u0444\u0438\u0433\u0443\u0440\u0430\u0446\u0438\u0438", 0x00E5FF));
        this.items.add(new DialItem(5, ICON_HUD, "\u0412\u0438\u0434\u0436\u0435\u0442 HUD", "\u0421\u043a\u0440\u044b\u0442\u044c \u0438\u043b\u0438 \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u043a\u0430\u0440\u0442\u043e\u0447\u043a\u0443", 0x00FF88));
        this.items.add(new DialItem(6, ICON_SUBTITLES, "\u0421\u0443\u0431\u0442\u0438\u0442\u0440\u044b", "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0440\u0435\u0436\u0438\u043c \u0442\u0435\u043a\u0441\u0442\u0430", 0x60A5FA));
        this.items.add(new DialItem(7, ICON_PREV, "\u041f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0439", "\u041f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0439 \u0442\u0440\u0435\u043a \u0432 \u043e\u0447\u0435\u0440\u0435\u0434\u0438", 0x00E5FF));
    }

    protected void method_25426() {
        super.method_25426();
        if (this.openTimestamp <= 0L) {
            this.openTimestamp = System.currentTimeMillis();
            this.playSound((class_6880<class_3414>)class_3417.field_14793, 1.4f, 0.75f);
            this.playSound((class_3414)class_3417.field_15015.comp_349(), 1.6f, 0.6f);
        }
    }

    public void method_25393() {
        super.method_25393();
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null || mc.field_1724.field_3913 == null) {
            return;
        }
        long windowHandle = mc.method_22683().method_4490();
        boolean forward = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1894);
        boolean back = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1881);
        boolean left = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1913);
        boolean right = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1849);
        boolean jump = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1903);
        boolean sneak = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1832);
        boolean sprint = RadialMenuScreen.isKeyDown(windowHandle, mc.field_1690.field_1867);
        mc.field_1724.field_3913.field_54155 = new class_10185(forward, back, left, right, jump, sneak, sprint);
        float fwd = 0.0f;
        if (forward) {
            fwd += 1.0f;
        }
        if (back) {
            fwd -= 1.0f;
        }
        float strafe = 0.0f;
        if (left) {
            strafe += 1.0f;
        }
        if (right) {
            strafe -= 1.0f;
        }
        class_241 raw = new class_241(strafe, fwd);
        try {
            Field mvField = class_744.class.getDeclaredField("movementVector");
            mvField.setAccessible(true);
            mvField.set(mc.field_1724.field_3913, raw.method_35581());
        }
        catch (Exception ignored) {
            try {
                for (Field f : class_744.class.getDeclaredFields()) {
                    if (f.getType() != class_241.class) continue;
                    f.setAccessible(true);
                    f.set(mc.field_1724.field_3913, raw.method_35581());
                }
            }
            catch (Exception exception) {
            }
        }
    }

    private static boolean isKeyDown(long windowHandle, class_304 binding) {
        class_3675.class_306 key = class_3675.method_15981((String)binding.method_1428());
        if (key.method_1442() == class_3675.class_307.field_1672) {
            return GLFW.glfwGetMouseButton((long)windowHandle, (int)key.method_1444()) == 1;
        }
        if (key.method_1444() > 0) {
            return GLFW.glfwGetKey((long)windowHandle, (int)key.method_1444()) == 1;
        }
        return false;
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isExiting) {
            return true;
        }
        if (verticalAmount > 0.05) {
            this.selectedIndex = (this.selectedIndex + 7) % 8;
            this.targetAngle -= STEP_ANGLE;
            HudConfig.INSTANCE.radialLastSelectedIndex = this.selectedIndex;
            this.playSound((class_6880<class_3414>)class_3417.field_15204, 1.45f + (float)(this.selectedIndex % 4) * 0.08f, 0.85f);
            return true;
        }
        if (verticalAmount < -0.05) {
            this.selectedIndex = (this.selectedIndex + 1) % 8;
            this.targetAngle += STEP_ANGLE;
            HudConfig.INSTANCE.radialLastSelectedIndex = this.selectedIndex;
            this.playSound((class_6880<class_3414>)class_3417.field_15204, 1.2f + (float)(this.selectedIndex % 4) * 0.08f, 0.85f);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256) {
            this.triggerExit(-1);
            return true;
        }
        class_310 mc = class_310.method_1551();
        if (mc != null) {
            int key = input.comp_4795();
            class_3675.class_306 fwd = class_3675.method_15981((String)mc.field_1690.field_1894.method_1428());
            class_3675.class_306 bck = class_3675.method_15981((String)mc.field_1690.field_1881.method_1428());
            class_3675.class_306 lft = class_3675.method_15981((String)mc.field_1690.field_1913.method_1428());
            class_3675.class_306 rgt = class_3675.method_15981((String)mc.field_1690.field_1849.method_1428());
            class_3675.class_306 jmp = class_3675.method_15981((String)mc.field_1690.field_1903.method_1428());
            class_3675.class_306 snk = class_3675.method_15981((String)mc.field_1690.field_1832.method_1428());
            class_3675.class_306 spr = class_3675.method_15981((String)mc.field_1690.field_1867.method_1428());
            if (key == fwd.method_1444() || key == bck.method_1444() || key == lft.method_1444() || key == rgt.method_1444() || key == jmp.method_1444() || key == snk.method_1444() || key == spr.method_1444()) {
                return false;
            }
        }
        return super.method_25404(input);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        if (this.isExiting) {
            return true;
        }
        long now = System.currentTimeMillis();
        float openAnim = class_3532.method_15363((float)((float)(now - this.openTimestamp) / 180.0f), (float)0.0f, (float)1.0f);
        float openEase = RadialMenuScreen.easeOutCubic(openAnim);
        float slideX = (1.0f - openEase) * 75.0f;
        float menuScale = Math.max(0.1f, 0.92f + 0.08f * openEase);
        float centerX = (float)this.field_22789 + slideX;
        float centerY = (float)this.field_22790 / 2.0f;
        float localX = centerX + ((float)click.comp_4798() - centerX) / menuScale;
        float localY = centerY + ((float)click.comp_4799() - centerY) / menuScale;
        float baseRadius = 100.0f;
        TrackState trackState = this.musicClient != null ? this.musicClient.getState() : null;
        boolean isPlaying = trackState != null && trackState.isActive() && trackState.isPlaying();

        for (int i = 0; i < 8; ++i) {
            float sectorAngle = (float)i * STEP_ANGLE;
            float phi = sectorAngle - this.currentAngle;
            while ((double)phi > Math.PI) {
                phi -= (float)Math.PI * 2;
            }
            while ((double)phi <= -Math.PI) {
                phi += (float)Math.PI * 2;
            }
            if (Math.cos(phi) < -0.15) {
                continue;
            }
            float r = baseRadius + this.podPops[i];
            float px = centerX - (float)Math.cos(phi) * r;
            float py = centerY - (float)Math.sin(phi) * r;
            int podSize = (i == this.selectedIndex) ? 34 : 26;
            float half = (float)podSize / 2.0f;
            String label = this.items.get(i).getDynamicLabel(trackState, isPlaying);
            int pillW = this.field_22793.method_1727(label) + 12;

            if (localX >= px - half - (float)pillW - 5.0f && localX <= px + half &&
                localY >= py - half && localY <= py + half) {
                this.selectedIndex = i;
                HudConfig.INSTANCE.radialLastSelectedIndex = i;
                this.triggerExit(i);
                return true;
            }
        }

        int tagW = 228;
        int tagH = 46;
        int tagX = (int)(centerX - baseRadius - (float)tagW - 26.0f);
        int tagY = (int)(centerY - (float)tagH / 2.0f);
        if (localX >= (float)tagX && localX <= (float)(tagX + tagW) &&
            localY >= (float)tagY && localY <= (float)(tagY + tagH)) {
            HudConfig.INSTANCE.radialLastSelectedIndex = this.selectedIndex;
            this.triggerExit(this.selectedIndex);
            return true;
        }

        return super.method_25402(click, bl);
    }

    private void checkKeyHeld() {
        if (this.isExiting) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.openTimestamp < 180L) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        long windowHandle = mc.method_22683().method_4490();
        class_3675.class_306 bound = class_3675.method_15981((String)MusicControlKeybinds.radialMenuKey.method_1428());
        boolean isPressed = false;
        if (bound.method_1442() == class_3675.class_307.field_1672) {
            isPressed = GLFW.glfwGetMouseButton((long)windowHandle, (int)bound.method_1444()) == 1;
        } else if (bound.method_1444() > 0) {
            isPressed = GLFW.glfwGetKey((long)windowHandle, (int)bound.method_1444()) == 1;
        }
        if (!isPressed) {
            this.triggerExit(this.selectedIndex);
        }
    }

    private void triggerExit(int actionIndex) {
        if (this.isExiting) {
            return;
        }
        this.isExiting = true;
        this.exitTimestamp = System.currentTimeMillis();
        this.actionToExecuteOnExit = actionIndex;
        if (actionIndex >= 0) {
            this.selectedIndex = actionIndex;
            HudConfig.INSTANCE.radialLastSelectedIndex = actionIndex;
            HudConfig.save();
            this.playSound((class_6880<class_3414>)class_3417.field_14622, 1.8f, 0.9f);
            this.playSound((class_3414)class_3417.field_15015.comp_349(), 1.4f, 0.7f);
        } else {
            HudConfig.INSTANCE.radialLastSelectedIndex = this.selectedIndex;
            HudConfig.save();
            this.playSound((class_6880<class_3414>)class_3417.field_14624, 0.8f, 0.8f);
        }
    }

    private void executeAction(int index) {
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        TrackState trackState = this.musicClient != null ? this.musicClient.getState() : null;
        switch (index) {
            case 0: {
                if (this.musicClient == null) break;
                this.musicClient.sendPlayPauseAsync();
                break;
            }
            case 1: {
                if (this.musicClient == null) break;
                this.musicClient.sendNextAsync();
                break;
            }
            case 2: {
                if (trackState == null || !trackState.isActive() || trackState.title().isEmpty()) break;
                boolean isFav = FavoritesManager.INSTANCE.toggleFavorite(trackState, this.coverManager);
                if (mc.field_1705 == null) break;
                String msg = isFav ? "\u00a7b[Music Subtitles] \u00a7e\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e \u0432 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435: \u00a7f" + trackState.artist() + " - " + trackState.title() : "\u00a7b[Music Subtitles] \u00a77\u0423\u0434\u0430\u043b\u0435\u043d\u043e \u0438\u0437 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0433\u043e: \u00a7f" + trackState.artist() + " - " + trackState.title();
                mc.field_1705.method_1758((class_2561)class_2561.method_43470((String)msg), false);
                break;
            }
            case 3: {
                mc.execute(() -> mc.method_1507((class_437)new FavoritesScreen(null)));
                break;
            }
            case 4: {
                mc.execute(() -> mc.method_1507((class_437)new HudEditScreen()));
                break;
            }
            case 5: {
                HudConfig.INSTANCE.enabled = !HudConfig.INSTANCE.enabled;
                HudConfig.save();
                if (mc.field_1705 == null) break;
                String status = HudConfig.INSTANCE.enabled ? "\u00a7a\u0412\u041a\u041b" : "\u00a7c\u0412\u042b\u041a\u041b";
                mc.field_1705.method_1758((class_2561)class_2561.method_43470((String)("\u00a7b[Music Subtitles] \u00a7f\u0412\u0438\u0434\u0436\u0435\u0442 HUD: " + status)), false);
                break;
            }
            case 6: {
                String string;
                if (!HudConfig.INSTANCE.subtitlesEnabled) {
                    HudConfig.INSTANCE.subtitlesEnabled = true;
                    HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
                } else {
                    switch (HudConfig.INSTANCE.subtitlesMode) {
                        case "WORLD_3D": {
                            HudConfig.INSTANCE.subtitlesMode = "WORLD_BLOCK";
                            break;
                        }
                        case "WORLD_BLOCK":
                        case "WORLD_SURFACE": {
                            HudConfig.INSTANCE.subtitlesMode = "HUD_BOTTOM";
                            break;
                        }
                        case "HUD_BOTTOM": {
                            HudConfig.INSTANCE.subtitlesMode = "SCREEN_RANDOM";
                            break;
                        }
                        case "SCREEN_RANDOM": {
                            HudConfig.INSTANCE.subtitlesMode = "CHAT";
                            break;
                        }
                        case "CHAT": {
                            HudConfig.INSTANCE.subtitlesEnabled = false;
                            HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
                            break;
                        }
                        default: {
                            HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
                        }
                    }
                }
                HudConfig.save();
                if (mc.field_1705 == null) break;
                if (!HudConfig.INSTANCE.subtitlesEnabled) {
                    string = "\u00a7c\u0412\u042b\u041a\u041b";
                } else {
                    switch (HudConfig.INSTANCE.subtitlesMode) {
                        case "WORLD_3D": {
                            string = "\u00a7a3D \u0432 \u043c\u0438\u0440\u0435";
                            break;
                        }
                        case "WORLD_BLOCK":
                        case "WORLD_SURFACE": {
                            string = "\u00a7b3D \u043d\u0430 \u0431\u043b\u043e\u043a\u0430\u0445";
                            break;
                        }
                        case "HUD_BOTTOM": {
                            string = "\u00a7e2D \u0441\u043d\u0438\u0437\u0443";
                            break;
                        }
                        case "CHAT": {
                            string = "\u00a76\u0412 \u0447\u0430\u0442\u0435";
                            break;
                        }
                        default: {
                            string = "\u00a7d2D \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u043e";
                        }
                    }
                }
                String modeName = string;
                mc.field_1705.method_1758((class_2561)class_2561.method_43470((String)("\u00a7b[Music Subtitles] \u00a7f\u0420\u0435\u0436\u0438\u043c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432: " + modeName)), false);
                break;
            }
            case 7: {
                if (this.musicClient == null) break;
                this.musicClient.sendPreviousAsync();
            }
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.checkKeyHeld();
        long now = System.currentTimeMillis();
        float dt = this.lastRadialRenderTime > 0L ? Math.min(0.08f, (float)(now - this.lastRadialRenderTime) / 1000.0f) : 0.016f;
        this.lastRadialRenderTime = now;
        float openAnim = class_3532.method_15363((float)((float)(now - this.openTimestamp) / 180.0f), (float)0.0f, (float)1.0f);
        float openEase = RadialMenuScreen.easeOutCubic(openAnim);
        float exitAnim = this.isExiting ? class_3532.method_15363((float)((float)(now - this.exitTimestamp) / 110.0f), (float)0.0f, (float)1.0f) : 0.0f;
        if (exitAnim >= 1.0f) {
            int toRun = this.actionToExecuteOnExit;
            this.method_25419();
            if (toRun >= 0) {
                this.executeAction(toRun);
            }
            return;
        }
        float alpha = (1.0f - exitAnim) * openEase;
        if (alpha <= 0.01f) {
            return;
        }

        float slideX = (1.0f - openEase) * 75.0f + exitAnim * 75.0f;
        float angleDiff = this.targetAngle - this.currentAngle;
        this.currentAngle += angleDiff * Math.min(1.0f, dt * 16.0f);

        float centerX = (float)this.field_22789 + slideX;
        float centerY = (float)this.field_22790 / 2.0f;
        float baseRadius = 100.0f;

        TrackState trackState = this.musicClient != null ? this.musicClient.getState() : null;
        boolean hasTrack = trackState != null && trackState.isActive() && !trackState.title().isEmpty();
        boolean isPlaying = hasTrack && trackState.isPlaying();

        long beatInterval = 500L;
        long curPos = hasTrack && trackState.getInterpolatedPositionMs() > 0L ? trackState.getInterpolatedPositionMs() : now - this.openTimestamp;
        float beatCycle = (float)(curPos % beatInterval) / (float)beatInterval;
        float beatEnergy = (float)Math.pow(Math.max(0.0, 1.0 - (double)beatCycle * 1.8), 2.2);

        float menuScale = 0.92f + 0.08f * openEase - exitAnim * 0.04f;
        context.method_51448().pushMatrix();
        context.method_51448().translate(centerX, centerY);
        context.method_51448().scale(menuScale, menuScale);
        context.method_51448().translate(-centerX, -centerY);

        this.renderAmbientGlow(context, centerX, centerY, baseRadius, alpha);
        this.renderDialArc(context, centerX, centerY, baseRadius, alpha);
        this.renderCenterVinyl(context, centerX, centerY, alpha, beatEnergy, hasTrack, isPlaying, now);
        this.renderSectorPods(context, centerX, centerY, baseRadius, alpha, beatEnergy, trackState, isPlaying, dt);
        this.renderActiveTag(context, centerX, centerY, baseRadius, alpha, beatEnergy, trackState, hasTrack);
        this.renderBottomHint(context, alpha);

        context.method_51448().popMatrix();
    }

    private void renderAmbientGlow(class_332 context, float cx, float cy, float radius, float alpha) {
        int glowSize = (int)(radius * 3.2f);
        int half = glowSize / 2;
        context.method_51448().pushMatrix();
        context.method_51448().translate(cx, cy);
        int tint = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;
        context.method_25291(class_10799.field_56883, TEX_AMBIENT_GLOW, -half, -half, 0.0f, 0.0f, glowSize, glowSize, glowSize, glowSize, tint);
        context.method_51448().popMatrix();
    }

    private void renderDialArc(class_332 context, float cx, float cy, float radius, float alpha) {
        int ringSize = (int)(radius * 2.3f);
        int half = ringSize / 2;
        context.method_51448().pushMatrix();
        context.method_51448().translate(cx, cy);
        int tint = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;
        context.method_25291(class_10799.field_56883, TEX_RING, -half, -half, 0.0f, 0.0f, ringSize, ringSize, ringSize, ringSize, tint);
        context.method_51448().popMatrix();
    }

    private void renderCenterVinyl(class_332 context, float cx, float cy, float alpha, float beatEnergy, boolean hasTrack, boolean isPlaying, long now) {
        int size = (int)(80.0f + beatEnergy * 4.0f);
        int half = size / 2;

        int domRgb = (hasTrack && this.coverManager != null) ? (this.coverManager.getSmoothDominantColor() & 0xFFFFFF) : 0x38BDF8;
        if (domRgb == 0 || domRgb == 0xFFFFFF) {
            domRgb = 0x38BDF8;
        }
        int numBars = 32;
        int innerR = half + 3;
        for (int b = 0; b < numBars; ++b) {
            float barAngle = (float)b * ((float)Math.PI * 2.0f / (float)numBars);
            float wave1 = 0.5f + 0.5f * (float)Math.sin((double)(now % 1400L) / 1400.0 * Math.PI * 2.0 + (double)b * 0.45);
            float wave2 = 0.5f + 0.5f * (float)Math.cos((double)(now % 900L) / 900.0 * Math.PI * 2.0 - (double)b * 0.75);
            float amp = isPlaying ? (0.25f + 0.45f * wave1 + 0.30f * wave2 + beatEnergy * 0.35f) : (0.14f + 0.12f * wave1);
            int barH = Math.max(2, Math.round(amp * 12.0f * alpha));
            float mix = 0.5f + 0.5f * (float)Math.sin(barAngle);
            int rC = (int)((float)((domRgb >> 16) & 0xFF) * (1.0f - mix) + 56.0f * mix);
            int gC = (int)((float)((domRgb >> 8) & 0xFF) * (1.0f - mix) + 189.0f * mix);
            int bC = (int)((float)(domRgb & 0xFF) * (1.0f - mix) + 248.0f * mix);
            int barAlpha = (int)(alpha * (isPlaying ? 225.0f : 115.0f));
            int barCol = (barAlpha << 24) | (rC << 16) | (gC << 8) | bC;
            context.method_51448().pushMatrix();
            context.method_51448().translate(cx, cy);
            context.method_51448().rotate(barAngle);
            MusicHudRenderer.renderRoundedRect(context, -1, -(innerR + barH), 2, barH, 1, barCol);
            context.method_51448().popMatrix();
        }

        float vinylSpeed = isPlaying ? 0.045f : 0.006f;
        float vinylAngle = (float)((now - this.openTimestamp) * vinylSpeed) % 360.0f;

        context.method_51448().pushMatrix();
        context.method_51448().translate(cx, cy);
        context.method_51448().rotate((float)Math.toRadians(vinylAngle));
        int tint = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;
        context.method_25291(class_10799.field_56883, TEX_VINYL, -half, -half, 0.0f, 0.0f, size, size, size, size, tint);

        if (hasTrack && this.coverManager != null) {
            class_2960 coverId = this.coverManager.getCurrentCoverId();
            if (coverId != null) {
                int coverSize = (int)((float)size * 0.46f);
                int coverHalf = coverSize / 2;
                context.method_25291(class_10799.field_56883, coverId, -coverHalf, -coverHalf, 0.0f, 0.0f, coverSize, coverSize, coverSize, coverSize, tint);
            }
        }

        int spindleSize = (int)((float)size * 0.22f);
        int spindleHalf = spindleSize / 2;
        context.method_25291(class_10799.field_56883, TEX_SPINDLE, -spindleHalf, -spindleHalf, 0.0f, 0.0f, spindleSize, spindleSize, spindleSize, spindleSize, tint);
        context.method_51448().popMatrix();
    }

    private void renderSectorPods(class_332 context, float cx, float cy, float radius, float alpha, float beatEnergy, TrackState trackState, boolean isPlaying, float dt) {
        class_327 tr = this.field_22793;
        float dt60 = Math.min(3.0f, dt * 60.0f);
        for (int i = 0; i < 8; ++i) {
            DialItem item = this.items.get(i);
            float sectorAngle = (float)i * STEP_ANGLE;
            float phi = sectorAngle - this.currentAngle;
            while ((double)phi > Math.PI) {
                phi -= (float)Math.PI * 2;
            }
            while ((double)phi <= -Math.PI) {
                phi += (float)Math.PI * 2;
            }
            if (Math.cos(phi) < -0.15) {
                continue;
            }
            boolean isSelected = (i == this.selectedIndex);
            float closeness = Math.max(0.0f, 1.0f - Math.abs(phi) / 1.4137167f);
            float targetPop = isSelected ? 8.0f + beatEnergy * 2.0f : closeness * 2.5f;
            this.podPops[i] += (targetPop - this.podPops[i]) * (1.0f - (float)Math.pow(0.72, dt60));
            float r = radius + this.podPops[i];
            float px = cx - (float)Math.cos(phi) * r;
            float py = cy - (float)Math.sin(phi) * r;

            int podSize = isSelected ? 34 : 26;
            int half = podSize / 2;
            class_2960 podTex = isSelected ? TEX_POD_HOVER : TEX_POD_NORMAL;
            int podTint = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;
            context.method_25291(class_10799.field_56883, podTex, (int)px - half, (int)py - half, 0.0f, 0.0f, podSize, podSize, podSize, podSize, podTint);

            class_2960 iconTex = item.iconTexture;
            if (i == 0) {
                iconTex = isPlaying ? ICON_PAUSE : ICON_PLAY;
            }
            int iconSize = isSelected ? 18 : 14;
            int iconHalf = iconSize / 2;
            context.method_25291(class_10799.field_56883, iconTex, (int)px - iconHalf, (int)py - iconHalf, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, podTint);

            String label = item.getDynamicLabel(trackState, isPlaying);
            int lw = tr.method_1727(label);
            int pillW = lw + (isSelected ? 16 : 12);
            int pillH = 14;
            int pillX = (int)px - half - pillW - 5;
            int pillY = (int)py - pillH / 2;
            float itemAlpha = alpha * (isSelected ? 1.0f : 0.65f + closeness * 0.35f);

            int pillBg = isSelected ? ((int)(itemAlpha * 244.0f) << 24 | 0x0F1D30) : ((int)(itemAlpha * 226.0f) << 24 | 0x0A0D14);
            int pillBorder = isSelected ? ((int)(itemAlpha * 235.0f) << 24 | 0x38BDF8) : ((int)(itemAlpha * 110.0f) << 24 | 0x334155);
            MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 4, pillBg);
            MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 4, pillBorder, pillBorder);
            if (isSelected) {
                MusicHudRenderer.renderRoundedRect(context, pillX + 2, pillY + 3, 2, pillH - 6, 1, (int)(itemAlpha * 245.0f) << 24 | item.color);
            }

            int textCol = isSelected ? ((int)(itemAlpha * 255.0f) << 24 | 0xF8FAFC) : ((int)(itemAlpha * 225.0f) << 24 | 0xCBD5E1);
            context.method_51433(tr, label, pillX + (isSelected ? 8 : 6), pillY + 3, textCol, true);
        }
    }

    private void renderActiveTag(class_332 context, float cx, float cy, float radius, float alpha, float beatEnergy, TrackState trackState, boolean hasTrack) {
        DialItem activeItem = this.items.get(this.selectedIndex);
        class_327 tr = this.field_22793;

        int tagW = 228;
        int tagH = 48;
        int tagX = (int)(cx - radius - (float)tagW - 26.0f);
        int tagY = (int)(cy - (float)tagH / 2.0f);

        int glowAlpha = (int)(alpha * 50.0f);
        int bgAlpha = (int)(alpha * 246.0f);
        int headerAlpha = (int)(alpha * 220.0f);
        int borderTopAlpha = (int)(alpha * 195.0f);
        int borderBotAlpha = (int)(alpha * 155.0f);

        MusicHudRenderer.renderRoundedRect(context, tagX - 2, tagY - 2, tagW + 4, tagH + 4, 9, glowAlpha << 24 | (activeItem.color & 0xFFFFFF));
        MusicHudRenderer.renderRoundedRect(context, tagX, tagY, tagW, tagH, 7, bgAlpha << 24 | 0x0A0D14);
        MusicHudRenderer.renderRoundedRect(context, tagX + 2, tagY + 2, tagW - 4, 17, 5, headerAlpha << 24 | 0x06080D);
        MusicHudRenderer.renderRoundedBorder(context, tagX, tagY, tagW, tagH, 7, borderTopAlpha << 24 | 0x475569, borderBotAlpha << 24 | activeItem.color);
        MusicHudRenderer.renderRoundedRect(context, tagX + 4, tagY + 5, 2, tagH - 12, 1, (int)(alpha * 245.0f) << 24 | activeItem.color);

        String promptLine = "\u00a7f" + activeItem.title + " \u00a78\u00b7 \u00a77" + activeItem.description;
        String trimmedPrompt = tr.method_27523(promptLine, tagW - 18);
        context.method_51433(tr, trimmedPrompt, tagX + 10, tagY + 6, (int)(alpha * 255.0f) << 24 | 0xF8FAFC, true);

        int timeW = 0;
        if (hasTrack && trackState.durationMs() > 0L) {
            long posSec = Math.max(0L, trackState.getInterpolatedPositionMs() / 1000L);
            long durSec = Math.max(1L, trackState.durationMs() / 1000L);
            long pm = posSec / 60L;
            long ps = posSec % 60L;
            long dm = durSec / 60L;
            long ds = durSec % 60L;
            String timeStr = pm + (ps < 10L ? ":0" : ":") + ps + " / " + dm + (ds < 10L ? ":0" : ":") + ds;
            timeW = tr.method_1727(timeStr) + 6;
            context.method_51433(tr, timeStr, tagX + tagW - timeW - 2, tagY + 23, (int)(alpha * 190.0f) << 24 | 0x94A3B8, false);
        }

        String trackLine;
        if (hasTrack) {
            trackLine = "\u00a7b" + trackState.artist() + " \u00a77\u2014 \u00a7f" + trackState.title();
        } else {
            trackLine = "\u00a77\u041e\u0436\u0438\u0434\u0430\u043d\u0438\u0435 \u0432\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u044f";
        }
        String trimmedTrack = tr.method_27523(trackLine, Math.max(40, tagW - 18 - timeW));
        context.method_51433(tr, trimmedTrack, tagX + 10, tagY + 23, (int)(alpha * 245.0f) << 24 | 0xE2E8F0, true);

        int barW = tagW - 18;
        int barY = tagY + tagH - 7;
        float progress = hasTrack ? class_3532.method_15363((float)trackState.getProgress(), (float)0.0f, (float)1.0f) : 0.0f;
        int pStart = (int)(alpha * 245.0f) << 24 | (activeItem.color & 0xFFFFFF);
        int pEnd = (int)(alpha * 245.0f) << 24 | 0x38BDF8;
        MusicHudRenderer.renderProgressBarWithPlayhead(context, tagX + 10, barY, barW, 2, progress, pStart, pEnd, System.currentTimeMillis());

        int chevronSize = 16;
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)(tagX + tagW + 7), cy);
        context.method_51448().rotate((float)Math.toRadians(180.0));
        int chevTint = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;
        context.method_25291(class_10799.field_56883, TEX_SELECTOR, -chevronSize / 2, -chevronSize / 2, 0.0f, 0.0f, chevronSize, chevronSize, chevronSize, chevronSize, chevTint);
        context.method_51448().popMatrix();
    }

    private void renderBottomHint(class_332 context, float alpha) {
        String keyName = MusicControlKeybinds.radialMenuKey.method_16007().getString().toUpperCase(java.util.Locale.ROOT);
        String hint = "\u00a77\u041a\u043e\u043b\u0451\u0441\u0438\u043a\u043e \u2014 \u0432\u044b\u0431\u043e\u0440   \u00a78|   \u00a7b\u041e\u0442\u043f\u0443\u0441\u0442\u0438\u0442\u0435 [\u00a7f" + keyName + "\u00a7b] \u0438\u043b\u0438 \u041b\u041a\u041c   \u00a78|   \u00a77ESC \u2014 \u043e\u0442\u043c\u0435\u043d\u0430";
        int hw = this.field_22793.method_1727(hint);
        int pillW = hw + 16;
        int pillH = 16;
        int pillX = this.field_22789 - pillW - 12;
        int pillY = this.field_22790 - pillH - 8;
        MusicHudRenderer.renderRoundedRect(context, pillX, pillY, pillW, pillH, 5, (int)(alpha * 238.0f) << 24 | 0x0A0D14);
        MusicHudRenderer.renderRoundedBorder(context, pillX, pillY, pillW, pillH, 5, (int)(alpha * 140.0f) << 24 | 0x475569, (int)(alpha * 95.0f) << 24 | 0x1E293B);
        context.method_51433(this.field_22793, hint, pillX + 8, pillY + 4, (int)(alpha * 240.0f) << 24 | 0xCBD5E1, true);
    }

    private static float easeOutCubic(float t) {
        float f = 1.0f - t;
        return 1.0f - f * f * f;
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

    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
    }

    protected void method_57734(class_332 context) {
    }

    public void method_52752(class_332 context) {
    }

    public void method_25419() {
        HudConfig.INSTANCE.radialLastSelectedIndex = this.selectedIndex;
        HudConfig.save();
        super.method_25419();
    }

    public boolean method_25421() {
        return false;
    }

    private static class DialItem {
        final int id;
        final class_2960 iconTexture;
        final String title;
        final String description;
        final int color;

        DialItem(int id, class_2960 iconTexture, String title, String description, int color) {
            this.id = id;
            this.iconTexture = iconTexture;
            this.title = title;
            this.description = description;
            this.color = color;
        }

        String getDynamicLabel(TrackState trackState, boolean isPlaying) {
            if (this.id == 0) {
                return isPlaying ? "Пауза" : "Плей";
            }
            if (this.id == 5) {
                return HudConfig.INSTANCE.enabled ? "HUD: ВКЛ" : "HUD: ВЫКЛ";
            }
            if (this.id == 6) {
                if (!HudConfig.INSTANCE.subtitlesEnabled) {
                    return "Субтитры: ВЫКЛ";
                }
                switch (HudConfig.INSTANCE.subtitlesMode) {
                    case "WORLD_3D": return "3D в мире";
                    case "WORLD_BLOCK":
                    case "WORLD_SURFACE": return "3D блоки";
                    case "HUD_BOTTOM": return "2D снизу";
                    case "CHAT": return "В чате";
                    default: return "2D случайно";
                }
            }
            return this.title;
        }
    }
}
