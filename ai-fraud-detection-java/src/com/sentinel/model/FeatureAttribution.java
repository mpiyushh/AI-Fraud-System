package com.sentinel.model;

/**
 * Represents an individual Explainable AI (XAI / SHAP) feature attribution.
 * Explains how much a specific signal contributed towards the final risk score.
 */
public class FeatureAttribution {
    private final String featureName;
    private final double impactPoints; // e.g. +35.0 or -15.0
    private final boolean riskIncreasing;
    private final String description;

    public FeatureAttribution(String featureName, double impactPoints, boolean riskIncreasing, String description) {
        this.featureName = featureName;
        this.impactPoints = impactPoints;
        this.riskIncreasing = riskIncreasing;
        this.description = description;
    }

    public String getFeatureName() {
        return featureName;
    }

    public double getImpactPoints() {
        return impactPoints;
    }

    public boolean isRiskIncreasing() {
        return riskIncreasing;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        String sign = impactPoints >= 0 ? "+" : "";
        return String.format("%s: %s%.1f pts (%s)", featureName, sign, impactPoints, description);
    }
}
