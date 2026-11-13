package com.example.yandexmusichud.lyrics;

import com.example.yandexmusichud.config.HudConfig;
import com.example.yandexmusichud.lyrics.LyricLine;
import net.minecraft.class_243;
import org.joml.Quaternionf;

public class SubtitleBubble {
    public final LyricLine line;
    public final long spawnSystemTimeMs;
    public float screenX;
    public float screenY;
    public class_243 worldPos;
    public Quaternionf worldRot;
    public boolean isSurfaceAttached = false;
    public boolean isFastPaced = false;
    public boolean isLiveCaption = false;
    public long liveDurationMs = 3500L;
    public float[] charGroundTargetY = null;
    public CharPhysics[] charPhysics = null;
    public long lastInteractionTimeMs = 0L;
    public boolean hasAdaptedToScreen = false;
    public long fadeOutStartSystemTimeMs = -1L;
    public long fadeOutDurationMs = 1600L;

    public static class CharPhysics {
        public float extraX = 0.0f;
        public float extraY = 0.0f;
        public float velX = 0.0f;
        public float velY = 0.0f;
        public float rot = 0.0f;
        public float rotVel = 0.0f;
        public boolean touched = false;
        public boolean fellOff = false;
        public boolean isDragging = false;
    }

    public void startFadeOut(long nowMs) {
        if (this.fadeOutStartSystemTimeMs < 0L) {
            this.fadeOutStartSystemTimeMs = nowMs;
            this.fadeOutDurationMs = 1600L;
        }
    }

    public void startFadeOutFast(long nowMs) {
        if (this.fadeOutStartSystemTimeMs < 0L) {
            this.fadeOutStartSystemTimeMs = nowMs;
            this.fadeOutDurationMs = 750L;
        }
    }

    public void startFadeOutPause(long nowMs) {
        if (this.fadeOutStartSystemTimeMs < 0L || this.fadeOutDurationMs > 320L) {
            this.fadeOutStartSystemTimeMs = nowMs;
            this.fadeOutDurationMs = 320L;
        }
    }

    public SubtitleBubble(LyricLine line, float screenX, float screenY) {
        this(line, screenX, screenY, false);
    }

    public SubtitleBubble(LyricLine line, float screenX, float screenY, boolean isFastPaced) {
        this.line = line;
        this.spawnSystemTimeMs = System.currentTimeMillis();
        this.screenX = screenX;
        this.screenY = screenY;
        this.isFastPaced = isFastPaced;
    }

    public SubtitleBubble(LyricLine line, class_243 worldPos, Quaternionf worldRot) {
        this(line, worldPos, worldRot, false, false);
    }

    public SubtitleBubble(LyricLine line, class_243 worldPos, Quaternionf worldRot, boolean isSurfaceAttached) {
        this(line, worldPos, worldRot, isSurfaceAttached, false);
    }

    public SubtitleBubble(LyricLine line, class_243 worldPos, Quaternionf worldRot, boolean isSurfaceAttached, boolean isFastPaced) {
        this.line = line;
        this.spawnSystemTimeMs = System.currentTimeMillis();
        this.worldPos = worldPos;
        this.worldRot = worldRot;
        this.isSurfaceAttached = isSurfaceAttached;
        this.isFastPaced = isFastPaced;
    }

    public float getScale(long currentTimeMs) {
        long age;
        String anim = HudConfig.INSTANCE.subtitlesAnimation;
        if (anim == null) {
            anim = "KARAOKE_BOUNCE";
        }
        if ((age = currentTimeMs - this.spawnSystemTimeMs) < 0L) {
            return 0.0f;
        }
        if (this.isFastPaced) {
            if (age < 60L) {
                float t = (float)age / 60.0f;
                return 0.85f + 0.18f * SubtitleBubble.easeOutBack(t);
            }
            return 1.0f;
        }
        switch (anim.toUpperCase(java.util.Locale.ROOT)) {
            case "ELASTIC_POP": {
                if (age < 450L) {
                    float t = (float)age / 450.0f;
                    return 0.5f + 0.5f * (1.0f + 0.75f * (float)(Math.sin((double)t * Math.PI * 2.5) * Math.exp((double)(-t) * 3.5)));
                }
                return 1.0f;
            }
            case "WAVE_FLOAT": {
                float base = age < 300L ? 0.7f + 0.3f * SubtitleBubble.easeOutCubic((float)age / 300.0f) : 1.0f;
                return base + 0.035f * (float)Math.sin((double)(currentTimeMs - this.spawnSystemTimeMs) / 380.0);
            }
            case "SLIDE_FADE": {
                if (age < 280L) {
                    return 0.82f + 0.18f * SubtitleBubble.easeOutCubic((float)age / 280.0f);
                }
                return 1.0f;
            }
            case "PULSE_GLOW": {
                float base = age < 220L ? (float)age / 220.0f : 1.0f;
                return base + 0.045f * (float)Math.sin((double)(currentTimeMs - this.spawnSystemTimeMs) / 180.0);
            }
            case "CLASSIC": {
                if (age < 180L) {
                    return (float)age / 180.0f;
                }
                return 1.0f;
            }
        }
        if (age < 320L) {
            float t = (float)age / 320.0f;
            return 0.88f + 0.12f * SubtitleBubble.easeOutCubic(t);
        }
        return 1.0f;
    }

    public float getOffsetX(long currentTimeMs) {
        if (this.isFastPaced) {
            return 0.0f;
        }
        String anim = HudConfig.INSTANCE.subtitlesAnimation;
        long age = currentTimeMs - this.spawnSystemTimeMs;
        float drift = (float)Math.cos((double)(currentTimeMs - this.spawnSystemTimeMs) / 840.0) * 1.0f;
        if ("SLIDE_FADE".equalsIgnoreCase(anim)) {
            if (age < 320L) {
                float t = (float)age / 320.0f;
                return -28.0f * (1.0f - t * t * t) + drift;
            }
        } else if ("WAVE_FLOAT".equalsIgnoreCase(anim)) {
            return (float)Math.sin((double)(currentTimeMs - this.spawnSystemTimeMs) / 600.0) * 2.5f + drift;
        }
        return drift;
    }

    public float getOffsetY(long currentTimeMs) {
        float t;
        String anim = HudConfig.INSTANCE.subtitlesAnimation;
        long age = currentTimeMs - this.spawnSystemTimeMs;
        float breath = (float)Math.sin((double)(currentTimeMs - this.spawnSystemTimeMs) / 520.0) * 1.8f;
        float kineticEntry = 0.0f;
        if (this.isFastPaced) {
            if (age < 60L) {
                float t2 = (float)age / 60.0f;
                kineticEntry = 3.0f * (1.0f - t2 * t2);
            }
            return kineticEntry;
        }
        if (age < 320L) {
            t = (float)age / 320.0f;
            kineticEntry = 10.0f * (1.0f - SubtitleBubble.easeOutCubic(t));
        }
        if ("WAVE_FLOAT".equalsIgnoreCase(anim)) {
            return (float)Math.sin((double)(currentTimeMs - this.spawnSystemTimeMs) / 320.0) * 3.5f + breath + kineticEntry;
        }
        if ("SLIDE_FADE".equalsIgnoreCase(anim) && age < 280L) {
            t = (float)age / 280.0f;
            return 10.0f * (1.0f - t * t) + breath;
        }
        return breath + kineticEntry;
    }

    public float getExitOffsetY(long trackPosMs) {
        if (this.isLiveCaption) {
            long age = System.currentTimeMillis() - this.spawnSystemTimeMs;
            long remaining = this.liveDurationMs - age;
            if (remaining < 250L && remaining > 0L) {
                float t = 1.0f - ((float)remaining / 250.0f);
                return (this.isFastPaced ? -5.0f : -12.0f) * SubtitleBubble.easeOutCubic(t);
            }
            return 0.0f;
        }
        boolean isMiside = "MISIDE_FALL".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || "MISIDE".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || (this.line != null && this.line.rawText != null && this.line.rawText.contains("<anim:miside>"));
        if (isMiside) {
            return 0.0f;
        }
        if (this.fadeOutStartSystemTimeMs > 0L) {
            long elapsed = System.currentTimeMillis() - this.fadeOutStartSystemTimeMs;
            float t = Math.min(1.0f, (float)elapsed / (float)this.fadeOutDurationMs);
            return (this.isFastPaced ? -6.0f : -14.0f) * SubtitleBubble.easeOutCubic(t);
        }
        if (this.line != null && trackPosMs > this.line.endMs) {
            long overtime = trackPosMs - this.line.endMs;
            float t = Math.min(1.0f, (float)overtime / (this.isFastPaced ? 140.0f : 400.0f));
            return (this.isFastPaced ? -5.0f : -12.0f) * SubtitleBubble.easeOutCubic(t);
        }
        return 0.0f;
    }

    public float getOverallAlpha(long trackPosMs, long currentTimeMs) {
        long ageSystem = currentTimeMs - this.spawnSystemTimeMs;
        if (ageSystem < 0L) {
            return 0.0f;
        }
        long fadeDur = this.isFastPaced ? 45L : 250L;
        float sysAlpha = 1.0f;
        if (ageSystem < fadeDur) {
            float t = (float)ageSystem / (float)fadeDur;
            sysAlpha = t * t * (3.0f - 2.0f * t);
        }
        if (this.isLiveCaption) {
            long remaining = this.liveDurationMs - ageSystem;
            if (remaining <= 0L) {
                return 0.0f;
            }
            if (remaining < 450L) {
                float t = (float)remaining / 450.0f;
                return sysAlpha * (t * t * (3.0f - 2.0f * t));
            }
            return sysAlpha;
        }
        if (this.fadeOutStartSystemTimeMs > 0L) {
            long fadeElapsed = currentTimeMs - this.fadeOutStartSystemTimeMs;
            if (fadeElapsed >= this.fadeOutDurationMs) {
                return 0.0f;
            }
            float progress = (float)fadeElapsed / (float)this.fadeOutDurationMs;
            float fadeProgress = 1.0f - progress * progress * (3.0f - 2.0f * progress);
            return sysAlpha * Math.max(0.0f, Math.min(1.0f, fadeProgress));
        }
        boolean isMiside = "MISIDE_FALL".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || "MISIDE".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || (this.line != null && this.line.rawText != null && this.line.rawText.contains("<anim:miside>"));
        long maxAge = isMiside ? 20000L : (this.isFastPaced ? 3000L : 12000L);
        long ageRemaining = maxAge - ageSystem;
        if (ageRemaining <= 0L) {
            return 0.0f;
        }
        if (ageRemaining < 1200L) {
            float progress = (float)(1200L - ageRemaining) / 1200.0f;
            float fade = 1.0f - progress * progress * (3.0f - 2.0f * progress);
            sysAlpha *= Math.max(0.0f, Math.min(1.0f, fade));
        }
        if (this.line != null && trackPosMs > this.line.endMs) {
            long overtime = trackPosMs - this.line.endMs;
            if (isMiside) {
                if (this.charPhysics != null) {
                    for (CharPhysics cp : this.charPhysics) {
                        if (cp != null && cp.isDragging) {
                            return sysAlpha;
                        }
                    }
                }
                if (this.lastInteractionTimeMs > 0L && (currentTimeMs - this.lastInteractionTimeMs) < 4000L) {
                    return sysAlpha;
                }
                long restDur = 4500L;
                long fadeOutDur = 1600L;
                if (overtime < restDur) {
                    return sysAlpha;
                }
                if (overtime >= restDur + fadeOutDur) {
                    return 0.0f;
                }
                float progress = (float)(overtime - restDur) / (float)fadeOutDur;
                float fade = 1.0f - progress * progress * (3.0f - 2.0f * progress);
                return sysAlpha * Math.max(0.0f, Math.min(1.0f, fade));
            }
            long fadeOutDur = this.isFastPaced ? 200L : 900L;
            if (overtime >= fadeOutDur) {
                return 0.0f;
            }
            float progress = (float)overtime / (float)fadeOutDur;
            float fade = 1.0f - progress * progress * (3.0f - 2.0f * progress);
            return sysAlpha * Math.max(0.0f, Math.min(1.0f, fade));
        }
        return sysAlpha;
    }

    public boolean isExpired(long trackPosMs, long currentTimeMs) {
        if (!this.isLiveCaption && this.line != null && trackPosMs >= 0L && trackPosMs < this.line.startMs - 500L) {
            return true;
        }
        if (this.fadeOutStartSystemTimeMs > 0L) {
            return currentTimeMs - this.fadeOutStartSystemTimeMs >= this.fadeOutDurationMs;
        }
        if (this.isLiveCaption) {
            return currentTimeMs - this.spawnSystemTimeMs > this.liveDurationMs;
        }
        boolean isMiside = "MISIDE_FALL".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || "MISIDE".equalsIgnoreCase(HudConfig.INSTANCE.subtitlesAnimation) || (this.line != null && this.line.rawText != null && this.line.rawText.contains("<anim:miside>"));
        if (isMiside && this.lastInteractionTimeMs > 0L && (currentTimeMs - this.lastInteractionTimeMs) < 4500L) {
            return false;
        }
        long expireBuffer = isMiside ? 6500L : (this.isFastPaced ? 250L : 950L);
        if (this.line != null && trackPosMs > this.line.endMs + expireBuffer) {
            return true;
        }
        long maxAge = isMiside ? 20000L : (this.isFastPaced ? 3000L : 12000L);
        return currentTimeMs - this.spawnSystemTimeMs > maxAge;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float)Math.pow(t - 1.0f, 3.0) + c1 * (float)Math.pow(t - 1.0f, 2.0);
    }

    private static float easeOutCubic(float t) {
        float f = t - 1.0f;
        return f * f * f + 1.0f;
    }
}

