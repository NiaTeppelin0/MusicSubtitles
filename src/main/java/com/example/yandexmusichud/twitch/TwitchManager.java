package com.example.yandexmusichud.twitch;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3417;

public class TwitchManager {
    private static final TwitchManager INSTANCE = new TwitchManager();
    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(7))
            .build();
    private static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("\\r?\\n");
    private static final Pattern TAG_SPLIT_PATTERN = Pattern.compile(";");

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
    private volatile long lastPingTime = 0L;
    private volatile int pingLatencyMs = 0;
    private volatile WebSocket webSocket = null;
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicInteger connectGeneration = new AtomicInteger(0);
    private volatile String currentJoinedChannel = "";

    private static final class_2960 TWITCH_AVATAR_ID = class_2960.method_60655("yandexmusichud", "dynamic/twitch_avatar");
    private volatile boolean isLive = false;
    private volatile int viewersCount = 0;
    private volatile String streamTitle = "";
    private volatile String channelDisplayName = "";
    private volatile boolean hasCustomAvatar = false;
    private volatile String lastAvatarUrl = "";
    private final AtomicBoolean isCheckingLive = new AtomicBoolean(false);
    private long lastStreamCommandTime = 0L;
    private volatile boolean pendingAutoCommand = false;

    public static TwitchManager getInstance() {
        return INSTANCE;
    }

    private TwitchManager() {}

    public boolean isLive() {
        return this.isLive;
    }

    public int getViewersCount() {
        return this.viewersCount;
    }

    public String getStreamTitle() {
        return this.streamTitle;
    }

    public String getChannelDisplayName() {
        return this.channelDisplayName != null && !this.channelDisplayName.isEmpty()
                ? this.channelDisplayName : this.currentJoinedChannel;
    }

    public boolean hasCustomAvatar() {
        return this.hasCustomAvatar;
    }

    public class_2960 getAvatarTextureId() {
        return this.hasCustomAvatar ? TWITCH_AVATAR_ID : null;
    }

    public void checkLiveStatusAsync() {
        String rawChannel = HudConfig.INSTANCE.twitchChannel;
        if (rawChannel == null || rawChannel.trim().isEmpty()) {
            this.isLive = false;
            this.viewersCount = 0;
            this.streamTitle = "";
            return;
        }
        String cleanChannel = cleanChannelName(rawChannel);
        checkLiveStatus(cleanChannel);
    }

    public void checkLiveStatus(String channel) {
        if (channel == null || channel.isEmpty()) {
            this.isLive = false;
            this.viewersCount = 0;
            this.streamTitle = "";
            return;
        }
        if (this.isCheckingLive.getAndSet(true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject vars = new JsonObject();
                vars.addProperty("login", channel);
                JsonObject payload = new JsonObject();
                payload.addProperty("query", "query($login: String!) { user(login: $login) { id displayName profileImageURL(width: 70) stream { id type viewersCount title } } }");
                payload.add("variables", vars);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://gql.twitch.tv/gql"))
                        .header("Client-ID", "kimne78kx3ncx6brgo4mv6wki5h1ko")
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(6))
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                long reqStart = System.currentTimeMillis();
                HttpResponse<String> response = SHARED_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                this.pingLatencyMs = Math.max(1, (int)(System.currentTimeMillis() - reqStart));
                if (response.statusCode() == 200) {
                    JsonElement parsed = JsonParser.parseString(response.body());
                    if (parsed.isJsonObject()) {
                        JsonObject root = parsed.getAsJsonObject();
                        if (root.has("data") && root.get("data").isJsonObject()) {
                            JsonObject data = root.getAsJsonObject("data");
                            if (data.has("user") && data.get("user").isJsonObject()) {
                                JsonObject user = data.getAsJsonObject("user");
                                if (user.has("displayName") && user.get("displayName").isJsonPrimitive()) {
                                    this.channelDisplayName = user.get("displayName").getAsString();
                                }
                                String avatarUrl = user.has("profileImageURL") && user.get("profileImageURL").isJsonPrimitive()
                                        ? user.get("profileImageURL").getAsString() : "";
                                if (!avatarUrl.isEmpty() && !avatarUrl.equals(this.lastAvatarUrl)) {
                                    this.lastAvatarUrl = avatarUrl;
                                    loadAvatarTexture(avatarUrl);
                                }
                                boolean wasLive = this.isLive;
                                boolean nowLive = false;
                                if (user.has("stream") && user.get("stream").isJsonObject()) {
                                    JsonObject stream = user.getAsJsonObject("stream");
                                    String type = stream.has("type") && stream.get("type").isJsonPrimitive() ? stream.get("type").getAsString() : "";
                                    nowLive = "live".equalsIgnoreCase(type);
                                    this.viewersCount = stream.has("viewersCount") && stream.get("viewersCount").isJsonPrimitive() ? stream.get("viewersCount").getAsInt() : 0;
                                    this.streamTitle = stream.has("title") && stream.get("title").isJsonPrimitive() ? stream.get("title").getAsString() : "";
                                } else {
                                    this.viewersCount = 0;
                                    this.streamTitle = "";
                                }
                                this.isLive = nowLive;
                                if (!wasLive && nowLive) {
                                    onStreamStarted();
                                }
                            } else {
                                this.isLive = false;
                                this.viewersCount = 0;
                                this.streamTitle = "";
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                this.isCheckingLive.set(false);
            }
        });
    }

    private void loadAvatarTexture(String url) {
        CompletableFuture.runAsync(() -> {
            class_1011 image = null;
            boolean handedOff = false;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();
                HttpResponse<byte[]> res = SHARED_HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200) {
                    byte[] bytes = res.body();
                    image = class_1011.method_4309(new ByteArrayInputStream(bytes));
                    class_310 mc = class_310.method_1551();
                    if (image != null && mc != null) {
                        int r = Math.min(image.method_4307(), image.method_4323()) / 2;
                        CoverTextureManager.roundImageCorners(image, r);
                        final class_1011 finalImage = image;
                        handedOff = true;
                        mc.execute(() -> {
                            boolean registered = false;
                            try {
                                if (this.hasCustomAvatar) {
                                    mc.method_1531().method_4615(TWITCH_AVATAR_ID);
                                }
                                class_1043 texture = new class_1043(() -> "twitch_avatar", finalImage);
                                mc.method_1531().method_4616(TWITCH_AVATAR_ID, texture);
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

    public synchronized void start() {
        this.manualDisconnect = false;
        if (!HudConfig.INSTANCE.twitchEnabled) {
            this.status = Status.DISCONNECTED;
            this.isLive = false;
            return;
        }
        String rawChannel = HudConfig.INSTANCE.twitchChannel;
        if (rawChannel == null || rawChannel.trim().isEmpty()) {
            this.status = Status.DISCONNECTED;
            this.isLive = false;
            this.lastErrorMessage = "\u041a\u0430\u043d\u0430\u043b \u043d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d";
            return;
        }
        String cleanChannel = cleanChannelName(rawChannel);
        if (this.status == Status.CONNECTED && cleanChannel.equalsIgnoreCase(this.currentJoinedChannel)) {
            this.checkLiveStatus(cleanChannel);
            return;
        }
        this.disconnectInternal();
        this.connect(cleanChannel);
        this.checkLiveStatus(cleanChannel);
    }

    private volatile boolean manualDisconnect = false;
    private volatile long lastReconnectAttemptTime = 0L;

    public synchronized void disconnect() {
        this.manualDisconnect = true;
        this.disconnectInternal();
    }

    public synchronized void shutdown() {
        this.disconnect();
    }

    private synchronized void disconnectInternal() {
        this.connectGeneration.incrementAndGet();
        WebSocket oldSocket = this.webSocket;
        this.webSocket = null;
        if (oldSocket != null) {
            try {
                oldSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Goodbye");
            } catch (Exception ignored) {
                try {
                    oldSocket.abort();
                } catch (Exception ignored2) {}
            }
        }
        this.isConnecting.set(false);
        this.status = Status.DISCONNECTED;
        this.currentJoinedChannel = "";
        this.isLive = false;
        this.viewersCount = 0;
        this.streamTitle = "";
    }

    private void connect(String channel) {
        if (this.manualDisconnect || !HudConfig.INSTANCE.twitchEnabled) {
            return;
        }
        if (this.isConnecting.getAndSet(true)) return;
        final int gen = this.connectGeneration.incrementAndGet();
        this.status = Status.CONNECTING;
        this.lastErrorMessage = "";

        CompletableFuture.runAsync(() -> {
            try {
                if (gen != this.connectGeneration.get() || this.manualDisconnect || !HudConfig.INSTANCE.twitchEnabled) {
                    this.isConnecting.set(false);
                    return;
                }

                WebSocket.Listener listener = new WebSocket.Listener() {
                    private final StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket ws) {
                        if (gen != connectGeneration.get() || manualDisconnect || !HudConfig.INSTANCE.twitchEnabled) {
                            isConnecting.set(false);
                            try {
                                ws.abort();
                            } catch (Exception ignored) {}
                            return;
                        }
                        webSocket = ws;
                        status = Status.CONNECTED;
                        currentJoinedChannel = channel;
                        isConnecting.set(false);

                        ws.sendText("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership\r\n", true);
                        ws.sendText("PASS oauth:justinfan" + (10000 + (int)(Math.random() * 89999)) + "\r\n", true);
                        ws.sendText("NICK justinfan" + (10000 + (int)(Math.random() * 89999)) + "\r\n", true);
                        ws.sendText("JOIN #" + channel.toLowerCase(Locale.ROOT) + "\r\n", true);

                        checkLiveStatus(channel);

                        lastPingTime = System.currentTimeMillis();
                        WebSocket.Listener.super.onOpen(ws);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        if (gen != connectGeneration.get()) {
                            return WebSocket.Listener.super.onText(ws, data, last);
                        }
                        this.buffer.append(data);
                        if (last) {
                            String message = this.buffer.toString();
                            this.buffer.setLength(0);
                            handleIrcMessage(ws, message);
                        }
                        return WebSocket.Listener.super.onText(ws, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                        if (gen == connectGeneration.get()) {
                            if (webSocket == ws) {
                                webSocket = null;
                            }
                            status = Status.DISCONNECTED;
                            isConnecting.set(false);
                            lastReconnectAttemptTime = System.currentTimeMillis();
                        }
                        return WebSocket.Listener.super.onClose(ws, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        if (gen == connectGeneration.get()) {
                            if (webSocket == ws) {
                                webSocket = null;
                            }
                            status = Status.ERROR;
                            lastErrorMessage = error.getMessage() != null ? error.getMessage() : "Connection failed";
                            isConnecting.set(false);
                            lastReconnectAttemptTime = System.currentTimeMillis();
                        }
                    }
                };

                SHARED_HTTP_CLIENT.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .buildAsync(URI.create("wss://irc-ws.chat.twitch.tv:443"), listener)
                        .thenAccept(ws -> {
                            if (gen != this.connectGeneration.get() || this.manualDisconnect || !HudConfig.INSTANCE.twitchEnabled) {
                                try {
                                    ws.abort();
                                } catch (Exception ignored) {}
                            } else {
                                this.webSocket = ws;
                            }
                        }).exceptionally(ex -> {
                            if (gen == this.connectGeneration.get()) {
                                this.status = Status.ERROR;
                                this.lastErrorMessage = ex.getMessage();
                                this.isConnecting.set(false);
                                this.lastReconnectAttemptTime = System.currentTimeMillis();
                            }
                            return null;
                        });
            } catch (Exception e) {
                if (gen == this.connectGeneration.get()) {
                    this.status = Status.ERROR;
                    this.lastErrorMessage = e.getMessage();
                    this.isConnecting.set(false);
                    this.lastReconnectAttemptTime = System.currentTimeMillis();
                }
            }
        });
    }

    private void handleIrcMessage(WebSocket ws, String raw) {
        String[] lines = LINE_SPLIT_PATTERN.split(raw);
        for (String line : lines) {
            if (line.isEmpty()) continue;

            if (line.startsWith("PING")) {
                long sendStart = System.nanoTime();
                ws.sendText("PONG " + line.substring(4) + "\r\n", true).thenRun(() -> {
                    int rtt = Math.max(1, (int)((System.nanoTime() - sendStart) / 1000000L));
                    if (this.pingLatencyMs <= 0) {
                        this.pingLatencyMs = rtt;
                    }
                });
                this.lastPingTime = System.currentTimeMillis();
                continue;
            }

            try {
                parseIrcLine(line);
            } catch (Exception ignored) {}
        }
    }

    private void parseIrcLine(String line) {
        Map<String, String> tags = new HashMap<>();
        String rest = line;

        if (line.startsWith("@")) {
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx > 0) {
                String tagPart = line.substring(1, spaceIdx);
                rest = line.substring(spaceIdx + 1);

                String[] tagPairs = TAG_SPLIT_PATTERN.split(tagPart);
                for (String pair : tagPairs) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) {
                        String key = pair.substring(0, eq);
                        String val = eq + 1 < pair.length() ? pair.substring(eq + 1) : "";
                        tags.put(key, val);
                    }
                }
            }
        }

        if (rest.contains(" USERNOTICE ")) {
            String msgId = tags.getOrDefault("msg-id", "");
            String displayName = tags.getOrDefault("display-name", tags.getOrDefault("login", "Viewer"));
            String systemMsg = tags.getOrDefault("system-msg", "");
            String userMsg = "";
            int lastColon = rest.indexOf(" :");
            if (lastColon >= 0) {
                userMsg = rest.substring(lastColon + 2);
            }

            if ("sub".equalsIgnoreCase(msgId) && HudConfig.INSTANCE.twitchShowSubs) {
                String subPlan = tags.getOrDefault("msg-param-sub-plan", "1000");
                String tier = parseTier(subPlan);
                fireAlert(new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.SUB)
                        .displayName(displayName)
                        .subTier(tier)
                        .message(userMsg)
                        .build());
            } else if ("resub".equalsIgnoreCase(msgId) && HudConfig.INSTANCE.twitchShowSubs) {
                int months = 1;
                try {
                    months = Integer.parseInt(tags.getOrDefault("msg-param-cumulative-months", "1"));
                } catch (Exception ignored) {}
                String tier = parseTier(tags.getOrDefault("msg-param-sub-plan", "1000"));
                fireAlert(new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.RESUB)
                        .displayName(displayName)
                        .subTier(tier)
                        .months(months)
                        .message(userMsg)
                        .build());
            } else if ("subgift".equalsIgnoreCase(msgId) && HudConfig.INSTANCE.twitchShowSubs) {
                String recipient = tags.getOrDefault("msg-param-recipient-display-name", "Чат");
                fireAlert(new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.SUB_GIFT)
                        .displayName(displayName)
                        .recipient(recipient)
                        .build());
            }
        }

        else if (rest.contains(" PRIVMSG ")) {
            String displayName = tags.getOrDefault("display-name", "Viewer");
            String customRewardId = tags.get("custom-reward-id");
            String bitsStr = tags.get("bits");

            int lastColon = rest.indexOf(" :");
            String userMsg = lastColon >= 0 ? rest.substring(lastColon + 2) : "";

            if (customRewardId != null && !customRewardId.isEmpty() && HudConfig.INSTANCE.twitchShowRewards) {
                fireAlert(new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.REWARD)
                        .displayName(displayName)
                        .rewardTitle(userMsg.isEmpty() ? "\u0411\u0430\u043b\u043b\u044b \u043a\u0430\u043d\u0430\u043b\u0430" : userMsg)
                        .build());
            } else if (bitsStr != null && !bitsStr.isEmpty() && HudConfig.INSTANCE.twitchShowBits) {
                int bits = 100;
                try {
                    bits = Integer.parseInt(bitsStr);
                } catch (Exception ignored) {}
                fireAlert(new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.BITS)
                        .displayName(displayName)
                        .bitsCount(bits)
                        .message(userMsg)
                        .build());
            }
        }
    }

    private void fireAlert(TwitchAlertEvent event) {
        TwitchAlertQueue.INSTANCE.enqueue(event);
        if (HudConfig.INSTANCE.twitchPlaySound) {
            playAlertSound();
        }
    }

    private void playAlertSound() {
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.field_1724 != null) {
                mc.execute(() -> {
                    if (mc.field_1724 != null) {
                        mc.field_1724.method_5783(class_3417.field_14709, 0.7f, 1.3f);
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    public void triggerTestAlert(TwitchAlertEvent.Type type) {
        TwitchAlertEvent event = switch (type) {
            case SUB -> new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.SUB)
                    .displayName("NiaFan_2026")
                    .subTier("Tier 3")
                    .message("\u0422\u043e\u043f\u043e\u0432\u044b\u0439 \u043c\u043e\u0434! \u0421\u043f\u0430\u0441\u0438\u0431\u043e \u0437\u0430 \u0441\u0442\u0440\u0438\u043c \u2764")
                    .build();
            case RESUB -> new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.RESUB)
                    .displayName("FoxLover")
                    .subTier("Tier 1")
                    .months(12)
                    .message("\u0423\u0436\u0435 \u0446\u0435\u043b\u044b\u0439 \u0433\u043e\u0434 \u0441 \u0432\u0430\u043c\u0438! \u2728")
                    .build();
            case REWARD -> new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.REWARD)
                    .displayName("MusicMaster")
                    .rewardTitle("\u0417\u0430\u043a\u0430\u0437 \u0442\u0440\u0435\u043a\u0430: Synthwave Odyssey")
                    .message("\u0412\u043a\u043b\u044e\u0447\u0438 \u043f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430!")
                    .build();
            case SUB_GIFT -> new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.SUB_GIFT)
                    .displayName("GenerousViewer")
                    .recipient("MinecraftCrafter")
                    .build();
            case BITS -> new TwitchAlertEvent.Builder(TwitchAlertEvent.Type.BITS)
                    .displayName("DiamondSupporter")
                    .bitsCount(500)
                    .message("\u041d\u0430 \u0440\u0430\u0437\u0432\u0438\u0442\u0438\u0435 \u043c\u043e\u0434\u0430! \u2726")
                    .build();
        };
        fireAlert(event);
    }

    private static String parseTier(String plan) {
        if ("Prime".equalsIgnoreCase(plan)) return "Prime";
        if ("2000".equals(plan)) return "Tier 2";
        if ("3000".equals(plan)) return "Tier 3";
        return "Tier 1";
    }

    public static String cleanChannelName(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("https://")) s = s.substring(8);
        if (s.startsWith("http://")) s = s.substring(7);
        if (s.startsWith("www.")) s = s.substring(4);
        if (s.startsWith("twitch.tv/")) s = s.substring(10);
        if (s.startsWith("@") || s.startsWith("#")) s = s.substring(1);
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash);
        return s;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getCurrentJoinedChannel() {
        return this.currentJoinedChannel;
    }

    public int getPingLatencyMs() {
        return this.pingLatencyMs;
    }

    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    private void onStreamStarted() {
        long now = System.currentTimeMillis();

        if (now - this.lastStreamCommandTime < 180000L) {
            return;
        }

        if (!HudConfig.INSTANCE.twitchAutoCommandEnabled) {
            return;
        }
        String cmd = HudConfig.INSTANCE.twitchAutoCommand;
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null || mc.field_1724.field_3944 == null) {

            this.pendingAutoCommand = true;
            return;
        }

        this.lastStreamCommandTime = now;
        executeStreamCommand(cmd.trim(), false);
    }

    public void checkPendingCommand() {
        if (this.pendingAutoCommand && this.isLive) {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.field_1724 != null && mc.field_1724.field_3944 != null) {
                this.pendingAutoCommand = false;
                onStreamStarted();
            }
        }
        if (HudConfig.INSTANCE.twitchEnabled && !this.manualDisconnect && (this.status == Status.DISCONNECTED || this.status == Status.ERROR) && !this.isConnecting.get()) {
            String rawChannel = HudConfig.INSTANCE.twitchChannel;
            if (rawChannel != null && !rawChannel.trim().isEmpty()) {
                long now = System.currentTimeMillis();
                if (now - this.lastReconnectAttemptTime >= 25000L) {
                    this.lastReconnectAttemptTime = now;
                    String cleanChannel = cleanChannelName(rawChannel);
                    if (!cleanChannel.isEmpty()) {
                        this.connect(cleanChannel);
                    }
                }
            }
        }
    }

    public void executeStreamCommand(String rawCommand, boolean isTest) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return;
        }
        String chan = this.currentJoinedChannel != null && !this.currentJoinedChannel.isEmpty()
                ? this.currentJoinedChannel : cleanChannelName(HudConfig.INSTANCE.twitchChannel);
        String name = this.channelDisplayName != null && !this.channelDisplayName.isEmpty()
                ? this.channelDisplayName : chan;

        String cmd = rawCommand.replace("{channel}", chan != null ? chan : "")
                               .replace("{name}", name != null ? name : "");

        final String finalCmd = cmd.trim();
        class_310 mc = class_310.method_1551();
        if (mc == null) return;

        mc.execute(() -> {
            try {
                if (mc.field_1724 != null && mc.field_1724.field_3944 != null) {
                    if (finalCmd.startsWith("/")) {
                        mc.field_1724.field_3944.method_45730(finalCmd.substring(1));
                    } else {
                        mc.field_1724.field_3944.method_45729(finalCmd);
                    }
                    if (mc.field_1705 != null) {
                        String prefix = isTest ? "\u00a7d[Twitch \u0422\u0435\u0441\u0442] " : "\u00a7d[Twitch] \u00a7a\u25cf \u0421\u0442\u0440\u0438\u043c \u0437\u0430\u043f\u0443\u0449\u0435\u043d! ";
                        String msg = prefix + "\u00a7f\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u0430: \u00a7e" + finalCmd;
                        mc.field_1705.method_1758((class_2561)class_2561.method_43470(msg), false);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}

