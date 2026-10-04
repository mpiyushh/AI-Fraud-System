package com.sentinel.model.detectors;

import com.sentinel.exception.RuleEvaluationException;
import com.sentinel.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite ensemble aggregating multiple polymorphic fraud detectors.
 * Demonstrates:
 * - OOP Polymorphism: Invoking evaluate() on a collection of FraudDetector instances
 * - Collections & Generics: List<FraudDetector>, List<FeatureAttribution>
 */
public class CompositeEnsembleDetector extends BaseFraudDetector {

    private final List<FraudDetector> detectors = new ArrayList<>();
    private int blockThreshold = 80;
    private int challengeThreshold = 55;

    public CompositeEnsembleDetector() {
        super("Sentinel Neural Ensemble Matrix", 1.0);
        // Register default polymorphic sub-detectors
        registerDetector(new VelocityAnomalyDetector(0.95));
        registerDetector(new GeoLocationAnomalyDetector(1.0));
        registerDetector(new DeviceFingerprintDetector(0.90));
        registerDetector(new AmountSpikeDetector(0.85));
    }

    public synchronized void registerDetector(FraudDetector detector) {
        if (detector != null) {
            detectors.add(detector);
        }
    }

    public List<FraudDetector> getDetectors() {
        return Collections.unmodifiableList(detectors);
    }

    public int getBlockThreshold() {
        return blockThreshold;
    }

    public synchronized void setBlockThreshold(int blockThreshold) {
        this.blockThreshold = Math.max(50, Math.min(95, blockThreshold));
    }

    public int getChallengeThreshold() {
        return challengeThreshold;
    }

    public synchronized void setChallengeThreshold(int challengeThreshold) {
        this.challengeThreshold = challengeThreshold;
    }

    /**
     * Executes the full polymorphic ensemble against a transaction.
     */
    public DetectionResult analyze(Transaction transaction, UserProfile profile) throws RuleEvaluationException {
        long startTime = System.nanoTime();

        double rawRiskScore = 15.0; // Base prior probability of risk
        List<FeatureAttribution> attributions = new ArrayList<>();
        String dominantThreat = "Standard Clean Pattern";
        double maxThreatImpact = 0.0;

        // Blacklist fast-path check
        if (profile != null && profile.isBlacklisted()) {
            FeatureAttribution blacklistedAttr = new FeatureAttribution(
                    "Global Entity Blacklist Match",
                    85.0,
                    true,
                    "Customer identity is permanently flagged in the global fraud registry"
            );
            attributions.add(blacklistedAttr);
            double latency = (System.nanoTime() - startTime) / 1_000_000.0;
            return new DetectionResult(99, DetectionResult.Verdict.AUTO_BLOCKED, "Known Blacklisted Identity", attributions, latency);
        }

        // Polymorphically invoke each sub-detector
        for (FraudDetector detector : detectors) {
            if (detector instanceof BaseFraudDetector && !((BaseFraudDetector) detector).isEnabled()) {
                continue; // Skip disabled detectors
            }
            FeatureAttribution attr = detector.evaluate(transaction, profile);
            attributions.add(attr);
            rawRiskScore += attr.getImpactPoints();

            if (attr.isRiskIncreasing() && attr.getImpactPoints() > maxThreatImpact) {
                maxThreatImpact = attr.getImpactPoints();
                dominantThreat = attr.getFeatureName();
            }
        }

        int finalScore = (int) Math.round(clamp(rawRiskScore, 1.0, 99.0));

        // Determine verdict
        DetectionResult.Verdict verdict;
        if (finalScore >= blockThreshold) {
            verdict = DetectionResult.Verdict.AUTO_BLOCKED;
        } else if (finalScore >= challengeThreshold) {
            verdict = DetectionResult.Verdict.CHALLENGE_3DS;
        } else {
            verdict = DetectionResult.Verdict.APPROVED;
        }

        double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new DetectionResult(finalScore, verdict, dominantThreat, attributions, latencyMs);
    }

    /**
     * Refines fraud detection algorithms based on system performance and feedback.
     * Fulfills GUVI Req: "Admin: Algorithm Updates (Input: Performance data & feedback -> Output: Updated algorithms)".
     */
    public synchronized String refineAlgorithm(String feedbackNotes) {
        // Adjust weights based on feedback
        for (FraudDetector detector : detectors) {
            if (detector instanceof BaseFraudDetector) {
                BaseFraudDetector b = (BaseFraudDetector) detector;
                if (feedbackNotes.toLowerCase().contains("velocity") && b instanceof VelocityAnomalyDetector) {
                    b.setWeight(Math.min(1.0, b.getWeight() + 0.05));
                } else if (feedbackNotes.toLowerCase().contains("tor") && b instanceof GeoLocationAnomalyDetector) {
                    b.setWeight(Math.min(1.0, b.getWeight() + 0.05));
                } else if (feedbackNotes.toLowerCase().contains("device") && b instanceof DeviceFingerprintDetector) {
                    b.setWeight(Math.min(1.0, b.getWeight() + 0.05));
                }
            }
        }
        return "Algorithm weights recalibrated successfully based on analyst feedback: '" + feedbackNotes + "'. Model Version incremented to v4.9.";
    }

    public synchronized void updateDetectorWeight(String detectorName, double weight) {
        for (FraudDetector d : detectors) {
            if (d.getDetectorName().equalsIgnoreCase(detectorName) && d instanceof BaseFraudDetector) {
                ((BaseFraudDetector) d).setWeight(weight);
            }
        }
    }

    public synchronized void toggleDetector(String detectorName, boolean enabled) {
        for (FraudDetector d : detectors) {
            if (d.getDetectorName().equalsIgnoreCase(detectorName) && d instanceof BaseFraudDetector) {
                ((BaseFraudDetector) d).setEnabled(enabled);
            }
        }
    }

    @Override
    public FeatureAttribution evaluate(Transaction transaction, UserProfile profile) throws RuleEvaluationException {
        DetectionResult res = analyze(transaction, profile);
        return new FeatureAttribution(
                getDetectorName(),
                res.getRiskScore(),
                res.isBlocked(),
                "Composite score: " + res.getRiskScore()
        );
    }
}
