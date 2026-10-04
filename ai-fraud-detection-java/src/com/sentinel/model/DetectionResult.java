package com.sentinel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the output of the AI Fraud Detection evaluation.
 */
public class DetectionResult {

    public enum Verdict {
        APPROVED,
        CHALLENGE_3DS,
        AUTO_BLOCKED
    }

    private final int riskScore; // 0 to 100
    private final Verdict verdict;
    private final String dominantThreat;
    private final List<FeatureAttribution> attributions;
    private final double latencyMs;
    private final long evaluationTimestamp;

    public DetectionResult(int riskScore, Verdict verdict, String dominantThreat, List<FeatureAttribution> attributions, double latencyMs) {
        this.riskScore = Math.min(100, Math.max(0, riskScore));
        this.verdict = verdict;
        this.dominantThreat = dominantThreat;
        this.attributions = attributions != null ? new ArrayList<>(attributions) : new ArrayList<>();
        this.latencyMs = latencyMs;
        this.evaluationTimestamp = System.currentTimeMillis();
    }

    public int getRiskScore() {
        return riskScore;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getDominantThreat() {
        return dominantThreat;
    }

    public List<FeatureAttribution> getAttributions() {
        return Collections.unmodifiableList(attributions);
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public long getEvaluationTimestamp() {
        return evaluationTimestamp;
    }

    public boolean isBlocked() {
        return verdict == Verdict.AUTO_BLOCKED;
    }

    public boolean isChallenged() {
        return verdict == Verdict.CHALLENGE_3DS;
    }

    public boolean isApproved() {
        return verdict == Verdict.APPROVED;
    }
}
