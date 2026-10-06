package com.sentinel.supabase;

import com.sentinel.backend.util.JsonUtils;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.UserProfile;

import java.util.*;

/**
 * Supabase REST API Repository for User Profiles.
 * Communicates via PostgREST HTTPS endpoints.
 */
public class SupabaseUserProfileRepository {

    private final SupabaseClient client;

    public SupabaseUserProfileRepository() {
        this.client = SupabaseClient.getInstance();
    }

    public void save(UserProfile profile) throws DatabaseException {
        if (profile == null) return;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", profile.getUserId());
        map.put("full_name", profile.getFullName());
        map.put("primary_location", profile.getPrimaryLocation());
        map.put("trusted_device", profile.getTrustedDeviceHash());
        map.put("avg_amount", profile.getHistoricalAverageAmount());
        map.put("std_dev", profile.getStandardDeviation());
        map.put("tx_count", profile.getTransactionCount());
        map.put("chargeback_count", profile.getChargebackCount());
        map.put("is_blacklisted", profile.isBlacklisted());
        map.put("last_seen_timestamp", profile.getLastSeenTimestamp());
        map.put("last_seen_location", profile.getLastSeenLocation());

        String json = JsonUtils.toJson(map);
        client.upsert("user_profiles", json, "user_id");
    }

    public UserProfile findById(String userId) throws DatabaseException {
        if (userId == null || userId.trim().isEmpty()) return null;

        String response = client.get("user_profiles", "user_id=eq." + userId.trim() + "&select=*");
        Object parsed = JsonUtils.parse(response);

        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return mapToProfile((Map<?, ?>) list.get(0));
            }
        }
        return null;
    }

    public List<UserProfile> findAll() throws DatabaseException {
        String response = client.get("user_profiles", "select=*");
        Object parsed = JsonUtils.parse(response);

        List<UserProfile> profiles = new ArrayList<>();
        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            for (Object item : list) {
                if (item instanceof Map) {
                    UserProfile profile = mapToProfile((Map<?, ?>) item);
                    if (profile != null) {
                        profiles.add(profile);
                    }
                }
            }
        }
        return profiles;
    }

    public void setBlacklisted(String userId, boolean blacklisted) throws DatabaseException {
        if (userId == null || userId.trim().isEmpty()) return;
        Map<String, Object> map = Collections.singletonMap("is_blacklisted", blacklisted);
        client.patch("user_profiles", "user_id=eq." + userId.trim(), JsonUtils.toJson(map));
    }

    public void delete(String userId) throws DatabaseException {
        if (userId == null || userId.trim().isEmpty()) return;
        client.delete("user_profiles", "user_id=eq." + userId.trim());
    }

    private UserProfile mapToProfile(Map<?, ?> map) {
        String userId = getString(map, "user_id");
        String fullName = getString(map, "full_name");
        String primaryLoc = getString(map, "primary_location");
        String trustedDev = getString(map, "trusted_device");
        double avg = getDouble(map, "avg_amount", 50.0);
        double std = getDouble(map, "std_dev", 20.0);
        int txCount = getInteger(map, "tx_count", 1);
        int cbCount = getInteger(map, "chargeback_count", 0);
        boolean blacklisted = getBoolean(map, "is_blacklisted", false);
        long lastSeenTime = getLong(map, "last_seen_timestamp", System.currentTimeMillis());
        String lastSeenLoc = getString(map, "last_seen_location", primaryLoc);

        UserProfile p = new UserProfile(userId, fullName, primaryLoc, trustedDev, avg, std, txCount, cbCount, blacklisted);
        p.setLastSeenTimestamp(lastSeenTime);
        p.setLastSeenLocation(lastSeenLoc);
        return p;
    }

    private String getString(Map<?, ?> map, String key) {
        return getString(map, key, "");
    }

    private String getString(Map<?, ?> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : defaultVal;
    }

    private double getDouble(Map<?, ?> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private long getLong(Map<?, ?> map, String key, long defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private int getInteger(Map<?, ?> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultVal;
    }
}
