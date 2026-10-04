package com.sentinel.model.detectors;

import com.sentinel.exception.RuleEvaluationException;
import com.sentinel.model.BaseFraudDetector;
import com.sentinel.model.FeatureAttribution;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects device hardware spoofing, canvas mutations, and blacklisted bot devices.
 * Demonstrates Collections (Set, Collections.unmodifiableSet) & Polymorphism.
 */
public class DeviceFingerprintDetector extends BaseFraudDetector {

    private final Set<String> blacklistedDeviceHashes = Collections.synchronizedSet(new HashSet<>());

    public DeviceFingerprintDetector(double weight) {
        super("Device Integrity Sentinel", weight);
        // Pre-populate known adversarial device fingerprints
        blacklistedDeviceHashes.add("DEV_SPOOFED_EMULATOR_01");
        blacklistedDeviceHashes.add("DEV_HEADLESS_SELENIUM_BOT");
        blacklistedDeviceHashes.add("DEV_MALICIOUS_CANVAS_MUTATION");
    }

    public void blacklistDevice(String deviceHash) {
        blacklistedDeviceHashes.add(deviceHash);
    }

    @Override
    public FeatureAttribution evaluate(Transaction transaction, UserProfile profile) throws RuleEvaluationException {
        try {
            String deviceHash = transaction.getDeviceHash();

            // 1. Hard blacklisted device check
            if (blacklistedDeviceHashes.contains(deviceHash)) {
                return new FeatureAttribution(
                        "Blacklisted Adversarial Device Fingerprint",
                        50.0 * weight,
                        true,
                        "Hardware canvas signature matches known credential-stuffing bot cluster"
                );
            }

            // 2. Profile trusted device match
            if (profile != null && profile.getTrustedDeviceHash() != null) {
                if (profile.getTrustedDeviceHash().equals(deviceHash)) {
                    return new FeatureAttribution(
                            "Verified Trusted Device",
                            -25.0 * weight,
                            false,
                            "Device hash matches customer's established primary device"
                    );
                } else {
                    return new FeatureAttribution(
                            "Unrecognized New Hardware Fingerprint",
                            28.0 * weight,
                            true,
                            "First transaction seen on this hardware/browser profile"
                    );
                }
            }

            // Default neutral attribution for new profiles
            return new FeatureAttribution(
                    "Standard Device Signal",
                    5.0 * weight,
                    true,
                    "Neutral device score for first-time session"
            );

        } catch (Exception ex) {
            throw new RuleEvaluationException(getDetectorName(), ex.getMessage(), ex);
        }
    }
}
