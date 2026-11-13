package com.example.yandexmusichud.texture;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.texture.CoverTextureManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_156;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;

public class CustomBackgroundManager {
    public static final Path BACKGROUNDS_DIR = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("backgrounds");
    private static final class_2960 DYNAMIC_BG_ID = class_2960.method_60655((String)"yandexmusichud", (String)"dynamic/custom_background");
    public static final CustomBackgroundManager INSTANCE = new CustomBackgroundManager();
    private final List<String> validBackgrounds = new ArrayList<String>();
    private final List<InvalidFileEntry> invalidFiles = new ArrayList<InvalidFileEntry>();
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicSubtitles-CustomBgLoader");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger loadGeneration = new AtomicInteger(0);
    private volatile class_2960 currentTextureId = null;
    private volatile String loadedFileName = "";
    private volatile String loadingRequestKey = "";
    private volatile String failedFileName = "";
    private volatile long failedFileCheckTime = 0L;
    private volatile int loadedCornerRadius = -1;
    private volatile int loadedBaseHeight = -1;
    private long lastScanTime = 0L;
    private boolean alertDismissed = false;

    public CustomBackgroundManager() {
        this.ensureDirectoryExists();
        this.scanBackgrounds();
    }

    public void ensureDirectoryExists() {
        try {
            Path oldReadme;
            Path readme;
            if (!Files.exists(BACKGROUNDS_DIR, new LinkOption[0])) {
                Files.createDirectories(BACKGROUNDS_DIR, new FileAttribute[0]);
            }
            if (!Files.exists(readme = BACKGROUNDS_DIR.resolve("README.txt"), new LinkOption[0])) {
                String text = "===============================================================\r\n\u041f\u0410\u041f\u041a\u0410 \u0414\u041b\u042f \u041a\u0410\u0421\u0422\u041e\u041c\u041d\u042b\u0425 \u0424\u041e\u041d\u041e\u0412 HUD\r\n===============================================================\r\n- \u041f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u043c\u044b\u0435 \u0444\u043e\u0440\u043c\u0430\u0442\u044b: .PNG, .JPG, .JPEG, .BMP\r\n- \u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u0435\u043c\u043e\u0435 \u0441\u043e\u043e\u0442\u043d\u043e\u0448\u0435\u043d\u0438\u0435 \u0441\u0442\u043e\u0440\u043e\u043d: 260x74 (\u0438\u043b\u0438 \u0448\u0438\u0440\u043e\u043a\u043e\u0444\u043e\u0440\u043c\u0430\u0442\u043d\u043e\u0435).\r\n- \u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435 \u0432 \u043c\u0435\u043d\u044e \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a (\u043a\u043b\u0430\u0432\u0438\u0448\u0430 O -> \u0412\u0438\u0434\u0436\u0435\u0442 -> \u0424\u043e\u043d).\r\n===============================================================\r\n";
                Files.writeString(readme, (CharSequence)text, new OpenOption[0]);
            }
            if (Files.exists(oldReadme = BACKGROUNDS_DIR.resolve("README_\u0412\u0410\u0416\u041d\u041e_\u0422\u041e\u041b\u042c\u041a\u041e_PNG.txt"), new LinkOption[0])) {
                Files.deleteIfExists(oldReadme);
            }
        }
        catch (Exception exception) {

        }
    }

    public static String detectFileFormat(byte[] header) {
        if (header.length >= 8 && (header[0] & 0xFF) == 137 && header[1] == 80 && header[2] == 78 && header[3] == 71 && header[4] == 13 && header[5] == 10 && header[6] == 26 && header[7] == 10) {
            return "PNG";
        }
        if (header.length >= 3 && (header[0] & 0xFF) == 255 && (header[1] & 0xFF) == 216 && (header[2] & 0xFF) == 255) {
            return "JPEG";
        }
        if (header.length >= 12 && header[0] == 82 && header[1] == 73 && header[2] == 70 && header[3] == 70 && header[8] == 87 && header[9] == 69 && header[10] == 66 && header[11] == 80) {
            return "WEBP";
        }
        if (header.length >= 4 && header[0] == 71 && header[1] == 73 && header[2] == 70 && header[3] == 56) {
            return "GIF";
        }
        if (header.length >= 2 && header[0] == 66 && header[1] == 77) {
            return "BMP";
        }
        return "UNKNOWN";
    }

    public synchronized void scanBackgrounds() {
        this.ensureDirectoryExists();
        this.validBackgrounds.clear();
        this.invalidFiles.clear();
        File dir = BACKGROUNDS_DIR.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) continue;
            String name = f.getName();
            String lower = name.toLowerCase(Locale.ROOT);
            if (name.equalsIgnoreCase("README_\u0412\u0410\u0416\u041d\u041e_\u0422\u041e\u041b\u042c\u041a\u041e_PNG.txt") || name.equalsIgnoreCase("README.txt") || lower.endsWith(".txt") || lower.endsWith(".md")) continue;
            byte[] header = new byte[16];
            int readBytes = 0;
            try (FileInputStream fis2 = new FileInputStream(f);){
                readBytes = fis2.read(header);
            }
            catch (Exception fis2) {

            }
            String detected = readBytes >= 2 ? CustomBackgroundManager.detectFileFormat(header) : "EMPTY";
            if ("WEBP".equals(detected) || lower.endsWith(".webp")) {
                this.invalidFiles.add(new InvalidFileEntry(name, "\u0424\u043e\u0440\u043c\u0430\u0442 WebP \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442\u0441\u044f. \u041f\u0435\u0440\u0435\u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u0435 \u043a\u0430\u043a .PNG \u0438\u043b\u0438 .JPG.", false));
                continue;
            }
            boolean isKnownImage = "PNG".equals(detected) || "JPEG".equals(detected) || "BMP".equals(detected) || "GIF".equals(detected) || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".jfif") || lower.endsWith(".bmp");
            if (isKnownImage && readBytes > 0) {
                this.validBackgrounds.add(name);
                continue;
            }
            if (readBytes <= 0) {
                this.invalidFiles.add(new InvalidFileEntry(name, "\u0424\u0430\u0439\u043b \u043f\u0443\u0441\u0442 (0 \u0431\u0430\u0439\u0442).", false));
                continue;
            }
            this.invalidFiles.add(new InvalidFileEntry(name, "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439 \u0444\u043e\u0440\u043c\u0430\u0442 \u0444\u0430\u0439\u043b\u0430. \u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435 .PNG, .JPG \u0438\u043b\u0438 .BMP", false));
        }
        this.lastScanTime = System.currentTimeMillis();
        if (this.invalidFiles.isEmpty()) {
            this.alertDismissed = false;
        }
    }

    public void checkBackgroundsThrottled() {
    }

    public synchronized boolean hasInvalidFiles() {
        return !this.invalidFiles.isEmpty();
    }

    public synchronized boolean isAlertActive() {
        return !this.invalidFiles.isEmpty() && !this.alertDismissed;
    }

    public synchronized void dismissAlert() {
        this.alertDismissed = true;
    }

    public synchronized void resetAlertDismissal() {
        this.alertDismissed = false;
    }

    public synchronized List<InvalidFileEntry> getInvalidFiles() {
        return new ArrayList<InvalidFileEntry>(this.invalidFiles);
    }

    public synchronized InvalidFileEntry getFirstInvalidFile() {
        return this.invalidFiles.isEmpty() ? null : this.invalidFiles.get(0);
    }

    public synchronized List<String> getAvailableBackgrounds() {
        this.scanBackgrounds();
        return new ArrayList<String>(this.validBackgrounds);
    }

    public void openBackgroundsFolder() {
        this.ensureDirectoryExists();
        com.example.yandexmusichud.favorites.FavoritesManager.openFolderSafely(BACKGROUNDS_DIR);
    }

    private static Path resolveSafeBackgroundPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        try {
            Path base = BACKGROUNDS_DIR.toAbsolutePath().normalize();
            Path resolved = base.resolve(fileName).normalize();
            if (!resolved.startsWith(base) || resolved.equals(base)) {
                return null;
            }
            return resolved;
        }
        catch (Exception e) {
            return null;
        }
    }

    public boolean autoConvertFile(String fileName) {
        this.ensureDirectoryExists();
        Path p = CustomBackgroundManager.resolveSafeBackgroundPath(fileName);
        if (p == null || !Files.exists(p, new LinkOption[0])) {
            return false;
        }
        Path backup = null;
        try {
            BufferedImage bimg = ImageIO.read(p.toFile());
            if (bimg == null) {
                return false;
            }
            String baseName = fileName;
            if (baseName.contains(".")) {
                baseName = baseName.substring(0, baseName.lastIndexOf(46));
            }
            String targetName = baseName + ".png";
            Path targetPath = CustomBackgroundManager.resolveSafeBackgroundPath(targetName);
            if (targetPath == null) {
                return false;
            }
            if (targetPath.equals(p)) {
                backup = CustomBackgroundManager.resolveSafeBackgroundPath(baseName + "_old_temp.dat");
                if (backup == null) {
                    return false;
                }
                Files.move(p, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                boolean ok = ImageIO.write((RenderedImage)bimg, "PNG", targetPath.toFile());
                if (!ok || !Files.exists(targetPath, new LinkOption[0]) || Files.size(targetPath) == 0L) {
                    Files.move(backup, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return false;
                }
                Files.deleteIfExists(backup);
                backup = null;
            } else {
                Path tmpTarget = CustomBackgroundManager.resolveSafeBackgroundPath(baseName + "_conv_temp.png");
                if (tmpTarget == null) {
                    return false;
                }
                boolean ok = ImageIO.write((RenderedImage)bimg, "PNG", tmpTarget.toFile());
                if (!ok || !Files.exists(tmpTarget, new LinkOption[0]) || Files.size(tmpTarget) == 0L) {
                    Files.deleteIfExists(tmpTarget);
                    return false;
                }
                Files.move(tmpTarget, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(p);
            }
            this.scanBackgrounds();
            HudConfig.INSTANCE.customBackgroundFile = targetName;
            HudConfig.save();
            this.reload();
            return true;
        }
        catch (Exception e) {
            if (backup != null && Files.exists(backup, new LinkOption[0])) {
                try {
                    Files.move(backup, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
            return false;
        }
    }

    public class_2960 getBackgroundTexture() {
        String configuredFile = HudConfig.INSTANCE.customBackgroundFile;
        if (configuredFile == null || configuredFile.trim().isEmpty()) {
            if (this.currentTextureId != null || !this.loadedFileName.isEmpty() || !this.loadingRequestKey.isEmpty()) {
                this.unload();
            }
            return null;
        }
        int curRadius = class_3532.method_15340((int)HudConfig.INSTANCE.cornerRadius, (int)0, (int)20);
        int curBaseH = HudConfig.INSTANCE.getHudBaseHeight();
        if (configuredFile.equals(this.failedFileName) && System.currentTimeMillis() - this.failedFileCheckTime < 2500L) {
            return null;
        }
        if (!configuredFile.equals(this.loadedFileName) || curRadius != this.loadedCornerRadius || curBaseH != this.loadedBaseHeight) {
            this.loadBackground(configuredFile, curRadius, curBaseH);
        }
        return this.currentTextureId;
    }

    public void loadBackground(String fileName) {
        int curRadius = class_3532.method_15340((int)HudConfig.INSTANCE.cornerRadius, (int)0, (int)20);
        int curBaseH = HudConfig.INSTANCE.getHudBaseHeight();
        this.loadBackground(fileName, curRadius, curBaseH);
    }

    private void loadBackground(String fileName, int r, int baseH) {
        String reqKey = fileName + "|" + r + "|" + baseH;
        if (reqKey.equals(this.loadingRequestKey)) {
            return;
        }
        this.loadingRequestKey = reqKey;
        int gen = this.loadGeneration.incrementAndGet();
        this.loader.submit(() -> {
            if (gen != this.loadGeneration.get()) {
                return;
            }
            this.ensureDirectoryExists();
            Path filePath = CustomBackgroundManager.resolveSafeBackgroundPath(fileName);
            if (filePath == null || !Files.exists(filePath, new LinkOption[0])) {
                this.failedFileName = fileName;
                this.failedFileCheckTime = System.currentTimeMillis();
                if (reqKey.equals(this.loadingRequestKey)) {
                    this.loadingRequestKey = "";
                }
                this.unload();
                return;
            }
            class_1011 image = null;
            try (FileInputStream in2 = new FileInputStream(filePath.toFile());){
                image = class_1011.method_4309((InputStream)in2);
            }
            catch (Exception in2) {

            }
            if (image == null) {
                try {
                    BufferedImage bimg = ImageIO.read(filePath.toFile());
                    if (bimg != null) {
                        image = new class_1011(bimg.getWidth(), bimg.getHeight(), false);
                        for (int y = 0; y < bimg.getHeight(); ++y) {
                            for (int x = 0; x < bimg.getWidth(); ++x) {
                                int argb = bimg.getRGB(x, y);
                                int a = argb >> 24 & 0xFF;
                                int red = argb >> 16 & 0xFF;
                                int g = argb >> 8 & 0xFF;
                                int b = argb & 0xFF;
                                image.method_61941(x, y, a << 24 | red << 16 | g << 8 | b);
                            }
                        }
                    }
                }
                catch (Exception bimg) {

                }
            }
            if (image == null) {
                this.failedFileName = fileName;
                this.failedFileCheckTime = System.currentTimeMillis();
                if (reqKey.equals(this.loadingRequestKey)) {
                    this.loadingRequestKey = "";
                }
                this.unload();
                return;
            }
            if (gen != this.loadGeneration.get()) {
                image.close();
                return;
            }
            if (r > 0 && baseH > 0) {
                int pxRadius = (int)((float)r * ((float)image.method_4323() / (float)baseH));
                CoverTextureManager.roundImageCorners(image, pxRadius);
            }
            if (gen != this.loadGeneration.get()) {
                image.close();
                return;
            }
            class_1011 finalImage = image;
            Runnable bindAction = () -> {
                class_1043 texture = null;
                try {
                    if (gen != this.loadGeneration.get()) {
                        finalImage.close();
                        return;
                    }
                    if (this.currentTextureId != null) {
                        class_310.method_1551().method_1531().method_4615(DYNAMIC_BG_ID);
                    }
                    texture = new class_1043(() -> "custom_background", finalImage);
                    class_310.method_1551().method_1531().method_4616(DYNAMIC_BG_ID, (class_1044)texture);
                    this.currentTextureId = DYNAMIC_BG_ID;
                    this.loadedFileName = fileName;
                    this.failedFileName = "";
                    this.loadedCornerRadius = r;
                    this.loadedBaseHeight = baseH;
                    if (reqKey.equals(this.loadingRequestKey)) {
                        this.loadingRequestKey = "";
                    }
                }
                catch (Exception e) {
                    if (texture != null) {
                        texture.close();
                    } else {
                        finalImage.close();
                    }
                    if (reqKey.equals(this.loadingRequestKey)) {
                        this.loadingRequestKey = "";
                    }
                    this.unload();
                }
            };
            class_310 mc = class_310.method_1551();
            if (mc == null) {
                finalImage.close();
                return;
            }
            if (RenderSystem.isOnRenderThread()) {
                bindAction.run();
            } else {
                mc.execute(bindAction);
            }
        });
    }

    public void reload() {
        this.failedFileName = "";
        this.failedFileCheckTime = 0L;
        this.loadedFileName = "";
        this.loadingRequestKey = "";
        String configuredFile = HudConfig.INSTANCE.customBackgroundFile;
        if (configuredFile != null && !configuredFile.isEmpty()) {
            this.loadBackground(configuredFile);
        } else {
            this.unload();
        }
    }

    public void unload() {
        int unloadGen = this.loadGeneration.incrementAndGet();
        this.loadingRequestKey = "";
        Runnable unbindAction = () -> {
            if (unloadGen != this.loadGeneration.get()) {
                return;
            }
            if (this.currentTextureId != null) {
                class_310.method_1551().method_1531().method_4615(DYNAMIC_BG_ID);
                this.currentTextureId = null;
            }
            this.loadedFileName = "";
            this.loadedCornerRadius = -1;
            this.loadedBaseHeight = -1;
        };
        if (RenderSystem.isOnRenderThread()) {
            unbindAction.run();
        } else {
            class_310 mc = class_310.method_1551();
            if (mc != null) {
                mc.execute(unbindAction);
            } else {
                this.currentTextureId = null;
                this.loadedFileName = "";
                this.loadedCornerRadius = -1;
                this.loadedBaseHeight = -1;
            }
        }
    }

    public void shutdown() {
        this.unload();
        this.loader.shutdownNow();
    }

    public record InvalidFileEntry(String fileName, String reason, boolean canAutoConvert) {
    }
}

