package com.sentinel.supabase;

import com.sentinel.backend.util.JsonUtils;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.SystemConfiguration;

import java.util.*;

/**
 * Supabase REST API Repository for System Configurations.
 * Communicates via PostgREST HTTPS endpoints.
 */
public class SupabaseConfigurationRepository {

    private final SupabaseClient client;

    public SupabaseConfigurationRepository() {
        this.client = SupabaseClient.getInstance();
    }

    public void save(SystemConfiguration config) throws DatabaseException {
        if (config == null) return;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("config_key", "DEFAULT");
        map.put("block_threshold", config.getBlockThreshold());
        map.put("challenge_threshold", config.getChallengeThreshold());
        map.put("velocity_limit", config.getVelocityBurstLimit());
        map.put("max_amount", config.getMaxAmountCeiling());
        map.put("tor_block", config.isTorHardBlock());
        map.put("model_version", config.getActiveModelVersion());

        String json = JsonUtils.toJson(map);
        client.upsert("system_configurations", json, "config_key");
    }

    public SystemConfiguration findById(String id) throws DatabaseException {
        String key = (id != null && !id.trim().isEmpty()) ? id.trim() : "DEFAULT";
        String response = client.get("system_configurations", "config_key=eq." + key + "&select=*");
        Object parsed = JsonUtils.parse(response);

        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return mapToConfig((Map<?, ?>) list.get(0));
            }
        }
        return new SystemConfiguration();
    }

    public List<SystemConfiguration> findAll() throws DatabaseException {
        List<SystemConfiguration> list = new ArrayList<>();
        list.add(findById("DEFAULT"));
        return list;
    }

    private SystemConfiguration mapToConfig(Map<?, ?> map) {
        int block = getInteger(map, "block_threshold", 80);
        int challenge = getInteger(map, "challenge_threshold", 55);
        int velocity = getInteger(map, "velocity_limit", 3);
        double maxAmount = getDouble(map, "max_amount", 10000.0);
        boolean torBlock = getBoolean(map, "tor_block", true);
        String modelVer = getString(map, "model_version", "v4.8-Neural-Ensemble");

        return new SystemConfiguration(block, challenge, velocity, maxAmount, torBlock, modelVer);
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
