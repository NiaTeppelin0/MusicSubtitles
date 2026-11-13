package com.example.yandexmusichud.local;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class LocalMusicTrack {
    private final Path path;
    private final String filename;
    private final String title;
    private final String artist;
    private final String album;
    private final long durationMs;
    private final String format;
    private final Path lrcPath;
    private final byte[] coverArt;

    public LocalMusicTrack(Path path, String filename, String title, String artist, String album, long durationMs, String format, Path lrcPath, byte[] coverArt) {
        this.path = path;
        this.filename = filename != null ? filename : "";
        this.title = title != null && !title.isBlank() ? title.trim() : (filename != null ? filename : "Без названия");
        this.artist = artist != null && !artist.isBlank() ? artist.trim() : "Моя музыка";
        this.album = album != null && !album.isBlank() ? album.trim() : "Локальный альбом";
        this.durationMs = Math.max(0L, durationMs);
        this.format = format != null ? format.toUpperCase(java.util.Locale.ROOT) : "AUDIO";
        this.lrcPath = lrcPath;
        this.coverArt = coverArt;
    }

    public Path getPath() {
        return this.path;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getTitle() {
        return this.title;
    }

    public String getArtist() {
        return this.artist;
    }

    public String getAlbum() {
        return this.album;
    }

    public long getDurationMs() {
        return this.durationMs;
    }

    public String getFormat() {
        return this.format;
    }

    public Path getLrcPath() {
        return this.lrcPath;
    }

    public byte[] getCoverArt() {
        return this.coverArt;
    }

    public boolean hasLyrics() {
        return this.lrcPath != null && Files.exists(this.lrcPath);
    }

    public String getFormattedDuration() {
        long sec = this.durationMs / 1000L;
        long m = sec / 60L;
        long s = sec % 60L;
        return String.format("%02d:%02d", m, s);
    }

    public static LocalMusicTrack fromFile(Path path) {
        String fname = path.getFileName().toString();
        String lower = fname.toLowerCase(java.util.Locale.ROOT);
        String fmt = "MP3";
        if (lower.endsWith(".wav")) {
            fmt = "WAV";
        } else if (lower.endsWith(".ogg")) {
            fmt = "OGG";
        }

        String baseName = fname.replaceFirst("(?i)\\.(mp3|wav|ogg)$", "").trim();
        com.example.yandexmusichud.network.TrackMetadataSanitizer.SanitizedMetadata meta = com.example.yandexmusichud.network.TrackMetadataSanitizer.sanitize(baseName, "", "");
        String parsedTitle = !meta.cleanTitle().isEmpty() ? meta.cleanTitle() : baseName;
        String parsedArtist = !meta.cleanArtist().isEmpty() && !meta.cleanArtist().equalsIgnoreCase("Музыка") ? meta.cleanArtist() : "Моя музыка";

        String parsedAlbum = "Локальный альбом";
        long parsedDuration = 0L;
        byte[] extractedCover = null;

        if ("MP3".equals(fmt)) {
            try {
                byte[] head = new byte[10];
                try (InputStream is = Files.newInputStream(path)) {
                    int r = is.read(head);
                    if (r == 10 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
                        int vMajor = head[3] & 0xFF;
                        int flags = head[5] & 0xFF;
                        int tagSize = ((head[6] & 0x7F) << 21) | ((head[7] & 0x7F) << 14) | ((head[8] & 0x7F) << 7) | (head[9] & 0x7F);
                        if (tagSize > 0 && tagSize <= 25000000) {
                            byte[] tagData = is.readNBytes(tagSize);
                            int offset = 0;
                            if ((flags & 0x40) != 0 && tagData.length >= 4) {
                                int extSize = vMajor == 4
                                    ? (((tagData[0] & 0x7F) << 21) | ((tagData[1] & 0x7F) << 14) | ((tagData[2] & 0x7F) << 7) | (tagData[3] & 0x7F))
                                    : (((tagData[0] & 0xFF) << 24) | ((tagData[1] & 0xFF) << 16) | ((tagData[2] & 0xFF) << 8) | (tagData[3] & 0xFF));
                                if (extSize > 0 && extSize < tagData.length) {
                                    offset += vMajor == 4 ? extSize : (extSize + 4);
                                }
                            }
                            int headerLen = vMajor == 2 ? 6 : 10;
                            while (offset + headerLen <= tagData.length) {
                                if (tagData[offset] == 0) {
                                    break;
                                }
                                String frameId;
                                int frameSize;
                                if (vMajor == 2) {
                                    frameId = new String(tagData, offset, 3, StandardCharsets.ISO_8859_1);
                                    frameSize = ((tagData[offset + 3] & 0xFF) << 16) | ((tagData[offset + 4] & 0xFF) << 8) | (tagData[offset + 5] & 0xFF);
                                    offset += 6;
                                } else {
                                    frameId = new String(tagData, offset, 4, StandardCharsets.ISO_8859_1);
                                    if (vMajor == 4) {
                                        frameSize = ((tagData[offset + 4] & 0x7F) << 21) | ((tagData[offset + 5] & 0x7F) << 14) | ((tagData[offset + 6] & 0x7F) << 7) | (tagData[offset + 7] & 0x7F);
                                    } else {
                                        frameSize = ((tagData[offset + 4] & 0xFF) << 24) | ((tagData[offset + 5] & 0xFF) << 16) | ((tagData[offset + 6] & 0xFF) << 8) | (tagData[offset + 7] & 0xFF);
                                    }
                                    offset += 10;
                                }
                                if (frameSize <= 0 || offset + frameSize > tagData.length) {
                                    break;
                                }
                                byte[] frameBytes = Arrays.copyOfRange(tagData, offset, offset + frameSize);
                                offset += frameSize;

                                if ("TIT2".equals(frameId) || "TT2".equals(frameId)) {
                                    String text = decodeTextFrame(frameBytes);
                                    if (text != null && !text.isBlank()) {
                                        parsedTitle = text.trim();
                                    }
                                } else if ("TPE1".equals(frameId) || "TP1".equals(frameId)) {
                                    String text = decodeTextFrame(frameBytes);
                                    if (text != null && !text.isBlank()) {
                                        parsedArtist = text.trim();
                                    }
                                } else if ("TALB".equals(frameId) || "TAL".equals(frameId)) {
                                    String text = decodeTextFrame(frameBytes);
                                    if (text != null && !text.isBlank()) {
                                        parsedAlbum = text.trim();
                                    }
                                } else if ("TLEN".equals(frameId) || "TLE".equals(frameId)) {
                                    String text = decodeTextFrame(frameBytes);
                                    if (text != null) {
                                        try {
                                            parsedDuration = Long.parseLong(text.trim());
                                        } catch (Exception ignored) {}
                                    }
                                } else if (("APIC".equals(frameId) || "PIC".equals(frameId)) && extractedCover == null) {
                                    extractedCover = decodeApicFrame(frameBytes, "PIC".equals(frameId));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (parsedDuration <= 0L) {
                try (InputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
                    javazoom.jl.decoder.Bitstream bs = new javazoom.jl.decoder.Bitstream(bis);
                    javazoom.jl.decoder.Header h = bs.readFrame();
                    if (h != null) {
                        long fileSize = Files.size(path);
                        long audioBytes = Math.max(1024L, fileSize - (long)Math.max(0, bs.header_pos()));
                        float ms = h.total_ms((int)Math.min((long)Integer.MAX_VALUE, audioBytes));
                        if (ms > 0) {
                            parsedDuration = (long)ms;
                        }
                    }
                    bs.close();
                } catch (Exception ignored) {}
            }
        } else if ("WAV".equals(fmt)) {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(path.toFile())) {
                AudioFormat af = ais.getFormat();
                long frames = ais.getFrameLength();
                if (frames > 0 && af.getFrameRate() > 0) {
                    parsedDuration = (long)((frames / (double)af.getFrameRate()) * 1000.0);
                }
            } catch (Exception ignored) {}
        } else if ("OGG".equals(fmt)) {
            ByteBuffer nativeOgg = null;
            long dec = 0L;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                byte[] oggBytes = Files.readAllBytes(path);
                if (oggBytes.length > 0) {
                    nativeOgg = MemoryUtil.memAlloc(oggBytes.length);
                    nativeOgg.put(oggBytes).flip();
                    IntBuffer err = stack.mallocInt(1);
                    dec = STBVorbis.stb_vorbis_open_memory(nativeOgg, err, null);
                    if (dec != 0L) {
                        float s = STBVorbis.stb_vorbis_stream_length_in_seconds(dec);
                        if (s > 0.0f) {
                            parsedDuration = (long)(s * 1000.0f);
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (dec != 0L) {
                    try {
                        STBVorbis.stb_vorbis_close(dec);
                    } catch (Exception ignored) {}
                }
                if (nativeOgg != null) {
                    MemoryUtil.memFree(nativeOgg);
                }
            }
        }

        Path companionLrc = null;
        try {
            Path sib = path.resolveSibling(baseName + ".lrc");
            if (Files.exists(sib)) {
                companionLrc = sib;
            } else {
                Path global = FabricLoader.getInstance().getConfigDir().resolve("yandexmusichud").resolve("lyrics").resolve(baseName + ".lrc");
                if (Files.exists(global)) {
                    companionLrc = global;
                }
            }
        } catch (Exception ignored) {}

        return new LocalMusicTrack(path, fname, parsedTitle, parsedArtist, parsedAlbum, parsedDuration, fmt, companionLrc, extractedCover);
    }

    private static String decodeTextFrame(byte[] bytes) {
        if (bytes == null || bytes.length <= 1) {
            return null;
        }
        int enc = bytes[0] & 0xFF;
        Charset charset = StandardCharsets.ISO_8859_1;
        int start = 1;
        if (enc == 1) {
            charset = StandardCharsets.UTF_16;
        } else if (enc == 2) {
            charset = StandardCharsets.UTF_16BE;
        } else if (enc == 3) {
            charset = StandardCharsets.UTF_8;
        }
        int len = bytes.length - start;
        if (enc == 1 || enc == 2) {
            while (len >= 2 && bytes[start + len - 2] == 0 && bytes[start + len - 1] == 0) {
                len -= 2;
            }
            if ((len & 1) == 1 && bytes[start + len - 1] == 0) {
                len--;
            }
        } else {
            while (len > 0 && bytes[start + len - 1] == 0) {
                len--;
            }
        }
        if (len <= 0) {
            return null;
        }
        if (enc == 0) {
            boolean hasHighBytes = false;
            for (int i = start; i < start + len; i++) {
                int b = bytes[i] & 0xFF;
                if (b >= 0xC0 || b == 0xA8 || b == 0xB8) {
                    hasHighBytes = true;
                    break;
                }
            }
            if (hasHighBytes) {
                try {
                    charset = Charset.forName("windows-1251");
                } catch (Exception ignored) {}
            }
        }
        try {
            return new String(bytes, start, len, charset).replace("\u0000", "").trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] decodeApicFrame(byte[] bytes, boolean picFormat) {
        if (bytes == null || bytes.length < 10) {
            return null;
        }
        int enc = bytes[0] & 0xFF;
        int pos = 1;
        if (picFormat) {
            pos += 3;
        } else {
            while (pos < bytes.length && bytes[pos] != 0) {
                pos++;
            }
            if (pos >= bytes.length) {
                return null;
            }
            pos++;
        }
        if (pos >= bytes.length) {
            return null;
        }
        pos++;
        boolean foundDescEnd = false;
        if (enc == 1 || enc == 2) {
            while (pos + 1 < bytes.length) {
                if (bytes[pos] == 0 && bytes[pos + 1] == 0) {
                    pos += 2;
                    foundDescEnd = true;
                    break;
                }
                pos += 2;
            }
        } else {
            while (pos < bytes.length && bytes[pos] != 0) {
                pos++;
            }
            if (pos < bytes.length) {
                pos++;
                foundDescEnd = true;
            }
        }
        if (!foundDescEnd || pos >= bytes.length) {
            return null;
        }
        return Arrays.copyOfRange(bytes, pos, bytes.length);
    }

    private Path normalizedPath() {
        return this.path != null ? this.path.toAbsolutePath().normalize() : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalMusicTrack other)) {
            return false;
        }
        return java.util.Objects.equals(this.normalizedPath(), other.normalizedPath());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(this.normalizedPath());
    }
}
