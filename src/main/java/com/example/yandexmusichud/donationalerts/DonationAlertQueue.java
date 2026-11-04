package com.example.yandexmusichud.donationalerts;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DonationAlertQueue {
    public static final DonationAlertQueue INSTANCE = new DonationAlertQueue();

    private final ConcurrentLinkedQueue<DonationAlertEvent> queue = new ConcurrentLinkedQueue<>();
    private DonationAlertEvent activeAlert = null;
    private long alertStartTime = 0L;
    private float animProgress = 0.0f;
    private long lastTickTime = 0L;
    private int totalHandledCount = 0;

    private DonationAlertQueue() {}

    public void enqueue(DonationAlertEvent event) {
        if (event == null) return;
        this.queue.add(event);
    }

    public void update() {
        long now = System.currentTimeMillis();
        float dt = this.lastTickTime > 0L ? Math.min(0.1f, (float)(now - this.lastTickTime) / 1000.0f) : 0.016f;
        this.lastTickTime = now;

        long durationMs = 5000L;
        long enterDurationMs = 420L;
        long exitDurationMs = 450L;

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
            this.animProgress = easeOutBack(t);
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

    public DonationAlertEvent getActiveAlert() {
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

