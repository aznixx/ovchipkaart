package nl.ovchipkaart.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    private LocalDateTime dateTime;
    private String type;
    private double amount;
    private double balanceAfter;
    private String description;

    public Transaction(String type, double amount, double balanceAfter, String description) {
        this.dateTime = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public Transaction(LocalDateTime dateTime, String type, double amount, double balanceAfter, String description) {
        this.dateTime = dateTime;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        String sign = amount >= 0 ? "+" : "";
        String date = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return date + " | " + type + " | " + sign + "€" + String.format("%.2f", amount)
                + " | balance: €" + String.format("%.2f", balanceAfter)
                + " | " + description;
    }
}
