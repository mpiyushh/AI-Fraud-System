package com.sentinel.backend.dto;

import com.sentinel.model.UserProfile;

import java.util.*;

/**
 * Data Transfer Object for User Profiles.
 */
public class UserProfileDTO {

    public static Map<String, Object> toMap(UserProfile profile) {
        if (profile == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", profile.getUserId());
        map.put("fullName", profile.getFullName());
        map.put("primaryLocation", profile.getPrimaryLocation());
        map.put("trustedDeviceHash", profile.getTrustedDeviceHash());
        map.put("historicalAverageAmount", profile.getHistoricalAverageAmount());
        map.put("standardDeviation", profile.getStandardDeviation());
        map.put("transactionCount", profile.getTransactionCount());
        map.put("chargebackCount", profile.getChargebackCount());
        map.put("isBlacklisted", profile.isBlacklisted());
        map.put("lastSeenTimestamp", profile.getLastSeenTimestamp());
        map.put("lastSeenLocation", profile.getLastSeenLocation());
        return map;
    }

    public static List<Map<String, Object>> toList(List<UserProfile> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null) {
            for (UserProfile p : list) {
                result.add(toMap(p));
            }
        }
        return result;
    }
}
