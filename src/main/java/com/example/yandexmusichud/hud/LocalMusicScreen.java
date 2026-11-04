package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.local.LocalMusicPlayer;
import com.example.yandexmusichud.local.LocalMusicScanner;
import com.example.yandexmusichud.local.LocalMusicTrack;
import java.awt.Desktop;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_10799;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

public class LocalMusicScreen extends class_437 {
    private static final class_2960 TEX_VINYL = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_record.png");
    private static final class_2960 TEX_SPINDLE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_spindle.png");
    private final class_437 parent;
    private class_342 searchField;
    private final List<LocalMusicTrack> filteredTracks = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrubber = false;
    private float dragSeekPreviewFrac = 0.0f;
    private ModernButtonWidget btnPlayPause;
    private ModernButtonWidget btnShuffle;
    private ModernButtonWidget btnRepeat;
    private ModernSliderWidget sliderVolume;

    public LocalMusicScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("🎵 Моя музыка"));
        this.parent = parent;
        LocalMusicPlayer.getInstance().init();
    }

    protected void method_25426() {
        super.method_25426();
        int panelW = Math.min(480, this.field_22789 - 24);
        int panelH = this.field_22790 - 40;
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = 20;

        int searchW = panelW - 170;
        this.searchField = new class_342(this.field_22793, panelX + 10, panelY + 28, searchW, 18, (class_2561)class_2561.method_43470("Поиск..."));
        this.searchField.method_47404((class_2561)class_2561.method_43470("🔍 Поиск по названию или автору..."));
        this.searchField.method_1863(this::updateFilteredList);
        this.method_37063(this.searchField);

        ModernButtonWidget btnFolder = new ModernButtonWidget(panelX + 10 + searchW + 4, panelY + 28, 70, 18, (class_2561)class_2561.method_43470("📁 Папка"), b -> {
            this.openMusicFolder();
        });
        btnFolder.setCustomBgColor(0xFF131C2E);
        this.method_37063(btnFolder);

        ModernButtonWidget btnRefresh = new ModernButtonWidget(panelX + 10 + searchW + 78, panelY + 28, 72, 18, (class_2561)class_2561.method_43470("🔄 Обновить"), b -> {
            LocalMusicPlayer.getInstance().reloadPlaylist();
            this.updateFilteredList(this.searchField != null ? this.searchField.method_1882() : "");
        });
        btnRefresh.setCustomBgColor(0xFF0F2922);
        this.method_37063(btnRefresh);

        int bottomY = panelY + panelH - 32;
        int ctrlBtnW = 28;
        int startBtnX = panelX + (panelW - (ctrlBtnW * 5 + 16)) / 2;

        this.btnShuffle = new ModernButtonWidget(startBtnX, bottomY, ctrlBtnW, 20, (class_2561)class_2561.method_43470("🔀"), b -> {
            boolean next = !LocalMusicPlayer.getInstance().isShuffle();
            LocalMusicPlayer.getInstance().setShuffle(next);
            this.updateControlButtons();
        });
        this.method_37063(this.btnShuffle);

        ModernButtonWidget btnPrev = new ModernButtonWidget(startBtnX + ctrlBtnW + 4, bottomY, ctrlBtnW, 20, (class_2561)class_2561.method_43470("⏮"), b -> {
            LocalMusicPlayer.getInstance().previous();
            this.updateControlButtons();
        });
        this.method_37063(btnPrev);

        this.btnPlayPause = new ModernButtonWidget(startBtnX + (ctrlBtnW + 4) * 2, bottomY, ctrlBtnW, 20, (class_2561)class_2561.method_43470("▶"), b -> {
            LocalMusicPlayer.getInstance().togglePlayPause();
            this.updateControlButtons();
        });
        this.btnPlayPause.setPrimary(true);
        this.method_37063(this.btnPlayPause);

        ModernButtonWidget btnNext = new ModernButtonWidget(startBtnX + (ctrlBtnW + 4) * 3, bottomY, ctrlBtnW, 20, (class_2561)class_2561.method_43470("⏭"), b -> {
            LocalMusicPlayer.getInstance().next();
            this.updateControlButtons();
        });
        this.method_37063(btnNext);

        this.btnRepeat = new ModernButtonWidget(startBtnX + (ctrlBtnW + 4) * 4, bottomY, ctrlBtnW, 20, (class_2561)class_2561.method_43470("🔁"), b -> {
            boolean next = !LocalMusicPlayer.getInstance().isRepeat();
            LocalMusicPlayer.getInstance().setRepeat(next);
            this.updateControlButtons();
        });
        this.method_37063(this.btnRepeat);

        int sliderW = 104;
        int sliderX = panelX + panelW - sliderW - 10;
        this.sliderVolume = new ModernSliderWidget(sliderX, bottomY, sliderW, 20, "🔊 Громкость: ", (double)LocalMusicPlayer.getInstance().getVolume(), val -> String.format("%d%%", (int)Math.round(val * 100.0)), val -> {
            LocalMusicPlayer.getInstance().setVolume(val.floatValue());
        });
        this.sliderVolume.setOnRelease(() -> HudConfig.save());
        this.method_37063(this.sliderVolume);

        ModernButtonWidget btnBack = new ModernButtonWidget(panelX + 10, bottomY, 60, 20, (class_2561)class_2561.method_43470("Назад"), b -> {
            this.field_22787.method_1507(this.parent);
        });
        this.method_37063(btnBack);

        this.updateFilteredList(this.searchField != null ? this.searchField.method_1882() : "");
        this.updateControlButtons();
    }

    private void updateControlButtons() {
        LocalMusicPlayer player = LocalMusicPlayer.getInstance();
        if (this.btnPlayPause != null) {
            String pp = (player.isPlaying() && !player.isPaused()) ? "⏸" : "▶";
            this.btnPlayPause.method_25355((class_2561)class_2561.method_43470(pp));
        }
        if (this.btnShuffle != null) {
            this.btnShuffle.setCustomBgColor(player.isShuffle() ? 0xCC0F2E47 : 0xD80E1420);
        }
        if (this.btnRepeat != null) {
            this.btnRepeat.setCustomBgColor(player.isRepeat() ? 0xCC0F2E47 : 0xD80E1420);
        }
    }

    private void openMusicFolder() {
        Path dir = LocalMusicScanner.INSTANCE.getMusicDirectory();
        com.example.yandexmusichud.favorites.FavoritesManager.openFolderSafely(dir);
    }

    private void updateFilteredList(String query) {
        this.filteredTracks.clear();
        String q = query != null ? query.trim().toLowerCase() : "";
        List<LocalMusicTrack> all = LocalMusicPlayer.getInstance().getPlaylist();
        for (LocalMusicTrack track : all) {
            if (!q.isEmpty()) {
                boolean matchTitle = track.getTitle().toLowerCase().contains(q);
                boolean matchArtist = track.getArtist().toLowerCase().contains(q);
                boolean matchAlbum = track.getAlbum().toLowerCase().contains(q);
                boolean matchFmt = track.getFormat().toLowerCase().contains(q);
                if (!matchTitle && !matchArtist && !matchAlbum && !matchFmt) {
                    continue;
                }
            }
            this.filteredTracks.add(track);
        }

        int itemH = 34;
        int panelH = this.field_22790 - 40;
        int listH = panelH - 128;
        int totalContentH = this.filteredTracks.size() * itemH;
        this.maxScroll = Math.max(0, totalContentH - listH);
        this.scrollOffset = class_3532.method_15340(this.scrollOffset, 0, this.maxScroll);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.updateControlButtons();

        int panelW = Math.min(480, this.field_22789 - 24);
        int panelH = this.field_22790 - 40;
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = 20;

        MusicHudRenderer.renderRoundedRect(context, panelX - 2, panelY - 2, panelW + 4, panelH + 4, 12, 0x44000000);
        MusicHudRenderer.renderRoundedRect(context, panelX, panelY, panelW, panelH, 10, 0xF60A0D14);
        MusicHudRenderer.renderRoundedRect(context, panelX + 2, panelY + 2, panelW - 4, 48, 8, 0xEE06080D);
        MusicHudRenderer.renderRoundedRect(context, panelX + 2, panelY + panelH - 72, panelW - 4, 70, 8, 0xF206080D);
        context.method_25294(panelX + 10, panelY + 50, panelX + panelW - 10, panelY + 51, 0x441E293B);
        context.method_25294(panelX + 10, panelY + panelH - 72, panelX + panelW - 10, panelY + panelH - 71, 0x441E293B);
        MusicHudRenderer.renderRoundedBorder(context, panelX, panelY, panelW, panelH, 10, 0x66475569, 0x331E293B);

        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("§b🎵 §fМоя музыка §8· §7Локальная медиатека"), panelX + panelW / 2, panelY + 8, -1);
        String subHeader = "§7Треков: §b" + LocalMusicPlayer.getInstance().getPlaylist().size() + " §8| §7.minecraft/config/yandexmusichud/music/";
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(subHeader), panelX + panelW / 2, panelY + 18, -1);

        int listX = panelX + 10;
        int listY = panelY + 54;
        int listW = panelW - 20;
        int listH = panelH - 130;
        int itemH = 34;

        context.method_44379(listX, listY, listX + listW, listY + listH);

        LocalMusicTrack curPlaying = LocalMusicPlayer.getInstance().getCurrentTrack();
        boolean isPlayingNow = LocalMusicPlayer.getInstance().isPlaying() && !LocalMusicPlayer.getInstance().isPaused();
        long nowMs = System.currentTimeMillis();

        if (this.filteredTracks.isEmpty()) {
            if (LocalMusicPlayer.getInstance().getPlaylist().isEmpty()) {
                String msg1 = "§7В папке пока нет музыкальных файлов.";
                String msg2 = "§fНажмите §b[📁 Папка] §fи скопируйте туда свои §e.mp3, .wav §fили §e.ogg§f!";
                String msg3 = "§8Поддерживаются синхронные субтитры .lrc с тем же именем.";
                context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(msg1), panelX + panelW / 2, listY + listH / 2 - 20, -1);
                context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(msg2), panelX + panelW / 2, listY + listH / 2, -1);
                context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(msg3), panelX + panelW / 2, listY + listH / 2 + 16, -1);
            } else {
                context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("§7Ничего не найдено по вашему запросу."), panelX + panelW / 2, listY + listH / 2 - 4, -1);
            }
        } else {
            for (int i = 0; i < this.filteredTracks.size(); i++) {
                LocalMusicTrack track = this.filteredTracks.get(i);
                int iy = listY + i * itemH - this.scrollOffset;
                if (iy + itemH < listY || iy > listY + listH) {
                    continue;
                }

                boolean isThisPlaying = curPlaying != null && track.getPath().equals(curPlaying.getPath());
                boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= iy && mouseY <= iy + itemH - 2 && mouseY >= listY && mouseY <= listY + listH;

                int bgColor = isThisPlaying ? 0xE60F1D30 : (hovered ? 0xD8131B2B : 0xAA0D121C);
                MusicHudRenderer.renderRoundedRect(context, listX, iy, listW, itemH - 2, 5, bgColor);
                int borderColor = isThisPlaying ? 0xAA38BDF8 : (hovered ? 0x55334155 : 0x221E293B);
                MusicHudRenderer.renderRoundedBorder(context, listX, iy, listW, itemH - 2, 5, borderColor, borderColor);

                if (isThisPlaying) {
                    MusicHudRenderer.renderRoundedRect(context, listX + 2, iy + 5, 2, itemH - 12, 1, 0xFF38BDF8);
                    if (isPlayingNow) {
                        for (int b = 0; b < 3; b++) {
                            float phase = (float)(nowMs % 850L) / 850.0f * 6.2831855f + (float)b * 1.45f;
                            int bh = 3 + Math.round((0.5f + 0.5f * (float)Math.sin(phase)) * 10.0f);
                            int bx = listX + 9 + b * 4;
                            int by = iy + 22 - bh;
                            int bCol = b == 0 ? 0xFF10B981 : (b == 1 ? 0xFF38BDF8 : 0xFF818CF8);
                            MusicHudRenderer.renderRoundedRect(context, bx, by, 3, bh, 1, bCol);
                        }
                    } else {
                        MusicHudRenderer.renderRoundedRect(context, listX + 10, iy + 10, 3, 11, 1, 0xFFFBBF24);
                        MusicHudRenderer.renderRoundedRect(context, listX + 15, iy + 10, 3, 11, 1, 0xFFFBBF24);
                    }
                } else {
                    int iconColor = hovered ? 0xFFF8FAFC : 0xFF64748B;
                    context.method_51433(this.field_22793, "▶", listX + 10, iy + 11, iconColor, true);
                }

                int textStartX = listX + 30;
                int textMaxW = listW - 132;
                String title = this.field_22793.method_27523(track.getTitle(), textMaxW);
                int titleColor = isThisPlaying ? 0xFF38BDF8 : 0xFFF8FAFC;
                context.method_51433(this.field_22793, title, textStartX, iy + 5, titleColor, true);

                String artist = this.field_22793.method_27523(track.getArtist(), textMaxW);
                context.method_51433(this.field_22793, artist, textStartX, iy + 17, hovered ? 0xFFCBD5E1 : 0xFF64748B, false);

                int badgeX = listX + listW - 92;
                int fmtColor = "MP3".equals(track.getFormat()) ? 0x44F59E0B : ("WAV".equals(track.getFormat()) ? 0x4438BDF8 : 0x44A855F7);
                int fmtBorder = "MP3".equals(track.getFormat()) ? 0x88F59E0B : ("WAV".equals(track.getFormat()) ? 0x8838BDF8 : 0x88A855F7);
                MusicHudRenderer.renderRoundedRect(context, badgeX, iy + 9, 30, 13, 3, fmtColor);
                MusicHudRenderer.renderRoundedBorder(context, badgeX, iy + 9, 30, 13, 3, fmtBorder);
                context.method_51433(this.field_22793, track.getFormat(), badgeX + 4, iy + 12, 0xFFF8FAFC, false);

                if (track.hasLyrics()) {
                    MusicHudRenderer.renderRoundedRect(context, badgeX - 28, iy + 9, 25, 13, 3, 0x3310B981);
                    MusicHudRenderer.renderRoundedBorder(context, badgeX - 28, iy + 9, 25, 13, 3, 0x8810B981);
                    context.method_51433(this.field_22793, "LRC", badgeX - 24, iy + 12, 0xFF10B981, false);
                }

                String dur = track.getFormattedDuration();
                context.method_51433(this.field_22793, dur, listX + listW - 42, iy + 12, hovered ? 0xFFE2E8F0 : 0xFF94A3B8, false);
            }

            if (this.maxScroll > 0) {
                int barW = 3;
                int barX = listX + listW - barW - 1;
                float ratio = (float)listH / (float)(this.filteredTracks.size() * itemH);
                int thumbH = Math.max(16, (int)((float)listH * ratio));
                int thumbY = listY + (int)(((float)this.scrollOffset / (float)this.maxScroll) * (float)(listH - thumbH));
                MusicHudRenderer.renderRoundedRect(context, barX, listY, barW, listH, 1, 0x331E293B);
                MusicHudRenderer.renderRoundedRect(context, barX, thumbY, barW, thumbH, 1, 0xAA38BDF8);
            }
        }

        context.method_44380();

        int scrubberY = panelY + panelH - 52;
        int scrubberX = panelX + 54;
        int scrubberW = panelW - 108;
        int scrubberH = 4;

        long curPos = LocalMusicPlayer.getInstance().getPositionMs();
        long totalDur = LocalMusicPlayer.getInstance().getDurationMs();
        float progress = this.isDraggingScrubber ? this.dragSeekPreviewFrac : (totalDur > 0 ? Math.min(1.0f, (float)curPos / (float)totalDur) : 0.0f);
        if (this.isDraggingScrubber && totalDur > 0L) {
            curPos = (long)((float)totalDur * this.dragSeekPreviewFrac);
        }

        long posSec = curPos / 1000L;
        String curTimeStr = String.format("%02d:%02d", posSec / 60L, posSec % 60L);
        context.method_51433(this.field_22793, curTimeStr, panelX + 14, scrubberY - 2, 0xFFE2E8F0, false);

        long durSec = totalDur / 1000L;
        String durTimeStr = String.format("%02d:%02d", durSec / 60L, durSec % 60L);
        context.method_51433(this.field_22793, durTimeStr, panelX + panelW - 44, scrubberY - 2, 0xFF64748B, false);

        MusicHudRenderer.renderProgressBarWithPlayhead(context, scrubberX, scrubberY, scrubberW, scrubberH, progress, 0xFF38BDF8, 0xFF10B981, nowMs);

        int discCx = panelX + 34;
        int discCy = scrubberY - 11;
        float vinylAngle = isPlayingNow ? (float)(nowMs % 4000L) / 4000.0f * 360.0f : 0.0f;
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)discCx, (float)discCy);
        context.method_51448().rotate((float)Math.toRadians(vinylAngle));
        context.method_25291(class_10799.field_56883, TEX_VINYL, -8, -8, 0.0f, 0.0f, 16, 16, 16, 16, -1);
        context.method_25291(class_10799.field_56883, TEX_SPINDLE, -3, -3, 0.0f, 0.0f, 6, 6, 6, 6, -1);
        context.method_51448().popMatrix();

        if (curPlaying != null) {
            String playingInfo = (isPlayingNow ? "§b▶ " : "§e⏸ ") + "§f" + curPlaying.getArtist() + " §8— §b" + curPlaying.getTitle();
            String trimmed = this.field_22793.method_27523(playingInfo, panelW - 96);
            context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(trimmed), panelX + panelW / 2, scrubberY - 15, -1);
        } else {
            context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("§7Выберите трек из списка для воспроизведения"), panelX + panelW / 2, scrubberY - 15, -1);
        }
        super.method_25394(context, mouseX, mouseY, delta);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        if (super.method_25402(click, bl)) {
            return true;
        }

        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        if (click.method_74245() != 0) {
            return false;
        }

        int panelW = Math.min(480, this.field_22789 - 24);
        int panelH = this.field_22790 - 40;
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = 20;

        int scrubberY = panelY + panelH - 52;
        int scrubberX = panelX + 54;
        int scrubberW = panelW - 108;
        if (mouseY >= (double)(scrubberY - 4) && mouseY <= (double)(scrubberY + 8) && mouseX >= (double)scrubberX && mouseX <= (double)(scrubberX + scrubberW)) {
            this.isDraggingScrubber = true;
            this.updateScrubberPreview(mouseX, scrubberX, scrubberW);
            return true;
        }

        int listX = panelX + 10;
        int listY = panelY + 52;
        int listW = panelW - 20;
        int listH = panelH - 128;
        int itemH = 34;

        if (mouseX >= (double)listX && mouseX <= (double)(listX + listW) && mouseY >= (double)listY && mouseY <= (double)(listY + listH)) {
            int clickOffset = (int)mouseY - listY + this.scrollOffset;
            int clickedIdx = clickOffset / itemH;
            if (clickedIdx >= 0 && clickedIdx < this.filteredTracks.size()) {
                LocalMusicTrack track = this.filteredTracks.get(clickedIdx);
                LocalMusicTrack cur = LocalMusicPlayer.getInstance().getCurrentTrack();
                if (cur != null && cur.getPath().equals(track.getPath()) && (LocalMusicPlayer.getInstance().isPlaying() || LocalMusicPlayer.getInstance().isPaused())) {
                    LocalMusicPlayer.getInstance().togglePlayPause();
                } else {
                    LocalMusicPlayer.getInstance().playTrack(track);
                }
                this.updateControlButtons();
                return true;
            }
        }
        return false;
    }

    public boolean method_25406(class_11909 click) {
        if (this.isDraggingScrubber) {
            this.isDraggingScrubber = false;
            long totalDur = LocalMusicPlayer.getInstance().getDurationMs();
            if (totalDur > 0L) {
                long targetMs = (long)((float)totalDur * this.dragSeekPreviewFrac);
                LocalMusicPlayer.getInstance().seekTo(targetMs);
            }
            return true;
        }
        return super.method_25406(click);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        if (this.isDraggingScrubber) {
            int panelW = Math.min(480, this.field_22789 - 24);
            int panelX = (this.field_22789 - panelW) / 2;
            int scrubberX = panelX + 54;
            int scrubberW = panelW - 108;
            this.updateScrubberPreview(click.comp_4798(), scrubberX, scrubberW);
            return true;
        }
        return super.method_25403(click, deltaX, deltaY);
    }

    private void updateScrubberPreview(double mouseX, int scrubberX, int scrubberW) {
        float frac = (float)(mouseX - (double)scrubberX) / (float)scrubberW;
        this.dragSeekPreviewFrac = Math.max(0.0f, Math.min(1.0f, frac));
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.sliderVolume != null && mouseX >= (double)this.sliderVolume.method_46426() && mouseX <= (double)(this.sliderVolume.method_46426() + this.sliderVolume.method_25368()) && mouseY >= (double)this.sliderVolume.method_46427() && mouseY <= (double)(this.sliderVolume.method_46427() + this.sliderVolume.method_25364())) {
            float cur = LocalMusicPlayer.getInstance().getVolume();
            float next = Math.max(0.0f, Math.min(1.0f, cur + (float)verticalAmount * 0.05f));
            LocalMusicPlayer.getInstance().setVolume(next);
            this.sliderVolume.setValue((double)next);
            return true;
        }
        if (this.maxScroll > 0) {
            this.scrollOffset = class_3532.method_15340((int)((double)this.scrollOffset - verticalAmount * 24.0), 0, this.maxScroll);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25404(class_11908 input) {
        int keyCode = input.comp_4795();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.field_22787.method_1507(this.parent);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE && (this.searchField == null || !this.searchField.method_25370())) {
            LocalMusicPlayer.getInstance().togglePlayPause();
            this.updateControlButtons();
            return true;
        }
        return super.method_25404(input);
    }
}
