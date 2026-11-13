package com.example.yandexmusichud.donationalerts;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;

public class DonationAlertsManager {
    private static final DonationAlertsManager INSTANCE = new DonationAlertsManager();
    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Pattern EDGE_QUOTES_PATTERN = Pattern.compile("^[\"']+|[\"']+$");
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("access_token\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern JWT_PATTERN = Pattern.compile("(eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+)");
    private static final Pattern HTML_UID_PATTERN = Pattern.compile("\"user_id\"\\s*:\\s*(\\d+)|user_id\\s*=\\s*(\\d+)");
    private static final Pattern HTML_NAME_PATTERN = Pattern.compile("\"user(?:name)?\"\\s*:\\s*\"([^\"]+)\"|user_name\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern HTML_AVATAR_PATTERN = Pattern.compile("\"avatar\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DOT_SPLIT_PATTERN = Pattern.compile("\\.");

    public enum Status {
        DISCONNECTED("\u041e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e", 0x777788),
        CONNECTING("\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435...", 0xFFCC00),
        CONNECTED("\u0412 \u0441\u0435\u0442\u0438", 0x00FF88),
        ERROR("\u041e\u0448\u0438\u0431\u043a\u0430", 0xFF4455);

        public final String label;
        public final int color;

        Status(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private volatile Status status = Status.DISCONNECTED;
    private volatile String lastErrorMessage = "";
    private volatile String streamerUsername = "";
    private volatile String streamerAvatarUrl = "";
    private volatile long userId = -1L;
    private volatile boolean hasCustomAvatar = false;
    private static final class_2960 DA_AVATAR_ID = class_2960.method_60655("yandexmusichud", "dynamic/da_avatar");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DonationAlerts-Poller");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private volatile long lastSeenDonationId = -1L;
    private final Set<Long> processedDonationIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean isInitialPoll = true;

    private volatile String resolvedBearerToken = "";
    private volatile String socketConnectionToken = "";
    private volatile String lastUsedRawToken = "";
    private volatile WebSocket centrifugoSocket = null;
    private volatile boolean isSocketConnecting = false;
    private final AtomicInteger wsGeneration = new AtomicInteger(0);
    private volatile boolean closed = false;

    public static DonationAlertsManager getInstance() {
        return INSTANCE;
    }

    private DonationAlertsManager() {
        this.scheduler.scheduleWithFixedDelay(this::pollSafe, 2, 4, TimeUnit.SECONDS);
    }

    public static String sanitizeToken(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.regionMatches(true, 0, "Bearer ", 0, 7)) {
            s = s.substring(7).trim();
        }
        if (s.contains("token=")) {
            int idx = s.indexOf("token=");
            s = s.substring(idx + 6);
            int amp = s.indexOf('&');
            if (amp != -1) s = s.substring(0, amp);
            int hash = s.indexOf('#');
            if (hash != -1) s = s.substring(0, hash);
        }
        if (s.contains("donationalerts.com/")) {
            int slash = s.lastIndexOf('/');
            if (slash != -1 && slash < s.length() - 1) {
                s = s.substring(slash + 1);
            }
        }
        s = EDGE_QUOTES_PATTERN.matcher(s).replaceAll("").trim();
        return s;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    public String getStreamerUsername() {
        return this.streamerUsername != null && !this.streamerUsername.isEmpty() ? this.streamerUsername : "\u0421\u0442\u0440\u0438\u043c\u0435\u0440";
    }

    public boolean hasCustomAvatar() {
        return this.hasCustomAvatar;
    }

    public class_2960 getAvatarTextureId() {
        return this.hasCustomAvatar ? DA_AVATAR_ID : null;
    }

    public void start() {
        if (this.closed) {
            return;
        }
        if (!HudConfig.INSTANCE.daEnabled || HudConfig.INSTANCE.daToken == null || HudConfig.INSTANCE.daToken.trim().isEmpty()) {
            this.status = Status.DISCONNECTED;
            return;
        }
        this.isInitialPoll = true;
        this.status = Status.CONNECTING;
        this.lastErrorMessage = "";
        CompletableFuture.runAsync(this::pollSafe);
    }

    public void disconnect() {
        this.wsGeneration.incrementAndGet();
        this.status = Status.DISCONNECTED;
        this.isInitialPoll = true;
        this.lastSeenDonationId = -1L;
        this.processedDonationIds.clear();
        closeSocket();
    }

    public synchronized void shutdown() {
        this.closed = true;
        this.disconnect();
        this.scheduler.shutdownNow();
    }

    private void pollSafe() {
        if (this.closed || !HudConfig.INSTANCE.daEnabled) {
            this.status = Status.DISCONNECTED;
            return;
        }
        String raw = HudConfig.INSTANCE.daToken != null ? HudConfig.INSTANCE.daToken.trim() : "";
        String token = sanitizeToken(raw);
        if (token.isEmpty()) {
            this.status = Status.DISCONNECTED;
            return;
        }

        if (!token.equals(this.lastUsedRawToken)) {
            this.lastUsedRawToken = token;
            this.resolvedBearerToken = "";
            this.socketConnectionToken = "";
            this.streamerUsername = "";
            this.streamerAvatarUrl = "";
            this.userId = -1L;
            this.hasCustomAvatar = false;
            this.isInitialPoll = true;
            this.lastSeenDonationId = -1L;
            this.processedDonationIds.clear();
            this.status = Status.CONNECTING;
            this.lastErrorMessage = "";
            this.wsGeneration.incrementAndGet();
            closeSocket();
        }

        if (this.isPolling.getAndSet(true)) {
            return;
        }
        try {
            this.doPoll(token);
        } catch (Throwable t) {
            this.status = Status.ERROR;
            String msg = t.getMessage() != null ? t.getMessage() : "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f";
            if (msg.contains("timed out") || msg.contains("Timeout")) {
                msg = "\u0422\u0430\u0439\u043c\u0430\u0443\u0442 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f \u0441 DA";
            }
            this.lastErrorMessage = msg;
        } finally {
            this.isPolling.set(false);
        }
    }

    private void doPoll(String token) {
        if (this.closed || !HudConfig.INSTANCE.daEnabled) {
            return;
        }
        HttpClient client = SHARED_HTTP_CLIENT;

        String bearer = "";
        if (token.startsWith("eyJ")) {
            bearer = token;
        } else {
            if (this.resolvedBearerToken != null && !this.resolvedBearerToken.isEmpty()) {
                bearer = this.resolvedBearerToken;
            } else {
                bearer = resolveWidgetTokenViaApi(client, token);
                if (bearer == null || bearer.isEmpty()) {
                    if (this.status == Status.ERROR && !this.lastErrorMessage.isEmpty()) {
                        return;
                    }
                    bearer = crawlWidgetAndResolveToken(client, token);
                }
            }
        }

        if (bearer == null || bearer.isEmpty()) {
            return;
        }

        if (this.streamerUsername.isEmpty()) {
            fetchUserProfile(client, bearer);
        }

        fetchDonations(client, bearer, token);

        if (!this.closed && HudConfig.INSTANCE.daEnabled && this.userId > 0
                && (this.centrifugoSocket == null || this.centrifugoSocket.isInputClosed() || this.centrifugoSocket.isOutputClosed())) {
            connectWebSocket(client, bearer, this.userId);
        }
    }

    private String resolveWidgetTokenViaApi(HttpClient client, String widgetToken) {
        if (widgetToken.contains("/r/")) {
            this.status = Status.ERROR;
            this.lastErrorMessage = "\u042d\u0442\u043e \u0441\u0441\u044b\u043b\u043a\u0430 \u043d\u0430 \u0434\u043e\u043d\u0430\u0442 (\u043d\u0443\u0436\u0435\u043d \u0442\u043e\u043a\u0435\u043d)";
            return null;
        }
        try {
            String encodedToken = URLEncoder.encode(widgetToken, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/api/v1/token/widget?token=" + encodedToken))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            String body = res.body() != null ? res.body().trim() : "";

            if (!body.isEmpty() && body.startsWith("{")) {
                JsonElement parsed = JsonParser.parseString(body);
                if (parsed.isJsonObject()) {
                    JsonObject json = parsed.getAsJsonObject();
                    if (json.has("success") && json.get("success").isJsonPrimitive() && json.get("success").getAsBoolean()) {
                        if (json.has("data") && json.get("data").isJsonObject()) {
                            JsonObject dataObj = json.getAsJsonObject("data");
                            if (dataObj.has("token") && dataObj.get("token").isJsonPrimitive()) {
                                String jwt = dataObj.get("token").getAsString();
                                if (jwt != null && !jwt.isEmpty()) {
                                    this.resolvedBearerToken = jwt;
                                    this.lastErrorMessage = "";
                                    try {
                                        String[] parts = DOT_SPLIT_PATTERN.split(jwt);
                                        if (parts.length >= 2) {
                                            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                                            JsonElement pElem = JsonParser.parseString(payloadJson);
                                            if (pElem.isJsonObject()) {
                                                JsonObject payloadObj = pElem.getAsJsonObject();
                                                if (payloadObj.has("sub") && payloadObj.get("sub").isJsonPrimitive()) {
                                                    this.userId = Long.parseLong(payloadObj.get("sub").getAsString());
                                                }
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                    return jwt;
                                }
                            }
                        }
                    } else if (json.has("code") && json.get("code").isJsonPrimitive() && json.get("code").getAsInt() == 1001) {
                        this.status = Status.ERROR;
                        this.lastErrorMessage = "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0442\u043e\u043a\u0435\u043d (\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0432 DA)";
                        return null;
                    }
                }
            } else if (code == 401 || code == 422) {
                this.status = Status.ERROR;
                this.lastErrorMessage = "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0442\u043e\u043a\u0435\u043d (401)";
                return null;
            } else if (code == 504 || code == 502 || code == 503) {
                this.status = Status.ERROR;
                this.lastErrorMessage = "\u0421\u0431\u043e\u0439 \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u0432 DA (" + code + ")";
                return null;
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("timed out") || msg.contains("Timeout")) {
                this.status = Status.ERROR;
                this.lastErrorMessage = "\u0422\u0430\u0439\u043c\u0430\u0443\u0442 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f \u043a DA";
            }
        }
        return null;
    }

    private String crawlWidgetAndResolveToken(HttpClient client, String widgetToken) {
        try {
            String encodedToken = URLEncoder.encode(widgetToken, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/widget/alerts?token=" + encodedToken))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(Duration.ofSeconds(7))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            String body = res.body() != null ? res.body() : "";

            if (code == 200) {

                Matcher mToken = ACCESS_TOKEN_PATTERN.matcher(body);
                String jwt = "";
                if (mToken.find()) {
                    jwt = mToken.group(1);
                } else {
                    Matcher mJwt = JWT_PATTERN.matcher(body);
                    if (mJwt.find()) {
                        jwt = mJwt.group(1);
                    }
                }

                parseUserInfoFromHtml(client, body);

                if (!jwt.isEmpty()) {
                    this.resolvedBearerToken = jwt;
                    this.lastErrorMessage = "";
                    return jwt;
                } else {
                    this.status = Status.ERROR;
                    this.lastErrorMessage = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u0437\u0432\u043b\u0435\u0447\u044c \u0442\u043e\u043a\u0435\u043d \u0438\u0437 DA \u0432\u0438\u0434\u0436\u0435\u0442\u0430";
                    return null;
                }
            } else if (code == 401 || code == 422) {
                this.status = Status.ERROR;
                this.lastErrorMessage = "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0442\u043e\u043a\u0435\u043d (\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0432 DA)";
                return null;
            } else if (code == 504 || code == 502 || code == 503) {
                this.status = Status.ERROR;
                this.lastErrorMessage = "\u0421\u0431\u043e\u0439 \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u0432 DA (" + code + ")";
                return null;
            } else {
                this.status = Status.ERROR;
                this.lastErrorMessage = "HTTP " + code + " \u043e\u0442 DA";
                return null;
            }
        } catch (Exception e) {
            this.status = Status.ERROR;
            String msg = e.getMessage() != null ? e.getMessage() : "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u0435\u0442\u0438";
            if (msg.contains("timed out") || msg.contains("Timeout")) {
                msg = "\u0422\u0430\u0439\u043c\u0430\u0443\u0442 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f \u043a DA";
            }
            this.lastErrorMessage = msg;
            return null;
        }
    }

    private void parseUserInfoFromHtml(HttpClient client, String html) {
        try {

            Matcher mUid = HTML_UID_PATTERN.matcher(html);
            if (mUid.find()) {
                String uidStr = mUid.group(1) != null ? mUid.group(1) : mUid.group(2);
                if (uidStr != null) {
                    try {
                        this.userId = Long.parseLong(uidStr);
                    } catch (Exception ignored) {}
                }
            }

            Matcher mName = HTML_NAME_PATTERN.matcher(html);
            if (mName.find()) {
                String name = mName.group(1) != null ? mName.group(1) : mName.group(2);
                if (name != null && !name.trim().isEmpty() && this.streamerUsername.isEmpty()) {
                    this.streamerUsername = name.trim();
                }
            }

            Matcher mAv = HTML_AVATAR_PATTERN.matcher(html);
            if (mAv.find()) {
                String av = mAv.group(1).replace("\\/", "/");
                if (av != null && !av.equals(this.streamerAvatarUrl)) {
                    this.streamerAvatarUrl = av;
                    loadAvatar(client, av);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void fetchDonations(HttpClient client, String bearer, String rawToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/api/v1/alerts/donations"))
                    .header("Authorization", "Bearer " + bearer)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            if (code == 200) {
                this.status = Status.CONNECTED;
                this.lastErrorMessage = "";

                JsonElement parsed = JsonParser.parseString(res.body());
                if (parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    if (root.has("data") && root.get("data").isJsonArray()) {
                        JsonArray arr = root.getAsJsonArray("data");
                        handleDonationList(arr);
                    }
                }
            } else if (code == 401) {
                String body = res.body() != null ? res.body() : "";
                if (body.contains("scope") || !rawToken.startsWith("eyJ")) {
                    if (this.centrifugoSocket != null || this.isSocketConnecting) {
                        this.status = Status.CONNECTED;
                        this.lastErrorMessage = "";
                    } else {
                        this.status = Status.CONNECTING;
                    }
                } else {
                    if (!rawToken.startsWith("eyJ")) {
                        this.resolvedBearerToken = "";
                    }
                    this.status = Status.ERROR;
                    this.lastErrorMessage = "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u0442\u043e\u043a\u0435\u043d (401)";
                }
            } else if (code == 504 || code == 502 || code == 503) {
                this.status = Status.ERROR;
                this.lastErrorMessage = "\u0421\u0431\u043e\u0439 \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u0432 DA (" + code + ")";
            } else {
                this.status = Status.ERROR;
                this.lastErrorMessage = "HTTP " + code;
            }
        } catch (Exception e) {
            this.status = Status.ERROR;
            String msg = e.getMessage() != null ? e.getMessage() : "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u0435\u0442\u0438";
            if (msg.contains("timed out") || msg.contains("Timeout")) {
                msg = "\u0422\u0430\u0439\u043c\u0430\u0443\u0442 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f \u043a DA";
            }
            this.lastErrorMessage = msg;
        }
    }

    private void fetchUserProfile(HttpClient client, String bearer) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/api/v1/user/widget"))
                    .header("Authorization", "Bearer " + bearer)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                req = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.donationalerts.com/api/v1/user/oauth"))
                        .header("Authorization", "Bearer " + bearer)
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                res = client.send(req, HttpResponse.BodyHandlers.ofString());
            }
            if (res.statusCode() == 200) {
                JsonElement parsed = JsonParser.parseString(res.body());
                if (parsed.isJsonObject()) {
                    JsonObject obj = parsed.getAsJsonObject();
                    if (obj.has("data") && obj.get("data").isJsonObject()) {
                        JsonObject d = obj.getAsJsonObject("data");
                        if (d.has("id") && d.get("id").isJsonPrimitive()) {
                            this.userId = d.get("id").getAsLong();
                        }
                        if (d.has("name") && d.get("name").isJsonPrimitive()) {
                            this.streamerUsername = d.get("name").getAsString();
                        }
                        if (d.has("socket_connection_token") && d.get("socket_connection_token").isJsonPrimitive()) {
                            this.socketConnectionToken = d.get("socket_connection_token").getAsString();
                        }
                        if (d.has("avatar") && d.get("avatar").isJsonPrimitive()) {
                            String avatar = d.get("avatar").getAsString();
                            if (avatar != null && !avatar.equals(this.streamerAvatarUrl)) {
                                this.streamerAvatarUrl = avatar;
                                loadAvatar(client, avatar);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private synchronized void connectWebSocket(HttpClient client, String bearer, long uid) {
        if (this.closed || !HudConfig.INSTANCE.daEnabled || this.isSocketConnecting) return;
        this.isSocketConnecting = true;
        final int gen = this.wsGeneration.get();
        try {
            client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .buildAsync(URI.create("wss://centrifugo.donationalerts.com/connection/websocket"), new WebSocket.Listener() {
                        private final StringBuilder buffer = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            if (closed || gen != wsGeneration.get() || !HudConfig.INSTANCE.daEnabled) {
                                isSocketConnecting = false;
                                try {
                                    webSocket.abort();
                                } catch (Throwable ignored) {}
                                return;
                            }
                            DonationAlertsManager.this.centrifugoSocket = webSocket;
                            DonationAlertsManager.this.isSocketConnecting = false;
                            DonationAlertsManager.this.status = Status.CONNECTED;
                            DonationAlertsManager.this.lastErrorMessage = "";
                            String tokenToSend = (DonationAlertsManager.this.socketConnectionToken != null && !DonationAlertsManager.this.socketConnectionToken.isEmpty())
                                    ? DonationAlertsManager.this.socketConnectionToken
                                    : bearer;
                            JsonObject params = new JsonObject();
                            params.addProperty("token", tokenToSend);
                            JsonObject authObj = new JsonObject();
                            authObj.add("params", params);
                            authObj.addProperty("id", 1);
                            webSocket.sendText(authObj.toString(), true);
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            if (closed || gen != wsGeneration.get()) {
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }
                            buffer.append(data);
                            if (last) {
                                String text = buffer.toString();
                                buffer.setLength(0);
                                handleCentrifugoMessage(client, webSocket, text, uid, bearer);
                            }
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            if (gen == wsGeneration.get()) {
                                DonationAlertsManager.this.isSocketConnecting = false;
                                if (DonationAlertsManager.this.centrifugoSocket == webSocket) {
                                    DonationAlertsManager.this.centrifugoSocket = null;
                                }
                            }
                            WebSocket.Listener.super.onError(webSocket, error);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            if (gen == wsGeneration.get()) {
                                DonationAlertsManager.this.isSocketConnecting = false;
                                if (DonationAlertsManager.this.centrifugoSocket == webSocket) {
                                    DonationAlertsManager.this.centrifugoSocket = null;
                                }
                            }
                            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                        }
                    }).whenComplete((ws, err) -> {
                        if (err != null) {
                            if (gen == wsGeneration.get()) {
                                this.isSocketConnecting = false;
                            }
                        } else if (ws != null && (this.closed || gen != this.wsGeneration.get() || !HudConfig.INSTANCE.daEnabled)) {
                            this.isSocketConnecting = false;
                            try {
                                ws.abort();
                            } catch (Throwable ignored) {}
                        }
                    });
        } catch (Throwable t) {
            this.isSocketConnecting = false;
        }
    }

    private void subscribeCentrifugoChannel(HttpClient client, WebSocket ws, String bearer, long uid, String clientId) {
        CompletableFuture.runAsync(() -> {
            try {
                if (this.closed || !HudConfig.INSTANCE.daEnabled) {
                    return;
                }
                JsonArray channelsArr = new JsonArray();
                channelsArr.add("$alerts:donation_" + uid);
                JsonObject subObj = new JsonObject();
                subObj.add("channels", channelsArr);
                subObj.addProperty("client", clientId);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.donationalerts.com/api/v1/centrifuge/subscribe"))
                        .header("Authorization", "Bearer " + bearer)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(Duration.ofSeconds(6))
                        .POST(HttpRequest.BodyPublishers.ofString(subObj.toString()))
                        .build();

                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    JsonElement parsed = JsonParser.parseString(res.body());
                    if (parsed.isJsonObject()) {
                        JsonObject json = parsed.getAsJsonObject();
                        if (json.has("channels") && json.get("channels").isJsonArray()) {
                            JsonArray arr = json.getAsJsonArray("channels");
                            if (!arr.isEmpty() && arr.get(0).isJsonObject()) {
                                JsonObject ch = arr.get(0).getAsJsonObject();
                                if (ch.has("token") && ch.get("token").isJsonPrimitive()) {
                                    String channelToken = ch.get("token").getAsString();
                                    JsonObject wsParams = new JsonObject();
                                    wsParams.addProperty("channel", "$alerts:donation_" + uid);
                                    wsParams.addProperty("token", channelToken);
                                    JsonObject wsSubObj = new JsonObject();
                                    wsSubObj.add("params", wsParams);
                                    wsSubObj.addProperty("method", 1);
                                    wsSubObj.addProperty("id", 2);
                                    ws.sendText(wsSubObj.toString(), true);
                                    DonationAlertsManager.this.status = Status.CONNECTED;
                                    DonationAlertsManager.this.lastErrorMessage = "";
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        });
    }

    private void handleCentrifugoMessage(HttpClient client, WebSocket ws, String text, long uid, String bearer) {
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject json = parsed.getAsJsonObject();
            if (json.has("id") && json.get("id").isJsonPrimitive() && json.get("id").getAsInt() == 1) {
                if (json.has("result") && json.get("result").isJsonObject()) {
                    JsonObject resObj = json.getAsJsonObject("result");
                    if (resObj.has("client") && resObj.get("client").isJsonPrimitive()) {
                        String clientId = resObj.get("client").getAsString();
                        subscribeCentrifugoChannel(client, ws, bearer, uid, clientId);
                    }
                }
            }
            if (json.has("result") && json.get("result").isJsonObject()) {
                JsonObject res = json.getAsJsonObject("result");
                if (res.has("data") && res.get("data").isJsonObject()) {
                    JsonObject d = res.getAsJsonObject("data");
                    if (d.has("data") && d.get("data").isJsonObject()) {
                        JsonObject don = d.getAsJsonObject("data");
                        handleSingleDonationObject(don);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void handleSingleDonationObject(JsonObject obj) {
        long id = obj.has("id") && obj.get("id").isJsonPrimitive() ? obj.get("id").getAsLong() : -1L;
        if (id <= 0 || this.processedDonationIds.contains(id)) {
            return;
        }
        if (this.processedDonationIds.size() > 1000) {
            long minKeep = this.lastSeenDonationId - 500L;
            this.processedDonationIds.removeIf(oldId -> oldId < minKeep);
            if (this.processedDonationIds.size() > 1000) {
                this.processedDonationIds.clear();
            }
        }
        this.processedDonationIds.add(id);
        if (id > this.lastSeenDonationId) {
            this.lastSeenDonationId = id;
        }
        String donorName = obj.has("username") && obj.get("username").isJsonPrimitive()
                ? obj.get("username").getAsString()
                : (obj.has("name") && obj.get("name").isJsonPrimitive() ? obj.get("name").getAsString() : "\u0410\u043d\u043e\u043d\u0438\u043c");
        double amount = obj.has("amount") && obj.get("amount").isJsonPrimitive() ? obj.get("amount").getAsDouble() : 0.0;
        String currency = obj.has("currency") && obj.get("currency").isJsonPrimitive() ? obj.get("currency").getAsString() : "RUB";
        String message = obj.has("message") && obj.get("message").isJsonPrimitive() ? obj.get("message").getAsString() : "";

        if (amount >= (double)HudConfig.INSTANCE.daMinAmountAlert) {
            triggerDonationAlert(new DonationAlertEvent(id, donorName, amount, currency, message));
        }
    }

    private void closeSocket() {
        WebSocket oldSocket = this.centrifugoSocket;
        this.centrifugoSocket = null;
        this.isSocketConnecting = false;
        if (oldSocket != null) {
            try {
                oldSocket.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect");
            } catch (Throwable ignored) {
                try {
                    oldSocket.abort();
                } catch (Throwable ignored2) {}
            }
        }
    }

    private void handleDonationList(JsonArray arr) {
        if (arr.isEmpty()) {
            this.isInitialPoll = false;
            return;
        }

        if (this.isInitialPoll) {
            for (JsonElement elem : arr) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                long id = obj.has("id") && obj.get("id").isJsonPrimitive() ? obj.get("id").getAsLong() : -1L;
                if (id > 0) {
                    this.processedDonationIds.add(id);
                    if (id > this.lastSeenDonationId) {
                        this.lastSeenDonationId = id;
                    }
                }
            }
            this.isInitialPoll = false;
            return;
        }

        for (JsonElement elem : arr) {
            if (!elem.isJsonObject()) continue;
            JsonObject obj = elem.getAsJsonObject();
            handleSingleDonationObject(obj);
        }
    }

    private void loadAvatar(HttpClient client, String avatarUrl) {
        CompletableFuture.runAsync(() -> {
            class_1011 image = null;
            boolean handedOff = false;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(avatarUrl))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();
                HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200) {
                    byte[] bytes = res.body();
                    image = class_1011.method_4309(new ByteArrayInputStream(bytes));
                    class_310 mc = class_310.method_1551();
                    if (image != null && mc != null && !this.closed) {
                        int r = Math.min(image.method_4307(), image.method_4323()) / 2;
                        if (r > 0) {
                            CoverTextureManager.roundImageCorners(image, r);
                        }
                        final class_1011 finalImage = image;
                        handedOff = true;
                        mc.execute(() -> {
                            boolean registered = false;
                            try {
                                if (this.hasCustomAvatar) {
                                    mc.method_1531().method_4615(DA_AVATAR_ID);
                                }
                                class_1043 texture = new class_1043(() -> "da_avatar", finalImage);
                                mc.method_1531().method_4616(DA_AVATAR_ID, texture);
                                this.hasCustomAvatar = true;
                                registered = true;
                            } catch (Exception ignored) {
                            } finally {
                                if (!registered) {
                                    try {
                                        finalImage.close();
                                    } catch (Exception ignored2) {}
                                }
                            }
                        });
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (!handedOff && image != null) {
                    try {
                        image.close();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    public void triggerDonationAlert(DonationAlertEvent event) {
        triggerDonationAlert(event, false);
    }

    public void triggerDonationAlert(DonationAlertEvent event, boolean force) {
        if (event == null) return;
        if (!force && HudConfig.INSTANCE.daMinAmountAlert > 0 && event.getAmount() < (double)HudConfig.INSTANCE.daMinAmountAlert) {
            return;
        }
        DonationAlertQueue.INSTANCE.enqueue(event);
        if (HudConfig.INSTANCE.daSoundEnabled) {
            playAlertSound();
        }
    }

    public void sendTestDonation(String donorName, double amount, String currency, String message) {
        long testId = System.currentTimeMillis();
        triggerDonationAlert(new DonationAlertEvent(testId, donorName, amount, currency, message), true);
    }

    private static void playAlertSound() {
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null) {
                mc.execute(() -> {
                    try {
                        if (mc.method_1483() != null) {
                            class_3414 sound = (class_3414)class_3417.field_14793.comp_349();
                            mc.method_1483().method_4873((class_1113)class_1109.method_4757(sound, 1.25f, 0.9f));
                        }
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {}
    }
}

