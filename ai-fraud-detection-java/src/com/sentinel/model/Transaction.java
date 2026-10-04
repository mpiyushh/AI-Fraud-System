package com.sentinel.model;

import com.sentinel.exception.InvalidTransactionException;

import java.io.Serializable;

/**
 * Domain entity representing a payment transaction.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final String userId;
    private final String userFullName;
    private final String cardMasked;
    private final String bank;
    private final double amount;
    private final String currency;
    private final long timestamp;
    private final String ipAddress;
    private final String location;
    private final String deviceHash;
    private final boolean vpnOrTor;

    // Evaluated by the engine
    private DetectionResult result;
    private String analystStatus; // PENDING, OVERRIDDEN, BLACKLISTED

    public Transaction(String transactionId, String userId, String userFullName, String cardMasked,
                       double amount, String currency, long timestamp, String ipAddress,
                       String location, String deviceHash, boolean vpnOrTor) throws InvalidTransactionException {
        this(transactionId, userId, userFullName, cardMasked, "Chase", amount, currency, timestamp, ipAddress, location, deviceHash, vpnOrTor);
    }

    public Transaction(String transactionId, String userId, String userFullName, String cardMasked,
                       String bank, double amount, String currency, long timestamp, String ipAddress,
                       String location, String deviceHash, boolean vpnOrTor) throws InvalidTransactionException {
        
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new InvalidTransactionException(transactionId, "Transaction ID cannot be null or empty");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException(transactionId, "Amount must be strictly positive: " + amount);
        }

        this.transactionId = transactionId;
        this.userId = userId != null ? userId : "ANON_USER";
        this.userFullName = userFullName != null ? userFullName : "Unknown Customer";
        this.cardMasked = cardMasked != null ? cardMasked : "Visa •• 0000";
        this.bank = bank != null ? bank : "Chase";
        this.amount = amount;
        this.currency = currency != null ? currency : "USD";
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.ipAddress = ipAddress != null ? ipAddress : "0.0.0.0";
        this.location = location != null ? location : "Unknown Location";
        this.deviceHash = deviceHash != null ? deviceHash : "DEV_UNASSIGNED";
        this.vpnOrTor = vpnOrTor;
        this.analystStatus = "PENDING";
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public String getCardMasked() {
        return cardMasked;
    }

    public String getBank() {
        return bank;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getLocation() {
        return location;
    }

    public String getDeviceHash() {
        return deviceHash;
    }

    public boolean isVpnOrTor() {
        return vpnOrTor;
    }

    public DetectionResult getResult() {
        return result;
    }

    public void setResult(DetectionResult result) {
        this.result = result;
    }

    public String getAnalystStatus() {
        return analystStatus;
    }

    public void setAnalystStatus(String analystStatus) {
        this.analystStatus = analystStatus;
    }

    @Override
    public String toString() {
        return String.format("[%s] User:%s | Bank:%s | Card:%s | $%.2f | Risk:%s",
                transactionId, userFullName, bank, cardMasked, amount,
                result != null ? result.getRiskScore() : "Unscored");
    }
}
