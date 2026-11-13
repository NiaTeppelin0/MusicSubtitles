package com.example.yandexmusichud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.ducking.SmartDuckingManager;
import com.example.yandexmusichud.helper.HelperProcessManager;
import com.example.yandexmusichud.hud.ChangelogScreen;
import com.example.yandexmusichud.hud.HudEditScreen;
import com.example.yandexmusichud.hud.IntroScreen;
import com.example.yandexmusichud.hud.KeybindSetupScreen;
import com.example.yandexmusichud.hud.LyricEditorScreen;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.hud.MusicSyncScreen;
import com.example.yandexmusichud.hud.SystemDiagnosticsScreen;
import com.example.yandexmusichud.hud.TwitchSettingsScreen;
import com.example.yandexmusichud.twitch.TwitchManager;
import com.example.yandexmusichud.keybinds.MusicControlKeybinds;
import com.example.yandexmusichud.lyrics.LiveCaptionServer;
import com.example.yandexmusichud.lyrics.LyricsManager;
import com.example.yandexmusichud.lyrics.LyricsRenderer;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.stats.SessionStatsManager;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11909;
import net.minecraft.class_2172;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3417;
import net.minecraft.class_437;
import net.minecraft.class_442;

public class YandexMusicHudMod
implements ClientModInitializer {
    public static final String MOD_ID = "yandexmusichud";
    public static YandexMusicHudMod INSTANCE;
    private HelperProcessManager helperManager;
    private CoverTextureManager coverManager;
    private NowPlayingClient nowPlayingClient;
    private MusicHudRenderer hudRenderer;
    private LyricsManager lyricsManager;
    private LyricsRenderer lyricsRenderer;
    private LiveCaptionServer captionServer;

    public LyricsManager getLyricsManager() {
        return this.lyricsManager;
    }

    public CoverTextureManager getCoverManager() {
        return this.coverManager;
    }

    public void restartHelper() {
        new Thread(() -> {
            if (this.helperManager != null) {
                this.helperManager.stop();
                int p = this.helperManager.start();
                if (p > 0) {
                    HudConfig.INSTANCE.helperPort = p;
                    this.nowPlayingClient.start(p);
                }
            }
        }, "MusicSubtitles-HelperRestarter").start();
    }

    public void onInitializeClient() {
        INSTANCE = this;
        HudConfig.load();
        this.coverManager = new CoverTextureManager();
        this.nowPlayingClient = new NowPlayingClient(this.coverManager);
        this.hudRenderer = new MusicHudRenderer(this.nowPlayingClient, this.coverManager);
        this.helperManager = new HelperProcessManager();
        this.lyricsManager = new LyricsManager();
        this.lyricsRenderer = new LyricsRenderer(this.lyricsManager, this.nowPlayingClient);
        this.lyricsRenderer.register();
        MusicControlKeybinds.register();
        com.example.yandexmusichud.compat.VoiceChatCompat.init();
        com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.init();
        com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().init();
        this.captionServer = new LiveCaptionServer(this.lyricsManager);
        this.captionServer.start();
        this.exportUserscript();
        TwitchManager.getInstance().start();
        com.example.yandexmusichud.donationalerts.DonationAlertsManager.getInstance().start();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder root = ClientCommandManager.literal("musicsubtitles");
            root.then(ClientCommandManager.literal("cancel").executes(ctx -> {
                this.lyricsManager.disableCurrentTrack();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §cСубтитры для текущей песни отключены."));
                return 1;
            }));
            root.then(ClientCommandManager.literal("enable").executes(ctx -> {
                this.lyricsManager.enableCurrentTrack();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §aСубтитры для текущей песни включены."));
                return 1;
            }));
            root.then(ClientCommandManager.literal("reload").executes(ctx -> {
                this.lyricsManager.reloadCurrentTrack();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §eКэш субтитров сброшен, выполняется повторный поиск..."));
                return 1;
            }));
            root.then(ClientCommandManager.literal("test").executes(ctx -> {
                this.lyricsManager.spawnTestBubble();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §aТестовые 3D субтитры созданы на 15 секунд прямо перед вами!"));
                return 1;
            }));
            root.then(ClientCommandManager.literal("test3d").executes(ctx -> {
                this.lyricsManager.spawnTestBubble();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §aТестовые 3D субтитры созданы на 15 секунд прямо перед вами!"));
                return 1;
            }));
            root.then(ClientCommandManager.literal("diag").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new SystemDiagnosticsScreen(mc.field_1755, new KeybindSetupScreen(mc.field_1755))));
                return 1;
            }));
            root.then(ClientCommandManager.literal("local").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.LocalMusicScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("keybinds").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new KeybindSetupScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("setup").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.InitialSetupScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("wizard").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.InitialSetupScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("intro").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new IntroScreen(mc.field_1755)));
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §aЗапуск заставки Music Subtitles..."));
                return 1;
            }));
            root.then(ClientCommandManager.literal("youtube").executes(ctx -> {
                Path scriptPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("youtube_captions_sync.user.js");
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §eЮзерскрипт субтитров YouTube сохранён в:\n§f" + scriptPath.toString() + "\n§bУстановите его в Tampermonkey/Violentmonkey для мгновенного переноса субтитров из браузера в Minecraft!"));
                return 1;
            }));
            root.then(ClientCommandManager.literal("twitch").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new TwitchSettingsScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("da").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.DonationAlertsSettingsScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("donationalerts").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.DonationAlertsSettingsScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("anim").executes(ctx -> {
                String current = HudConfig.INSTANCE.subtitlesAnimation;
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §fТекущая анимация субтитров: §e" + current + "\n§7Доступные режимы: §abounce §7(Караоке прыжок), §bwave §7(Неоновая волна), §cmiside §7(MiSide падение), §dpop §7(Упругий поп-ап), §6pulse §7(Пульс в такт), §eslide §7(Слайд-появление), §fclassic §7(Классика)\n§7Использование: §e/musicsubtitles anim <режим> §7или §e/musicsubtitles nextanim"));
                return 1;
            }).then(ClientCommandManager.argument("mode", (ArgumentType)StringArgumentType.word()).suggests((ctx, builder) -> class_2172.method_9265(List.of("bounce", "wave", "miside", "pop", "pulse", "slide", "classic"), (SuggestionsBuilder)builder)).executes(ctx -> {
                String mode = StringArgumentType.getString((CommandContext)ctx, "mode").toLowerCase(Locale.ROOT);
                String mapped = switch (mode) {
                    case "wave" -> "WAVE_FLOAT";
                    case "miside", "miside_fall" -> "MISIDE_FALL";
                    case "pop" -> "ELASTIC_POP";
                    case "pulse" -> "PULSE_GLOW";
                    case "slide" -> "SLIDE_FADE";
                    case "classic" -> "CLASSIC";
                    default -> "KARAOKE_BOUNCE";
                };
                HudConfig.INSTANCE.subtitlesAnimation = mapped;
                HudConfig.save();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §aАнимация субтитров изменена на: §e" + mapped));
                return 1;
            })));
            root.then(ClientCommandManager.literal("nextanim").executes(ctx -> {
                String[] all = new String[]{"KARAOKE_BOUNCE", "WAVE_FLOAT", "MISIDE_FALL", "ELASTIC_POP", "PULSE_GLOW", "SLIDE_FADE", "CLASSIC"};
                int curIdx = 0;
                for (int i = 0; i < all.length; ++i) {
                    if (!all[i].equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation)) continue;
                    curIdx = i;
                    break;
                }
                String next = all[(curIdx + 1) % all.length];
                HudConfig.INSTANCE.subtitlesAnimation = next;
                HudConfig.save();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§6[Музыка] §aПереключено на анимацию: §e" + next));
                return 1;
            }));
            root.then(ClientCommandManager.literal("changelog").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new ChangelogScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("editor").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new LyricEditorScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("fav").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.FavoritesScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("favorites").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.FavoritesScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("font").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.FontSelectorScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("fonts").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.FontSelectorScreen(mc.field_1755)));
                return 1;
            }));
            root.then(ClientCommandManager.literal("tutorial").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new com.example.yandexmusichud.hud.ModTutorialScreen(mc.field_1755)));
                return 1;
            }));
            root.executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new HudEditScreen()));
                return 1;
            });

            var node = dispatcher.register(root);
            dispatcher.register(ClientCommandManager.literal("ymh").redirect(node));
            dispatcher.register(ClientCommandManager.literal("ms").redirect(node));

            LiteralArgumentBuilder syncRoot = ClientCommandManager.literal("musicsync");
            syncRoot.then(ClientCommandManager.literal("gui").executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new MusicSyncScreen(mc.field_1755)));
                return 1;
            }));
            syncRoot.then(ClientCommandManager.literal("create")
                .executes(ctx -> {
                    com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.createRoom();
                    ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §eСоздание комнаты..."));
                    return 1;
                })
                .then(ClientCommandManager.argument("password", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
                    String pass = StringArgumentType.getString((CommandContext)ctx, "password");
                    com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.createRoom(pass);
                    ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §eСоздание комнаты с паролем..."));
                    return 1;
                }))
            );
            syncRoot.then(ClientCommandManager.literal("join").then(ClientCommandManager.argument("code", (ArgumentType)StringArgumentType.word())
                .executes(ctx -> {
                    String code = StringArgumentType.getString((CommandContext)ctx, "code");
                    com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.joinRoom(code);
                    ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §eПодключение к комнате " + code.toUpperCase(Locale.ROOT) + "..."));
                    return 1;
                })
                .then(ClientCommandManager.argument("password", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
                    String code = StringArgumentType.getString((CommandContext)ctx, "code");
                    String pass = StringArgumentType.getString((CommandContext)ctx, "password");
                    com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.joinRoom(code, pass);
                    ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §eПодключение к комнате " + code.toUpperCase(Locale.ROOT) + "..."));
                    return 1;
                }))
            ));
            syncRoot.then(ClientCommandManager.literal("kick").then(ClientCommandManager.argument("target", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
                String target = StringArgumentType.getString((CommandContext)ctx, "target");
                com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.kickPlayer(target);
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §cЗапрос на исключение игрока: " + target));
                return 1;
            })));
            syncRoot.then(ClientCommandManager.literal("ban").then(ClientCommandManager.argument("target", (ArgumentType)StringArgumentType.word()).executes(ctx -> {
                String target = StringArgumentType.getString((CommandContext)ctx, "target");
                com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.banPlayer(target);
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §4Запрос на блокировку игрока: " + target));
                return 1;
            })));
            syncRoot.then(ClientCommandManager.literal("leave").executes(ctx -> {
                com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.leaveRoom();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §cВы покинули комнату."));
                return 1;
            }));
            syncRoot.then(ClientCommandManager.literal("status").executes(ctx -> {
                FabricClientCommandSource src = (FabricClientCommandSource)ctx.getSource();
                if (com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.isInRoom()) {
                    String r = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getCurrentRoom();
                    boolean host = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.isRoomHost();
                    String st = com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().getSyncStatusText();
                    src.sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §aКомната: §e" + r + (host ? " §6(Вы - хост)" : " §7(Слушатель)") + " §8| §fСтатус: " + st));
                } else {
                    src.sendFeedback((class_2561)class_2561.method_43470("§b♫ [Music Sync] §7Вы не в комнате. Используйте §e/musicsync create §7или §e/musicsync join <code>§7, либо §b/musicsync gui"));
                }
                return 1;
            }));
            syncRoot.then(ClientCommandManager.literal("diag").executes(ctx -> {
                FabricClientCommandSource src = (FabricClientCommandSource)ctx.getSource();
                src.sendFeedback((class_2561)class_2561.method_43470("§b✦ [Music Sync Диагностика] -------------------"));
                boolean inRoom = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.isInRoom();
                String r = inRoom ? com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getCurrentRoom() : "Нет";
                boolean host = com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.isRoomHost();
                src.sendFeedback((class_2561)class_2561.method_43470("§7Комната: §e" + r + " §8(Роль: " + (host ? "§6Хост" : "§bСлушатель") + "§8)"));
                src.sendFeedback((class_2561)class_2561.method_43470("§7Статус: " + com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().getSyncStatusText()));
                src.sendFeedback((class_2561)class_2561.method_43470("§7Источник звука: §f" + com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().getStreamSourceInfo()));
                src.sendFeedback((class_2561)class_2561.method_43470("§7Дрифт (рассинхрон): §e" + com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().getLastCalculatedDriftMs() + " мс"));
                src.sendFeedback((class_2561)class_2561.method_43470("§7Relay Ping: §a" + com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getRelayPingMs() + " мс"));
                src.sendFeedback((class_2561)class_2561.method_43470("§7Участников в комнате: §f" + com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.getRoomMembers().size()));
                src.sendFeedback((class_2561)class_2561.method_43470("§b✦ ------------------------------------------"));
                return 1;
            }));
            syncRoot.executes(ctx -> {
                class_310 mc = class_310.method_1551();
                mc.execute(() -> mc.method_1507((class_437)new MusicSyncScreen(mc.field_1755)));
                return 1;
            });
            var syncNode = dispatcher.register(syncRoot);
            dispatcher.register(ClientCommandManager.literal("sync").redirect(syncNode));
        });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> this.hudRenderer.render(drawContext, tickCounter));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((scr, drawContext, mouseX, mouseY, tickDelta) -> {
                if (this.lyricsRenderer != null) {
                    this.lyricsRenderer.render2DOverScreen(drawContext, mouseX, mouseY);
                }
            });
            ScreenMouseEvents.allowMouseClick(screen).register((scr, click) -> {
                if (click.comp_4800() != null && click.comp_4800().comp_4801() == 0) {
                    if (this.lyricsRenderer != null) {
                        if (this.lyricsRenderer.handleMousePress(click.comp_4798(), click.comp_4799())) {
                            return false;
                        }
                    }
                    if (!(scr instanceof HudEditScreen) && this.hudRenderer != null) {
                        if (this.hudRenderer.handleMouseClick(click.comp_4798(), click.comp_4799())) {
                            return false;
                        }
                    }
                }
                if (MusicHudRenderer.lastCancelBtnVisible) {
                    if (click.comp_4800() != null && click.comp_4800().comp_4801() == 0) {
                        double mx = click.comp_4798();
                        double my = click.comp_4799();
                        if (mx >= (double)MusicHudRenderer.lastCancelBtnScreenX && mx <= (double)(MusicHudRenderer.lastCancelBtnScreenX + MusicHudRenderer.lastCancelBtnScreenW) &&
                            my >= (double)MusicHudRenderer.lastCancelBtnScreenY && my <= (double)(MusicHudRenderer.lastCancelBtnScreenY + MusicHudRenderer.lastCancelBtnScreenH)) {
                            if (this.lyricsManager != null) {
                                this.lyricsManager.disableCurrentTrack();
                                class_310 mc = class_310.method_1551();
                                if (mc != null) {
                                    if (mc.method_1483() != null) {
                                        mc.method_1483().method_4873((class_1113)class_1109.method_4757(class_3417.field_14709, 1.0f, 0.8f));
                                    }
                                    if (mc.field_1705 != null) {
                                        mc.field_1705.method_1758((class_2561)class_2561.method_43470("§6[Музыка] §cСубтитры для текущей песни отключены. §7(Включить: §e/musicsubtitles enable§7)"), false);
                                    }
                                }
                            }
                            return false;
                        }
                    }
                }
                return true;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((scr, click) -> {
                if (this.lyricsRenderer != null && click.comp_4800() != null && click.comp_4800().comp_4801() == 0) {
                    this.lyricsRenderer.handleMouseRelease();
                }
                return true;
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MusicControlKeybinds.handleTick(this.nowPlayingClient, this.coverManager);
            SessionStatsManager.INSTANCE.onTick(this.nowPlayingClient.getState());
            SmartDuckingManager.INSTANCE.onTick();
            TwitchManager.getInstance().checkPendingCommand();
            if (!IntroScreen.hasShownIntro && HudConfig.INSTANCE.showIntroSplash && client.method_53466() && client.method_18506() == null && client.field_1755 instanceof class_442) {
                IntroScreen.hasShownIntro = true;
                client.method_1507((class_437)new IntroScreen(client.field_1755));
            } else if (client.method_53466() && client.method_18506() == null && client.field_1755 instanceof class_442) {


                if (HudConfig.INSTANCE.promptYandexMusicOnStartup && !com.example.yandexmusichud.hud.YandexMusicSettingsScreen.hasPromptedStartup) {
                    com.example.yandexmusichud.hud.YandexMusicSettingsScreen.hasPromptedStartup = true;
                    client.method_1507((class_437)new com.example.yandexmusichud.hud.YandexMusicSettingsScreen(client.field_1755));
                } else if (!HudConfig.INSTANCE.initialSetupCompleted && !HudConfig.INSTANCE.showIntroSplash) {
                    client.method_1507((class_437)new com.example.yandexmusichud.hud.InitialSetupScreen(client.field_1755));
                }
            }
            if (client.method_22683() != null) {
                int screenWidth = client.method_22683().method_4486();
                int screenHeight = client.method_22683().method_4502();
                this.lyricsManager.update(this.nowPlayingClient.getState(), screenWidth, screenHeight);
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            this.startHelper();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (this.captionServer != null) {
                this.captionServer.stop();
            }
            if (this.lyricsManager != null) {
                this.lyricsManager.shutdown();
            }
            com.example.yandexmusichud.local.LocalMusicPlayer.getInstance().stop();
            com.example.yandexmusichud.hud.PlayerBadgeManager.INSTANCE.shutdown();
            this.nowPlayingClient.shutdown();
            this.coverManager.shutdown();
            com.example.yandexmusichud.texture.CustomBackgroundManager.INSTANCE.shutdown();
            this.helperManager.shutdown();
            TwitchManager.getInstance().shutdown();
            com.example.yandexmusichud.donationalerts.DonationAlertsManager.getInstance().shutdown();
        });
    }

    private void exportUserscript() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
            Files.createDirectories(configDir, new FileAttribute[0]);
            Path targetFile = configDir.resolve("youtube_captions_sync.user.js");
            try (InputStream is = this.getClass().getResourceAsStream("/assets/yandexmusichud/youtube_captions_sync.user.js");){
                if (is != null) {
                    Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        catch (Exception exception) {

        }
    }

    private void startHelper() {
        Thread startThread = new Thread(() -> {
            int port = this.helperManager.start();
            if (port > 0) {
                HudConfig.INSTANCE.helperPort = port;
                this.nowPlayingClient.start(port);
            } else if (HudConfig.INSTANCE.helperPort > 0) {
                this.nowPlayingClient.start(HudConfig.INSTANCE.helperPort);
            }
        }, "MusicSubtitles-HelperStarter");
        startThread.setDaemon(true);
        startThread.start();
    }
}

