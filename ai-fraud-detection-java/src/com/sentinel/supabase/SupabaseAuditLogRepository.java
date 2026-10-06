package com.sentinel.supabase;

import com.sentinel.backend.util.JsonUtils;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.AuditLog;

import java.util.*;

/**
 * Supabase REST API Repository for Audit Logs.
 * Communicates via PostgREST HTTPS endpoints.
 */
public class SupabaseAuditLogRepository {

    private final SupabaseClient client;

    public SupabaseAuditLogRepository() {
        this.client = SupabaseClient.getInstance();
    }

    public void save(AuditLog log) throws DatabaseException {
        if (log == null) return;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tx_id", log.getTransactionId());
        map.put("analyst_name", log.getAnalystName());
        map.put("action_taken", log.getActionTaken());
        map.put("timestamp", log.getTimestamp());

        String json = JsonUtils.toJson(map);
        String response = client.post("audit_logs", json);
        Object parsed = JsonUtils.parse(response);

        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                Long generatedId = getLong((Map<?, ?>) list.get(0), "log_id", 0L);
                if (generatedId > 0) {
                    log.setLogId(generatedId);
                }
            }
        }
    }

    public AuditLog findById(Long logId) throws DatabaseException {
        if (logId == null) return null;

        String response = client.get("audit_logs", "log_id=eq." + logId + "&select=*");
        Object parsed = JsonUtils.parse(response);

        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return mapToAuditLog((Map<?, ?>) list.get(0));
            }
        }
        return null;
    }

    public List<AuditLog> findByTransactionId(String txId) throws DatabaseException {
        if (txId == null || txId.trim().isEmpty()) return Collections.emptyList();

        String response = client.get("audit_logs", "tx_id=eq." + txId.trim() + "&select=*&order=timestamp.desc");
        Object parsed = JsonUtils.parse(response);

        List<AuditLog> list = new ArrayList<>();
        if (parsed instanceof List) {
            for (Object item : (List<?>) parsed) {
                if (item instanceof Map) {
                    AuditLog log = mapToAuditLog((Map<?, ?>) item);
                    if (log != null) list.add(log);
                }
            }
        }
        return list;
    }

    public List<AuditLog> getRecentLogs(int limit) throws DatabaseException {
        String response = client.get("audit_logs", "select=*&order=timestamp.desc&limit=" + limit);
        Object parsed = JsonUtils.parse(response);

        List<AuditLog> list = new ArrayList<>();
        if (parsed instanceof List) {
            for (Object item : (List<?>) parsed) {
                if (item instanceof Map) {
                    AuditLog log = mapToAuditLog((Map<?, ?>) item);
                    if (log != null) list.add(log);
                }
            }
        }
        return list;
    }

    public List<AuditLog> findAll() throws DatabaseException {
        return getRecentLogs(100);
    }

    private AuditLog mapToAuditLog(Map<?, ?> map) {
        long logId = getLong(map, "log_id", 0L);
        String txId = getString(map, "tx_id");
        String analyst = getString(map, "analyst_name");
        String action = getString(map, "action_taken");
        long timestamp = getLong(map, "timestamp", System.currentTimeMillis());

        return new AuditLog(logId, txId, analyst, action, timestamp);
    }

    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : "";
    }

    private long getLong(Map<?, ?> map, String key, long defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }
}
