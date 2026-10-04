package com.sentinel.model;

import com.sentinel.exception.RuleEvaluationException;

/**
 * Strategy interface for fraud detection components.
 * Demonstrates Polymorphism & Interface segregation in OOP.
 */
public interface FraudDetector {

    /**
     * Unique identifier / name of the detector model.
     */
    String getDetectorName();

    /**
     * Evaluates a transaction and returns an attribution or assessment.
     *
     * @param transaction the candidate transaction
     * @param profile the customer's historical profile
     * @return feature attribution detailing risk score delta and explanation
     * @throws RuleEvaluationException if evaluation fails unexpectedly
     */
    FeatureAttribution evaluate(Transaction transaction, UserProfile profile) throws RuleEvaluationException;

    /**
     * Relative weight of this detector in the composite ensemble (0.0 to 1.0).
     */
    double getWeight();
}
