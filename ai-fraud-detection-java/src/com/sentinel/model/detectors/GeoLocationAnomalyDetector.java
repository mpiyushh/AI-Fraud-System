package com.sentinel.model.detectors;

import com.sentinel.exception.RuleEvaluationException;
import com.sentinel.model.BaseFraudDetector;
import com.sentinel.model.FeatureAttribution;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects impossible travel velocity and Tor/Datacenter proxy exit nodes.
 * Demonstrates Collections (HashSet) & Generics.
 */
public class GeoLocationAnomalyDetector extends BaseFraudDetector {

    private final Set<String> highRiskLocations = new HashSet<>();

    public GeoLocationAnomalyDetector(double weight) {
        super("Geo-Velocity & Proxy Sentry", weight);
        highRiskLocations.add("Tor Exit Node");
        highRiskLocations.add("Anonymous Proxy");
        highRiskLocations.add("Bulletproof Hosting ASN");
    }

    @Override
    public FeatureAttribution evaluate(Transaction transaction, UserProfile profile) throws RuleEvaluationException {
        try {
            // Check 1: Tor / VPN flag
            if (transaction.isVpnOrTor()) {
                double points = 35.0 * weight;
                return new FeatureAttribution(
                        "Anonymized Network Route (Tor/VPN)",
                        points,
                        true,
                        "Request originates from a masked proxy or Tor exit node: " + transaction.getLocation()
                );
            }

            // Check 2: Travel velocity between last seen location and current location
            if (profile != null && profile.getLastSeenLocation() != null) {
                String prevLoc = profile.getLastSeenLocation();
                String currLoc = transaction.getLocation();

                if (!prevLoc.equalsIgnoreCase(currLoc)) {
                    long elapsedMinutes = (transaction.getTimestamp() - profile.getLastSeenTimestamp()) / (1000 * 60);
                    // If changed country/city in less than 30 minutes -> Impossible travel
                    if (elapsedMinutes < 30) {
                        double points = clamp(40.0 * weight, 20.0, 45.0);
                        return new FeatureAttribution(
                                "Impossible Physical Travel Speed",
                                points,
                                true,
                                String.format("Origin jumped from [%s] to [%s] in %d minutes", prevLoc, currLoc, elapsedMinutes)
                        );
                    }
                }
            }

            return new FeatureAttribution(
                    "Standard Geolocation Footprint",
                    -10.0 * weight,
                    false,
                    "Consistent with customer's typical geographic region: " + transaction.getLocation()
            );

        } catch (Exception ex) {
            throw new RuleEvaluationException(getDetectorName(), ex.getMessage(), ex);
        }
    }
}
