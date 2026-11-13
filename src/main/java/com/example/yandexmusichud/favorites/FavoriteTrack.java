package com.example.yandexmusichud.favorites;

import java.util.Locale;

public class FavoriteTrack {
    public String id;
    public String title;
    public String artist;
    public String album;
    public String platform;
    public String platformName;
    public String playUrl;
    public long addedAt;
    public String coverBase64;

    public FavoriteTrack() {
    }

    public FavoriteTrack(String title, String artist, String album, String platform, String platformName, String playUrl, String coverBase64) {
        this.title = title != null ? title.trim() : "";
        this.artist = artist != null ? artist.trim() : "";
        this.album = album != null ? album.trim() : "";
        this.id = FavoriteTrack.createId(this.artist, this.title);
        this.platform = platform != null ? platform : "music";
        this.platformName = platformName != null ? platformName : "\u041c\u0443\u0437\u044b\u043a\u0430";
        this.playUrl = playUrl != null ? playUrl : "";
        this.addedAt = System.currentTimeMillis();
        this.coverBase64 = coverBase64;
    }

    public static String createId(String artist, String title) {
        String a = artist != null ? artist.trim().toLowerCase(Locale.ROOT) : "";
        String t = title != null ? title.trim().toLowerCase(Locale.ROOT) : "";
        return a + "___" + t;
    }
}

