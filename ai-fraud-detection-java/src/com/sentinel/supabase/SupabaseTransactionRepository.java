package com.sentinel.supabase;

import com.sentinel.backend.util.JsonUtils;
import com.sentinel.exception.DatabaseException;
import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.DetectionResult;
import com.sentinel.model.Transaction;

import java.util.*;

/**
 * Supabase REST API Repository for Transactions.
 * Communicates via PostgREST HTTPS endpoints.
 */
public class SupabaseTransactionRepository {

    private final SupabaseClient client;

    public SupabaseTransactionRepository() {
        this.client = SupabaseClient.getInstance();
    }

    public void save(Transaction tx) throws DatabaseException {
        if (tx == null) return;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tx_id", tx.getTransactionId());
        map.put("user_id", tx.getUserId());
        map.put("user_name", tx.getUserFullName());
        map.put("card_masked", tx.getCardMasked());
        map.put("bank", tx.getBank() != null ? tx.getBank() : "Chase");
        map.put("amount", tx.getAmount());
        map.put("currency", tx.getCurrency() != null ? tx.getCurrency() : "USD");
        map.put("tx_timestamp", tx.getTimestamp());
        map.put("ip_address", tx.getIpAddress());
        map.put("location", tx.getLocation());
        map.put("device_hash", tx.getDeviceHash());
        map.put("is_vpn_tor", tx.isVpnOrTor());

        if (tx.getResult() != null) {
            map.put("risk_score", tx.getResult().getRiskScore());
            map.put("verdict", tx.getResult().getVerdict() != null ? tx.getResult().getVerdict().name() : "APPROVED");
            map.put("dominant_threat", tx.getResult().getDominantThreat());
        }

        map.put("analyst_status", tx.getAnalystStatus() != null ? tx.getAnalystStatus() : "PENDING");

        String json = JsonUtils.toJson(map);
        client.upsert("transactions", json, "tx_id");
    }

    public Transaction findById(String txId) throws DatabaseException {
        if (txId == null || txId.trim().isEmpty()) return null;

        String response = client.get("transactions", "tx_id=eq." + txId.trim() + "&select=*");
        Object parsed = JsonUtils.parse(response);

        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return mapToTransaction((Map<?, ?>) list.get(0));
            }
        }
        return null;
    }

    public List<Transaction> getRecentTransactions(int limit) throws DatabaseException {
        String response = client.get("transactions", "select=*&order=tx_timestamp.desc&limit=" + limit);
        Object parsed = JsonUtils.parse(response);

        List<Transaction> transactions = new ArrayList<>();
        if (parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            for (Object item : list) {
                if (item instanceof Map) {
                    Transaction tx = mapToTransaction((Map<?, ?>) item);
                    if (tx != null) {
                        transactions.add(tx);
                    }
                }
            }
        }
        return transactions;
    }

    public List<Transaction> findAll() throws DatabaseException {
        return getRecentTransactions(50);
    }

    public void updateAnalystStatus(String txId, String status) throws DatabaseException {
        if (txId == null || status == null) return;
        Map<String, Object> map = Collections.singletonMap("analyst_status", status);
        client.patch("transactions", "tx_id=eq." + txId.trim(), JsonUtils.toJson(map));
    }

    public void delete(String txId) throws DatabaseException {
        if (txId == null || txId.trim().isEmpty()) return;
        client.delete("transactions", "tx_id=eq." + txId.trim());
    }

    private Transaction mapToTransaction(Map<?, ?> map) {
        try {
            String txId = getString(map, "tx_id");
            String userId = getString(map, "user_id");
            String userName = getString(map, "user_name");
            String cardMasked = getString(map, "card_masked");
            String bank = getString(map, "bank", "Chase");
            double amount = getDouble(map, "amount", 0.0);
            String currency = getString(map, "currency", "USD");
            long timestamp = getLong(map, "tx_timestamp", System.currentTimeMillis());
            String ip = getString(map, "ip_address", "127.0.0.1");
            String location = getString(map, "location", "Unknown");
            String deviceHash = getString(map, "device_hash", "DEV_UNKNOWN");
            boolean isVpnTor = getBoolean(map, "is_vpn_tor", false);

            Transaction tx = new Transaction(txId, userId, userName, cardMasked, bank, amount, currency, timestamp, ip, location, deviceHash, isVpnTor);

            String verdictStr = getString(map, "verdict");
            Integer riskScore = getInteger(map, "risk_score");
            String dominantThreat = getString(map, "dominant_threat");

            if (verdictStr != null && !verdictStr.isEmpty()) {
                try {
                    DetectionResult.Verdict verdict = DetectionResult.Verdict.valueOf(verdictStr);
                    DetectionResult result = new DetectionResult(riskScore != null ? riskScore : 0, verdict, dominantThreat, Collections.emptyList(), 10.0);
                    tx.setResult(result);
                } catch (Exception ignored) {}
            }

            String analystStatus = getString(map, "analyst_status", "PENDING");
            tx.setAnalystStatus(analystStatus);
            return tx;

        } catch (InvalidTransactionException e) {
            System.err.println("[MAPPING ERROR] Failed to map transaction: " + e.getMessage());
            return null;
        }
    }

    private String getString(Map<?, ?> map, String key) {
        return getString(map, key, null);
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

    private Integer getInteger(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultVal;
    }
}
