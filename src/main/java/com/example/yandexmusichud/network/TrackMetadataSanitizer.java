package com.example.yandexmusichud.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackMetadataSanitizer {
    private static final Pattern TRACK_NUMBER_PREFIX = Pattern.compile("^(?:track\\s*)?\\d{1,3}[.\\-_\\s]+(?=[A-Za-zА-Яа-яЁё0-9])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RADIO_PREFIX = Pattern.compile("^(?:record|радио\\s*рекорд|dfm|europa\\s*plus|европа\\s*плюс|energy|nrj|kiss\\s*fm|maximum|максимум|nashe|наше\\s*радио|retro\\s*fm|ретро\\s*фм)\\s*[:\\-|•~]+\\s*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VIDEO_NOISE_TAGS = Pattern.compile("(?iu)[\\(\\[\\{][^\\)\\]\\}]*(?:official\\s*(?:music\\s*)?video|official\\s*video|official\\s*audio|audio\\s*track|lyric\\s*video|lyrics?|official\\s*visualizer|visualizer|video\\s*clip|music\\s*video|clip|\u043a\u043b\u0438\u043f|\u0432\u0438\u0434\u0435\u043e|\u043f\u0440\u0435\u043c\u044c\u0435\u0440\u0430|\u0430\u0443\u0434\u0438\u043e|\\baudio\\b|\u0441\u043d\u0438\u043f\u043f\u0435\u0442|\u043c\u0443\u0434\\s*\u0432\u0438\u0434\u0435\u043e|\u0442\u0435\u043a\u0441\u0442\\s*\u043f\u0435\u0441\u043d\u0438|\u0441\u043b\u043e\u0432\u0430\\s*\u043f\u0435\u0441\u043d\u0438|(?:\u0440\u0443\u0441\u0441\u043a\u0438\u0435\\s*)?\u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b|\u0442\u0435\u043a\u0441\u0442|\u043f\u0435\u0440\u0435\u0432\u043e\u0434|karaoke|instrumental|tiktok[^\\)\\]\\}]*|shorts|prod\\.?[^\\)\\]\\}]*|mv|hd|4k(?:\\s*60fps)?|1080p|720p|hq|uhd|full\\s*hd|soundtrack|ost[^\\)\\]\\}]*|original\\s*soundtrack|remaster(?:ed)?|live\\s*performance|live\\s*session)[^\\)\\]\\}]*[\\)\\]\\}]");
    private static final Pattern MODIFIER_NOISE_TAGS = Pattern.compile("(?iu)[\\(\\[\\{][^\\)\\]\\}]*(?:slowed(?:\\s*(?:\\+|&|and)?\\s*reverb)?|sped\\s*up|speed\\s*up|nightcore|fast\\s*version|reverb|bass\\s*boost(?:ed)?|ultra\\s*slowed|super\\s*slowed|tiktok\\s*(?:version|remix|edit)|prod\\.?\\s*by[^\\)\\]\\}]*)[^\\)\\]\\}]*[\\)\\]\\}]");
    private static final Pattern TRAILING_MODIFIER_NOISE = Pattern.compile("(?iu)\\s*(?:[\\|\u2022\u00b7~\\-\u2013\u2014]|\\s*\\u002F\\u002F)\\s*(?:slowed(?:\\s*(?:\\+|&|and)?\\s*reverb)?|sped\\s*up|speed\\s*up|nightcore|fast\\s*version|bass\\s*boost(?:ed)?|lyrics?|\u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b|\u0442\u0435\u043a\u0441\u0442\\s*\u043f\u0435\u0441\u043d\u0438)\\s*$");
    private static final Pattern PROMO_TRAILING_NOISE = Pattern.compile("(?iu)\\s*(?:[\\|•·~\\-–—]|\\s*\u002F\u002F)\\s*(?:премьера.*|official.*|клип.*|video.*|audio.*|lyric.*|текст\\s*песни.*|слова\\s*песни.*|release.*|out\\s*now.*)$");
    private static final Pattern YEAR_TAGS = Pattern.compile("(?i)[\\(\\[](?:20[0-2]\\d|19[5-9]\\d)[\\)\\]]");
    private static final Pattern BROWSER_TRAILING_NOISE = Pattern.compile("(?iu)\\s*(?:[\\|•·~\\-–—]\\s*(?:youtube(?:\\s*music)?|soundcloud|spotify|vk|вконтакте|yandex|яндекс(?:\\s*музыка)?|слушать(?:\\s*онлайн)?(?:\\s*на\\s*яндекс\\s*музыке)?|apple\\s*music|deezer|music|video|audio))+\\s*$");
    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("(?i)\\.(?:mp3|wav|ogg|flac|m4a|aac|wma|opus)$");
    private static final Pattern FEAT_IN_TITLE_PATTERN = Pattern.compile("(?i)[\\(\\[](?:feat\\.?|ft\\.?|featuring|with)\\s+([^\\]\\)]+)[\\)\\]]");
    private static final Pattern FEAT_BARE_PATTERN = Pattern.compile("(?i)\\s+(?:feat\\.?|ft\\.?|featuring)\\s+([^\\(\\)\\[\\]\\-]+)");
    private static final Pattern REMASTER_PATTERN = Pattern.compile("(?i)[\\(\\[](?:\\d{4}\\s*remaster(?:ed)?|remaster(?:ed)?(?:\\s*\\d{4})?|deluxe(?:\\s*edition)?|anniversary(?:\\s*edition)?|special(?:\\s*edition)?|bonus\\s*track|radio\\s*edit|extended\\s*(?:mix|version)|album\\s*version)[\\)\\]]");
    private static final Pattern LIVE_PATTERN = Pattern.compile("(?iu)\\b(?:live|концерт|живой\\s*звук|live\\s*at|live\\s*from|live\\s*in)\\b");
    private static final Pattern REMIX_PATTERN = Pattern.compile("(?iu)\\b(?:remix|rmx|ремикс|club\\s*mix|vip\\s*mix|dub\\s*mix|edit)\\b");
    private static final Pattern SPEED_UP_PATTERN = Pattern.compile("(?i)\\b(?:speed\\s*up|nightcore|sped\\s*up|fast\\s*version)\\b");
    private static final Pattern SLOWED_PATTERN = Pattern.compile("(?i)\\b(?:slowed(?:\\s*(?:\\+|and)?\\s*reverb)?|reverb)\\b");
    private static final Pattern ACOUSTIC_PATTERN = Pattern.compile("(?iu)\\b(?:acoustic|акустика|unplugged|piano\\s*version)\\b");
    private static final Pattern TOPIC_SUFFIX_PATTERN = Pattern.compile("(?iu)\\s*-\\s*(?:topic|\u0442\u0435\u043c\u0430)$");
    private static final Pattern QUOTES_PATTERN = Pattern.compile("[\"\'«»“”„]");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern FEATURED_SPLIT_PATTERN = Pattern.compile("[,&/]|(?i)\\s+(?:and|x)\\s+");
    private static final Pattern PRIMARY_ARTIST_SPLIT_PATTERN = Pattern.compile("(?i)\\s+(?:feat\\.?|ft\\.?|featuring|&|x|vs\\.?)\\s+|,\\s*");

    public record SanitizedMetadata(
        String cleanTitle,
        String cleanArtist,
        String searchTitle,
        String searchArtist,
        String primaryArtist,
        List<String> featuredArtists,
        String rawTitle,
        String rawArtist,
        boolean isLive,
        boolean isRemix,
        boolean isAcoustic,
        boolean isSpeedUp,
        boolean isSlowed
    ) {}

    public static SanitizedMetadata sanitize(String rawTitle, String rawArtist, String rawAlbum) {
        String t = rawTitle != null ? rawTitle.trim() : "";
        String a = rawArtist != null ? rawArtist.trim() : "";

        t = BROWSER_TRAILING_NOISE.matcher(t).replaceFirst("");
        a = BROWSER_TRAILING_NOISE.matcher(a).replaceFirst("");

        t = PROMO_TRAILING_NOISE.matcher(t).replaceFirst("");
        a = PROMO_TRAILING_NOISE.matcher(a).replaceFirst("");

        t = normalizeDashes(t);
        a = normalizeDashes(a);
        a = TOPIC_SUFFIX_PATTERN.matcher(a).replaceFirst("").trim();

        t = FILE_EXTENSION_PATTERN.matcher(t).replaceFirst("");
        a = FILE_EXTENSION_PATTERN.matcher(a).replaceFirst("");

        t = RADIO_PREFIX.matcher(t).replaceFirst("");
        t = TRACK_NUMBER_PREFIX.matcher(t).replaceFirst("");

        boolean hasGenericArtist = isGenericArtist(a);

        if (hasGenericArtist || a.isEmpty()) {
            ParsedPair split = splitArtistTitle(t);
            if (split != null) {
                a = split.artist;
                t = split.title;
            }
        } else if (t.contains(" - ") && !hasGenericArtist) {
            ParsedPair split = splitArtistTitle(t);
            if (split != null) {
                if (isLikelyArtist(split.artist, a)) {
                    t = split.title;
                } else if (isLikelyArtist(split.title, a)) {
                    t = split.artist;
                } else if (!isVersionOrSubTag(split.title) && !isVersionOrSubTag(split.artist)) {
                    a = split.artist;
                    t = split.title;
                }
            }
        }
        t = PROMO_TRAILING_NOISE.matcher(t).replaceFirst("");

        if (isLikelyInverted(a, t)) {
            String temp = a;
            a = t;
            t = temp;
        }

        boolean isLive = LIVE_PATTERN.matcher(t).find() || LIVE_PATTERN.matcher(a).find();
        boolean isRemix = REMIX_PATTERN.matcher(t).find() || REMIX_PATTERN.matcher(a).find();
        boolean isAcoustic = ACOUSTIC_PATTERN.matcher(t).find() || ACOUSTIC_PATTERN.matcher(a).find();
        boolean isSpeedUp = SPEED_UP_PATTERN.matcher(t).find() || SPEED_UP_PATTERN.matcher(a).find();
        boolean isSlowed = SLOWED_PATTERN.matcher(t).find() || SLOWED_PATTERN.matcher(a).find();

        List<String> featuredArtists = new ArrayList<String>();
        Matcher featInTitle = FEAT_IN_TITLE_PATTERN.matcher(t);
        if (featInTitle.find()) {
            addFeaturedArtists(featuredArtists, featInTitle.group(1));
            t = featInTitle.replaceFirst("").trim();
        }

        Matcher featBare = FEAT_BARE_PATTERN.matcher(t);
        if (featBare.find()) {
            addFeaturedArtists(featuredArtists, featBare.group(1));
            t = featBare.replaceFirst("").trim();
        }

        Matcher featInArtist = FEAT_IN_TITLE_PATTERN.matcher(a);
        if (featInArtist.find()) {
            addFeaturedArtists(featuredArtists, featInArtist.group(1));
            a = featInArtist.replaceFirst("").trim();
        }

        Matcher featBareArtist = FEAT_BARE_PATTERN.matcher(a);
        if (featBareArtist.find()) {
            addFeaturedArtists(featuredArtists, featBareArtist.group(1));
            a = featBareArtist.replaceFirst("").trim();
        }

        t = VIDEO_NOISE_TAGS.matcher(t).replaceAll("");
        a = VIDEO_NOISE_TAGS.matcher(a).replaceAll("");
        t = YEAR_TAGS.matcher(t).replaceAll("");
        a = YEAR_TAGS.matcher(a).replaceAll("");

        String searchTitle = cleanSearchText(t);
        String searchArtist = cleanSearchText(a);
        if (isGenericArtist(searchArtist) || isGenericArtist(a)) {
            searchArtist = "";
        }

        String cleanTitle = cleanDisplayText(t);
        String cleanArtist = cleanDisplayText(a);

        if (cleanTitle.isEmpty() && !cleanArtist.isEmpty()) {
            cleanTitle = cleanArtist;
            if (searchTitle.isEmpty()) {
                searchTitle = cleanSearchText(a);
            }
            searchArtist = "";
            cleanArtist = "\u041c\u0443\u0437\u044b\u043a\u0430";
        }

        String primaryArtist = extractPrimaryArtist(cleanArtist);

        return new SanitizedMetadata(
            cleanTitle,
            cleanArtist,
            searchTitle,
            searchArtist,
            primaryArtist,
            Collections.unmodifiableList(featuredArtists),
            rawTitle != null ? rawTitle : "",
            rawArtist != null ? rawArtist : "",
            isLive,
            isRemix,
            isAcoustic,
            isSpeedUp,
            isSlowed
        );
    }

    private static String normalizeDashes(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u2014', '-')
                .replace('\u2013', '-')
                .replace('\u2212', '-')
                .replace('\u2010', '-')
                .replace('\u2015', '-');
    }

    private static String cleanSearchText(String s) {
        if (s == null) {
            return "";
        }
        String res = REMASTER_PATTERN.matcher(s).replaceAll("");
        res = MODIFIER_NOISE_TAGS.matcher(res).replaceAll("");
        res = TRAILING_MODIFIER_NOISE.matcher(res).replaceFirst("");
        res = QUOTES_PATTERN.matcher(res).replaceAll("");
        return MULTI_SPACE_PATTERN.matcher(res).replaceAll(" ").trim();
    }

    private static String cleanDisplayText(String s) {
        if (s == null) {
            return "";
        }
        String res = QUOTES_PATTERN.matcher(s).replaceAll("");
        return MULTI_SPACE_PATTERN.matcher(res).replaceAll(" ").trim();
    }

    private static void addFeaturedArtists(List<String> list, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String[] parts = FEATURED_SPLIT_PATTERN.split(text);
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                list.add(trimmed);
            }
        }
    }

    public static String extractPrimaryArtist(String artist) {
        if (artist == null || artist.isBlank()) {
            return "";
        }
        String a = normalizeDashes(artist);
        String[] split = PRIMARY_ARTIST_SPLIT_PATTERN.split(a);
        if (split.length > 0 && !split[0].isBlank()) {
            return split[0].trim();
        }
        return artist.trim();
    }

    public static boolean isGenericArtist(String a) {
        if (a == null || a.isBlank()) {
            return true;
        }
        String s = a.trim().toLowerCase(Locale.ROOT);
        return s.equals("youtube") || s.equals("youtube music") || s.equals("soundcloud") ||
               s.equals("spotify") || s.equals("google chrome") || s.equals("chrome") ||
               s.equals("chrome.exe") || s.equals("msedge") || s.equals("msedge.exe") ||
               s.equals("opera") || s.equals("opera.exe") || s.equals("firefox") ||
               s.equals("firefox.exe") || s.equals("brave") || s.equals("brave.exe") ||
               s.equals("vivaldi") || s.equals("yandex") || s.equals("browser") ||
               s.equals("яндекс") || s.equals("яндекс браузер") || s.equals("яндекс.браузер") ||
               s.equals("media player") || s.equals("windows media player") || s.equals("vlc") ||
               s.equals("aimp") || s.equals("foobar2000") || s.equals("unknown") ||
               s.equals("unknown artist") || s.equals("various artists") || s.equals("\u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439 \u0438\u0441\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c") ||
               s.equals("\u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439 \u0430\u0432\u0442\u043e\u0440") || s.equals("\u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439") || s.equals("\u0430\u0432\u0442\u043e\u0440 \u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u0435\u043d") ||
               s.equals("\u043c\u043e\u044f \u043c\u0443\u0437\u044b\u043a\u0430") || s.equals("local") || s.equals("music") ||
               s.equals("7clouds") || s.equals("syrebralvibes") || s.equals("trap nation") ||
               s.equals("bass nation") || s.equals("chill nation") || s.equals("proximity") ||
               s.equals("mrsuicidesheep") || s.equals("cloudkid") || s.equals("ncs") ||
               s.equals("nocopyrightsounds") || s.equals("vevo") || s.equals("lyrics") ||
               s.equals("\u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b") || s.equals("\u0442\u0435\u043a\u0441\u0442 \u043f\u0435\u0441\u043d\u0438") || s.equals("\u0440\u0443\u0441\u0441\u043a\u0438\u0435 \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b") ||
               s.equals("\u0430\u0443\u0434\u0438\u043e") || s.equals("\u043c\u0443\u0437\u044b\u043a\u0430");
    }

    private static boolean isLikelyArtist(String candidate, String knownArtist) {
        if (candidate == null || knownArtist == null) {
            return false;
        }
        String c = candidate.toLowerCase(Locale.ROOT);
        String k = knownArtist.toLowerCase(Locale.ROOT);
        return c.equals(k) || k.contains(c) || c.contains(k);
    }

    private static boolean isLikelyInverted(String artist, String title) {
        if (artist == null || title == null) {
            return false;
        }
        String a = artist.trim();
        String t = title.trim();

        if (a.endsWith(")") || a.endsWith("]")) {
            if (!t.endsWith(")") && !t.endsWith("]")) {
                String al = a.toLowerCase(Locale.ROOT);
                if (al.contains("remix") || al.contains("version") || al.contains("live") ||
                    al.contains("mix") || al.contains("edit") || al.contains("remaster") ||
                    al.contains("acoustic") || al.contains("radio edit")) {
                    return true;
                }
            }
        }

        if (a.startsWith("\"") && a.endsWith("\"") || a.startsWith("«") && a.endsWith("»")) {
            return true;
        }

        return false;
    }

    private static ParsedPair splitArtistTitle(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();

        String[] delimiters = new String[] { " - ", " -- ", " | ", " \u002F\u002F ", " ~ ", " : " };
        for (String delim : delimiters) {
            int idx = s.indexOf(delim);
            if (idx > 0 && idx + delim.length() < s.length()) {
                String left = s.substring(0, idx).trim();
                String right = s.substring(idx + delim.length()).trim();
                if (!left.isEmpty() && !right.isEmpty()) {
                    if (right.contains(" - ")) {
                        String[] sub = right.split(" - ", 2);
                        if (sub.length == 2 && isVersionOrSubTag(sub[1])) {
                            return new ParsedPair(left, sub[0].trim() + " (" + sub[1].trim() + ")");
                        }
                    }
                    return new ParsedPair(left, right);
                }
            }
        }
        return null;
    }

    private static boolean isVersionOrSubTag(String s) {
        if (s == null) {
            return false;
        }
        String l = s.trim().toLowerCase(Locale.ROOT);
        return l.contains("live") || l.contains("remix") || l.contains("mix") ||
               l.contains("version") || l.contains("acoustic") || l.contains("edit") ||
               l.contains("remaster") || l.contains("topic") || l.contains("slowed") ||
               l.contains("reverb") || l.contains("speed") || l.contains("sped") ||
               l.contains("nightcore") || l.contains("instrumental") || l.contains("karaoke") ||
               l.contains("cover") || l.contains("ost") || l.contains("soundtrack") ||
               l.contains("bonus") || l.contains("deluxe") || l.contains("demo") ||
               l.contains("part ") || l.contains("pt.") || l.contains("vol.") ||
               l.contains("\u0447\u0430\u0441\u0442\u044c") || l.contains("intro") || l.contains("outro");
    }

    private record ParsedPair(String artist, String title) {}
}
