package com.example.yandexmusichud.donationalerts;

public class DonationAlertEvent {
    private final long id;
    private final String donorName;
    private final double amount;
    private final String currency;
    private final String message;
    private final long timestamp;

    public DonationAlertEvent(long id, String donorName, double amount, String currency, String message) {
        this.id = id;
        this.donorName = donorName != null && !donorName.trim().isEmpty() ? donorName.trim() : "Аноним";
        this.amount = Math.max(0.0, amount);
        this.currency = currency != null && !currency.trim().isEmpty() ? currency.trim().toUpperCase() : "RUB";
        this.message = message != null ? message.trim() : "";
        this.timestamp = System.currentTimeMillis();
    }

    public long getId() {
        return this.id;
    }

    public String getDonorName() {
        return this.donorName;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getCurrency() {
        return this.currency;
    }

    public String getMessage() {
        return this.message;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getCurrencySymbol() {
        return switch (this.currency) {
            case "RUB" -> "₽";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "BYN" -> "Br";
            case "KZT" -> "₸";
            case "UAH" -> "₴";
            default -> this.currency;
        };
    }

    public String getFormattedAmount() {
        if (this.amount == (long) this.amount) {
            return String.format("%,d %s", (long) this.amount, this.getCurrencySymbol());
        }
        return String.format("%,.2f %s", this.amount, this.getCurrencySymbol());
    }

    public String getBadgeText() {
        return "ДОНАТ";
    }

    public String getTitle() {
        return "§6§l✦ §f" + this.donorName + " §7задонатил §e§l" + this.getFormattedAmount();
    }

    public String getSubtitle() {
        if (!this.message.isEmpty()) {
            return "§f\"" + this.message + "\"";
        }
        return "§7Спасибо за поддержку стрима! §6❤";
    }

    public int getAccentColor() {
        return 0xFA7218;
    }
}

