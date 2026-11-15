package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.YandexMusicHudMod;
import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.ColorWheelWidget;
import com.example.yandexmusichud.hud.FavoritesScreen;
import com.example.yandexmusichud.hud.FontSelectorScreen;
import com.example.yandexmusichud.hud.KeybindSetupScreen;
import com.example.yandexmusichud.hud.LocalMusicScreen;
import com.example.yandexmusichud.lyrics.CustomFontManager;
import com.example.yandexmusichud.lyrics.LyricLine;
import com.example.yandexmusichud.lyrics.LyricsRenderer;
import com.example.yandexmusichud.hud.ModernButtonWidget;
import com.example.yandexmusichud.hud.ModernSliderWidget;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.hud.NiaAuthorScreen;
import com.example.yandexmusichud.hud.SystemDiagnosticsScreen;
import com.example.yandexmusichud.hud.TwitchSettingsScreen;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.stats.SessionStatsManager;
import com.example.yandexmusichud.texture.CustomBackgroundManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_6880;
import org.lwjgl.glfw.GLFW;

public class HudEditScreen
extends class_437 {
    private static final class_2960 TWITCH_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/twitch_icon.png");
    private static final class_2960 MOD_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/mod_icon.png");
    private static final class_2960 SCREEN_CRACK_TEXTURE = class_2960.method_60655("yandexmusichud", "textures/gui/screen_crack.png");
    private static final class_2960 ICON_HUD = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_hud.png");
    private static final class_2960 ICON_SUBTITLES = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_subtitles.png");
    private static final class_2960 ICON_SETTINGS = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_settings.png");
    private static final class_2960 ICON_FAVORITE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_favorite.png");
    private static final class_2960 ICON_LIBRARY = class_2960.method_60655("yandexmusichud", "textures/gui/radial/icon_library.png");
    private static final String[] DOCK_ICONS = new String[]{"::", "1", "2", "3", "4", "Вкл", "Изб", "Клв", "Тест", "Tw", "Окн", "X"};
    private static final String[] DOCK_TOOLTIPS = new String[]{"Вытащить в окно", "Виджет HUD", "Субтитры", "Цветовая палитра", "Сессия и плеер", "Включение мода", "Избранное", "Горячие клавиши", "Диагностика", "Twitch стрим", "Открепить в окно", "Закрыть (ESC)"};
    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean dragging2DSub = false;
    private int dragSubOffsetX;
    private int dragSubOffsetY;
    private boolean draggingConfig = false;
    private int dragConfigOffsetX;
    private int dragConfigOffsetY;
    private int dockSide = 0;
    private int dockX = 0;
    private int dockY = 50;
    private int dockW = 34;
    private int dockH = 226;
    private boolean flyoutOpen = true;
    private boolean draggingDock = false;
    private int dragDockStartX = 0;
    private int dragDockStartY = 0;
    private final float[] dockIconScale = new float[12];
    private final List<ScreenCrackImpact> screenCrackImpacts = new ArrayList<ScreenCrackImpact>();
    private int subPreviewW = 180;
    private int subPreviewH = 26;
    private int hudX;
    private int hudY;
    private int hudWidth;
    private int hudHeight;
    private int panelX = -1;
    private int panelY = -1;
    private int panelWidth = 332;
    private int panelHeight = 246;
    private int currentTab = 0;
    private float tabIndicatorX = -1.0f;
    private float tabIndicatorW = -1.0f;
    private long tabSwitchStartTime = 0L;
    private long screenOpenTimestamp = 0L;
    private long lastFrameTime = 0L;
    private int paletteTarget = 0;
    private ColorWheelWidget paletteColorWheel;
    private List<String> availablePngBackgrounds = List.of();
    private String feedbackToast = null;
    private long feedbackToastTime = 0L;
    private int feedbackColor = 58879;
    private boolean alertSoundPlayed = false;
    private final List<EasterEggParticle> easterEggParticles = new ArrayList<EasterEggParticle>();
    private final List<CursorSparkle> cursorSparkles = new ArrayList<CursorSparkle>();
    private int niaEasterEggClicks = 0;
    private long lastNiaClickTime = 0L;
    private final List<HelpEntry> helpEntries = new ArrayList<HelpEntry>();
    private final class_437 parent;
    private long lastRenderMs = 0L;
    private float screenShake = 0.0f;
    private float curTiltX = 0.0f;
    private float curTiltY = 0.0f;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private int sparkleTimer = 0;
    private long dockImpactTime = 0L;
    private int dockImpactSide = 0;
    private String activeDropdownId = null;
    private int activeDropdownX = 0;
    private int activeDropdownY = 0;
    private int activeDropdownW = 0;
    private int activeDropdownH = 0;
    private int activeDropdownBtnY = 0;
    private int activeDropdownScroll = 0;
    private int activeDropdownMaxVisible = 8;
    private List<DropdownItem> activeDropdownItems = new ArrayList<DropdownItem>();
    private Consumer<String> activeDropdownCallback = null;
    private Supplier<String> activeDropdownSelectedKey = null;

    public static class DropdownItem {
        public final String key;
        public final String label;
        public final String hint;

        public DropdownItem(String key, String label, String hint) {
            this.key = key;
            this.label = label;
            this.hint = hint;
        }
    }

    private static final String[][] ANIMATION_OPTIONS = new String[][]{
        {"KARAOKE_BOUNCE", "Караоке", ""},
        {"WAVE_FLOAT", "Волна", ""},
        {"ELASTIC_POP", "Пружина", ""},
        {"PULSE_GLOW", "Пульс", ""},
        {"SLIDE_FADE", "Слайд", ""},
        {"CLASSIC", "Классика", ""},
        {"MISIDE_FALL", "MiSide", ""}
    };

    private static final String[][] MODE_OPTIONS = new String[][]{
        {"WORLD_3D", "3D в мире", ""},
        {"WORLD_BLOCK", "На блоке", ""},
        {"HUD_BOTTOM", "2D экран", ""},
        {"SCREEN_RANDOM", "2D разброс", ""},
        {"CHAT", "В чате", ""},
        {"OFF", "ВЫКЛ", ""}
    };

    private static final String[][] TRAIL_OPTIONS = new String[][]{
        {"1", "1 слово", ""},
        {"2", "2 слова", ""},
        {"3", "3 слова", ""},
        {"4", "4 слова", ""},
        {"5", "5 слов", ""},
        {"6", "6 слов", ""},
        {"7", "7 слов", ""},
        {"8", "8 слов", ""}
    };

    private static final String[][] HUD_STYLE_OPTIONS = new String[][]{
        {"MINIMAL_DARK", "Тёмный минимал", ""},
        {"GLASS_AMBIENT", "Стекло + Амбиент", ""},
        {"VINYL_DECK", "Винил-дека", ""},
        {"DYNAMIC_ISLAND", "Остров (Капсула)", ""}
    };

    private static class ScreenCrackImpact {
        final int originX;
        final int originY;
        final int side;
        final long startTime;
        final long durationMs = 3800L;

        ScreenCrackImpact(int originX, int originY, int side) {
            this.originX = originX;
            this.originY = originY;
            this.side = side;
            this.startTime = System.currentTimeMillis();
        }
    }

    public HudEditScreen() {
        this(null);
    }

    public HudEditScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("Настройки"));
        this.parent = parent;
        for (int i = 0; i < this.dockIconScale.length; ++i) {
            this.dockIconScale[i] = 1.0f;
        }
    }

    public void playSound(class_3414 sound, float pitch, float volume) {
        try {
            if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)sound, (float)pitch, (float)volume));
            }
        }
        catch (Exception ignored) {
        }
    }

    public void playSound(class_6880<class_3414> sound, float pitch, float volume) {
        if (sound != null && sound.comp_349() != null) {
            this.playSound((class_3414)sound.comp_349(), pitch, volume);
        }
    }

    private void triggerScreenCrack(int edgeX, int edgeY, int side) {
        this.screenCrackImpacts.add(new ScreenCrackImpact(edgeX, edgeY, side));
    }

    private void renderScreenCracks(class_332 context) {
        if (this.screenCrackImpacts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int crackSize = Math.max(220, Math.min(340, this.field_22790 - 20));
        for (int i = this.screenCrackImpacts.size() - 1; i >= 0; --i) {
            ScreenCrackImpact impact = this.screenCrackImpacts.get(i);
            long elapsed = now - impact.startTime;
            if (elapsed > impact.durationMs) {
                this.screenCrackImpacts.remove(i);
                continue;
            }
            float alpha;
            if (elapsed < 1600L) {
                alpha = 1.0f;
            } else {
                float healProgress = (float)(elapsed - 1600L) / (float)(impact.durationMs - 1600L);
                alpha = 1.0f - healProgress;
            }
            if (alpha <= 0.01f) continue;

            float grow;
            if (elapsed < 120L) {
                float gt = (float)elapsed / 120.0f;
                grow = (float)Math.sin(gt * Math.PI * 0.5);
            } else {
                grow = 1.0f;
            }

            if (elapsed < 180L) {
                float flashT = (float)elapsed / 180.0f;
                float flashAlpha = (1.0f - flashT) * alpha;
                int flashCol = (int)(flashAlpha * 200.0f) << 24 | 0xFFFFFF;
                int craterW = (int)(12.0f * (1.0f - flashT * 0.5f));
                int craterH = (int)(28.0f * (1.0f - flashT * 0.5f));
                int craterX = impact.side == 1 ? impact.originX : impact.originX - craterW;
                MusicHudRenderer.renderRoundedRect(context, craterX, impact.originY - craterH / 2, craterW, craterH, 5, flashCol);
            }

            int colorARGB = (int)(alpha * 255.0f) << 24 | 0xFFFFFF;

            context.method_51448().pushMatrix();
            context.method_51448().translate((float)impact.originX, (float)impact.originY);
            context.method_51448().scale(grow, grow);
            if (impact.side == 1) {
                context.method_25293(class_10799.field_56883, SCREEN_CRACK_TEXTURE, 0, -crackSize / 2, 0.0f, 0.0f, crackSize, crackSize, 512, 512, 512, 512, colorARGB);
            } else {
                context.method_25293(class_10799.field_56883, SCREEN_CRACK_TEXTURE, -crackSize, -crackSize / 2, 512.0f, 0.0f, crackSize, crackSize, -512, 512, 512, 512, colorARGB);
            }
            context.method_51448().popMatrix();
        }
    }

    private void dockTo(int side) {
        this.dockSide = side;
        HudConfig.INSTANCE.configDockSide = side;
        HudConfig.save();
        this.dockH = Math.min(226, this.field_22790 - 20);
        this.dockW = 34;
        if (this.dockSide == 1) {
            this.dockX = 0;
        } else {
            this.dockX = this.field_22789 - this.dockW;
        }
        this.dockY = Math.max(10, Math.min(this.panelY, this.field_22790 - this.dockH - 10));
        this.panelWidth = Math.min(294, this.field_22789 - this.dockW - 16);
        this.panelHeight = this.dockH;
        if (this.dockSide == 1) {
            this.panelX = this.dockW + 6;
        } else {
            this.panelX = this.dockX - this.panelWidth - 6;
        }
        this.panelY = this.dockY;
        this.flyoutOpen = true;
        this.draggingConfig = false;
        this.draggingDock = false;
        this.dockImpactTime = System.currentTimeMillis();
        this.dockImpactSide = side;
        this.screenShake = 0.0f;
        this.playSound(class_3417.field_15081, 1.0f, 1.0f);
        this.playSound(class_3417.field_15081, 1.25f, 0.8f);
        this.playSound(class_3417.field_14583, 0.95f, 0.9f);
        this.playSound(class_3417.field_14627, 1.2f, 0.85f);
        this.playSound((class_6880<class_3414>)class_3417.field_14725, 0.8f, 0.8f);
        int impactY = this.dockY + this.dockH / 2;
        this.triggerScreenCrack(this.dockSide == 1 ? 0 : this.field_22789, impactY, this.dockSide);
        this.initColorWheels();
        this.buildTabWidgets();
    }

    private void undockToFloating(int mouseX, int mouseY) {
        this.dockSide = 0;
        HudConfig.INSTANCE.configDockSide = 0;
        HudConfig.save();
        this.panelWidth = Math.min(332, this.field_22789 - 24);
        this.panelHeight = Math.min(246, this.field_22790 - 28);
        if (mouseX >= 0 && mouseY >= 0) {
            this.panelX = mouseX - this.panelWidth / 2;
            this.panelY = mouseY - 14;
        } else {
            this.panelX = (this.field_22789 - this.panelWidth) / 2;
            this.panelY = (this.field_22790 - this.panelHeight) / 2;
        }
        this.panelX = Math.max(10, Math.min(this.panelX, this.field_22789 - this.panelWidth - 10));
        this.panelY = Math.max(10, Math.min(this.panelY, this.field_22790 - this.panelHeight - 10));
        this.draggingDock = false;
        this.screenShake = 0.0f;
        this.playSound((class_6880<class_3414>)class_3417.field_15015, 1.35f, 0.8f);
        this.triggerFeedback("§bОкно откреплено", 58879);
        this.initColorWheels();
        this.closeDropdown();
        this.buildTabWidgets();
    }

    private int getDockIconColor(int i) {
        return switch (i) {
            case 0 -> -6710887;
            case 1 -> this.currentTab == 0 && this.flyoutOpen ? -16718337 : -1;
            case 2 -> this.currentTab == 1 && this.flyoutOpen ? -16718337 : -1;
            case 3 -> this.currentTab == 2 && this.flyoutOpen ? -16718337 : -1;
            case 4 -> this.currentTab == 3 && this.flyoutOpen ? -16718337 : -1;
            case 5 -> HudConfig.INSTANCE.enabled ? -16711936 : -65536;
            case 6 -> -13312;
            case 7 -> -256;
            case 8 -> -16718337;
            case 9 -> -7256833;
            case 10 -> -16718337;
            case 11 -> -65536;
            default -> -1;
        };
    }

    private String getTabHeaderTitle() {
        return switch (this.currentTab) {
            case 0 -> "§fВиджет HUD";
            case 1 -> "§fСубтитры";
            case 2 -> "§fЦветовая палитра";
            case 3 -> "§fСессия и плеер";
            default -> "§fНастройки";
        };
    }

    protected void method_25426() {
        super.method_25426();
        this.screenOpenTimestamp = System.currentTimeMillis();
        this.refreshBackgroundList();
        this.recalculateHudDimensions();
        this.hudX = HudConfig.INSTANCE.getAbsoluteX(this.field_22789, this.hudWidth);
        this.hudY = HudConfig.INSTANCE.getAbsoluteY(this.field_22790, this.hudHeight);
        this.hudX = Math.max(0, Math.min(this.hudX, this.field_22789 - this.hudWidth));
        this.hudY = Math.max(0, Math.min(this.hudY, this.field_22790 - this.hudHeight));
        this.dockSide = HudConfig.INSTANCE.configDockSide;
        this.dockW = 34;
        this.dockH = Math.min(226, this.field_22790 - 20);
        this.dockY = (this.field_22790 - this.dockH) / 2;
        if (this.dockSide == 1) {
            this.dockX = 0;
            this.panelWidth = Math.min(294, this.field_22789 - this.dockW - 16);
            this.panelHeight = this.dockH;
            this.panelX = this.dockW + 6;
            this.panelY = this.dockY;
        } else if (this.dockSide == 2) {
            this.dockX = this.field_22789 - this.dockW;
            this.panelWidth = Math.min(294, this.field_22789 - this.dockW - 16);
            this.panelHeight = this.dockH;
            this.panelX = this.dockX - this.panelWidth - 6;
            this.panelY = this.dockY;
        } else {
            this.panelWidth = Math.min(332, this.field_22789 - 24);
            this.panelHeight = Math.min(246, this.field_22790 - 28);
            if (this.panelX < 0 || this.panelY < 0 || this.panelX + this.panelWidth > this.field_22789 || this.panelY + this.panelHeight > this.field_22790) {
                this.panelX = (this.field_22789 - this.panelWidth) / 2;
                this.panelY = (this.field_22790 - this.panelHeight) / 2;
            }
        }
        this.initColorWheels();
        this.buildTabWidgets();
    }

    private void initColorWheels() {
        int contentX = this.panelX + (this.dockSide != 0 ? 8 : 10);
        int contentY = this.panelY + (this.dockSide != 0 ? 22 : 46);
        int initialRgb = this.getCurrentTargetColor();
        int wheelSize = this.dockSide != 0 ? 68 : 80;
        this.paletteColorWheel = new ColorWheelWidget(contentX + 2, contentY + 24, wheelSize, initialRgb, rgb -> {
            this.setCurrentTargetColor((int)rgb);
        });
    }

    private int getCurrentTargetColor() {
        return switch (this.paletteTarget) {
            case 0 -> HudConfig.INSTANCE.configBackgroundColor;
            case 1 -> HudConfig.INSTANCE.hudBackgroundColor;
            case 2 -> HudConfig.INSTANCE.progressBarColor;
            case 3 -> HudConfig.INSTANCE.subtitlesColor;
            case 4 -> HudConfig.INSTANCE.subtitlesBgColor;
            case 5 -> HudConfig.INSTANCE.subtitlesActiveColor;
            default -> 0xFFFFFF;
        };
    }

    private void setCurrentTargetColor(int rgb) {
        switch (this.paletteTarget) {
            case 0 -> HudConfig.INSTANCE.configBackgroundColor = rgb;
            case 1 -> HudConfig.INSTANCE.hudBackgroundColor = rgb;
            case 2 -> HudConfig.INSTANCE.progressBarColor = rgb;
            case 3 -> HudConfig.INSTANCE.subtitlesColor = rgb;
            case 4 -> HudConfig.INSTANCE.subtitlesBgColor = rgb;
            case 5 -> HudConfig.INSTANCE.subtitlesActiveColor = rgb;
        }
    }

    private void refreshBackgroundList() {
        this.availablePngBackgrounds = CustomBackgroundManager.INSTANCE.getAvailableBackgrounds();
    }

    private void recalculateHudDimensions() {
        float scale = class_3532.method_15363(HudConfig.INSTANCE.hudScale, 0.2f, 4.0f);
        this.hudWidth = (int)((float)HudConfig.INSTANCE.getHudBaseWidth() * scale);
        this.hudHeight = (int)((float)HudConfig.INSTANCE.getHudBaseHeight() * scale);
    }

    private void registerHelp(int x, int y, int w, int h, String title, String desc, String rec) {
        this.helpEntries.add(new HelpEntry(x, y, w, h, title, desc, rec));
    }

    private void buildTabWidgets() {
        this.method_37067();
        this.helpEntries.clear();
        int headerBtnY = this.panelY + (this.dockSide != 0 ? 4 : 6);
        if (this.dockSide != 0) {
            if (!this.flyoutOpen) {
                return;
            }
            int closeFlyoutW = 16;
            int closeFlyoutH = 14;
            int closeFlyoutX = this.panelX + this.panelWidth - closeFlyoutW - 6;
            ModernButtonWidget collapseBtn = new ModernButtonWidget(closeFlyoutX, headerBtnY, closeFlyoutW, closeFlyoutH, (class_2561)class_2561.method_43470(this.dockSide == 1 ? "◀" : "▶"), b -> {
                this.flyoutOpen = false;
                this.screenShake = 0.6f;
                this.playSound(class_3417.field_14709, 1.3f, 0.7f);
                this.buildTabWidgets();
            });
            collapseBtn.setCustomBgColor(0xDD283448);
            this.method_37063(collapseBtn);
            this.registerHelp(closeFlyoutX, headerBtnY, closeFlyoutW, closeFlyoutH, "Спрятать панель", "Сворачивает конфигурацию обратно в боковое меню.", "Нажмите на иконку в боковом меню, чтобы снова открыть.");
            int undockFlyoutW = 16;
            int undockFlyoutX = closeFlyoutX - undockFlyoutW - 4;
            ModernButtonWidget undockBtn = new ModernButtonWidget(undockFlyoutX, headerBtnY, undockFlyoutW, closeFlyoutH, (class_2561)class_2561.method_43470("⤢"), b -> {
                this.undockToFloating(-1, -1);
            });
            undockBtn.setCustomBgColor(0xDD283448);
            this.method_37063(undockBtn);
            this.registerHelp(undockFlyoutX, headerBtnY, undockFlyoutW, closeFlyoutH, "Вытащить в окно", "Открепляет меню от боковой стороны экрана.", "Превращает в свободное перемещаемое окно.");
        } else {
            int closeBtnW = 16;
            int closeBtnH = 15;
            int closeBtnX = this.panelX + this.panelWidth - closeBtnW - 8;
            ModernButtonWidget closeBtn = new ModernButtonWidget(closeBtnX, headerBtnY, closeBtnW, closeBtnH, (class_2561)class_2561.method_43470("X"), b -> {
                this.method_25419();
            });
            closeBtn.setCustomBgColor(0xDD442228);
            this.method_37063(closeBtn);
            this.registerHelp(closeBtnX, headerBtnY, closeBtnW, closeBtnH, "Закрыть", "Закрывает меню настроек (ESC).", "Все параметры сохраняются мгновенно.");
            int masterW = 36;
            int masterH = 15;
            int masterX = closeBtnX - masterW - 4;
            ModernButtonWidget masterBtn = new ModernButtonWidget(masterX, headerBtnY, masterW, masterH, (class_2561)class_2561.method_43470(HudConfig.INSTANCE.enabled ? "§aВКЛ" : "§cВЫКЛ"), b -> {
                HudConfig.INSTANCE.enabled = !HudConfig.INSTANCE.enabled;
                this.screenShake = 1.2f;
                b.method_25355((class_2561)class_2561.method_43470(HudConfig.INSTANCE.enabled ? "§aВКЛ" : "§cВЫКЛ"));
                b.setCustomBgColor(HudConfig.INSTANCE.enabled ? -585408992 : -578093056);
                HudConfig.save();
                this.triggerFeedback(HudConfig.INSTANCE.enabled ? "§aМод включен" : "§cМод выключен", HudConfig.INSTANCE.enabled ? 58879 : 0xFF5F56);
            });
            masterBtn.setCustomBgColor(HudConfig.INSTANCE.enabled ? -585408992 : -578093056);
            this.method_37063(masterBtn);
            this.registerHelp(masterX, headerBtnY, masterW, masterH, "Главный статус", "Полное включение или отключение интерфейса мода.", "ВКЛ для работы карточки и субтитров.");
            int favBtnW = 18;
            int favBtnX = masterX - favBtnW - 4;
            ModernButtonWidget favBtn = new ModernButtonWidget(favBtnX, headerBtnY, favBtnW, masterH, (class_2561)class_2561.method_43470(""), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new FavoritesScreen(this));
                }
            });
            favBtn.setIconTexture(ICON_FAVORITE, 10);
            favBtn.setCustomBgColor(0xDD283448);
            this.method_37063(favBtn);
            this.registerHelp(favBtnX, headerBtnY, favBtnW, masterH, "Избранное", "Список сохранённых любимых треков.", "Нажмите для открытия каталога песен.");
            int keyBtnW = 18;
            int keyBtnX = favBtnX - keyBtnW - 4;
            ModernButtonWidget keyBtn = new ModernButtonWidget(keyBtnX, headerBtnY, keyBtnW, masterH, (class_2561)class_2561.method_43470("§e⌨"), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new KeybindSetupScreen(this));
                }
            });
            keyBtn.setCustomBgColor(0xDD283448);
            this.method_37063(keyBtn);
            this.registerHelp(keyBtnX, headerBtnY, keyBtnW, masterH, "Горячие клавиши", "Экран настройки кнопок переключения треков.", "Управляйте музыкой с клавиатуры во время игры.");
            int diagBtnW = 18;
            int diagBtnX = keyBtnX - diagBtnW - 4;
            ModernButtonWidget diagBtn = new ModernButtonWidget(diagBtnX, headerBtnY, diagBtnW, masterH, (class_2561)class_2561.method_43470("§b⚡"), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new SystemDiagnosticsScreen(this, new KeybindSetupScreen(this)));
                }
            });
            diagBtn.setCustomBgColor(0xDD283448);
            this.method_37063(diagBtn);
            this.registerHelp(diagBtnX, headerBtnY, diagBtnW, masterH, "Диагностика", "Проверка связи со службой Windows Media и плеером.", "Нажмите для отчёта о работе субтитров.");
            int twitchBtnW = 18;
            int twitchBtnX = diagBtnX - twitchBtnW - 4;
            ModernButtonWidget twitchBtn = new ModernButtonWidget(twitchBtnX, headerBtnY, twitchBtnW, masterH, (class_2561)class_2561.method_43470(""), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new TwitchSettingsScreen(this));
                }
            });
            twitchBtn.setIconTexture(TWITCH_ICON, 10);
            twitchBtn.setCustomBgColor(HudConfig.INSTANCE.twitchEnabled ? 0xDD9146FF : 0xDD283448);
            this.method_37063(twitchBtn);
            this.registerHelp(twitchBtnX, headerBtnY, twitchBtnW, masterH, "Twitch интеграция", "Настройка стрима, плашек сабов и наград канала.", "Нажмите для открытия настроек Twitch.");
            this.registerHelp(this.panelX + 8, this.panelY + 4, twitchBtnX - (this.panelX + 8), 18, "Перемещение панели", "Зажмите ЛКМ за шапку и перетащите меню.", "Свободно перемещается по экрану.");
            int tabY = this.panelY + 25;
            int tabH = 18;
            int tabW = (this.panelWidth - 16 - 9) / 4;
            ModernButtonWidget tab0 = new ModernButtonWidget(this.panelX + 8, tabY, tabW, tabH, (class_2561)class_2561.method_43470("Виджет"), b -> {
                if (this.currentTab != 0) {
                    this.closeDropdown();
                    this.currentTab = 0;
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    this.buildTabWidgets();
                    this.triggerFeedback("Вкладка: §bВиджет HUD", 58879);
                }
            });
            tab0.setPrimary(this.currentTab == 0);
            this.method_37063(tab0);
            this.registerHelp(tab0.method_46426(), tab0.method_46427(), tab0.method_25368(), tab0.method_25364(), "Вкладка «Виджет»", "Внешний вид, макет и размер карточки трека.", "Основные параметры визитки плеера.");
            ModernButtonWidget tab1 = new ModernButtonWidget(this.panelX + 8 + tabW + 3, tabY, tabW, tabH, (class_2561)class_2561.method_43470("Текст"), b -> {
                if (this.currentTab != 1) {
                    this.closeDropdown();
                    this.currentTab = 1;
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    this.buildTabWidgets();
                    this.triggerFeedback("Вкладка: §bСубтитры", 58879);
                }
            });
            tab1.setPrimary(this.currentTab == 1);
            this.method_37063(tab1);
            this.registerHelp(tab1.method_46426(), tab1.method_46427(), tab1.method_25368(), tab1.method_25364(), "Вкладка «Текст»", "Отображение субтитров в мире и на экране.", "Выбор 2D/3D режима, анимаций и спринта.");
            ModernButtonWidget tab2 = new ModernButtonWidget(this.panelX + 8 + (tabW + 3) * 2, tabY, tabW, tabH, (class_2561)class_2561.method_43470("Цвет"), b -> {
                if (this.currentTab != 2) {
                    this.closeDropdown();
                    this.currentTab = 2;
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    if (this.paletteColorWheel != null) {
                        this.paletteColorWheel.setColorFromRgb(this.getCurrentTargetColor());
                    }
                    this.buildTabWidgets();
                    this.triggerFeedback("Вкладка: §bЦвета", 58879);
                }
            });
            tab2.setPrimary(this.currentTab == 2);
            this.method_37063(tab2);
            this.registerHelp(tab2.method_46426(), tab2.method_46427(), tab2.method_25368(), tab2.method_25364(), "Вкладка «Цвета»", "Выбор палитры фона, текста и полосы прогресса.", "Круг для точного подбора оттенков.");
            ModernButtonWidget tab3 = new ModernButtonWidget(this.panelX + 8 + (tabW + 3) * 3, tabY, tabW, tabH, (class_2561)class_2561.method_43470("Сессия"), b -> {
                if (this.currentTab != 3) {
                    this.closeDropdown();
                    this.currentTab = 3;
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    this.buildTabWidgets();
                    this.triggerFeedback("Вкладка: §bСтатистика и сессия", 58879);
                }
            });
            tab3.setPrimary(this.currentTab == 3);
            this.method_37063(tab3);
            this.registerHelp(tab3.method_46426(), tab3.method_46427(), tab3.method_25368(), tab3.method_25364(), "Вкладка «Сессия»", "Музыкальная статистика сессии, настройки и плеер.", "Время, треки, приглушение звука и плеер.");
        }
        int contentX = this.panelX + (this.dockSide != 0 ? 8 : 10);
        int contentY = this.panelY + (this.dockSide != 0 ? 24 : 46);
        int colW = (this.panelWidth - (this.dockSide != 0 ? 20 : 24)) / 2;
        if (this.currentTab == 0) {
            String styleArrow = (this.activeDropdownId != null && "HUD_STYLE".equals(this.activeDropdownId)) ? " §b▲" : " §7▼";
            ModernButtonWidget btnStyle = new ModernButtonWidget(contentX, contentY, colW, 19, (class_2561)class_2561.method_43470("Стиль: §b" + this.getHudStyleName() + styleArrow), b -> {
                List<DropdownItem> items = new ArrayList<DropdownItem>();
                for (String[] opt : HUD_STYLE_OPTIONS) {
                    items.add(new DropdownItem(opt[0], opt[1], opt[2]));
                }
                this.openDropdown("HUD_STYLE", contentX, contentY, Math.max(colW, 148), items, () -> {
                    String cur = HudConfig.INSTANCE.hudStyle;
                    return cur != null ? cur : "MINIMAL_DARK";
                }, chosenKey -> {
                    HudConfig.INSTANCE.hudStyle = chosenKey;
                    this.recalculateHudDimensions();
                    this.hudX = HudConfig.INSTANCE.getAbsoluteX(this.field_22789, this.hudWidth);
                    this.hudY = HudConfig.INSTANCE.getAbsoluteY(this.field_22790, this.hudHeight);
                    this.hudX = Math.max(0, Math.min(this.hudX, this.field_22789 - this.hudWidth));
                    this.hudY = Math.max(0, Math.min(this.hudY, this.field_22790 - this.hudHeight));
                    HudConfig.save();
                    this.screenShake = 0.5f;
                    this.triggerFeedback("§b✦ Стиль HUD: §f" + this.getHudStyleName(), 0x38BDF8);
                    this.buildTabWidgets();
                });
                this.buildTabWidgets();
            });
            this.method_37063(btnStyle);
            this.registerHelp(btnStyle.method_46426(), btnStyle.method_46427(), btnStyle.method_25368(), btnStyle.method_25364(), "Стиль карточки HUD", "Выбор дизайна карточки плеера из 4 пресетов.", "Тёмный минимал, Стекло + Амбиент, Винил-дека или Остров.");

            boolean isLarge = "LARGE".equalsIgnoreCase(HudConfig.INSTANCE.hudSize);
            ModernButtonWidget btnSize = new ModernButtonWidget(contentX + colW + 4, contentY, colW, 19, (class_2561)class_2561.method_43470("Размер: " + (isLarge ? "§bБольшой" : "§7Компакт")), b -> {
                HudConfig.INSTANCE.hudSize = "LARGE".equalsIgnoreCase(HudConfig.INSTANCE.hudSize) ? "COMPACT" : "LARGE";
                this.recalculateHudDimensions();
                HudConfig.save();
                this.screenShake = 0.8f;
                this.buildTabWidgets();
            });
            this.method_37063(btnSize);
            this.registerHelp(btnSize.method_46426(), btnSize.method_46427(), btnSize.method_25368(), btnSize.method_25364(), "Размер карточки", "Переключение между большим и компактным видом.", "Большой с винилом или аккуратный Компакт.");

            ModernButtonWidget btnEq = new ModernButtonWidget(contentX, contentY + 22, colW, 19, (class_2561)class_2561.method_43470("Эквалайзер: " + (HudConfig.INSTANCE.showEqualizer ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.showEqualizer = !HudConfig.INSTANCE.showEqualizer;
                HudConfig.save();
                this.screenShake = 0.8f;
                this.buildTabWidgets();
            });
            this.method_37063(btnEq);
            this.registerHelp(btnEq.method_46426(), btnEq.method_46427(), btnEq.method_25368(), btnEq.method_25364(), "Эквалайзер", "Столбики на карточке, скачущие под музыку.", "ВКЛ для живого вида плеера.");

            ModernButtonWidget btnBeat = new ModernButtonWidget(contentX + colW + 4, contentY + 22, colW, 19, (class_2561)class_2561.method_43470("Бит-пульс: " + (HudConfig.INSTANCE.beatBounce ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.beatBounce = !HudConfig.INSTANCE.beatBounce;
                HudConfig.save();
                this.screenShake = 0.0f;
                this.buildTabWidgets();
            });
            this.method_37063(btnBeat);
            this.registerHelp(btnBeat.method_46426(), btnBeat.method_46427(), btnBeat.method_25368(), btnBeat.method_25364(), "Бит-пульс", "Мягкая пульсация карточки и меню в такт басам.", "Создаёт эффект живой музыки.");

            ModernButtonWidget btnBar = new ModernButtonWidget(contentX, contentY + 44, colW, 19, (class_2561)class_2561.method_43470("Полоса: " + (HudConfig.INSTANCE.showProgressBar ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.showProgressBar = !HudConfig.INSTANCE.showProgressBar;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(btnBar);
            this.registerHelp(btnBar.method_46426(), btnBar.method_46427(), btnBar.method_25368(), btnBar.method_25364(), "Полоса времени", "Линия прогресса трека внизу карточки.", "Показывает, сколько времени прошло.");

            ModernButtonWidget btnAuto = new ModernButtonWidget(contentX + colW + 4, contentY + 44, colW, 19, (class_2561)class_2561.method_43470("Отражение: " + (HudConfig.INSTANCE.autoAdaptLayout ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.autoAdaptLayout = !HudConfig.INSTANCE.autoAdaptLayout;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(btnAuto);
            this.registerHelp(btnAuto.method_46426(), btnAuto.method_46427(), btnAuto.method_25368(), btnAuto.method_25364(), "Отражение макета", "Меняет обложку и текст местами у левого края.", "Обложка всегда ближе к краю экрана.");

            int bgBtnW = colW - 22;
            int folderBtnW = 18;
            String currentBgName = HudConfig.INSTANCE.customBackgroundFile.isEmpty() ? "Сплошной" : HudConfig.INSTANCE.customBackgroundFile;
            String bgBtnLabel = currentBgName.length() > 9 ? currentBgName.substring(0, 8) + "…" : currentBgName;
            String bgArrow = (this.activeDropdownId != null && "BG_FILE".equals(this.activeDropdownId)) ? " §b▲" : " §7▼";
            ModernButtonWidget btnBg = new ModernButtonWidget(contentX, contentY + 66, bgBtnW, 19, (class_2561)class_2561.method_43470("Фон: " + bgBtnLabel + bgArrow), b -> {
                this.refreshBackgroundList();
                List<DropdownItem> items = new ArrayList<DropdownItem>();
                items.add(new DropdownItem("", "Сплошной", "Цвет"));
                for (String file : this.availablePngBackgrounds) {
                    items.add(new DropdownItem(file, file, "Файл"));
                }
                this.openDropdown("BG_FILE", contentX, contentY + 66, Math.max(bgBtnW + folderBtnW + 4, 130), items, () -> HudConfig.INSTANCE.customBackgroundFile, chosenKey -> {
                    HudConfig.INSTANCE.customBackgroundFile = chosenKey;
                    HudConfig.save();
                    CustomBackgroundManager.INSTANCE.reload();
                    String name = chosenKey.isEmpty() ? "Сплошной" : chosenKey;
                    this.screenShake = 0.5f;
                    this.triggerFeedback("§b✦ Фон HUD: §e" + name, 58879);
                    this.buildTabWidgets();
                });
                this.buildTabWidgets();
            });
            this.method_37063(btnBg);
            this.registerHelp(btnBg.method_46426(), btnBg.method_46427(), btnBg.method_25368(), btnBg.method_25364(), "Картинка фона (.PNG / .JPG)", "Выбор текстуры фона из выпадающего списка.", "Сплошной цвет или картинки из папки backgrounds.");

            ModernButtonWidget btnFolder = new ModernButtonWidget(contentX + bgBtnW + 4, contentY + 66, folderBtnW, 19, (class_2561)class_2561.method_43470("📁"), b -> {
                CustomBackgroundManager.INSTANCE.openBackgroundsFolder();
                this.refreshBackgroundList();
                this.triggerFeedback("§a📁 Папка фонов открыта", 58879);
            });
            btnFolder.setCustomBgColor(0xDD182232);
            this.method_37063(btnFolder);
            this.registerHelp(btnFolder.method_46426(), btnFolder.method_46427(), btnFolder.method_25368(), btnFolder.method_25364(), "Папка фонов", "Открывает папку backgrounds в Проводнике.", "Добавляйте картинки прямо во время игры.");

            boolean isPopup = "POPUP".equalsIgnoreCase(HudConfig.INSTANCE.hudDisplayType);
            int popupW = colW - 22;
            ModernButtonWidget btnPopup = new ModernButtonWidget(contentX + colW + 4, contentY + 66, popupW, 19, (class_2561)class_2561.method_43470("Показ: " + (isPopup ? "§bВсплыв." : "§aВсегда")), b -> {
                HudConfig.INSTANCE.hudDisplayType = "POPUP".equalsIgnoreCase(HudConfig.INSTANCE.hudDisplayType) ? "ALWAYS" : "POPUP";
                HudConfig.save();
                this.screenShake = 0.8f;
                this.buildTabWidgets();
            });
            this.method_37063(btnPopup);
            this.registerHelp(btnPopup.method_46426(), btnPopup.method_46427(), btnPopup.method_25368(), btnPopup.method_25364(), "Показ карточки", "Всплывает при смене трека или видна всегда.", "Всплывающий не загромождает экран.");

            ModernButtonWidget btnMiniKaraoke = new ModernButtonWidget(contentX + colW + 4 + popupW + 4, contentY + 66, 18, 19, (class_2561)class_2561.method_43470(HudConfig.INSTANCE.hudMiniKaraoke ? "§a♪" : "§7♪"), b -> {
                HudConfig.INSTANCE.hudMiniKaraoke = !HudConfig.INSTANCE.hudMiniKaraoke;
                HudConfig.save();
                this.screenShake = 0.5f;
                this.triggerFeedback("§bМини-караоке в HUD: " + (HudConfig.INSTANCE.hudMiniKaraoke ? "§aВКЛ" : "§cВЫКЛ"), HudConfig.INSTANCE.hudMiniKaraoke ? 0x10B981 : 0xFF5F56);
                this.buildTabWidgets();
            });
            btnMiniKaraoke.setCustomBgColor(HudConfig.INSTANCE.hudMiniKaraoke ? 0xDD0D221D : 0xDD182232);
            this.method_37063(btnMiniKaraoke);
            this.registerHelp(btnMiniKaraoke.method_46426(), btnMiniKaraoke.method_46427(), btnMiniKaraoke.method_25368(), btnMiniKaraoke.method_25364(), "Мини-караоке в карточке", "Показывает живую караоке-строку прямо внутри виджета трека.", "ВКЛ — удобно следить за словами с первого взгляда.");

            int sliderW = this.panelWidth - (this.dockSide != 0 ? 16 : 20);
            int curSliderY = contentY + 89;
            if (!HudConfig.INSTANCE.customBackgroundFile.isEmpty()) {
                ModernSliderWidget sCustomAlpha = new ModernSliderWidget(contentX, curSliderY, sliderW, 16, "Фон карт.: ", HudConfig.INSTANCE.customBackgroundAlpha, val -> String.format("%d%%", (int)(val * 100.0)), val -> {
                    HudConfig.INSTANCE.customBackgroundAlpha = val.floatValue();
                }).setOnRelease(HudConfig::save);
                this.method_37063(sCustomAlpha);
                this.registerHelp(sCustomAlpha.method_46426(), sCustomAlpha.method_46427(), sCustomAlpha.method_25368(), sCustomAlpha.method_25364(), "Прозрачность картинки", "Плотность картинки фона от 0 до 100%.", "Позволяет сделать текстуру полупрозрачной.");
                curSliderY += 19;
            }
            ModernSliderWidget sAlpha = new ModernSliderWidget(contentX, curSliderY, sliderW, 16, "Прозрачность: ", HudConfig.INSTANCE.hudBackgroundAlpha, val -> String.format("%d%%", (int)(val * 100.0)), val -> {
                HudConfig.INSTANCE.hudBackgroundAlpha = val.floatValue();
            }).setOnRelease(HudConfig::save);
            this.method_37063(sAlpha);
            this.registerHelp(sAlpha.method_46426(), sAlpha.method_46427(), sAlpha.method_25368(), sAlpha.method_25364(), "Прозрачность", "Плотность фона карточки от 0 до 100%.", "80% - 90% дают глубокий матовый оникс.");
            curSliderY += 19;
            ModernSliderWidget sRad = new ModernSliderWidget(contentX, curSliderY, sliderW, 16, "Скругление: ", (double)HudConfig.INSTANCE.cornerRadius / 20.0, val -> String.format("%d px", (int)Math.round(val * 20.0)), val -> {
                HudConfig.INSTANCE.cornerRadius = (int)Math.round(val * 20.0);
            }).setOnRelease(HudConfig::save);
            this.method_37063(sRad);
            this.registerHelp(sRad.method_46426(), sRad.method_46427(), sRad.method_25368(), sRad.method_25364(), "Скругление углов", "Плавность углов карточки от 0 до 20 пикселей.", "10 - 14 px выглядит наиболее аккуратно.");
            curSliderY += 19;
            int resetBtnW = 54;
            int scaleSliderW = sliderW - resetBtnW - 4;
            double initialScaleNorm = ((double)HudConfig.INSTANCE.hudScale - 0.2) / 3.8;
            ModernSliderWidget sScale = new ModernSliderWidget(contentX, curSliderY, scaleSliderW, 16, "Масштаб: ", initialScaleNorm, val -> String.format("%.2fx", 0.2 + val * 3.8), val -> {
                float newScale = (float)(0.2 + val * 3.8);
                HudConfig.INSTANCE.hudScale = (float)Math.round(newScale * 20.0f) / 20.0f;
                this.recalculateHudDimensions();
            }).setOnRelease(HudConfig::save);
            this.method_37063(sScale);
            this.registerHelp(sScale.method_46426(), sScale.method_46427(), sScale.method_25368(), sScale.method_25364(), "Масштаб", "Размер карточки на экране (от 0.2x до 4.0x).", "1.0x - 1.1x оптимально для игры.");

            ModernButtonWidget btnReset = new ModernButtonWidget(contentX + scaleSliderW + 4, curSliderY, resetBtnW, 16, (class_2561)class_2561.method_43470("§7Сброс"), b -> {
                HudConfig.INSTANCE.resetToDefault();
                this.recalculateHudDimensions();
                this.hudX = HudConfig.INSTANCE.getAbsoluteX(this.field_22789, this.hudWidth);
                this.hudY = HudConfig.INSTANCE.getAbsoluteY(this.field_22790, this.hudHeight);
                this.initColorWheels();
                HudConfig.save();
                this.screenShake = 1.2f;
                this.buildTabWidgets();
            });
            this.method_37063(btnReset);
            this.registerHelp(btnReset.method_46426(), btnReset.method_46427(), btnReset.method_25368(), btnReset.method_25364(), "Сброс вида", "Возврат стиля, размера, масштаба и позиции по умолчанию.", "Поможет, если карточка сместилась.");
        } else if (this.currentTab == 1) {
            String modeArrow = (this.activeDropdownId != null && "SUB_MODE".equals(this.activeDropdownId)) ? " §b▲" : " §7▼";
            ModernButtonWidget btnMode = new ModernButtonWidget(contentX, contentY, colW, 19, (class_2561)class_2561.method_43470("Режим: " + this.getSubtitlesModeName() + modeArrow), b -> {
                List<DropdownItem> items = new ArrayList<DropdownItem>();
                for (String[] opt : MODE_OPTIONS) {
                    items.add(new DropdownItem(opt[0], opt[1], opt[2]));
                }
                this.openDropdown("SUB_MODE", contentX, contentY, colW, items, () -> {
                    if (!HudConfig.INSTANCE.subtitlesEnabled) {
                        return "OFF";
                    }
                    return HudConfig.INSTANCE.subtitlesMode;
                }, chosenKey -> {
                    if ("OFF".equalsIgnoreCase(chosenKey)) {
                        HudConfig.INSTANCE.subtitlesEnabled = false;
                    } else {
                        HudConfig.INSTANCE.subtitlesEnabled = true;
                        HudConfig.INSTANCE.subtitlesMode = chosenKey;
                    }
                    HudConfig.save();
                    this.screenShake = 0.5f;
                    this.triggerFeedback("§b✦ Режим: §f" + this.getSubtitlesModeName(), 58879);
                    this.buildTabWidgets();
                });
                this.buildTabWidgets();
            });
            this.method_37063(btnMode);
            this.registerHelp(btnMode.method_46426(), btnMode.method_46427(), btnMode.method_25368(), btnMode.method_25364(), "Режим субтитров", "Выбор режима отображения из выпадающего списка.", "2D экран для субтитров внизу, 3D для мира, или ВЫКЛ.");
            int btnAnimX = contentX + colW + 4;
            int btnAnimY = contentY;
            String animArrow = (this.activeDropdownId != null && "ANIMATION".equals(this.activeDropdownId)) ? " §b▲" : " §7▼";
            String animBtnText = "Анимация: §b" + this.getAnimationName() + animArrow;
            ModernButtonWidget btnAnim = new ModernButtonWidget(btnAnimX, btnAnimY, colW, 19, (class_2561)class_2561.method_43470(animBtnText), b -> {
                List<DropdownItem> items = new ArrayList<DropdownItem>();
                for (String[] opt : ANIMATION_OPTIONS) {
                    items.add(new DropdownItem(opt[0], opt[1], opt[2]));
                }
                this.openDropdown("ANIMATION", btnAnimX, btnAnimY, colW, items, () -> {
                    String cur = HudConfig.INSTANCE.subtitlesAnimation;
                    return cur != null ? cur : "KARAOKE_BOUNCE";
                }, chosenKey -> {
                    HudConfig.INSTANCE.subtitlesAnimation = chosenKey;
                    HudConfig.save();
                    this.screenShake = 0.5f;
                    this.triggerFeedback("§b✦ Анимация: §f" + this.getAnimationName(), 58879);
                    this.buildTabWidgets();
                });
                this.buildTabWidgets();
            });
            this.method_37063(btnAnim);
            this.registerHelp(btnAnim.method_46426(), btnAnim.method_46427(), btnAnim.method_25368(), btnAnim.method_25364(), "Анимация слов", "Нажмите для выбора эффекта из выпадающего списка.", "Караоке, Волна, MiSide и другие анимации.");
            ModernButtonWidget btnOccl = new ModernButtonWidget(contentX, contentY + 22, colW, 19, (class_2561)class_2561.method_43470("За мобами: " + (HudConfig.INSTANCE.subtitlesSeeThrough ? "§eСквозь" : "§aСкрывать")), b -> {
                HudConfig.INSTANCE.subtitlesSeeThrough = !HudConfig.INSTANCE.subtitlesSeeThrough;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(btnOccl);
            this.registerHelp(btnOccl.method_46426(), btnOccl.method_46427(), btnOccl.method_25368(), btnOccl.method_25364(), "Перекрытие мобами", "Прятать 3D-субтитры за мобами и игроками.", "Скрывать делает 3D-текст естественным.");
            ModernButtonWidget btnBgText = new ModernButtonWidget(contentX + colW + 4, contentY + 22, colW, 19, (class_2561)class_2561.method_43470("Фон текста: " + (HudConfig.INSTANCE.subtitlesBgEnabled ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.subtitlesBgEnabled = !HudConfig.INSTANCE.subtitlesBgEnabled;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(btnBgText);
            this.registerHelp(btnBgText.method_46426(), btnBgText.method_46427(), btnBgText.method_25368(), btnBgText.method_25364(), "Подложка текста", "Тёмный фон за словами для лучшей читаемости.", "Буквы не теряются на светлом фоне.");
            ModernButtonWidget btnShadow = new ModernButtonWidget(contentX, contentY + 44, colW, 19, (class_2561)class_2561.method_43470("Тень текста: " + (HudConfig.INSTANCE.subtitlesTextShadow ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.subtitlesTextShadow = !HudConfig.INSTANCE.subtitlesTextShadow;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(btnShadow);
            this.registerHelp(btnShadow.method_46426(), btnShadow.method_46427(), btnShadow.method_25368(), btnShadow.method_25364(), "Тень букв", "Контурная тень под текстом.", "Делает текст чётким и читаемым.");
            ModernButtonWidget btnF1 = new ModernButtonWidget(contentX + colW + 4, contentY + 44, colW, 19, (class_2561)class_2561.method_43470("В F1: " + (HudConfig.INSTANCE.subtitlesVisibleInF1 ? "§aВКЛ" : "§7ВЫКЛ")), b -> {
                HudConfig.INSTANCE.subtitlesVisibleInF1 = !HudConfig.INSTANCE.subtitlesVisibleInF1;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(btnF1);
            this.registerHelp(btnF1.method_46426(), btnF1.method_46427(), btnF1.method_25368(), btnF1.method_25364(), "Показ при F1", "Оставляет субтитры при скрытом интерфейсе.", "Удобно для красивых скриншотов и видео.");
            int fullW = this.panelWidth - (this.dockSide != 0 ? 16 : 20);
            boolean is3DMode = "WORLD_3D".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode);
            if (is3DMode) {
                ModernButtonWidget btnSprint = new ModernButtonWidget(contentX, contentY + 66, colW, 19, (class_2561)class_2561.method_43470("При беге: " + (HudConfig.INSTANCE.sprintWordsEnabled ? "§a1-2 сл." : "§7Строка")), b -> {
                    HudConfig.INSTANCE.sprintWordsEnabled = !HudConfig.INSTANCE.sprintWordsEnabled;
                    HudConfig.save();
                    this.buildTabWidgets();
                });
                this.method_37063(btnSprint);
                this.registerHelp(btnSprint.method_46426(), btnSprint.method_46427(), btnSprint.method_25368(), btnSprint.method_25364(), "Слова при беге", "Показ слов короткими порциями во время спринта.", "Очень эффектно смотрится на бегу.");
                int halfColW = (colW - 4) / 2;
                ModernButtonWidget btnSprintSize = new ModernButtonWidget(contentX + colW + 4, contentY + 66, halfColW, 19, (class_2561)class_2561.method_43470("Порц: §b" + HudConfig.INSTANCE.sprintWordsChunkSize), b -> {
                    HudConfig.INSTANCE.sprintWordsChunkSize = HudConfig.INSTANCE.sprintWordsChunkSize == 1 ? 2 : 1;
                    HudConfig.save();
                    this.buildTabWidgets();
                });
                this.method_37063(btnSprintSize);
                this.registerHelp(btnSprintSize.method_46426(), btnSprintSize.method_46427(), btnSprintSize.method_25368(), btnSprintSize.method_25364(), "Слов за раз", "Сколько слов показывать на бегу: 1 или 2.", "2 слова читаются комфортнее всего.");
                int btnTrailX = contentX + colW + 4 + halfColW + 4;
                int btnTrailY = contentY + 66;
                String trailArrow = (this.activeDropdownId != null && "TRAIL".equals(this.activeDropdownId)) ? " §e▲" : " §7▼";
                ModernButtonWidget btnTrail = new ModernButtonWidget(btnTrailX, btnTrailY, halfColW, 19, (class_2561)class_2561.method_43470("Шлейф: §e" + HudConfig.INSTANCE.sprintWordsTrailLength + trailArrow), b -> {
                    List<DropdownItem> items = new ArrayList<DropdownItem>();
                    for (String[] opt : TRAIL_OPTIONS) {
                        items.add(new DropdownItem(opt[0], opt[1], opt[2]));
                    }
                    this.openDropdown("TRAIL", btnTrailX, btnTrailY, Math.max(halfColW, 92), items, () -> String.valueOf(HudConfig.INSTANCE.sprintWordsTrailLength), chosenKey -> {
                        try {
                            HudConfig.INSTANCE.sprintWordsTrailLength = Integer.parseInt(chosenKey);
                        } catch (Exception ignored) {
                        }
                        HudConfig.save();
                        this.screenShake = 0.5f;
                        this.triggerFeedback("§e✦ Длина шлейфа: §f" + HudConfig.INSTANCE.sprintWordsTrailLength + " сл.", 58879);
                        this.buildTabWidgets();
                    });
                    this.buildTabWidgets();
                });
                this.method_37063(btnTrail);
                this.registerHelp(btnTrail.method_46426(), btnTrail.method_46427(), btnTrail.method_25368(), btnTrail.method_25364(), "Шлейф слов", "Выбор количества парящих слов позади игрока (1-8).", "4-5 слов создают красивый след.");
                double subScaleNorm = ((double)HudConfig.INSTANCE.subtitlesScale - 0.5) / 2.5;
                ModernSliderWidget sSubScale = new ModernSliderWidget(contentX, contentY + 89, colW, 16, "Текст: ", Math.max(0.0, Math.min(1.0, subScaleNorm)), val -> String.format("%.1fx", 0.5 + val * 2.5), val -> {
                    HudConfig.INSTANCE.subtitlesScale = (float)(0.5 + val * 2.5);
                }).setOnRelease(HudConfig::save);
                this.method_37063(sSubScale);
                this.registerHelp(sSubScale.method_46426(), sSubScale.method_46427(), sSubScale.method_25368(), sSubScale.method_25364(), "Размер текста", "Масштаб шрифта субтитров от 0.5x до 3.0x.", "1.0x - 1.2x для лёгкого чтения.");
                double subDistNorm = ((double)HudConfig.INSTANCE.subtitlesDistance - 2.0) / 10.0;
                ModernSliderWidget sDist = new ModernSliderWidget(contentX + colW + 4, contentY + 89, colW, 16, "Дальность: ", Math.max(0.0, Math.min(1.0, subDistNorm)), val -> String.format("%.1f бл.", 2.0 + val * 10.0), val -> {
                    HudConfig.INSTANCE.subtitlesDistance = (float)(2.0 + val * 10.0);
                }).setOnRelease(HudConfig::save);
                this.method_37063(sDist);
                this.registerHelp(sDist.method_46426(), sDist.method_46427(), sDist.method_25368(), sDist.method_25364(), "Дистанция 3D", "Расстояние от глаз игрока до субтитров.", "4.5 - 5.5 блоков не мешают обзору.");
                double sprintDistNorm = ((double)HudConfig.INSTANCE.sprintWordsDistance - 1.5) / 10.5;
                ModernSliderWidget sSprintDist = new ModernSliderWidget(contentX, contentY + 108, colW, 16, "Дист. бега: ", Math.max(0.0, Math.min(1.0, sprintDistNorm)), val -> String.format("%.1f бл.", 1.5 + val * 10.5), val -> {
                    HudConfig.INSTANCE.sprintWordsDistance = (float)(1.5 + val * 10.5);
                }).setOnRelease(HudConfig::save);
                this.method_37063(sSprintDist);
                this.registerHelp(sSprintDist.method_46426(), sSprintDist.method_46427(), sSprintDist.method_25368(), sSprintDist.method_25364(), "Дистанция при беге", "Расстояние до порций слов во время спринта.", "3.0 - 4.0 блоков прямо перед глазами.");
                double sprintHeightNorm = ((double)HudConfig.INSTANCE.sprintWordsHeight + 1.0) / 2.0;
                ModernSliderWidget sSprintHeight = new ModernSliderWidget(contentX + colW + 4, contentY + 108, colW, 16, "Высота бега: ", Math.max(0.0, Math.min(1.0, sprintHeightNorm)), val -> String.format("%+.1f бл.", -1.0 + val * 2.0), val -> {
                    HudConfig.INSTANCE.sprintWordsHeight = (float)(-1.0 + val * 2.0);
                }).setOnRelease(HudConfig::save);
                this.method_37063(sSprintHeight);
                this.registerHelp(sSprintHeight.method_46426(), sSprintHeight.method_46427(), sSprintHeight.method_25368(), sSprintHeight.method_25364(), "Высота при беге", "Смещение слов выше или ниже уровня глаз.", "0.0 блоков держит слова по центру.");
            } else {
                ModernButtonWidget btnReset2D = new ModernButtonWidget(contentX, contentY + 66, fullW, 19, (class_2561)class_2561.method_43470("Сброс позиции 2D субтитров"), b -> {
                    HudConfig.INSTANCE.sub2DPositionX = 0.5;
                    HudConfig.INSTANCE.sub2DPositionY = 0.82;
                    HudConfig.save();
                    this.triggerFeedback("2D субтитры сброшены по центру", 58879);
                });
                this.method_37063(btnReset2D);
                this.registerHelp(btnReset2D.method_46426(), btnReset2D.method_46427(), btnReset2D.method_25368(), btnReset2D.method_25364(), "По центру", "Возвращает 2D-субтитры в центр экрана.", "Классическое положение внизу экрана.");
                double subScaleNorm = ((double)HudConfig.INSTANCE.subtitlesScale - 0.5) / 2.5;
                ModernSliderWidget sSubScale = new ModernSliderWidget(contentX, contentY + 89, fullW, 16, "Масштаб текста: ", Math.max(0.0, Math.min(1.0, subScaleNorm)), val -> String.format("%.1fx", 0.5 + val * 2.5), val -> {
                    HudConfig.INSTANCE.subtitlesScale = (float)(0.5 + val * 2.5);
                }).setOnRelease(HudConfig::save);
                this.method_37063(sSubScale);
                this.registerHelp(sSubScale.method_46426(), sSubScale.method_46427(), sSubScale.method_25368(), sSubScale.method_25364(), "Размер текста", "Масштаб 2D-субтитров на экране.", "1.0x - 1.2x комфортно для глаз.");
                double subDistNorm = ((double)HudConfig.INSTANCE.subtitlesDistance - 2.0) / 10.0;
                ModernSliderWidget sDist = new ModernSliderWidget(contentX, contentY + 108, fullW, 16, "Дальность 3D: ", Math.max(0.0, Math.min(1.0, subDistNorm)), val -> String.format("%.1f бл.", 2.0 + val * 10.0), val -> {
                    HudConfig.INSTANCE.subtitlesDistance = (float)(2.0 + val * 10.0);
                }).setOnRelease(HudConfig::save);
                this.method_37063(sDist);
                this.registerHelp(sDist.method_46426(), sDist.method_46427(), sDist.method_25368(), sDist.method_25364(), "Дистанция 3D", "Расстояние от глаз игрока до субтитров.", "4.5 - 5.5 блоков не закрывают обзор.");
            }
            double initialOffsetNorm = ((double)HudConfig.INSTANCE.subtitlesOffsetMs + 4000.0) / 8000.0;
            int syncSliderW = fullW - 96;
            ModernSliderWidget sSync = new ModernSliderWidget(contentX, contentY + 127, syncSliderW, 16, "Синхр: ", Math.max(0.0, Math.min(1.0, initialOffsetNorm)), val -> String.format("%+.1f с", -4.0 + val * 8.0), val -> {
                HudConfig.INSTANCE.subtitlesOffsetMs = (int)Math.round((-4.0 + val * 8.0) * 1000.0);
            }).setOnRelease(HudConfig::save);
            this.method_37063(sSync);
            this.registerHelp(sSync.method_46426(), sSync.method_46427(), sSync.method_25368(), sSync.method_25364(), "Синхронизация", "Сдвиг времени субтитров от -4 до +4 секунд.", "Для Bluetooth-наушников поставьте +0.5 сек.");
            ModernButtonWidget bM = new ModernButtonWidget(contentX + syncSliderW + 4, contentY + 127, 28, 16, (class_2561)class_2561.method_43470("-0.5"), b -> {
                HudConfig.INSTANCE.subtitlesOffsetMs = Math.max(-4000, HudConfig.INSTANCE.subtitlesOffsetMs - 500);
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(bM);
            this.registerHelp(bM.method_46426(), bM.method_46427(), bM.method_25368(), bM.method_25364(), "Раньше на 0.5с", "Показывает слова чуть раньше звука.", "Если субтитры отстают от музыки.");
            ModernButtonWidget bZ = new ModernButtonWidget(contentX + syncSliderW + 34, contentY + 127, 26, 16, (class_2561)class_2561.method_43470("0с"), b -> {
                HudConfig.INSTANCE.subtitlesOffsetMs = 0;
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(bZ);
            this.registerHelp(bZ.method_46426(), bZ.method_46427(), bZ.method_25368(), bZ.method_25364(), "Сброс сдвига", "Возвращает сдвиг времени на 0 секунд.", "Стандарт для колонок и проводных наушников.");
            ModernButtonWidget bP = new ModernButtonWidget(contentX + syncSliderW + 62, contentY + 127, 32, 16, (class_2561)class_2561.method_43470("+0.5"), b -> {
                HudConfig.INSTANCE.subtitlesOffsetMs = Math.min(4000, HudConfig.INSTANCE.subtitlesOffsetMs + 500);
                HudConfig.save();
                this.buildTabWidgets();
            });
            this.method_37063(bP);
            this.registerHelp(bP.method_46426(), bP.method_46427(), bP.method_25368(), bP.method_25364(), "Позже на 0.5с", "Показывает слова чуть позже звука.", "Удобно для беспроводных наушников.");
            int editorBtnY = contentY + 148;
            int halfBtnW = (fullW - 4) / 2;
            ModernButtonWidget btnLyricEditor = new ModernButtonWidget(contentX, editorBtnY, halfBtnW, 19, (class_2561)class_2561.method_43470("§bРедактор"), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new LyricEditorScreen(this));
                }
            });
            btnLyricEditor.setCustomBgColor(0xDD1B3348);
            this.method_37063(btnLyricEditor);
            this.registerHelp(btnLyricEditor.method_46426(), btnLyricEditor.method_46427(), btnLyricEditor.method_25368(), btnLyricEditor.method_25364(), "Редактор субтитров", "Страница всех слов и предложений песни с возможностью редактирования.", "Позволяет менять текст, тайминги и персональный стиль трека.");
            ModernButtonWidget btnFont = new ModernButtonWidget(contentX + halfBtnW + 4, editorBtnY, halfBtnW, 19, (class_2561)class_2561.method_43470("§eШрифт"), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new FontSelectorScreen(this, HudConfig.INSTANCE.subtitlesFont, string -> {
                        HudConfig.INSTANCE.subtitlesFont = string;
                        HudConfig.save();
                        this.buildTabWidgets();
                    }));
                }
            });
            btnFont.setCustomBgColor(0xDD283448);
            this.method_37063(btnFont);
            this.registerHelp(btnFont.method_46426(), btnFont.method_46427(), btnFont.method_25368(), btnFont.method_25364(), "Выбор шрифта", "Выбор шрифта субтитров с предпросмотром.", "Поддерживаются встроенные и пользовательские шрифты.");

            int liveBtnY = editorBtnY + 22;
            int w1 = (fullW - 8) * 32 / 100;
            int w2 = (fullW - 8) * 36 / 100;
            int w3 = fullW - w1 - w2 - 8;
            ModernButtonWidget btnLiveStt = new ModernButtonWidget(contentX, liveBtnY, w1, 18, (class_2561)class_2561.method_43470("Речь: " + (HudConfig.INSTANCE.liveSubtitlesEnabled ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.liveSubtitlesEnabled = !HudConfig.INSTANCE.liveSubtitlesEnabled;
                HudConfig.save();
                if (YandexMusicHudMod.INSTANCE != null && YandexMusicHudMod.INSTANCE.getLyricsManager() != null) {
                    YandexMusicHudMod.INSTANCE.getLyricsManager().onLiveSubtitlesToggled(HudConfig.INSTANCE.liveSubtitlesEnabled);
                }
                b.method_25355((class_2561)class_2561.method_43470("Речь: " + (HudConfig.INSTANCE.liveSubtitlesEnabled ? "§aВКЛ" : "§cВЫКЛ")));
                b.setCustomBgColor(HudConfig.INSTANCE.liveSubtitlesEnabled ? -585408992 : -578093056);
                this.triggerFeedback(HudConfig.INSTANCE.liveSubtitlesEnabled ? "§aРаспознавание речи: ВКЛ" : "§cРаспознавание речи: ВЫКЛ", HudConfig.INSTANCE.liveSubtitlesEnabled ? 58879 : 0xFF5F56);
            });
            btnLiveStt.setCustomBgColor(HudConfig.INSTANCE.liveSubtitlesEnabled ? -585408992 : -578093056);
            this.method_37063(btnLiveStt);
            this.registerHelp(btnLiveStt.method_46426(), btnLiveStt.method_46427(), btnLiveStt.method_25368(), btnLiveStt.method_25364(), "Распознавание речи", "Включает или полностью отключает живое распознавание (STT).", "Отключите, если не хотите субтитры по звуку без готового текста.");

            ModernButtonWidget btnLiveWord = new ModernButtonWidget(contentX + w1 + 4, liveBtnY, w2, 18, (class_2561)class_2561.method_43470("Слова: " + (HudConfig.INSTANCE.liveWordByWord ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.liveWordByWord = !HudConfig.INSTANCE.liveWordByWord;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470("Слова: " + (HudConfig.INSTANCE.liveWordByWord ? "§aВКЛ" : "§cВЫКЛ")));
                this.triggerFeedback(HudConfig.INSTANCE.liveWordByWord ? "§aПоказ по словам (Live): ВКЛ" : "§7Показ фразами (Live)", 58879);
            });
            btnLiveWord.setCustomBgColor(0xDD283448);
            this.method_37063(btnLiveWord);
            this.registerHelp(btnLiveWord.method_46426(), btnLiveWord.method_46427(), btnLiveWord.method_25368(), btnLiveWord.method_25364(), "По словам в реальном времени", "Выводит каждое распознанное слово по очереди в реальном времени.", "При ВЫКЛ показывает сразу целую фразу.");

            String curEngine = HudConfig.INSTANCE.liveSttEngine != null ? HudConfig.INSTANCE.liveSttEngine.toUpperCase(Locale.ROOT) : "AUTO";
            ModernButtonWidget btnLiveEngine = new ModernButtonWidget(contentX + w1 + 4 + w2 + 4, liveBtnY, w3, 18, (class_2561)class_2561.method_43470(HudEditScreen.formatEngineLabel(curEngine)), b -> {
                String c = HudConfig.INSTANCE.liveSttEngine != null ? HudConfig.INSTANCE.liveSttEngine.toUpperCase(Locale.ROOT) : "AUTO";
                String next = "AUTO".equals(c) ? "WHISPER" : ("WHISPER".equals(c) ? "GOOGLE" : "AUTO");
                HudConfig.INSTANCE.liveSttEngine = next;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470(HudEditScreen.formatEngineLabel(next)));
                if ("GOOGLE".equals(next) && YandexMusicHudMod.INSTANCE != null && YandexMusicHudMod.INSTANCE.getLyricsManager() != null) {
                    YandexMusicHudMod.INSTANCE.getLyricsManager().onLiveSubtitlesToggled(HudConfig.INSTANCE.liveSubtitlesEnabled);
                }
                this.triggerFeedback("§bДвижок распознавания: §f" + next, 58879);
            });
            btnLiveEngine.setCustomBgColor(0xDD223044);
            this.method_37063(btnLiveEngine);
            this.registerHelp(btnLiveEngine.method_46426(), btnLiveEngine.method_46427(), btnLiveEngine.method_25368(), btnLiveEngine.method_25364(), "Движок STT", "Авто: локальный Whisper + Google; Whisper: только локально; Google: облако.", "Авто даёт максимальную точность на музыке с вокалом.");
        } else if (this.currentTab == 2) {
            int pillW = (this.panelWidth - (this.dockSide != 0 ? 16 : 20) - 20) / 6;
            String[] targetNames = new String[]{"Конфиг", "Фон HUD", "Полоса", "Текст", "Подложка", "Караоке"};
            for (int i = 0; i < 6; ++i) {
                int tIdx = i;
                ModernButtonWidget btn = new ModernButtonWidget(contentX + i * (pillW + 4), contentY, pillW, 18, (class_2561)class_2561.method_43470(targetNames[i]), b -> {
                    this.paletteTarget = tIdx;
                    this.screenShake = 0.0f;
                    if (this.paletteColorWheel != null) {
                        this.paletteColorWheel.setColorFromRgb(this.getCurrentTargetColor());
                    }
                    this.buildTabWidgets();
                });
                btn.setPrimary(this.paletteTarget == tIdx);
                this.method_37063(btn);
            }
            int wheelS = this.dockSide != 0 ? 68 : 80;
            int offsetResetX = wheelS + 12;
            int resetW = this.panelWidth - (this.dockSide != 0 ? 16 : 20) - offsetResetX;
            double curBr = this.paletteColorWheel != null ? (double)this.paletteColorWheel.getBrightness() : 1.0;
            ModernSliderWidget sliderBr = new ModernSliderWidget(contentX + offsetResetX, contentY + 50, resetW, 18, "Яркость: ", curBr, val -> Math.round(val * 100.0) + "%", val -> {
                if (this.paletteColorWheel != null) {
                    this.paletteColorWheel.setBrightness(val.floatValue());
                    this.setCurrentTargetColor(this.paletteColorWheel.getRgb());
                }
            });
            sliderBr.setOnRelease(() -> HudConfig.save());
            this.method_37063(sliderBr);

            ModernButtonWidget btnResetCol = new ModernButtonWidget(contentX + offsetResetX, contentY + 74, resetW, 18, (class_2561)class_2561.method_43470("Сбросить цвет"), b -> {
                switch (this.paletteTarget) {
                    case 0 -> HudConfig.INSTANCE.configBackgroundColor = 0x0A0D14;
                    case 1 -> HudConfig.INSTANCE.hudBackgroundColor = 0x0A0D14;
                    case 2 -> HudConfig.INSTANCE.progressBarColor = -13312;
                    case 3 -> HudConfig.INSTANCE.subtitlesColor = -1;
                    case 4 -> HudConfig.INSTANCE.subtitlesBgColor = 0x11111E;
                    case 5 -> HudConfig.INSTANCE.subtitlesActiveColor = -10496;
                }
                this.initColorWheels();
                HudConfig.save();
                this.screenShake = 1.0f;
            });
            this.method_37063(btnResetCol);

            int[] presetRgbs = new int[]{0x38BDF8, 0x10B981, 0xA855F7, 0xF59E0B, 0xF43F5E, 0xF8FAFC};
            String[] presetNames = new String[]{"Неон", "Изумруд", "Аметист", "Золото", "Рубин", "Белый"};
            int presetGap = 3;
            int presetW = Math.max(16, (resetW - presetGap * 5) / 6);
            int curTargetRgb = this.getCurrentTargetColor() & 0xFFFFFF;
            for (int pi = 0; pi < presetRgbs.length; ++pi) {
                final int presetRgb = presetRgbs[pi];
                final String presetName = presetNames[pi];
                boolean isCurPreset = curTargetRgb == presetRgb;
                String swatchMark = isCurPreset ? (presetRgb == 0xF8FAFC || presetRgb == 0xF59E0B ? "§0✓" : "§f✓") : "";
                int px = contentX + offsetResetX + pi * (presetW + presetGap);
                int py = contentY + 96;
                ModernButtonWidget btnPreset = new ModernButtonWidget(px, py, presetW, 16, (class_2561)class_2561.method_43470(swatchMark), b -> {
                    this.setCurrentTargetColor(presetRgb);
                    if (this.paletteColorWheel != null) {
                        this.paletteColorWheel.setColorFromRgb(presetRgb);
                    }
                    HudConfig.save();
                    String hexStr = Integer.toHexString(0x1000000 | (presetRgb & 0xFFFFFF)).substring(1).toUpperCase(java.util.Locale.ROOT);
                    this.triggerFeedback("§bПресет «" + presetName + "»: §e#" + hexStr, presetRgb);
                    this.screenShake = 0.4f;
                    this.buildTabWidgets();
                });
                btnPreset.setCustomBgColor(0xFF000000 | presetRgb);
                this.method_37063(btnPreset);
                this.registerHelp(px, py, presetW, 16, "Пресет: " + presetName, "Быстрая установка оттенка для выбранного элемента.", "Нажмите, чтобы применить цвет и обновить круг.");
            }
        } else if (this.currentTab == 3) {
            int leftCardW = this.dockSide != 0 ? 100 : 120;
            int rightCardX = contentX + leftCardW + 6;
            int rightCardW = this.panelWidth - (this.dockSide != 0 ? 16 : 20) - leftCardW - 6;
            int btnY = contentY;
            ModernButtonWidget btnDucking = new ModernButtonWidget(rightCardX, btnY, rightCardW, 17, (class_2561)class_2561.method_43470("Приглушение: " + (HudConfig.INSTANCE.smartDuckingEnabled ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.smartDuckingEnabled = !HudConfig.INSTANCE.smartDuckingEnabled;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470("Приглушение: " + (HudConfig.INSTANCE.smartDuckingEnabled ? "§aВКЛ" : "§cВЫКЛ")));
                this.triggerFeedback(HudConfig.INSTANCE.smartDuckingEnabled ? "§aПриглушение музыки: ВКЛ" : "§cПриглушение музыки: ВЫКЛ", 58879);
            });
            this.method_37063(btnDucking);
            this.registerHelp(btnDucking.method_46426(), btnDucking.method_46427(), btnDucking.method_25368(), btnDucking.method_25364(), "Умное приглушение", "Приглушает музыку во время разговора в Simple Voice Chat.", "Помогает четко слышать голос тиммейтов.");
            ModernButtonWidget btnAudioAnalysis = new ModernButtonWidget(rightCardX, btnY += 20, rightCardW, 17, (class_2561)class_2561.method_43470("Анализ звука: " + (HudConfig.INSTANCE.audioAnalysisEnabled ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.audioAnalysisEnabled = !HudConfig.INSTANCE.audioAnalysisEnabled;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470("Анализ звука: " + (HudConfig.INSTANCE.audioAnalysisEnabled ? "§aВКЛ" : "§cВЫКЛ")));
                this.triggerFeedback(HudConfig.INSTANCE.audioAnalysisEnabled ? "§aАнализ звука: ВКЛ" : "§cАнализ звука: ВЫКЛ", 58879);
            });
            this.method_37063(btnAudioAnalysis);
            this.registerHelp(btnAudioAnalysis.method_46426(), btnAudioAnalysis.method_46427(), btnAudioAnalysis.method_25368(), btnAudioAnalysis.method_25364(), "Анализ звука", "Запись короткого фрагмента системного звука для выравнивания текста без таймкодов.", "Выключите, если не хотите захват системного звука.");
            ModernButtonWidget btnShare = new ModernButtonWidget(rightCardX, btnY += 20, rightCardW, 17, (class_2561)class_2561.method_43470("§bОтправить в чат"), b -> {
                boolean sent = SessionStatsManager.INSTANCE.shareStatsToChat();
                if (sent) {
                    this.triggerFeedback("§aСтатистика отправлена в чат!", 65416);
                } else {
                    this.triggerFeedback("§cОтправка доступна только в мире!", 0xFF5555);
                }
            });
            btnShare.setCustomBgColor(0xDD007AFF);
            this.method_37063(btnShare);
            this.registerHelp(btnShare.method_46426(), btnShare.method_46427(), btnShare.method_25368(), btnShare.method_25364(), "Поделиться в чат", "Отправляет статистику сессии в игровой чат.", "Покажите друзьям сколько треков вы прослушали.");
            ModernButtonWidget btnPulseSync = new ModernButtonWidget(rightCardX, btnY += 20, rightCardW, 17, (class_2561)class_2561.method_43470("PulseSync: " + (HudConfig.INSTANCE.pulseSyncIntegration ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.pulseSyncIntegration = !HudConfig.INSTANCE.pulseSyncIntegration;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470("PulseSync: " + (HudConfig.INSTANCE.pulseSyncIntegration ? "§aВКЛ" : "§cВЫКЛ")));
                this.triggerFeedback(HudConfig.INSTANCE.pulseSyncIntegration ? "§aPulseSync интеграция: ВКЛ" : "§cPulseSync интеграция: ВЫКЛ", 58879);
            });
            this.method_37063(btnPulseSync);
            this.registerHelp(btnPulseSync.method_46426(), btnPulseSync.method_46427(), btnPulseSync.method_25368(), btnPulseSync.method_25364(), "PulseSync интеграция", "Прямой перехват Яндекс Музыки через PulseSync.", "Идеальные тайминги без задержек SMTC.");
            ModernButtonWidget btnLocalMusic = new ModernButtonWidget(rightCardX, btnY += 20, rightCardW, 17, (class_2561)class_2561.method_43470("§bМоя музыка (Плеер)"), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new LocalMusicScreen(this));
                }
            });
            btnLocalMusic.setCustomBgColor(0xDD00A3C4);
            this.method_37063(btnLocalMusic);
            this.registerHelp(btnLocalMusic.method_46426(), btnLocalMusic.method_46427(), btnLocalMusic.method_25368(), btnLocalMusic.method_25364(), "Моя музыка", "Встроенный плеер локальных аудиофайлов (.mp3, .wav).", "Играйте музыку прямо из игры без браузера!");
            ModernButtonWidget btnYmSettings = new ModernButtonWidget(rightCardX, btnY += 20, rightCardW, 18, (class_2561)class_2561.method_43470("§6Яндекс Музыка API"), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new YandexMusicSettingsScreen(this));
                }
            });
            btnYmSettings.setCustomBgColor(-585419960);
            this.method_37063(btnYmSettings);
            this.registerHelp(btnYmSettings.method_46426(), btnYmSettings.method_46427(), btnYmSettings.method_25368(), btnYmSettings.method_25364(), "Яндекс Музыка API", "Настройка токена Яндекс, PulseSync и подключения.", "Позволяет получать тексты и управлять музыкой напрямую.");

            ModernButtonWidget btnTabBadge = new ModernButtonWidget(rightCardX, btnY += 20, rightCardW, 17, (class_2561)class_2561.method_43470("Иконка в табе: " + (HudConfig.INSTANCE.tabBadgeEnabled ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.tabBadgeEnabled = !HudConfig.INSTANCE.tabBadgeEnabled;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470("Иконка в табе: " + (HudConfig.INSTANCE.tabBadgeEnabled ? "§aВКЛ" : "§cВЫКЛ")));
                this.triggerFeedback(HudConfig.INSTANCE.tabBadgeEnabled ? "§aИконка мода в табе: ВКЛ" : "§cИконка в табе: ВЫКЛ", 58879);
            });
            this.method_37063(btnTabBadge);
            this.registerHelp(btnTabBadge.method_46426(), btnTabBadge.method_46427(), btnTabBadge.method_25368(), btnTabBadge.method_25364(), "Иконка мода в табе", "Отображает цветную иконку мода перед ником в списке игроков (Tab).", "Совместимо с плагинами таба и серверами.");

            int roomBtnW = (rightCardW - 2) / 2;
            boolean inRoom = PlayerBadgeManager.INSTANCE.isInRoom();
            String roomText = inRoom ? ("§e♫ " + PlayerBadgeManager.INSTANCE.getCurrentRoom()) : "§b♫ Комната";
            ModernButtonWidget btnSyncRoom = new ModernButtonWidget(rightCardX, btnY += 20, roomBtnW, 17, (class_2561)class_2561.method_43470(roomText), b -> {
                if (this.field_22787 != null) {
                    this.field_22787.method_1507((class_437)new MusicSyncScreen(this));
                }
            });
            btnSyncRoom.setCustomBgColor(inRoom ? 0xDD9333EA : 0xDD0D9488);
            this.method_37063(btnSyncRoom);
            this.registerHelp(btnSyncRoom.method_46426(), btnSyncRoom.method_46427(), btnSyncRoom.method_25368(), btnSyncRoom.method_25364(), "Слушать вместе", "Открыть меню совместного прослушивания (Music Sync).", "Создание комнаты, подключение друзей, участники и диагностика.");

            ModernButtonWidget btnSafeMode = new ModernButtonWidget(rightCardX + roomBtnW + 2, btnY, roomBtnW, 17, (class_2561)class_2561.method_43470("Стрим: " + (HudConfig.INSTANCE.safeMode ? "§aВКЛ" : "§cВЫКЛ")), b -> {
                HudConfig.INSTANCE.safeMode = !HudConfig.INSTANCE.safeMode;
                HudConfig.save();
                b.method_25355((class_2561)class_2561.method_43470("Стрим: " + (HudConfig.INSTANCE.safeMode ? "§aВКЛ" : "§cВЫКЛ")));
                this.triggerFeedback(HudConfig.INSTANCE.safeMode ? "§aБезопасный режим: ВКЛ" : "§cБезопасный режим: ВЫКЛ", 58879);
            });
            this.method_37063(btnSafeMode);
            this.registerHelp(btnSafeMode.method_46426(), btnSafeMode.method_46427(), btnSafeMode.method_25368(), btnSafeMode.method_25364(), "Безопасный режим", "Автоматический фоновый поиск трека по названию и авто-стриминг из сети.", "Позволяет слушать совместную музыку без предварительной загрузки треков.");
        }
    }

    private void openDropdown(String id, int btnX, int btnY, int btnW, List<DropdownItem> items, Supplier<String> selectedSupplier, Consumer<String> onSelect) {
        if (id != null && id.equals(this.activeDropdownId)) {
            this.closeDropdown();
            return;
        }
        this.activeDropdownId = id;
        this.activeDropdownX = btnX;
        this.activeDropdownBtnY = btnY;
        boolean hasPreviewBadge = "HUD_STYLE".equals(id) || "SUB_MODE".equals(id) || "ANIMATION".equals(id);
        this.activeDropdownW = hasPreviewBadge ? Math.max(btnW, 174) : btnW;
        this.activeDropdownItems = new ArrayList<DropdownItem>(items);
        this.activeDropdownSelectedKey = selectedSupplier;
        this.activeDropdownCallback = onSelect;
        this.activeDropdownScroll = 0;
        int count = this.activeDropdownItems.size();
        int maxVis = Math.min(count, Math.max(4, Math.min(8, (this.field_22790 - 40) / 18)));
        this.activeDropdownMaxVisible = maxVis;
        int dropH = maxVis * 18 + 6;
        this.activeDropdownH = dropH;
        if (btnY + 19 + dropH > this.field_22790 - 8) {
            this.activeDropdownY = Math.max(6, btnY - dropH - 2);
        } else {
            this.activeDropdownY = btnY + 19 + 2;
        }
        this.activeDropdownX = Math.max(4, Math.min(this.activeDropdownX, this.field_22789 - this.activeDropdownW - 6));
        this.playSound(class_3417.field_14709, 1.2f, 0.8f);
    }

    private void closeDropdown() {
        this.activeDropdownId = null;
        this.activeDropdownItems.clear();
        this.activeDropdownCallback = null;
        this.activeDropdownSelectedKey = null;
        this.activeDropdownScroll = 0;
    }

    private String getHudStyleName() {
        String s = HudConfig.INSTANCE.hudStyle;
        if (s == null) {
            return "Тёмный мин.";
        }
        return switch (s.toUpperCase(java.util.Locale.ROOT)) {
            case "GLASS_AMBIENT" -> "Стекло";
            case "VINYL_DECK" -> "Винил-дека";
            case "DYNAMIC_ISLAND" -> "Остров";
            default -> "Тёмный мин.";
        };
    }

    private String getAnimationName() {
        String a = HudConfig.INSTANCE.subtitlesAnimation;
        if (a == null) {
            return "Караоке";
        }
        return switch (a.toUpperCase(java.util.Locale.ROOT)) {
            case "WAVE_FLOAT" -> "Волна";
            case "ELASTIC_POP" -> "Пружина";
            case "PULSE_GLOW" -> "Пульс";
            case "SLIDE_FADE" -> "Слайд";
            case "CLASSIC" -> "Классика";
            case "MISIDE_FALL", "MISIDE" -> "MiSide";
            default -> "Караоке";
        };
    }

    private void cycleAnimationMode() {
        String a = HudConfig.INSTANCE.subtitlesAnimation;
        if (a == null) {
            a = "KARAOKE_BOUNCE";
        }
        HudConfig.INSTANCE.subtitlesAnimation = switch (a.toUpperCase(java.util.Locale.ROOT)) {
            case "KARAOKE_BOUNCE" -> "WAVE_FLOAT";
            case "WAVE_FLOAT" -> "ELASTIC_POP";
            case "ELASTIC_POP" -> "PULSE_GLOW";
            case "PULSE_GLOW" -> "SLIDE_FADE";
            case "SLIDE_FADE" -> "CLASSIC";
            case "CLASSIC" -> "MISIDE_FALL";
            case "MISIDE_FALL", "MISIDE" -> "KARAOKE_BOUNCE";
            default -> "KARAOKE_BOUNCE";
        };
        HudConfig.save();
    }

    private String getSubtitlesModeName() {
        if (!HudConfig.INSTANCE.subtitlesEnabled) {
            return "§cВЫКЛ";
        }
        return switch (HudConfig.INSTANCE.subtitlesMode) {
            case "WORLD_3D" -> "3D в мире";
            case "WORLD_BLOCK" -> "На блоке";
            case "HUD_BOTTOM" -> "2D экран";
            case "SCREEN_RANDOM" -> "2D разброс";
            case "CHAT" -> "В чате";
            default -> "3D в мире";
        };
    }

    private void cycleSubtitlesMode() {
        if (!HudConfig.INSTANCE.subtitlesEnabled) {
            HudConfig.INSTANCE.subtitlesEnabled = true;
            HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
        } else {
            switch (HudConfig.INSTANCE.subtitlesMode) {
                case "WORLD_3D" -> HudConfig.INSTANCE.subtitlesMode = "WORLD_BLOCK";
                case "WORLD_BLOCK" -> HudConfig.INSTANCE.subtitlesMode = "HUD_BOTTOM";
                case "HUD_BOTTOM" -> HudConfig.INSTANCE.subtitlesMode = "SCREEN_RANDOM";
                case "SCREEN_RANDOM" -> HudConfig.INSTANCE.subtitlesMode = "CHAT";
                case "CHAT" -> {
                    HudConfig.INSTANCE.subtitlesEnabled = false;
                    HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
                }
                default -> HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
            }
        }
    }

    private void cyclePngBackground() {
        this.refreshBackgroundList();
        if (this.availablePngBackgrounds.isEmpty()) {
            HudConfig.INSTANCE.customBackgroundFile = "";
            return;
        }
        int curIdx = -1;
        for (int i = 0; i < this.availablePngBackgrounds.size(); ++i) {
            if (!this.availablePngBackgrounds.get(i).equalsIgnoreCase(HudConfig.INSTANCE.customBackgroundFile)) continue;
            curIdx = i;
            break;
        }
        HudConfig.INSTANCE.customBackgroundFile = curIdx == -1 ? this.availablePngBackgrounds.get(0) : (curIdx + 1 < this.availablePngBackgrounds.size() ? this.availablePngBackgrounds.get(curIdx + 1) : "");
        CustomBackgroundManager.INSTANCE.reload();
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

    private void spawnCursorSparkle(int mx, int my) {
        Random r = new Random();
        float angle = (float)((double)r.nextFloat() * Math.PI * 2.0);
        float spd = 0.4f + r.nextFloat() * 1.5f;
        float vx = (float)Math.cos(angle) * spd;
        float vy = (float)Math.sin(angle) * spd - 0.2f;
        String sym = switch (r.nextInt(5)) {
            case 0 -> "✦";
            case 1 -> "·";
            case 2 -> "✧";
            case 3 -> "♪";
            default -> "♥";
        };
        int col = switch (r.nextInt(5)) {
            case 0 -> -16718337;
            case 1 -> -10496;
            case 2 -> -49023;
            case 3 -> -2080517;
            default -> -1;
        };
        this.cursorSparkles.add(new CursorSparkle(mx, my, vx, vy, col, sym, 16 + r.nextInt(12)));
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        class_310 mc = class_310.method_1551();
        long nowMs = System.currentTimeMillis();
        float dtSec = this.lastRenderMs > 0L ? Math.min(0.1f, Math.max(0.001f, (float)(nowMs - this.lastRenderMs) / 1000.0f)) : 0.016f;
        this.lastRenderMs = nowMs;
        TrackState ts = NowPlayingClient.INSTANCE != null ? NowPlayingClient.INSTANCE.getState() : null;
        boolean isPlaying = ts != null && ts.isActive() && !"stopped".equalsIgnoreCase(ts.status()) && !"paused".equalsIgnoreCase(ts.status());
        float beatPhase = isPlaying ? (float)((double)(ts.getInterpolatedPositionMs() % 500L) / 500.0) : (float)((double)(nowMs % 600L) / 600.0);
        float beatEnergy = isPlaying ? (float)Math.pow(Math.max(0.0, 1.0 - (double)beatPhase * 1.8), 2.2) : (float)Math.sin((double)nowMs * 0.005) * 0.2f + 0.2f;
        if (isPlaying && HudConfig.INSTANCE.beatBounce && beatEnergy > 0.65f) {
            this.screenShake = Math.max(this.screenShake, beatEnergy * 1.8f);
        }
        this.screenShake *= (float)Math.pow(0.86, dtSec * 60.0f);
        float shakeX = (float)Math.sin((double)nowMs * 0.055) * this.screenShake;
        float shakeY = (float)Math.cos((double)nowMs * 0.075) * this.screenShake;
        float slamBounceX = 0.0f;
        long impactAge = nowMs - this.dockImpactTime;
        if (impactAge < 340L && this.dockSide != 0) {
            float t = (float)impactAge / 340.0f;
            float dampedSine = (float)(Math.sin(t * Math.PI * 3.2) * Math.exp(-t * 4.8));
            slamBounceX = (this.dockImpactSide == 1 ? 1.0f : -1.0f) * dampedSine * 15.0f;
        }
        float targetTiltX = class_3532.method_15363((float)(mouseX - this.field_22789 / 2) / ((float)this.field_22789 / 2.0f), -1.0f, 1.0f);
        float targetTiltY = class_3532.method_15363((float)(mouseY - this.field_22790 / 2) / ((float)this.field_22790 / 2.0f), -1.0f, 1.0f);
        float tiltLerp = Math.min(1.0f, dtSec * 7.5f);
        this.curTiltX += (targetTiltX - this.curTiltX) * tiltLerp;
        this.curTiltY += (targetTiltY - this.curTiltY) * tiltLerp;
        if (Math.abs(mouseX - this.lastMouseX) > 2 || Math.abs(mouseY - this.lastMouseY) > 2) {
            if (++this.sparkleTimer % 3 == 0) {
                this.spawnCursorSparkle(mouseX, mouseY);
            }
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        }
        if (this.draggingConfig) {
            long handle = mc.method_22683().method_4490();
            int mouseState = GLFW.glfwGetMouseButton(handle, 0);
            if (mouseState != 1) {
                this.draggingConfig = false;
            } else {
                int newX = mouseX - this.dragConfigOffsetX;
                int newY = mouseY - this.dragConfigOffsetY;
                newX = Math.max(0, Math.min(newX, this.field_22789 - this.panelWidth));
                newY = Math.max(0, Math.min(newY, this.field_22790 - this.panelHeight));
                if (newX != this.panelX || newY != this.panelY) {
                    this.panelX = newX;
                    this.panelY = newY;
                    this.initColorWheels();
                    this.buildTabWidgets();
                }
            }
        }
        if (this.dragging) {
            long handle = mc.method_22683().method_4490();
            int mouseState = GLFW.glfwGetMouseButton(handle, 0);
            if (mouseState != 1) {
                this.dragging = false;
                HudConfig.INSTANCE.setFromAbsolute(this.hudX, this.hudY, this.field_22789, this.field_22790, this.hudWidth, this.hudHeight);
                HudConfig.save();
            } else {
                this.hudX = mouseX - this.dragOffsetX;
                this.hudY = mouseY - this.dragOffsetY;
                this.hudX = Math.max(0, Math.min(this.hudX, this.field_22789 - this.hudWidth));
                this.hudY = Math.max(0, Math.min(this.hudY, this.field_22790 - this.hudHeight));
                int snapThreshold = 15;
                if (this.hudX < snapThreshold) {
                    this.hudX = 0;
                }
                if (this.field_22789 - (this.hudX + this.hudWidth) < snapThreshold) {
                    this.hudX = this.field_22789 - this.hudWidth;
                }
                if (this.hudY < snapThreshold) {
                    this.hudY = 0;
                }
                if (this.field_22790 - (this.hudY + this.hudHeight) < snapThreshold) {
                    this.hudY = this.field_22790 - this.hudHeight;
                }
            }
        }
        boolean is2DBottom = HudConfig.INSTANCE.subtitlesEnabled && !"WORLD_3D".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"WORLD_BLOCK".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"WORLD_SURFACE".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"3D_HEAD".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"CHAT".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"OFF".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode);
        if (this.dragging2DSub && is2DBottom) {
            long handle = mc.method_22683().method_4490();
            int mouseState = GLFW.glfwGetMouseButton(handle, 0);
            if (mouseState != 1) {
                this.dragging2DSub = false;
                HudConfig.save();
            } else {
                int newCenterX = mouseX - this.dragSubOffsetX;
                int newCenterY = mouseY - this.dragSubOffsetY;
                if (Math.abs(newCenterX - this.field_22789 / 2) < 16) {
                    newCenterX = this.field_22789 / 2;
                }
                HudConfig.INSTANCE.sub2DPositionX = class_3532.method_15350((double)newCenterX / (double)this.field_22789, 0.05, 0.95);
                HudConfig.INSTANCE.sub2DPositionY = class_3532.method_15350((double)newCenterY / (double)this.field_22790, 0.05, 0.95);
            }
        }
        if (this.dragging2DSub && is2DBottom) {
            this.renderLiveSubtitlePreview(context, mouseX, mouseY);
            return;
        }
        context.method_25296(0, 0, this.field_22789, 42, 0x3324A1DE, 0);
        context.method_25296(0, this.field_22790 - 46, this.field_22789, this.field_22790, 0, 0x66060A12);
        float openProg = Math.min(1.0f, (float)(nowMs - this.screenOpenTimestamp) / 160.0f);
        float openEase = (float)(1.0 - Math.pow(1.0 - openProg, 3.0));
        float openScale = 0.96f + 0.04f * openEase;
        float screenMidX = (float)this.field_22789 * 0.5f;
        float screenMidY = (float)this.field_22790 * 0.5f;
        context.method_51448().pushMatrix();
        context.method_51448().translate(screenMidX, screenMidY);
        context.method_51448().scale(openScale, openScale);
        context.method_51448().translate(-screenMidX, -screenMidY);
        context.method_51448().translate(shakeX + slamBounceX, shakeY);
        if (this.draggingConfig && this.dockSide == 0) {
            if (this.panelX <= 32) {
                MusicHudRenderer.renderRoundedRect(context, 0, 0, 4, this.field_22790, 0, 0xEE00E5FF);
                MusicHudRenderer.renderRoundedRect(context, 0, Math.max(10, this.panelY), 34, 226, 7, 0x4400E5FF);
            } else if (this.panelX + this.panelWidth >= this.field_22789 - 32) {
                MusicHudRenderer.renderRoundedRect(context, this.field_22789 - 4, 0, 4, this.field_22790, 0, 0xEE00E5FF);
                MusicHudRenderer.renderRoundedRect(context, this.field_22789 - 34, Math.max(10, this.panelY), 34, 226, 7, 0x4400E5FF);
            }
        }
        int shadowOffX = (int)(-this.curTiltX * 7.0f);
        int shadowOffY = (int)(6.0f - this.curTiltY * 5.0f);
        int curPanelX = this.panelX;
        int curPanelY = this.panelY;
        int hoveredDockIcon = -1;
        float dt = this.lastFrameTime > 0L ? Math.min(0.1f, (float)(nowMs - this.lastFrameTime) / 1000.0f) : 0.016f;
        this.lastFrameTime = nowMs;
        int slotH = 16;
        int startSlotY = this.dockY + (this.dockH - 12 * slotH) / 2;
        if (this.dockSide != 0) {
            boolean inDockX = mouseX >= this.dockX && mouseX <= this.dockX + this.dockW;
            boolean inDockY = mouseY >= this.dockY && mouseY <= this.dockY + this.dockH;
            for (int i = 0; i < 12; ++i) {
                int iconCenterY = startSlotY + i * slotH + slotH / 2;
                float target = 1.0f;
                if (inDockX && inDockY) {
                    float dist = Math.abs(mouseY - iconCenterY);
                    if (dist < 10.0f) {
                        target = 1.32f;
                        hoveredDockIcon = i;
                    } else if (dist < 22.0f) {
                        target = 1.0f + (22.0f - dist) / 12.0f * 0.2f;
                    }
                }
                this.dockIconScale[i] += (target - this.dockIconScale[i]) * Math.min(1.0f, dt * 22.0f);
            }
            MusicHudRenderer.renderRoundedRect(context, this.dockX - 1, this.dockY - 1, this.dockW + 2, this.dockH + 2, 9, 0x38000000);
            int dockBg = 0xF4000000 | (HudConfig.INSTANCE.configBackgroundColor & 0xFFFFFF);
            MusicHudRenderer.renderRoundedRect(context, this.dockX, this.dockY, this.dockW, this.dockH, 8, dockBg);
            MusicHudRenderer.renderRoundedBorder(context, this.dockX, this.dockY, this.dockW, this.dockH, 8, 0x66475569, 0x441E293B);
            context.method_25294(this.dockX + 6, this.dockY, this.dockX + this.dockW - 6, this.dockY + 1, 0x4494A3B8);
            for (int i = 0; i < 12; ++i) {
                int iconCenterY = startSlotY + i * slotH + slotH / 2;
                int iconCenterX = this.dockX + this.dockW / 2;
                if (i == 1 || i == 5 || i == 10) {
                    int divY = startSlotY + i * slotH;
                    context.method_25294(this.dockX + 6, divY, this.dockX + this.dockW - 6, divY + 1, 0x33475569);
                }
                float s = this.dockIconScale[i];
                boolean isActiveTabIcon = this.flyoutOpen && i >= 1 && i <= 4 && (i - 1) == this.currentTab;
                if (isActiveTabIcon) {
                    MusicHudRenderer.renderRoundedRect(context, iconCenterX - 13, iconCenterY - 7, 26, 14, 4, 0x88132032);
                    MusicHudRenderer.renderRoundedBorder(context, iconCenterX - 13, iconCenterY - 7, 26, 14, 4, 0x9938BDF8, 0x44334155);
                    int sideBarX = this.dockSide == 1 ? this.dockX + 1 : this.dockX + this.dockW - 3;
                    MusicHudRenderer.renderRoundedRect(context, sideBarX, iconCenterY - 5, 2, 10, 1, 0xFF38BDF8);
                } else if (s > 1.08f) {
                    int auraAlpha = (int)((s - 1.0f) / 0.32f * 55.0f);
                    MusicHudRenderer.renderRoundedRect(context, iconCenterX - 12, iconCenterY - 7, 24, 14, 4, auraAlpha << 24 | 0x38BDF8);
                }
                int col = this.getDockIconColor(i);
                context.method_51448().pushMatrix();
                context.method_51448().translate((float)iconCenterX, (float)iconCenterY);
                context.method_51448().scale(s, s);
                context.method_51448().translate((float)(-iconCenterX), (float)(-iconCenterY));
                this.renderDockIcon(context, i, iconCenterX, iconCenterY, col, isActiveTabIcon);
                context.method_51448().popMatrix();
            }
            if (this.flyoutOpen) {
                MusicHudRenderer.renderRoundedRect(context, curPanelX - 2, curPanelY - 2, this.panelWidth + 4, this.panelHeight + 4, 12, 0x44000000);
                int panelBg = 0xF4000000 | (HudConfig.INSTANCE.configBackgroundColor & 0xFFFFFF);
                MusicHudRenderer.renderRoundedRect(context, curPanelX, curPanelY, this.panelWidth, this.panelHeight, 10, panelBg);
                MusicHudRenderer.renderRoundedRect(context, curPanelX + 2, curPanelY + 2, this.panelWidth - 4, 20, 8, 0xCC06080D);
                context.method_25294(curPanelX + 8, curPanelY + 21, curPanelX + this.panelWidth - 8, curPanelY + 22, 0x33334155);
                MusicHudRenderer.renderRoundedBorder(context, curPanelX, curPanelY, this.panelWidth, this.panelHeight, 10, 0x66475569, 0x441E293B);
                context.method_25294(curPanelX + 14, curPanelY, curPanelX + this.panelWidth - 14, curPanelY + 1, 0x5594A3B8);
                String headerTitle = this.getTabHeaderTitle();
                context.method_51433(this.field_22793, headerTitle, curPanelX + 10, curPanelY + 7, -1, true);
                if (this.currentTab == 2 && this.paletteColorWheel != null) {
                    this.paletteColorWheel.render(context, mouseX, mouseY);
                    this.renderColorPreviewCard(context, curPanelX + 8 + 68 + 12, curPanelY + 22 + 24, this.panelWidth - 16 - 68 - 14, 22, this.getCurrentTargetColor());
                }
                if (this.currentTab == 3) {
                    this.renderSessionStatsCard(context, curPanelX + 8, curPanelY + 24, 100, this.panelHeight - 32, isPlaying, nowMs);
                    if (this.panelHeight >= 210) {
                        int rightCardX = curPanelX + 8 + 100 + 6;
                        int rightCardW = this.panelWidth - 16 - 100 - 6;
                        this.renderNametagBadgePreview(context, rightCardX, curPanelY + 24 + 168, rightCardW);
                    }
                }
            }
        } else {
            MusicHudRenderer.renderRoundedRect(context, this.panelX + shadowOffX, this.panelY + shadowOffY, this.panelWidth, this.panelHeight, 14, 0x66000000);
            int glowPulse = (int)(22.0f + (isPlaying ? beatEnergy * 24.0f : (float)Math.sin((double)nowMs * 0.003) * 8.0f));
            MusicHudRenderer.renderRoundedRect(context, curPanelX - 1, curPanelY - 1, this.panelWidth + 2, this.panelHeight + 2, 13, glowPulse << 24 | 0x38BDF8);
            int floatBg = 0xF4000000 | (HudConfig.INSTANCE.configBackgroundColor & 0xFFFFFF);
            MusicHudRenderer.renderRoundedRect(context, curPanelX, curPanelY, this.panelWidth, this.panelHeight, 12, floatBg);
            MusicHudRenderer.renderRoundedRect(context, curPanelX + 2, curPanelY + 2, this.panelWidth - 4, 22, 10, 0xCC06080D);
            context.method_25294(curPanelX + 8, curPanelY + 23, curPanelX + this.panelWidth - 8, curPanelY + 24, 0x33334155);
            MusicHudRenderer.renderRoundedBorder(context, curPanelX, curPanelY, this.panelWidth, this.panelHeight, 12, 0x66475569, 0x441E293B);
            context.method_25294(curPanelX + 18, curPanelY, curPanelX + this.panelWidth - 18, curPanelY + 1, 0x5594A3B8);
            boolean hoverGrip = mouseX >= curPanelX + 8 && mouseX <= curPanelX + 125 && mouseY >= curPanelY + 4 && mouseY <= curPanelY + 22;
            String titleStr = (hoverGrip ? "§b:: " : "§8:: ") + this.getTabHeaderTitle();
            context.method_51433(this.field_22793, titleStr, curPanelX + 10, curPanelY + 8, -1, true);
            int tabY = curPanelY + 25;
            int tabH = 18;
            int tabW = (this.panelWidth - 16 - 9) / 4;
            float targetTabX = curPanelX + 8 + (tabW + 3) * this.currentTab;
            float targetTabW = tabW;
            if (this.tabIndicatorX < 0.0f) {
                this.tabIndicatorX = targetTabX;
                this.tabIndicatorW = targetTabW;
            } else {
                this.tabIndicatorX += (targetTabX - this.tabIndicatorX) * Math.min(1.0f, dt * 18.0f);
                this.tabIndicatorW += (targetTabW - this.tabIndicatorW) * Math.min(1.0f, dt * 18.0f);
            }
            MusicHudRenderer.renderRoundedRect(context, curPanelX + 7, tabY - 1, this.panelWidth - 14, tabH + 2, 6, 0xDD06080D);
            MusicHudRenderer.renderRoundedBorder(context, curPanelX + 7, tabY - 1, this.panelWidth - 14, tabH + 2, 6, 0x38334155, 0x221E293B);
            int indX = (int)this.tabIndicatorX;
            int indW = (int)this.tabIndicatorW;
            MusicHudRenderer.renderRoundedRect(context, indX, tabY, indW, tabH, 5, 0x88132032);
            MusicHudRenderer.renderRoundedBorder(context, indX, tabY, indW, tabH, 5, 0xAA38BDF8, 0x55334155);
            context.method_25294(indX + 6, tabY + tabH - 2, indX + indW - 6, tabY + tabH - 1, 0xFF38BDF8);
            if (this.currentTab == 2 && this.paletteColorWheel != null) {
                this.paletteColorWheel.render(context, mouseX, mouseY);
                this.renderColorPreviewCard(context, curPanelX + 10 + 80 + 12, curPanelY + 46 + 24, this.panelWidth - 20 - 80 - 14, 22, this.getCurrentTargetColor());
            }
            if (this.currentTab == 3) {
                this.renderSessionStatsCard(context, curPanelX + 10, curPanelY + 46, 120, this.panelHeight - 56, isPlaying, nowMs);
                if (this.panelHeight >= 220) {
                    int rightCardX = curPanelX + 10 + 120 + 6;
                    int rightCardW = this.panelWidth - 20 - 120 - 6;
                    this.renderNametagBadgePreview(context, rightCardX, curPanelY + 46 + 165, rightCardW);
                }
            }
        }
        long tabAge = nowMs - this.tabSwitchStartTime;
        float tabSlideY = 0.0f;
        if (this.tabSwitchStartTime > 0L && tabAge >= 0L && tabAge < 180L) {
            float tp = (float)tabAge / 180.0f;
            float tabEase = 1.0f - (1.0f - tp) * (1.0f - tp) * (1.0f - tp);
            tabSlideY = (1.0f - tabEase) * 4.5f;
            if (this.dockSide == 0 || this.flyoutOpen) {
                int washAlpha = (int)((1.0f - tabEase) * 26.0f);
                int wy = curPanelY + (this.dockSide != 0 ? 22 : 44);
                MusicHudRenderer.renderRoundedRect(context, curPanelX + 6, wy, this.panelWidth - 12, this.panelHeight - (wy - curPanelY) - 6, 6, washAlpha << 24 | 0x38BDF8);
            }
        }
        context.method_51448().pushMatrix();
        if (tabSlideY > 0.05f) {
            context.method_51448().translate(0.0f, tabSlideY);
        }
        super.method_25394(context, mouseX, mouseY, delta);
        context.method_51448().popMatrix();
        this.renderLiveHudPreview(context, mouseX, mouseY);
        if (is2DBottom) {
            this.renderLiveSubtitlePreview(context, mouseX, mouseY);
        }
        for (int i = this.cursorSparkles.size() - 1; i >= 0; --i) {
            CursorSparkle sp = this.cursorSparkles.get(i);
            if (!sp.update()) {
                this.cursorSparkles.remove(i);
                continue;
            }
            float alpha = 1.0f - (float)sp.age / (float)sp.maxLife;
            int col = (int)(alpha * 240.0f) << 24 | sp.color & 0xFFFFFF;
            context.method_51433(this.field_22793, sp.symbol, (int)sp.x, (int)sp.y, col, true);
        }
        for (int i = this.easterEggParticles.size() - 1; i >= 0; --i) {
            EasterEggParticle ep = this.easterEggParticles.get(i);
            if (!ep.update()) {
                this.easterEggParticles.remove(i);
                continue;
            }
            float alpha = 1.0f - (float)ep.age / (float)ep.maxLife;
            int col = (int)(alpha * 255.0f) << 24 | ep.color & 0xFFFFFF;
            context.method_51433(this.field_22793, ep.symbol, (int)ep.x, (int)ep.y, col, true);
        }
        this.renderScreenCracks(context);
        if (this.activeDropdownId != null) {
            this.renderActiveDropdown(context, mouseX, mouseY);
        }
        this.renderFloatingTooltip(context, mouseX, mouseY);
        if (this.dockSide != 0 && hoveredDockIcon >= 0 && this.dockIconScale[hoveredDockIcon] >= 1.15f) {
            String tip = DOCK_TOOLTIPS[hoveredDockIcon];
            if (hoveredDockIcon == 5) {
                tip = HudConfig.INSTANCE.enabled ? "§aМод: ВКЛ" : "§cМод: ВЫКЛ";
            }
            int tipW = this.field_22793.method_1727(tip) + 14;
            int tipH = 18;
            int tipX = this.dockSide == 1 ? this.dockX + this.dockW + 6 : this.dockX - tipW - 6;
            int tipY = startSlotY + hoveredDockIcon * slotH + slotH / 2 - tipH / 2;
            tipY = Math.max(6, Math.min(tipY, this.field_22790 - tipH - 6));

            MusicHudRenderer.renderRoundedRect(context, tipX - 2, tipY - 2, tipW + 4, tipH + 4, 6, 0xAA000000);
            MusicHudRenderer.renderRoundedRect(context, tipX, tipY, tipW, tipH, 5, 0xFF0D1117);
            MusicHudRenderer.renderRoundedBorder(context, tipX, tipY, tipW, tipH, 5, 0xFF38BDF8, 0xFF1E293B);
            context.method_25294(tipX + 6, tipY, tipX + tipW - 6, tipY + 1, 0xFF38BDF8);
            context.method_51433(this.field_22793, tip, tipX + 7, tipY + 5, -1, true);
        }
        this.renderModBrandHeader(context, mouseX, mouseY, nowMs);
        context.method_51448().popMatrix();
        if (CustomBackgroundManager.INSTANCE.isAlertActive()) {
            this.renderInvalidBackgroundAlertModal(context, mouseX, mouseY);
        }
        this.renderFeedbackToast(context);
    }

    private void renderColorPreviewCard(class_332 context, int cardX, int cardY, int cardW, int cardH, int curColor) {
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 5, 0xEE0C0F17);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 5, 0x55475569, 0x331E293B);

        int swatchSize = 18;
        int swatchX = cardX + 4;
        int swatchY = cardY + 2;

        MusicHudRenderer.renderRoundedRect(context, swatchX - 1, swatchY - 1, swatchSize + 2, swatchSize + 2, 4, 0x66FFFFFF);
        MusicHudRenderer.renderRoundedRect(context, swatchX, swatchY, swatchSize, swatchSize, 3, 0xFF000000 | (curColor & 0xFFFFFF));
        MusicHudRenderer.renderRoundedBorder(context, swatchX, swatchY, swatchSize, swatchSize, 3, 0xAAFFFFFF, 0x55000000);

        int textX = swatchX + swatchSize + 6;
        int r = curColor >> 16 & 0xFF;
        int g = curColor >> 8 & 0xFF;
        int b = curColor & 0xFF;

        String hexStr = Integer.toHexString(0x1000000 | (curColor & 0xFFFFFF)).substring(1).toUpperCase(java.util.Locale.ROOT);
        context.method_51433(this.field_22793, "§7HEX: §e#" + hexStr, textX, cardY + 2, -1, false);
        context.method_51433(this.field_22793, "§c" + r + " §a" + g + " §b" + b, textX, cardY + 12, -1, false);
    }

    private void renderSessionStatsCard(class_332 context, int cardX, int cardY, int cardW, int cardH, boolean isPlaying, long nowMs) {
        MusicHudRenderer.renderRoundedRect(context, cardX - 1, cardY - 1, cardW + 2, cardH + 2, 8, 0x44000000);
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 7, 0xEE0B0E17);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 7, 0x5538BDF8, 0x331E293B);
        context.method_25294(cardX + 8, cardY, cardX + cardW - 8, cardY + 1, 0x5538BDF8);

        int dotCol = isPlaying ? 0xFF10B981 : 0xFF64748B;
        if (isPlaying) {
            float pulse = (float)Math.sin((double)nowMs * 0.005) * 0.5f + 0.5f;
            int pulseAlpha = (int)(100.0f + pulse * 155.0f);
            dotCol = pulseAlpha << 24 | 0x10B981;
        }
        context.method_51433(this.field_22793, "●", cardX + 7, cardY + 6, dotCol, false);
        context.method_51433(this.field_22793, "§b♫ Сессия", cardX + 17, cardY + 6, -1, true);

        int curY = cardY + 20;
        String timeStr = SessionStatsManager.INSTANCE.getFormattedListeningTime();
        int chipW = cardW - 12;
        MusicHudRenderer.renderRoundedRect(context, cardX + 6, curY, chipW, 16, 4, 0x66111827);
        MusicHudRenderer.renderRoundedBorder(context, cardX + 6, curY, chipW, 16, 4, 0x3338BDF8, 0x221E293B);
        context.method_51433(this.field_22793, "§7⏱ §a" + timeStr, cardX + 10, curY + 4, -1, false);

        curY += 19;
        int tracksCount = SessionStatsManager.INSTANCE.getTotalTracksPlayed();
        MusicHudRenderer.renderRoundedRect(context, cardX + 6, curY, chipW, 16, 4, 0x66111827);
        MusicHudRenderer.renderRoundedBorder(context, cardX + 6, curY, chipW, 16, 4, 0x3338BDF8, 0x221E293B);
        context.method_51433(this.field_22793, "§7♫ §b" + tracksCount + " треков", cardX + 10, curY + 4, -1, false);

        curY += 21;
        context.method_25294(cardX + 8, curY, cardX + cardW - 8, curY + 1, 0x3338BDF8);
        curY += 4;

        context.method_51433(this.field_22793, "§8★ Топ артист:", cardX + 7, curY, -1, false);
        curY += 10;
        String topArtist = String.valueOf(SessionStatsManager.INSTANCE.getTopArtist());
        if (this.field_22793.method_1727(topArtist) > chipW) {
            topArtist = this.field_22793.method_27523(topArtist, chipW - 8) + "..";
        }
        context.method_51433(this.field_22793, "§d" + topArtist, cardX + 7, curY, -1, false);

        curY += 13;
        context.method_51433(this.field_22793, "§8♪ Топ трек:", cardX + 7, curY, -1, false);
        curY += 10;
        String topTrack = String.valueOf(SessionStatsManager.INSTANCE.getTopTrack());
        if (this.field_22793.method_1727(topTrack) > chipW) {
            topTrack = this.field_22793.method_27523(topTrack, chipW - 8) + "..";
        }
        context.method_51433(this.field_22793, "§f" + topTrack, cardX + 7, curY, -1, false);

        if (cardH >= 132) {
            int botY = cardY + cardH - 18;
            int botBg = isPlaying ? 0xCC0D221D : 0xCC111622;
            int botBorder = isPlaying ? 0x8810B981 : 0x44334155;
            MusicHudRenderer.renderRoundedRect(context, cardX + 5, botY, cardW - 10, 14, 4, botBg);
            MusicHudRenderer.renderRoundedBorder(context, cardX + 5, botY, cardW - 10, 14, 4, botBorder, botBorder);

            if (isPlaying) {
                int eqX = cardX + 8;
                int eqBaseY = botY + 10;
                for (int b = 0; b < 5; ++b) {
                    float wave = (float)Math.sin((double)nowMs * 0.009 + (double)b * 1.1) * 0.5f + 0.5f;
                    int barH = (int)(2.0f + wave * 7.0f);
                    MusicHudRenderer.renderRoundedRect(context, eqX + b * 4, eqBaseY - barH, 2, barH, 1, 0xFF10B981);
                }
                context.method_51433(this.field_22793, "§aВ эфире", cardX + 32, botY + 3, -1, false);
            } else {
                context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("§8На паузе"), cardX + cardW / 2, botY + 3, -1);
            }
        }
    }

    private void renderNametagBadgePreview(class_332 context, int bx, int by, int maxW) {
        class_310 mc = this.field_22787;
        class_2561 playerText = mc != null && mc.field_1724 != null ? mc.field_1724.method_5477() : class_2561.method_43470("Player");
        int nameW = this.field_22793.method_27525(playerText);
        int iconSize = 9;
        int padLeft = 4;
        int padRight = 5;
        int spaceW = this.field_22793.method_1727(" ");
        int badgeW = padLeft + iconSize + spaceW + nameW + padRight;
        int badgeH = 14;
        int drawX = bx + Math.max(0, (maxW - badgeW) / 2);
        int drawY = by;

        context.method_51433(this.field_22793, "§8Вид бейджа игрока:", bx + 2, drawY - 9, -1, false);

        context.method_25294(drawX, drawY, drawX + badgeW, drawY + badgeH, 0xF2131124);

        context.method_25294(drawX, drawY, drawX + badgeW, drawY + 1, 0xFF00F5D4);
        context.method_25294(drawX, drawY + badgeH - 1, drawX + badgeW, drawY + badgeH, 0xFF00F5D4);
        context.method_25294(drawX, drawY, drawX + 1, drawY + badgeH, 0xFF00F5D4);
        context.method_25294(drawX + badgeW - 1, drawY, drawX + badgeW, drawY + badgeH, 0xFF00F5D4);

        context.method_25291(class_10799.field_56883, PlayerBadgeManager.MOD_ICON, drawX + padLeft, drawY + 2, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, -1);
        context.method_27535(this.field_22793, playerText, drawX + padLeft + iconSize + spaceW, drawY + 3, -1);
    }

    private void renderModBrandHeader(class_332 context, int mouseX, int mouseY, long nowMs) {
        if (this.dragging || this.dragging2DSub || this.draggingConfig || this.draggingDock) {
            return;
        }
        int brandX = 4;
        int brandY = 4;
        int brandW = 168;
        int brandH = 34;
        boolean hovered = mouseX >= brandX && mouseX <= brandX + brandW && mouseY >= brandY && mouseY <= brandY + brandH;

        float pulse = (float)Math.sin((double)nowMs * 0.0035) * 0.5f + 0.5f;
        int glowAlpha = (int)(25.0f + pulse * 30.0f);
        if (hovered) {
            glowAlpha = Math.min(255, glowAlpha + 55);
        }
        MusicHudRenderer.renderRoundedRect(context, brandX - 2, brandY - 2, brandW + 4, brandH + 4, 8, glowAlpha << 24 | 0x38BDF8);
        MusicHudRenderer.renderRoundedRect(context, brandX, brandY, brandW, brandH, 7, 0xF20A0E17);
        MusicHudRenderer.renderRoundedBorder(context, brandX, brandY, brandW, brandH, 7, hovered ? 0xEE38BDF8 : 0x7738BDF8, hovered ? 0xAA9333EA : 0x44475569);
        context.method_25294(brandX + 8, brandY, brandX + brandW - 8, brandY + 1, hovered ? 0xAA38BDF8 : 0x5594A3B8);

        int iconX = brandX + 5;
        int iconY = brandY + 5;
        MusicHudRenderer.renderRoundedRect(context, iconX - 1, iconY - 1, 26, 26, 5, 0x3338BDF8);
        MusicHudRenderer.renderRoundedBorder(context, iconX - 1, iconY - 1, 26, 26, 5, 0x6638BDF8, 0x33475569);
        try {
            context.method_25291(class_10799.field_56883, MOD_ICON, iconX, iconY, 0.0f, 0.0f, 24, 24, 24, 24, 0xFFFFFFFF);
        } catch (Throwable ignored) {
            MusicHudRenderer.renderRoundedRect(context, iconX, iconY, 24, 24, 4, 0xFF38BDF8);
            context.method_51433(this.field_22793, "♫", iconX + 8, iconY + 7, -1, true);
        }

        int textX = brandX + 35;
        context.method_51433(this.field_22793, "§bMusic §fSubtitles", textX, brandY + 6, -1, true);
        if (hovered) {
            context.method_51433(this.field_22793, "§8Версия: §e2.8.1 §d[Автор ↗]", textX, brandY + 19, -1, false);
        } else {
            context.method_51433(this.field_22793, "§8Версия: §e2.8.1", textX, brandY + 19, -1, false);
        }
    }

    private void renderFloatingTooltip(class_332 context, int mouseX, int mouseY) {
        if (this.activeDropdownId != null) {
            return;
        }
        HelpEntry hovered = null;
        for (HelpEntry entry : this.helpEntries) {
            if (mouseX < entry.x || mouseX > entry.x + entry.w || mouseY < entry.y || mouseY > entry.y + entry.h) continue;
            hovered = entry;
            break;
        }
        if (hovered == null) {
            return;
        }
        int maxW = 206;
        List<String> descLines = this.wrapText(hovered.description, maxW - 16);
        List<String> recLines = this.wrapText(hovered.recommendation, maxW - 16);
        int totalH = 24 + descLines.size() * 10 + 6 + recLines.size() * 10 + 8;
        int tipX = mouseX + 12;
        int tipY = mouseY + 12;
        if (tipX + maxW > this.field_22789 - 6) {
            tipX = mouseX - maxW - 8;
        }
        tipX = Math.max(6, Math.min(tipX, this.field_22789 - maxW - 6));
        if (tipY + totalH > this.field_22790 - 6) {
            tipY = this.field_22790 - totalH - 6;
        }
        tipY = Math.max(6, Math.min(tipY, this.field_22790 - totalH - 6));

        MusicHudRenderer.renderRoundedRect(context, tipX - 2, tipY - 2, maxW + 4, totalH + 4, 8, 0x88000000);
        MusicHudRenderer.renderRoundedRect(context, tipX, tipY, maxW, totalH, 6, 0xFF0A0D14);
        MusicHudRenderer.renderRoundedRect(context, tipX + 2, tipY + 2, maxW - 4, 16, 4, 0xCC06080D);
        MusicHudRenderer.renderRoundedBorder(context, tipX, tipY, maxW, totalH, 6, 0xFF38BDF8, 0x44334155);
        context.method_25294(tipX + 10, tipY, tipX + maxW - 10, tipY + 1, 0xFF38BDF8);
        int curY = tipY + 6;
        context.method_51433(this.field_22793, "§b" + hovered.title, tipX + 8, curY, -1, true);
        curY += 15;
        for (String line : descLines) {
            context.method_51433(this.field_22793, "§f" + line, tipX + 8, curY, -1, false);
            curY += 10;
        }
        curY += 4;
        for (String line : recLines) {
            context.method_51433(this.field_22793, "§a" + line, tipX + 8, curY, -1, false);
            curY += 10;
        }
    }

    private void renderActiveDropdown(class_332 context, int mouseX, int mouseY) {
        if (this.activeDropdownId == null || this.activeDropdownItems.isEmpty()) {
            return;
        }
        int dropX = this.activeDropdownX;
        int dropY = this.activeDropdownY;
        int dropW = this.activeDropdownW;
        int dropH = this.activeDropdownH;

        MusicHudRenderer.renderRoundedRect(context, dropX - 2, dropY - 2, dropW + 4, dropH + 4, 8, 0x66000000);
        MusicHudRenderer.renderRoundedRect(context, dropX, dropY, dropW, dropH, 6, 0xF80A0D14);
        MusicHudRenderer.renderRoundedBorder(context, dropX, dropY, dropW, dropH, 6, 0x9938BDF8, 0x55334155);
        context.method_25294(dropX + 8, dropY, dropX + dropW - 8, dropY + 1, 0x5594A3B8);

        String curSelectedKey = this.activeDropdownSelectedKey != null ? this.activeDropdownSelectedKey.get() : null;
        int startIdx = this.activeDropdownScroll;
        int endIdx = Math.min(this.activeDropdownItems.size(), startIdx + this.activeDropdownMaxVisible);
        long nowMs = System.currentTimeMillis();
        boolean hasBadge = "HUD_STYLE".equals(this.activeDropdownId) || "SUB_MODE".equals(this.activeDropdownId) || "ANIMATION".equals(this.activeDropdownId);

        DropdownItem hoveredItem = null;
        int hoveredItemY = 0;
        for (int i = startIdx; i < endIdx; ++i) {
            DropdownItem item = this.activeDropdownItems.get(i);
            int rowIdx = i - startIdx;
            int itemX = dropX + 3;
            int itemY = dropY + 3 + rowIdx * 18;
            int itemW = dropW - 6;
            int itemH = 18;

            boolean isHovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + itemH;
            if (isHovered) {
                hoveredItem = item;
                hoveredItemY = itemY;
            }
            boolean isSelected = false;
            if (curSelectedKey != null) {
                if ("ANIMATION".equals(this.activeDropdownId)) {
                    isSelected = item.key.equalsIgnoreCase(curSelectedKey) || (item.key.equals("MISIDE_FALL") && "MISIDE".equalsIgnoreCase(curSelectedKey));
                } else {
                    isSelected = item.key.equalsIgnoreCase(curSelectedKey);
                }
            }

            if (isSelected) {
                MusicHudRenderer.renderRoundedRect(context, itemX, itemY, itemW, itemH, 4, isHovered ? 0xDD112A28 : 0xCC0C2020);
                MusicHudRenderer.renderRoundedBorder(context, itemX, itemY, itemW, itemH, 4, 0xEE10B981, 0x7738BDF8);
                MusicHudRenderer.renderRoundedRect(context, itemX + 2, itemY + 4, 2, itemH - 8, 1, 0xFF10B981);
            } else if (isHovered) {
                MusicHudRenderer.renderRoundedRect(context, itemX, itemY, itemW, itemH, 4, 0xDD132032);
                MusicHudRenderer.renderRoundedBorder(context, itemX, itemY, itemW, itemH, 4, 0xAA38BDF8, 0x55334155);
            }

            if (hasBadge) {
                this.renderDropdownPreviewBadge(context, this.activeDropdownId, item.key, itemX + itemW - 24, itemY + 3, 20, 12, isSelected, isHovered, nowMs);
            }

            int labelX = itemX + 8;
            int maxLblW = Math.max(24, hasBadge ? itemW - 34 : itemW - 14);
            String drawLabel = this.field_22793.method_1727(item.label) > maxLblW ? this.field_22793.method_27523(item.label, maxLblW) : item.label;
            int textColor = isSelected ? 0xFF34D399 : (isHovered ? 0xFFF8FAFC : 0xFFCBD5E1);
            context.method_51433(this.field_22793, drawLabel, labelX, itemY + 5, textColor, false);
        }
        if (this.activeDropdownItems.size() > this.activeDropdownMaxVisible) {
            int scrollbarH = Math.max(12, dropH * this.activeDropdownMaxVisible / this.activeDropdownItems.size());
            int maxScroll = this.activeDropdownItems.size() - this.activeDropdownMaxVisible;
            int scrollbarY = dropY + 3 + (dropH - 6 - scrollbarH) * this.activeDropdownScroll / maxScroll;
            MusicHudRenderer.renderRoundedRect(context, dropX + dropW - 4, scrollbarY, 2, scrollbarH, 1, 0x8838BDF8);
        }
        if (hoveredItem != null && ("HUD_STYLE".equals(this.activeDropdownId) || "ANIMATION".equals(this.activeDropdownId) || "SUB_MODE".equals(this.activeDropdownId))) {
            this.renderDropdownHoverPreview(context, hoveredItem, dropX, dropY, dropW, hoveredItemY, nowMs);
        }
    }

    private void renderDropdownHoverPreview(class_332 context, DropdownItem item, int dropX, int dropY, int dropW, int itemY, long nowMs) {
        int cardW = 168;
        if ("SUB_MODE".equals(this.activeDropdownId)) {
            String title = "§a✦ Режим: §f" + item.label;
            String desc1 = "";
            String desc2 = "";
            if (item.key.contains("3D")) {
                desc1 = "§b☁ Парит в воздухе";
                desc2 = "§7Объёмный 3D текст возле источника";
            } else if (item.key.contains("BLOCK")) {
                desc1 = "§a▦ Проекция на блоки";
                desc2 = "§7Субтитры ложатся на пол";
            } else if (item.key.contains("BOTTOM")) {
                desc1 = "§e📺 Нижняя панель";
                desc2 = "§7Классическая 2D строка внизу экрана";
            } else if (item.key.contains("RANDOM")) {
                desc1 = "§d✨ Разброс по экрану";
                desc2 = "§7Динамическое появление слов в разных точках";
            } else if (item.key.contains("CHAT")) {
                desc1 = "§f💬 В игровой чат";
                desc2 = "§7Отправка строк субтитров в чат";
            } else {
                desc1 = "§c✕ Отключено";
                desc2 = "§7Субтитры полностью скрыты";
            }
            int maxTextW = Math.max(this.field_22793.method_1727(title), Math.max(this.field_22793.method_1727(desc1), this.field_22793.method_1727(desc2)));
            cardW = Math.max(cardW, maxTextW + 24);
        }
        int cardH = "HUD_STYLE".equals(this.activeDropdownId) ? 62 : ("SUB_MODE".equals(this.activeDropdownId) ? 52 : 46);
        int cardX = dropX + dropW + 6;
        if (cardX + cardW > this.field_22789 - 6) {
            cardX = dropX - cardW - 6;
        }
        if (cardX < 6) {
            cardX = 6;
        }
        int cardY = Math.max(6, Math.min(this.field_22790 - cardH - 6, itemY - 6));

        MusicHudRenderer.renderRoundedRect(context, cardX - 2, cardY - 2, cardW + 4, cardH + 4, 8, 0x66000000);
        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 6, 0xF60A0E17);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 6, 0xAA38BDF8, 0x55334155);
        context.method_25294(cardX + 6, cardY, cardX + cardW - 6, cardY + 1, 0x5594A3B8);

        if ("HUD_STYLE".equals(this.activeDropdownId)) {
            context.method_51433(this.field_22793, "§b✦ Стиль: §f" + item.label, cardX + 8, cardY + 6, -1, true);
            int prevW = 86;
            int prevH = 28;
            int prevX = cardX + 8;
            int prevY = cardY + 22;
            if (item.key.contains("ISLAND")) {
                MusicHudRenderer.renderRoundedRect(context, prevX, prevY + 2, prevW, prevH - 4, (prevH - 4) / 2, 0xFF0D0F18);
                MusicHudRenderer.renderRoundedBorder(context, prevX, prevY + 2, prevW, prevH - 4, (prevH - 4) / 2, 0xAA38BDF8, 0x55334155);
                context.method_51433(this.field_22793, "§bılı §fОстров", prevX + 10, prevY + 7, -1, false);
            } else if (item.key.contains("VINYL")) {
                MusicHudRenderer.renderRoundedRect(context, prevX, prevY, prevW, prevH, 4, 0xEE111827);
                MusicHudRenderer.renderRoundedBorder(context, prevX, prevY, prevW, prevH, 4, 0xAA38BDF8, 0x55334155);
                MusicHudRenderer.renderRoundedRect(context, prevX + 4, prevY + 4, 20, 20, 10, 0xFF0A0A0A);
                MusicHudRenderer.renderRoundedBorder(context, prevX + 4, prevY + 4, 20, 20, 10, 0xFFF59E0B, 0xFF38BDF8);
                context.method_51433(this.field_22793, "§6♪ §fВинил", prevX + 28, prevY + 10, -1, false);
            } else if (item.key.contains("GLASS")) {
                MusicHudRenderer.renderRoundedRect(context, prevX, prevY, prevW, prevH, 5, 0x881E293B);
                MusicHudRenderer.renderRoundedBorder(context, prevX, prevY, prevW, prevH, 5, 0xCC38BDF8, 0x889333EA);
                context.method_51433(this.field_22793, "§d✦ §fСтекло", prevX + 8, prevY + 10, -1, false);
            } else {
                MusicHudRenderer.renderRoundedRect(context, prevX, prevY, prevW, prevH, 4, 0xEE090D16);
                MusicHudRenderer.renderRoundedBorder(context, prevX, prevY, prevW, prevH, 4, 0xAA38BDF8, 0x55334155);
                context.method_51433(this.field_22793, "§b■ §fТёмный", prevX + 8, prevY + 10, -1, false);
            }
            context.method_51433(this.field_22793, "§aЖивой вид", cardX + prevW + 14, cardY + 24, -1, false);
            context.method_51433(this.field_22793, "§7на экране", cardX + prevW + 14, cardY + 36, -1, false);
        } else if ("ANIMATION".equals(this.activeDropdownId)) {
            context.method_51433(this.field_22793, "§d✦ Анимация: §f" + item.label, cardX + 8, cardY + 6, -1, true);
            String sample = "Музыка §e♪";
            int baseAnimY = cardY + 24;
            if (item.key.contains("BOUNCE")) {
                float bounce = (float)Math.abs(Math.sin((double)nowMs * 0.008)) * 6.0f;
                context.method_51433(this.field_22793, sample, cardX + 12, baseAnimY - (int)bounce, -1, true);
            } else if (item.key.contains("WAVE")) {
                String plain = "Музыка ♪";
                int curX = cardX + 12;
                for (int ci = 0; ci < plain.length(); ci++) {
                    char c = plain.charAt(ci);
                    float waveY = (float)Math.sin((double)nowMs * 0.007 + (double)ci * 0.8) * 4.0f;
                    context.method_51433(this.field_22793, String.valueOf(c), curX, baseAnimY + (int)waveY, ci == plain.length() - 1 ? 0xFFFDE047 : 0xFFFFFFFF, true);
                    curX += this.field_22793.method_1727(String.valueOf(c));
                }
            } else if (item.key.contains("POP")) {
                float s = 1.0f + (float)Math.sin((double)nowMs * 0.009) * 0.18f;
                context.method_51448().pushMatrix();
                context.method_51448().translate((float)(cardX + 36), (float)(baseAnimY + 4));
                context.method_51448().scale(s, s);
                context.method_51433(this.field_22793, sample, -24, -4, -1, true);
                context.method_51448().popMatrix();
            } else if (item.key.contains("PULSE")) {
                float p = (float)Math.sin((double)nowMs * 0.005) * 0.35f + 0.65f;
                int alpha = (int)(p * 255.0f);
                context.method_51433(this.field_22793, sample, cardX + 12, baseAnimY, alpha << 24 | 0x38BDF8, true);
            } else if (item.key.contains("SLIDE")) {
                int slide = (int)(((nowMs % 1500L) / 1500.0) * 32.0) - 16;
                context.method_51433(this.field_22793, sample, cardX + 16 + slide, baseAnimY, -1, true);
            } else if (item.key.contains("MISIDE")) {
                float fall = (float)((nowMs % 1200L) / 1200.0) * 6.0f;
                context.method_51433(this.field_22793, sample, cardX + 12, baseAnimY + (int)fall, -1, true);
            } else {
                context.method_51433(this.field_22793, sample, cardX + 12, baseAnimY, -1, true);
            }
        } else if ("SUB_MODE".equals(this.activeDropdownId)) {
            context.method_51433(this.field_22793, "§a✦ Режим: §f" + item.label, cardX + 8, cardY + 6, -1, true);
            String desc1;
            String desc2;
            if (item.key.contains("3D")) {
                desc1 = "§b☁ Парит в воздухе";
                desc2 = "§7Объёмный 3D текст возле источника";
            } else if (item.key.contains("BLOCK")) {
                desc1 = "§a▦ Проекция на блоки";
                desc2 = "§7Субтитры ложатся на пол";
            } else if (item.key.contains("BOTTOM")) {
                desc1 = "§e📺 Нижняя панель";
                desc2 = "§7Классическая 2D строка внизу экрана";
            } else if (item.key.contains("RANDOM")) {
                desc1 = "§d✨ Разброс по экрану";
                desc2 = "§7Динамическое появление слов в разных точках";
            } else if (item.key.contains("CHAT")) {
                desc1 = "§f💬 В игровой чат";
                desc2 = "§7Отправка строк субтитров в чат";
            } else {
                desc1 = "§c✕ Отключено";
                desc2 = "§7Субтитры полностью скрыты";
            }
            context.method_51433(this.field_22793, desc1, cardX + 10, cardY + 22, -1, false);
            context.method_51433(this.field_22793, desc2, cardX + 10, cardY + 35, -1, false);
        }
    }

    private void renderDockIcon(class_332 context, int index, int cx, int cy, int col, boolean active) {
        int tint = 0xFF000000 | (col & 0xFFFFFF);
        switch (index) {
            case 0 -> {
                int dotCol = 0xCC64748B;
                for (int ry = -4; ry <= 4; ry += 4) {
                    context.method_25294(cx - 3, cy + ry - 1, cx - 1, cy + ry + 1, dotCol);
                    context.method_25294(cx + 1, cy + ry - 1, cx + 3, cy + ry + 1, dotCol);
                }
            }
            case 1 -> {
                try {
                    context.method_25291(class_10799.field_56883, ICON_HUD, cx - 5, cy - 5, 0.0f, 0.0f, 10, 10, 10, 10, tint);
                } catch (Throwable t) {
                    MusicHudRenderer.renderRoundedRect(context, cx - 5, cy - 4, 10, 8, 2, tint);
                }
            }
            case 2 -> {
                try {
                    context.method_25291(class_10799.field_56883, ICON_SUBTITLES, cx - 5, cy - 5, 0.0f, 0.0f, 10, 10, 10, 10, tint);
                } catch (Throwable t) {
                    MusicHudRenderer.renderRoundedRect(context, cx - 5, cy - 3, 10, 6, 2, tint);
                }
            }
            case 3 -> {
                MusicHudRenderer.renderRoundedRect(context, cx - 5, cy - 4, 5, 5, 2, 0xEE38BDF8);
                MusicHudRenderer.renderRoundedRect(context, cx, cy - 4, 5, 5, 2, 0xEEF43F5E);
                MusicHudRenderer.renderRoundedRect(context, cx - 3, cy, 6, 5, 2, 0xEE10B981);
                if (active) {
                    MusicHudRenderer.renderRoundedBorder(context, cx - 6, cy - 5, 12, 11, 3, 0xFFFFFFFF);
                }
            }
            case 4 -> {
                try {
                    context.method_25291(class_10799.field_56883, ICON_LIBRARY, cx - 5, cy - 5, 0.0f, 0.0f, 10, 10, 10, 10, tint);
                } catch (Throwable t) {
                    MusicHudRenderer.renderRoundedRect(context, cx - 4, cy - 4, 8, 8, 2, tint);
                }
            }
            case 5 -> {
                boolean en = HudConfig.INSTANCE.enabled;
                int pBg = en ? 0xEE10B981 : 0xEE334155;
                MusicHudRenderer.renderRoundedRect(context, cx - 6, cy - 3, 12, 7, 3, pBg);
                int kx = en ? cx + 1 : cx - 5;
                MusicHudRenderer.renderRoundedRect(context, kx, cy - 2, 4, 5, 2, 0xFFFFFFFF);
            }
            case 6 -> {
                try {
                    context.method_25291(class_10799.field_56883, ICON_FAVORITE, cx - 5, cy - 5, 0.0f, 0.0f, 10, 10, 10, 10, 0xFFFBBF24);
                } catch (Throwable t) {
                    context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("★"), cx, cy - 4, 0xFFFBBF24);
                }
            }
            case 7 -> {
                MusicHudRenderer.renderRoundedRect(context, cx - 6, cy - 4, 12, 9, 2, 0xEE1E293B);
                MusicHudRenderer.renderRoundedBorder(context, cx - 6, cy - 4, 12, 9, 2, 0xCCFBBF24);
                for (int kx = -4; kx <= 2; kx += 3) {
                    context.method_25294(cx + kx, cy - 2, cx + kx + 2, cy, 0xFFF8FAFC);
                }
                context.method_25294(cx - 3, cy + 1, cx + 3, cy + 3, 0xFFFBBF24);
            }
            case 8 -> {
                try {
                    context.method_25291(class_10799.field_56883, ICON_SETTINGS, cx - 5, cy - 5, 0.0f, 0.0f, 10, 10, 10, 10, 0xFF38BDF8);
                } catch (Throwable t) {
                    MusicHudRenderer.renderRoundedRect(context, cx - 4, cy - 4, 8, 8, 4, 0xFF38BDF8);
                }
            }
            case 9 -> {
                try {
                    context.method_25291(class_10799.field_56883, TWITCH_ICON, cx - 5, cy - 5, 0.0f, 0.0f, 10, 10, 10, 10, 0xFFFFFFFF);
                } catch (Throwable t) {
                    MusicHudRenderer.renderRoundedRect(context, cx - 4, cy - 4, 8, 8, 2, 0xFF9146FF);
                }
            }
            case 10 -> {
                MusicHudRenderer.renderRoundedRect(context, cx - 6, cy - 4, 12, 9, 2, 0xEE0F172A);
                MusicHudRenderer.renderRoundedBorder(context, cx - 6, cy - 4, 12, 9, 2, 0xDD38BDF8);
                context.method_25294(cx - 5, cy - 3, cx + 5, cy - 1, 0xDD38BDF8);
            }
            case 11 -> {
                MusicHudRenderer.renderRoundedRect(context, cx - 5, cy - 5, 10, 10, 3, 0x55F43F5E);
                MusicHudRenderer.renderRoundedBorder(context, cx - 5, cy - 5, 10, 10, 3, 0xCCF43F5E);
                context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("✕"), cx, cy - 4, 0xFFFCA5A5);
            }
        }
    }

    private void renderDropdownPreviewBadge(class_332 context, String dropdownId, String key, int bx, int by, int bw, int bh, boolean isSelected, boolean isHovered, long nowMs) {
        MusicHudRenderer.renderRoundedRect(context, bx, by, bw, bh, 3, 0xEE06080D);
        int borderCol = isSelected ? 0xAA10B981 : (isHovered ? 0xAA38BDF8 : 0x55334155);
        MusicHudRenderer.renderRoundedBorder(context, bx, by, bw, bh, 3, borderCol, borderCol);
        String k = key != null ? key.toUpperCase(java.util.Locale.ROOT) : "";
        if ("HUD_STYLE".equals(dropdownId)) {
            switch (k) {
                case "MINIMAL_DARK" -> {
                    context.method_25294(bx + 2, by + 2, bx + 3, by + bh - 2, 0xFF38BDF8);
                    MusicHudRenderer.renderRoundedRect(context, bx + 4, by + 3, 5, 6, 1, 0xFF1E293B);
                    context.method_25294(bx + 10, by + 4, bx + bw - 3, by + 5, 0xFFF8FAFC);
                    context.method_25294(bx + 10, by + 7, bx + bw - 5, by + 8, 0xFF64748B);
                }
                case "GLASS_AMBIENT" -> {
                    MusicHudRenderer.renderHorizontalAmbientGlow(context, bx + 1, by + 1, bw - 2, bh - 2, 2, 0x38BDF8, 140, bw - 3, false);
                    MusicHudRenderer.renderRoundedRect(context, bx + 3, by + 3, 5, 6, 1, 0xFF38BDF8);
                    context.method_25294(bx + 9, by + 4, bx + bw - 3, by + 5, 0xFFFFFFFF);
                }
                case "VINYL_DECK" -> {
                    MusicHudRenderer.renderRoundedRect(context, bx + 7, by + 2, 8, 8, 4, 0xFF1E293B);
                    MusicHudRenderer.renderRoundedRect(context, bx + 9, by + 4, 4, 4, 2, 0xFF38BDF8);
                    MusicHudRenderer.renderRoundedRect(context, bx + 3, by + 2, 7, 8, 1, 0xFF0F172A);
                    MusicHudRenderer.renderRoundedBorder(context, bx + 3, by + 2, 7, 8, 1, 0x8894A3B8);
                }
                case "DYNAMIC_ISLAND" -> {
                    MusicHudRenderer.renderRoundedRect(context, bx + 2, by + 3, bw - 4, bh - 6, (bh - 6) / 2, 0xFF0F172A);
                    MusicHudRenderer.renderRoundedBorder(context, bx + 2, by + 3, bw - 4, bh - 6, (bh - 6) / 2, 0xAA38BDF8);
                    MusicHudRenderer.renderRoundedRect(context, bx + 4, by + 4, 4, 4, 2, 0xFF38BDF8);
                    context.method_25294(bx + bw - 6, by + 4, bx + bw - 5, by + 8, 0xFF38BDF8);
                }
            }
        } else if ("ANIMATION".equals(dropdownId)) {
            float t = (float)(nowMs % 1000L) / 1000.0f;
            switch (k) {
                case "KARAOKE_BOUNCE" -> {
                    int lift = (int)(Math.sin(t * Math.PI) * 3.0);
                    context.method_25294(bx + 4, by + 6, bx + 7, by + 9, 0xFF64748B);
                    context.method_25294(bx + 9, by + 6 - lift, bx + 12, by + 9 - lift, 0xFF38BDF8);
                    context.method_25294(bx + 14, by + 6, bx + 17, by + 9, 0xFF64748B);
                }
                case "WAVE_FLOAT" -> {
                    for (int d = 0; d < 3; ++d) {
                        int wy = by + 5 + (int)Math.round(Math.sin((double)nowMs * 0.008 + d * 1.2) * 2.0);
                        context.method_25294(bx + 4 + d * 5, wy, bx + 7 + d * 5, wy + 2, 0xFF38BDF8);
                    }
                }
                case "ELASTIC_POP" -> {
                    int pw = 8 + (int)Math.round(Math.sin(t * Math.PI * 2.0) * 3.0);
                    MusicHudRenderer.renderRoundedRect(context, bx + (bw - pw) / 2, by + 3, pw, 6, 2, 0xFF10B981);
                }
                case "PULSE_GLOW" -> {
                    MusicHudRenderer.renderRoundedRect(context, bx + 5, by + 2, 10, 8, 4, 0x5538BDF8);
                    MusicHudRenderer.renderRoundedRect(context, bx + 7, by + 4, 6, 4, 2, 0xFFFFFFFF);
                }
                case "SLIDE_FADE" -> {
                    int sx = bx + 3 + (int)(t * 6.0f);
                    MusicHudRenderer.renderRoundedRect(context, sx, by + 4, 8, 4, 1, 0xFF38BDF8);
                }
                case "MISIDE_FALL", "MISIDE" -> {
                    int fallY = by + 2 + (int)(t * 6.0f);
                    context.method_25294(bx + 4, by + 3, bx + 7, by + 6, 0xFFF43F5E);
                    context.method_25294(bx + 9, fallY, bx + 12, fallY + 3, 0xFFEC4899);
                    context.method_25294(bx + 14, by + 7, bx + 17, by + 10, 0xFFF43F5E);
                }
                default -> {
                    context.method_25294(bx + 5, by + 5, bx + bw - 5, by + 7, 0xFFE2E8F0);
                }
            }
        } else if ("SUB_MODE".equals(dropdownId)) {
            switch (k) {
                case "WORLD_3D" -> {
                    MusicHudRenderer.renderRoundedBorder(context, bx + 4, by + 2, 9, 6, 1, 0x8864748B);
                    MusicHudRenderer.renderRoundedRect(context, bx + 7, by + 4, 9, 6, 1, 0xCC38BDF8);
                }
                case "WORLD_BLOCK" -> {
                    MusicHudRenderer.renderRoundedRect(context, bx + 4, by + 6, 12, 4, 1, 0xFF334155);
                    context.method_25294(bx + 5, by + 4, bx + 15, by + 6, 0xFF10B981);
                }
                case "HUD_BOTTOM" -> {
                    context.method_25294(bx + 4, by + bh - 4, bx + bw - 4, by + bh - 2, 0xFF38BDF8);
                }
                case "SCREEN_RANDOM" -> {
                    context.method_25294(bx + 3, by + 3, bx + 7, by + 5, 0xFF38BDF8);
                    context.method_25294(bx + 11, by + 7, bx + 16, by + 9, 0xFF10B981);
                    context.method_25294(bx + 6, by + 8, bx + 9, by + 10, 0xFFFBBF24);
                }
                case "CHAT" -> {
                    MusicHudRenderer.renderRoundedRect(context, bx + 3, by + 3, 10, 6, 2, 0xFF38BDF8);
                    context.method_25294(bx + 4, by + 9, bx + 6, by + 10, 0xFF38BDF8);
                }
                case "OFF" -> {
                    MusicHudRenderer.renderRoundedRect(context, bx + 6, by + 3, 8, 6, 2, 0xCCF43F5E);
                }
            }
        }
    }

    private void renderLiveHudPreview(class_332 context, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= this.hudX && mouseX <= this.hudX + this.hudWidth && mouseY >= this.hudY && mouseY <= this.hudY + this.hudHeight;
        int borderColor = this.dragging ? -13312 : (isHovered ? -301931009 : 0x44FFFFFF);
        float scale = class_3532.method_15363(HudConfig.INSTANCE.hudScale, 0.2f, 4.0f);
        int baseW = HudConfig.INSTANCE.getHudBaseWidth();
        int baseH = HudConfig.INSTANCE.getHudBaseHeight();
        int r = "DYNAMIC_ISLAND".equalsIgnoreCase(HudConfig.INSTANCE.hudStyle) ? baseH / 2 : class_3532.method_15340(HudConfig.INSTANCE.cornerRadius, 0, 20);
        MusicHudRenderer.renderRoundedBorder(context, this.hudX - 2, this.hudY - 2, this.hudWidth + 4, this.hudHeight + 4, Math.min(28, (int)((float)r * scale) + 2), borderColor, borderColor);
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)this.hudX, (float)this.hudY);
        context.method_51448().scale(scale, scale);
        boolean mirror = HudConfig.INSTANCE.autoAdaptLayout && this.hudX < this.field_22789 / 2;
        MusicHudRenderer.renderStylePreview(context, this.field_22793, baseW, baseH, mirror);
        if (HudConfig.INSTANCE.subtitlesEnabled) {
            String fullText = "§e⚠ §fСубтитры не сходятся §c✕";
            int textW = this.field_22793.method_1727(fullText);
            int btnW = textW + 14;
            int btnH = 15;
            int btnX = mirror ? 4 : baseW - 4 - btnW;
            int btnY = baseH + 4;
            MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 5, 0xCC120A0E);
            MusicHudRenderer.renderRoundedBorder(context, btnX, btnY, btnW, btnH, 5, 0xFFEE5253, 0xFFEE5253);
            int textStartX = btnX + (btnW - textW) / 2;
            int textStartY = btnY + (btnH - 9) / 2;
            context.method_51433(this.field_22793, fullText, textStartX, textStartY, -1, true);
        }
        context.method_51448().popMatrix();
    }

    private void renderLiveSubtitlePreview(class_332 context, int mouseX, int mouseY) {
        boolean isHovered;
        String previewText = "Предпросмотр субтитров песни ♪";
        String font = HudConfig.INSTANCE.subtitlesFont;
        boolean isCustom = CustomFontManager.isCustomFont(font);
        boolean isDefaultFont = !isCustom && (font == null || font.isEmpty() || "default".equalsIgnoreCase(font) || "minecraft:default".equalsIgnoreCase(font) || "minecraft".equalsIgnoreCase(font));
        boolean isRuneFont = !isCustom && (font != null && (font.contains("alt") || font.contains("illager")));

        String fontName = font != null ? font.trim() : "default";
        int textW = 0;
        int textH = 9;
        for (int i = 0; i < previewText.length(); ++i) {
            char c = previewText.charAt(i);
            if (isCustom) {
                CustomFontManager.CustomGlyph g = CustomFontManager.getGlyph(font, c);
                textW += (g != null ? Math.max(1, Math.round(g.advance)) : 6);
            } else if (isDefaultFont) {
                textW += this.field_22793.method_1727(LyricsRenderer.singleCharString(c));
            } else {
                String renderStr = isRuneFont ? LyricsRenderer.transliterateForRune(c) : LyricsRenderer.singleCharString(c);
                if (renderStr.isEmpty()) renderStr = " ";
                net.minecraft.class_5250 charComp = LyricsRenderer.getCachedStyledChar(fontName, renderStr);
                textW += this.field_22793.method_27525(charComp);
            }
        }
        float subScale = class_3532.method_15363(HudConfig.INSTANCE.subtitlesScale, 0.5f, 3.0f);
        int subCenterX = (int)((double)this.field_22789 * HudConfig.INSTANCE.sub2DPositionX);
        int subCenterY = (int)((double)this.field_22790 * HudConfig.INSTANCE.sub2DPositionY);
        int padX = 10;
        int padY = 6;
        int boxW = (int)((float)(textW + padX * 2) * subScale);
        int boxH = (int)((float)(textH + padY * 2) * subScale);
        int boxX = subCenterX - boxW / 2;
        int boxY = subCenterY - boxH / 2;
        this.subPreviewW = boxW;
        this.subPreviewH = boxH;
        boolean bl = isHovered = mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH;
        int borderColor = this.dragging2DSub ? -22016 : (isHovered ? -1442809089 : 1140882175);
        MusicHudRenderer.renderRoundedBorder(context, boxX - 2, boxY - 2, boxW + 4, boxH + 4, 6, borderColor, borderColor);
        if (this.dragging2DSub) {
            boolean isCentered = Math.abs(subCenterX - this.field_22789 / 2) < 2;
            int px = (int)(HudConfig.INSTANCE.sub2DPositionX * 100.0);
            int py = (int)(HudConfig.INSTANCE.sub2DPositionY * 100.0);
            String coordText = (isCentered ? "§a✦ По центру §7(X: " : "§e✦ X: ") + px + "%, Y: " + py + "%";
            context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(coordText), subCenterX, Math.max(16, boxY - 14), -1);
        }
        context.method_51448().pushMatrix();
        long subNow = System.currentTimeMillis();
        float breathY = (float)Math.sin((double)subNow / 520.0) * 1.8f;
        float driftX = (float)Math.cos((double)subNow / 840.0) * 1.0f;
        context.method_51448().translate((float)subCenterX + driftX, (float)subCenterY + breathY);
        context.method_51448().scale(subScale, subScale);
        int startX = -textW / 2;
        int startY = -textH / 2;
        if (HudConfig.INSTANCE.subtitlesBgEnabled) {
            int bgAlpha = (int)(HudConfig.INSTANCE.subtitlesBgAlpha * 255.0f);
            int bgColor = bgAlpha << 24 | HudConfig.INSTANCE.subtitlesBgColor & 0xFFFFFF;
            MusicHudRenderer.renderRoundedRect(context, startX - padX, startY - padY, textW + padX * 2, textH + padY * 2, HudConfig.INSTANCE.subtitlesBgRadius, bgColor);
        }
        int textColor = 0xFF000000 | HudConfig.INSTANCE.subtitlesColor & 0xFFFFFF;
        int curDrawX = startX;
        for (int i = 0; i < previewText.length(); ++i) {
            char c = previewText.charAt(i);
            int charW;
            CustomFontManager.CustomGlyph g = isCustom ? CustomFontManager.getGlyph(font, c) : null;
            net.minecraft.class_5250 charComp = null;
            String renderStr = isRuneFont ? LyricsRenderer.transliterateForRune(c) : LyricsRenderer.singleCharString(c);
            if (renderStr.isEmpty()) renderStr = " ";

            if (isCustom) {
                charW = g != null ? Math.max(1, Math.round(g.advance)) : 6;
            } else if (isDefaultFont) {
                charW = this.field_22793.method_1727(renderStr);
            } else {
                charComp = LyricsRenderer.getCachedStyledChar(fontName, renderStr);
                charW = this.field_22793.method_27525(charComp);
            }

            if (HudConfig.INSTANCE.subtitlesTextShadow) {
                int shadowColor = 0x9005060A;
                if (isCustom && g != null) {
                    CustomFontManager.drawGlyph2D(context, g, curDrawX + 1, startY + 1, shadowColor);
                } else if (isDefaultFont) {
                    context.method_51433(this.field_22793, renderStr, curDrawX + 1, startY + 1, shadowColor, false);
                } else if (charComp != null) {
                    context.method_51439(this.field_22793, charComp, curDrawX + 1, startY + 1, shadowColor, false);
                }
            }

            if (isCustom && g != null) {
                CustomFontManager.drawGlyph2D(context, g, curDrawX, startY, textColor);
            } else if (isDefaultFont) {
                context.method_51433(this.field_22793, renderStr, curDrawX, startY, textColor, false);
            } else if (charComp != null) {
                context.method_51439(this.field_22793, charComp, curDrawX, startY, textColor, false);
            }
            curDrawX += charW;
        }
        context.method_51448().popMatrix();
    }

    private void triggerNiaEasterEgg(int originX, int originY) {
        String msg;
        ++this.niaEasterEggClicks;
        this.lastNiaClickTime = System.currentTimeMillis();
        this.screenShake = 3.2f;
        Random r = new Random();
        int count = this.niaEasterEggClicks % 10 == 0 ? 80 : 36;
        for (int i = 0; i < count; ++i) {
            float angle = (float)((double)r.nextFloat() * Math.PI * 2.0);
            float speed = 1.8f + r.nextFloat() * (this.niaEasterEggClicks % 10 == 0 ? 6.5f : 4.2f);
            float vx = (float)Math.cos(angle) * speed;
            float vy = (float)Math.sin(angle) * speed - 1.6f;
            String sym = switch (r.nextInt(6)) {
                case 0 -> "❤";
                case 1 -> "✦";
                case 2 -> "🌸";
                case 3 -> "★";
                case 4 -> "♪";
                default -> "♫";
            };
            int col = switch (r.nextInt(6)) {
                case 0 -> -49023;
                case 1 -> -10496;
                case 2 -> -16718337;
                case 3 -> -2080517;
                case 4 -> -32597;
                default -> -16711809;
            };
            int life = 35 + r.nextInt(35);
            this.easterEggParticles.add(new EasterEggParticle(originX, originY, vx, vy, col, sym, life));
        }
        try {
            if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                float pitch = 1.0f + (float)(this.niaEasterEggClicks % 8) * 0.12f;
                this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)((class_3414)class_3417.field_14793.comp_349()), pitch, 0.9f));
            }
        }
        catch (Exception ignored) {
        }
        if (this.niaEasterEggClicks % 10 == 0) {
            msg = "§6✨ МАКСИМАЛЬНАЯ СИЛА ВДОХНОВЕНИЯ НИИ! 🌸👑❤";
        } else {
            msg = switch (this.niaEasterEggClicks % 5) {
                case 1 -> "§d❤ Автор мода: Ния (Nia_Teppelin_0) ✦";
                case 2 -> "§b✦ «Музыка должна звучать красиво» — Ния ♫";
                case 3 -> "§e🌸 Секретный режим Нии активирован! ✨";
                case 4 -> "§d👑 Спасибо за то, что ты делаешь, Ния! ❤";
                default -> "§a✦ Заряд вдохновения Нии: §e" + this.niaEasterEggClicks * 100 + "% §d❤";
            };
        }
        this.triggerFeedback(msg, this.niaEasterEggClicks % 10 == 0 ? -10496 : 16728193);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        if (CustomBackgroundManager.INSTANCE.isAlertActive()) {
            this.handleAlertModalClick(mx, my);
            return true;
        }
        if (this.activeDropdownId != null) {
            if (mx >= this.activeDropdownX && mx <= this.activeDropdownX + this.activeDropdownW &&
                my >= this.activeDropdownY && my <= this.activeDropdownY + this.activeDropdownH) {
                int relY = my - (this.activeDropdownY + 3);
                int rowIdx = relY / 18;
                int actualIdx = this.activeDropdownScroll + rowIdx;
                if (actualIdx >= 0 && actualIdx < this.activeDropdownItems.size()) {
                    DropdownItem chosen = this.activeDropdownItems.get(actualIdx);
                    Consumer<String> cb = this.activeDropdownCallback;
                    this.closeDropdown();
                    this.playSound(class_3417.field_14709, 1.25f, 0.8f);
                    if (cb != null) {
                        cb.accept(chosen.key);
                    }
                }
                return true;
            }
            if (mx >= this.activeDropdownX && mx <= this.activeDropdownX + this.activeDropdownW &&
                my >= this.activeDropdownBtnY && my <= this.activeDropdownBtnY + 19) {
                this.closeDropdown();
                this.playSound(class_3417.field_14709, 1.0f, 0.8f);
                this.buildTabWidgets();
                return true;
            }
            this.closeDropdown();
            this.buildTabWidgets();
            return true;
        }
        if (!this.dragging && !this.dragging2DSub && !this.draggingConfig && !this.draggingDock) {
            int brandX = 4;
            int brandY = 4;
            int brandW = 158;
            int brandH = 34;
            if (mx >= brandX && mx <= brandX + brandW && my >= brandY && my <= brandY + brandH) {
                if (this.field_22787 != null) {
                    this.playSound(class_3417.field_14709, 1.2f, 0.8f);
                    this.field_22787.method_1507((class_437)new NiaAuthorScreen(this));
                    return true;
                }
            }
        }
        if (this.dockSide != 0) {
            if (mx >= this.dockX && mx <= this.dockX + this.dockW && my >= this.dockY && my <= this.dockY + this.dockH) {
                int slotH = 16;
                int startSlotY = this.dockY + (this.dockH - 12 * slotH) / 2;
                if (my < startSlotY) {
                    this.draggingDock = true;
                    this.dragDockStartX = mx;
                    this.dragDockStartY = my;
                    return true;
                }
                if (my >= startSlotY + 12 * slotH) {
                    return true;
                }
                int iconIdx = (my - startSlotY) / slotH;
                if (iconIdx == 0) {
                    this.draggingDock = true;
                    this.dragDockStartX = mx;
                    this.dragDockStartY = my;
                    return true;
                } else if (iconIdx == 1) {
                    if (this.currentTab == 0 && this.flyoutOpen) {
                        this.flyoutOpen = false;
                    } else {
                        this.currentTab = 0;
                        this.flyoutOpen = true;
                    }
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    this.playSound(class_3417.field_14709, 1.2f, 0.8f);
                    this.buildTabWidgets();
                    return true;
                } else if (iconIdx == 2) {
                    if (this.currentTab == 1 && this.flyoutOpen) {
                        this.flyoutOpen = false;
                    } else {
                        this.currentTab = 1;
                        this.flyoutOpen = true;
                    }
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    this.playSound(class_3417.field_14709, 1.2f, 0.8f);
                    this.buildTabWidgets();
                    return true;
                } else if (iconIdx == 3) {
                    if (this.currentTab == 2 && this.flyoutOpen) {
                        this.flyoutOpen = false;
                    } else {
                        this.currentTab = 2;
                        this.flyoutOpen = true;
                    }
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    if (this.paletteColorWheel != null) {
                        this.paletteColorWheel.setColorFromRgb(this.getCurrentTargetColor());
                    }
                    this.playSound(class_3417.field_14709, 1.2f, 0.8f);
                    this.buildTabWidgets();
                    return true;
                } else if (iconIdx == 4) {
                    if (this.currentTab == 3 && this.flyoutOpen) {
                        this.flyoutOpen = false;
                    } else {
                        this.currentTab = 3;
                        this.flyoutOpen = true;
                    }
                    this.screenShake = 0.0f;
                    this.tabSwitchStartTime = System.currentTimeMillis();
                    this.playSound(class_3417.field_14709, 1.2f, 0.8f);
                    this.buildTabWidgets();
                    return true;
                } else if (iconIdx == 5) {
                    HudConfig.INSTANCE.enabled = !HudConfig.INSTANCE.enabled;
                    this.screenShake = 0.0f;
                    this.playSound((class_6880<class_3414>)(HudConfig.INSTANCE.enabled ? class_3417.field_15015 : class_3417.field_14624), 1.2f, 0.8f);
                    HudConfig.save();
                    this.triggerFeedback(HudConfig.INSTANCE.enabled ? "§aМод включен" : "§cМод выключен", HudConfig.INSTANCE.enabled ? 58879 : 0xFF5F56);
                    this.buildTabWidgets();
                    return true;
                } else if (iconIdx == 6) {
                    if (this.field_22787 != null) {
                        this.field_22787.method_1507((class_437)new FavoritesScreen(this));
                    }
                    return true;
                } else if (iconIdx == 7) {
                    if (this.field_22787 != null) {
                        this.field_22787.method_1507((class_437)new KeybindSetupScreen(this));
                    }
                    return true;
                } else if (iconIdx == 8) {
                    if (this.field_22787 != null) {
                        this.field_22787.method_1507((class_437)new SystemDiagnosticsScreen(this, new KeybindSetupScreen(this)));
                    }
                    return true;
                } else if (iconIdx == 9) {
                    if (this.field_22787 != null) {
                        this.field_22787.method_1507((class_437)new TwitchSettingsScreen(this));
                    }
                    return true;
                } else if (iconIdx == 10) {
                    this.undockToFloating(-1, -1);
                    return true;
                } else if (iconIdx == 11) {
                    this.method_25419();
                    return true;
                }
            }
            if (this.flyoutOpen) {
                int flyoutHeaderH = 22;
                int buttonsLeftLimit = this.panelX + this.panelWidth - 44;
                if (mx >= this.panelX && mx < buttonsLeftLimit && my >= this.panelY && my <= this.panelY + flyoutHeaderH) {
                    this.draggingConfig = true;
                    this.dragConfigOffsetX = mx - this.panelX;
                    this.dragConfigOffsetY = my - this.panelY;
                    return true;
                }
            }
        }
        if (this.dockSide == 0) {
            int headerH = 24;
            int buttonsLeftLimit = this.panelX + this.panelWidth - 156;
            if (mx >= this.panelX && mx < buttonsLeftLimit && my >= this.panelY && my <= this.panelY + headerH) {
                this.draggingConfig = true;
                this.dragConfigOffsetX = mx - this.panelX;
                this.dragConfigOffsetY = my - this.panelY;
                return true;
            }
        }
        if (this.currentTab == 2 && this.paletteColorWheel != null && (this.dockSide == 0 || this.flyoutOpen) && this.paletteColorWheel.mouseClicked(mx, my)) {
            return true;
        }
        if (mx >= this.hudX && mx <= this.hudX + this.hudWidth && my >= this.hudY && my <= this.hudY + this.hudHeight) {
            this.dragging = true;
            this.dragOffsetX = mx - this.hudX;
            this.dragOffsetY = my - this.hudY;
            return true;
        }
        boolean is2DSub = HudConfig.INSTANCE.subtitlesEnabled && !"WORLD_3D".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"WORLD_BLOCK".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"WORLD_SURFACE".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"3D_HEAD".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"CHAT".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"OFF".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode);
        if (is2DSub) {
            int subCenterX = (int)((double)this.field_22789 * HudConfig.INSTANCE.sub2DPositionX);
            int subCenterY = (int)((double)this.field_22790 * HudConfig.INSTANCE.sub2DPositionY);
            int boxX = subCenterX - this.subPreviewW / 2;
            int boxY = subCenterY - this.subPreviewH / 2;
            if (mx >= boxX && mx <= boxX + this.subPreviewW && my >= boxY && my <= boxY + this.subPreviewH) {
                this.dragging2DSub = true;
                this.dragSubOffsetX = mx - subCenterX;
                this.dragSubOffsetY = my - subCenterY;
                return true;
            }
        }
        return super.method_25402(click, bl);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        if (this.draggingDock) {
            if (this.dockSide == 1 && mx > 38) {
                this.undockToFloating(mx, my);
                this.draggingConfig = true;
                this.dragConfigOffsetX = this.panelWidth / 2;
                this.dragConfigOffsetY = 14;
                return true;
            } else if (this.dockSide == 2 && mx < this.field_22789 - 38) {
                this.undockToFloating(mx, my);
                this.draggingConfig = true;
                this.dragConfigOffsetX = this.panelWidth / 2;
                this.dragConfigOffsetY = 14;
                return true;
            }
            return true;
        }
        if (this.draggingConfig) {
            if (this.dockSide != 0) {
                if (this.dockSide == 1 && mx > this.dockW + 55) {
                    this.undockToFloating(mx, my);
                    this.dragConfigOffsetX = this.panelWidth / 2;
                    this.dragConfigOffsetY = 14;
                    return true;
                } else if (this.dockSide == 2 && mx < this.field_22789 - this.dockW - 55) {
                    this.undockToFloating(mx, my);
                    this.dragConfigOffsetX = this.panelWidth / 2;
                    this.dragConfigOffsetY = 14;
                    return true;
                }
            } else {
                int newX = mx - this.dragConfigOffsetX;
                int newY = my - this.dragConfigOffsetY;
                newX = Math.max(0, Math.min(newX, this.field_22789 - this.panelWidth));
                newY = Math.max(0, Math.min(newY, this.field_22790 - this.panelHeight));
                if (newX != this.panelX || newY != this.panelY) {
                    this.panelX = newX;
                    this.panelY = newY;
                    this.initColorWheels();
                    this.buildTabWidgets();
                }
                return true;
            }
        }
        if (this.currentTab == 2 && this.paletteColorWheel != null && (this.dockSide == 0 || this.flyoutOpen) && this.paletteColorWheel.mouseDragged(mx, my)) {
            return true;
        }
        if (this.dragging) {
            this.hudX = mx - this.dragOffsetX;
            this.hudY = my - this.dragOffsetY;
            this.hudX = Math.max(0, Math.min(this.hudX, this.field_22789 - this.hudWidth));
            this.hudY = Math.max(0, Math.min(this.hudY, this.field_22790 - this.hudHeight));
            int snapThreshold = 15;
            if (this.hudX < snapThreshold) {
                this.hudX = 0;
            }
            if (this.field_22789 - (this.hudX + this.hudWidth) < snapThreshold) {
                this.hudX = this.field_22789 - this.hudWidth;
            }
            if (this.hudY < snapThreshold) {
                this.hudY = 0;
            }
            if (this.field_22790 - (this.hudY + this.hudHeight) < snapThreshold) {
                this.hudY = this.field_22790 - this.hudHeight;
            }
            return true;
        }
        boolean is2DBottom = HudConfig.INSTANCE.subtitlesEnabled && !"WORLD_3D".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"WORLD_BLOCK".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"WORLD_SURFACE".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"3D_HEAD".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"CHAT".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode) && !"OFF".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesMode);
        if (this.dragging2DSub && is2DBottom) {
            int newCenterX = mx - this.dragSubOffsetX;
            int newCenterY = my - this.dragSubOffsetY;
            if (Math.abs(newCenterX - this.field_22789 / 2) < 16) {
                newCenterX = this.field_22789 / 2;
            }
            HudConfig.INSTANCE.sub2DPositionX = class_3532.method_15350((double)newCenterX / (double)this.field_22789, 0.05, 0.95);
            HudConfig.INSTANCE.sub2DPositionY = class_3532.method_15350((double)newCenterY / (double)this.field_22790, 0.05, 0.95);
            return true;
        }
        return super.method_25403(click, deltaX, deltaY);
    }

    public boolean method_25406(class_11909 click) {
        if (this.draggingDock) {
            this.draggingDock = false;
            return true;
        }
        if (this.draggingConfig) {
            this.draggingConfig = false;
            if (this.dockSide == 0) {
                if (this.panelX <= 28) {
                    this.dockTo(1);
                    return true;
                } else if (this.panelX + this.panelWidth >= this.field_22789 - 28) {
                    this.dockTo(2);
                    return true;
                }
            }
            return true;
        }
        if (this.currentTab == 2 && this.paletteColorWheel != null) {
            boolean wasWheelDragging = this.paletteColorWheel.isDragging();
            this.paletteColorWheel.mouseReleased();
            if (wasWheelDragging) {
                HudConfig.save();
            }
        }
        if (this.dragging) {
            this.dragging = false;
            HudConfig.INSTANCE.setFromAbsolute(this.hudX, this.hudY, this.field_22789, this.field_22790, this.hudWidth, this.hudHeight);
            HudConfig.save();
            return true;
        }
        if (this.dragging2DSub) {
            this.dragging2DSub = false;
            HudConfig.save();
            return true;
        }
        return super.method_25406(click);
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25419() {
        this.closeDropdown();
        HudConfig.INSTANCE.setFromAbsolute(this.hudX, this.hudY, this.field_22789, this.field_22790, this.hudWidth, this.hudHeight);
        HudConfig.save();
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        } else {
            super.method_25419();
        }
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256 && this.activeDropdownId != null) {
            this.closeDropdown();
            this.buildTabWidgets();
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.activeDropdownId != null) {
            int mx = (int)mouseX;
            int my = (int)mouseY;
            if (mx >= this.activeDropdownX && mx <= this.activeDropdownX + this.activeDropdownW &&
                my >= this.activeDropdownY && my <= this.activeDropdownY + this.activeDropdownH) {
                int maxScroll = Math.max(0, this.activeDropdownItems.size() - this.activeDropdownMaxVisible);
                if (maxScroll > 0) {
                    if (verticalAmount > 0) {
                        this.activeDropdownScroll = Math.max(0, this.activeDropdownScroll - 1);
                    } else if (verticalAmount < 0) {
                        this.activeDropdownScroll = Math.min(maxScroll, this.activeDropdownScroll + 1);
                    }
                    return true;
                }
            }
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void triggerFeedback(String message, int color) {
        this.feedbackToast = message;
        this.feedbackToastTime = System.currentTimeMillis();
        this.feedbackColor = color;
    }

    private void renderFeedbackToast(class_332 context) {
        float alpha;
        if (this.feedbackToast == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.feedbackToastTime;
        if (elapsed > 1800L) {
            this.feedbackToast = null;
            return;
        }
        int animYOffset = 0;
        if (elapsed < 200L) {
            alpha = (float)elapsed / 200.0f;
            animYOffset = (int)((1.0f - alpha) * 6.0f);
        } else {
            alpha = elapsed > 1400L ? 1.0f - (float)(elapsed - 1400L) / 400.0f : 1.0f;
        }
        alpha = class_3532.method_15363(alpha, 0.0f, 1.0f);
        int a255 = (int)(alpha * 255.0f);
        if (a255 <= 0) {
            return;
        }
        int toastW = this.field_22793.method_1727(this.feedbackToast) + 26;
        int toastH = 18;
        int toastX = this.panelX + (this.panelWidth - toastW) / 2;
        int toastY = this.panelY + this.panelHeight - toastH - 8 + animYOffset;
        int bgCol = (int)(alpha * 240.0f) << 24 | 0x12141E;
        int borderCol = (int)(alpha * 220.0f) << 24 | this.feedbackColor & 0xFFFFFF;
        MusicHudRenderer.renderRoundedRect(context, toastX, toastY, toastW, toastH, 7, bgCol);
        MusicHudRenderer.renderRoundedBorder(context, toastX, toastY, toastW, toastH, 7, borderCol, borderCol);
        int dotCol = (int)(alpha * 255.0f) << 24 | this.feedbackColor & 0xFFFFFF;
        MusicHudRenderer.renderRoundedRect(context, toastX + 7, toastY + 6, 6, 6, 3, dotCol);
        context.method_51433(this.field_22793, this.feedbackToast, toastX + 18, toastY + 5, -1, true);
    }

    private void renderInvalidBackgroundAlertModal(class_332 context, int mouseX, int mouseY) {
        if (!CustomBackgroundManager.INSTANCE.isAlertActive()) {
            return;
        }
        if (!this.alertSoundPlayed) {
            this.alertSoundPlayed = true;
            try {
                if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                    this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)((class_3414)class_3417.field_14624.comp_349()), 0.6f, 0.9f));
                }
            }
            catch (Exception ignored) {
            }
        }
        context.method_25294(0, 0, this.field_22789, this.field_22790, -2013265920);
        int modalW = Math.min(420, this.field_22789 - 24);
        int modalH = 185;
        int modalX = (this.field_22789 - modalW) / 2;
        int modalY = (this.field_22790 - modalH) / 2;
        MusicHudRenderer.renderRoundedRect(context, modalX, modalY, modalW, modalH, 12, -267119074);
        MusicHudRenderer.renderRoundedBorder(context, modalX, modalY, modalW, modalH, 12, -56781, -1996536764);
        context.method_51433(this.field_22793, "§c§l[!] Ошибка чтения файла", modalX + 16, modalY + 14, -1, true);
        context.method_51433(this.field_22793, "§fВ папке §eyandexmusichud/backgrounds §fнайден повреждённый файл", modalX + 16, modalY + 28, -1118482, false);
        CustomBackgroundManager.InvalidFileEntry inv = CustomBackgroundManager.INSTANCE.getFirstInvalidFile();
        String fName = inv != null ? inv.fileName() : "неизвестный файл";
        String reason = inv != null ? inv.reason() : "Формат не поддерживается!";
        int cardW = modalW - 32;
        int cardH = 48;
        int cardY = modalY + 44;
        MusicHudRenderer.renderRoundedRect(context, modalX + 16, cardY, cardW, cardH, 6, 1157571123);
        MusicHudRenderer.renderRoundedBorder(context, modalX + 16, cardY, cardW, cardH, 6, -1996545485, 0x44FF4444);
        context.method_51433(this.field_22793, "§e• Файл: §f" + fName, modalX + 22, cardY + 6, -1, false);
        List<String> wrapped = this.wrapText(reason, cardW - 16);
        for (int i = 0; i < Math.min(2, wrapped.size()); ++i) {
            context.method_51433(this.field_22793, "§c" + wrapped.get(i), modalX + 22, cardY + 19 + i * 11, -1, false);
        }
        context.method_51433(this.field_22793, "§a✔ Поддерживаются форматы: .PNG, .JPG, .JPEG, .BMP", modalX + 16, modalY + 101, -11141291, false);
        boolean canConvert = inv != null && inv.canAutoConvert();
        int btnCount = canConvert ? 3 : 2;
        int gap = 6;
        int btnW = (cardW - gap * (btnCount - 1)) / btnCount;
        int btnY = modalY + 122;
        int btnH = 22;
        int curBtnX = modalX + 16;
        boolean hoverF = mouseX >= curBtnX && mouseX <= curBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        MusicHudRenderer.renderRoundedRect(context, curBtnX, btnY, btnW, btnH, 5, hoverF ? 0xFF3B4C66 : 0xDD283448);
        MusicHudRenderer.renderRoundedBorder(context, curBtnX, btnY, btnW, btnH, 5, hoverF ? 0xCC80D8FF : 0x5580D8FF, hoverF ? 0xCC80D8FF : 0x5580D8FF);
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("Папка фонов"), curBtnX + btnW / 2, btnY + 7, -1);
        curBtnX += btnW + gap;
        if (canConvert) {
            boolean hoverC = mouseX >= curBtnX && mouseX <= curBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            MusicHudRenderer.renderRoundedRect(context, curBtnX, btnY, btnW, btnH, 5, hoverC ? 0xFF00B4D8 : 0xDD008DA8);
            MusicHudRenderer.renderRoundedBorder(context, curBtnX, btnY, btnW, btnH, 5, hoverC ? 0xFFFFFFFF : 0xAA00E5FF, hoverC ? 0xFFFFFFFF : 0xAA00E5FF);
            context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("В формат .PNG"), curBtnX + btnW / 2, btnY + 7, -1);
            curBtnX += btnW + gap;
        }
        boolean hoverR = mouseX >= curBtnX && mouseX <= curBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        MusicHudRenderer.renderRoundedRect(context, curBtnX, btnY, btnW, btnH, 5, hoverR ? 0xFF28A745 : 0xDD1E7E34);
        MusicHudRenderer.renderRoundedBorder(context, curBtnX, btnY, btnW, btnH, 5, hoverR ? 0xFF80FF80 : 0xAA80FF80, hoverR ? 0xFF80FF80 : 0xAA80FF80);
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("Проверить"), curBtnX + btnW / 2, btnY + 7, -1);
        int closeX = modalX + modalW - 24;
        int closeY = modalY + 10;
        boolean hoverClose = mouseX >= closeX - 4 && mouseX <= closeX + 18 && mouseY >= closeY - 2 && mouseY <= closeY + 16;
        context.method_51433(this.field_22793, hoverClose ? "§c§l[X]" : "§7[X]", closeX, closeY, -1, true);
    }

    private boolean handleAlertModalClick(int mx, int my) {
        if (!CustomBackgroundManager.INSTANCE.isAlertActive()) {
            return false;
        }
        int modalW = Math.min(420, this.field_22789 - 24);
        int modalH = 185;
        int modalX = (this.field_22789 - modalW) / 2;
        int modalY = (this.field_22790 - modalH) / 2;
        if (mx < modalX || mx > modalX + modalW || my < modalY || my > modalY + modalH) {
            CustomBackgroundManager.INSTANCE.dismissAlert();
            this.triggerFeedback("§7Предупреждение скрыто", 0xAAAAAA);
            return true;
        }
        int closeX = modalX + modalW - 24;
        int closeY = modalY + 10;
        if (mx >= closeX - 4 && mx <= closeX + 18 && my >= closeY - 2 && my <= closeY + 16) {
            CustomBackgroundManager.INSTANCE.dismissAlert();
            this.triggerFeedback("§7Предупреждение скрыто", 0xAAAAAA);
            return true;
        }
        CustomBackgroundManager.InvalidFileEntry inv = CustomBackgroundManager.INSTANCE.getFirstInvalidFile();
        boolean canConvert = inv != null && inv.canAutoConvert();
        int cardW = modalW - 32;
        int btnCount = canConvert ? 3 : 2;
        int gap = 6;
        int btnW = (cardW - gap * (btnCount - 1)) / btnCount;
        int btnY = modalY + 122;
        int btnH = 22;
        int curBtnX = modalX + 16;
        if (mx >= curBtnX && mx <= curBtnX + btnW && my >= btnY && my <= btnY + btnH) {
            CustomBackgroundManager.INSTANCE.openBackgroundsFolder();
            this.triggerFeedback("§a📁 Открыта папка backgrounds", 58879);
            return true;
        }
        curBtnX += btnW + gap;
        if (canConvert) {
            if (mx >= curBtnX && mx <= curBtnX + btnW && my >= btnY && my <= btnY + btnH) {
                if (inv != null) {
                    boolean ok = CustomBackgroundManager.INSTANCE.autoConvertFile(inv.fileName());
                    if (ok) {
                        this.refreshBackgroundList();
                        this.buildTabWidgets();
                        this.triggerFeedback("§a✔ Файл успешно конвертирован в PNG!", 58879);
                    } else {
                        this.triggerFeedback("§cОшибка конвертации файла", 0xFF5F56);
                    }
                }
                return true;
            }
            curBtnX += btnW + gap;
        }
        if (mx >= curBtnX && mx <= curBtnX + btnW && my >= btnY && my <= btnY + btnH) {
            CustomBackgroundManager.INSTANCE.scanBackgrounds();
            this.refreshBackgroundList();
            this.buildTabWidgets();
            if (CustomBackgroundManager.INSTANCE.hasInvalidFiles()) {
                this.triggerFeedback("§c⚠ Ошибки всё ещё не устранены!", 0xFF5F56);
            } else {
                this.triggerFeedback("§a✔ Все файлы проверены: ошибок нет!", 58879);
            }
            return true;
        }
        return mx >= modalX && mx <= modalX + modalW && my >= modalY && my <= modalY + modalH;
    }

    public record HelpEntry(int x, int y, int w, int h, String title, String description, String recommendation) {
    }

    private static class CursorSparkle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        String symbol;
        int age = 0;
        int maxLife;

        CursorSparkle(float x, float y, float vx, float vy, int color, String symbol, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.symbol = symbol;
            this.maxLife = maxLife;
        }

        boolean update() {
            this.x += this.vx;
            this.y += this.vy;
            this.vx *= 0.94f;
            this.vy *= 0.94f;
            return ++this.age < this.maxLife;
        }
    }

    private static class EasterEggParticle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        String symbol;
        int age = 0;
        int maxLife;

        EasterEggParticle(float x, float y, float vx, float vy, int color, String symbol, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.symbol = symbol;
            this.maxLife = maxLife;
        }

        boolean update() {
            this.x += this.vx;
            this.y += this.vy;
            this.vx *= 0.98f;
            this.vy *= 0.96f;
            return ++this.age < this.maxLife;
        }
    }

    private static String formatEngineLabel(String engine) {
        if ("WHISPER".equalsIgnoreCase(engine)) {
            return "ИИ: §eWhisper";
        }
        if ("GOOGLE".equalsIgnoreCase(engine)) {
            return "ИИ: §7Google";
        }
        return "ИИ: §bАвто";
    }
}
