package com.example.yandexmusichud.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeniusLyricsProvider {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1800L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final long COOLDOWN_MS = 600000L;
    private static final Pattern LYRICS_CONTAINER_OPEN = Pattern.compile("<div\\b[^>]*\\bdata-lyrics-container\\s*=\\s*(?:[\"']?true[\"']?)[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern OLD_LYRICS_OPEN = Pattern.compile("<div\\b[^>]*\\bclass\\s*=\\s*[\"'][^\"']*\\blyrics\\b[^\"']*[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIV_TAG_TOKEN = Pattern.compile("</?div\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BR_TAG_PATTERN = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TAG_PATTERN = Pattern.compile("</?p[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("\\r?\\n");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern CONTRIBUTORS_PATTERN = Pattern.compile("^\\d+\\s+Contributors.*$", Pattern.CASE_INSENSITIVE);
    private static volatile long cooldownUntilMs = 0L;

    public static String fetchLyrics(String title, String artist, long timeoutMs) {
        if (title == null || title.isBlank()) {
            return null;
        }
        long nowMs = System.currentTimeMillis();
        if (nowMs < cooldownUntilMs) {
            return null;
        }
        long totalBudgetMs = timeoutMs > 0L ? timeoutMs : 2800L;
        long startNs = System.nanoTime();
        long perReqTimeoutMs = Math.max(800L, totalBudgetMs / 2L);
        try {
            String query = artist != null && !artist.isBlank() ? artist.trim() + " " + title.trim() : title.trim();
            String searchUrl = "https://genius.com/api/search/multi?per_page=5&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchUrl)).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36").header("Accept", "application/json").timeout(Duration.ofMillis(perReqTimeoutMs)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 403 || status == 429 || status >= 500) {
                cooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS;
                return null;
            }
            if (status != 200 || response.body() == null || response.body().isBlank()) {
                return null;
            }
            JsonElement rootEl = JsonParser.parseString(response.body());
            if (rootEl == null || !rootEl.isJsonObject()) {
                return null;
            }
            JsonObject root = rootEl.getAsJsonObject();
            if (!root.has("response") || !root.get("response").isJsonObject()) {
                return null;
            }
            JsonObject respObj = root.getAsJsonObject("response");
            if (!respObj.has("sections") || !respObj.get("sections").isJsonArray()) {
                return null;
            }
            String bestSongUrl = GeniusLyricsProvider.selectBestSongUrl(respObj.getAsJsonArray("sections"), title, artist);
            if (bestSongUrl == null) {
                return null;
            }
            long remMs = Math.max(700L, totalBudgetMs - (System.nanoTime() - startNs) / 1000000L);
            HttpRequest pageReq = HttpRequest.newBuilder().uri(URI.create(bestSongUrl)).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36").header("Accept", "text/html,application/xhtml+xml").timeout(Duration.ofMillis(remMs)).GET().build();
            HttpResponse<String> pageResp = HTTP_CLIENT.send(pageReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int pageStatus = pageResp.statusCode();
            if (pageStatus == 403 || pageStatus == 429 || pageStatus >= 500) {
                cooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS;
                return null;
            }
            if (pageStatus != 200 || pageResp.body() == null || pageResp.body().isBlank()) {
                return null;
            }
            return GeniusLyricsProvider.parseGeniusHtml(pageResp.body());
        }
        catch (Exception ignored) {
            cooldownUntilMs = System.currentTimeMillis() + 60000L;
            return null;
        }
    }

    private static String selectBestSongUrl(JsonArray sections, String title, String artist) {
        String bestSongUrl = null;
        double bestScore = -1.0;
        for (JsonElement secEl : sections) {
            if (!secEl.isJsonObject()) {
                continue;
            }
            JsonObject sec = secEl.getAsJsonObject();
            if (!sec.has("type") || !sec.get("type").isJsonPrimitive() || !"song".equalsIgnoreCase(sec.get("type").getAsString()) || !sec.has("hits") || !sec.get("hits").isJsonArray()) {
                continue;
            }
            JsonArray hits = sec.getAsJsonArray("hits");
            for (JsonElement hitEl : hits) {
                if (!hitEl.isJsonObject()) {
                    continue;
                }
                JsonObject hit = hitEl.getAsJsonObject();
                if (!hit.has("result") || !hit.get("result").isJsonObject()) {
                    continue;
                }
                JsonObject res = hit.getAsJsonObject("result");
                String resTitle = res.has("title") && res.get("title").isJsonPrimitive() ? res.get("title").getAsString() : "";
                String resArtist = "";
                if (res.has("primary_artist") && res.get("primary_artist").isJsonObject()) {
                    JsonObject pa = res.getAsJsonObject("primary_artist");
                    resArtist = pa.has("name") && pa.get("name").isJsonPrimitive() ? pa.get("name").getAsString() : "";
                }
                String url = res.has("url") && res.get("url").isJsonPrimitive() ? res.get("url").getAsString() : "";
                if (url.isBlank()) {
                    continue;
                }
                double tSim = GeniusLyricsProvider.computeStringSimilarity(title, resTitle);
                double aSim = artist != null && !artist.isBlank() ? GeniusLyricsProvider.computeStringSimilarity(artist, resArtist) : 1.0;
                if (tSim < 0.35) {
                    continue;
                }
                if (artist != null && !artist.isBlank() && aSim < 0.25 && tSim < 0.85) {
                    continue;
                }
                double score = tSim * 0.65 + aSim * 0.35;
                if (score > bestScore) {
                    bestScore = score;
                    bestSongUrl = url;
                }
            }
        }
        return bestScore >= 0.50 ? bestSongUrl : null;
    }

    private static List<String> extractBalancedDivs(String html, Pattern openPattern) {
        ArrayList<String> results = new ArrayList<String>();
        Matcher openMatcher = openPattern.matcher(html);
        int searchFrom = 0;
        while (openMatcher.find(searchFrom)) {
            int contentStart = openMatcher.end();
            Matcher tagMatcher = DIV_TAG_TOKEN.matcher(html);
            int depth = 1;
            int cursor = contentStart;
            int contentEnd = -1;
            while (tagMatcher.find(cursor)) {
                String tok = tagMatcher.group();
                if (tok.startsWith("</")) {
                    --depth;
                    if (depth == 0) {
                        contentEnd = tagMatcher.start();
                        searchFrom = tagMatcher.end();
                        break;
                    }
                } else if (!tok.endsWith("/>")) {
                    ++depth;
                }
                cursor = tagMatcher.end();
            }
            if (contentEnd >= contentStart) {
                results.add(html.substring(contentStart, contentEnd));
            } else {
                searchFrom = contentStart;
            }
        }
        return results;
    }

    public static String parseGeniusHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        List<String> rawContainers = GeniusLyricsProvider.extractBalancedDivs(html, LYRICS_CONTAINER_OPEN);
        if (rawContainers.isEmpty()) {
            rawContainers = GeniusLyricsProvider.extractBalancedDivs(html, OLD_LYRICS_OPEN);
        }
        if (rawContainers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean lastWasEmpty = true;
        for (String container : rawContainers) {
            String text = BR_TAG_PATTERN.matcher(container).replaceAll("\n");
            text = P_TAG_PATTERN.matcher(text).replaceAll("\n");
            text = HTML_TAG_PATTERN.matcher(text).replaceAll("");
            text = GeniusLyricsProvider.unescapeHtml(text);
            String[] lines = LINE_SPLIT_PATTERN.split(text);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    if (!lastWasEmpty) {
                        sb.append("\n");
                        lastWasEmpty = true;
                    }
                    continue;
                }
                if (CONTRIBUTORS_PATTERN.matcher(line).find() || line.contains("Translations") && (line.contains("English") || line.contains("Russian") || line.contains("Espa\u00f1ol")) || line.endsWith("Lyrics") && line.length() < 50 && !line.startsWith("[")) {
                    continue;
                }
                sb.append(line).append("\n");
                lastWasEmpty = false;
            }
            if (!lastWasEmpty) {
                sb.append("\n");
                lastWasEmpty = true;
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static String unescapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&amp;", "&").replace("&quot;", "\"").replace("&#x27;", "'").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&#8217;", "\u2019").replace("&#8216;", "\u2018").replace("&#8220;", "\u201c").replace("&#8221;", "\u201d").replace("&#8212;", "-").replace("&#8211;", "-");
    }

    public static double computeStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        String a = GeniusLyricsProvider.normalize(s1);
        String b = GeniusLyricsProvider.normalize(s2);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.contains(b) || b.contains(a)) {
            return (double)Math.min(a.length(), b.length()) / (double)Math.max(a.length(), b.length());
        }
        String[] wordsA = WHITESPACE_PATTERN.split(a);
        String[] wordsB = WHITESPACE_PATTERN.split(b);
        HashSet<String> setA = new HashSet<String>(Arrays.asList(wordsA));
        HashSet<String> setB = new HashSet<String>(Arrays.asList(wordsB));
        setA.removeIf(w -> w.length() < 2 && (w.isEmpty() || w.codePointAt(0) <= 0x2E80));
        setB.removeIf(w -> w.length() < 2 && (w.isEmpty() || w.codePointAt(0) <= 0x2E80));
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }
        HashSet<String> intersection = new HashSet<String>(setA);
        intersection.retainAll(setB);
        HashSet<String> union = new HashSet<String>(setA);
        union.addAll(setB);
        return (double)intersection.size() / (double)union.size();
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String noPunct = NON_ALNUM_PATTERN.matcher(s.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435')).replaceAll("");
        return WHITESPACE_PATTERN.matcher(noPunct).replaceAll(" ").trim();
    }
}
