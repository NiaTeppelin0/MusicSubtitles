package com.example.yandexmusichud.twitch;

import com.example.yandexmusichud.config.HudConfig;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TwitchAlertQueue {
    public static final TwitchAlertQueue INSTANCE = new TwitchAlertQueue();

    private final ConcurrentLinkedQueue<TwitchAlertEvent> queue = new ConcurrentLinkedQueue<>();
    private TwitchAlertEvent activeAlert = null;
    private long alertStartTime = 0L;
    private float animProgress = 0.0f;
    private long lastTickTime = 0L;
    private int totalHandledCount = 0;

    private TwitchAlertQueue() {}

    public void enqueue(TwitchAlertEvent event) {
        if (event == null) return;
        this.queue.add(event);
    }

    public void update() {
        long now = System.currentTimeMillis();
        float dt = this.lastTickTime > 0L ? Math.min(0.1f, (float)(now - this.lastTickTime) / 1000.0f) : 0.016f;
        this.lastTickTime = now;

        long durationMs = (long)Math.max(2, HudConfig.INSTANCE.twitchToastDurationSeconds) * 1000L;
        long enterDurationMs = 450L;
        long exitDurationMs = 500L;

        if (this.activeAlert == null) {
            if (!this.queue.isEmpty()) {
                this.activeAlert = this.queue.poll();
                this.totalHandledCount++;
                this.alertStartTime = now;
                this.animProgress = 0.0f;
            } else {
                this.animProgress = Math.max(0.0f, this.animProgress - dt * 4.0f);
            }
            return;
        }

        long elapsed = now - this.alertStartTime;
        if (elapsed < enterDurationMs) {

            float t = (float)elapsed / (float)enterDurationMs;
            float target = easeOutBack(t);
            this.animProgress = target;
        } else if (elapsed < durationMs - exitDurationMs) {

            this.animProgress = 1.0f;
        } else if (elapsed < durationMs) {

            long exitElapsed = elapsed - (durationMs - exitDurationMs);
            float t = (float)exitElapsed / (float)exitDurationMs;
            this.animProgress = 1.0f - easeInCubic(t);
        } else {

            this.activeAlert = null;
            this.animProgress = 0.0f;
            this.alertStartTime = 0L;
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float)Math.pow(x - 1.0f, 3.0) + c1 * (float)Math.pow(x - 1.0f, 2.0);
    }

    private static float easeInCubic(float x) {
        return x * x * x;
    }

    public TwitchAlertEvent getActiveAlert() {
        return this.activeAlert;
    }

    public float getAnimProgress() {
        return Math.max(0.0f, Math.min(1.0f, this.animProgress));
    }

    public boolean isShowing() {
        return this.activeAlert != null || !this.queue.isEmpty() || this.animProgress > 0.01f;
    }

    public int getQueuedCount() {
        return this.queue.size();
    }

    public int getTotalHandledCount() {
        return this.totalHandledCount;
    }

    public void clear() {
        this.queue.clear();
        this.activeAlert = null;
        this.animProgress = 0.0f;
    }
}

