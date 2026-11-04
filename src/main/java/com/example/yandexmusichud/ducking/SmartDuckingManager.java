package com.example.yandexmusichud.ducking;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.helper.HelperProcessManager;
import com.example.yandexmusichud.local.LocalMusicPlayer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public class SmartDuckingManager {
    public static final SmartDuckingManager INSTANCE = new SmartDuckingManager();
    private boolean voiceChatAvailable = false;
    private Method getClientMethod;
    private Method getTalkCacheMethod;
    private Method getMicThreadMethod;
    private Method isMicTalkingMethod;
    private Field playerCacheField;
    private Field lastTalkField;
    private volatile boolean duckingActive = false;
    private volatile float currentDuckingFactor = 1.0f;
    private long lastVoiceActivityTime = 0L;

    private SmartDuckingManager() {
        this.initVoiceChatReflection();
    }

    private void initVoiceChatReflection() {
        if (!FabricLoader.getInstance().isModLoaded("voicechat")) {
            return;
        }
        try {
            Class<?> clientManagerClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            this.getClientMethod = clientManagerClass.getMethod("getClient", new Class[0]);
            Class<?> clientVoicechatClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientVoicechat");
            this.getTalkCacheMethod = clientVoicechatClass.getMethod("getTalkCache", new Class[0]);
            this.getMicThreadMethod = clientVoicechatClass.getMethod("getMicThread", new Class[0]);
            Class<?> micThreadClass = Class.forName("de.maxhenkel.voicechat.voice.client.MicThread");
            this.isMicTalkingMethod = micThreadClass.getMethod("isTalking", new Class[0]);
            Class<?> talkCacheClass = Class.forName("de.maxhenkel.voicechat.voice.client.TalkCache");
            this.playerCacheField = talkCacheClass.getDeclaredField("playerCache");
            this.playerCacheField.setAccessible(true);
            Class<?> playerCacheClass = Class.forName("de.maxhenkel.voicechat.voice.client.TalkCache$PlayerCache");
            this.lastTalkField = playerCacheClass.getDeclaredField("lastTalk");
            this.lastTalkField.setAccessible(true);
            this.voiceChatAvailable = true;
        }
        catch (Throwable ignored) {
            this.voiceChatAvailable = false;
        }
    }

    public void onTick() {
        if (!HudConfig.INSTANCE.smartDuckingEnabled || !this.voiceChatAvailable) {
            if (this.duckingActive || this.currentDuckingFactor < 1.0f) {
                this.duckingActive = false;
                this.currentDuckingFactor = 1.0f;
                if (HelperProcessManager.INSTANCE != null) {
                    HelperProcessManager.INSTANCE.applyExternalDuckingFactor(1.0f);
                }
            }
            return;
        }
        boolean voiceTalking = this.checkVoiceChatTalking();
        long now = System.currentTimeMillis();
        if (voiceTalking) {
            this.lastVoiceActivityTime = now;
        }
        boolean shouldDuck = now - this.lastVoiceActivityTime < 800L;
        this.duckingActive = shouldDuck;
        float target = shouldDuck ? 0.35f : 1.0f;
        float next = this.currentDuckingFactor + (target - this.currentDuckingFactor) * 0.2f;
        if (Math.abs(next - target) < 0.01f) {
            next = target;
        }
        this.currentDuckingFactor = next;

        if (HelperProcessManager.INSTANCE != null) {
            boolean localActive = LocalMusicPlayer.getInstance().isPlaying() || LocalMusicPlayer.getInstance().isPaused();
            HelperProcessManager.INSTANCE.applyExternalDuckingFactor(localActive ? 1.0f : next);
        }
    }

    private boolean checkVoiceChatTalking() {
        try {
            if (this.getClientMethod == null) {
                return false;
            }
            Object client = this.getClientMethod.invoke(null, new Object[0]);
            if (client == null) {
                return false;
            }
            if (this.getMicThreadMethod != null && this.isMicTalkingMethod != null) {
                Object micThread = this.getMicThreadMethod.invoke(client, new Object[0]);
                if (micThread != null && Boolean.TRUE.equals(this.isMicTalkingMethod.invoke(micThread, new Object[0]))) {
                    return true;
                }
            }
            if (this.getTalkCacheMethod != null && this.playerCacheField != null && this.lastTalkField != null) {
                Object talkCache = this.getTalkCacheMethod.invoke(client, new Object[0]);
                if (talkCache != null) {
                    Object cacheObj = this.playerCacheField.get(talkCache);
                    if (cacheObj instanceof Map<?, ?> map) {
                        Object[] snapshot = null;
                        for (int attempt = 0; attempt < 3; attempt++) {
                            try {
                                snapshot = map.values().toArray();
                                break;
                            } catch (RuntimeException ignored) {}
                        }
                        if (snapshot != null) {
                            long now = System.currentTimeMillis();
                            for (Object playerCache : snapshot) {
                                if (playerCache != null && now - this.lastTalkField.getLong(playerCache) < 500L) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable ignored) {}
        return false;
    }

    public boolean isDuckingActive() {
        return this.duckingActive;
    }

    public float getCurrentDuckingFactor() {
        return this.currentDuckingFactor;
    }

    public boolean isVoiceChatAvailable() {
        return this.voiceChatAvailable;
    }
}
