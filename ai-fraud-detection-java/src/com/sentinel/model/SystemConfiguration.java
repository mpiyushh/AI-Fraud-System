package com.sentinel.model;

import java.io.Serializable;

/**
 * System Configuration Entity for Admin Management.
 * Fulfills GUVI Req: "Admin - System Configuration (detection thresholds, algorithm parameters)".
 */
public class SystemConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private int blockThreshold;       // e.g. 80
    private int challengeThreshold;   // e.g. 55
    private int velocityBurstLimit;   // e.g. 3 tx/min
    private double maxAmountCeiling;  // e.g. $10,000.00
    private boolean torHardBlock;     // true/false
    private String activeModelVersion;// e.g. "v4.8-Neural-Ensemble"

    public SystemConfiguration() {
        this.blockThreshold = 80;
        this.challengeThreshold = 55;
        this.velocityBurstLimit = 3;
        this.maxAmountCeiling = 10000.0;
        this.torHardBlock = true;
        this.activeModelVersion = "v4.8-Neural-Ensemble";
    }

    public SystemConfiguration(int blockThreshold, int challengeThreshold, int velocityBurstLimit,
                               double maxAmountCeiling, boolean torHardBlock, String activeModelVersion) {
        this.blockThreshold = blockThreshold;
        this.challengeThreshold = challengeThreshold;
        this.velocityBurstLimit = velocityBurstLimit;
        this.maxAmountCeiling = maxAmountCeiling;
        this.torHardBlock = torHardBlock;
        this.activeModelVersion = activeModelVersion;
    }

    public int getBlockThreshold() {
        return blockThreshold;
    }

    public void setBlockThreshold(int blockThreshold) {
        this.blockThreshold = blockThreshold;
    }

    public int getChallengeThreshold() {
        return challengeThreshold;
    }

    public void setChallengeThreshold(int challengeThreshold) {
        this.challengeThreshold = challengeThreshold;
    }

    public int getVelocityBurstLimit() {
        return velocityBurstLimit;
    }

    public void setVelocityBurstLimit(int velocityBurstLimit) {
        this.velocityBurstLimit = velocityBurstLimit;
    }

    public double getMaxAmountCeiling() {
        return maxAmountCeiling;
    }

    public void setMaxAmountCeiling(double maxAmountCeiling) {
        this.maxAmountCeiling = maxAmountCeiling;
    }

    public boolean isTorHardBlock() {
        return torHardBlock;
    }

    public void setTorHardBlock(boolean torHardBlock) {
        this.torHardBlock = torHardBlock;
    }

    public String getActiveModelVersion() {
        return activeModelVersion;
    }

    public void setActiveModelVersion(String activeModelVersion) {
        this.activeModelVersion = activeModelVersion;
    }
}
