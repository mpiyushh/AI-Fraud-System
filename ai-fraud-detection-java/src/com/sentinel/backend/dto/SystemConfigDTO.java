package com.sentinel.backend.dto;

import com.sentinel.model.AuditLog;
import com.sentinel.model.SystemConfiguration;
import com.sentinel.service.MetricsAggregator;

import java.util.*;

/**
 * Data Transfer Objects for System Config, Metrics, and Audit Logs.
 */
public class SystemConfigDTO {

    public static Map<String, Object> toMap(SystemConfiguration config) {
        if (config == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("blockThreshold", config.getBlockThreshold());
        map.put("challengeThreshold", config.getChallengeThreshold());
        map.put("velocityBurstLimit", config.getVelocityBurstLimit());
        map.put("maxAmountCeiling", config.getMaxAmountCeiling());
        map.put("torHardBlock", config.isTorHardBlock());
        map.put("activeModelVersion", config.getActiveModelVersion());
        return map;
    }

    public static Map<String, Object> metricsToMap(MetricsAggregator.MetricsSnapshot snapshot, String modelVersion) {
        if (snapshot == null) return Collections.emptyMap();
        long screened = snapshot.getTotalScreened();
        long blocked = snapshot.getTotalBlocked();
        long challenged = snapshot.getTotalChallenged();
        long approved = Math.max(0, screened - blocked - challenged);

        double approvalRate = screened > 0 ? ((double) approved / screened) * 100.0 : 100.0;
        double challengeRate = screened > 0 ? ((double) challenged / screened) * 100.0 : 0.0;
        double blockRate = screened > 0 ? ((double) blocked / screened) * 100.0 : 0.0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalScreened", screened);
        map.put("totalVolume", snapshot.getTotalVolume());
        map.put("blockedCount", blocked);
        map.put("challengedCount", challenged);
        map.put("approvedCount", approved);
        map.put("blockedCapital", snapshot.getBlockedCapital());
        map.put("approvalRatePct", Math.round(approvalRate * 10.0) / 10.0);
        map.put("challengeRatePct", Math.round(challengeRate * 10.0) / 10.0);
        map.put("blockRatePct", Math.round(blockRate * 10.0) / 10.0);
        map.put("avgLatencyMs", Math.round(snapshot.getAvgLatency() * 100.0) / 100.0);
        map.put("falsePositiveRateEstimate", snapshot.getFalsePositiveRate());
        map.put("activeModelVersion", modelVersion);
        return map;
    }

    public static Map<String, Object> auditLogToMap(AuditLog log) {
        if (log == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("logId", log.getLogId());
        map.put("transactionId", log.getTransactionId());
        map.put("analystName", log.getAnalystName());
        map.put("actionTaken", log.getActionTaken());
        map.put("timestamp", log.getTimestamp());
        return map;
    }

    public static List<Map<String, Object>> auditLogsToList(List<AuditLog> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null) {
            for (AuditLog l : list) {
                result.add(auditLogToMap(l));
            }
        }
        return result;
    }
}
