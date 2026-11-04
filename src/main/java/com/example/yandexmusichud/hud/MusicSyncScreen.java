package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.local.LocalMusicPlayer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_342;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

public class MusicSyncScreen extends class_437 {
    private final class_437 parent;
    private int selectedTab = 0;
    private boolean lastKnownInRoom = false;
    private ModernTextFieldWidget roomInputField;
    private ModernTextFieldWidget createPasswordField;
    private ModernTextFieldWidget joinPasswordField;
    private final List<AmbientParticle> bgParticles = new ArrayList<>();
    private final Random random = new Random();
    private boolean particlesInit = false;
    private String statusFeedback = "";
    private int statusFeedbackColor = 0xFFFFFFFF;
    private long statusFeedbackTime = 0L;

    public MusicSyncScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("Синхронизация Музыки"));
        this.parent = parent;
    }

    private void initParticles(int w, int h) {
        if (this.particlesInit) {
            return;
        }
        this.particlesInit = true;
        String[] symbols = new String[]{"\u266a", "\u266b", "\u2726", "\u2736", "\u25cf"};
        int[] colors = new int[]{0x00F5D4, 0x7B2CBF, 0x00BBF9, 0xFEE440, 0xF15BB5};
        for (int i = 0; i < 30; i++) {
            float x = this.random.nextFloat() * (float)w;
            float y = this.random.nextFloat() * (float)h;
            float vx = (this.random.nextFloat() - 0.5f) * 0.45f;
            float vy = -0.2f - this.random.nextFloat() * 0.45f;
            float size = 0.4f + this.random.nextFloat() * 0.6f;
            int col = colors[this.random.nextInt(colors.length)];
            String sym = symbols[this.random.nextInt(symbols.length)];
            this.bgParticles.add(new AmbientParticle(x, y, vx, vy, size, col, sym));
        }
    }

    public void refreshUI() {
        this.method_25426();
    }

    @Override
    protected void method_25426() {
        this.method_37067();
        super.method_25426();
        this.lastKnownInRoom = PlayerBadgeManager.INSTANCE.isInRoom();
        int w = this.field_22789;
        int h = this.field_22790;
        this.initParticles(w, h);

        int cardW = Math.min(w - 32, 430);
        int cardH = Math.min(250, h - 24);
        int cardX = (w - cardW) / 2;
        int cardY = Math.max(12, (h - cardH) / 2);

        int tabBtnW = (cardW - 40) / 2;
        ModernButtonWidget membersTabBtn = new ModernButtonWidget(cardX + 16, cardY + 36, tabBtnW, 20, (class_2561)class_2561.method_43470("👥 Участники"), btn -> {
            this.selectedTab = 0;
            this.playClickSound();
            this.method_25426();
        });
        membersTabBtn.setPrimary(this.selectedTab == 0);
        this.method_37063(membersTabBtn);

        ModernButtonWidget diagTabBtn = new ModernButtonWidget(cardX + 16 + tabBtnW + 8, cardY + 36, tabBtnW, 20, (class_2561)class_2561.method_43470("📊 Диагностика"), btn -> {
            this.selectedTab = 1;
            this.playClickSound();
            this.method_25426();
        });
        diagTabBtn.setPrimary(this.selectedTab == 1);
        this.method_37063(diagTabBtn);

        if (this.selectedTab == 0) {
            this.initMembersTab(cardX, cardY, cardW, cardH);
        } else {
            this.initDiagTab(cardX, cardY, cardW, cardH);
        }

        ModernButtonWidget backBtn = new ModernButtonWidget(cardX + 16, cardY + cardH - 26, 75, 18, (class_2561)class_2561.method_43470("← Назад"), btn -> {
            this.playClickSound();
            this.closeScreen();
        });
        this.method_37063(backBtn);
    }

    private void initMembersTab(int cardX, int cardY, int cardW, int cardH) {
        boolean inRoom = PlayerBadgeManager.INSTANCE.isInRoom();
        if (inRoom) {
            String roomCode = PlayerBadgeManager.INSTANCE.getCurrentRoom();
            boolean isHost = PlayerBadgeManager.INSTANCE.isRoomHost();
            ModernButtonWidget copyBtn = new ModernButtonWidget(cardX + cardW - 162, cardY + 58, 72, 18, (class_2561)class_2561.method_43470("📋 Код"), btn -> {
                this.playClickSound();
                if (roomCode != null) {
                    GLFW.glfwSetClipboardString(class_310.method_1551().method_22683().method_4490(), roomCode);
                    this.statusFeedback = "§aКод скопирован!";
                    this.statusFeedbackColor = 0xFF00F5D4;
                    this.statusFeedbackTime = System.currentTimeMillis();
                }
            });
            this.method_37063(copyBtn);

            ModernButtonWidget leaveBtn = new ModernButtonWidget(cardX + cardW - 86, cardY + 58, 70, 18, (class_2561)class_2561.method_43470("🚪 Выйти"), btn -> {
                this.playClickSound();
                PlayerBadgeManager.INSTANCE.leaveRoom();
                this.statusFeedback = "§7Вы вышли из комнаты";
                this.statusFeedbackColor = 0xFFAAAAAA;
                this.statusFeedbackTime = System.currentTimeMillis();
                this.method_25426();
            });
            leaveBtn.setCustomBgColor(0xCC771111);
            this.method_37063(leaveBtn);

            if (isHost) {
                List<PlayerBadgeManager.RoomMember> members = PlayerBadgeManager.INSTANCE.getRoomMembers();
                int listY = cardY + 114;
                int maxItems = Math.min(members.size(), 5);
                for (int i = 0; i < maxItems; i++) {
                    PlayerBadgeManager.RoomMember m = members.get(i);
                    if (!m.isHost()) {
                        int btnY = listY + i * 20 + 1;
                        int banX = cardX + cardW - 58;
                        int kickX = cardX + cardW - 100;
                        ModernButtonWidget kickBtn = new ModernButtonWidget(kickX, btnY, 38, 16, (class_2561)class_2561.method_43470("👢 Кик"), btn -> {
                            this.playClickSound();
                            String id = (m.uuid() != null && !m.uuid().isEmpty()) ? m.uuid() : m.name();
                            PlayerBadgeManager.INSTANCE.kickPlayer(id);
                            this.statusFeedback = "§cИсключён: " + m.name();
                            this.statusFeedbackColor = 0xFFFF5555;
                            this.statusFeedbackTime = System.currentTimeMillis();
                            this.method_25426();
                        });
                        kickBtn.setCustomBgColor(0xCC663300);
                        this.method_37063(kickBtn);

                        ModernButtonWidget banBtn = new ModernButtonWidget(banX, btnY, 36, 16, (class_2561)class_2561.method_43470("🚫 Бан"), btn -> {
                            this.playClickSound();
                            String id = (m.uuid() != null && !m.uuid().isEmpty()) ? m.uuid() : m.name();
                            PlayerBadgeManager.INSTANCE.banPlayer(id);
                            this.statusFeedback = "§4Заблокирован: " + m.name();
                            this.statusFeedbackColor = 0xFFFF2222;
                            this.statusFeedbackTime = System.currentTimeMillis();
                            this.method_25426();
                        });
                        banBtn.setCustomBgColor(0xCC771111);
                        this.method_37063(banBtn);
                    }
                }
            }
        } else {
            int innerW = cardW - 32;
            int colGap = 8;
            int colW = (innerW - colGap) / 2;
            int colH = cardH - 62 - 34;

            int col1X = cardX + 16;
            int col1Y = cardY + 62;

            this.createPasswordField = new ModernTextFieldWidget(this.field_22793, col1X + 10, col1Y + 46, colW - 20, 20, (class_2561)class_2561.method_43470("Пароль"));
            this.createPasswordField.method_1880(32);
            this.createPasswordField.setPlaceholder("🔒 Без пароля");
            this.method_37063(this.createPasswordField);

            ModernButtonWidget createBtn = new ModernButtonWidget(col1X + 10, col1Y + colH - 26, colW - 20, 20, (class_2561)class_2561.method_43470("✨ Создать комнату"), btn -> {
                this.playClickSound();
                String pwd = (this.createPasswordField != null) ? this.createPasswordField.method_1882().trim() : null;
                PlayerBadgeManager.INSTANCE.createRoom(pwd);
                this.statusFeedback = "§eСоздание комнаты...";
                this.statusFeedbackColor = 0xFFFFAA00;
                this.statusFeedbackTime = System.currentTimeMillis();
                this.method_25426();
            });
            createBtn.setPrimary(true);
            this.method_37063(createBtn);

            int col2X = col1X + colW + colGap;
            int col2Y = col1Y;

            int codeW = colW - 20 - 46;
            this.roomInputField = new ModernTextFieldWidget(this.field_22793, col2X + 10, col2Y + 46, codeW, 20, (class_2561)class_2561.method_43470("Код"));
            this.roomInputField.method_1880(5);
            this.roomInputField.setPlaceholder("🔑 КОД");
            this.method_37063(this.roomInputField);

            ModernButtonWidget pasteBtn = new ModernButtonWidget(col2X + 10 + codeW + 4, col2Y + 46, 42, 20, (class_2561)class_2561.method_43470("📋 Встав."), btn -> {
                this.playClickSound();
                String cb = GLFW.glfwGetClipboardString(class_310.method_1551().method_22683().method_4490());
                if (cb != null) {
                    String clean = cb.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
                    if (clean.length() > 5) {
                        clean = clean.substring(0, 5);
                    }
                    this.roomInputField.method_1852(clean);
                }
            });
            this.method_37063(pasteBtn);

            this.joinPasswordField = new ModernTextFieldWidget(this.field_22793, col2X + 10, col2Y + 82, colW - 20, 20, (class_2561)class_2561.method_43470("Пароль"));
            this.joinPasswordField.method_1880(32);
            this.joinPasswordField.setPlaceholder("🔒 Пароль");
            this.method_37063(this.joinPasswordField);

            ModernButtonWidget joinBtn = new ModernButtonWidget(col2X + 10, col2Y + colH - 26, colW - 20, 20, (class_2561)class_2561.method_43470("🚀 Присоединиться"), btn -> {
                this.playClickSound();
                String code = this.roomInputField.method_1882().trim().toUpperCase();
                String pwd = this.joinPasswordField.method_1882().trim();
                if (!code.isEmpty()) {
                    PlayerBadgeManager.INSTANCE.joinRoom(code, pwd.isEmpty() ? null : pwd);
                    this.statusFeedback = "§eВход в комнату " + code + "...";
                    this.statusFeedbackColor = 0xFFFFAA00;
                    this.statusFeedbackTime = System.currentTimeMillis();
                    this.method_25426();
                }
            });
            this.method_37063(joinBtn);
        }
    }

    private void initDiagTab(int cardX, int cardY, int cardW, int cardH) {
        ModernButtonWidget resyncBtn = new ModernButtonWidget(cardX + cardW - 176, cardY + cardH - 26, 80, 18, (class_2561)class_2561.method_43470("⚡ Синхрон."), btn -> {
            this.playClickSound();
            TrackState st = PlayerBadgeManager.INSTANCE.getSyncedTrack();
            if (st != null && st.isActive()) {
                LocalMusicPlayer.getInstance().syncToTrack(st);
                this.statusFeedback = "§aСигнал синхронизации обновлён!";
                this.statusFeedbackColor = 0xFF00F5D4;
                this.statusFeedbackTime = System.currentTimeMillis();
            } else {
                this.statusFeedback = "§7Нет активного сетевого трека";
                this.statusFeedbackColor = 0xFFAAAAAA;
                this.statusFeedbackTime = System.currentTimeMillis();
            }
        });
        this.method_37063(resyncBtn);

        ModernButtonWidget clearCacheBtn = new ModernButtonWidget(cardX + cardW - 90, cardY + cardH - 26, 74, 18, (class_2561)class_2561.method_43470("🧹 Очистить"), btn -> {
            this.playClickSound();
            this.clearTrackCache();
            this.statusFeedback = "§aКэш очищен!";
            this.statusFeedbackColor = 0xFF00F5D4;
            this.statusFeedbackTime = System.currentTimeMillis();
        });
        this.method_37063(clearCacheBtn);
    }

    private void clearTrackCache() {
        try {
            Path cacheDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("cache");
            if (Files.exists(cacheDir)) {
                File[] files = cacheDir.toFile().listFiles();
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private long[] getCacheStats() {
        long count = 0L;
        long totalBytes = 0L;
        try {
            Path cacheDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("cache");
            if (Files.exists(cacheDir)) {
                File[] files = cacheDir.toFile().listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            count++;
                            totalBytes += f.length();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return new long[]{count, totalBytes};
    }

    private void playClickSound() {
        try {
            if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)class_3417.field_15234, 1.0f, 0.8f));
            }
        } catch (Throwable ignored) {}
    }

    private void closeScreen() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    @Override
    public boolean method_25404(class_11908 input) {
        int keyCode = input.comp_4795();
        if (keyCode == 256) {
            this.closeScreen();
            return true;
        }
        return super.method_25404(input);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int w = this.field_22789;
        int h = this.field_22790;
        context.method_25294(0, 0, w, h, 0xCC090C12);

        float dt60 = Math.min(3.0f, delta);
        for (AmbientParticle p : this.bgParticles) {
            p.update(w, h, dt60);
            int a = (int)(p.alpha * 120.0f);
            if (a > 3) {
                context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, a << 24 | (p.color & 0xFFFFFF), false);
            }
        }

        class_327 f = this.field_22793;
        int cardW = Math.min(w - 32, 430);
        int cardH = Math.min(250, h - 24);
        int cardX = (w - cardW) / 2;
        int cardY = Math.max(12, (h - cardH) / 2);

        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 8, 0xEE121624);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 8, 0xFF00F5D4, 0x5500F5D4);

        context.method_51433(f, "§b♫ СОВМЕСТНОЕ ПРОСЛУШИВАНИЕ", cardX + 16, cardY + 12, -1, true);
        context.method_51433(f, "§7Синхронизация треков между игроками на любых серверах", cardX + 16, cardY + 23, 0xFF888899, false);

        boolean curInRoom = PlayerBadgeManager.INSTANCE.isInRoom();
        if (curInRoom != this.lastKnownInRoom) {
            this.lastKnownInRoom = curInRoom;
            this.method_25426();
            return;
        }

        if (this.selectedTab == 0) {
            this.renderMembersTab(context, f, cardX, cardY, cardW, cardH);
        } else {
            this.renderDiagTab(context, f, cardX, cardY, cardW, cardH);
        }

        if (!this.statusFeedback.isEmpty() && System.currentTimeMillis() - this.statusFeedbackTime < 4000L) {
            context.method_51433(f, this.statusFeedback, cardX + 100, cardY + cardH - 22, this.statusFeedbackColor, true);
        }

        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void renderMembersTab(class_332 context, class_327 f, int cardX, int cardY, int cardW, int cardH) {
        boolean inRoom = PlayerBadgeManager.INSTANCE.isInRoom();
        if (inRoom) {
            String roomCode = PlayerBadgeManager.INSTANCE.getCurrentRoom();
            boolean isHost = PlayerBadgeManager.INSTANCE.isRoomHost();
            String hostName = isHost ? "Вы (Хост)" : PlayerBadgeManager.INSTANCE.getRoomHostName();
            String pwdLabel = PlayerBadgeManager.INSTANCE.roomHasPassword() ? " §6[🔒 С паролем]" : " §7[🔓 Без пароля]";

            context.method_51433(f, "§fКомната: §e§l" + roomCode + pwdLabel, cardX + 16, cardY + 62, -1, true);
            context.method_51433(f, "§7Организатор: §b" + hostName, cardX + 16, cardY + 76, 0xFFCCCCCC, false);
            String roleText = isHost ? "§6👑 Вы транслируете музыку" : "§b🎧 Синхронизация с хостом";
            int roleW = f.method_1727(roleText);
            context.method_51433(f, roleText, cardX + cardW - 16 - roleW, cardY + 76, -1, false);

            List<PlayerBadgeManager.RoomMember> members = PlayerBadgeManager.INSTANCE.getRoomMembers();
            int listY = cardY + 98;
            context.method_51433(f, "§aПодключенные слушатели (" + members.size() + "):", cardX + 16, listY, 0xFF00F5D4, false);

            int startItemY = cardY + 114;
            int maxItems = Math.min(members.size(), 5);
            for (int i = 0; i < maxItems; i++) {
                PlayerBadgeManager.RoomMember m = members.get(i);
                int itemY = startItemY + i * 20;
                int rowBg = (i % 2 == 0) ? 0x22FFFFFF : 0x11FFFFFF;
                MusicHudRenderer.renderRoundedRect(context, cardX + 16, itemY, cardW - 32, 18, 4, rowBg);
                MusicHudRenderer.renderRoundedBorder(context, cardX + 16, itemY, cardW - 32, 18, 4, 0x3300F5D4, 0x1100F5D4);
                String badge = m.isHost() ? "§6👑 Организатор" : "§b🎧 Слушатель";
                context.method_51433(f, badge, cardX + 22, itemY + 5, -1, false);
                context.method_51433(f, "§f" + m.name(), cardX + 115, itemY + 5, -1, false);
                int ping = m.pingMs() > 0 ? m.pingMs() : PlayerBadgeManager.INSTANCE.getRelayPingMs();
                String pingStr = "§7" + ping + " мс";
                int pingW = f.method_1727(pingStr);
                if (isHost && !m.isHost()) {
                    int pingX = cardX + cardW - 106 - pingW;
                    context.method_51433(f, pingStr, pingX, itemY + 5, 0xFFAAAAAA, false);
                } else {
                    String onlineStr = "§a● Онлайн";
                    int onlineW = f.method_1727(onlineStr);
                    int onlineX = cardX + cardW - 22 - onlineW;
                    int pingX = onlineX - 10 - pingW;
                    context.method_51433(f, pingStr, pingX, itemY + 5, 0xFFAAAAAA, false);
                    context.method_51433(f, onlineStr, onlineX, itemY + 5, 0xFF00F5D4, false);
                }
            }
            if (members.isEmpty()) {
                context.method_51433(f, "§8Ожидание подключения друзей... Код: §e" + roomCode, cardX + 22, startItemY + 5, 0xFF777788, false);
            }
        } else {
            int innerW = cardW - 32;
            int colGap = 8;
            int colW = (innerW - colGap) / 2;
            int colH = cardH - 62 - 34;

            int col1X = cardX + 16;
            int col1Y = cardY + 62;
            MusicHudRenderer.renderRoundedRect(context, col1X, col1Y, colW, colH, 6, 0x440E1424);
            MusicHudRenderer.renderRoundedBorder(context, col1X, col1Y, colW, colH, 6, 0x5500F5D4, 0x2200F5D4);
            context.method_51433(f, "§e✦ Создать сессию", col1X + 10, col1Y + 8, -1, true);
            context.method_51433(f, "§7Вы будете хостом", col1X + 10, col1Y + 20, 0xFF888899, false);
            context.method_51433(f, "§fПароль (опционально):", col1X + 10, col1Y + 34, -1, false);
            context.method_51433(f, "§8Слушатели будут", col1X + 10, col1Y + 74, 0xFF777788, false);
            context.method_51433(f, "§8слушать вашу музыку", col1X + 10, col1Y + 85, 0xFF777788, false);
            context.method_51433(f, "§8в реальном времени", col1X + 10, col1Y + 96, 0xFF777788, false);

            int col2X = col1X + colW + colGap;
            int col2Y = col1Y;
            MusicHudRenderer.renderRoundedRect(context, col2X, col2Y, colW, colH, 6, 0x440E1424);
            MusicHudRenderer.renderRoundedBorder(context, col2X, col2Y, colW, colH, 6, 0x5500F5D4, 0x2200F5D4);
            context.method_51433(f, "§b🔗 Подключиться", col2X + 10, col2Y + 8, -1, true);
            context.method_51433(f, "§7Синхронизация с другом", col2X + 10, col2Y + 20, 0xFF888899, false);
            context.method_51433(f, "§fКод друга:", col2X + 10, col2Y + 34, -1, false);
            context.method_51433(f, "§fПароль (если есть):", col2X + 10, col2Y + 70, -1, false);
        }
    }

    private void renderDiagTab(class_332 context, class_327 f, int cardX, int cardY, int cardW, int cardH) {
        String statusText = LocalMusicPlayer.getInstance().getSyncStatusText();
        int statusColor = LocalMusicPlayer.getInstance().getSyncStatusColor();
        long drift = LocalMusicPlayer.getInstance().getLastCalculatedDriftMs();
        int ping = PlayerBadgeManager.INSTANCE.getRelayPingMs();

        MusicHudRenderer.renderRoundedRect(context, cardX + 16, cardY + 62, cardW - 32, 22, 5, 0x33000000 | (statusColor & 0x00FFFFFF));
        MusicHudRenderer.renderRoundedBorder(context, cardX + 16, cardY + 62, cardW - 32, 22, 5, statusColor, statusColor);
        context.method_51433(f, "§fСтатус синхронизации: " + statusText, cardX + 24, cardY + 69, statusColor, true);

        int rowY = cardY + 90;
        int step = 13;

        TrackState track = PlayerBadgeManager.INSTANCE.getSyncedTrack();
        String trackName = (track != null && track.isActive()) ? (track.artist() + " - " + track.title()) : "Нет активного потока";
        context.method_51433(f, "§7Трек сессии: §f" + f.method_27523(trackName, cardW - 120), cardX + 16, rowY, -1, false);

        rowY += step;
        context.method_51433(f, "§7Источник аудио: §b" + LocalMusicPlayer.getInstance().getStreamSourceInfo(), cardX + 16, rowY, -1, false);

        rowY += step;
        String driftSign = drift > 0 ? "+" : "";
        context.method_51433(f, "§7Рассинхрон с хостом: §e" + driftSign + drift + " мс §8(порог авто-выравнивания 45 мс)", cardX + 16, rowY, -1, false);

        rowY += step;
        context.method_51433(f, "§7Задержка до Relay: §a" + ping + " мс §8(компенсируется в реальном времени)", cardX + 16, rowY, -1, false);

        rowY += step;
        context.method_51433(f, "§7Аудио-движок: §fJavaSound PCM Pipeline (VolumeAudioDevice)", cardX + 16, rowY, -1, false);

        rowY += step;
        context.method_51433(f, "§7Режим сглаживания: §3Acoustic Micro-Glide (Без щелчков/seek)", cardX + 16, rowY, -1, false);

        rowY += step;
        long[] cStats = this.getCacheStats();
        double mb = (double)cStats[1] / (1024.0 * 1024.0);
        context.method_51433(f, String.format("§7Кэш потоков: §f%d файлов (%.1f МБ)", cStats[0], mb), cardX + 16, rowY, -1, false);
    }

    private static class AmbientParticle {
        float x;
        float y;
        float vx;
        float vy;
        float alpha;
        int color;
        String symbol;

        AmbientParticle(float x, float y, float vx, float vy, float alpha, int color, String symbol) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.alpha = alpha;
            this.color = color;
            this.symbol = symbol;
        }

        void update(int w, int h, float dt60) {
            this.x += this.vx * dt60;
            this.y += this.vy * dt60;
            if (this.y < -10.0f) {
                this.y = h + 10;
            }
            if (this.x < -10.0f) {
                this.x = w + 10;
            }
            if (this.x > (float)(w + 10)) {
                this.x = -10.0f;
            }
        }
    }
}
