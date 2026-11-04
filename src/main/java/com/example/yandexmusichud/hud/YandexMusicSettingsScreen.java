package com.example.yandexmusichud.hud;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.helper.HelperProcessManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
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

public class YandexMusicSettingsScreen extends class_437 {
    public static boolean hasPromptedStartup = false;
    private final class_437 parent;
    private class_342 tokenField;
    private String testStatus = "";
    private int testStatusColor = 0xFFCCCCCC;
    private boolean isTesting = false;
    private final List<AmbientParticle> bgParticles = new ArrayList<AmbientParticle>();
    private final Random random = new Random();
    private boolean particlesInit = false;

    public YandexMusicSettingsScreen(class_437 screen) {
        super((class_2561)class_2561.method_43470("Подключение Яндекс Музыки"));
        this.parent = screen;
    }

    private void initParticles(int n, int n2) {
        if (this.particlesInit) {
            return;
        }
        this.particlesInit = true;
        String[] stringArray = new String[]{"\u266a", "\u266b", "\u2726", "\u2736", "\u25cf"};
        int[] nArray = new int[]{16754470, 16764032, 16766287, 0xFFAA00, 0xFF6600, 65416};
        for (int i = 0; i < 30; ++i) {
            float f = this.random.nextFloat() * (float)n;
            float f2 = this.random.nextFloat() * (float)n2;
            float f3 = (this.random.nextFloat() - 0.5f) * 0.45f;
            float f4 = -0.2f - this.random.nextFloat() * 0.45f;
            float f5 = 0.4f + this.random.nextFloat() * 0.6f;
            int n3 = nArray[this.random.nextInt(nArray.length)];
            String string = stringArray[this.random.nextInt(stringArray.length)];
            this.bgParticles.add(new AmbientParticle(f, f2, f3, f4, f5, n3, string));
        }
    }

    @Override
    protected void method_25426() {
        super.method_25426();
        int n = this.field_22789;
        int n2 = this.field_22790;
        this.initParticles(n, n2);
        int cardW = Math.min(n - 32, 380);
        int cardH = Math.min(210, n2 - 16);
        int cardX = (n - cardW) / 2;
        int cardY = Math.max(8, (n2 - cardH) / 2);

        ModernButtonWidget linkBtn = new ModernButtonWidget(cardX + 16, cardY + 40, cardW - 32, 20, (class_2561)class_2561.method_43470("🔗 Как получить токен? (Инструкция)"), modernButtonWidget -> {
            IntroScreen.openUrl("https://github.com/MarshalX/yandex-music-api/discussions/513");
            this.playClickSound();
        });
        linkBtn.setCustomBgColor(-585353172);
        this.method_37063(linkBtn);

        int fieldH = 18;
        int clearW = 24;
        int pasteW = 54;
        int tokenFieldW = cardW - 32 - pasteW - clearW - 8;
        int fieldY = cardY + 89;

        this.tokenField = new class_342(this.field_22793, cardX + 16, fieldY, tokenFieldW, fieldH, (class_2561)class_2561.method_43470("OAuth Токен"));
        this.tokenField.method_1880(1024);
        this.tokenField.method_1852(sanitizeToken(HudConfig.INSTANCE.yandexMusicToken != null ? HudConfig.INSTANCE.yandexMusicToken : ""));
        this.tokenField.method_1863(newText -> {
            if (newText != null && (newText.contains("&") || newText.contains("access_token=") || newText.toLowerCase().startsWith("oauth ") || newText.toLowerCase().startsWith("bearer "))) {
                String clean = sanitizeToken(newText);
                if (!clean.equals(newText)) {
                    this.tokenField.method_1852(clean);
                }
            }
        });
        this.method_37063(this.tokenField);

        ModernButtonWidget pasteBtn = new ModernButtonWidget(cardX + 16 + tokenFieldW + 4, fieldY, pasteW, fieldH, (class_2561)class_2561.method_43470("📋 Встав."), modernButtonWidget -> {
            String cb = "";
            try {
                cb = GLFW.glfwGetClipboardString(class_310.method_1551().method_22683().method_4490());
            } catch (Throwable ignored) {}
            if (cb != null && !cb.isEmpty()) {
                this.tokenField.method_1852(sanitizeToken(cb));
                this.playClickSound();
            }
        });
        pasteBtn.setCustomBgColor(-585408968);
        this.method_37063(pasteBtn);

        ModernButtonWidget clearBtn = new ModernButtonWidget(cardX + 16 + tokenFieldW + 4 + pasteW + 4, fieldY, clearW, fieldH, (class_2561)class_2561.method_43470("✕"), modernButtonWidget -> {
            this.tokenField.method_1852("");
            this.playClickSound();
        });
        clearBtn.setCustomBgColor(-583392728);
        this.method_37063(clearBtn);

        ModernButtonWidget testBtn = new ModernButtonWidget(cardX + 16, cardY + 114, 130, 18, (class_2561)class_2561.method_43470("⚡ Проверить токен"), modernButtonWidget -> this.runConnectionTest());
        testBtn.setCustomBgColor(-586012100);
        this.method_37063(testBtn);

        int splitBtnW = (cardW - 32 - 6) / 2;
        ModernButtonWidget startupToggleBtn = new ModernButtonWidget(cardX + 16, cardY + 138, splitBtnW, 18, (class_2561)class_2561.method_43470("При запуске: " + (HudConfig.INSTANCE.promptYandexMusicOnStartup ? "§aВКЛ" : "§7ВЫКЛ")), b -> {
            HudConfig.INSTANCE.promptYandexMusicOnStartup = !HudConfig.INSTANCE.promptYandexMusicOnStartup;
            b.method_25355((class_2561)class_2561.method_43470("При запуске: " + (HudConfig.INSTANCE.promptYandexMusicOnStartup ? "§aВКЛ" : "§7ВЫКЛ")));
            b.setCustomBgColor(HudConfig.INSTANCE.promptYandexMusicOnStartup ? -585408968 : -583392728);
            HudConfig.save();
            this.playClickSound();
        });
        startupToggleBtn.setCustomBgColor(HudConfig.INSTANCE.promptYandexMusicOnStartup ? -585408968 : -583392728);
        this.method_37063(startupToggleBtn);

        ModernButtonWidget tabBadgeBtn = new ModernButtonWidget(cardX + 16 + splitBtnW + 6, cardY + 138, splitBtnW, 18, (class_2561)class_2561.method_43470("Иконка в Tab: " + (HudConfig.INSTANCE.tabBadgeEnabled ? "§aВКЛ" : "§7ВЫКЛ")), b -> {
            HudConfig.INSTANCE.tabBadgeEnabled = !HudConfig.INSTANCE.tabBadgeEnabled;
            b.method_25355((class_2561)class_2561.method_43470("Иконка в Tab: " + (HudConfig.INSTANCE.tabBadgeEnabled ? "§aВКЛ" : "§7ВЫКЛ")));
            b.setCustomBgColor(HudConfig.INSTANCE.tabBadgeEnabled ? -585408968 : -583392728);
            HudConfig.save();
            this.playClickSound();
        });
        tabBadgeBtn.setCustomBgColor(HudConfig.INSTANCE.tabBadgeEnabled ? -585408968 : -583392728);
        this.method_37063(tabBadgeBtn);

        int actionBtnW = (cardW - 32 - 8) / 2;
        int actionBtnY = cardY + cardH - 26;

        ModernButtonWidget saveBtn = new ModernButtonWidget(cardX + 16, actionBtnY, actionBtnW, 20, (class_2561)class_2561.method_43470("💾 Сохранить"), modernButtonWidget -> this.saveAndClose());
        saveBtn.setCustomBgColor(-585408968);
        this.method_37063(saveBtn);

        ModernButtonWidget cancelBtn = new ModernButtonWidget(cardX + 16 + actionBtnW + 8, actionBtnY, actionBtnW, 20, (class_2561)class_2561.method_43470("Отмена"), modernButtonWidget -> this.closeScreen());
        cancelBtn.setCustomBgColor(-583392728);
        this.method_37063(cancelBtn);
    }

    public static String sanitizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.startsWith("'") && s.endsWith("'") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.toLowerCase().startsWith("oauth ")) {
            s = s.substring(6).trim();
        } else if (s.toLowerCase().startsWith("bearer ")) {
            s = s.substring(7).trim();
        }
        int tokenIdx = s.indexOf("access_token=");
        if (tokenIdx != -1) {
            s = s.substring(tokenIdx + 13);
        } else if (s.contains("token=") && !s.contains("token_type=")) {
            int altTokenIdx = s.indexOf("token=");
            s = s.substring(altTokenIdx + 6);
        }
        int ampIdx = s.indexOf('&');
        if (ampIdx != -1) {
            s = s.substring(0, ampIdx);
        }
        int hashIdx = s.indexOf('#');
        if (hashIdx != -1) {
            s = s.substring(0, hashIdx);
        }
        int qIdx = s.indexOf('?');
        if (qIdx != -1) {
            s = s.substring(0, qIdx);
        }
        int semiIdx = s.indexOf(';');
        if (semiIdx != -1) {
            s = s.substring(0, semiIdx);
        }
        int spaceIdx = s.indexOf(' ');
        if (spaceIdx != -1) {
            s = s.substring(0, spaceIdx);
        }
        return s.trim();
    }

    private void runConnectionTest() {
        if (this.isTesting) {
            return;
        }
        String raw = this.tokenField != null ? this.tokenField.method_1882().trim() : "";
        String token = sanitizeToken(raw);
        if (!token.equals(raw) && this.tokenField != null) {
            this.tokenField.method_1852(token);
        }
        if (token.isEmpty()) {
            this.testStatus = "§c✕ Токен не указан!";
            this.testStatusColor = 0xFFFF5555;
            this.playClickSound();
            return;
        }
        this.isTesting = true;
        this.testStatus = "§e⏳ Проверка...";
        this.testStatusColor = 0xFFFFAA00;
        this.playClickSound();

        CompletableFuture.runAsync(() -> {
            boolean tokenOk = false;
            String tokenUser = "";
            try {
                URL u = URI.create("https://api.music.yandex.net/account/status").toURL();
                HttpURLConnection conn = (HttpURLConnection)u.openConnection();
                conn.setRequestProperty("Authorization", "OAuth " + token);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    tokenOk = true;
                    try (InputStream is = conn.getInputStream()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        if (json.has("result") && json.get("result").isJsonObject()) {
                            JsonObject res = json.getAsJsonObject("result");
                            if (res.has("account") && res.get("account").isJsonObject()) {
                                JsonObject acc = res.getAsJsonObject("account");
                                if (acc.has("displayName") && !acc.get("displayName").isJsonNull()) {
                                    tokenUser = acc.get("displayName").getAsString();
                                } else if (acc.has("fullName") && !acc.get("fullName").isJsonNull()) {
                                    tokenUser = acc.get("fullName").getAsString();
                                } else if (acc.has("login") && !acc.get("login").isJsonNull()) {
                                    tokenUser = acc.get("login").getAsString();
                                }
                            }
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {}

            if (tokenOk) {
                this.testStatus = "§a✔ " + (!tokenUser.isEmpty() ? tokenUser : "OK");
                this.testStatusColor = 0xFF55FF55;
            } else {
                this.testStatus = "§c✕ Ошибка токена";
                this.testStatusColor = 0xFFFF5555;
            }
            this.isTesting = false;
        });
    }

    private void saveAndClose() {
        if (this.tokenField != null) {
            HudConfig.INSTANCE.yandexMusicToken = sanitizeToken(this.tokenField.method_1882());
        }
        HudConfig.save();
        this.playClickSound();
        this.closeScreen();
    }

    private void closeScreen() {
        if (this.field_22787 != null) {
            if (!HudConfig.INSTANCE.initialSetupCompleted) {
                this.field_22787.method_1507((class_437)new InitialSetupScreen(this.parent));
            } else if (this.parent != null && !(this.parent instanceof YandexMusicSettingsScreen)) {
                this.field_22787.method_1507(this.parent);
            } else {
                this.field_22787.method_1507((class_437)new net.minecraft.class_442());
            }
        }
    }

    private void playClickSound() {
        try {
            if (this.field_22787 != null && this.field_22787.method_1483() != null) {
                this.field_22787.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)class_3417.field_15234, 1.0f, 0.8f));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int w = this.field_22789;
        int h = this.field_22790;
        context.method_25294(0, 0, w, h, 0xCC090C12);

        float dt60 = Math.min(3.0f, delta);
        for (AmbientParticle p : this.bgParticles) {
            p.update(w, h, dt60);
            int a = (int)(p.alpha * 130.0f);
            if (a > 3) {
                context.method_51433(this.field_22793, p.symbol, (int)p.x, (int)p.y, a << 24 | (p.color & 0xFFFFFF), false);
            }
        }

        class_327 f = this.field_22793;
        int cardW = Math.min(w - 32, 380);
        int cardH = Math.min(210, h - 16);
        int cardX = (w - cardW) / 2;
        int cardY = Math.max(8, (h - cardH) / 2);

        MusicHudRenderer.renderRoundedRect(context, cardX, cardY, cardW, cardH, 8, 0xEE121624);
        MusicHudRenderer.renderRoundedBorder(context, cardX, cardY, cardW, cardH, 8, 0xFFFFAA00, 0x55FFAA00);

        context.method_51433(f, "§6✦ ТОКЕН ЯНДЕКС МУЗЫКИ", cardX + 16, cardY + 12, -1, true);
        context.method_51433(f, "§7Авторизация для получения треков и караоке", cardX + 16, cardY + 25, 0xFF888899, false);

        String stepHint = f.method_27523("§e1. §7Откройте ссылку  §8•  §e2. §7Скопируйте токен  §8•  §e3. §7Вставьте сюда", Math.max(80, cardW - 32));
        context.method_51433(f, stepHint, cardX + 16, cardY + 64, 0xFFCCCCCC, false);
        context.method_51433(f, "§fOAuth Токен (y0_...):", cardX + 16, cardY + 77, -1, false);

        if (!this.testStatus.isEmpty()) {
            context.method_51433(f, this.testStatus, cardX + 152, cardY + 119, this.testStatusColor, true);
        } else {
            context.method_51433(f, "§8(нажмите для проверки)", cardX + 152, cardY + 119, 0xFF666677, false);
        }

        super.method_25394(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean method_25404(class_11908 input) {
        int keyCode = input.comp_4795();
        if (keyCode == 256) {
            this.closeScreen();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            this.saveAndClose();
            return true;
        }
        return super.method_25404(input);
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private static class AmbientParticle {
        float x;
        float y;
        float vx;
        float vy;
        float alpha;
        int color;
        String symbol;

        AmbientParticle(float f, float f2, float f3, float f4, float f5, int n, String string) {
            this.x = f;
            this.y = f2;
            this.vx = f3;
            this.vy = f4;
            this.alpha = f5;
            this.color = n;
            this.symbol = string;
        }

        void update(int n, int n2, float dt60) {
            this.x += this.vx * dt60;
            this.y += this.vy * dt60;
            if (this.y < -10.0f) {
                this.y = n2 + 10;
            }
            if (this.x < -10.0f) {
                this.x = n + 10;
            }
            if (this.x > (float)(n + 10)) {
                this.x = -10.0f;
            }
        }

        void update(int n, int n2) {
            this.update(n, n2, 1.0f);
        }
    }
}
