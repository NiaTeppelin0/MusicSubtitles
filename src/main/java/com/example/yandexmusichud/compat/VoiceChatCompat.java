package com.example.yandexmusichud.compat;

import com.example.yandexmusichud.config.HudConfig;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.plugins.impl.VoicechatClientApiImpl;
import net.fabricmc.loader.api.FabricLoader;

public class VoiceChatCompat {
    public static final String CATEGORY_ID = "music_subtitles";
    private static boolean initialized = false;
    private static boolean voiceChatPresent = false;
    private static volatile VoicechatClientApi cachedApi = null;
    private static volatile long lastVolumeCheckTime = 0L;
    private static volatile float cachedVolumeMultiplier = 1.0f;
    private static String currentRoom = null;
    private static String currentHost = null;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            voiceChatPresent = FabricLoader.getInstance().isModLoaded("voicechat");
            if (voiceChatPresent) {
                registerCategorySafe();
            }
        } catch (Throwable ignored) {
            voiceChatPresent = false;
        }
    }

    public static boolean isVoiceChatAvailable() {
        if (!initialized) {
            init();
        }
        return voiceChatPresent;
    }

    private static VoicechatClientApi getApi() {
        VoicechatClientApi api = cachedApi;
        if (api == null) {
            api = VoicechatClientApiImpl.instance();
            if (api != null) {
                cachedApi = api;
            }
        }
        return api;
    }

    private static void registerCategorySafe() {
        try {
            VoicechatClientApi api = getApi();
            if (api != null) {
                VolumeCategory category = api.volumeCategoryBuilder()
                    .setId(CATEGORY_ID)
                    .setName("Музыка (Music Subtitles)")
                    .setDescription("Фоновая музыка совместного прослушивания")
                    .build();
                api.registerClientVolumeCategory(category);
            }
        } catch (Throwable ignored) {
        }
    }

    public static float getCategoryVolumeMultiplier() {
        if (!isVoiceChatAvailable()) {
            return 1.0f;
        }
        long now = System.currentTimeMillis();
        if (now - lastVolumeCheckTime < 150L) {
            return cachedVolumeMultiplier;
        }
        lastVolumeCheckTime = now;
        try {
            VoicechatClientApi api = getApi();
            if (api != null && api.getVolumeConfig() != null) {
                double vol = api.getVolumeConfig().getCategoryVolume(CATEGORY_ID);
                float mult = (float) Math.max(0.0, Math.min(2.0, vol));
                cachedVolumeMultiplier = mult;
                return mult;
            }
        } catch (Throwable ignored) {
        }
        return cachedVolumeMultiplier;
    }

    public static float getEffectiveVolume(float baseModVolume) {
        float vcMult = getCategoryVolumeMultiplier();
        return Math.max(0.0f, Math.min(1.0f, baseModVolume * vcMult));
    }

    public static synchronized void onRoomJoined(String room, String host) {
        currentRoom = room;
        currentHost = host;
    }

    public static synchronized void onRoomLeft() {
        currentRoom = null;
        currentHost = null;
    }

    public static synchronized boolean isRoomProtected(String host) {
        if (currentRoom == null || currentRoom.isEmpty()) {
            return false;
        }
        if (host != null && !host.isEmpty() && currentHost != null && !currentHost.isEmpty()) {
            return host.equalsIgnoreCase(currentHost);
        }
        return true;
    }

    public static synchronized boolean isInActiveRoom() {
        return currentRoom != null && !currentRoom.isEmpty();
    }
}
