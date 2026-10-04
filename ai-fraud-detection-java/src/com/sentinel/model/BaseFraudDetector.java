package com.sentinel.model;

/**
 * Abstract base class providing common functionality for all fraud detectors.
 * Demonstrates Inheritance in OOP.
 */
public abstract class BaseFraudDetector implements FraudDetector {

    protected final String detectorName;
    protected double weight;
    protected boolean enabled;

    public BaseFraudDetector(String detectorName, double weight) {
        this.detectorName = detectorName;
        this.weight = Math.max(0.0, Math.min(1.0, weight));
        this.enabled = true;
    }

    @Override
    public String getDetectorName() {
        return detectorName;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = Math.max(0.0, Math.min(1.0, weight));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
