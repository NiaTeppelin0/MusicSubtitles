package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.lyrics.LyricLine;
import com.example.yandexmusichud.lyrics.LyricsManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;

public class LiveCaptionServer {
    public static final int PORT = 38473;
    private static final int MAX_BODY_BYTES = 4 * 1024 * 1024;
    private final LyricsManager lyricsManager;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public LiveCaptionServer(LyricsManager lyricsManager) {
        this.lyricsManager = lyricsManager;
    }

    public void start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 38473), 0);
            this.executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "MusicSubtitles-CaptionServer");
                t.setDaemon(true);
                return t;
            });
            this.server.setExecutor(this.executor);
            this.server.createContext("/captions/push", new PushHandler());
            this.server.createContext("/captions/transcript", new TranscriptHandler());
            this.server.createContext("/captions/status", new StatusHandler());
            this.server.start();
            System.out.println("[MusicSubtitles] LiveCaptionServer started on http://127.0.0.1:38473");
        }
        catch (Exception e) {
            System.out.println("[MusicSubtitles] Could not start LiveCaptionServer on port 38473: " + e.getMessage());
        }
    }

    public void stop() {
        if (this.server != null) {
            try {
                this.server.stop(0);
            }
            catch (Exception exception) {

            }
            this.server = null;
        }
        if (this.executor != null) {
            try {
                this.executor.shutdownNow();
            }
            catch (Exception exception) {

            }
            this.executor = null;
        }
    }

    private static boolean isValidLocalHost(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.trim().toLowerCase(Locale.ROOT);
        return h.equals("127.0.0.1") || h.equals("127.0.0.1:38473") || h.equals("localhost") || h.equals("localhost:38473") || h.equals("[::1]") || h.equals("[::1]:38473");
    }

    private static String getAllowedYoutubeOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || origin.isBlank()) {
            return null;
        }
        String o = origin.trim().toLowerCase(Locale.ROOT);
        if (o.equals("https://www.youtube.com") || o.equals("https://music.youtube.com") || o.equals("https://m.youtube.com") || o.equals("https://youtube.com")) {
            return origin.trim();
        }
        return "";
    }

    private static boolean isAllowedOrigin(HttpExchange exchange) {
        String allowed = LiveCaptionServer.getAllowedYoutubeOrigin(exchange);
        return allowed == null || !allowed.isEmpty();
    }

    private static void sendCors(HttpExchange exchange) {
        String allowedOrigin = LiveCaptionServer.getAllowedYoutubeOrigin(exchange);
        if (allowedOrigin != null && !allowedOrigin.isEmpty()) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readLimitedBody(InputStream is, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int total = 0;
        int nRead;
        while ((nRead = is.read(chunk, 0, chunk.length)) != -1) {
            total += nRead;
            if (total > maxBytes) {
                return null;
            }
            buffer.write(chunk, 0, nRead);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
        LiveCaptionServer.sendCors(exchange);
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody();){
            os.write(bytes);
        }
    }

    private class PushHandler
    implements HttpHandler {
        private PushHandler() {
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!LiveCaptionServer.isValidLocalHost(exchange) || !LiveCaptionServer.isAllowedOrigin(exchange)) {
                LiveCaptionServer.sendResponse(exchange, 403, "{\"error\":\"Forbidden\"}");
                return;
            }
            LiveCaptionServer.sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1L);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                LiveCaptionServer.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try (InputStream is = exchange.getRequestBody();){
                String body = LiveCaptionServer.readLimitedBody(is, MAX_BODY_BYTES);
                if (body == null) {
                    LiveCaptionServer.sendResponse(exchange, 413, "{\"error\":\"Payload too large\"}");
                    return;
                }
                JsonElement parsed = JsonParser.parseString((String)body);
                if (parsed == null || !parsed.isJsonObject()) {
                    LiveCaptionServer.sendResponse(exchange, 400, "{\"error\":\"Invalid JSON\"}");
                    return;
                }
                JsonObject json = parsed.getAsJsonObject();
                String text = json.has("text") && json.get("text").isJsonPrimitive() ? json.get("text").getAsString() : "";
                long rawDuration = json.has("durationMs") && json.get("durationMs").isJsonPrimitive() && json.getAsJsonPrimitive("durationMs").isNumber() ? json.getAsJsonPrimitive("durationMs").getAsLong() : 3500L;
                long durationMs = Math.max(250L, Math.min(30000L, rawDuration));
                if (!text.isEmpty()) {
                    LiveCaptionServer.this.lyricsManager.pushLiveCaption(text, durationMs);
                    LiveCaptionServer.sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    LiveCaptionServer.sendResponse(exchange, 400, "{\"error\":\"Empty text\"}");
                }
            }
            catch (Exception e) {
                int code = e instanceof com.google.gson.JsonParseException || e instanceof NumberFormatException ? 400 : 500;
                JsonObject err = new JsonObject();
                err.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                LiveCaptionServer.sendResponse(exchange, code, err.toString());
            }
        }
    }

    private class TranscriptHandler
    implements HttpHandler {
        private TranscriptHandler() {
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!LiveCaptionServer.isValidLocalHost(exchange) || !LiveCaptionServer.isAllowedOrigin(exchange)) {
                LiveCaptionServer.sendResponse(exchange, 403, "{\"error\":\"Forbidden\"}");
                return;
            }
            LiveCaptionServer.sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1L);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                LiveCaptionServer.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try (InputStream is = exchange.getRequestBody();){
                String body = LiveCaptionServer.readLimitedBody(is, MAX_BODY_BYTES);
                if (body == null) {
                    LiveCaptionServer.sendResponse(exchange, 413, "{\"error\":\"Payload too large\"}");
                    return;
                }
                JsonElement parsed = JsonParser.parseString((String)body);
                if (parsed == null || !parsed.isJsonObject()) {
                    LiveCaptionServer.sendResponse(exchange, 400, "{\"error\":\"Invalid JSON\"}");
                    return;
                }
                JsonObject json = parsed.getAsJsonObject();
                String videoTitle = json.has("videoTitle") && json.get("videoTitle").isJsonPrimitive() ? json.get("videoTitle").getAsString() : "";
                ArrayList<LyricLine> lines = new ArrayList<LyricLine>();
                if (json.has("lines") && json.get("lines").isJsonArray()) {
                    JsonArray arr = json.getAsJsonArray("lines");
                    for (JsonElement elem : arr) {
                        if (!elem.isJsonObject()) continue;
                        JsonObject obj = elem.getAsJsonObject();
                        long startMs = obj.has("startMs") && obj.get("startMs").isJsonPrimitive() && obj.getAsJsonPrimitive("startMs").isNumber() ? Math.max(0L, obj.getAsJsonPrimitive("startMs").getAsLong()) : 0L;
                        long endMs = obj.has("endMs") && obj.get("endMs").isJsonPrimitive() && obj.getAsJsonPrimitive("endMs").isNumber() ? obj.getAsJsonPrimitive("endMs").getAsLong() : startMs + 3500L;
                        endMs = Math.max(startMs + 100L, endMs);
                        String text = obj.has("text") && obj.get("text").isJsonPrimitive() ? obj.get("text").getAsString() : "";
                        if (text.trim().isEmpty()) continue;
                        lines.add(new LyricLine(startMs, endMs, text.trim(), null));
                    }
                }
                if (!lines.isEmpty()) {
                    LiveCaptionServer.this.lyricsManager.pushTranscript(videoTitle, lines);
                    LiveCaptionServer.sendResponse(exchange, 200, "{\"success\":true,\"count\":" + lines.size() + "}");
                } else {
                    LiveCaptionServer.sendResponse(exchange, 400, "{\"error\":\"No lines provided\"}");
                }
            }
            catch (Exception e) {
                int code = e instanceof com.google.gson.JsonParseException || e instanceof NumberFormatException ? 400 : 500;
                JsonObject err = new JsonObject();
                err.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                LiveCaptionServer.sendResponse(exchange, code, err.toString());
            }
        }
    }

    private static class StatusHandler
    implements HttpHandler {
        private StatusHandler() {
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!LiveCaptionServer.isValidLocalHost(exchange) || !LiveCaptionServer.isAllowedOrigin(exchange)) {
                LiveCaptionServer.sendResponse(exchange, 403, "{\"error\":\"Forbidden\"}");
                return;
            }
            LiveCaptionServer.sendCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1L);
                return;
            }
            LiveCaptionServer.sendResponse(exchange, 200, "{\"status\":\"ok\",\"service\":\"MusicSubtitles-LiveCaptionServer\",\"port\":38473}");
        }
    }
}

