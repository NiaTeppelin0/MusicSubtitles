package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.lyrics.CustomFontManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.class_10799;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_11719;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_342;
import net.minecraft.class_437;
import net.minecraft.class_5250;

public class FontSelectorScreen extends class_437 {
    private final class_437 parent;
    private final Consumer<String> onSelectFont;
    private String selectedFontId;
    private class_342 searchField;
    private String searchQuery = "";
    private String selectedCategory = "ALL";
    private final List<CustomFontManager.FontInfo> filteredFonts = new ArrayList<CustomFontManager.FontInfo>();
    private float scrollOffset = 0.0f;
    private float targetScroll = 0.0f;
    private boolean isDraggingScrollbar = false;
    private double lastDragMouseY = 0.0;
    private String toastMessage = null;
    private long toastTime = 0L;
    private long lastClickTime = 0L;
    private String lastClickedId = null;

    private ModernButtonWidget tabAll;
    private ModernButtonWidget tabRu;
    private ModernButtonWidget tabFile;
    private ModernButtonWidget tabSys;
    private ModernButtonWidget tabMc;

    public FontSelectorScreen(class_437 parent, String currentFontId, Consumer<String> onSelectFont) {
        super((class_2561)class_2561.method_43470("Выбор шрифта"));
        this.parent = parent;
        this.selectedFontId = (currentFontId != null && !currentFontId.isEmpty()) ? currentFontId : "minecraft:default";
        this.onSelectFont = onSelectFont;
    }

    public FontSelectorScreen(class_437 parent, Consumer<String> onSelectFont) {
        this(parent, "minecraft:default", onSelectFont);
    }

    public FontSelectorScreen(class_437 parent) {
        this(parent, com.example.yandexmusichud.config.HudConfig.INSTANCE.subtitlesFont, font -> {
            com.example.yandexmusichud.config.HudConfig.INSTANCE.subtitlesFont = font;
            com.example.yandexmusichud.config.HudConfig.save();
        });
    }

    protected void method_25426() {
        super.method_25426();
        CustomFontManager.init();

        int screenW = this.field_22789;
        int screenH = this.field_22790;

        int headerY = 28;
        int searchW = Math.min(130, screenW / 4);
        int searchX = 14;

        this.searchField = new class_342(this.field_22793, searchX, headerY, searchW, 16, (class_2561)class_2561.method_43470("Поиск"));
        this.searchField.method_1880(50);
        this.searchField.method_1852(this.searchQuery);
        this.searchField.method_1863(query -> {
            this.searchQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
            this.targetScroll = 0.0f;
            this.scrollOffset = 0.0f;
            this.updateFilteredList();
        });
        this.method_37063(this.searchField);

        int tabX = searchX + searchW + 6;
        int tabH = 16;
        boolean compact = screenW < 600;
        int wAll = compact ? 28 : 40;
        int wRu = compact ? 32 : 58;
        int wFile = compact ? 38 : 68;
        int wSys = compact ? 34 : 62;
        int wMc = compact ? 32 : 52;
        int curX = tabX;

        this.tabAll = new ModernButtonWidget(curX, headerY, wAll, tabH, (class_2561)class_2561.method_43470("Все"), b -> {
            this.selectedCategory = "ALL";
            this.updateTabsVisual();
            this.targetScroll = 0.0f;
            this.scrollOffset = 0.0f;
            this.updateFilteredList();
        });
        this.method_37063(this.tabAll);
        curX += wAll + 3;

        this.tabRu = new ModernButtonWidget(curX, headerY, wRu, tabH, (class_2561)class_2561.method_43470(compact ? "RU" : "С русским"), b -> {
            this.selectedCategory = "RU";
            this.updateTabsVisual();
            this.targetScroll = 0.0f;
            this.scrollOffset = 0.0f;
            this.updateFilteredList();
        });
        this.method_37063(this.tabRu);
        curX += wRu + 3;

        this.tabFile = new ModernButtonWidget(curX, headerY, wFile, tabH, (class_2561)class_2561.method_43470(compact ? ".ttf" : "Файлы .ttf"), b -> {
            this.selectedCategory = "FILE";
            this.updateTabsVisual();
            this.targetScroll = 0.0f;
            this.scrollOffset = 0.0f;
            this.updateFilteredList();
        });
        this.method_37063(this.tabFile);
        curX += wFile + 3;

        this.tabSys = new ModernButtonWidget(curX, headerY, wSys, tabH, (class_2561)class_2561.method_43470(compact ? "Win" : "Windows"), b -> {
            this.selectedCategory = "SYSTEM";
            this.updateTabsVisual();
            this.targetScroll = 0.0f;
            this.scrollOffset = 0.0f;
            this.updateFilteredList();
        });
        this.method_37063(this.tabSys);
        curX += wSys + 3;

        this.tabMc = new ModernButtonWidget(curX, headerY, wMc, tabH, (class_2561)class_2561.method_43470("Майн"), b -> {
            this.selectedCategory = "MC";
            this.updateTabsVisual();
            this.targetScroll = 0.0f;
            this.scrollOffset = 0.0f;
            this.updateFilteredList();
        });
        this.method_37063(this.tabMc);

        int rightBtnW = 54;
        ModernButtonWidget bFolder = new ModernButtonWidget(screenW - 14 - rightBtnW * 2 - 20, headerY, rightBtnW + 14, tabH, (class_2561)class_2561.method_43470("📂 Папка"), b -> {
            CustomFontManager.openFontsFolder();
            this.toastMessage = "Папка открыта: config/yandexmusichud/fonts";
            this.toastTime = System.currentTimeMillis();
            this.playSound(class_3417.field_14709, 1.3f, 0.7f);
        });
        this.method_37063(bFolder);

        ModernButtonWidget bReload = new ModernButtonWidget(screenW - 14 - rightBtnW, headerY, rightBtnW, tabH, (class_2561)class_2561.method_43470("🔄 Обнов."), b -> {
            CustomFontManager.reload();
            this.updateFilteredList();
            this.toastMessage = "Список шрифтов обновлён!";
            this.toastTime = System.currentTimeMillis();
            this.playSound(class_3417.field_14709, 1.4f, 0.7f);
        });
        this.method_37063(bReload);

        int footerY = screenH - 28;
        int applyW = screenW < 600 ? 96 : 120;
        int cancelW = screenW < 600 ? 74 : 90;
        int applyX = screenW < 600 ? screenW - 14 - cancelW - 6 - applyW : screenW / 2 - applyW - 4;
        int cancelX = screenW < 600 ? screenW - 14 - cancelW : screenW / 2 + 4;
        ModernButtonWidget bApply = new ModernButtonWidget(applyX, footerY, applyW, 20, (class_2561)class_2561.method_43470("Применить"), b -> {
            this.applyAndClose();
        });
        bApply.setCustomBgColor(0xDD1B5E38);
        this.method_37063(bApply);

        ModernButtonWidget bCancel = new ModernButtonWidget(cancelX, footerY, cancelW, 20, (class_2561)class_2561.method_43470("Отмена"), b -> {
            this.closeScreen();
        });
        bCancel.setCustomBgColor(0xDD3A2228);
        this.method_37063(bCancel);

        this.updateTabsVisual();
        this.updateFilteredList();
    }

    private void updateTabsVisual() {
        if (this.tabAll != null) {
            this.tabAll.setCustomBgColor("ALL".equals(this.selectedCategory) ? 0xDD00838F : 0xAA1C2232);
        }
        if (this.tabRu != null) {
            this.tabRu.setCustomBgColor("RU".equals(this.selectedCategory) ? 0xDD00695C : 0xAA1C2232);
        }
        if (this.tabFile != null) {
            this.tabFile.setCustomBgColor("FILE".equals(this.selectedCategory) ? 0xDDBF360C : 0xAA1C2232);
        }
        if (this.tabSys != null) {
            this.tabSys.setCustomBgColor("SYSTEM".equals(this.selectedCategory) ? 0xDD0D47A1 : 0xAA1C2232);
        }
        if (this.tabMc != null) {
            this.tabMc.setCustomBgColor("MC".equals(this.selectedCategory) ? 0xDD1B5E20 : 0xAA1C2232);
        }
    }

    private void updateFilteredList() {
        this.filteredFonts.clear();
        List<CustomFontManager.FontInfo> all = CustomFontManager.getAllFonts();
        for (CustomFontManager.FontInfo fi : all) {
            if ("RU".equals(this.selectedCategory) && !fi.supportsCyrillic) {
                continue;
            }
            if ("SYSTEM".equals(this.selectedCategory) && (fi.isMinecraft || !fi.id.startsWith("sys:"))) {
                continue;
            }
            if ("FILE".equals(this.selectedCategory) && (fi.isMinecraft || !fi.id.startsWith("file:"))) {
                continue;
            }
            if ("MC".equals(this.selectedCategory) && !fi.isMinecraft) {
                continue;
            }
            if (!this.searchQuery.isEmpty()) {
                String nameLower = fi.name.toLowerCase(Locale.ROOT);
                String idLower = fi.id.toLowerCase(Locale.ROOT);
                if (!nameLower.contains(this.searchQuery) && !idLower.contains(this.searchQuery)) {
                    continue;
                }
            }
            this.filteredFonts.add(fi);
        }
    }

    private void applyAndClose() {
        if (this.onSelectFont != null && this.selectedFontId != null) {
            this.onSelectFont.accept(this.selectedFontId);
        }
        this.closeScreen();
    }

    private void closeScreen() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int screenW = this.field_22789;
        int screenH = this.field_22790;

        context.method_25294(0, 0, screenW, screenH, 0xF2080A12);
        context.method_25296(0, 0, screenW, 50, 0x5500E5FF, 0x00000000);

        class_327 tr = this.field_22793;
        String title = "ВЫБОР ШРИФТА СУБТИТРОВ";
        context.method_51433(tr, title, 14, 10, 0xFFFFFFFF, true);
        String countStr = "Найдено шрифтов: " + this.filteredFonts.size();
        context.method_51433(tr, countStr, 175, 11, 0xFF88A0B8, false);

        int viewportY = 48;
        int footerH = 34;
        int viewportH = screenH - viewportY - footerH;
        int cardW = Math.min(screenW - 28, 480);
        int cardX = (screenW - cardW) / 2;
        int cardH = 42;

        float maxScroll = Math.max(0.0f, (float)(this.filteredFonts.size() * cardH - viewportH));
        this.targetScroll = Math.max(0.0f, Math.min(this.targetScroll, maxScroll));
        this.scrollOffset += (this.targetScroll - this.scrollOffset) * 0.35f;

        context.method_25294(0, viewportY - 1, screenW, viewportY, 0x33446688);

        context.method_44379(cardX - 4, viewportY, cardX + cardW + 14, viewportY + viewportH);

        for (int i = 0; i < this.filteredFonts.size(); i++) {
            CustomFontManager.FontInfo info = this.filteredFonts.get(i);
            int itemY = viewportY + (int)((float)(i * cardH) - this.scrollOffset);
            if (itemY + cardH < viewportY || itemY > viewportY + viewportH) {
                continue;
            }

            boolean isSelected = info.id.equalsIgnoreCase(this.selectedFontId);
            boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= itemY && mouseY <= itemY + cardH - 3 && mouseY >= viewportY && mouseY <= viewportY + viewportH;

            int bgCol = isSelected ? 0xDD122A3C : (isHovered ? 0xDD182232 : 0xAA0E121E);
            MusicHudRenderer.renderRoundedRect(context, cardX, itemY, cardW, cardH - 3, 4, bgCol);

            int borderCol = isSelected ? 0xFF00E5FF : (isHovered ? 0x8800B0FF : 0x33283648);
            int borderCol2 = isSelected ? 0xFF0090B0 : (isHovered ? 0x88007099 : 0x22182230);
            MusicHudRenderer.renderRoundedBorder(context, cardX, itemY, cardW, cardH - 3, 4, borderCol, borderCol2);

            String srcBadge;
            int srcBg;
            int srcFg;
            if (info.isMinecraft) {
                srcBadge = "MC";
                srcBg = 0xFF1B5E20;
                srcFg = 0xFFA5D6A7;
            } else if (info.id.startsWith("file:")) {
                srcBadge = "TTF";
                srcBg = 0xFFBF360C;
                srcFg = 0xFFFFCCBC;
            } else {
                srcBadge = "WIN";
                srcBg = 0xFF0D47A1;
                srcFg = 0xFF90CAF9;
            }

            int b1W = tr.method_1727(srcBadge) + 6;
            MusicHudRenderer.renderRoundedRect(context, cardX + 6, itemY + 5, b1W, 11, 2, srcBg);
            context.method_51433(tr, srcBadge, cardX + 9, itemY + 6, srcFg, false);

            int b2X = cardX + 6 + b1W + 3;
            String langBadge = info.supportsCyrillic ? "RU+EN" : "EN";
            int langBg = info.supportsCyrillic ? 0xFF004D40 : 0xFF3E2723;
            int langFg = info.supportsCyrillic ? 0xFF80CBC4 : 0xFFFFCC80;
            int b2W = tr.method_1727(langBadge) + 6;
            MusicHudRenderer.renderRoundedRect(context, b2X, itemY + 5, b2W, 11, 2, langBg);
            context.method_51433(tr, langBadge, b2X + 3, itemY + 6, langFg, false);

            int textX = b2X + b2W + 5;
            String nameText = info.name;
            if (tr.method_1727(nameText) > 155) {
                while (nameText.length() > 3 && tr.method_1727(nameText + "..") > 155) {
                    nameText = nameText.substring(0, nameText.length() - 1);
                }
                nameText = nameText + "..";
            }
            int nameCol = isSelected ? 0xFF00E5FF : (isHovered ? 0xFFFFFFFF : 0xFFE0E6F0);
            context.method_51433(tr, nameText, textX, itemY + 6, nameCol, true);

            int nameW = tr.method_1727(nameText);
            String idStr = info.id;
            if (cardW > 390) {
                context.method_51433(tr, idStr, textX + nameW + 8, itemY + 7, 0xFF6C7C94, false);
            }

            int btnW = 54;
            int btnH = 18;
            int btnX = cardX + cardW - btnW - 6;
            int btnY = itemY + 10;
            if (isSelected) {
                MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 3, 0xEE00838F);
                context.method_51433(tr, "✓ Выбран", btnX + 5, btnY + 5, 0xFFFFFFFF, true);
            } else {
                boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
                int bCol = btnHover ? 0xDD2A4060 : 0xAA1C283E;
                MusicHudRenderer.renderRoundedRect(context, btnX, btnY, btnW, btnH, 3, bCol);
                MusicHudRenderer.renderRoundedBorder(context, btnX, btnY, btnW, btnH, 3, btnHover ? 0xFF00E5FF : 0x443A4C68, 0x221A2A40);
                context.method_51433(tr, "Выбрать", btnX + 8, btnY + 5, btnHover ? 0xFFFFFFFF : 0xFFB0C4DE, false);
            }

            int prevX = cardX + 8;
            int prevY = itemY + 20;
            int prevMaxW = btnX - prevX - 6;
            if (info.isMinecraft) {
                String sample = info.supportsCyrillic ? "Съешь ещё этих мягких булок - 123 AaBb" : "The quick brown fox jumps - 123 AaBb";
                class_5250 sampleComp = class_2561.method_43470(sample);
                try {
                    class_2960 fontId = class_2960.method_60654(info.id);
                    sampleComp.method_10862(class_2583.field_24360.method_27704(new class_11719.class_11721(fontId)));
                } catch (Exception ignored) {}
                context.method_51439(tr, sampleComp, prevX, prevY + 2, isSelected ? 0xFFFFFFFF : 0xFFD2DCE8, false);
            } else {
                class_2960 prevTex = CustomFontManager.getPreviewTexture(info);
                if (prevTex != null) {
                    try {
                        int texW = 1200;
                        int texH = 64;
                        int drawH = 15;
                        float scale = (float)drawH / (float)texH;
                        int maxRegionW = (int)((float)prevMaxW / scale);
                        int regionW = Math.min(texW, maxRegionW);
                        int drawW = (int)((float)regionW * scale);
                        context.method_25302(class_10799.field_56883, prevTex, prevX, prevY + 1, 0.0f, 0.0f, drawW, drawH, regionW, texH, texW, texH);
                    } catch (Throwable ignored) {}
                }
            }
        }

        context.method_44380();

        if (maxScroll > 0.0f) {
            int scrollX = cardX + cardW + 4;
            context.method_25294(scrollX, viewportY, scrollX + 3, viewportY + viewportH, 0x33101420);
            float thumbRatio = (float)viewportH / (float)(this.filteredFonts.size() * cardH);
            int thumbH = Math.max(16, (int)((float)viewportH * thumbRatio));
            int thumbY = viewportY + (int)((this.scrollOffset / maxScroll) * (float)(viewportH - thumbH));
            context.method_25294(scrollX, thumbY, scrollX + 3, thumbY + thumbH, this.isDraggingScrollbar ? 0xFF00E5FF : 0xAA446688);
        }

        int footerY = screenH - footerH;
        context.method_25294(0, footerY - 1, screenW, footerY, 0x33446688);
        context.method_25294(0, footerY, screenW, screenH, 0xF00B0E18);

        int applyX = screenW < 600 ? screenW - 14 - 74 - 6 - 96 : screenW / 2 - 124;
        String selDisplay = "Выбран шрифт: §b" + CustomFontManager.getDisplayName(this.selectedFontId) + " §7(" + this.selectedFontId + ")";
        String trimmedSel = tr.method_27523(selDisplay, Math.max(60, applyX - 22));
        context.method_51433(tr, trimmedSel, 14, footerY + 10, 0xFFFFFFFF, false);

        super.method_25394(context, mouseX, mouseY, delta);

        if (this.toastMessage != null) {
            long age = System.currentTimeMillis() - this.toastTime;
            if (age < 3500L) {
                int toastAlpha = age < 3000L ? 255 : (int)((3500L - age) * 255L / 500L);
                int tW = tr.method_1727(this.toastMessage) + 16;
                int tX = (screenW - tW) / 2;
                int tY = screenH - 58;
                MusicHudRenderer.renderRoundedRect(context, tX, tY, tW, 20, 4, toastAlpha << 24 | 0x141A28);
                MusicHudRenderer.renderRoundedBorder(context, tX, tY, tW, 20, 4, toastAlpha << 24 | 0x00E5FF, toastAlpha << 24 | 0x0088AA);
                context.method_51433(tr, this.toastMessage, tX + 8, tY + 6, toastAlpha << 24 | 0xFFFFFF, true);
            } else {
                this.toastMessage = null;
            }
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int cardH = 42;
        int screenH = this.field_22790;
        int viewportH = screenH - 48 - 34;
        float maxScroll = Math.max(0.0f, (float)(this.filteredFonts.size() * cardH - viewportH));
        this.targetScroll -= (float)verticalAmount * 28.0f;
        this.targetScroll = Math.max(0.0f, Math.min(this.targetScroll, maxScroll));
        return true;
    }

    public boolean method_25402(class_11909 click, boolean bl) {
        int mx = (int)click.comp_4798();
        int my = (int)click.comp_4799();
        if (click.method_74245() == 0) {
            int screenW = this.field_22789;
            int screenH = this.field_22790;
            int cardW = Math.min(screenW - 28, 480);
            int cardX = (screenW - cardW) / 2;
            int cardH = 42;
            int viewportY = 48;
            int footerH = 34;
            int viewportH = screenH - viewportY - footerH;

            int scrollX = cardX + cardW + 2;
            if (mx >= scrollX && mx <= scrollX + 12 && my >= viewportY && my <= viewportY + viewportH) {
                this.isDraggingScrollbar = true;
                this.lastDragMouseY = (double)my;
                return true;
            }

            if (mx >= cardX && mx <= cardX + cardW && my >= viewportY && my <= viewportY + viewportH) {
                int clickedIndex = (int)(((float)my - (float)viewportY + this.scrollOffset) / (float)cardH);
                if (clickedIndex >= 0 && clickedIndex < this.filteredFonts.size()) {
                    CustomFontManager.FontInfo clicked = this.filteredFonts.get(clickedIndex);
                    long now = System.currentTimeMillis();
                    boolean isDouble = (now - this.lastClickTime < 350L && clicked.id.equals(this.lastClickedId));
                    this.lastClickTime = now;
                    this.lastClickedId = clicked.id;
                    this.selectedFontId = clicked.id;
                    this.playSound(class_3417.field_14709, 1.4f, 0.7f);

                    int itemY = viewportY + (int)((float)(clickedIndex * cardH) - this.scrollOffset);
                    int btnW = 54;
                    int btnH = 18;
                    int btnX = cardX + cardW - btnW - 6;
                    int btnY = itemY + 10;
                    boolean clickedBtn = (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH);

                    if (clickedBtn || isDouble) {
                        this.applyAndClose();
                        return true;
                    }
                    return true;
                }
            }
        }
        return super.method_25402(click, bl);
    }

    public boolean method_25406(class_11909 click) {
        if (click.method_74245() == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.method_25406(click);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        if (this.isDraggingScrollbar) {
            int screenH = this.field_22790;
            int viewportH = screenH - 48 - 34;
            int cardH = 42;
            float maxScroll = Math.max(0.0f, (float)(this.filteredFonts.size() * cardH - viewportH));
            if (maxScroll > 0.0f) {
                float scrollDelta = (float)deltaY * (maxScroll / (float)viewportH);
                this.targetScroll = Math.max(0.0f, Math.min(this.targetScroll + scrollDelta, maxScroll));
                this.scrollOffset = this.targetScroll;
            }
            return true;
        }
        return super.method_25403(click, deltaX, deltaY);
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256) {
            this.closeScreen();
            return true;
        }
        if (input.comp_4795() == 257 || input.comp_4795() == 335) {
            this.applyAndClose();
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25421() {
        return false;
    }

    private void playSound(class_3414 sound, float pitch, float volume) {
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.method_1483() != null && sound != null) {
            mc.method_1483().method_4873((class_1113)class_1109.method_4757(sound, pitch, volume));
        }
    }
}
