package com.example.yandexmusichud.favorites;

import com.example.yandexmusichud.favorites.FavoriteTrack;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.awt.Desktop;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_156;
import net.minecraft.class_310;

public class FavoritesManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FAVORITES_FILE = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("favorites.json");
    public static final FavoritesManager INSTANCE = new FavoritesManager();
    private final Map<String, FavoriteTrack> favoritesMap = new ConcurrentHashMap<String, FavoriteTrack>();
    private final List<FavoriteTrack> favoritesList = new ArrayList<FavoriteTrack>();

    public FavoritesManager() {
        this.load();
    }

    public synchronized void load() {
        try {
            if (Files.exists(FAVORITES_FILE, new LinkOption[0])) {
                String json = Files.readString(FAVORITES_FILE, StandardCharsets.UTF_8);
                Type type = new TypeToken<List<FavoriteTrack>>(){}.getType();
                List<FavoriteTrack> list = GSON.fromJson(json, type);
                this.favoritesMap.clear();
                this.favoritesList.clear();
                if (list != null) {
                    for (FavoriteTrack track : list) {
                        if (track.id == null || track.id.isEmpty()) {
                            track.id = FavoriteTrack.createId(track.artist, track.title);
                        }
                        this.favoritesMap.put(track.id, track);
                        this.favoritesList.add(track);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(FAVORITES_FILE.getParent(), new FileAttribute[0]);
            String json = GSON.toJson(this.favoritesList);
            Files.writeString(FAVORITES_FILE, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isFavorite(String artist, String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String id = FavoriteTrack.createId(artist, title);
        return this.favoritesMap.containsKey(id);
    }

    public synchronized boolean toggleFavorite(TrackState state, CoverTextureManager coverManager) {
        if (state == null || !state.isActive() || state.title().isBlank()) {
            return false;
        }
        String id = FavoriteTrack.createId(state.artist(), state.title());
        if (this.favoritesMap.containsKey(id)) {
            this.removeFavorite(id);
            return false;
        }
        String coverB64 = coverManager != null ? coverManager.getLastBase64() : null;
        FavoriteTrack track = new FavoriteTrack(state.title(), state.artist(), state.album(), state.platform(), state.platformName(), state.playUrl(), coverB64);
        this.addFavorite(track);
        return true;
    }

    public synchronized void addFavorite(FavoriteTrack track) {
        if (track == null || track.id == null) {
            return;
        }
        this.favoritesMap.put(track.id, track);
        this.favoritesList.removeIf(t -> t.id.equals(track.id));
        this.favoritesList.add(0, track);
        this.save();
    }

    public synchronized void removeFavorite(String id) {
        if (id == null) {
            return;
        }
        this.favoritesMap.remove(id);
        this.favoritesList.removeIf(t -> t.id.equals(id));
        this.save();
    }

    public synchronized List<FavoriteTrack> getFavorites() {
        return Collections.unmodifiableList(new ArrayList<FavoriteTrack>(this.favoritesList));
    }

    public static boolean isSafeHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) && host != null && !host.isBlank();
        }
        catch (Exception e) {
            return false;
        }
    }

    public static void openUrlSafely(String url) {
        if (!FavoritesManager.isSafeHttpUrl(url)) {
            return;
        }
        String finalUrl = url.trim();
        CompletableFuture.runAsync(() -> {
            boolean opened = false;
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                try {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", finalUrl).start();
                    opened = true;
                }
                catch (Throwable ignored) {
                }
                if (!opened) {
                    try {
                        new ProcessBuilder("explorer.exe", finalUrl).start();
                        opened = true;
                    }
                    catch (Throwable ignored) {
                    }
                }
            } else if (os.contains("mac")) {
                try {
                    new ProcessBuilder("open", finalUrl).start();
                    opened = true;
                }
                catch (Throwable ignored) {
                }
            } else {
                try {
                    new ProcessBuilder("xdg-open", finalUrl).start();
                    opened = true;
                }
                catch (Throwable ignored) {
                }
            }
            if (!opened) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(finalUrl));
                        opened = true;
                    }
                }
                catch (Throwable ignored) {
                }
            }
            if (!opened) {
                try {
                    class_156.method_668().method_673(new URI(finalUrl));
                }
                catch (Throwable ignored) {
                }
            }
        });
    }

    public static void openFolderSafely(Path folder) {
        if (folder == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                if (!Files.exists(folder, new LinkOption[0])) {
                    Files.createDirectories(folder, new FileAttribute[0]);
                }
                String absPath = folder.toAbsolutePath().toString();
                boolean opened = false;
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("win")) {
                    try {
                        new ProcessBuilder("explorer.exe", absPath).start();
                        opened = true;
                    }
                    catch (Throwable ignored) {
                    }
                } else if (os.contains("mac")) {
                    try {
                        new ProcessBuilder("open", absPath).start();
                        opened = true;
                    }
                    catch (Throwable ignored) {
                    }
                } else {
                    try {
                        new ProcessBuilder("xdg-open", absPath).start();
                        opened = true;
                    }
                    catch (Throwable ignored) {
                    }
                }
                if (!opened) {
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                            Desktop.getDesktop().open(folder.toFile());
                            opened = true;
                        }
                    }
                    catch (Throwable ignored) {
                    }
                }
                if (!opened) {
                    try {
                        class_156.method_668().method_672(folder.toFile());
                    }
                    catch (Throwable ignored) {
                    }
                }
            }
            catch (Throwable ignored) {
            }
        });
    }

    public static void playTrack(FavoriteTrack track) {
        class_310 mc;
        if (track == null) {
            return;
        }
        String url = track.playUrl;
        if (!FavoritesManager.isSafeHttpUrl(url)) {
            url = TrackState.generatePlayUrl(track.title, track.artist);
        }
        if (!FavoritesManager.isSafeHttpUrl(url)) {
            url = "https://music.yandex.ru";
        }
        String finalUrl = url.trim();
        if (!FavoritesManager.isSafeHttpUrl(finalUrl)) {
            return;
        }
        if (track.playUrl == null || track.playUrl.isBlank() || !FavoritesManager.isSafeHttpUrl(track.playUrl)) {
            track.playUrl = finalUrl;
            INSTANCE.save();
        }
        if ((mc = class_310.method_1551()) != null && mc.field_1774 != null) {
            mc.execute(() -> mc.field_1774.method_1455(finalUrl));
        }
        FavoritesManager.openUrlSafely(finalUrl);
    }
}

