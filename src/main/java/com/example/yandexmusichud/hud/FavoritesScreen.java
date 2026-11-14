package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.favorites.FavoriteTrack;
import com.example.yandexmusichud.favorites.FavoritesManager;
import com.example.yandexmusichud.hud.ModernButtonWidget;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import com.example.yandexmusichud.network.NowPlayingClient;
import com.example.yandexmusichud.network.TrackState;
import com.example.yandexmusichud.texture.CoverTextureManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_364;
import net.minecraft.class_437;

public class FavoritesScreen
extends class_437 {
    private static final class_2960 TEX_VINYL = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_record.png");
    private static final class_2960 TEX_SPINDLE = class_2960.method_60655("yandexmusichud", "textures/gui/radial/vinyl_spindle.png");
    private static final AtomicLong THUMB_ID_GEN = new AtomicLong(0L);
    private final class_437 parent;
    private ModernTextFieldWidget searchField;
    private final List<FavoriteTrack> filteredTracks = new ArrayList<FavoriteTrack>();
    private static final Map<String, class_2960> THUMBNAIL_CACHE = new ConcurrentHashMap<String, class_2960>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private ModernButtonWidget btnPlayPause;
    private ModernButtonWidget btnFavCurrent;

    public FavoritesScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"\u2b50 \u0418\u0437\u0431\u0440\u0430\u043d\u043d\u0430\u044f \u043c\u0443\u0437\u044b\u043a\u0430"));
        this.parent = parent;
    }

    protected void method_25426() {
        super.method_25426();
        int panelWidth = Math.min(460, this.field_22789 - 24);
        int panelHeight = this.field_22790 - 40;
        int panelX = (this.field_22789 - panelWidth) / 2;
        int panelY = 20;

        int searchW = panelWidth - 178;
        this.searchField = new ModernTextFieldWidget(this.field_22793, panelX + 10, panelY + 26, searchW, 18, (class_2561)class_2561.method_43470((String)"\u041f\u043e\u0438\u0441\u043a...")).setPlaceholder("\ud83d\udd0d \u041f\u043e\u0438\u0441\u043a \u043f\u043e \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044e \u0438\u043b\u0438 \u0430\u0432\u0442\u043e\u0440\u0443...");
        this.searchField.method_1863(this::updateFilteredList);
        this.method_37063(this.searchField);

        this.btnFavCurrent = new ModernButtonWidget(panelX + 14 + searchW, panelY + 26, 76, 18, (class_2561)class_2561.method_43470((String)"\u2605 \u0422\u0435\u043a\u0443\u0449\u0438\u0439"), b -> {
            NowPlayingClient client = NowPlayingClient.INSTANCE;
            if (client != null && client.getState().isActive() && !client.getState().title().isEmpty()) {
                FavoritesManager.INSTANCE.toggleFavorite(client.getState(), null);
                this.updateFilteredList(this.searchField != null ? this.searchField.method_1882() : "");
            }
        });
        this.btnFavCurrent.setCustomBgColor(0xFF132238);
        this.method_37063(this.btnFavCurrent);

        this.method_37063(new ModernButtonWidget(panelX + 94 + searchW, panelY + 26, 74, 18, (class_2561)class_2561.method_43470((String)"\ud83d\uddd1 \u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c"), b -> {
            for (FavoriteTrack t : new ArrayList<FavoriteTrack>(FavoritesManager.INSTANCE.getFavorites())) {
                FavoritesManager.INSTANCE.removeFavorite(t.id);
            }
            this.updateFilteredList(this.searchField.method_1882());
        }).setCustomBgColor(0xFF4C111B));

        int bottomY = panelY + panelHeight - 28;
        this.method_37063(new ModernButtonWidget(panelX + 10, bottomY, 64, 20, (class_2561)class_2561.method_43470((String)"\u041d\u0430\u0437\u0430\u0434"), b -> this.field_22787.method_1507(this.parent)));

        int ctrlStartX = panelX + panelWidth - 106;
        this.method_37063(new ModernButtonWidget(ctrlStartX, bottomY, 28, 20, (class_2561)class_2561.method_43470((String)"\u23ee"), b -> {
            if (NowPlayingClient.INSTANCE != null) {
                NowPlayingClient.INSTANCE.sendPreviousAsync();
            }
        }));
        this.btnPlayPause = new ModernButtonWidget(ctrlStartX + 32, bottomY, 32, 20, (class_2561)class_2561.method_43470((String)"\u23ef"), b -> {
            if (NowPlayingClient.INSTANCE != null) {
                NowPlayingClient.INSTANCE.sendPlayPauseAsync();
            }
        }).setPrimary(true);
        this.method_37063(this.btnPlayPause);
        this.method_37063(new ModernButtonWidget(ctrlStartX + 68, bottomY, 28, 20, (class_2561)class_2561.method_43470((String)"\u23ed"), b -> {
            if (NowPlayingClient.INSTANCE != null) {
                NowPlayingClient.INSTANCE.sendNextAsync();
            }
        }));

        this.updateFilteredList(this.searchField != null ? this.searchField.method_1882() : "");
    }

    private void updateFilteredList(String query) {
        this.filteredTracks.clear();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        for (FavoriteTrack track : FavoritesManager.INSTANCE.getFavorites()) {
            if (!q.isEmpty() && !track.title.toLowerCase(Locale.ROOT).contains(q) && !track.artist.toLowerCase(Locale.ROOT).contains(q) && (track.album == null || !track.album.toLowerCase(Locale.ROOT).contains(q)) && (track.platformName == null || !track.platformName.toLowerCase(Locale.ROOT).contains(q))) continue;
            this.filteredTracks.add(track);
        }
        int itemHeight = 36;
        int panelHeight = this.field_22790 - 40;
        int listHeight = panelHeight - 122;
        int totalContentHeight = this.filteredTracks.size() * itemHeight;
        this.maxScroll = Math.max(0, totalContentHeight - listHeight);
        this.scrollOffset = class_3532.method_15340((int)this.scrollOffset, (int)0, (int)this.maxScroll);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(460, this.field_22789 - 24);
        int panelHeight = this.field_22790 - 40;
        int panelX = (this.field_22789 - panelWidth) / 2;
        int panelY = 20;

        NowPlayingClient client = NowPlayingClient.INSTANCE;
        TrackState curState = client != null ? client.getState() : null;
        boolean hasActiveTrack = curState != null && curState.isActive() && !curState.title().isEmpty();
        boolean isPlayingNow = hasActiveTrack && curState.isPlaying();
        long nowMs = System.currentTimeMillis();

        if (this.btnPlayPause != null) {
            this.btnPlayPause.method_25355((class_2561)class_2561.method_43470(isPlayingNow ? "\u23f8" : "\u25b6"));
        }
        if (this.btnFavCurrent != null && hasActiveTrack) {
            boolean isCurFav = FavoritesManager.INSTANCE.isFavorite(curState.artist(), curState.title());
            this.btnFavCurrent.method_25355((class_2561)class_2561.method_43470(isCurFav ? "\u2605 \u0412 \u0438\u0437\u0431\u0440." : "\u2606 \u0422\u0435\u043a\u0443\u0449\u0438\u0439"));
            this.btnFavCurrent.setCustomBgColor(isCurFav ? 0xCC0F2E47 : 0xFF132238);
        }

        MusicHudRenderer.renderRoundedRect(context, panelX - 2, panelY - 2, panelWidth + 4, panelHeight + 4, 12, 0x44000000);
        MusicHudRenderer.renderRoundedRect(context, panelX, panelY, panelWidth, panelHeight, 10, 0xF60A0D14);
        MusicHudRenderer.renderRoundedRect(context, panelX + 2, panelY + 2, panelWidth - 4, 46, 8, 0xEE06080D);
        MusicHudRenderer.renderRoundedRect(context, panelX + 2, panelY + panelHeight - 64, panelWidth - 4, 62, 8, 0xF206080D);
        context.method_25294(panelX + 10, panelY + 48, panelX + panelWidth - 10, panelY + 49, 0x441E293B);
        context.method_25294(panelX + 10, panelY + panelHeight - 64, panelX + panelWidth - 10, panelY + panelHeight - 63, 0x441E293B);
        MusicHudRenderer.renderRoundedBorder(context, panelX, panelY, panelWidth, panelHeight, 10, 0x66475569, 0x331E293B);

        String headerTitle = "\u00a7e\u2b50 \u00a7f\u0418\u0437\u0431\u0440\u0430\u043d\u043d\u0430\u044f \u043c\u0443\u0437\u044b\u043a\u0430 \u00a78\u00b7 \u00a77\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e: \u00a7b" + FavoritesManager.INSTANCE.getFavorites().size();
        context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(headerTitle), panelX + panelWidth / 2, panelY + 10, -1);

        int listX = panelX + 10;
        int listY = panelY + 52;
        int listW = panelWidth - 20;
        int listH = panelHeight - 122;
        int itemH = 36;
        context.method_44379(listX, listY, listX + listW, listY + listH);
        if (this.filteredTracks.isEmpty()) {
            String emptyMsg = FavoritesManager.INSTANCE.getFavorites().isEmpty() ? "\u00a77\u041d\u0435\u0442 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043d\u044b\u0445 \u0442\u0440\u0435\u043a\u043e\u0432. \u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u00a7e\u2606 \u00a77\u0432 \u043f\u043b\u0430\u0448\u043a\u0435 \u0438\u043b\u0438 \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u00a7bK\u00a77!" : "\u00a77\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e \u043f\u043e \u0432\u0430\u0448\u0435\u043c\u0443 \u0437\u0430\u043f\u0440\u043e\u0441\u0443.";
            context.method_27534(this.field_22793, (class_2561)class_2561.method_43470(emptyMsg), panelX + panelWidth / 2, listY + listH / 2 - 4, -1);
        } else {
            for (int i = 0; i < this.filteredTracks.size(); ++i) {
                int itemY = listY + i * itemH - this.scrollOffset;
                if (itemY + itemH < listY || itemY > listY + listH) continue;
                FavoriteTrack track = this.filteredTracks.get(i);
                boolean isThisPlaying = hasActiveTrack && track.title != null && track.title.equalsIgnoreCase(curState.title()) && (track.artist == null || track.artist.equalsIgnoreCase(curState.artist()));
                boolean isHovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= itemY && mouseY <= itemY + itemH - 2 && mouseY >= listY && mouseY <= listY + listH;
                int itemBg = isThisPlaying ? 0xE60F1D30 : (isHovered ? 0xD8131B2B : 0xAA0D121C);
                int itemBorder = isThisPlaying ? 0xAA38BDF8 : (isHovered ? 0x55334155 : 0x221E293B);
                MusicHudRenderer.renderRoundedRect(context, listX, itemY, listW, itemH - 2, 5, itemBg);
                MusicHudRenderer.renderRoundedBorder(context, listX, itemY, listW, itemH - 2, 5, itemBorder, itemBorder);
                if (isThisPlaying) {
                    MusicHudRenderer.renderRoundedRect(context, listX + 2, itemY + 5, 2, itemH - 12, 1, 0xFF38BDF8);
                }

                class_2960 coverId = this.getThumbnailId(track);
                context.method_25291(class_10799.field_56883, coverId, listX + 6, itemY + 2, 0.0f, 0.0f, 30, 30, 30, 30, -1);
                MusicHudRenderer.renderRoundedBorder(context, listX + 6, itemY + 2, 30, 30, 3, isThisPlaying ? 0xAA38BDF8 : 0x33FFFFFF);

                int textLeft = listX + 42;
                int maxTextW = listW - 154;
                if (isThisPlaying) {
                    if (isPlayingNow) {
                        for (int b = 0; b < 3; b++) {
                            float phase = (float)(nowMs % 850L) / 850.0f * 6.2831855f + (float)b * 1.45f;
                            int bh = 3 + Math.round((0.5f + 0.5f * (float)Math.sin(phase)) * 8.0f);
                            int bx = textLeft + b * 4;
                            int by = itemY + 14 - bh;
                            int bCol = b == 0 ? 0xFF10B981 : (b == 1 ? 0xFF38BDF8 : 0xFF818CF8);
                            MusicHudRenderer.renderRoundedRect(context, bx, by, 3, bh, 1, bCol);
                        }
                    } else {
                        MusicHudRenderer.renderRoundedRect(context, textLeft + 1, itemY + 5, 3, 9, 1, 0xFFFBBF24);
                        MusicHudRenderer.renderRoundedRect(context, textLeft + 6, itemY + 5, 3, 9, 1, 0xFFFBBF24);
                    }
                    textLeft += 16;
                    maxTextW -= 16;
                }

                String titleText = this.field_22793.method_27523(track.title, maxTextW);
                String artistText = this.field_22793.method_27523(track.artist, maxTextW);
                context.method_51433(this.field_22793, titleText, textLeft, itemY + 5, isThisPlaying ? 0xFF38BDF8 : 0xFFF8FAFC, true);
                context.method_51433(this.field_22793, artistText, listX + 42, itemY + 18, isHovered ? 0xFFCBD5E1 : 0xFF64748B, false);

                String pBadge = track.platformName != null && !track.platformName.isEmpty() ? track.platformName : "\u041c\u0443\u0437\u044b\u043a\u0430";
                int pColor = this.getPlatformColor(track.platform);
                int badgeW = this.field_22793.method_1727(pBadge) + 8;
                int badgeX = listX + listW - 102 - badgeW;
                if (badgeX > listX + 100) {
                    MusicHudRenderer.renderRoundedRect(context, badgeX, itemY + 10, badgeW, 14, 3, (pColor & 0xFFFFFF) | 0x28000000);
                    MusicHudRenderer.renderRoundedBorder(context, badgeX, itemY + 10, badgeW, 14, 3, (pColor & 0xFFFFFF) | 0x66000000);
                    context.method_51433(this.field_22793, pBadge, badgeX + 4, itemY + 13, pColor | 0xFF000000, false);
                }

                int playBtnX = listX + listW - 96;
                int playBtnY = itemY + 7;
                int playBtnW = 68;
                int playBtnH = 20;
                boolean playHover = mouseX >= playBtnX && mouseX <= playBtnX + playBtnW && mouseY >= playBtnY && mouseY <= playBtnY + playBtnH;
                int playBg = playHover ? 0xEE0284C7 : 0xD80F2942;
                int playBorder = playHover ? 0xFF38BDF8 : 0x880284C7;
                MusicHudRenderer.renderRoundedRect(context, playBtnX, playBtnY, playBtnW, playBtnH, 4, playBg);
                MusicHudRenderer.renderRoundedBorder(context, playBtnX, playBtnY, playBtnW, playBtnH, 4, playBorder);
                context.method_25300(this.field_22793, "\u25b6 \u0421\u043b\u0443\u0448\u0430\u0442\u044c", playBtnX + playBtnW / 2, playBtnY + 6, 0xFFF8FAFC);

                int delBtnX = listX + listW - 24;
                int delBtnY = itemY + 7;
                int delBtnW = 20;
                int delBtnH = 20;
                boolean delHover = mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= delBtnY && mouseY <= delBtnY + delBtnH;
                int delBg = delHover ? 0xEE991B1B : 0xAA161E2E;
                int delBorder = delHover ? 0xFFF87171 : 0x55334155;
                MusicHudRenderer.renderRoundedRect(context, delBtnX, delBtnY, delBtnW, delBtnH, 4, delBg);
                MusicHudRenderer.renderRoundedBorder(context, delBtnX, delBtnY, delBtnW, delBtnH, 4, delBorder);
                context.method_25300(this.field_22793, "\u2716", delBtnX + delBtnW / 2, delBtnY + 6, delHover ? 0xFFFFFFFF : 0xFF94A3B8);
            }
        }
        context.method_44380();
        if (this.maxScroll > 0) {
            int scrollbarX = listX + listW - 3;
            int scrollbarW = 3;
            int scrollbarTrackH = listH;
            float scrollFraction = (float)this.scrollOffset / (float)this.maxScroll;
            int thumbH = Math.max(16, (int)((float)scrollbarTrackH * ((float)listH / (float)(listH + this.maxScroll))));
            int thumbY = listY + (int)((float)(scrollbarTrackH - thumbH) * scrollFraction);
            MusicHudRenderer.renderRoundedRect(context, scrollbarX, listY, scrollbarW, scrollbarTrackH, 1, 0x331E293B);
            MusicHudRenderer.renderRoundedRect(context, scrollbarX, thumbY, scrollbarW, thumbH, 1, 0xAA38BDF8);
        }

        int dockY = panelY + panelHeight - 60;
        int discCx = panelX + 90;
        int discCy = dockY + 20;
        float vinylAngle = isPlayingNow ? (float)(nowMs % 4000L) / 4000.0f * 360.0f : 0.0f;
        context.method_51448().pushMatrix();
        context.method_51448().translate((float)discCx, (float)discCy);
        context.method_51448().rotate((float)Math.toRadians(vinylAngle));
        context.method_25291(class_10799.field_56883, TEX_VINYL, -10, -10, 0.0f, 0.0f, 20, 20, 20, 20, -1);
        context.method_25291(class_10799.field_56883, TEX_SPINDLE, -3, -3, 0.0f, 0.0f, 6, 6, 6, 6, -1);
        context.method_51448().popMatrix();

        int infoX = panelX + 106;
        int infoMaxW = panelWidth - 222;
        if (hasActiveTrack) {
            String nowTitle = this.field_22793.method_27523(curState.title(), infoMaxW);
            String nowArtist = this.field_22793.method_27523(curState.artist(), infoMaxW);
            context.method_51433(this.field_22793, nowTitle, infoX, dockY + 6, 0xFFF8FAFC, true);
            context.method_51433(this.field_22793, nowArtist, infoX, dockY + 17, 0xFF94A3B8, false);
            float prog = class_3532.method_15363(curState.getProgress(), 0.0f, 1.0f);
            MusicHudRenderer.renderProgressBarWithPlayhead(context, infoX, dockY + 30, infoMaxW, 3, prog, 255, 0x38BDF8, nowMs);
        } else {
            context.method_51433(this.field_22793, "\u041d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u043e\u0433\u043e \u0442\u0440\u0435\u043a\u0430", infoX, dockY + 10, 0xFF94A3B8, false);
            MusicHudRenderer.renderRoundedRect(context, infoX, dockY + 26, infoMaxW, 3, 1, 0x441E293B);
        }

        super.method_25394(context, mouseX, mouseY, delta);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        if (click.method_74245() == 0) {
            int panelWidth = Math.min(460, this.field_22789 - 24);
            int panelHeight = this.field_22790 - 40;
            int panelX = (this.field_22789 - panelWidth) / 2;
            int panelY = 20;
            int listX = panelX + 10;
            int listY = panelY + 52;
            int listW = panelWidth - 20;
            int listH = panelHeight - 122;
            int itemH = 36;
            if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
                for (int i = 0; i < this.filteredTracks.size(); ++i) {
                    int itemY = listY + i * itemH - this.scrollOffset;
                    if (itemY + itemH < listY || itemY > listY + listH) continue;
                    FavoriteTrack track = this.filteredTracks.get(i);
                    int playBtnX = listX + listW - 96;
                    int playBtnY = itemY + 7;
                    int playBtnW = 68;
                    int playBtnH = 20;
                    if (mx >= playBtnX && mx <= playBtnX + playBtnW && my >= playBtnY && my <= playBtnY + playBtnH) {
                        FavoritesManager.playTrack(track);
                        if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                            this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)((class_3414)class_3417.field_15015.comp_349()), (float)1.2f, (float)0.7f));
                        }
                        return true;
                    }
                    int delBtnX = listX + listW - 24;
                    int delBtnY = itemY + 7;
                    int delBtnW = 20;
                    int delBtnH = 20;
                    if (mx < delBtnX || mx > delBtnX + delBtnW || my < delBtnY || my > delBtnY + delBtnH) continue;
                    FavoritesManager.INSTANCE.removeFavorite(track.id);
                    this.updateFilteredList(this.searchField.method_1882());
                    return true;
                }
            }
        }
        return super.method_25402(click, bl);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.handleMouseScroll(verticalAmount)) {
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean handleMouseScroll(double amount) {
        if (this.maxScroll > 0) {
            this.scrollOffset = class_3532.method_15340((int)((int)((double)this.scrollOffset - amount * 24.0)), (int)0, (int)this.maxScroll);
            return true;
        }
        return false;
    }

    private int getPlatformColor(String platform) {
        if (platform == null) {
            return -13312;
        }
        return switch (platform.toLowerCase(Locale.ROOT)) {
            case "spotify" -> -14829228;
            case "youtube" -> -65536;
            case "soundcloud" -> -43776;
            case "vk" -> -16746497;
            case "apple" -> -246716;
            case "yandex" -> -13312;
            case "browser" -> -16723201;
            default -> -5197632;
        };
    }

    private class_2960 getThumbnailId(FavoriteTrack track) {
        String idKey;
        if (track == null || track.coverBase64 == null || track.coverBase64.isEmpty()) {
            return CoverTextureManager.DEFAULT_COVER;
        }
        idKey = track.id != null ? track.id : "unknown";
        class_2960 cached = THUMBNAIL_CACHE.get(idKey);
        if (cached != null) {
            return cached;
        }
        class_1011 img = null;
        boolean handedOff = false;
        try {
            class_310 mc = class_310.method_1551();
            if (mc == null || !mc.method_18854()) {
                return CoverTextureManager.DEFAULT_COVER;
            }
            byte[] bytes = Base64.getDecoder().decode(track.coverBase64);
            img = class_1011.method_4309((InputStream)new ByteArrayInputStream(bytes));
            if (img == null) {
                return CoverTextureManager.DEFAULT_COVER;
            }
            class_2960 texUri = class_2960.method_60655((String)"yandexmusichud", (String)("favorites_thumb_" + THUMB_ID_GEN.incrementAndGet()));
            class_1043 texture = new class_1043(() -> "fav_thumb", img);
            handedOff = true;
            mc.method_1531().method_4616(texUri, (class_1044)texture);
            THUMBNAIL_CACHE.put(idKey, texUri);
            return texUri;
        }
        catch (Exception e) {
            if (!handedOff && img != null) {
                try {
                    img.close();
                } catch (Exception ignored) {}
            }
            return CoverTextureManager.DEFAULT_COVER;
        }
    }
}

