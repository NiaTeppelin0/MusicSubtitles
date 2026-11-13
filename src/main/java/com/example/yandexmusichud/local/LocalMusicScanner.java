package com.example.yandexmusichud.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public class LocalMusicScanner {
    public static final LocalMusicScanner INSTANCE = new LocalMusicScanner();

    public Path getMusicDirectory() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("music");
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException ignored) {}
        return dir;
    }

    public List<LocalMusicTrack> scanTracks() {
        Path dir = this.getMusicDirectory();
        List<LocalMusicTrack> tracks = new ArrayList<>();
        if (!Files.exists(dir)) {
            return tracks;
        }

        try (Stream<Path> stream = Files.walk(dir, 5)) {
            stream.filter(Files::isRegularFile).filter(p -> {
                String s = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return s.endsWith(".mp3") || s.endsWith(".wav") || s.endsWith(".ogg");
            }).forEach(p -> {
                try {
                    tracks.add(LocalMusicTrack.fromFile(p));
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        tracks.sort(Comparator.comparing(LocalMusicTrack::getArtist, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LocalMusicTrack::getTitle, String.CASE_INSENSITIVE_ORDER));
        return tracks;
    }
}
