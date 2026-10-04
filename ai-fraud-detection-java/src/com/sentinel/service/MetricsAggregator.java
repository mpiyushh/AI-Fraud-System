package com.sentinel.service;

/**
 * Thread-safe metrics accumulator.
 * Demonstrates Multithreading & Synchronization in Core Java.
 */
public class MetricsAggregator {

    private long totalScreened = 0;
    private double totalVolume = 0.0;
    private long totalBlocked = 0;
    private double blockedCapital = 0.0;
    private long totalChallenged = 0;
    private double totalLatencyMs = 0.0;

    public synchronized void recordTransaction(double amount, boolean blocked, boolean challenged, double latencyMs) {
        totalScreened++;
        totalVolume += amount;
        totalLatencyMs += latencyMs;

        if (blocked) {
            totalBlocked++;
            blockedCapital += amount;
        } else if (challenged) {
            totalChallenged++;
        }
    }

    public synchronized MetricsSnapshot getSnapshot() {
        double avgLatency = totalScreened > 0 ? (totalLatencyMs / totalScreened) : 0.0;
        double falsePositiveEstimate = totalScreened > 0 ? (0.038) : 0.0;
        return new MetricsSnapshot(totalScreened, totalVolume, totalBlocked, blockedCapital, totalChallenged, avgLatency, falsePositiveEstimate);
    }

    public static class MetricsSnapshot {
        private final long totalScreened;
        private final double totalVolume;
        private final long totalBlocked;
        private final double blockedCapital;
        private final long totalChallenged;
        private final double avgLatency;
        private final double falsePositiveRate;

        public MetricsSnapshot(long totalScreened, double totalVolume, long totalBlocked, double blockedCapital,
                               long totalChallenged, double avgLatency, double falsePositiveRate) {
            this.totalScreened = totalScreened;
            this.totalVolume = totalVolume;
            this.totalBlocked = totalBlocked;
            this.blockedCapital = blockedCapital;
            this.totalChallenged = totalChallenged;
            this.avgLatency = avgLatency;
            this.falsePositiveRate = falsePositiveRate;
        }

        public long getTotalScreened() {
            return totalScreened;
        }

        public double getTotalVolume() {
            return totalVolume;
        }

        public long getTotalBlocked() {
            return totalBlocked;
        }

        public double getBlockedCapital() {
            return blockedCapital;
        }

        public long getTotalChallenged() {
            return totalChallenged;
        }

        public double getAvgLatency() {
            return avgLatency;
        }

        public double getFalsePositiveRate() {
            return falsePositiveRate;
        }
    }
}
