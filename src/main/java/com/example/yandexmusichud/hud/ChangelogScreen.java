package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.hud.HudEditScreen;
import com.example.yandexmusichud.hud.LyricEditorScreen;
import com.example.yandexmusichud.hud.ModernButtonWidget;
import com.example.yandexmusichud.hud.MusicHudRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_437;

public class ChangelogScreen extends class_437 {
    private final class_437 parent;
    private final List<ChangeEntry> entries = new ArrayList<ChangeEntry>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;

    public ChangelogScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470("Music Subtitles 2.8 Changelog"));
        this.parent = parent;
        this.initEntries();
    }

    private void initEntries() {
        this.entries.clear();
        this.entries.add(new ChangeEntry(
            "1. Совместное прослушивание (Music Sync)",
            "Слушайте музыку синхронно с друзьями на любых серверах через ультрабыстрый релей.",
            "СЕТЬ & РЕЛЕЙ",
            "♫",
            0xFF00F5D4
        ));
        this.entries.add(new ChangeEntry(
            "2. Комнаты с паролями и управление",
            "Создание защищённых сессий, 5-значные коды, кик и бан нежелательных слушателей.",
            "КОМНАТЫ",
            "🔒",
            0xFF10B981
        ));
        this.entries.add(new ChangeEntry(
            "3. Modern Glassmorphic UI",
            "Стильный стеклянный неоновый дизайн окон, адаптивные текстовые поля и кнопки.",
            "ИНТЕРФЕЙС",
            "✨",
            0xFF38BDF8
        ));
        this.entries.add(new ChangeEntry(
            "4. Вкладка детальной диагностики",
            "Мониторинг рассинхрона в реальном времени, акустический micro-glide и сетевой пинг.",
            "ДИАГНОСТИКА",
            "⚡",
            0xFFF59E0B
        ));
        this.entries.add(new ChangeEntry(
            "5. Умная пауза спринта в субтитрах",
            "При остановке во время бега режим субтитров мягко ждёт секунду перед переключением.",
            "СУБТИТРЫ",
            "🏃",
            0xFFA855F7
        ));
        this.entries.add(new ChangeEntry(
            "6. Динамические плашки подсказок",
            "Тултипы автоматически подстраивают ширину под длину текста без обрезания строк.",
            "УДОБСТВО",
            "✦",
            0xFF06B6D4
        ));
        this.entries.add(new ChangeEntry(
            "7. Настройка отображения в Табе",
            "Быстрое включение/отключение видимости мода в списке игроков прямо после интро.",
            "ТАБЛИЦА ИГРОКОВ",
            "👥",
            0xFFFFCC00
        ));
        this.entries.add(new ChangeEntry(
            "8. Очистка кэша и оптимизация",
            "Оптимизированный рендеринг, мгновенная очистка кэша потоков и стабильный звук.",
            "ОПТИМИЗАЦИЯ",
            "🚀",
            0xFFE11D48
        ));
    }

    protected void method_25426() {
        super.method_25426();
        int panelW = Math.min(520, this.field_22789 - 16);
        int panelH = Math.min(390, this.field_22790 - 20);
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = (this.field_22790 - panelH) / 2;
        int bottomY = panelY + panelH - 28;

        int btnGap = 6;
        int btnW = (panelW - 24 - btnGap * 3) / 4;
        int btnH = 20;

        ModernButtonWidget btnYm = new ModernButtonWidget(panelX + 12, bottomY, btnW, btnH, (class_2561)class_2561.method_43470("§6♪ Яндекс API"), b -> {
            this.playSound(class_3417.field_14709, 1.2f, 0.7f);
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new YandexMusicSettingsScreen(this));
            }
        });
        btnYm.setCustomBgColor(0xDD3A2E1A);
        this.method_37063(btnYm);

        ModernButtonWidget btnEditor = new ModernButtonWidget(panelX + 12 + btnW + btnGap, bottomY, btnW, btnH, (class_2561)class_2561.method_43470("§b♫ Редактор"), b -> {
            this.playSound(class_3417.field_14709, 1.2f, 0.7f);
            HudConfig.INSTANCE.lastSeenChangelogVersion = "2.8";
            HudConfig.save();
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new LyricEditorScreen(this));
            }
        });
        btnEditor.setCustomBgColor(0xDD172B3E);
        this.method_37063(btnEditor);

        ModernButtonWidget btnConfig = new ModernButtonWidget(panelX + 12 + (btnW + btnGap) * 2, bottomY, btnW, btnH, (class_2561)class_2561.method_43470("§e⚙ Настройки"), b -> {
            this.playSound(class_3417.field_14709, 1.2f, 0.7f);
            HudConfig.INSTANCE.lastSeenChangelogVersion = "2.8";
            HudConfig.save();
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new HudEditScreen(this));
            }
        });
        btnConfig.setCustomBgColor(0xDD2D2B1B);
        this.method_37063(btnConfig);

        ModernButtonWidget btnClose = new ModernButtonWidget(panelX + 12 + (btnW + btnGap) * 3, bottomY, btnW, btnH, (class_2561)class_2561.method_43470("§a✔ В игру!"), b -> {
            this.closeScreen();
        });
        btnClose.setPrimary(true);
        this.method_37063(btnClose);

        int closeXBtnW = 18;
        int closeXBtnH = 16;
        ModernButtonWidget btnX = new ModernButtonWidget(panelX + panelW - closeXBtnW - 8, panelY + 8, closeXBtnW, closeXBtnH, (class_2561)class_2561.method_43470("✕"), b -> {
            this.closeScreen();
        });
        btnX.setCustomBgColor(0xDD442228);
        this.method_37063(btnX);
    }

    private void closeScreen() {
        this.playSound(class_3417.field_14709, 1.0f, 0.7f);
        HudConfig.INSTANCE.lastSeenChangelogVersion = "2.8";
        HudConfig.save();
        if (this.field_22787 != null) {
            if (HudConfig.INSTANCE.promptYandexMusicOnStartup) {
                YandexMusicSettingsScreen.hasPromptedStartup = true;
                this.field_22787.method_1507((class_437)new YandexMusicSettingsScreen(this.parent));
            } else if (!HudConfig.INSTANCE.initialSetupCompleted) {
                this.field_22787.method_1507((class_437)new InitialSetupScreen(this.parent));
            } else {
                this.field_22787.method_1507(this.parent);
            }
        }
    }

    public void method_25419() {
        this.closeScreen();
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll > 0) {
            this.scrollOffset = class_3532.method_15340((int)(this.scrollOffset - (int)(verticalAmount * 28.0)), 0, this.maxScroll);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        int panelW = Math.min(520, this.field_22789 - 16);
        int panelH = Math.min(390, this.field_22790 - 20);
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = (this.field_22790 - panelH) / 2;
        int listX = panelX + 12;
        int listW = panelW - 24;
        int listY = panelY + 42;
        int listH = panelH - 76;

        if (this.maxScroll > 0 && click.comp_4800() != null && click.comp_4800().comp_4801() == 0 && mx >= (double)(listX + listW + 1) && mx <= (double)(listX + listW + 8) && my >= (double)listY && my <= (double)(listY + listH)) {
            this.isDraggingScrollbar = true;
            this.updateScrollFromMouse(my, listY, listH);
            return true;
        }
        return super.method_25402(click, bl);
    }

    public boolean method_25406(class_11909 click) {
        this.isDraggingScrollbar = false;
        return super.method_25406(click);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        if (this.isDraggingScrollbar && this.maxScroll > 0) {
            int panelH = Math.min(390, this.field_22790 - 20);
            int panelY = (this.field_22790 - panelH) / 2;
            int listY = panelY + 42;
            int listH = panelH - 76;
            this.updateScrollFromMouse(click.comp_4799(), listY, listH);
            return true;
        }
        return super.method_25403(click, deltaX, deltaY);
    }

    private void updateScrollFromMouse(double mouseY, int listY, int listH) {
        float ratio = (float)(mouseY - (double)listY) / (float)listH;
        this.scrollOffset = class_3532.method_15340(Math.round(ratio * (float)this.maxScroll), 0, this.maxScroll);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        MusicHudRenderer.renderRoundedRect(context, 0, 0, this.field_22789, this.field_22790, 0, 0x9506080E);

        class_327 tr = this.field_22793;
        int panelW = Math.min(520, this.field_22789 - 16);
        int panelH = Math.min(390, this.field_22790 - 20);
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = (this.field_22790 - panelH) / 2;

        MusicHudRenderer.renderRoundedRect(context, panelX, panelY, panelW, panelH, 10, 0xF40B0E18);
        MusicHudRenderer.renderRoundedBorder(context, panelX, panelY, panelW, panelH, 10, 0x8800E5FF, 0x33446688);

        int badgeW = 44;
        int badgeH = 14;
        int badgeX = panelX + 12;
        int badgeY = panelY + 11;
        MusicHudRenderer.renderRoundedRect(context, badgeX, badgeY, badgeW, badgeH, 4, 0xDDFFB300);
        context.method_51433(tr, "v2.8", badgeX + 10, badgeY + 3, 0xFF111111, false);

        String mainTitle = "§6✦ Что нового в обновлении §e2.8 §6✦";
        context.method_51433(tr, mainTitle, badgeX + badgeW + 8, panelY + 10, -1, true);

        String subTitle = tr.method_27523("§7Совместное прослушивание, комнаты с паролем, стеклянный дизайн и оптимизация", Math.max(80, panelW - (badgeW + 28)));
        context.method_51433(tr, subTitle, badgeX + badgeW + 8, panelY + 23, 0x99AABB, false);

        int listX = panelX + 12;
        int listY = panelY + 42;
        int listW = panelW - 24;
        int listH = panelH - 76;

        int itemH = 42;
        int itemGap = 5;
        int totalContentH = this.entries.size() * (itemH + itemGap) - itemGap;
        this.maxScroll = Math.max(0, totalContentH - listH);
        this.scrollOffset = class_3532.method_15340(this.scrollOffset, 0, this.maxScroll);

        context.method_44379(listX, listY, listX + listW, listY + listH);
        int curY = listY - this.scrollOffset;

        for (int i = 0; i < this.entries.size(); ++i) {
            ChangeEntry entry = this.entries.get(i);
            int boxY = curY;
            if (boxY + itemH >= listY && boxY <= listY + listH) {
                boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= boxY && mouseY <= boxY + itemH;
                int bg = hovered ? 0xEE162035 : 0xDD101422;
                int border = hovered ? (0xFF000000 | entry.accentColor) : 0x33445577;

                MusicHudRenderer.renderRoundedRect(context, listX, boxY, listW, itemH, 6, bg);
                MusicHudRenderer.renderRoundedBorder(context, listX, boxY, listW, itemH, 6, border, border);

                MusicHudRenderer.renderRoundedRect(context, listX, boxY + 2, 3, itemH - 4, 2, entry.accentColor);

                int iconBoxSize = 26;
                int iconBoxX = listX + 7;
                int iconBoxY = boxY + (itemH - iconBoxSize) / 2;
                int iconBg = 0x33000000 | (entry.accentColor & 0x22FFFFFF);
                MusicHudRenderer.renderRoundedRect(context, iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 5, iconBg);
                MusicHudRenderer.renderRoundedBorder(context, iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 5, 0x44000000 | entry.accentColor, 0x44000000 | entry.accentColor);

                int iconW = tr.method_1727(entry.icon);
                context.method_51433(tr, entry.icon, iconBoxX + (iconBoxSize - iconW) / 2, iconBoxY + 6, entry.accentColor, false);

                int textStartX = iconBoxX + iconBoxSize + 8;

                int badgeTagW = tr.method_1727(entry.categoryBadge) + 8;
                int badgeTagX = listX + listW - badgeTagW - 8;
                int badgeTagY = boxY + 6;
                MusicHudRenderer.renderRoundedRect(context, badgeTagX, badgeTagY, badgeTagW, 12, 3, 0x33000000 | (entry.accentColor & 0x25FFFFFF));
                MusicHudRenderer.renderRoundedBorder(context, badgeTagX, badgeTagY, badgeTagW, 12, 3, entry.accentColor, entry.accentColor);
                context.method_51433(tr, entry.categoryBadge, badgeTagX + 4, badgeTagY + 2, entry.accentColor, false);

                if (entry.description != null && !entry.description.isEmpty()) {
                    context.method_51433(tr, "§f§l" + entry.title, textStartX, boxY + 6, -1, true);
                    int maxDescW = badgeTagX - textStartX - 6;
                    String displayDesc = tr.method_27523(entry.description, maxDescW);
                    context.method_51433(tr, "§7— §e" + displayDesc, textStartX, boxY + 20, 0xDDEEFF, false);
                } else {
                    context.method_51433(tr, "§f§l" + entry.title, textStartX, boxY + 16, -1, true);
                }
            }
            curY += itemH + itemGap;
        }
        context.method_44380();

        if (this.maxScroll > 0) {
            int scrollbarW = 4;
            int scrollbarX = listX + listW + 3;
            int scrollbarH = listH;
            float thumbRatio = (float)listH / (float)totalContentH;
            int thumbH = Math.max(20, (int)((float)scrollbarH * thumbRatio));
            int thumbY = listY + (int)((float)(scrollbarH - thumbH) * ((float)this.scrollOffset / (float)this.maxScroll));
            MusicHudRenderer.renderRoundedRect(context, scrollbarX, listY, scrollbarW, scrollbarH, 2, 0x22FFFFFF);
            int thumbColor = this.isDraggingScrollbar ? 0xFF00E5FF : 0xAA00E5FF;
            MusicHudRenderer.renderRoundedRect(context, scrollbarX, thumbY, scrollbarW, thumbH, 2, thumbColor);
        }

        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void playSound(class_3414 sound, float pitch, float volume) {
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.method_1483() != null && sound != null) {
            mc.method_1483().method_4873((class_1113)class_1109.method_4757(sound, pitch, volume));
        }
    }

    private static class ChangeEntry {
        final String title;
        final String description;
        final String categoryBadge;
        final String icon;
        final int accentColor;

        ChangeEntry(String title, String description, String categoryBadge, String icon, int accentColor) {
            this.title = title;
            this.description = description;
            this.categoryBadge = categoryBadge;
            this.icon = icon;
            this.accentColor = accentColor;
        }
    }
}
