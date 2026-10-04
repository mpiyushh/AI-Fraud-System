package com.sentinel.exception;

/**
 * Thrown when an anomaly detector or neural model fails during feature extraction or scoring.
 */
public class RuleEvaluationException extends FraudSystemException {

    private final String detectorName;

    public RuleEvaluationException(String detectorName, String message, Throwable cause) {
        super("ERR_RULE_EVALUATION", "Detector [" + detectorName + "] failed: " + message, cause);
        this.detectorName = detectorName;
    }

    public String getDetectorName() {
        return detectorName;
    }
}
