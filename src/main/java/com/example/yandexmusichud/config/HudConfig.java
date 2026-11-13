package com.example.yandexmusichud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public class HudConfig {
    public static HudConfig INSTANCE = new HudConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud.json");
    public boolean enabled = true;
    public String hudStyle = "MINIMAL_DARK";
    public double hudPositionX = 0.98;
    public double hudPositionY = 0.02;
    public int configDockSide = 0;
    public int configBackgroundColor = 0x0A0D14;
    public String anchor = "TOP_RIGHT";
    public float hudScale = 1.0f;
    public int hudBackgroundColor = 0x0A0D14;
    public float hudBackgroundAlpha = 0.88f;
    public int cornerRadius = 10;
    public boolean showProgressBar = true;
    public int progressBarColor = -13312;
    public int progressBarHeight = 2;
    public String hudDisplayType = "ALWAYS";
    public int popupDurationSeconds = 6;
    public boolean autoAdaptLayout = true;
    public String customBackgroundFile = "";
    public float customBackgroundAlpha = 1.0f;
    public int pollIntervalMs = 300;
    public int hideDelaySeconds = 5;
    public int helperPort = 0;
    public boolean pulseSyncIntegration = true;
    public int pulseSyncPort = 2007;
    public boolean subtitlesEnabled = true;
    public String subtitlesMode = "WORLD_3D";
    public float subtitlesScale = 1.0f;
    public float subtitlesDistance = 2.8f;
    public int subtitlesColor = -1;
    public boolean subtitlesBgEnabled = false;
    public boolean subtitlesSeeThrough = false;
    public int subtitlesBgColor = 0x11111E;
    public float subtitlesBgAlpha = 0.6f;
    public int subtitlesBgRadius = 4;
    public boolean subtitlesTextShadow = true;
    public int subtitlesOffsetMs = 0;
    public boolean subtitlesVisibleInF1 = true;
    public String subtitlesAnimation = "KARAOKE_BOUNCE";
    public int subtitlesActiveColor = -10496;
    public String subtitlesFont = "minecraft:default";
    public boolean showIntroSplash = true;
    public String hudSize = "LARGE";
    public boolean showEqualizer = true;
    public boolean beatBounce = true;
    public boolean hudMiniKaraoke = true;
    public double sub2DPositionX = 0.5;
    public double sub2DPositionY = 0.82;
    public float localMusicVolume = 0.8f;
    public boolean localMusicShuffle = false;
    public boolean localMusicRepeat = false;
    public boolean sprintWordsEnabled = true;
    public float sprintWordsDistance = 3.5f;
    public float sprintWordsHeight = 0.0f;
    public float sprintWordsSpread = 0.35f;
    public int sprintWordsChunkSize = 2;
    public int sprintWordsTrailLength = 5;
    public boolean showHelpInspector = true;
    public int radialLastSelectedIndex = 0;
    public boolean smartDuckingEnabled = true;
    public boolean audioAnalysisEnabled = true;
    public String yandexMusicToken = "";
    public String yandexSignKey = "";
    public boolean twitchEnabled = false;
    public String twitchChannel = "";
    public boolean twitchShowSubs = true;
    public boolean twitchShowRewards = true;
    public boolean twitchShowBits = true;
    public boolean twitchPlaySound = true;
    public int twitchToastDurationSeconds = 5;
    public String twitchToastPosition = "TOP";
    public boolean twitchAutoCommandEnabled = false;
    public boolean playerNametagBadge = true;
    public boolean showOwnNametagBadge = true;
    public boolean tabBadgeEnabled = true;
    public String relayServerUrl = "ws://5.83.140.251:25766";
    public boolean relayServerEnabled = true;
    public boolean safeMode = true;
    public String twitchAutoCommand = "/stream live";
    public boolean daEnabled = true;
    public String daToken = "";
    public boolean daAlertsInHud = true;
    public boolean daSoundEnabled = true;
    public int daMinAmountAlert = 0;
    public boolean hasSeenTutorial = false;
    public boolean initialSetupCompleted = false;
    public String musicSourcePreset = "ALL";
    public boolean promptYandexMusicOnStartup = true;
    public String lastSeenChangelogVersion = "";
    public Map<String, Integer> trackOffsets = new HashMap<String, Integer>();
    public Map<String, String> trackStyles = new HashMap<String, String>();
    public transient int hudWidth = 260;
    public transient int hudHeight = 74;

    public int getHudBaseWidth() {
        if ("DYNAMIC_ISLAND".equalsIgnoreCase(this.hudStyle)) {
            return "LARGE".equalsIgnoreCase(this.hudSize) ? 256 : 196;
        }
        return "LARGE".equalsIgnoreCase(this.hudSize) ? 260 : 200;
    }

    public int getHudBaseHeight() {
        if ("DYNAMIC_ISLAND".equalsIgnoreCase(this.hudStyle)) {
            return "LARGE".equalsIgnoreCase(this.hudSize) ? 54 : 44;
        }
        return "LARGE".equalsIgnoreCase(this.hudSize) ? 74 : 62;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH, new LinkOption[0])) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = (HudConfig)GSON.fromJson(json, HudConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new HudConfig();
                }
                if (HudConfig.INSTANCE.hudStyle == null || HudConfig.INSTANCE.hudStyle.isEmpty()) {
                    HudConfig.INSTANCE.hudStyle = "MINIMAL_DARK";
                }
                if (HudConfig.INSTANCE.hudBackgroundColor == 1710638 && HudConfig.INSTANCE.cornerRadius == 0) {
                    HudConfig.INSTANCE.hudBackgroundColor = 0x0A0D14;
                    HudConfig.INSTANCE.hudBackgroundAlpha = 0.88f;
                    HudConfig.INSTANCE.cornerRadius = 10;
                }
                if (HudConfig.INSTANCE.configBackgroundColor == 0x0D0F18) {
                    HudConfig.INSTANCE.configBackgroundColor = 0x0A0D14;
                }
                if (HudConfig.INSTANCE.trackOffsets == null) {
                    HudConfig.INSTANCE.trackOffsets = new HashMap<String, Integer>();
                }
                if (HudConfig.INSTANCE.trackStyles == null) {
                    HudConfig.INSTANCE.trackStyles = new HashMap<String, String>();
                }
                if (HudConfig.INSTANCE.yandexMusicToken != null) {
                    HudConfig.INSTANCE.yandexMusicToken = com.example.yandexmusichud.hud.YandexMusicSettingsScreen.sanitizeToken(HudConfig.INSTANCE.yandexMusicToken);
                }
                if (HudConfig.INSTANCE.yandexSignKey == null) {
                    HudConfig.INSTANCE.yandexSignKey = "";
                }
            }
            catch (Exception e) {
                try {
                    Path backupPath = CONFIG_PATH.resolveSibling("yandexmusichud.json.broken-" + System.currentTimeMillis());
                    Files.copy(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (Exception ignored) {
                }
                INSTANCE = new HudConfig();
            }
        }
        HudConfig.save();
    }

    public static synchronized void save() {
        try {
            if (INSTANCE != null && INSTANCE.yandexMusicToken != null) {
                INSTANCE.yandexMusicToken = com.example.yandexmusichud.hud.YandexMusicSettingsScreen.sanitizeToken(INSTANCE.yandexMusicToken);
            }
            Files.createDirectories(CONFIG_PATH.getParent(), new FileAttribute[0]);
            Path tmpPath = CONFIG_PATH.resolveSibling("yandexmusichud.json.tmp");
            Files.writeString(tmpPath, (CharSequence)GSON.toJson((Object)INSTANCE), new OpenOption[0]);
            try {
                Files.move(tmpPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (Exception moveEx) {
                Files.move(tmpPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException iOException) {

        }
    }

    public void setTrackStyle(String artist, String title, int color, int activeColor, String mode, float scale) {
        this.setTrackStyle(artist, title, color, activeColor, mode, scale, "minecraft:default");
    }

    private static String buildTrackKey(String artist, String title) {
        return (artist + "___" + title).toLowerCase(Locale.ROOT);
    }

    public void setTrackStyle(String artist, String title, int color, int activeColor, String mode, float scale, String font) {
        if (this.trackStyles == null) {
            this.trackStyles = new HashMap<String, String>();
        }
        if (artist == null || title == null) {
            return;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        String safeFont = (font != null && !font.isEmpty() && !"minecraft".equalsIgnoreCase(font.trim())) ? font.trim() : "minecraft:default";
        this.trackStyles.put(key, color + ":" + activeColor + ":" + mode + ":" + scale + ":" + safeFont);
    }

    public String getTrackFont(String artist, String title, String defaultVal) {
        if (this.trackStyles == null || this.trackStyles.isEmpty() || artist == null || title == null) {
            return defaultVal;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        String val = this.trackStyles.get(key);
        if (val != null) {
            String[] parts = val.split(":", 5);
            if (parts.length >= 5) {
                String f = parts[4].trim();
                if ("minecraft".equalsIgnoreCase(f) || f.isEmpty()) {
                    return "minecraft:default";
                }
                return f;
            }
        }
        return defaultVal;
    }

    public int getTrackColor(String artist, String title, int defaultVal) {
        if (this.trackStyles == null || this.trackStyles.isEmpty() || artist == null || title == null) {
            return defaultVal;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        String val = this.trackStyles.get(key);
        if (val != null) {
            String[] parts = val.split(":");
            if (parts.length >= 1) {
                try {
                    return Integer.parseInt(parts[0]);
                } catch (Exception ignored) {}
            }
        }
        return defaultVal;
    }

    public int getTrackActiveColor(String artist, String title, int defaultVal) {
        if (this.trackStyles == null || this.trackStyles.isEmpty() || artist == null || title == null) {
            return defaultVal;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        String val = this.trackStyles.get(key);
        if (val != null) {
            String[] parts = val.split(":");
            if (parts.length >= 2) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (Exception ignored) {}
            }
        }
        return defaultVal;
    }

    public String getTrackMode(String artist, String title, String defaultVal) {
        if (this.trackStyles == null || this.trackStyles.isEmpty() || artist == null || title == null) {
            return defaultVal;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        String val = this.trackStyles.get(key);
        if (val != null) {
            String[] parts = val.split(":");
            if (parts.length >= 3 && !parts[2].equalsIgnoreCase("DEFAULT")) {
                return parts[2];
            }
        }
        return defaultVal;
    }

    public float getTrackScale(String artist, String title, float defaultVal) {
        if (this.trackStyles == null || this.trackStyles.isEmpty() || artist == null || title == null) {
            return defaultVal;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        String val = this.trackStyles.get(key);
        if (val != null) {
            String[] parts = val.split(":");
            if (parts.length >= 4) {
                try {
                    return Float.parseFloat(parts[3]);
                } catch (Exception ignored) {}
            }
        }
        return defaultVal;
    }

    public int getTrackSyncOffset(String artist, String title) {
        if (this.trackOffsets == null || this.trackOffsets.isEmpty() || artist == null || title == null) {
            return 0;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        return this.trackOffsets.getOrDefault(key, 0);
    }

    public void setTrackSyncOffset(String artist, String title, int offsetMs) {
        if (this.trackOffsets == null) {
            this.trackOffsets = new HashMap<String, Integer>();
        }
        if (artist == null || title == null) {
            return;
        }
        String key = HudConfig.buildTrackKey(artist, title);
        if (offsetMs == 0) {
            this.trackOffsets.remove(key);
        } else {
            this.trackOffsets.put(key, offsetMs);
        }
        HudConfig.save();
    }


    public int getAbsoluteX(int screenWidth, int widgetWidth) {
        return switch (this.anchor) {
            case "TOP_RIGHT", "BOTTOM_RIGHT" -> (int)((double)screenWidth * this.hudPositionX) - widgetWidth;
            case "TOP_LEFT", "BOTTOM_LEFT" -> (int)((double)screenWidth * this.hudPositionX);
            default -> (int)((double)screenWidth * this.hudPositionX) - widgetWidth / 2;
        };
    }

    public int getAbsoluteY(int screenHeight, int widgetHeight) {
        return switch (this.anchor) {
            case "BOTTOM_LEFT", "BOTTOM_RIGHT" -> (int)((double)screenHeight * this.hudPositionY) - widgetHeight;
            case "TOP_LEFT", "TOP_RIGHT" -> (int)((double)screenHeight * this.hudPositionY);
            default -> (int)((double)screenHeight * this.hudPositionY) - widgetHeight / 2;
        };
    }

    public void setFromAbsolute(int x, int y, int screenWidth, int screenHeight, int widgetWidth, int widgetHeight) {
        int snapThreshold = 15;
        boolean nearLeft = x < snapThreshold;
        boolean nearRight = screenWidth - (x + widgetWidth) < snapThreshold;
        boolean nearTop = y < snapThreshold;
        boolean nearBottom = screenHeight - (y + widgetHeight) < snapThreshold;
        boolean bl = nearBottom;
        if (nearRight && nearTop) {
            this.anchor = "TOP_RIGHT";
            this.hudPositionX = (double)(x + widgetWidth) / (double)screenWidth;
            this.hudPositionY = (double)y / (double)screenHeight;
        } else if (nearLeft && nearTop) {
            this.anchor = "TOP_LEFT";
            this.hudPositionX = (double)x / (double)screenWidth;
            this.hudPositionY = (double)y / (double)screenHeight;
        } else if (nearRight && nearBottom) {
            this.anchor = "BOTTOM_RIGHT";
            this.hudPositionX = (double)(x + widgetWidth) / (double)screenWidth;
            this.hudPositionY = (double)(y + widgetHeight) / (double)screenHeight;
        } else if (nearLeft && nearBottom) {
            this.anchor = "BOTTOM_LEFT";
            this.hudPositionX = (double)x / (double)screenWidth;
            this.hudPositionY = (double)(y + widgetHeight) / (double)screenHeight;
        } else {
            this.anchor = "CUSTOM";
            this.hudPositionX = (double)(x + widgetWidth / 2) / (double)screenWidth;
            this.hudPositionY = (double)(y + widgetHeight / 2) / (double)screenHeight;
        }
    }

    public void resetToDefault() {
        this.hudStyle = "MINIMAL_DARK";
        this.hudPositionX = 0.98;
        this.hudPositionY = 0.02;
        this.anchor = "TOP_RIGHT";
        this.configBackgroundColor = 0x0A0D14;
        this.hudScale = 1.0f;
        this.hudBackgroundColor = 0x0A0D14;
        this.hudBackgroundAlpha = 0.88f;
        this.cornerRadius = 10;
        this.showProgressBar = true;
        this.progressBarColor = -13312;
        this.progressBarHeight = 2;
        this.hudDisplayType = "ALWAYS";
        this.popupDurationSeconds = 6;
        this.autoAdaptLayout = true;
        this.customBackgroundFile = "";
        this.customBackgroundAlpha = 1.0f;
        this.pollIntervalMs = 300;
        this.subtitlesEnabled = true;
        this.subtitlesMode = "WORLD_3D";
        this.subtitlesScale = 1.0f;
        this.subtitlesDistance = 2.8f;
        this.subtitlesColor = -1;
        this.subtitlesBgEnabled = false;
        this.subtitlesSeeThrough = false;
        this.subtitlesBgColor = 0x11111E;
        this.subtitlesBgAlpha = 0.6f;
        this.subtitlesBgRadius = 4;
        this.subtitlesTextShadow = true;
        this.subtitlesOffsetMs = 0;
        this.subtitlesFont = "minecraft:default";
        this.hudSize = "LARGE";
        this.showEqualizer = true;
        this.beatBounce = true;
        this.hudMiniKaraoke = true;
        this.sub2DPositionX = 0.5;
        this.sub2DPositionY = 0.82;
        this.sprintWordsEnabled = true;
        this.sprintWordsDistance = 3.5f;
        this.sprintWordsHeight = 0.0f;
        this.sprintWordsSpread = 0.35f;
        this.sprintWordsChunkSize = 2;
        this.sprintWordsTrailLength = 5;
        this.radialLastSelectedIndex = 0;
        this.smartDuckingEnabled = true;
    }
}

