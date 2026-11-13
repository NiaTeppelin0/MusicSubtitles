package com.example.yandexmusichud.keybinds;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.favorites.FavoritesManager;
import com.example.yandexmusichud.hud.FavoritesScreen;
import com.example.yandexmusichud.hud.HudEditScreen;
import com.example.yandexmusichud.hud.RadialMenuScreen;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;

public class MusicControlKeybinds {
    private static final class_304.class_11900 CATEGORY = class_304.class_11900.method_74698((class_2960)class_2960.method_60655("yandexmusichud", "music_subtitles"));
    public static class_304 playPauseKey;
    public static class_304 nextTrackKey;
    public static class_304 prevTrackKey;
    public static class_304 toggleHudKey;
    public static class_304 editHudKey;
    public static class_304 toggleSubtitlesKey;
    public static class_304 syncEarlierKey;
    public static class_304 syncLaterKey;
    public static class_304 toggleFavoriteKey;
    public static class_304 openFavoritesKey;
    public static class_304 radialMenuKey;
    public static class_304 localMusicKey;

    private static NowPlayingClient activeClient;
    private static CoverTextureManager activeCoverManager;

    public static void register() {
        playPauseKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.play_pause", class_3675.class_307.field_1668, -1, CATEGORY));
        nextTrackKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.next_track", class_3675.class_307.field_1668, -1, CATEGORY));
        prevTrackKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.prev_track", class_3675.class_307.field_1668, -1, CATEGORY));
        toggleHudKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.toggle_hud", class_3675.class_307.field_1668, -1, CATEGORY));
        editHudKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.edit_hud", class_3675.class_307.field_1668, -1, CATEGORY));
        toggleSubtitlesKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.toggle_subtitles", class_3675.class_307.field_1668, -1, CATEGORY));
        syncEarlierKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.sync_earlier", class_3675.class_307.field_1668, -1, CATEGORY));
        syncLaterKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.sync_later", class_3675.class_307.field_1668, -1, CATEGORY));
        toggleFavoriteKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.toggle_favorite", class_3675.class_307.field_1668, 75, CATEGORY));
        openFavoritesKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.open_favorites", class_3675.class_307.field_1668, 74, CATEGORY));
        radialMenuKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.radial_menu", class_3675.class_307.field_1668, 82, CATEGORY));
        localMusicKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.yandexmusichud.local_music", class_3675.class_307.field_1668, 77, CATEGORY));
    }

    public static void handleTick(NowPlayingClient client, CoverTextureManager coverManager) {
        activeClient = client;
        activeCoverManager = coverManager;

        class_310 mc = class_310.method_1551();
        if (mc == null) return;

        if (mc.field_1755 != null) {
            drainAllKeys();
            return;
        }

        if (radialMenuKey != null) {
            while (radialMenuKey.method_1436()) {
                if (activeClient != null && activeCoverManager != null) {
                    mc.method_1507((class_437)new RadialMenuScreen(activeClient, activeCoverManager));
                }
            }
        }
        if (localMusicKey != null) {
            while (localMusicKey.method_1436()) {
                mc.method_1507((class_437)new com.example.yandexmusichud.hud.LocalMusicScreen(null));
            }
        }
        if (playPauseKey != null) {
            while (playPauseKey.method_1436()) {
                if (com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().isPlaying() || com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().isPaused()) {
                    com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().togglePlayPause();
                } else if (activeClient != null) {
                    activeClient.sendPlayPauseAsync();
                }
            }
        }
        if (nextTrackKey != null) {
            while (nextTrackKey.method_1436()) {
                if (com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().isPlaying() || com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().isPaused()) {
                    com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().next();
                } else if (activeClient != null) {
                    activeClient.sendNextAsync();
                }
            }
        }
        if (prevTrackKey != null) {
            while (prevTrackKey.method_1436()) {
                if (com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().isPlaying() || com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().isPaused()) {
                    com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().previous();
                } else if (activeClient != null) {
                    activeClient.sendPreviousAsync();
                }
            }
        }
        if (toggleFavoriteKey != null) {
            while (toggleFavoriteKey.method_1436()) {
                if (activeClient != null && activeCoverManager != null) {
                    TrackState state = activeClient.getState();
                    if (state != null && state.isActive() && !state.title().isEmpty()) {
                        boolean isFav = FavoritesManager.INSTANCE.toggleFavorite(state, activeCoverManager);
                        if (mc.field_1705 != null) {
                            String msg = isFav ? "\u00a76[Music Subtitles] \u00a7e\u2605 \u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e \u0432 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0435: \u00a7f" + state.artist() + " - " + state.title() : "\u00a76[Music Subtitles] \u00a77\u2606 \u0423\u0434\u0430\u043b\u0435\u043d\u043e \u0438\u0437 \u0438\u0437\u0431\u0440\u0430\u043d\u043d\u043e\u0433\u043e: \u00a7f" + state.artist() + " - " + state.title();
                            mc.field_1705.method_1758((class_2561)class_2561.method_43470(msg), false);
                        }
                    }
                }
            }
        }
        if (openFavoritesKey != null) {
            while (openFavoritesKey.method_1436()) {
                mc.method_1507((class_437)new FavoritesScreen(null));
            }
        }
        if (toggleHudKey != null) {
            while (toggleHudKey.method_1436()) {
                HudConfig.INSTANCE.enabled = !HudConfig.INSTANCE.enabled;
                HudConfig.save();
                if (mc.field_1705 != null) {
                    String status = HudConfig.INSTANCE.enabled ? "\u00a7a\u0412\u041a\u041b" : "\u00a7c\u0412\u042b\u041a\u041b";
                    mc.field_1705.method_1758((class_2561)class_2561.method_43470("\u00a76[Music Subtitles] \u00a7f\u041f\u043b\u0430\u0448\u043a\u0430 HUD: " + status), false);
                }
            }
        }
        if (editHudKey != null) {
            while (editHudKey.method_1436()) {
                mc.method_1507((class_437)new HudEditScreen());
            }
        }
        if (toggleSubtitlesKey != null) {
            while (toggleSubtitlesKey.method_1436()) {
                cycleSubtitlesMode(mc);
            }
        }
        if (syncEarlierKey != null) {
            while (syncEarlierKey.method_1436()) {
                if (activeClient != null) adjustTrackSync(activeClient, -200, mc);
            }
        }
        if (syncLaterKey != null) {
            while (syncLaterKey.method_1436()) {
                if (activeClient != null) adjustTrackSync(activeClient, 200, mc);
            }
        }
    }

    private static void drainAllKeys() {
        if (playPauseKey != null) while (playPauseKey.method_1436()) {}
        if (nextTrackKey != null) while (nextTrackKey.method_1436()) {}
        if (prevTrackKey != null) while (prevTrackKey.method_1436()) {}
        if (toggleHudKey != null) while (toggleHudKey.method_1436()) {}
        if (editHudKey != null) while (editHudKey.method_1436()) {}
        if (toggleSubtitlesKey != null) while (toggleSubtitlesKey.method_1436()) {}
        if (syncEarlierKey != null) while (syncEarlierKey.method_1436()) {}
        if (syncLaterKey != null) while (syncLaterKey.method_1436()) {}
        if (toggleFavoriteKey != null) while (toggleFavoriteKey.method_1436()) {}
        if (openFavoritesKey != null) while (openFavoritesKey.method_1436()) {}
        if (radialMenuKey != null) while (radialMenuKey.method_1436()) {}
        if (localMusicKey != null) while (localMusicKey.method_1436()) {}
    }

    private static void cycleSubtitlesMode(class_310 mc) {
        if (!HudConfig.INSTANCE.subtitlesEnabled) {
            HudConfig.INSTANCE.subtitlesEnabled = true;
            HudConfig.INSTANCE.subtitlesMode = "WORLD_3D";
        } else {
            String curMode = HudConfig.INSTANCE.subtitlesMode != null ? HudConfig.INSTANCE.subtitlesMode : "WORLD_3D";
            switch (curMode) {
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
        if (mc.field_1705 != null) {
            String modeName;
            if (!HudConfig.INSTANCE.subtitlesEnabled) {
                modeName = "\u00a7c\u0412\u042b\u041a\u041b";
            } else {
                String safeMode = HudConfig.INSTANCE.subtitlesMode != null ? HudConfig.INSTANCE.subtitlesMode : "";
                modeName = switch (safeMode) {
                    case "WORLD_3D" -> "\u00a7a3D \u0432 \u043c\u0438\u0440\u0435";
                    case "WORLD_BLOCK", "WORLD_SURFACE" -> "\u00a7b3D \u043d\u0430 \u0431\u043b\u043e\u043a\u0430\u0445";
                    case "HUD_BOTTOM" -> "\u00a7e2D \u0441\u043d\u0438\u0437\u0443";
                    case "CHAT" -> "\u00a7b\u0412 \u0447\u0430\u0442\u0435";
                    default -> "\u00a7d2D \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u043e";
                };
            }
            mc.field_1705.method_1758((class_2561)class_2561.method_43470(("\u00a76[Music Subtitles] \u00a7f\u0420\u0435\u0436\u0438\u043c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432: " + modeName)), false);
        }
    }


    private static void adjustTrackSync(NowPlayingClient client, int deltaMs, class_310 mc) {
        TrackState state = client.getState();
        if (state == null || !state.isActive() || state.title().isEmpty()) {
            return;
        }
        int current = HudConfig.INSTANCE.getTrackSyncOffset(state.artist(), state.title());
        int newOffset = current + deltaMs;
        HudConfig.INSTANCE.setTrackSyncOffset(state.artist(), state.title(), newOffset);
        if (mc.field_1705 != null) {
            String sign = newOffset >= 0 ? "+" : "";
            double sec = (double)newOffset / 1000.0;
            mc.field_1705.method_1758((class_2561)class_2561.method_43470(String.format("\u00a76[Music Subtitles] \u00a7f\u0421\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0430\u0446\u0438\u044f \u0442\u0440\u0435\u043a\u0430: \u00a7e%s%.2f\u0441 \u00a77(\u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e)", sign, sec)), false);
        }
    }
}

