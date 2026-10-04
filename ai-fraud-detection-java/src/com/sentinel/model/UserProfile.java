package com.sentinel.model;

import java.io.Serializable;

/**
 * Historical customer profile maintained in the feature store / database.
 */
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userId;
    private String fullName;
    private String primaryLocation;
    private String trustedDeviceHash;
    private double historicalAverageAmount;
    private double standardDeviation;
    private int transactionCount;
    private int chargebackCount;
    private boolean blacklisted;
    private long lastSeenTimestamp;
    private String lastSeenLocation;

    public UserProfile(String userId, String fullName, String primaryLocation, String trustedDeviceHash,
                       double historicalAverageAmount, double standardDeviation, int transactionCount,
                       int chargebackCount, boolean blacklisted) {
        this.userId = userId;
        this.fullName = fullName;
        this.primaryLocation = primaryLocation;
        this.trustedDeviceHash = trustedDeviceHash;
        this.historicalAverageAmount = historicalAverageAmount;
        this.standardDeviation = standardDeviation > 0 ? standardDeviation : 20.0;
        this.transactionCount = transactionCount;
        this.chargebackCount = chargebackCount;
        this.blacklisted = blacklisted;
        this.lastSeenTimestamp = System.currentTimeMillis();
        this.lastSeenLocation = primaryLocation;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPrimaryLocation() {
        return primaryLocation;
    }

    public void setPrimaryLocation(String primaryLocation) {
        this.primaryLocation = primaryLocation;
    }

    public String getTrustedDeviceHash() {
        return trustedDeviceHash;
    }

    public void setTrustedDeviceHash(String trustedDeviceHash) {
        this.trustedDeviceHash = trustedDeviceHash;
    }

    public double getHistoricalAverageAmount() {
        return historicalAverageAmount;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public int getChargebackCount() {
        return chargebackCount;
    }

    public void incrementChargeback() {
        this.chargebackCount++;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public void setLastSeenTimestamp(long lastSeenTimestamp) {
        this.lastSeenTimestamp = lastSeenTimestamp;
    }

    public String getLastSeenLocation() {
        return lastSeenLocation;
    }

    public void setLastSeenLocation(String lastSeenLocation) {
        this.lastSeenLocation = lastSeenLocation;
    }

    /**
     * Welford's algorithm to update running mean and standard deviation.
     */
    public synchronized void recordNewTransaction(double amount, String location) {
        this.transactionCount++;
        double delta = amount - this.historicalAverageAmount;
        this.historicalAverageAmount += delta / this.transactionCount;
        this.lastSeenTimestamp = System.currentTimeMillis();
        this.lastSeenLocation = location;
    }
}
