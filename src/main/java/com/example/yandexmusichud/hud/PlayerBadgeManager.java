package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.class_266;
import net.minecraft.class_269;
import net.minecraft.class_274;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_355;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_640;
import net.minecraft.class_8646;
import net.minecraft.class_9013;
import net.minecraft.class_9015;
import net.minecraft.class_9022;
import net.minecraft.class_9025;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class PlayerBadgeManager {
    public static final PlayerBadgeManager INSTANCE = new PlayerBadgeManager();
    public static final class_2960 MOD_ICON = class_2960.method_60655("yandexmusichud", "textures/gui/mod_icon.png");
    private static final Set<UUID> MOD_USERS = Collections.synchronizedSet(new HashSet<UUID>());
    private static final Comparator<class_640> ENTRY_COMPARATOR = initComparator();
    private static final java.lang.reflect.Field HEADER_FIELD = initHeaderField();

    @SuppressWarnings("unchecked")
    private static Comparator<class_640> initComparator() {
        try {
            java.lang.reflect.Field f = class_355.class.getDeclaredField("field_2156");
            f.setAccessible(true);
            Object obj = f.get(null);
            if (obj instanceof Comparator) {
                return (Comparator<class_640>) obj;
            }
        } catch (Throwable ignored) {}
        return Comparator.comparing(e -> e.method_2966().name(), String::compareToIgnoreCase);
    }

    private static java.lang.reflect.Field initHeaderField() {
        try {
            java.lang.reflect.Field f = class_355.class.getDeclaredField("field_2153");
            f.setAccessible(true);
            return f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final HttpClient RELAY_HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    private volatile boolean isConnectingRelay = false;
    private long lastTickUpdate = 0L;
    private long lastRelayAttempt = 0L;
    private long lastPingTime = 0L;
    private long lastPingSentTime = 0L;
    private int relayPingMs = 0;
    private WebSocket relayWs = null;
    private String lastServerAddress = "";
    private String currentRoom = null;
    private boolean isHost = false;
    private String roomHostName = "";
    private boolean roomHasPassword = false;
    private String currentRoomPassword = null;
    private volatile TrackState syncedTrack = null;
    private long lastTrackUpdateSent = 0L;

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

    public static boolean isModTabText(class_2561 text) {
        if (text == null || !HudConfig.INSTANCE.tabBadgeEnabled) {
            return false;
        }
        class_310 mc = class_310.method_1551();
        if (mc.method_1562() == null || mc.field_1705 == null) {
            return false;
        }
        String s = text.getString();
        class_355 playerList = mc.field_1705.method_1750();
        for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry == null || entry.method_2966() == null) {
                continue;
            }
            UUID uuid = entry.method_2966().id();
            boolean isMod = (uuid != null && MOD_USERS.contains(uuid))
                || (mc.field_1724 != null && uuid != null && uuid.equals(mc.field_1724.method_5667()))
                || mc.method_1496();
            if (!isMod) {
                continue;
            }
            String name = entry.method_2966().name();
            if (name != null && !name.isEmpty() && s.contains(name)) {
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

    public void renderTabBadges(class_332 context) {
        if (true) {
            return;
        }
        if (!HudConfig.INSTANCE.tabBadgeEnabled) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1690 == null || mc.field_1724 == null || mc.method_1562() == null || mc.field_1772 == null) {
            return;
        }
        if (!mc.field_1690.field_1907.method_1434()) {
            return;
        }
        class_355 playerList = mc.field_1705 != null ? mc.field_1705.method_1750() : null;
        if (playerList == null) {
            return;
        }
        Collection<class_640> rawEntries = mc.field_1724.field_3944 != null ? mc.field_1724.field_3944.method_45732() : null;
        if (rawEntries == null || rawEntries.isEmpty()) {
            return;
        }

        List<class_640> entries = rawEntries.stream().sorted(ENTRY_COMPARATOR).limit(80L).toList();
        int totalCount = entries.size();
        if (totalCount == 0) {
            return;
        }

        int screenWidth = mc.method_22683().method_4486();
        class_327 textRenderer = mc.field_1772;

        int rows = totalCount;
        int cols = 1;
        while (rows > 20) {
            rows = (totalCount + ++cols - 1) / cols;
        }

        boolean showHeads = mc.method_1542() || mc.method_1562().method_48296().method_10771();
        int spaceW = textRenderer.method_1727(" ");
        int maxNameWidth = 0;
        int objectiveWidth = 0;

        try {
            class_269 scoreboard = mc.field_1687 != null ? mc.field_1687.method_8428() : null;
            class_266 objective = scoreboard != null ? scoreboard.method_1189(class_8646.field_45156) : null;
            int maxScoreW = 0;
            for (class_640 e : entries) {
                class_2561 fn = playerList.method_1918(e);
                maxNameWidth = Math.max(maxNameWidth, textRenderer.method_27525(fn));
                if (objective != null && scoreboard != null && objective.method_1118() != class_274.class_275.field_1471) {
                    class_9015 scoreHolder = class_9015.method_55420(e.method_2966());
                    class_9013 scoreInfo = scoreboard.method_55430(scoreHolder, objective);
                    if (scoreInfo != null) {
                        class_9022 numberFormat = objective.method_55380((class_9022) class_9025.field_47568);
                        class_2561 scoreText = class_9013.method_55398(scoreInfo, numberFormat);
                        int sw = textRenderer.method_27525(scoreText);
                        if (sw > 0) {
                            maxScoreW = Math.max(maxScoreW, spaceW + sw);
                        }
                    }
                }
            }
            if (objective != null) {
                if (objective.method_1118() == class_274.class_275.field_1471) {
                    objectiveWidth = 90;
                } else {
                    objectiveWidth = maxScoreW;
                }
            }
        } catch (Throwable ignored) {
            for (class_640 e : entries) {
                class_2561 fn = playerList.method_1918(e);
                maxNameWidth = Math.max(maxNameWidth, textRenderer.method_27525(fn));
            }
        }

        int colWidth = Math.min(cols * ((showHeads ? 9 : 0) + maxNameWidth + objectiveWidth + 13), screenWidth - 50) / cols;
        int left = screenWidth / 2 - (colWidth * cols + (cols - 1) * 5) / 2;
        int top = 10;

        try {
            if (HEADER_FIELD != null) {
                class_2561 header = (class_2561) HEADER_FIELD.get(playerList);
                if (header != null) {
                    List<?> lines = textRenderer.method_1728(header, screenWidth - 50);
                    top += lines.size() * 9 + 1;
                }
            }
        } catch (Throwable ignored) {}

        int iconSize = 8;

        for (int i = 0; i < totalCount; i++) {
            class_640 entry = entries.get(i);
            if (entry == null || entry.method_2966() == null) {
                continue;
            }
            UUID uuid = entry.method_2966().id();
            boolean isMod = (uuid != null && MOD_USERS.contains(uuid))
                || (mc.field_1724 != null && uuid != null && uuid.equals(mc.field_1724.method_5667()))
                || mc.method_1496();
            if (!isMod) {
                continue;
            }

            int col = i / rows;
            int row = i % rows;
            int entryX = left + col * colWidth + col * 5;
            int entryY = top + row * 9;

            int headW = showHeads ? 9 : 0;
            class_2561 formattedName = playerList.method_1918(entry);
            int nameW = textRenderer.method_27525(formattedName);

            int padLeft = 3;
            int padRight = 4;
            int cardW = padLeft + iconSize + spaceW + nameW + padRight;

            int cardX = showHeads ? (entryX + 8) : Math.max(0, entryX - 1);
            int rightLimit = entryX + colWidth - (objectiveWidth > 0 ? objectiveWidth + 14 : 12);
            int maxCardW = rightLimit - cardX;
            if (cardW > maxCardW) {
                cardW = maxCardW;
            }
            int cardH = 9;
            int cardY = entryY;

            context.method_25294(cardX, cardY, cardX + cardW, cardY + cardH, 0xFF131124);

            context.method_25294(cardX, cardY, cardX + cardW, cardY + 1, 0xFF00F5D4);
            context.method_25294(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFF00F5D4);
            context.method_25294(cardX, cardY, cardX + 1, cardY + cardH, 0xFF00F5D4);
            context.method_25294(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, 0xFF00F5D4);

            int iconX = cardX + padLeft;
            int iconY = entryY;
            try {
                context.method_25291(class_10799.field_56883, MOD_ICON, iconX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize, -1);
            } catch (Throwable ignored) {}

            int textX = iconX + iconSize + spaceW;
            int textY = entryY;
            context.method_27535(textRenderer, formattedName, textX, textY, entry.method_2958() == class_1934.field_9219 ? -1862270977 : -1);
        }
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
            this.updateTabBadges(mc);
            this.checkRelayConnection(mc, now);
        }
        if (this.isRoomHost() && NowPlayingClient.INSTANCE != null) {
            this.sendTrackUpdateIfHost(NowPlayingClient.INSTANCE.getState());
        }
    }

    private void updateTabBadges(class_310 mc) {
        if (mc.method_1562() == null) {
            return;
        }
        for (class_640 entry : mc.method_1562().method_2880()) {
            if (entry == null) {
                continue;
            }
            class_2561 curName = entry.method_2971();
            if (curName != null && curName.getString().contains("\uE000")) {
                entry.method_2962(null);
            }
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
                    if (currentRoom != null && !currentRoom.isEmpty()) {
                        if (isHost) {
                            if (currentRoomPassword != null && !currentRoomPassword.isEmpty()) {
                                relayWs.sendText(String.format("{\"event\":\"create_room\",\"password\":\"%s\"}", currentRoomPassword.replace("\"", "\\\"")), true);
                            } else {
                                relayWs.sendText("{\"event\":\"create_room\"}", true);
                            }
                        } else {
                            if (currentRoomPassword != null && !currentRoomPassword.isEmpty()) {
                                relayWs.sendText(String.format("{\"event\":\"join_room\",\"room\":\"%s\",\"password\":\"%s\"}", currentRoom, currentRoomPassword.replace("\"", "\\\"")), true);
                            } else {
                                relayWs.sendText(String.format("{\"event\":\"join_room\",\"room\":\"%s\"}", currentRoom), true);
                            }
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

    public void sendHandshake(class_310 mc) {
        if (this.relayWs == null || mc == null || mc.field_1724 == null) {
            return;
        }
        String sIp = "singleplayer";
        if (mc.method_1558() != null && mc.method_1558().field_3761 != null) {
            sIp = mc.method_1558().field_3761;
        }
        this.lastServerAddress = sIp;
        String json = String.format("{\"event\":\"handshake\",\"uuid\":\"%s\",\"name\":\"%s\",\"server\":\"%s\",\"version\":\"2.8\"}",
            mc.field_1724.method_5667().toString(),
            mc.field_1724.method_5477().getString(),
            sIp
        );
        try {
            this.relayWs.sendText(json, true);
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
        if (this.relayWs == null) {
            this.tryConnectRelay();
        }
        this.currentRoomPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : null;
        if (this.relayWs != null) {
            try {
                if (this.currentRoomPassword != null) {
                    this.relayWs.sendText(String.format("{\"event\":\"create_room\",\"password\":\"%s\"}", this.currentRoomPassword.replace("\"", "\\\"")), true);
                } else {
                    this.relayWs.sendText("{\"event\":\"create_room\"}", true);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public void joinRoom(String code) {
        this.joinRoom(code, null);
    }

    public void joinRoom(String code, String password) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        if (this.relayWs == null) {
            this.tryConnectRelay();
        }
        this.currentRoomPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : null;
        if (this.relayWs != null) {
            try {
                if (this.currentRoomPassword != null) {
                    this.relayWs.sendText(String.format("{\"event\":\"join_room\",\"room\":\"%s\",\"password\":\"%s\"}", code.trim().toUpperCase(), this.currentRoomPassword.replace("\"", "\\\"")), true);
                } else {
                    this.relayWs.sendText(String.format("{\"event\":\"join_room\",\"room\":\"%s\"}", code.trim().toUpperCase()), true);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public void kickPlayer(String uuidOrName) {
        if (this.relayWs != null && this.isRoomHost() && uuidOrName != null && !uuidOrName.trim().isEmpty()) {
            try {
                this.relayWs.sendText(String.format("{\"event\":\"kick_player\",\"uuid\":\"%s\"}", uuidOrName.trim()), true);
            } catch (Throwable ignored) {
            }
        }
    }

    public void banPlayer(String uuidOrName) {
        if (this.relayWs != null && this.isRoomHost() && uuidOrName != null && !uuidOrName.trim().isEmpty()) {
            try {
                this.relayWs.sendText(String.format("{\"event\":\"ban_player\",\"uuid\":\"%s\"}", uuidOrName.trim()), true);
            } catch (Throwable ignored) {
            }
        }
    }

    public void leaveRoom() {
        if (this.relayWs != null) {
            try {
                this.relayWs.sendText("{\"event\":\"leave_room\"}", true);
            } catch (Throwable ignored) {
            }
        }
        this.currentRoom = null;
        this.isHost = false;
        this.roomHostName = "";
        this.roomHasPassword = false;
        this.currentRoomPassword = null;
        this.syncedTrack = null;
        this.roomMembers.clear();
        com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().stop();
        com.example.yandexmusichud.compat.VoiceChatCompat.onRoomLeft();
    }

    public void sendTrackUpdateIfHost(TrackState state) {
        if (!this.isRoomHost() || this.relayWs == null || state == null || !state.isActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastTrackUpdateSent < 1000L) {
            return;
        }
        this.lastTrackUpdateSent = now;
        String titleEsc = state.title().replace("\"", "\\\"");
        String artistEsc = state.artist().replace("\"", "\\\"");
        String albumEsc = state.album() != null ? state.album().replace("\"", "\\\"") : "";
        String directMp3Url = state.directUrl();
        String coverUrl = state.coverUrl() != null ? state.coverUrl() : "";
        if ((directMp3Url == null || directMp3Url.isEmpty()) && !state.artist().isEmpty()) {
            String[] res = com.example.yandexmusichud.local.SafeModeStreamer.INSTANCE.resolveDirectMp3Url(state.artist(), state.title());
            if (res != null && res.length > 0 && res[0] != null && !res[0].isEmpty()) {
                directMp3Url = res[0];
                if (coverUrl.isEmpty() && res.length > 1 && res[1] != null) {
                    coverUrl = res[1];
                }
            }
        }
        if (directMp3Url == null) {
            directMp3Url = "";
        }
        String directEsc = directMp3Url.replace("\"", "\\\"");
        String combinedCover = !directMp3Url.isEmpty() ? (coverUrl + "|" + directMp3Url) : coverUrl;
        String coverEsc = combinedCover.replace("\"", "\\\"");
        String json = String.format("{\"event\":\"update_track\",\"track\":{\"artist\":\"%s\",\"title\":\"%s\",\"album\":\"%s\",\"position_ms\":%d,\"duration_ms\":%d,\"state\":\"%s\",\"direct_url\":\"%s\",\"cover_url\":\"%s\"}}",
            artistEsc, titleEsc, albumEsc, state.positionMs(), state.durationMs(), state.status(), directEsc, coverEsc);
        try {
            this.relayWs.sendText(json, true);
        } catch (Throwable ignored) {
        }
    }

    private void parseTrackPayload(com.google.gson.JsonObject tObj) {
        String artist = tObj.has("artist") ? tObj.get("artist").getAsString() : "";
        String title = tObj.has("title") ? tObj.get("title").getAsString() : "";
        String album = tObj.has("album") && !tObj.get("album").isJsonNull() ? tObj.get("album").getAsString() : "";
        long pos = tObj.has("position_ms") ? tObj.get("position_ms").getAsLong() : 0L;
        long dur = tObj.has("duration_ms") ? tObj.get("duration_ms").getAsLong() : 0L;
        String st = tObj.has("state") ? tObj.get("state").getAsString() : "playing";
        String directUrl = tObj.has("direct_url") && !tObj.get("direct_url").isJsonNull() ? tObj.get("direct_url").getAsString() : null;
        String coverUrl = tObj.has("cover_url") && !tObj.get("cover_url").isJsonNull() ? tObj.get("cover_url").getAsString() : null;
        if (coverUrl != null && coverUrl.contains("|")) {
            String[] parts = coverUrl.split("\\|", 2);
            coverUrl = parts[0];
            if (directUrl == null || directUrl.isEmpty()) {
                directUrl = parts[1];
            }
        }
        boolean active = !title.isEmpty() || !artist.isEmpty();
        this.syncedTrack = new TrackState(active, title, artist, album, st, pos, dur, null, System.currentTimeMillis(), directUrl, coverUrl);
        if (!this.isHost) {
            com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().syncToTrack(this.syncedTrack);
        }
    }

    private void handleIncomingRelayJson(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            String event = obj.has("event") ? obj.get("event").getAsString() : "";
            if ("pong".equals(event)) {
                long now = System.currentTimeMillis();
                if (this.lastPingSentTime > 0L) {
                    this.relayPingMs = (int)(now - this.lastPingSentTime);
                }
            } else if ("active_mod_users".equals(event) || "welcome".equals(event)) {
                com.google.gson.JsonArray arr = obj.has("users") ? obj.getAsJsonArray("users") : (obj.has("server_players") ? obj.getAsJsonArray("server_players") : null);
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        try {
                            String uStr = arr.get(i).getAsString();
                            registerModUser(UUID.fromString(uStr));
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } else if ("room_created".equals(event)) {
                this.currentRoom = obj.has("room") ? obj.get("room").getAsString() : "";
                this.isHost = true;
                this.roomHasPassword = obj.has("has_password") && obj.get("has_password").getAsBoolean();
                this.roomMembers.clear();
                class_310 mc = class_310.method_1551();
                String myName = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : "Хост";
                String myUuid = mc.field_1724 != null ? mc.field_1724.method_5667().toString() : "";
                this.roomMembers.add(new RoomMember(myName, myUuid, true, this.relayPingMs));
                com.example.yandexmusichud.compat.VoiceChatCompat.onRoomJoined(this.currentRoom, "");
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §aКомната создана! Код: §e" + this.currentRoom + (this.roomHasPassword ? " §6[С паролем]" : "") + " §7(Друзья могут ввести: §f/musicsync join " + this.currentRoom + "§7)"), false);
                }
                mc.execute(() -> {
                    if (mc.field_1755 instanceof MusicSyncScreen s) {
                        s.refreshUI();
                    }
                });
            } else if ("joined_room".equals(event)) {
                this.currentRoom = obj.has("room") ? obj.get("room").getAsString() : "";
                this.isHost = false;
                this.roomHasPassword = obj.has("has_password") && obj.get("has_password").getAsBoolean();
                this.roomHostName = obj.has("host_name") ? obj.get("host_name").getAsString() : "Хост";
                this.roomMembers.clear();
                this.roomMembers.add(new RoomMember(this.roomHostName, "", true, 0));
                class_310 mc = class_310.method_1551();
                String myName = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : "Игрок";
                String myUuid = mc.field_1724 != null ? mc.field_1724.method_5667().toString() : "";
                this.roomMembers.add(new RoomMember(myName, myUuid, false, this.relayPingMs));
                com.example.yandexmusichud.compat.VoiceChatCompat.onRoomJoined(this.currentRoom, this.roomHostName);
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §aВы вошли в комнату §e" + this.currentRoom + " §7(Хост: §f" + this.roomHostName + "§7)"), false);
                }
                if (obj.has("track") && !obj.get("track").isJsonNull()) {
                    this.parseTrackPayload(obj.getAsJsonObject("track"));
                }
                mc.execute(() -> {
                    if (mc.field_1755 instanceof MusicSyncScreen s) {
                        s.refreshUI();
                    }
                });
            } else if ("kicked".equals(event)) {
                String reason = obj.has("reason") ? obj.get("reason").getAsString() : "Вы были исключены из комнаты.";
                class_310 mc = class_310.method_1551();
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §c" + reason), false);
                }
                this.leaveRoom();
            } else if ("sync_track".equals(event)) {
                if (!this.isHost && obj.has("track") && !obj.get("track").isJsonNull()) {
                    this.parseTrackPayload(obj.getAsJsonObject("track"));
                }
            } else if ("player_joined".equals(event)) {
                String pName = obj.has("name") ? obj.get("name").getAsString() : "Игрок";
                String pUuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
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
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §f" + pName + " §aприсоединился к вашей сессии!"), false);
                }
                mc.execute(() -> {
                    if (mc.field_1755 instanceof MusicSyncScreen s) {
                        s.refreshUI();
                    }
                });
            } else if ("player_left".equals(event)) {
                String pName = obj.has("name") ? obj.get("name").getAsString() : "";
                String pUuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
                this.roomMembers.removeIf(m -> m.name().equalsIgnoreCase(pName) || (!pUuid.isEmpty() && m.uuid().equalsIgnoreCase(pUuid)));
                class_310 mc = class_310.method_1551();
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §7" + (pName.isEmpty() ? "Игрок" : pName) + " покинул комнату."), false);
                }
                mc.execute(() -> {
                    if (mc.field_1755 instanceof MusicSyncScreen s) {
                        s.refreshUI();
                    }
                });
            } else if ("room_closed".equals(event)) {
                String reason = obj.has("reason") ? obj.get("reason").getAsString() : "Комната закрыта";
                class_310 mc = class_310.method_1551();
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §cКомната закрыта: " + reason), false);
                }
                this.leaveRoom();
                mc.execute(() -> {
                    if (mc.field_1755 instanceof MusicSyncScreen s) {
                        s.refreshUI();
                    }
                });
            } else if ("error".equals(event)) {
                String msg = obj.has("message") ? obj.get("message").getAsString() : "Ошибка";
                class_310 mc = class_310.method_1551();
                if (mc.field_1724 != null) {
                    mc.field_1724.method_7353((class_2561)class_2561.method_43470("§b♫ [Music Sync] §c" + msg), false);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
