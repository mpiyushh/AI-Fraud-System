package com.sentinel.model.detectors;

import com.sentinel.exception.RuleEvaluationException;
import com.sentinel.model.BaseFraudDetector;
import com.sentinel.model.FeatureAttribution;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects rapid burst transactions within a rolling time window.
 * Demonstrates Collections (Deque, ConcurrentHashMap) & Generics.
 */
public class VelocityAnomalyDetector extends BaseFraudDetector {

    // User ID -> Deque of transaction timestamps
    private final Map<String, Deque<Long>> userHistory = new ConcurrentHashMap<>();
    private final long windowMillis;
    private final int maxThreshold;

    public VelocityAnomalyDetector(double weight) {
        super("Velocity Burst Monitor", weight);
        this.windowMillis = 60 * 1000L; // 60 seconds
        this.maxThreshold = 3;
    }

    @Override
    public FeatureAttribution evaluate(Transaction transaction, UserProfile profile) throws RuleEvaluationException {
        try {
            String userId = transaction.getUserId();
            long now = transaction.getTimestamp();

            Deque<Long> queue = userHistory.computeIfAbsent(userId, k -> new ArrayDeque<>());
            
            int countInWindow;
            synchronized (queue) {
                // Evict timestamps older than rolling window
                while (!queue.isEmpty() && (now - queue.peekFirst() > windowMillis)) {
                    queue.pollFirst();
                }
                queue.addLast(now);
                countInWindow = queue.size();
            }

            if (countInWindow > maxThreshold) {
                double points = clamp((countInWindow - maxThreshold) * 18.0 * weight, 15.0, 45.0);
                return new FeatureAttribution(
                        "Velocity Burst Anomaly",
                        points,
                        true,
                        String.format("%d transactions detected within 60 seconds (Limit: %d)", countInWindow, maxThreshold)
                );
            } else {
                return new FeatureAttribution(
                        "Normal Velocity Cadence",
                        -5.0 * weight,
                        false,
                        "Transaction rate is within safe consumer bounds (" + countInWindow + " tx/min)"
                );
            }
        } catch (Exception ex) {
            throw new RuleEvaluationException(getDetectorName(), ex.getMessage(), ex);
        }
    }
}
