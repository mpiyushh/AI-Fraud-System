package com.sentinel.model.detectors;

import com.sentinel.exception.RuleEvaluationException;
import com.sentinel.model.BaseFraudDetector;
import com.sentinel.model.FeatureAttribution;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;

/**
 * Calculates statistical z-score outlier deviations against the cardholder's historical baseline.
 * Demonstrates Inheritance & Mathematical scoring.
 */
public class AmountSpikeDetector extends BaseFraudDetector {

    public AmountSpikeDetector(double weight) {
        super("Monetary Value Outlier Detector", weight);
    }

    @Override
    public FeatureAttribution evaluate(Transaction transaction, UserProfile profile) throws RuleEvaluationException {
        try {
            double amount = transaction.getAmount();

            if (profile != null && profile.getTransactionCount() > 0) {
                double mean = profile.getHistoricalAverageAmount();
                double stdDev = Math.max(10.0, profile.getStandardDeviation());

                double zScore = (amount - mean) / stdDev;

                if (zScore > 3.0) {
                    double points = clamp(zScore * 8.0 * weight, 20.0, 45.0);
                    return new FeatureAttribution(
                            "Severe Monetary Value Spike",
                            points,
                            true,
                            String.format("Amount ($%.2f) is %.1f std deviations above normal average ($%.2f)", amount, zScore, mean)
                    );
                } else if (zScore > 1.8) {
                    return new FeatureAttribution(
                            "Elevated Monetary Value",
                            15.0 * weight,
                            true,
                            String.format("Amount is elevated compared to $%.2f historical baseline", mean)
                    );
                } else {
                    return new FeatureAttribution(
                            "Baseline Spending Alignment",
                            -12.0 * weight,
                            false,
                            String.format("Amount conforms to established spending habits ($%.2f avg)", mean)
                    );
                }
            }

            // Fallback for new accounts
            if (amount > 2000.0) {
                return new FeatureAttribution(
                        "High Initial Order Volume",
                        22.0 * weight,
                        true,
                        String.format("Unusually high transaction ($%.2f) on unseasoned account", amount)
                );
            }

            return new FeatureAttribution(
                    "Standard First-Time Retail Amount",
                    -5.0 * weight,
                    false,
                    "Amount is typical for retail checkout"
            );

        } catch (Exception ex) {
            throw new RuleEvaluationException(getDetectorName(), ex.getMessage(), ex);
        }
    }
}
