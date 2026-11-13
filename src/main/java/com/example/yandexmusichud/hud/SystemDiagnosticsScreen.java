package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.YandexMusicHudMod;
import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.helper.HelperProcessManager;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.lyrics.LyricLine;
import com.example.yandexmusichud.lyrics.LyricsManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_6880;

public class SystemDiagnosticsScreen
extends class_437 {
    private final class_437 parent;
    private final class_437 nextScreen;
    private final Random random = new Random();
    private final List<DiagnosticItem> items = new ArrayList<DiagnosticItem>();
    private final List<DiagParticle> particles = new ArrayList<DiagParticle>();
    private final List<DiagShockwave> shockwaves = new ArrayList<DiagShockwave>();
    private volatile boolean isScanning = false;
    private long lastScanTime = 0L;
    private int hoveredCardIndex = -1;
    private boolean isDetailedScan = false;
    private volatile String lastReport = "";
    private long reportCopiedToastTime = 0L;
    private double scrollOffset = 0.0;
    private double targetScrollOffset = 0.0;

    public SystemDiagnosticsScreen(class_437 parent, class_437 nextScreen) {
        super((class_2561)class_2561.method_43470((String)"Music Subtitles - \u0414\u0438\u0430\u0433\u043d\u043e\u0441\u0442\u0438\u043a\u0430 \u0441\u0438\u0441\u0442\u0435\u043c\u044b"));
        this.parent = parent;
        this.nextScreen = nextScreen;
    }

    protected void method_25426() {
        super.method_25426();
        this.playSound(class_3417.field_14703, 1.8f, 0.7f);
        this.playSound(class_3417.field_26980, 1.2f, 0.8f);
        for (int i = 0; i < 35; ++i) {
            this.spawnAmbientParticle(true);
        }
        this.runDiagnostics();
    }

    private void playSound(class_3414 sound, float pitch, float volume) {
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null) {
                if (mc.method_18854()) {
                    if (mc.method_1483() != null) {
                        mc.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)sound, (float)pitch, (float)volume));
                    }
                } else {
                    mc.execute(() -> {
                        try {
                            if (mc.method_1483() != null) {
                                mc.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)sound, (float)pitch, (float)volume));
                            }
                        } catch (Exception ignored) {}
                    });
                }
            }
        }
        catch (Exception exception) {

        }
    }

    private void playSound(class_6880<class_3414> sound, float pitch, float volume) {
        if (sound != null && sound.comp_349() != null) {
            this.playSound((class_3414)sound.comp_349(), pitch, volume);
        }
    }

    private void spawnAmbientParticle(boolean randomY) {
        String[] symbols = new String[]{"\u2726", "\u25c6", "\u25cf", "0", "1", "\u26a1", "\u2727"};
        String sym = symbols[this.random.nextInt(symbols.length)];
        float x = this.random.nextFloat() * (float)this.field_22789;
        float y = randomY ? this.random.nextFloat() * (float)this.field_22790 : (float)(this.field_22790 + 10);
        float vy = -0.3f - this.random.nextFloat() * 0.5f;
        float vx = (this.random.nextFloat() - 0.5f) * 0.4f;
        int[] colors = new int[]{58879, 65416, 8444159, 0xFFCC00, 12289788};
        int col = colors[this.random.nextInt(colors.length)];
        this.particles.add(new DiagParticle(x, y, vx, vy, col, sym, 90 + this.random.nextInt(70), 8.0f));
    }

    private void spawnBurstParticles(float originX, float originY, int color) {
        this.shockwaves.add(new DiagShockwave(originX, originY, 44.0f, color, 20));
        for (int i = 0; i < 35; ++i) {
            float angle = (float)((double)this.random.nextFloat() * Math.PI * 2.0);
            float speed = 1.2f + this.random.nextFloat() * 3.8f;
            float vx = (float)Math.cos(angle) * speed;
            float vy = (float)Math.sin(angle) * speed - 0.6f;
            this.particles.add(new DiagParticle(originX, originY, vx, vy, color, null, 25 + this.random.nextInt(20), 2.0f));
        }
    }

    public synchronized void runDiagnostics() {
        if (this.isScanning) {
            return;
        }
        this.isScanning = true;
        this.isDetailedScan = false;
        this.lastScanTime = System.currentTimeMillis();
        this.items.clear();
        DiagnosticItem osItem = new DiagnosticItem("\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u043e\u043d\u043d\u0430\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u0430 \u0438 \u043f\u043b\u0430\u0442\u0444\u043e\u0440\u043c\u0430");
        DiagnosticItem helperItem = new DiagnosticItem("\u0421\u043b\u0443\u0436\u0431\u0430 \u043c\u0435\u0434\u0438\u0430-\u0438\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u0438 (MusicHelper)");
        DiagnosticItem pulseSyncItem = new DiagnosticItem("PulseSync (\u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0430)");
        DiagnosticItem mediaItem = new DiagnosticItem("Медиа-сессия (GSMTC / MPRIS)");
        DiagnosticItem lyricsItem = new DiagnosticItem("Серверы синхронизации субтитров");
        DiagnosticItem twitchItem = new DiagnosticItem("Twitch Live и EventSub Gateway");
        this.items.add(osItem);
        this.items.add(helperItem);
        this.items.add(pulseSyncItem);
        this.items.add(mediaItem);
        this.items.add(lyricsItem);
        this.items.add(twitchItem);
        CompletableFuture.runAsync(() -> {
            try {
                TrackState state;
                boolean isWin11;
                String osName = System.getProperty("os.name", "Unknown");
                String osVersion = System.getProperty("os.version", "");
                String osArch = System.getProperty("os.arch", "");
                boolean isWindows = osName.toLowerCase(java.util.Locale.ROOT).contains("win");
                boolean isLinux = osName.toLowerCase(java.util.Locale.ROOT).contains("linux");
                boolean isMac = osName.toLowerCase(java.util.Locale.ROOT).contains("mac");
                boolean bl = isWin11 = isWindows && (osName.contains("11") || osVersion.startsWith("10.0") && SystemDiagnosticsScreen.parseBuild(osVersion) >= 22000);
                if (isWin11) {
                    osItem.status = Status.OK;
                    osItem.message = "Windows 11 (" + osArch + ") - полная поддержка системных медиа-сессий GSMTC.";
                    osItem.solution = "";
                } else if (isWindows) {
                    osItem.status = Status.OK;
                    osItem.message = osName + " (" + osArch + ") - поддерживается Windows Media Session.";
                    osItem.solution = "";
                } else if (isLinux) {
                    boolean isFlatpak = HelperProcessManager.isRunningInFlatpak();
                    String flatpakId = isFlatpak ? System.getenv("FLATPAK_ID") : null;
                    osItem.status = Status.OK;
                    if (isFlatpak) {
                        osItem.message = osName + " (" + osArch + ") [Flatpak: " + (flatpakId != null ? flatpakId : "Sandbox") + "] - поддержка MPRIS через flatpak-spawn.";
                    } else {
                        osItem.message = osName + " (" + osArch + ") - полная поддержка медиа через MPRIS D-Bus.";
                    }
                    osItem.solution = "";
                } else if (isMac) {
                    osItem.status = Status.OK;
                    osItem.message = osName + " (" + osArch + ") - встроенный мост macOS (Apple Music / Spotify / nowplaying-cli).";
                    osItem.solution = "";
                } else {
                    osItem.status = Status.WARNING;
                    osItem.message = osName + " (" + osArch + ") - стандартные медиа-сессии не настроены.";
                    osItem.solution = "Рекомендуется Windows или Linux с playerctl.";
                }
                Path configDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
                Path exePath = configDir.resolve("YandexMusicHelper.exe");
                boolean exeExists = Files.exists(exePath, new LinkOption[0]);
                boolean isHelperResponding = false;
                HttpURLConnection conn = null;
                try {
                    int port = HudConfig.INSTANCE.helperPort;
                    if (port <= 0) {
                        Path cfgPort = configDir.resolve("yandexmusichud_port.txt");
                        if (Files.exists(cfgPort, new LinkOption[0])) {
                            try {
                                port = Integer.parseInt(Files.readString(cfgPort).trim());
                            } catch (Exception ignored) {}
                        }
                    }
                    if (port <= 0) {
                        String tempEnv;
                        Path portFile = Path.of(System.getProperty("java.io.tmpdir"), new String[0]).resolve("yandexmusichud_port.txt");
                        if (!Files.exists(portFile, new LinkOption[0]) && (tempEnv = System.getenv("TEMP")) != null) {
                            portFile = Path.of(tempEnv, new String[0]).resolve("yandexmusichud_port.txt");
                        }
                        if (Files.exists(portFile, new LinkOption[0])) {
                            port = Integer.parseInt(Files.readString(portFile).trim());
                        }
                    }
                    if (port > 0) {
                        URL pingUrl = URI.create("http://127.0.0.1:" + port + "/now-playing").toURL();
                        conn = (HttpURLConnection)pingUrl.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(800);
                        conn.setReadTimeout(800);
                        int respCode = conn.getResponseCode();
                        isHelperResponding = respCode == 200;
                    }
                }
                catch (Exception ignored) {
                    isHelperResponding = false;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
                boolean isLinuxPlayerctlOk = isLinux && HelperProcessManager.INSTANCE != null && HelperProcessManager.INSTANCE.isLinuxPlayerctlAvailable();
                if (isLinux) {
                    boolean isFlatpak = HelperProcessManager.isRunningInFlatpak();
                    if (!isLinuxPlayerctlOk) {
                        helperItem.status = Status.WARNING;
                        helperItem.message = "Служба активна, но утилита playerctl не найдена в Linux.";
                        helperItem.solution = isFlatpak ? "Убедитесь, что на хосте установлен playerctl (sudo pacman -S playerctl / sudo apt install playerctl)." : "Установите playerctl в терминале: sudo pacman -S playerctl (или sudo apt install playerctl).";
                    } else if (isHelperResponding) {
                        helperItem.status = Status.OK;
                        helperItem.message = "Служба MPRIS активна и передаёт медиа-события.";
                        helperItem.solution = "";
                    } else {
                        helperItem.status = Status.WARNING;
                        helperItem.message = "Служба MPRIS не отвечает на запросы.";
                        helperItem.solution = "Нажмите «Перезапустить службу».";
                    }
                    helperItem.actionButtonText = "⚡ Перезапустить службу";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDiagnostics();
                    };
                } else if (isMac) {
                    helperItem.status = isHelperResponding ? Status.OK : Status.WARNING;
                    helperItem.message = isHelperResponding ? "Встроенный мост macOS активен и передаёт медиа-события." : "Служба macOS ожидает запуска плеера.";
                    helperItem.solution = "Запустите Spotify или Apple Music, либо установите nowplaying-cli (brew install nowplaying-cli).";
                    helperItem.actionButtonText = "⚡ Перезапустить службу";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDiagnostics();
                    };
                } else if (isHelperResponding) {
                    helperItem.status = Status.OK;
                    helperItem.message = "\u0424\u043e\u043d\u043e\u0432\u0430\u044f \u0441\u043b\u0443\u0436\u0431\u0430 \u0430\u043a\u0442\u0438\u0432\u043d\u0430 \u0438 \u043f\u0435\u0440\u0435\u0434\u0430\u0451\u0442 \u043c\u0435\u0434\u0438\u0430-\u0441\u043e\u0431\u044b\u0442\u0438\u044f \u0432 \u043c\u043e\u0434.";
                } else if (isWindows && exeExists) {
                    helperItem.status = Status.ERROR;
                    helperItem.message = "Файл службы найден, но фоновый процесс не отвечает на запросы.";
                    helperItem.solution = "Нажмите «Перезапустить службу». Если не помогает, проверьте исключения Защитника Windows для папки config/yandexmusichud.";
                    helperItem.actionButtonText = "⚡ Запустить службу";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDiagnostics();
                    };
                } else {
                    helperItem.status = Status.ERROR;
                    helperItem.message = "Файл службы не найден в папке конфигурации.";
                    helperItem.solution = "Файл будет автоматически извлечён из мода при перезапуске службы.";
                    helperItem.actionButtonText = "⚡ Восстановить и запустить";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDiagnostics();
                    };
                }
                boolean pulseSyncRunning = false;
                if (HudConfig.INSTANCE.pulseSyncIntegration) {
                    HttpURLConnection psConn = null;
                    try {
                        int psPort = HudConfig.INSTANCE.pulseSyncPort > 0 ? HudConfig.INSTANCE.pulseSyncPort : 2007;
                        URL psUrl = URI.create("http://127.0.0.1:" + psPort + "/get_track").toURL();
                        psConn = (HttpURLConnection)psUrl.openConnection();
                        psConn.setRequestMethod("GET");
                        psConn.setConnectTimeout(600);
                        psConn.setReadTimeout(600);
                        int psCode = psConn.getResponseCode();
                        pulseSyncRunning = psCode == 200;
                    } catch (Exception ignored) {
                    } finally {
                        if (psConn != null) {
                            psConn.disconnect();
                        }
                    }
                }
                if (pulseSyncRunning) {
                    pulseSyncItem.status = Status.OK;
                    pulseSyncItem.message = "\u0421\u0435\u0440\u0432\u0435\u0440 PulseSync \u0430\u043a\u0442\u0438\u0432\u0435\u043d (\u043f\u043e\u0440\u0442 " + HudConfig.INSTANCE.pulseSyncPort + "). \u041f\u0440\u044f\u043c\u0430\u044f \u043f\u0435\u0440\u0435\u0434\u0430\u0447\u0430 \u0442\u0430\u0439\u043c\u043a\u043e\u0434\u043e\u0432 \u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0438.";
                    pulseSyncItem.solution = "";
                } else if (!HudConfig.INSTANCE.pulseSyncIntegration) {
                    pulseSyncItem.status = Status.WARNING;
                    pulseSyncItem.message = "\u0418\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u044f PulseSync \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u0430 \u0432 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u0445 \u043c\u043e\u0434\u0430.";
                    pulseSyncItem.solution = "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u0435 PulseSync \u0432\u043e \u0432\u043a\u043b\u0430\u0434\u043a\u0435 \u00ab\u0418\u043d\u0444\u043e\u00bb.";
                } else {
                    pulseSyncItem.status = Status.OK;
                    pulseSyncItem.message = "PulseSync \u043d\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d (\u043f\u043e\u0440\u0442 " + HudConfig.INSTANCE.pulseSyncPort + " \u0441\u0432\u043e\u0431\u043e\u0434\u0435\u043d). \u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u043d\u0430\u044f \u0441\u0435\u0441\u0441\u0438\u044f.";
                    pulseSyncItem.solution = "\u0415\u0441\u043b\u0438 \u0432\u044b \u0441\u043b\u0443\u0448\u0430\u0435\u0442\u0435 \u042f\u043d\u0434\u0435\u043a\u0441 \u041c\u0443\u0437\u044b\u043a\u0443, \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 PulseSync \u0434\u043b\u044f \u0438\u0434\u0435\u0430\u043b\u044c\u043d\u043e\u0433\u043e \u0442\u0430\u0439\u043c\u0438\u043d\u0433\u0430.";
                }
                NowPlayingClient client = NowPlayingClient.INSTANCE;
                TrackState trackState = state = client != null ? client.getState() : TrackState.INACTIVE;
                if (state != null && state.isActive() && !state.title().isEmpty()) {
                    mediaItem.status = Status.OK;
                    mediaItem.message = "\u041f\u043b\u0435\u0435\u0440 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d [" + state.detectPlatformName() + "]: " + state.artist() + " - " + state.title();
                    mediaItem.solution = "";
                } else if (isLinux && !isLinuxPlayerctlOk) {
                    mediaItem.status = Status.WARNING;
                    mediaItem.message = "Медиа-сессия не подключена (отсутствует утилита playerctl).";
                    mediaItem.solution = "Выполните в терминале: sudo pacman -S playerctl (или sudo apt install playerctl) и запустите музыку.";
                    mediaItem.actionButtonText = "🔄 Проверить";
                    mediaItem.actionButtonAction = this::runDiagnostics;
                } else if (isHelperResponding) {
                    boolean spotifyRunning = TrackState.isSpotifyRunning();
                    mediaItem.status = Status.OK;
                    mediaItem.message = spotifyRunning ? "Spotify \u0437\u0430\u043f\u0443\u0449\u0435\u043d \u0438 \u0433\u043e\u0442\u043e\u0432 \u043a \u0440\u0430\u0431\u043e\u0442\u0435. \u0412\u043a\u043b\u044e\u0447\u0438\u0442\u0435 \u043b\u044e\u0431\u043e\u0439 \u0442\u0440\u0435\u043a." : "\u0421\u043b\u0443\u0436\u0431\u0430 \u0430\u043a\u0442\u0438\u0432\u043d\u0430 \u0438 \u043e\u0436\u0438\u0434\u0430\u0435\u0442 \u0432\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u044f \u0442\u0440\u0435\u043a\u0430 (Spotify, \u043f\u043b\u0435\u0435\u0440 \u0438\u043b\u0438 \u0431\u0440\u0430\u0443\u0437\u0435\u0440).";
                    mediaItem.solution = "";
                    mediaItem.actionButtonText = "\ud83d\udd04 \u041f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c";
                    mediaItem.actionButtonAction = this::runDiagnostics;
                } else {
                    mediaItem.status = Status.WARNING;
                    mediaItem.message = "\u041c\u0435\u0434\u0438\u0430-\u0441\u0435\u0441\u0441\u0438\u044f \u043d\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0430 (\u0442\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044f \u0440\u0430\u0431\u043e\u0442\u0430\u044e\u0449\u0430\u044f \u0441\u043b\u0443\u0436\u0431\u0430 Helper).";
                    mediaItem.solution = "\u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u0435 \u0441\u043b\u0443\u0436\u0431\u0443 Helper \u0432\u044b\u0448\u0435.";
                }
                boolean lrclibOk = SystemDiagnosticsScreen.pingUrl("https://lrclib.net/api/search?q=test", 2500);
                boolean yandexOk = SystemDiagnosticsScreen.pingUrl("https://api.music.yandex.net/search?text=test&type=track&page=0", 2500);
                if (lrclibOk || yandexOk) {
                    lyricsItem.status = Status.OK;
                    lyricsItem.message = "\u0421\u0432\u044f\u0437\u044c \u0441 \u0431\u0430\u0437\u043e\u0439 \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0445 \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432 (LrcLib) \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442 \u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u043e.";
                    lyricsItem.solution = "";
                } else {
                    lyricsItem.status = Status.WARNING;
                    lyricsItem.message = "\u0421\u0435\u0440\u0432\u0435\u0440\u044b \u0442\u0435\u043a\u0441\u0442\u043e\u0432 \u043d\u0435 \u043e\u0442\u0432\u0435\u0447\u0430\u044e\u0442 \u0437\u0430 \u043e\u0442\u0432\u0435\u0434\u0451\u043d\u043d\u043e\u0435 \u0432\u0440\u0435\u043c\u044f.";
                    lyricsItem.solution = "\u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0443 \u0438\u043b\u0438 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0431\u0440\u0430\u043d\u0434\u043c\u0430\u0443\u044d\u0440\u0430 Windows.";
                    lyricsItem.actionButtonText = "\ud83d\udd04 \u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u044c";
                    lyricsItem.actionButtonAction = this::runDiagnostics;
                }
                if (!com.example.yandexmusichud.config.HudConfig.INSTANCE.twitchEnabled) {
                    twitchItem.status = Status.WARNING;
                    twitchItem.message = "Twitch \u0438\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u044f \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u0430 \u0432 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u0445.";
                    twitchItem.solution = "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u0435 Twitch \u0432 \u043c\u0435\u043d\u044e \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a (\u043a\u043d\u043e\u043f\u043a\u0430 \u00a7d\u25C8).";
                } else if (com.example.yandexmusichud.config.HudConfig.INSTANCE.twitchChannel == null || com.example.yandexmusichud.config.HudConfig.INSTANCE.twitchChannel.trim().isEmpty()) {
                    twitchItem.status = Status.WARNING;
                    twitchItem.message = "\u041a\u0430\u043d\u0430\u043b Twitch \u043d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d.";
                    twitchItem.solution = "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f \u043a\u0430\u043d\u0430\u043b\u0430 \u0432 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u0445 Twitch.";
                } else {
                    com.example.yandexmusichud.twitch.TwitchManager tw = com.example.yandexmusichud.twitch.TwitchManager.getInstance();
                    if (tw.getStatus() == com.example.yandexmusichud.twitch.TwitchManager.Status.CONNECTED) {
                        twitchItem.status = Status.OK;
                        twitchItem.message = "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e \u043a \u043a\u0430\u043d\u0430\u043b\u0443 [#" + tw.getCurrentJoinedChannel() + "]. \u041f\u0438\u043d\u0433: " + tw.getPingLatencyMs() + "\u043c\u0441. \u0410\u043b\u0435\u0440\u0442\u044b \u0430\u043a\u0442\u0438\u0432\u043d\u044b.";
                        twitchItem.solution = "";
                    } else if (tw.getStatus() == com.example.yandexmusichud.twitch.TwitchManager.Status.CONNECTING) {
                        twitchItem.status = Status.WARNING;
                        twitchItem.message = "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f \u0441 Twitch IRC...";
                        twitchItem.solution = "";
                    } else {
                        twitchItem.status = Status.ERROR;
                        twitchItem.message = "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f: " + tw.getLastErrorMessage();
                        twitchItem.solution = "\u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u043f\u0440\u0430\u0432\u0438\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u0438\u043c\u0435\u043d\u0438 \u043a\u0430\u043d\u0430\u043b\u0430 \u0438 \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0441\u0435\u0442\u0438.";
                        twitchItem.actionButtonText = "\u21bb \u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u044c";
                        twitchItem.actionButtonAction = this::runDiagnostics;
                    }
                }
                this.lastReport = this.buildDiagnosticReport();
            }
            catch (Exception hasErr) {
            }
            finally {
                this.isScanning = false;
                boolean hasErr = this.items.stream().anyMatch(it -> it.status == Status.ERROR);
                if (!hasErr) {
                    this.playSound(class_3417.field_14709, 1.8f, 0.7f);
                    this.playSound((class_6880<class_3414>)class_3417.field_14725, 1.4f, 0.8f);
                } else {
                    this.playSound((class_6880<class_3414>)class_3417.field_14624, 0.8f, 0.8f);
                }
            }
        });
    }

    public synchronized void runDetailedDiagnostics() {
        if (this.isScanning) {
            return;
        }
        this.isScanning = true;
        this.isDetailedScan = true;
        this.lastScanTime = System.currentTimeMillis();
        this.items.clear();
        DiagnosticItem osItem = new DiagnosticItem("\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u043e\u043d\u043d\u0430\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u0430 \u0438 JVM");
        DiagnosticItem helperItem = new DiagnosticItem("\u0421\u043b\u0443\u0436\u0431\u0430 \u043c\u0435\u0434\u0438\u0430-\u0438\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0438\u0438 (MusicHelper)");
        DiagnosticItem mediaItem = new DiagnosticItem("\u041c\u0435\u0434\u0438\u0430-\u0441\u0435\u0441\u0441\u0438\u044f \u0438 \u0430\u0443\u0434\u0438\u043e\u043f\u043b\u0435\u0435\u0440");
        DiagnosticItem lyricsItem = new DiagnosticItem("\u041f\u0440\u043e\u0432\u0430\u0439\u0434\u0435\u0440\u044b \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432 \u0438 \u0437\u0430\u0434\u0435\u0440\u0436\u043a\u0430");
        DiagnosticItem cacheItem = new DiagnosticItem("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u043a\u044d\u0448 \u0438 \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0430\u0446\u0438\u044f");
        DiagnosticItem modsItem = new DiagnosticItem("\u0421\u043e\u0432\u043c\u0435\u0441\u0442\u0438\u043c\u043e\u0441\u0442\u044c Minecraft \u0438 \u043c\u043e\u0434\u043e\u0432");
        DiagnosticItem streamItem = new DiagnosticItem("\u0421\u0442\u0440\u0438\u043c\u0438\u043d\u0433 (Twitch / DonationAlerts)");
        this.items.add(osItem);
        this.items.add(helperItem);
        this.items.add(mediaItem);
        this.items.add(lyricsItem);
        this.items.add(cacheItem);
        this.items.add(modsItem);
        this.items.add(streamItem);
        CompletableFuture.runAsync(() -> {
            try {
                String osName = System.getProperty("os.name", "Unknown");
                String osVersion = System.getProperty("os.version", "");
                String osArch = System.getProperty("os.arch", "");
                String osLower = osName.toLowerCase(java.util.Locale.ROOT);
                boolean isWindows = osLower.contains("win");
                boolean isLinux = osLower.contains("linux");
                boolean isMac = osLower.contains("mac");
                int winBuild = SystemDiagnosticsScreen.parseBuild(osVersion);
                boolean isWin11 = isWindows && (osName.contains("11") || osVersion.startsWith("10.0") && winBuild >= 22000);
                Runtime rt = Runtime.getRuntime();
                long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
                long maxMb = rt.maxMemory() / 1048576L;
                int cores = rt.availableProcessors();
                String jvmVer = System.getProperty("java.version", "Unknown");
                String jvmVendor = System.getProperty("java.vendor", "Unknown");
                StringBuilder osLog = new StringBuilder();
                osLog.append("OS: ").append(osName).append(" v").append(osVersion).append(" (Build: ").append(winBuild).append(") [").append(osArch).append("]\n");
                osLog.append("JVM: ").append(jvmVer).append(" (").append(jvmVendor).append(") - ").append(System.getProperty("java.vm.name")).append("\n");
                osLog.append("RAM: ").append(usedMb).append(" MB / ").append(maxMb).append(" MB, CPU: ").append(cores).append(" cores");
                osItem.detailedLog = osLog.toString();
                if (isWin11) {
                    osItem.status = Status.OK;
                    osItem.message = "Windows 11 (Build " + winBuild + ") - полная поддержка GSMTC.";
                    osItem.solution = "";
                } else if (isWindows) {
                    osItem.status = Status.OK;
                    osItem.message = osName + " - поддерживается Windows Media Session.";
                    osItem.solution = "";
                } else if (isLinux) {
                    boolean isFlatpak = HelperProcessManager.isRunningInFlatpak();
                    String flatpakId = isFlatpak ? System.getenv("FLATPAK_ID") : null;
                    osItem.status = Status.OK;
                    if (isFlatpak) {
                        osItem.message = osName + " [Flatpak: " + (flatpakId != null ? flatpakId : "Sandbox") + "] - поддержка MPRIS через flatpak-spawn.";
                    } else {
                        osItem.message = osName + " - полная поддержка медиа через MPRIS D-Bus.";
                    }
                    osItem.solution = "";
                } else if (isMac) {
                    osItem.status = Status.OK;
                    osItem.message = osName + " - встроенный мост macOS (Apple Music / Spotify / nowplaying-cli).";
                    osItem.solution = "";
                } else {
                    osItem.status = Status.WARNING;
                    osItem.message = osName + " (" + osArch + ") - стандартные медиа-сессии не настроены.";
                    osItem.solution = "Рекомендуется Windows или Linux с playerctl.";
                }
                Path configDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
                Path exePath = configDir.resolve("YandexMusicHelper.exe");
                boolean exeExists = Files.exists(exePath, new LinkOption[0]);
                long exeSize = 0L;
                try {
                    if (exeExists) {
                        exeSize = Files.size(exePath);
                    }
                }
                catch (Exception ignored) {
                }
                int port = HudConfig.INSTANCE.helperPort;
                String portSource = "config";
                if (port <= 0) {
                    Path cfgPort = configDir.resolve("yandexmusichud_port.txt");
                    if (Files.exists(cfgPort, new LinkOption[0])) {
                        try {
                            port = Integer.parseInt(Files.readString(cfgPort).trim());
                            portSource = "config_dir (yandexmusichud_port.txt)";
                        } catch (Exception ignored) {}
                    }
                }
                if (port <= 0) {
                    String tempEnv;
                    Path portFile = Path.of(System.getProperty("java.io.tmpdir"), new String[0]).resolve("yandexmusichud_port.txt");
                    if (!Files.exists(portFile, new LinkOption[0]) && (tempEnv = System.getenv("TEMP")) != null) {
                        portFile = Path.of(tempEnv, new String[0]).resolve("yandexmusichud_port.txt");
                    }
                    if (Files.exists(portFile, new LinkOption[0])) {
                        try {
                            port = Integer.parseInt(Files.readString(portFile).trim());
                            portSource = "temp_file (" + portFile.getFileName() + ")";
                        }
                        catch (Exception ignored) {
                        }
                    }
                }
                long npPing = port > 0 ? SystemDiagnosticsScreen.pingUrlTimed("http://127.0.0.1:" + port + "/now-playing", 1000) : -1L;
                long msPing = port > 0 ? SystemDiagnosticsScreen.pingUrlTimed("http://127.0.0.1:" + port + "/cover", 1000) : -1L;
                boolean helperOk = npPing >= 0L;
                boolean isLinuxPlayerctlOk = isLinux && HelperProcessManager.INSTANCE != null && HelperProcessManager.INSTANCE.isLinuxPlayerctlAvailable();
                List<String> linuxPlayers = isLinux && HelperProcessManager.INSTANCE != null ? HelperProcessManager.INSTANCE.getLinuxActivePlayers() : Collections.emptyList();
                StringBuilder helperLog = new StringBuilder();
                if (isLinux) {
                    helperLog.append("Helper: Встроенный Linux MPRIS мост (D-Bus)\n");
                    helperLog.append("Port: ").append(port).append(" [").append(portSource).append("]\n");
                    helperLog.append("Ping /now-playing: ").append(npPing >= 0L ? npPing + " ms" : "TIMEOUT/FAIL").append("\n");
                    helperLog.append("Ping /cover: ").append(msPing >= 0L ? msPing + " ms" : "TIMEOUT/FAIL").append("\n");
                    helperLog.append("playerctl: ").append(isLinuxPlayerctlOk ? "Установлен и доступен" : "НЕ НАЙДЕН (команда: sudo pacman -S playerctl)").append("\n");
                    helperLog.append("MPRIS плееры: ").append(linuxPlayers.isEmpty() ? "Не обнаружены (включите музыку в плеере или браузере)" : String.join(", ", linuxPlayers));
                } else if (isMac) {
                    helperLog.append("Helper: Встроенный macOS мост\n");
                    helperLog.append("Port: ").append(port).append(" [").append(portSource).append("]\n");
                    helperLog.append("Ping /now-playing: ").append(npPing >= 0L ? npPing + " ms" : "TIMEOUT/FAIL").append("\n");
                    helperLog.append("Ping /cover: ").append(msPing >= 0L ? msPing + " ms" : "TIMEOUT/FAIL");
                } else {
                    helperLog.append("Helper Path: ").append(exePath).append("\n");
                    helperLog.append("File exists: ").append(exeExists).append(" (size: ").append(exeSize).append(" bytes)\n");
                    helperLog.append("Port: ").append(port).append(" [").append(portSource).append("]\n");
                    helperLog.append("Ping /now-playing: ").append(npPing >= 0L ? npPing + " ms" : "TIMEOUT/FAIL").append("\n");
                    helperLog.append("Ping /cover: ").append(msPing >= 0L ? msPing + " ms" : "TIMEOUT/FAIL");
                }
                helperItem.detailedLog = helperLog.toString();
                if (isLinux) {
                    boolean isFlatpak = HelperProcessManager.isRunningInFlatpak();
                    if (!isLinuxPlayerctlOk) {
                        helperItem.status = Status.WARNING;
                        helperItem.message = "Служба активна, но утилита playerctl не найдена в Linux.";
                        helperItem.solution = isFlatpak ? "Убедитесь, что на хосте установлен playerctl (sudo apt install playerctl / sudo pacman -S playerctl)." : "Установите playerctl в терминале: sudo pacman -S playerctl (или sudo apt install playerctl).";
                    } else if (helperOk) {
                        helperItem.status = Status.OK;
                        helperItem.message = "Служба активна (порт " + port + ", пинг " + npPing + "мс). MPRIS готов.";
                        helperItem.solution = "";
                    } else {
                        helperItem.status = Status.WARNING;
                        helperItem.message = "Служба MPRIS не отвечает на запросы.";
                        helperItem.solution = "Нажмите «Перезапустить».";
                    }
                    helperItem.actionButtonText = "⚡ Перезапустить";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDetailedDiagnostics();
                    };
                } else if (helperOk) {
                    helperItem.status = Status.OK;
                    helperItem.message = "Служба активна (порт " + port + ", пинг " + npPing + "мс). IPC стабилен.";
                    helperItem.solution = "";
                } else if (isMac) {
                    helperItem.status = Status.WARNING;
                    helperItem.message = "Служба macOS ожидает запуска плеера.";
                    helperItem.solution = "Запустите Spotify или Apple Music, либо установите nowplaying-cli (brew install nowplaying-cli).";
                    helperItem.actionButtonText = "⚡ Перезапустить";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDetailedDiagnostics();
                    };
                } else if (isWindows && exeExists) {
                    helperItem.status = Status.ERROR;
                    helperItem.message = "Файл службы найден, но фоновый процесс не отвечает на запросы.";
                    helperItem.solution = "Нажмите «Перезапустить службу». Проверьте исключения Защитника Windows.";
                    helperItem.actionButtonText = "⚡ Запустить службу";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDetailedDiagnostics();
                    };
                } else {
                    helperItem.status = Status.ERROR;
                    helperItem.message = "Файл службы не найден в папке конфигурации.";
                    helperItem.solution = "Файл будет автоматически извлечён из мода при перезапуске службы.";
                    helperItem.actionButtonText = "⚡ Восстановить";
                    helperItem.actionButtonAction = () -> {
                        if (YandexMusicHudMod.INSTANCE != null) {
                            YandexMusicHudMod.INSTANCE.restartHelper();
                        }
                        this.runDetailedDiagnostics();
                    };
                }
                NowPlayingClient client = NowPlayingClient.INSTANCE;
                TrackState state = client != null ? client.getState() : TrackState.INACTIVE;
                boolean isMediaActive = state != null && state.isActive() && !state.title().isEmpty();
                boolean spotifyRunning = TrackState.isSpotifyRunning();
                LyricsManager lm = YandexMusicHudMod.INSTANCE != null ? YandexMusicHudMod.INSTANCE.getLyricsManager() : null;
                List<LyricLine> currentLines = (lm != null && state != null && isMediaActive) ? lm.getLyricsForTrack(state) : null;
                boolean hasCurrentLyrics = currentLines != null && !currentLines.isEmpty();
                StringBuilder mediaLog = new StringBuilder();
                if (state != null) {
                    mediaLog.append("Track Active: ").append(state.isActive()).append("\n");
                    mediaLog.append("Title: ").append(state.title()).append("\n");
                    mediaLog.append("Artist: ").append(state.artist()).append("\n");
                    mediaLog.append("Platform: ").append(state.detectPlatformName()).append(" (raw: ").append(state.platform()).append(")\n");
                    mediaLog.append("Position: ").append(state.positionMs()).append(" ms / Duration: ").append(state.durationMs()).append(" ms\n");
                    mediaLog.append("Speed: ").append(state.playbackSpeed()).append(", Paused: ").append(state.isPaused()).append("\n");
                    mediaLog.append("Subtitles Enabled: ").append(HudConfig.INSTANCE.subtitlesEnabled).append(", Mode: ").append(HudConfig.INSTANCE.subtitlesMode).append("\n");
                    mediaLog.append("Track Lyrics: ").append(hasCurrentLyrics ? "Загружен (" + currentLines.size() + " строк)" : "НЕ НАЙДЕН (нет на серверах / тайм-аут)").append("\n");
                    if (isLinux && !isLinuxPlayerctlOk) {
                        mediaLog.append("Linux MPRIS Status: Ожидает установки playerctl (sudo pacman -S playerctl)\n");
                    }
                } else {
                    mediaLog.append("TrackState: null\n");
                }
                mediaLog.append("Spotify Running: ").append(spotifyRunning);
                mediaItem.detailedLog = mediaLog.toString();
                if (isMediaActive) {
                    mediaItem.status = hasCurrentLyrics ? Status.OK : Status.WARNING;
                    mediaItem.message = "Плеер [" + state.detectPlatformName() + "]: " + state.artist() + " - " + state.title() + (hasCurrentLyrics ? " [Текст готов: " + currentLines.size() + " строк]" : " [Внимание: текст песни ещё не найден!]");
                    mediaItem.solution = hasCurrentLyrics ? "" : "Если серверы недоступны, положите файл «" + state.title() + ".lrc» в папку config/yandexmusichud/lyrics/";
                } else if (isLinux && !isLinuxPlayerctlOk) {
                    mediaItem.status = Status.WARNING;
                    mediaItem.message = "Медиа-сессия не подключена (отсутствует утилита playerctl).";
                    mediaItem.solution = "Выполните в терминале: sudo pacman -S playerctl (или sudo apt install playerctl) и запустите музыку.";
                    mediaItem.actionButtonText = "🔄 Проверить";
                    mediaItem.actionButtonAction = this::runDetailedDiagnostics;
                } else if (helperOk) {
                    mediaItem.status = Status.OK;
                    mediaItem.message = spotifyRunning ? "Spotify запущен. Включите любой трек для синхронизации." : "Служба ожидает трек (браузер, Spotify или плеер).";
                    mediaItem.solution = "";
                    mediaItem.actionButtonText = "🔄 Проверить";
                    mediaItem.actionButtonAction = this::runDetailedDiagnostics;
                } else {
                    mediaItem.status = Status.WARNING;
                    mediaItem.message = "Медиа-сессия не подключена (требуется работающая служба Helper).";
                    mediaItem.solution = "Запустите службу Helper выше.";
                }
                long lrclibPing = SystemDiagnosticsScreen.pingUrlTimed("https://lrclib.net/api/search?q=coldplay", 4000);
                long yandexPing = SystemDiagnosticsScreen.pingUrlTimed("https://api.music.yandex.net/search?text=test&type=track&page=0", 3500);
                long geniusPing = SystemDiagnosticsScreen.pingUrlTimed("https://dumb.bloat.cat", 3500);
                if (geniusPing < 0L) {
                    geniusPing = SystemDiagnosticsScreen.pingUrlTimed("https://genius.fsky.io", 3500);
                }
                if (geniusPing < 0L) {
                    geniusPing = SystemDiagnosticsScreen.pingUrlTimed("https://api.genius.com/search?q=test", 3500);
                }
                long neteasePing = SystemDiagnosticsScreen.pingUrlTimed("https://music.163.com", 3500);
                StringBuilder lyricsLog = new StringBuilder();
                lyricsLog.append("LRCLIB API: ").append(lrclibPing >= 0L ? lrclibPing + " ms (Доступен)" : "TIMEOUT / Ошибка").append("\n");
                lyricsLog.append("Yandex Music API: ").append(yandexPing >= 0L ? yandexPing + " ms (Доступен)" : "TIMEOUT / Ошибка").append("\n");
                lyricsLog.append("Genius Lyrics: ").append(geniusPing >= 0L ? geniusPing + " ms (Доступен)" : "TIMEOUT / Ошибка").append("\n");
                lyricsLog.append("NetEase 163: ").append(neteasePing >= 0L ? neteasePing + " ms (Доступен)" : "TIMEOUT / Ошибка").append("\n");
                lyricsLog.append("Active Track Lyrics: ").append(hasCurrentLyrics ? "Успешно загружен (" + currentLines.size() + " строк)" : "Ожидает загрузки / не найден");
                lyricsItem.detailedLog = lyricsLog.toString();
                if (hasCurrentLyrics || lrclibPing >= 0L) {
                    lyricsItem.status = Status.OK;
                    lyricsItem.message = "Серверы субтитров доступны (LRCLIB: " + (lrclibPing >= 0L ? lrclibPing + "мс" : "—") + ", Yandex: " + (yandexPing >= 0L ? yandexPing + "мс" : "—") + ").";
                    lyricsItem.solution = "";
                } else {
                    lyricsItem.status = Status.WARNING;
                    lyricsItem.message = "Сервер синхронизированных текстов LRCLIB недоступен (тайм-аут).";
                    lyricsItem.solution = "Положите .lrc файл в config/yandexmusichud/lyrics/ или проверьте подключение к lrclib.net.";
                    lyricsItem.actionButtonText = "🔄 Повторить";
                    lyricsItem.actionButtonAction = this::runDetailedDiagnostics;
                }
                Path cacheDir = configDir.resolve("lyrics");
                if (!Files.exists(cacheDir, new LinkOption[0])) {
                    cacheDir = configDir.resolve("lyrics_cache");
                }
                int cacheFiles = 0;
                long cacheBytes = 0L;
                if (Files.exists(cacheDir, new LinkOption[0])) {
                    try (java.util.stream.Stream<Path> stream = Files.list(cacheDir)) {
                        List<Path> files = stream.toList();
                        cacheFiles = files.size();
                        for (Path p : files) {
                            try {
                                cacheBytes += Files.size(p);
                            }
                            catch (Exception ignored) {
                            }
                        }
                    }
                    catch (Exception ignored) {
                    }
                }
                StringBuilder cacheLog = new StringBuilder();
                cacheLog.append("Cache directory: ").append(cacheDir).append("\n");
                cacheLog.append("Cached tracks count: ").append(cacheFiles).append("\n");
                cacheLog.append("Total cache size: ").append(cacheBytes / 1024L).append(" KB (").append(cacheBytes).append(" bytes)\n");
                cacheLog.append("Tempo Clock: StudioCascadeAligner & Adaptive PLL Sync Ready\n");
                cacheLog.append("Word Timestamps: Syllable Cadence & Elastic Time Warping Active");
                cacheItem.detailedLog = cacheLog.toString();
                cacheItem.status = Status.OK;
                cacheItem.message = "Кэшировано треков: " + cacheFiles + " (" + cacheBytes / 1024L + " КБ). Движок каданса и PLL активен.";
                cacheItem.solution = "";
                FabricLoader fl = FabricLoader.getInstance();
                String flVer = fl.getModContainer("fabricloader").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("Unknown");
                String mcVer = fl.getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("1.21.11");
                boolean hasVoiceChat = fl.isModLoaded("voicechat");
                boolean hasIris = fl.isModLoaded("iris") || fl.isModLoaded("oculus");
                boolean hasSodium = fl.isModLoaded("sodium") || fl.isModLoaded("embeddium");
                boolean hasModMenu = fl.isModLoaded("modmenu");
                StringBuilder modsLog = new StringBuilder();
                modsLog.append("Minecraft Version: ").append(mcVer).append("\n");
                modsLog.append("Fabric Loader: ").append(flVer).append("\n");
                modsLog.append("Simple Voice Chat: ").append(hasVoiceChat ? "Обнаружен (Smart Voice Ducking активен)" : "Не установлен").append("\n");
                modsLog.append("Iris / Oculus Shaders: ").append(hasIris ? "Обнаружен (Шейдеры совместимы)" : "Не установлен").append("\n");
                modsLog.append("Sodium / Embeddium: ").append(hasSodium ? "Обнаружен (Оптимизация рендера)" : "Не установлен").append("\n");
                modsLog.append("ModMenu: ").append(hasModMenu ? "Обнаружен" : "Не установлен");
                modsItem.detailedLog = modsLog.toString();
                modsItem.status = Status.OK;
                modsItem.message = "Minecraft " + mcVer + " (Fabric " + flVer + ")" + (hasVoiceChat ? " | VoiceChat ✓" : "") + (hasIris ? " | Iris ✓" : "");
                modsItem.solution = "";
                boolean twEnabled = HudConfig.INSTANCE.twitchEnabled;
                String twChan = HudConfig.INSTANCE.twitchChannel != null ? HudConfig.INSTANCE.twitchChannel.trim() : "";
                com.example.yandexmusichud.twitch.TwitchManager tw = com.example.yandexmusichud.twitch.TwitchManager.getInstance();
                boolean daEnabled = HudConfig.INSTANCE.daEnabled;
                String daToken = HudConfig.INSTANCE.daToken;
                boolean daConfigured = daToken != null && !daToken.trim().isEmpty();
                com.example.yandexmusichud.donationalerts.DonationAlertsManager da = com.example.yandexmusichud.donationalerts.DonationAlertsManager.getInstance();
                StringBuilder streamLog = new StringBuilder();
                streamLog.append("Twitch Integration: ").append(twEnabled ? "Включена (канал: #" + twChan + ")" : "Отключена в настройках").append("\n");
                if (twEnabled) {
                    streamLog.append("Twitch Status: ").append(tw.getStatus()).append(", Latency: ").append(tw.getPingLatencyMs()).append(" ms\n");
                }
                String daExtra = "";
                if (daConfigured) {
                    if (da.getStatus() == com.example.yandexmusichud.donationalerts.DonationAlertsManager.Status.CONNECTED) {
                        daExtra = ", стример: " + da.getStreamerUsername();
                    } else if (!da.getLastErrorMessage().isEmpty()) {
                        daExtra = ", инфо: " + da.getLastErrorMessage();
                    }
                }
                streamLog.append("DonationAlerts: ").append(daEnabled ? (daConfigured ? "Включена (токен настроен, статус: " + da.getStatus() + daExtra + ")" : "Включена (токен НЕ указан)") : "Отключена в настройках");
                streamItem.detailedLog = streamLog.toString();
                if (!twEnabled && !daEnabled) {
                    streamItem.status = Status.OK;
                    streamItem.message = "Стриминговые модули отключены в настройках.";
                    streamItem.solution = "";
                } else if (twEnabled && twChan.isEmpty()) {
                    streamItem.status = Status.WARNING;
                    streamItem.message = "Twitch включен, но имя канала не задано.";
                    streamItem.solution = "Укажите канал Twitch в настройках мода.";
                } else if (twEnabled && tw.getStatus() == com.example.yandexmusichud.twitch.TwitchManager.Status.CONNECTED) {
                    String daDesc = daConfigured ? (da.getStatus() == com.example.yandexmusichud.donationalerts.DonationAlertsManager.Status.CONNECTED ? "✓ (" + da.getStreamerUsername() + ")" : da.getStatus().toString()) : "откл.";
                    streamItem.status = Status.OK;
                    streamItem.message = "Twitch: [#" + twChan + "] пинг " + tw.getPingLatencyMs() + "мс. DA: " + daDesc;
                    streamItem.solution = "";
                } else if (twEnabled) {
                    streamItem.status = Status.WARNING;
                    streamItem.message = "Twitch: " + tw.getStatus();
                    streamItem.solution = tw.getLastErrorMessage().isEmpty() ? "Ожидание соединения..." : tw.getLastErrorMessage();
                } else {
                    streamItem.status = Status.OK;
                    streamItem.message = "DonationAlerts: " + (daConfigured ? (da.getStatus() == com.example.yandexmusichud.donationalerts.DonationAlertsManager.Status.CONNECTED ? "подключен (" + da.getStreamerUsername() + ")" : da.getStatus().toString()) : "ожидает токен");
                    streamItem.solution = da.getLastErrorMessage();
                }
                this.lastReport = this.buildDiagnosticReport();
                try {
                    Files.createDirectories(configDir);
                    Path reportPath = configDir.resolve("diagnostic_report.txt");
                    Files.writeString(reportPath, this.lastReport);
                    class_310 mc = class_310.method_1551();
                    if (mc != null) {
                        String reportCopy = this.lastReport;
                        mc.execute(() -> {
                            if (mc.field_1774 != null) {
                                mc.field_1774.method_1455(reportCopy);
                            }
                        });
                    }
                    this.reportCopiedToastTime = System.currentTimeMillis();
                }
                catch (Exception ignored) {
                }
            }
            catch (Exception ignored) {
            }
            finally {
                this.isScanning = false;
                boolean hasErr = this.items.stream().anyMatch(it -> it.status == Status.ERROR);
                if (!hasErr) {
                    this.playSound(class_3417.field_14709, 1.8f, 0.7f);
                    this.playSound((class_6880<class_3414>)class_3417.field_14725, 1.4f, 0.8f);
                } else {
                    this.playSound((class_6880<class_3414>)class_3417.field_14624, 0.8f, 0.8f);
                }
            }
        });
    }

    public int getTotalCardsHeight() {
        int h = 0;
        int cardGap = 8;
        for (DiagnosticItem item : this.items) {
            int cardH = item.solution.isEmpty() && item.actionButtonText.isEmpty() ? 44 : 64;
            h += cardH + cardGap;
        }
        if (h > 0) {
            h -= cardGap;
        }
        return h;
    }

    public String buildDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("         MUSIC SUBTITLES v2.8 \u2014 \u0414\u0415\u0422\u0410\u041b\u042c\u041d\u042b\u0419 \u0414\u0418\u0410\u0413\u041d\u041e\u0421\u0422\u0418\u0427\u0415\u0421\u041a\u0418\u0419 \u041e\u0422\u0427\u0401\u0422\n");
        sb.append("================================================================================\n");
        sb.append("\u0414\u0430\u0442\u0430 \u0438 \u0432\u0440\u0435\u043c\u044f: ").append(java.time.LocalDateTime.now().toString()).append("\n");
        sb.append("\u0422\u0438\u043f \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0438: ").append(this.isDetailedScan ? "\u0413\u043b\u0443\u0431\u043e\u043a\u0430\u044f \u0434\u0435\u0442\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 (Deep Scan)" : "\u0411\u044b\u0441\u0442\u0440\u0430\u044f \u044d\u043a\u0441\u043f\u0440\u0435\u0441\u0441-\u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430").append("\n\n");
        for (int i = 0; i < this.items.size(); ++i) {
            DiagnosticItem item = this.items.get(i);
            sb.append("[").append(i + 1).append("] ").append(item.title).append("\n");
            sb.append("    \u0421\u0442\u0430\u0442\u0443\u0441:  ").append(item.status.name()).append("\n");
            sb.append("    \u0421\u0432\u043e\u0434\u043a\u0430:  ").append(item.message).append("\n");
            if (!item.solution.isEmpty()) {
                sb.append("    \u0420\u0435\u0448\u0435\u043d\u0438\u0435: ").append(item.solution).append("\n");
            }
            if (!item.detailedLog.isEmpty()) {
                sb.append("    \u0414\u0435\u0442\u0430\u043b\u0438:\n");
                for (String line : LINE_SPLIT_PATTERN.split(item.detailedLog)) {
                    sb.append("      ").append(line).append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("\u0421\u0418\u0421\u0422\u0415\u041c\u041d\u041e\u0415 \u041e\u041a\u0420\u0423\u0416\u0415\u041d\u0418\u0415:\n");
        sb.append("  OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(" (").append(System.getProperty("os.arch")).append(")\n");
        sb.append("  Java: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\n");
        sb.append("  JVM: ").append(System.getProperty("java.vm.name")).append("\n");
        Runtime rt = Runtime.getRuntime();
        long freeMb = rt.freeMemory() / 1048576L;
        long totalMb = rt.totalMemory() / 1048576L;
        long maxMb = rt.maxMemory() / 1048576L;
        sb.append("  RAM: \u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u043e ").append(totalMb - freeMb).append(" MB / \u0412\u044b\u0434\u0435\u043b\u0435\u043d\u043e ").append(totalMb).append(" MB / \u041c\u0430\u043a\u0441 ").append(maxMb).append(" MB\n");
        sb.append("  CPU \u044f\u0434\u0435\u0440: ").append(rt.availableProcessors()).append("\n");
        sb.append("================================================================================\n");
        sb.append("\u0418\u041d\u0421\u0422\u0420\u0423\u041a\u0426\u0418\u042f \u041f\u041e \u0423\u0421\u0422\u0420\u0410\u041d\u0415\u041d\u0418\u042e \u041d\u0415\u041f\u041e\u041b\u0410\u0414\u041e\u041a:\n");
        sb.append("  1. \u0421\u043a\u043e\u043f\u0438\u0440\u0443\u0439\u0442\u0435 \u044d\u0442\u043e\u0442 \u0442\u0435\u043a\u0441\u0442 \u0438 \u043e\u0442\u043f\u0440\u0430\u0432\u044c\u0442\u0435 \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u0443 / \u043d\u0435\u0439\u0440\u043e\u0441\u0435\u0442\u0438 \u0434\u043b\u044f \u0430\u043d\u0430\u043b\u0438\u0437\u0430.\n");
        sb.append("  2. \u0415\u0441\u043b\u0438 MusicHelper \u043d\u0435 \u043e\u0442\u0432\u0435\u0447\u0430\u0435\u0442: \u043f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0417\u0430\u0449\u0438\u0442\u043d\u0438\u043a Windows / \u0430\u043d\u0442\u0438\u0432\u0438\u0440\u0443\u0441.\n");
        sb.append("  3. \u0415\u0441\u043b\u0438 \u0442\u0440\u0435\u043a\u0438 \u043d\u0435 \u0441\u0447\u0438\u0442\u044b\u0432\u0430\u044e\u0442\u0441\u044f: \u0443\u0431\u0435\u0434\u0438\u0442\u0435\u0441\u044c, \u0447\u0442\u043e \u0431\u0440\u0430\u0443\u0437\u0435\u0440 \u0438\u043b\u0438 \u043f\u043b\u0435\u0435\u0440 \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u044b \u0438 \u0432\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u044f\u0442 \u0437\u0432\u0443\u043a.\n");
        sb.append("================================================================================\n");
        return sb.toString();
    }

    public void copyReportToClipboard() {
        if (this.lastReport == null || this.lastReport.isEmpty()) {
            this.lastReport = this.buildDiagnosticReport();
        }
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null && mc.field_1774 != null) {
                mc.field_1774.method_1455(this.lastReport);
            }
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
            if (!Files.exists(configDir, new LinkOption[0])) {
                Files.createDirectories(configDir);
            }
            Path reportPath = configDir.resolve("diagnostic_report.txt");
            Files.writeString(reportPath, this.lastReport);
            this.reportCopiedToastTime = System.currentTimeMillis();
            this.playSound((class_6880<class_3414>)class_3417.field_15015, 1.4f, 0.8f);
        }
        catch (Exception ignored) {
        }
    }

    private static final java.util.regex.Pattern DOT_SPLIT_PATTERN = java.util.regex.Pattern.compile("\\.");
    private static final java.util.regex.Pattern NON_DIGIT_PATTERN = java.util.regex.Pattern.compile("[^0-9]");
    private static final java.util.regex.Pattern WIN_BUILD_PATTERN = java.util.regex.Pattern.compile("10\\.0\\.(\\d+)");
    private static final java.util.regex.Pattern LINE_SPLIT_PATTERN = java.util.regex.Pattern.compile("\\n");
    private static volatile int cachedWinBuild = -1;

    private static int parseBuild(String osVersion) {
        try {
            String[] parts = DOT_SPLIT_PATTERN.split(osVersion);
            if (parts.length >= 3) {
                int b = Integer.parseInt(NON_DIGIT_PATTERN.matcher(parts[2]).replaceAll(""));
                if (b > 0) {
                    return b;
                }
            }
        }
        catch (Exception exception) {
        }
        if (cachedWinBuild >= 0) {
            return cachedWinBuild;
        }
        String osName = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (osName.contains("win")) {
            Process p = null;
            try {
                p = new ProcessBuilder("cmd.exe", "/c", "ver").redirectErrorStream(true).start();
                boolean finished = p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                if (finished) {
                    String out = new String(p.getInputStream().readAllBytes());
                    java.util.regex.Matcher m = WIN_BUILD_PATTERN.matcher(out);
                    if (m.find()) {
                        cachedWinBuild = Integer.parseInt(m.group(1));
                        return cachedWinBuild;
                    }
                }
            }
            catch (Throwable ignored) {
            } finally {
                if (p != null) {
                    p.destroyForcibly();
                }
            }
        }
        cachedWinBuild = 0;
        return 0;
    }

    private static boolean pingUrl(String urlStr, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL u = URI.create(urlStr).toURL();
            conn = (HttpURLConnection)u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            int code = conn.getResponseCode();
            return (code >= 200 && code < 400) || code == 401;
        }
        catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static long pingUrlTimed(String urlStr, int timeoutMs) {
        long t0 = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL u = URI.create(urlStr).toURL();
            conn = (HttpURLConnection)u.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            int code = conn.getResponseCode();
            long elapsed = System.currentTimeMillis() - t0;
            if ((code >= 200 && code < 400) || code == 401) {
                return Math.max(1L, elapsed);
            }
            return -1L;
        }
        catch (Exception e) {
            return -1L;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void renderParticles(class_332 context) {
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            DiagParticle p = this.particles.get(i);
            p.update();
            if (p.isDead()) {
                this.particles.remove(i);
                continue;
            }
            int alphaInt = (int)(p.getAlpha() * 255.0f);
            if (alphaInt <= 2) continue;
            int col = alphaInt << 24 | p.color & 0xFFFFFF;
            if (p.symbol != null) {
                context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, col, false);
                continue;
            }
            context.method_25294((int)p.x, (int)p.y, (int)(p.x + p.size), (int)(p.y + p.size), col);
        }
    }

    private void renderShockwaves(class_332 context) {
        for (int i = this.shockwaves.size() - 1; i >= 0; --i) {
            DiagShockwave s = this.shockwaves.get(i);
            s.update();
            if (s.isDead()) {
                this.shockwaves.remove(i);
                continue;
            }
            int alphaInt = (int)(s.getAlpha() * 200.0f);
            if (alphaInt <= 2) continue;
            int col = alphaInt << 24 | s.color & 0xFFFFFF;
            int r = (int)s.radius;
            int cx = (int)s.cx;
            int cy = (int)s.cy;
            context.method_25294(cx - r, cy - 1, cx + r, cy + 1, col);
            context.method_25294(cx - 1, cy - r, cx + 1, cy + r, col);
            int diag = (int)((float)r * 0.707f);
            context.method_25294(cx - diag, cy - diag, cx - diag + 2, cy - diag + 2, col);
            context.method_25294(cx + diag, cy - diag, cx + diag + 2, cy - diag + 2, col);
            context.method_25294(cx - diag, cy + diag, cx - diag + 2, cy + diag + 2, col);
            context.method_25294(cx + diag, cy + diag, cx + diag + 2, cy + diag + 2, col);
        }
    }

    private void renderHeartbeatWaveform(class_332 context, int centerX, int y) {
        int pointCount = 28;
        int barW = 3;
        int barGap = 2;
        int totalW = pointCount * (barW + barGap) - barGap;
        int startX = centerX - totalW / 2;
        long nowMs = System.currentTimeMillis();
        for (int i = 0; i < pointCount; ++i) {
            float wave = (float)Math.sin((double)nowMs * 0.007 + (double)i * 0.38);
            float pulsePeak = i >= 12 && i <= 16 ? (float)Math.sin((double)nowMs * 0.012 + (double)i) * 1.6f : 0.0f;
            float norm = Math.abs(wave + pulsePeak) * 0.5f;
            int barH = (int)(2.0f + norm * 8.0f);
            int col = i >= 12 && i <= 16 ? -587137144 : -1442781697;
            int bx = startX + i * (barW + barGap);
            context.method_25294(bx, y - barH / 2, bx + barW, y + barH / 2 + 1, col);
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int summaryCol;
        String statusSummary;
        int cardHeight;
        long nowMs = System.currentTimeMillis();
        class_327 tr = this.field_22793;
        int cx = this.field_22789 / 2;
        context.method_25296(0, 0, this.field_22789, this.field_22790, -100070892, -50068215);
        context.method_25296(0, 0, 160, 110, 352380415, 0);
        context.method_25296(this.field_22789 - 160, 0, this.field_22789, 110, 352386952, 0);
        if (this.random.nextFloat() < 0.28f) {
            this.spawnAmbientParticle(false);
        }
        this.renderParticles(context);
        this.renderShockwaves(context);
        int headerW = Math.min(540, this.field_22789 - 16);
        MusicHudRenderer.renderRoundedRect(context, cx - headerW / 2, 7, headerW, 44, 10, 402712063);
        MusicHudRenderer.renderRoundedBorder(context, cx - headerW / 2, 7, headerW, 44, 10, 855696895);
        String title = "\u2726 \u0426\u0415\u041d\u0422\u0420 \u0414\u0418\u0410\u0413\u041d\u041e\u0421\u0422\u0418\u041a\u0418 \u0418 \u0421\u041e\u0412\u041c\u0415\u0421\u0422\u0418\u041c\u041e\u0421\u0422\u0418 WINDOWS 11 / 10 \u2726";
        int titleW = tr.method_1727(title);
        context.method_51433(tr, title, cx - titleW / 2, 12, -16718337, false);
        String subtitle = "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u0430\u043d\u0430\u043b\u0438\u0437 \u043e\u043a\u0440\u0443\u0436\u0435\u043d\u0438\u044f, \u043c\u0435\u0434\u0438\u0430-\u0441\u0435\u0441\u0441\u0438\u0439 \u0438 \u0441\u0435\u0440\u0432\u0438\u0441\u0430 \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432";
        int subW = tr.method_1727(subtitle);
        context.method_51433(tr, subtitle, cx - subW / 2, 24, -5592406, false);
        this.renderHeartbeatWaveform(context, cx, 39);
        int cardWidth = Math.min(520, this.field_22789 - 40);
        int startX = cx - cardWidth / 2;
        int cardGap = 8;
        int bottomY = this.field_22790 - 32;
        int viewportTop = 54;
        int viewportBottom = bottomY - 24;
        int viewportH = viewportBottom - viewportTop;
        int totalCardsH = this.getTotalCardsHeight();
        double maxScroll = Math.max(0.0, (double)(totalCardsH - viewportH + 4));
        this.targetScrollOffset = Math.max(0.0, Math.min(maxScroll, this.targetScrollOffset));
        this.scrollOffset = (double)class_3532.method_16439(0.25f, (float)this.scrollOffset, (float)this.targetScrollOffset);

        if (totalCardsH > 0) {
            float scanProgress = (float)((double)(nowMs % 3200L) / 3200.0);
            int scanY = (int)((float)viewportTop + scanProgress * (float)Math.min(viewportH, totalCardsH));
            context.method_25296(startX, Math.max(viewportTop, scanY - 14), startX + cardWidth, scanY, 58879, 805365247);
            context.method_25294(startX, scanY - 1, startX + cardWidth, scanY + 1, -872349697);
            context.method_25296(startX, scanY + 1, startX + cardWidth, Math.min(viewportBottom, scanY + 15), 805365247, 58879);
        }

        context.method_44379(startX - 6, viewportTop, startX + cardWidth + 12, viewportBottom);
        int curY = viewportTop + 2 - (int)this.scrollOffset;
        this.hoveredCardIndex = -1;
        for (int i = 0; i < this.items.size(); ++i) {
            DiagnosticItem item = this.items.get(i);
            cardHeight = item.solution.isEmpty() && item.actionButtonText.isEmpty() ? 44 : 64;
            if (curY + cardHeight >= viewportTop - 10 && curY <= viewportBottom + 10) {
                boolean isHovered = mouseX >= startX && mouseX <= startX + cardWidth && mouseY >= curY && mouseY <= curY + cardHeight && mouseY >= viewportTop && mouseY <= viewportBottom;
                if (isHovered) {
                    this.hoveredCardIndex = i;
                }
                int bg = switch (item.status.ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 1 -> isHovered ? -1441584354 : -2012143080;
                    case 2 -> isHovered ? -1439159020 : -2010504689;
                    case 3 -> isHovered ? -1438509541 : -2009919212;
                    case 0 -> isHovered ? -1440997328 : -2011883484;
                };
                int border = switch (item.status.ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 1 -> -16718218;
                    case 2 -> -10929;
                    case 3 -> -44462;
                    case 0 -> -12285185;
                };
                MusicHudRenderer.renderRoundedRect(context, startX, curY, cardWidth, cardHeight, 6, bg);
                int borderCol = isHovered ? 0xCC000000 | border & 0xFFFFFF : 0x55000000 | border & 0xFFFFFF;
                MusicHudRenderer.renderRoundedBorder(context, startX, curY, cardWidth, cardHeight, 6, borderCol, borderCol);
                if (isHovered) {
                    MusicHudRenderer.renderRoundedRect(context, startX + 1, curY + 1, cardWidth - 2, cardHeight - 2, 5, 302048767);
                }
                String badge;
                if (item.status == Status.CHECKING) {
                    String[] SPINNER = new String[]{"\u280b", "\u2819", "\u2839", "\u2838", "\u283c", "\u2834", "\u2826", "\u2827", "\u2807", "\u280f"};
                    String spin = SPINNER[(int)(nowMs / 80L % (long)SPINNER.length)];
                    badge = spin + " \u041f\u0420\u041e\u0412\u0415\u0420\u041a\u0410...";
                } else {
                    badge = item.status == Status.OK ? "\u2713 \u0413\u041e\u0422\u041e\u0412\u041e" : (item.status == Status.WARNING ? "\u26a0 \u0412\u041d\u0418\u041c\u0410\u041d\u0418\u0415" : "\u2715 \u041e\u0428\u0418\u0411\u041a\u0410");
                }
                int badgeBg = switch (item.status.ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 1 -> 855697014;
                    case 2 -> 872404303;
                    case 3 -> 872370770;
                    case 0 -> 860130047;
                };
                MusicHudRenderer.renderRoundedRect(context, startX + 10, curY + 6, 82, 14, 4, badgeBg);
                MusicHudRenderer.renderRoundedBorder(context, startX + 10, curY + 6, 82, 14, 4, 0x66000000 | border & 0xFFFFFF);
                context.method_51433(tr, badge, startX + 14, curY + 9, border, false);
                context.method_51433(tr, item.title, startX + 98, curY + 9, -1, false);
                String msg = item.message;
                if (tr.method_1727(msg) > cardWidth - 24) {
                    msg = tr.method_27523(msg, cardWidth - 28) + "...";
                }
                context.method_51433(tr, msg, startX + 12, curY + 23, -3355444, false);
                if (!item.solution.isEmpty()) {
                    String prefix = item.status == Status.OK ? "\u2139 " : "\ud83d\udca1 \u0420\u0435\u0448\u0435\u043d\u0438\u0435: ";
                    String sol = prefix + item.solution;
                    int maxSolW = cardWidth - 24 - (item.actionButtonText.isEmpty() ? 0 : 130);
                    if (tr.method_1727(sol) > maxSolW) {
                        sol = tr.method_27523(sol, maxSolW - 8) + "...";
                    }
                    context.method_51433(tr, sol, startX + 12, curY + 39, item.status == Status.OK ? -8271996 : -8062, false);
                }
                if (!item.actionButtonText.isEmpty() && item.actionButtonAction != null) {
                    int btnW = 124;
                    int btnH = 18;
                    int btnX = startX + cardWidth - btnW - 8;
                    int btnY = curY + cardHeight - btnH - 6;
                    boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH && mouseY >= viewportTop && mouseY <= viewportBottom;
                    int btnBg = btnHovered ? -16731905 : -16745729;
                    MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 4, btnBg);
                    MusicHudRenderer.renderRoundedBorder(context, btnX, btnY, btnW, btnH, 4, btnHovered ? -1 : 0x66FFFFFF, btnHovered ? -1 : 0x44FFFFFF);
                    int btnTextW = tr.method_1727(item.actionButtonText);
                    context.method_51433(tr, item.actionButtonText, btnX + (btnW - btnTextW) / 2, btnY + 5, -1, true);
                }
            }
            curY += cardHeight + cardGap;
        }
        context.method_44380();

        if (maxScroll > 0.0) {
            int sbTrackH = viewportH;
            int sbX = startX + cardWidth + 4;
            int sbW = 4;
            float scrollFraction = (float)(this.scrollOffset / maxScroll);
            int thumbH = Math.max(16, (int)((float)sbTrackH * ((float)viewportH / (float)totalCardsH)));
            int thumbY = viewportTop + (int)((float)(sbTrackH - thumbH) * scrollFraction);
            MusicHudRenderer.renderRoundedRect(context, sbX, viewportTop, sbW, sbTrackH, 2, 0x33000000);
            MusicHudRenderer.renderRoundedRect(context, sbX, thumbY, sbW, thumbH, 2, 0x9900D8FF);
        }

        float barScale = Math.min(1.0f, (float)(this.field_22789 - 16) / 504.0f);
        int b1W = (int)(92.0f * barScale);
        int b2W = (int)(144.0f * barScale);
        int b3W = (int)(76.0f * barScale);
        int b4W = (int)(76.0f * barScale);
        int b5W = (int)(92.0f * barScale);
        int gap = (int)(6.0f * barScale);
        int totalBarW = b1W + gap + b2W + gap + b3W + gap + b4W + gap + b5W;
        int barStartX = cx - totalBarW / 2;
        int b1X = barStartX;
        int b2X = b1X + b1W + gap;
        int b3X = b2X + b2W + gap;
        int b4X = b3X + b3W + gap;
        int b5X = b4X + b4W + gap;
        int btnH = 22;

        boolean b1Hover = mouseX >= b1X && mouseX <= b1X + b1W && mouseY >= bottomY && mouseY <= bottomY + btnH;
        MusicHudRenderer.renderRoundedRect(context, b1X, bottomY, b1W, btnH, 5, b1Hover ? -14013124 : -14934486);
        MusicHudRenderer.renderRoundedBorder(context, b1X, bottomY, b1W, btnH, 5, b1Hover ? -16718337 : 0x44FFFFFF, b1Hover ? -16718337 : 0x22FFFFFF);
        context.method_51433(tr, "\ud83d\udd04 \u042d\u043a\u0441\u043f\u0440\u0435\u0441\u0441", b1X + (b1W - tr.method_1727("\ud83d\udd04 \u042d\u043a\u0441\u043f\u0440\u0435\u0441\u0441")) / 2, bottomY + 7, b1Hover ? -16718337 : -2039584, true);

        boolean b2Hover = mouseX >= b2X && mouseX <= b2X + b2W && mouseY >= bottomY && mouseY <= bottomY + btnH;
        int b2Glow = (int)((Math.sin((double)nowMs * 0.005) + 1.0) * 0.5 * 60.0);
        int b2Bg = b2Hover ? 0xFF00E5FF : (0xFF001e28 | (b2Glow << 8));
        int b2Border = b2Hover ? -1 : (-16711681 | (b2Glow << 16));
        MusicHudRenderer.renderRoundedRect(context, b2X, bottomY, b2W, btnH, 5, b2Bg);
        MusicHudRenderer.renderRoundedBorder(context, b2X, bottomY, b2W, btnH, 5, b2Border, b2Border);
        String b2Text = b2W > 120 ? "\u26a1 \u0414\u0435\u0442\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430" : "\u26a1 \u0414\u0435\u0442\u0430\u043b\u044c\u043d\u0430\u044f";
        context.method_51433(tr, b2Text, b2X + (b2W - tr.method_1727(b2Text)) / 2, bottomY + 7, b2Hover ? -16777216 : -1, !b2Hover);

        boolean b3Hover = mouseX >= b3X && mouseX <= b3X + b3W && mouseY >= bottomY && mouseY <= bottomY + btnH;
        MusicHudRenderer.renderRoundedRect(context, b3X, bottomY, b3W, btnH, 5, b3Hover ? -14013124 : -14934486);
        MusicHudRenderer.renderRoundedBorder(context, b3X, bottomY, b3W, btnH, 5, b3Hover ? -8333057 : 0x44FFFFFF, b3Hover ? -8333057 : 0x22FFFFFF);
        context.method_51433(tr, "\ud83d\udccb \u041e\u0442\u0447\u0451\u0442", b3X + (b3W - tr.method_1727("\ud83d\udccb \u041e\u0442\u0447\u0451\u0442")) / 2, bottomY + 7, b3Hover ? -8333057 : -2039584, true);

        boolean b4Hover = mouseX >= b4X && mouseX <= b4X + b4W && mouseY >= bottomY && mouseY <= bottomY + btnH;
        MusicHudRenderer.renderRoundedRect(context, b4X, bottomY, b4W, btnH, 5, b4Hover ? -14013124 : -14934486);
        MusicHudRenderer.renderRoundedBorder(context, b4X, bottomY, b4W, btnH, 5, b4Hover ? -8333057 : 0x44FFFFFF, b4Hover ? -8333057 : 0x22FFFFFF);
        context.method_51433(tr, "\ud83d\udcc1 \u041f\u0430\u043f\u043a\u0430", b4X + (b4W - tr.method_1727("\ud83d\udcc1 \u041f\u0430\u043f\u043a\u0430")) / 2, bottomY + 7, b4Hover ? -8333057 : -2039584, true);

        boolean b5Hover = mouseX >= b5X && mouseX <= b5X + b5W && mouseY >= bottomY && mouseY <= bottomY + btnH;
        int primaryCol = b5Hover ? -16718218 : -16725933;
        MusicHudRenderer.renderRoundedRect(context, b5X, bottomY, b5W, btnH, 5, primaryCol);
        MusicHudRenderer.renderRoundedBorder(context, b5X, bottomY, b5W, btnH, 5, b5Hover ? -1 : -2013200504, b5Hover ? -1 : -2013200504);
        int arrowShift = b5Hover ? (int)(Math.sin((double)nowMs * 0.01) * 2.0) : 0;
        String b5Text = "\u0414\u0430\u043b\u0435\u0435 \u2794";
        context.method_51433(tr, b5Text, b5X + (b5W - tr.method_1727(b5Text)) / 2 + arrowShift, bottomY + 7, -16777216, false);

        boolean hasError = this.items.stream().anyMatch(it -> it.status == Status.ERROR);
        boolean hasWarn = this.items.stream().anyMatch(it -> it.status == Status.WARNING);
        if (this.isScanning) {
            statusSummary = this.isDetailedScan ? "\u0418\u0434\u0451\u0442 \u0433\u043b\u0443\u0431\u043e\u043a\u0430\u044f \u0434\u0435\u0442\u0430\u043b\u044c\u043d\u0430\u044f \u0434\u0438\u0430\u0433\u043d\u043e\u0441\u0442\u0438\u043a\u0430..." : "\u0418\u0434\u0451\u0442 \u0441\u043a\u0430\u043d\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0441\u0438\u0441\u0442\u0435\u043c\u043d\u044b\u0445 \u0441\u0435\u0440\u0432\u0438\u0441\u043e\u0432 Windows...";
            summaryCol = -8333057;
        } else if (hasError) {
            statusSummary = "\u041e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u044b \u043e\u0448\u0438\u0431\u043a\u0438. \u0412\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u0435 \u0440\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u043e\u0432\u0430\u043d\u043d\u044b\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u0432\u044b\u0448\u0435.";
            summaryCol = -44462;
        } else if (hasWarn) {
            statusSummary = "\u0421\u0438\u0441\u0442\u0435\u043c\u0430 \u0433\u043e\u0442\u043e\u0432\u0430 \u043a \u0440\u0430\u0431\u043e\u0442\u0435. \u0412\u043a\u043b\u044e\u0447\u0438\u0442\u0435 \u043f\u043b\u0435\u0435\u0440 \u0434\u043b\u044f \u043e\u0442\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f \u0442\u0440\u0435\u043a\u043e\u0432.";
            summaryCol = -10929;
        } else {
            statusSummary = "\u2713 \u0412\u0441\u0435 \u0441\u043b\u0443\u0436\u0431\u044b \u0440\u0430\u0431\u043e\u0442\u0430\u044e\u0442 \u0438\u0434\u0435\u0430\u043b\u044c\u043d\u043e. " + (this.isDetailedScan ? "(\u0414\u0435\u0442\u0430\u043b\u044c\u043d\u044b\u0439 \u043e\u0442\u0447\u0451\u0442 \u0433\u043e\u0442\u043e\u0432)" : "");
            summaryCol = -9834322;
        }
        context.method_51433(tr, statusSummary, cx - tr.method_1727(statusSummary) / 2, bottomY - 14, summaryCol, false);

        long toastElapsed = nowMs - this.reportCopiedToastTime;
        if (toastElapsed < 3500L) {
            float toastAlpha = toastElapsed < 2800L ? 1.0f : 1.0f - (float)(toastElapsed - 2800L) / 700.0f;
            int toastA = (int)(toastAlpha * 255.0f);
            if (toastA > 4) {
                String toastMsg = "\u2713 \u041e\u0442\u0447\u0451\u0442 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d \u0432 \u0431\u0443\u0444\u0435\u0440 \u043e\u0431\u043c\u0435\u043d\u0430 \u0438 \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d \u0432 diagnostic_report.txt!";
                int tmW = tr.method_1727(toastMsg);
                int tBg = (toastA * 220 / 255) << 24 | 0x001a14;
                int tBorder = toastA << 24 | 0x00FF88;
                int tText = toastA << 24 | 0x88FFCC;
                MusicHudRenderer.renderRoundedRect(context, cx - tmW / 2 - 12, bottomY - 38, tmW + 24, 20, 5, tBg);
                MusicHudRenderer.renderRoundedBorder(context, cx - tmW / 2 - 12, bottomY - 38, tmW + 24, 20, 5, tBorder);
                context.method_51433(tr, toastMsg, cx - tmW / 2, bottomY - 32, tText, false);
            }
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int bottomY = this.field_22790 - 32;
        int viewportTop = 54;
        int viewportBottom = bottomY - 24;
        int viewportH = viewportBottom - viewportTop;
        int totalCardsH = this.getTotalCardsHeight();
        double maxScroll = Math.max(0.0, (double)(totalCardsH - viewportH + 4));
        if (maxScroll > 0.0) {
            this.targetScrollOffset = Math.max(0.0, Math.min(maxScroll, this.targetScrollOffset - verticalAmount * 28.0));
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int cx = this.field_22789 / 2;
        int cardWidth = Math.min(520, this.field_22789 - 40);
        int startX = cx - cardWidth / 2;
        int cardGap = 8;
        int bottomY = this.field_22790 - 32;
        int viewportTop = 54;
        int viewportBottom = bottomY - 24;
        int curY = viewportTop + 2 - (int)this.scrollOffset;
        for (DiagnosticItem item : this.items) {
            int cardHeight = item.solution.isEmpty() && item.actionButtonText.isEmpty() ? 44 : 64;
            if (!item.actionButtonText.isEmpty() && item.actionButtonAction != null && mouseY >= (double)viewportTop && mouseY <= (double)viewportBottom) {
                int btnW = 124;
                int btnH = 18;
                int btnX = startX + cardWidth - btnW - 8;
                int btnY = curY + cardHeight - btnH - 6;
                if (mouseX >= (double)btnX && mouseX <= (double)(btnX + btnW) && mouseY >= (double)btnY && mouseY <= (double)(btnY + btnH)) {
                    this.playSound((class_6880<class_3414>)class_3417.field_15015, 1.0f, 0.8f);
                    this.spawnBurstParticles((float)btnX + (float)btnW / 2.0f, (float)btnY + (float)btnH / 2.0f, 45311);
                    item.actionButtonAction.run();
                    return true;
                }
            }
            curY += cardHeight + cardGap;
        }
        float barScale = Math.min(1.0f, (float)(this.field_22789 - 16) / 504.0f);
        int b1W = (int)(92.0f * barScale);
        int b2W = (int)(144.0f * barScale);
        int b3W = (int)(76.0f * barScale);
        int b4W = (int)(76.0f * barScale);
        int b5W = (int)(92.0f * barScale);
        int gap = (int)(6.0f * barScale);
        int totalBarW = b1W + gap + b2W + gap + b3W + gap + b4W + gap + b5W;
        int barStartX = cx - totalBarW / 2;
        int b1X = barStartX;
        int b2X = b1X + b1W + gap;
        int b3X = b2X + b2W + gap;
        int b4X = b3X + b3W + gap;
        int b5X = b4X + b4W + gap;
        int btnH = 22;
        if (mouseX >= (double)b1X && mouseX <= (double)(b1X + b1W) && mouseY >= (double)bottomY && mouseY <= (double)(bottomY + btnH)) {
            this.playSound((class_6880<class_3414>)class_3417.field_15015, 1.0f, 0.8f);
            this.spawnBurstParticles((float)b1X + (float)b1W / 2.0f, (float)bottomY + (float)btnH / 2.0f, 58879);
            this.runDiagnostics();
            return true;
        }
        if (mouseX >= (double)b2X && mouseX <= (double)(b2X + b2W) && mouseY >= (double)bottomY && mouseY <= (double)(bottomY + btnH)) {
            this.playSound(class_3417.field_14703, 1.5f, 0.9f);
            this.spawnBurstParticles((float)b2X + (float)b2W / 2.0f, (float)bottomY + (float)btnH / 2.0f, 0x00FFFF);
            this.runDetailedDiagnostics();
            return true;
        }
        if (mouseX >= (double)b3X && mouseX <= (double)(b3X + b3W) && mouseY >= (double)bottomY && mouseY <= (double)(bottomY + btnH)) {
            this.playSound((class_6880<class_3414>)class_3417.field_15015, 1.2f, 0.8f);
            this.spawnBurstParticles((float)b3X + (float)b3W / 2.0f, (float)bottomY + (float)btnH / 2.0f, 0x88FF88);
            this.copyReportToClipboard();
            return true;
        }
        if (mouseX >= (double)b4X && mouseX <= (double)(b4X + b4W) && mouseY >= (double)bottomY && mouseY <= (double)(bottomY + btnH)) {
            this.playSound((class_6880<class_3414>)class_3417.field_15015, 1.0f, 0.8f);
            this.spawnBurstParticles((float)b4X + (float)b4W / 2.0f, (float)bottomY + (float)btnH / 2.0f, 8444159);
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud");
            com.example.yandexmusichud.favorites.FavoritesManager.openFolderSafely(dir);
            return true;
        }
        if (mouseX >= (double)b5X && mouseX <= (double)(b5X + b5W) && mouseY >= (double)bottomY && mouseY <= (double)(bottomY + btnH)) {
            this.playSound(class_3417.field_14627, 1.2f, 0.8f);
            this.spawnBurstParticles((float)b5X + (float)b5W / 2.0f, (float)bottomY + (float)btnH / 2.0f, 58998);
            this.proceedNext();
            return true;
        }
        return super.method_25402(click, bl);
    }

    private void proceedNext() {
        if (this.field_22787 != null) {
            if (this.nextScreen != null) {
                this.field_22787.method_1507(this.nextScreen);
            } else {
                this.field_22787.method_1507(this.parent);
            }
        }
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256 || input.comp_4795() == 257 || input.comp_4795() == 32) {
            this.proceedNext();
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25421() {
        return true;
    }

    private static class DiagParticle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        String symbol;
        int maxAge;
        int age = 0;
        float size;

        DiagParticle(float x, float y, float vx, float vy, int color, String symbol, int maxAge, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.symbol = symbol;
            this.maxAge = maxAge;
            this.size = size;
        }

        void update() {
            this.x += this.vx;
            this.y += this.vy;
            ++this.age;
        }

        boolean isDead() {
            return this.age >= this.maxAge;
        }

        float getAlpha() {
            float progress = (float)this.age / (float)this.maxAge;
            if (progress < 0.2f) {
                return progress / 0.2f;
            }
            return Math.max(0.0f, 1.0f - (progress - 0.2f) / 0.8f);
        }
    }

    private static class DiagShockwave {
        float cx;
        float cy;
        float radius = 2.0f;
        float maxRadius;
        int color;
        int age = 0;
        int maxAge;

        DiagShockwave(float cx, float cy, float maxRadius, int color, int maxAge) {
            this.cx = cx;
            this.cy = cy;
            this.maxRadius = maxRadius;
            this.color = color;
            this.maxAge = maxAge;
        }

        void update() {
            ++this.age;
            float t = (float)this.age / (float)this.maxAge;
            this.radius = 2.0f + (this.maxRadius - 2.0f) * (float)Math.sin((double)t * Math.PI / 2.0);
        }

        boolean isDead() {
            return this.age >= this.maxAge;
        }

        float getAlpha() {
            return Math.max(0.0f, 1.0f - (float)this.age / (float)this.maxAge);
        }
    }

    public static class DiagnosticItem {
        public final String title;
        public volatile Status status = Status.CHECKING;
        public volatile String message = "\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430...";
        public volatile String solution = "";
        public volatile String actionButtonText = "";
        public volatile Runnable actionButtonAction = null;
        public volatile String detailedLog = "";

        public DiagnosticItem(String title) {
            this.title = title;
        }
    }

    public static enum Status {
        CHECKING,
        OK,
        WARNING,
        ERROR;

    }
}

