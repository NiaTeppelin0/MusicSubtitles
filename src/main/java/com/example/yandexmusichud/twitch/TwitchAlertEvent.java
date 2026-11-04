package com.example.yandexmusichud.twitch;

public class TwitchAlertEvent {
    public enum Type {
        SUB,
        RESUB,
        SUB_GIFT,
        REWARD,
        BITS
    }

    private final Type type;
    private final String username;
    private final String displayName;
    private final String message;
    private final String subTier;
    private final int months;
    private final String recipient;
    private final String rewardTitle;
    private final int rewardCost;
    private final int bitsCount;
    private final long timestamp;

    private TwitchAlertEvent(Builder builder) {
        this.type = builder.type;
        this.username = builder.username != null ? builder.username : "Anonymous";
        this.displayName = builder.displayName != null && !builder.displayName.isEmpty() ? builder.displayName : this.username;
        this.message = builder.message != null ? builder.message : "";
        this.subTier = builder.subTier != null ? builder.subTier : "Tier 1";
        this.months = builder.months;
        this.recipient = builder.recipient != null ? builder.recipient : "";
        this.rewardTitle = builder.rewardTitle != null ? builder.rewardTitle : "";
        this.rewardCost = builder.rewardCost;
        this.bitsCount = builder.bitsCount;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() {
        return this.type;
    }

    public String getUsername() {
        return this.username;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getMessage() {
        return this.message;
    }

    public String getSubTier() {
        return this.subTier;
    }

    public int getMonths() {
        return this.months;
    }

    public String getRecipient() {
        return this.recipient;
    }

    public String getRewardTitle() {
        return this.rewardTitle;
    }

    public int getRewardCost() {
        return this.rewardCost;
    }

    public int getBitsCount() {
        return this.bitsCount;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getBadgeText() {
        return switch (this.type) {
            case SUB -> "SUB " + this.subTier;
            case RESUB -> "RESUB x" + this.months;
            case SUB_GIFT -> "GIFT SUB";
            case REWARD -> "НАГРАДА";
            case BITS -> this.bitsCount + " BITS";
        };
    }

    public String getTitle() {
        return switch (this.type) {
            case SUB -> "\u00a7d\u25C8 \u00a7f" + this.displayName + " \u00a77\u043f\u043e\u0434\u043f\u0438\u0441\u0430\u043b\u0441\u044f!";
            case RESUB -> "\u00a7d\u25C8 \u00a7f" + this.displayName + " \u00a77\u043f\u0440\u043e\u0434\u043b\u0438\u043b \u0441\u0430\u0431 \u00a7e(" + this.months + " \u043c\u0435\u0441.)";
            case SUB_GIFT -> "\u00a7d\u25C8 \u00a7f" + this.displayName + " \u00a77\u043f\u043e\u0434\u0430\u0440\u0438\u043b \u0441\u0430\u0431 \u00a7b" + this.recipient;
            case REWARD -> "\u00a7e\u2726 \u00a7f" + this.displayName + " \u00a77\u043d\u0430\u0433\u0440\u0430\u0434\u0430:";
            case BITS -> "\u00a76\u2727 \u00a7f" + this.displayName + " \u00a77\u043e\u0442\u043f\u0440\u0430\u0432\u0438\u043b \u00a7e" + this.bitsCount + " \u00a77Bits";
        };
    }

    public String getSubtitle() {
        return switch (this.type) {
            case REWARD -> this.rewardTitle + (!this.message.isEmpty() ? " \u00a77(\u00a7f" + this.message + "\u00a77)" : "");
            case RESUB, SUB -> !this.message.isEmpty() ? "\u00a7f\"" + this.message + "\"" : "\u00a77Спасибо за поддержку стрима! \u00a7d\u2764";
            case SUB_GIFT -> "\u00a77Праздник в чате! \u00a7d\u2728";
            case BITS -> !this.message.isEmpty() ? "\u00a7f\"" + this.message + "\"" : "\u00a77Хайп! \u00a76\u2726";
        };
    }

    public int getAccentColor() {
        return switch (this.type) {
            case SUB, SUB_GIFT -> 0x9146FF;
            case RESUB -> 0xB57BFF;
            case REWARD -> 0x00E5FF;
            case BITS -> 0xFFB300;
        };
    }

    public static class Builder {
        private final Type type;
        private String username;
        private String displayName;
        private String message;
        private String subTier;
        private int months;
        private String recipient;
        private String rewardTitle;
        private int rewardCost;
        private int bitsCount;

        public Builder(Type type) {
            this.type = type;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder subTier(String subTier) {
            this.subTier = subTier;
            return this;
        }

        public Builder months(int months) {
            this.months = months;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder rewardTitle(String rewardTitle) {
            this.rewardTitle = rewardTitle;
            return this;
        }

        public Builder rewardCost(int rewardCost) {
            this.rewardCost = rewardCost;
            return this;
        }

        public Builder bitsCount(int bitsCount) {
            this.bitsCount = bitsCount;
            return this;
        }

        public TwitchAlertEvent build() {
            return new TwitchAlertEvent(this);
        }
    }
}

