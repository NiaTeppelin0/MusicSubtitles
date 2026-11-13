package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_10799;
import net.minecraft.class_12249;
import net.minecraft.class_1657;
import net.minecraft.class_1921;
import net.minecraft.class_1934;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_640;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class PlayerBadgeManager {
    public static final PlayerBadgeManager INSTANCE = new PlayerBadgeManager();
    public static final class_2960 MOD_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/mod_icon.png");
    private static final Set<UUID> MOD_USERS = Collections.synchronizedSet(new HashSet<UUID>());
    private static final HttpClient RELAY_HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    private static volatile List<String> cachedModUserNames = Collections.emptyList();
    private static volatile long lastModNamesRefreshTime = 0L;
    private static volatile int lastModUsersCount = -1;
    private volatile boolean isConnectingRelay = false;
    private long lastTickUpdate = 0L;
    private long lastRelayAttempt = 0L;
    private long lastPingTime = 0L;
    private long lastPingSentTime = 0L;
    private volatile int relayPingMs = 0;
    private volatile WebSocket relayWs = null;
    private volatile String lastServerAddress = "";
    private volatile String currentRoom = null;
    private volatile boolean isHost = false;
    private volatile String roomHostName = "";
    private volatile boolean roomHasPassword = false;
    private volatile String currentRoomPassword = null;
    private volatile String pendingRoomAction = null;
    private volatile String pendingJoinCode = null;
    private volatile String pendingJoinPassword = null;
    private volatile TrackState syncedTrack = null;
    private long lastTrackUpdateSent = 0L;
    private String lastResolvedTrackKey = "";
    private String cachedResolvedDirectUrl = "";
    private String cachedResolvedCoverUrl = "";
    private volatile boolean isResolvingDirectUrl = false;

    public static record RoomMember(String name, String uuid, boolean isHost, int pingMs) {}
    private final List<RoomMember> roomMembers = new java.util.concurrent.CopyOnWriteArrayList<>();

    public List<RoomMember> getRoomMembers() {
        return Collections.unmodifiableList(this.roomMembers);
    }

    public boolean roomHasPassword() {
        return this.roomHasPassword;
    }

    public int getRelayPingMs() {
        return this.relayPingMs;
    }

    public void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderWorldBadges);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        this.lastRelayAttempt = System.currentTimeMillis();
    }

    public static void registerModUser(UUID uuid) {
        if (uuid != null) {
            MOD_USERS.add(uuid);
        }
    }

    public static void unregisterModUser(UUID uuid) {
        if (uuid != null) {
            MOD_USERS.remove(uuid);
        }
    }

    public static boolean hasMod(UUID uuid) {
        return uuid != null && MOD_USERS.contains(uuid);
    }

    private void renderWorldBadges(WorldRenderContext context) {
        if (!HudConfig.INSTANCE.playerNametagBadge) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null || mc.field_1724 == null || mc.field_1773 == null) {
            return;
        }
        if (MOD_USERS.isEmpty() && !mc.method_1496() && !HudConfig.INSTANCE.showOwnNametagBadge) {
            return;
        }
        class_4587 matrices = context.matrices();
        class_4597 consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }
        class_4184 camera = mc.field_1773.method_19418();
        class_243 cameraPos = camera.method_71156();
        class_327 textRenderer = mc.field_1772;
        if (textRenderer == null) {
            return;
        }

        for (class_1657 player : mc.field_1687.method_18456()) {
            if (player == null || !player.method_5805()) {
                continue;
            }
            boolean isSelf = player == mc.field_1724;
            if (isSelf) {
                if (!HudConfig.INSTANCE.showOwnNametagBadge) {
                    continue;
                }
                if (mc.field_1690.method_31044().method_31034()) {
                    continue;
                }
            } else {
                UUID uuid = player.method_5667();
                if (!MOD_USERS.contains(uuid) && !mc.method_1496()) {
                    continue;
                }
            }

            double dx = player.method_23317() - cameraPos.field_1352;
            double dy = player.method_23318() - cameraPos.field_1351;
            double dz = player.method_23321() - cameraPos.field_1350;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > 48.0 * 48.0 || distSq < 0.25) {
                continue;
            }

            float heightOffset = player.method_17682() + (isSelf ? 0.48f : 0.62f);
            class_2561 playerText = player.method_5477();
            int nameW = textRenderer.method_27525(playerText);
            float iconSize = 9.0f;
            float padLeft = 4.0f;
            float padRight = 5.0f;
            float spaceW = (float)textRenderer.method_1727(" ");
            float badgeW = padLeft + iconSize + spaceW + nameW + padRight;
            float badgeH = 10.0f;
            float x = -badgeW / 2.0f;
            float y = -badgeH / 2.0f;
            float scale = 0.025f;

            matrices.method_22903();
            matrices.method_46416((float)dx, (float)dy + heightOffset, (float)dz);
            matrices.method_22907(camera.method_23767());
            matrices.method_22905(-scale, -scale, scale);
            Matrix4f matrix4f = matrices.method_23760().method_23761();

            drawColorQuad(matrix4f, consumers, x, y, badgeW, badgeH, 0x13, 0x11, 0x24, 0xF2);

            drawColorQuad(matrix4f, consumers, x, y, badgeW, 1.0f, 0x00, 0xF5, 0xD4, 0xFF);
            drawColorQuad(matrix4f, consumers, x, y + badgeH - 1.0f, badgeW, 1.0f, 0x00, 0xF5, 0xD4, 0xFF);
            drawColorQuad(matrix4f, consumers, x, y, 1.0f, badgeH, 0x00, 0xF5, 0xD4, 0xFF);
            drawColorQuad(matrix4f, consumers, x + badgeW - 1.0f, y, 1.0f, badgeH, 0x00, 0xF5, 0xD4, 0xFF);

            float iconX = x + padLeft;
            float iconY = y + (badgeH - iconSize) / 2.0f;
            drawTexturedQuad(matrix4f, consumers, iconX, iconY, iconSize, iconSize, MOD_ICON);

            float textX = iconX + iconSize + spaceW;
            float textY = y + 1.0f;
            textRenderer.method_27522(playerText, textX, textY, 0xFFFFFFFF, true, matrix4f, consumers, class_327.class_6415.field_33994, 0, 0xF000F0);

            if (consumers instanceof class_4597.class_4598 immediate) {
                immediate.method_22994(class_12249.method_76001());
                immediate.method_22994(class_12249.method_76028(MOD_ICON));
            }

            matrices.method_22909();
        }
    }

    private static void refreshModUserNames(class_310 mc, long now) {
        if (mc == null || mc.method_1562() == null) {
            return;
        }
        int curCount = MOD_USERS.size();
        if (curCount == 0) {
            if (!cachedModUserNames.isEmpty()) {
                cachedModUserNames = Collections.emptyList();
            }
            lastModUsersCount = 0;
            return;
        }
        if (curCount == lastModUsersCount && now - lastModNamesRefreshTime < 500L) {
            return;
        }
        lastModNamesRefreshTime = now;
        lastModUsersCount = curCount;
        List<String> names = new ArrayList<>();
        for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry == null || entry.method_2966() == null) {
                continue;
            }
            UUID uuid = entry.method_2966().id();
            if (uuid != null && MOD_USERS.contains(uuid)) {
                String name = entry.method_2966().name();
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        cachedModUserNames = Collections.unmodifiableList(names);
    }

    public static boolean isModTabText(class_2561 text) {
        if (text == null || !HudConfig.INSTANCE.tabBadgeEnabled) {
            return false;
        }
        class_310 mc = class_310.method_1551();
        if (mc.method_1562() == null || mc.field_1705 == null) {
            return false;
        }
        boolean isSingle = mc.method_1496();
        if (MOD_USERS.isEmpty() && !isSingle && !HudConfig.INSTANCE.showOwnNametagBadge) {
            return false;
        }
        String s = text.getString();
        if (mc.field_1724 != null && (isSingle || HudConfig.INSTANCE.showOwnNametagBadge)) {
            class_2561 myText = mc.field_1724.method_5477();
            if (myText != null && s.contains(myText.getString())) {
                return true;
            }
        }
        if (MOD_USERS.isEmpty()) {
            return false;
        }
        refreshModUserNames(mc, System.currentTimeMillis());
        for (String name : cachedModUserNames) {
            if (s.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static int tabBadgeExtraWidth(class_327 renderer) {
        return 3 + 8 + renderer.method_1727(" ") + 4;
    }

    public static void drawTabBadge(class_332 context, class_327 renderer, class_2561 text, int x, int y, int color) {
        int spaceW = renderer.method_1727(" ");
        int nameW = renderer.method_27525(text);
        int cardX = x - 1;
        int cardW = 1 + 3 + 8 + spaceW + nameW + 3;
        int cardH = 12;
        int cardY = y - 2;
        context.method_25294(cardX, cardY, cardX + cardW, cardY + cardH, 0xFF131124);
        context.method_25294(cardX, cardY, cardX + cardW, cardY + 1, 0xFF00F5D4);
        context.method_25294(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFF00F5D4);
        context.method_25294(cardX, cardY + 1, cardX + 1, cardY + cardH - 1, 0xFF00F5D4);
        context.method_25294(cardX + cardW - 1, cardY + 1, cardX + cardW, cardY + cardH - 1, 0xFF00F5D4);
        int iconX = x + 3;
        try {
            context.method_25291(class_10799.field_56883, MOD_ICON, iconX, y, 0.0f, 0.0f, 8, 8, 8, 8, -1);
        } catch (Throwable ignored) {}
        context.method_27535(renderer, text, iconX + 8 + spaceW, y, color);
    }

    private static void drawColorQuad(Matrix4f matrix, class_4597 consumers, float x, float y, float w, float h, int r, int g, int b, int a) {
        class_1921 layer = class_12249.method_76001();
        class_4588 buffer = consumers.method_73477(layer);
        buffer.method_22918((Matrix4fc)matrix, x, y, 0.0f).method_1336(r, g, b, a).method_60803(0xF000F0);
        buffer.method_22918((Matrix4fc)matrix, x, y + h, 0.0f).method_1336(r, g, b, a).method_60803(0xF000F0);
        buffer.method_22918((Matrix4fc)matrix, x + w, y + h, 0.0f).method_1336(r, g, b, a).method_60803(0xF000F0);
        buffer.method_22918((Matrix4fc)matrix, x + w, y, 0.0f).method_1336(r, g, b, a).method_60803(0xF000F0);
    }

    private static void drawTexturedQuad(Matrix4f matrix, class_4597 consumers, float x, float y, float w, float h, class_2960 texture) {
        class_1921 layer = class_12249.method_76028(texture);
        class_4588 buffer = consumers.method_73477(layer);
        buffer.method_22918((Matrix4fc)matrix, x, y, 0.0f).method_1336(255, 255, 255, 255).method_22913(0.0f, 0.0f).method_60803(0xF000F0);
        buffer.method_22918((Matrix4fc)matrix, x, y + h, 0.0f).method_1336(255, 255, 255, 255).method_22913(0.0f, 1.0f).method_60803(0xF000F0);
        buffer.method_22918((Matrix4fc)matrix, x + w, y + h, 0.0f).method_1336(255, 255, 255, 255).method_22913(1.0f, 1.0f).method_60803(0xF000F0);
        buffer.method_22918((Matrix4fc)matrix, x + w, y, 0.0f).method_1336(255, 255, 255, 255).method_22913(1.0f, 0.0f).method_60803(0xF000F0);
    }

    private void onClientTick(class_310 mc) {
        if (mc.field_1724 != null) {
            MOD_USERS.add(mc.field_1724.method_5667());
        }
        long now = System.currentTimeMillis();
        if (now - this.lastTickUpdate >= 500L) {
            this.lastTickUpdate = now;
            refreshModUserNames(mc, now);
            this.checkRelayConnection(mc, now);
        }
        if (this.isRoomHost() && NowPlayingClient.INSTANCE != null) {
            this.sendTrackUpdateIfHost(NowPlayingClient.INSTANCE.getState());
        }
    }

    private void checkRelayConnection(class_310 mc, long now) {
        if (!HudConfig.INSTANCE.relayServerEnabled) {
            return;
        }
        String currentAddress = "singleplayer";
        if (mc.method_1558() != null && mc.method_1558().field_3761 != null) {
            currentAddress = mc.method_1558().field_3761;
        }
        if (!currentAddress.equalsIgnoreCase(this.lastServerAddress)) {
            this.lastServerAddress = currentAddress;
            if (this.relayWs != null) {
                this.sendHandshake(mc);
            }
        }
        if (this.relayWs == null && !this.isConnectingRelay) {
            if (now - this.lastRelayAttempt > 10000L) {
                this.lastRelayAttempt = now;
                this.tryConnectRelay();
            }
        } else if (this.relayWs != null) {
            if (now - this.lastPingTime > 15000L) {
                this.lastPingTime = now;
                this.lastPingSentTime = now;
                try {
                    this.relayWs.sendText("{\"event\":\"ping\"}", true);
                } catch (Throwable ignored) {
                    this.relayWs = null;
                }
            }
        }
    }

    private synchronized void tryConnectRelay() {
        if (!HudConfig.INSTANCE.relayServerEnabled || this.isConnectingRelay) {
            return;
        }
        if (this.relayWs != null) {
            return;
        }
        String url = HudConfig.INSTANCE.relayServerUrl;
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        this.isConnectingRelay = true;
        try {
            RELAY_HTTP_CLIENT.newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).buildAsync(URI.create(url.trim()), new WebSocket.Listener() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void onOpen(WebSocket webSocket) {
                    isConnectingRelay = false;
                    relayWs = webSocket;
                    class_310 mc = class_310.method_1551();
                    sendHandshake(mc);
                    String action = pendingRoomAction;
                    String joinCode = pendingJoinCode;
                    String joinPass = pendingJoinPassword;
                    pendingRoomAction = null;
                    pendingJoinCode = null;
                    pendingJoinPassword = null;
                    if ("CREATE".equals(action)) {
                        sendCreateRoomJson(webSocket, joinPass);
                    } else if ("JOIN".equals(action) && joinCode != null && !joinCode.isEmpty()) {
                        sendJoinRoomJson(webSocket, joinCode, joinPass);
                    } else if (currentRoom != null && !currentRoom.isEmpty()) {
                        if (isHost) {
                            sendCreateRoomJson(webSocket, currentRoomPassword);
                        } else {
                            sendJoinRoomJson(webSocket, currentRoom, currentRoomPassword);
                        }
                    }
                    WebSocket.Listener.super.onOpen(webSocket);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    buffer.append(data);
                    if (last) {
                        String full = buffer.toString();
                        buffer.setLength(0);
                        handleIncomingRelayJson(full);
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    isConnectingRelay = false;
                    relayWs = null;
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    isConnectingRelay = false;
                    relayWs = null;
                }
            }).exceptionally(ex -> {
                isConnectingRelay = false;
                relayWs = null;
                return null;
            });
        } catch (Throwable ignored) {
            this.isConnectingRelay = false;
            this.relayWs = null;
        }
    }

    private void sendCreateRoomJson(WebSocket ws, String password) {
        if (ws == null) {
            return;
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("event", "create_room");
        if (password != null && !password.isEmpty()) {
            msg.addProperty("password", password);
        }
        try {
            ws.sendText(msg.toString(), true);
        } catch (Throwable ignored) {}
    }

    private void sendJoinRoomJson(WebSocket ws, String roomCode, String password) {
        if (ws == null || roomCode == null || roomCode.isEmpty()) {
            return;
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("event", "join_room");
        msg.addProperty("room", roomCode);
        if (password != null && !password.isEmpty()) {
            msg.addProperty("password", password);
        }
        try {
            ws.sendText(msg.toString(), true);
        } catch (Throwable ignored) {}
    }

    public void sendHandshake(class_310 mc) {
        WebSocket ws = this.relayWs;
        if (ws == null || mc == null || mc.field_1724 == null) {
            return;
        }
        String sIp = "singleplayer";
        if (mc.method_1558() != null && mc.method_1558().field_3761 != null) {
            sIp = mc.method_1558().field_3761;
        }
        this.lastServerAddress = sIp;
        JsonObject msg = new JsonObject();
        msg.addProperty("event", "handshake");
        msg.addProperty("uuid", mc.field_1724.method_5667().toString());
        msg.addProperty("name", mc.field_1724.method_5477().getString());
        msg.addProperty("server", sIp);
        msg.addProperty("version", "2.8");
        try {
            ws.sendText(msg.toString(), true);
        } catch (Throwable ignored) {
            this.relayWs = null;
        }
    }

    public String getCurrentRoom() {
        return this.currentRoom;
    }

    public boolean isInRoom() {
        return this.currentRoom != null && !this.currentRoom.isEmpty();
    }

    public boolean isRoomHost() {
        return this.isInRoom() && this.isHost;
    }

    public String getRoomHostName() {
        return this.roomHostName;
    }

    public TrackState getSyncedTrack() {
        return this.syncedTrack;
    }

    public boolean hasSyncedTrack() {
        return !this.isHost && this.syncedTrack != null && this.syncedTrack.isActive();
    }

    public void createRoom() {
        this.createRoom(null);
    }

    public void createRoom(String password) {
        String cleanPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : null;
        this.currentRoomPassword = cleanPassword;
        WebSocket ws = this.relayWs;
        if (ws == null) {
            this.pendingRoomAction = "CREATE";
            this.pendingJoinCode = null;
            this.pendingJoinPassword = cleanPassword;
            this.tryConnectRelay();
            return;
        }
        this.pendingRoomAction = null;
        this.sendCreateRoomJson(ws, cleanPassword);
    }

    public void joinRoom(String code) {
        this.joinRoom(code, null);
    }

    public void joinRoom(String code, String password) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        String cleanPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : null;
        this.currentRoomPassword = cleanPassword;
        WebSocket ws = this.relayWs;
        if (ws == null) {
            this.pendingRoomAction = "JOIN";
            this.pendingJoinCode = cleanCode;
            this.pendingJoinPassword = cleanPassword;
            this.tryConnectRelay();
            return;
        }
        this.pendingRoomAction = null;
        this.sendJoinRoomJson(ws, cleanCode, cleanPassword);
    }

    public void kickPlayer(String uuidOrName) {
        WebSocket ws = this.relayWs;
        if (ws != null && this.isRoomHost() && uuidOrName != null && !uuidOrName.trim().isEmpty()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("event", "kick_player");
            msg.addProperty("uuid", uuidOrName.trim());
            try {
                ws.sendText(msg.toString(), true);
            } catch (Throwable ignored) {
            }
        }
    }

    public void banPlayer(String uuidOrName) {
        WebSocket ws = this.relayWs;
        if (ws != null && this.isRoomHost() && uuidOrName != null && !uuidOrName.trim().isEmpty()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("event", "ban_player");
            msg.addProperty("uuid", uuidOrName.trim());
            try {
                ws.sendText(msg.toString(), true);
            } catch (Throwable ignored) {
            }
        }
    }

    public void leaveRoom() {
        boolean wasHost = this.isHost;
        WebSocket ws = this.relayWs;
        if (ws != null) {
            try {
                ws.sendText("{\"event\":\"leave_room\"}", true);
            } catch (Throwable ignored) {
            }
        }
        this.pendingRoomAction = null;
        this.pendingJoinCode = null;
        this.pendingJoinPassword = null;
        this.currentRoom = null;
        this.isHost = false;
        this.roomHostName = "";
        this.roomHasPassword = false;
        this.currentRoomPassword = null;
        this.syncedTrack = null;
        this.roomMembers.clear();
        this.lastResolvedTrackKey = "";
        this.cachedResolvedDirectUrl = "";
        this.cachedResolvedCoverUrl = "";
        this.isResolvingDirectUrl = false;
        if (!wasHost) {
            com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().stop();
        }
        com.example.yandexmusichud.compat.VoiceChatCompat.onRoomLeft();
    }

    public synchronized void shutdown() {
        WebSocket ws = this.relayWs;
        this.relayWs = null;
        this.isConnectingRelay = false;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
            } catch (Throwable ignored) {}
        }
    }

    public void sendTrackUpdateIfHost(TrackState state) {
        WebSocket ws = this.relayWs;
        if (!this.isRoomHost() || ws == null || state == null || !state.isActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastTrackUpdateSent < 1000L) {
            return;
        }
        this.lastTrackUpdateSent = now;
        String directMp3Url = state.directUrl();
        String coverUrl = state.coverUrl() != null ? state.coverUrl() : "";
        if ((directMp3Url == null || directMp3Url.isEmpty()) && !state.artist().isEmpty()) {
            String trackKey = (state.artist() + " - " + state.title()).toLowerCase(Locale.ROOT);
            if (trackKey.equals(this.lastResolvedTrackKey)) {
                directMp3Url = this.cachedResolvedDirectUrl;
                if (coverUrl.isEmpty()) {
                    coverUrl = this.cachedResolvedCoverUrl;
                }
            } else if (!this.isResolvingDirectUrl) {
                this.lastResolvedTrackKey = trackKey;
                this.cachedResolvedDirectUrl = "";
                this.cachedResolvedCoverUrl = "";
                this.isResolvingDirectUrl = true;
                final String fArtist = state.artist();
                final String fTitle = state.title();
                CompletableFuture.runAsync(() -> {
                    try {
                        String[] res = com.example.yandexmusichud.local.SafeModeStreamer.INSTANCE.resolveDirectMp3Url(fArtist, fTitle);
                        if (res != null && res.length > 0 && res[0] != null && !res[0].isEmpty()) {
                            cachedResolvedDirectUrl = res[0];
                            if (res.length > 1 && res[1] != null) {
                                cachedResolvedCoverUrl = res[1];
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        isResolvingDirectUrl = false;
                    }
                });
            }
        }
        if (directMp3Url == null) {
            directMp3Url = "";
        }
        String combinedCover = !directMp3Url.isEmpty() ? (coverUrl + "|" + directMp3Url) : coverUrl;
        JsonObject trackJson = new JsonObject();
        trackJson.addProperty("artist", state.artist());
        trackJson.addProperty("title", state.title());
        trackJson.addProperty("album", state.album() != null ? state.album() : "");
        trackJson.addProperty("position_ms", state.positionMs());
        trackJson.addProperty("duration_ms", state.durationMs());
        trackJson.addProperty("state", state.status());
        trackJson.addProperty("direct_url", directMp3Url);
        trackJson.addProperty("cover_url", combinedCover);
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "update_track");
        payload.add("track", trackJson);
        try {
            ws.sendText(payload.toString(), true);
        } catch (Throwable ignored) {
        }
    }

    private static String getJsonString(JsonObject obj, String key, String def) {
        if (obj == null) {
            return def;
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : def;
    }

    private static long getJsonLong(JsonObject obj, String key, long def) {
        if (obj == null) {
            return def;
        }
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsLong();
            } catch (Throwable ignored) {}
        }
        return def;
    }

    private static boolean getJsonBoolean(JsonObject obj, String key, boolean def) {
        if (obj == null) {
            return def;
        }
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive()) {
            try {
                return el.getAsBoolean();
            } catch (Throwable ignored) {}
        }
        return def;
    }

    private static void sendChatAsync(class_310 mc, String msg) {
        if (mc != null) {
            mc.execute(() -> {
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470(msg), false);
                }
            });
        }
    }

    private void parseTrackPayload(JsonObject tObj) {
        if (tObj == null) {
            return;
        }
        String artist = getJsonString(tObj, "artist", "");
        String title = getJsonString(tObj, "title", "");
        String album = getJsonString(tObj, "album", "");
        long pos = getJsonLong(tObj, "position_ms", 0L);
        long dur = getJsonLong(tObj, "duration_ms", 0L);
        String st = getJsonString(tObj, "state", "playing");
        String directUrl = getJsonString(tObj, "direct_url", null);
        String coverUrl = getJsonString(tObj, "cover_url", null);
        int sep = coverUrl != null ? coverUrl.indexOf('|') : -1;
        if (sep >= 0) {
            if (directUrl == null || directUrl.isEmpty()) {
                directUrl = coverUrl.substring(sep + 1);
            }
            coverUrl = coverUrl.substring(0, sep);
        }
        boolean active = !title.isEmpty() || !artist.isEmpty();
        TrackState nextTrack = new TrackState(active, title, artist, album, st, pos, dur, null, System.currentTimeMillis(), directUrl, coverUrl);
        this.syncedTrack = nextTrack;
        if (!this.isHost) {
            CompletableFuture.runAsync(() -> com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().syncToTrack(nextTrack));
        }
    }

    private void handleIncomingRelayJson(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject obj = parsed.getAsJsonObject();
            String event = getJsonString(obj, "event", "");
            if ("pong".equals(event)) {
                long now = System.currentTimeMillis();
                if (this.lastPingSentTime > 0L) {
                    this.relayPingMs = (int)(now - this.lastPingSentTime);
                }
            } else if ("active_mod_users".equals(event) || "welcome".equals(event)) {
                JsonArray arr = obj.has("users") && obj.get("users").isJsonArray()
                    ? obj.getAsJsonArray("users")
                    : (obj.has("server_players") && obj.get("server_players").isJsonArray() ? obj.getAsJsonArray("server_players") : null);
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        try {
                            JsonElement elem = arr.get(i);
                            if (elem != null && elem.isJsonPrimitive()) {
                                registerModUser(UUID.fromString(elem.getAsString()));
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } else if ("room_created".equals(event)) {
                this.currentRoom = getJsonString(obj, "room", "");
                this.isHost = true;
                this.roomHasPassword = getJsonBoolean(obj, "has_password", false);
                this.roomMembers.clear();
                class_310 mc = class_310.method_1551();
                String myName = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : "Хост";
                String myUuid = mc.field_1724 != null ? mc.field_1724.method_5667().toString() : "";
                this.roomMembers.add(new RoomMember(myName, myUuid, true, this.relayPingMs));
                com.example.yandexmusichud.compat.VoiceChatCompat.onRoomJoined(this.currentRoom, "");
                sendChatAsync(mc, "§b♫ [Music Sync] §aКомната создана! Код: §e" + this.currentRoom + (this.roomHasPassword ? " §6[С паролем]" : "") + " §7(Друзья могут ввести: §f/musicsync join " + this.currentRoom + "§7)");
                if (mc != null) {
                    mc.execute(() -> {
                        if (mc.field_1755 instanceof MusicSyncScreen s) {
                            s.refreshUI();
                        }
                    });
                }
            } else if ("joined_room".equals(event)) {
                this.currentRoom = getJsonString(obj, "room", "");
                this.isHost = false;
                this.roomHasPassword = getJsonBoolean(obj, "has_password", false);
                this.roomHostName = getJsonString(obj, "host_name", "Хост");
                this.roomMembers.clear();
                this.roomMembers.add(new RoomMember(this.roomHostName, "", true, 0));
                class_310 mc = class_310.method_1551();
                String myName = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : "Игрок";
                String myUuid = mc.field_1724 != null ? mc.field_1724.method_5667().toString() : "";
                this.roomMembers.add(new RoomMember(myName, myUuid, false, this.relayPingMs));
                com.example.yandexmusichud.compat.VoiceChatCompat.onRoomJoined(this.currentRoom, this.roomHostName);
                sendChatAsync(mc, "§b♫ [Music Sync] §aВы вошли в комнату §e" + this.currentRoom + " §7(Хост: §f" + this.roomHostName + "§7)");
                if (obj.has("track") && obj.get("track").isJsonObject()) {
                    this.parseTrackPayload(obj.getAsJsonObject("track"));
                }
                if (mc != null) {
                    mc.execute(() -> {
                        if (mc.field_1755 instanceof MusicSyncScreen s) {
                            s.refreshUI();
                        }
                    });
                }
            } else if ("kicked".equals(event)) {
                String reason = getJsonString(obj, "reason", "Вы были исключены из комнаты.");
                class_310 mc = class_310.method_1551();
                sendChatAsync(mc, "§b♫ [Music Sync] §c" + reason);
                this.leaveRoom();
            } else if ("sync_track".equals(event)) {
                if (!this.isHost && obj.has("track") && obj.get("track").isJsonObject()) {
                    this.parseTrackPayload(obj.getAsJsonObject("track"));
                }
            } else if ("player_joined".equals(event)) {
                String pName = getJsonString(obj, "name", "Игрок");
                String pUuid = getJsonString(obj, "uuid", "");
                boolean exists = false;
                for (RoomMember m : this.roomMembers) {
                    if (m.name().equalsIgnoreCase(pName) || (!pUuid.isEmpty() && m.uuid().equalsIgnoreCase(pUuid))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    this.roomMembers.add(new RoomMember(pName, pUuid, false, 0));
                }
                class_310 mc = class_310.method_1551();
                sendChatAsync(mc, "§b♫ [Music Sync] §f" + pName + " §aприсоединился к вашей сессии!");
                if (mc != null) {
                    mc.execute(() -> {
                        if (mc.field_1755 instanceof MusicSyncScreen s) {
                            s.refreshUI();
                        }
                    });
                }
            } else if ("player_left".equals(event)) {
                String pName = getJsonString(obj, "name", "");
                String pUuid = getJsonString(obj, "uuid", "");
                this.roomMembers.removeIf(m -> m.name().equalsIgnoreCase(pName) || (!pUuid.isEmpty() && m.uuid().equalsIgnoreCase(pUuid)));
                class_310 mc = class_310.method_1551();
                sendChatAsync(mc, "§b♫ [Music Sync] §7" + (pName.isEmpty() ? "Игрок" : pName) + " покинул комнату.");
                if (mc != null) {
                    mc.execute(() -> {
                        if (mc.field_1755 instanceof MusicSyncScreen s) {
                            s.refreshUI();
                        }
                    });
                }
            } else if ("room_closed".equals(event)) {
                String reason = getJsonString(obj, "reason", "Комната закрыта");
                class_310 mc = class_310.method_1551();
                sendChatAsync(mc, "§b♫ [Music Sync] §cКомната закрыта: " + reason);
                this.leaveRoom();
                if (mc != null) {
                    mc.execute(() -> {
                        if (mc.field_1755 instanceof MusicSyncScreen s) {
                            s.refreshUI();
                        }
                    });
                }
            } else if ("error".equals(event)) {
                String msg = getJsonString(obj, "message", "Ошибка");
                class_310 mc = class_310.method_1551();
                sendChatAsync(mc, "§b♫ [Music Sync] §c" + msg);
            }
        } catch (Throwable ignored) {
        }
    }
}
