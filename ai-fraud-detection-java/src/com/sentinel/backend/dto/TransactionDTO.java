package com.sentinel.backend.dto;

import com.sentinel.model.DetectionResult;
import com.sentinel.model.FeatureAttribution;
import com.sentinel.model.Transaction;

import java.util.*;

/**
 * Data Transfer Object for transactions and fraud evaluation results.
 */
public class TransactionDTO {

    public static Map<String, Object> toMap(Transaction tx) {
        if (tx == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("transactionId", tx.getTransactionId());
        map.put("userId", tx.getUserId());
        map.put("userFullName", tx.getUserFullName());
        map.put("cardMasked", tx.getCardMasked());
        map.put("bank", tx.getBank());
        map.put("amount", tx.getAmount());
        map.put("currency", tx.getCurrency());
        map.put("timestamp", tx.getTimestamp());
        map.put("ipAddress", tx.getIpAddress());
        map.put("location", tx.getLocation());
        map.put("deviceHash", tx.getDeviceHash());
        map.put("vpnOrTor", tx.isVpnOrTor());
        map.put("analystStatus", tx.getAnalystStatus());

        DetectionResult result = tx.getResult();
        if (result != null) {
            Map<String, Object> eval = new LinkedHashMap<>();
            eval.put("riskScore", result.getRiskScore());
            eval.put("verdict", result.getVerdict().name());
            eval.put("dominantThreat", result.getDominantThreat());
            eval.put("latencyMs", result.getLatencyMs());
            eval.put("isApproved", result.isApproved());
            eval.put("isChallenged", result.isChallenged());
            eval.put("isBlocked", result.isBlocked());

            List<Map<String, Object>> attributions = new ArrayList<>();
            for (FeatureAttribution attr : result.getAttributions()) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("featureName", attr.getFeatureName());
                a.put("impactPoints", attr.getImpactPoints());
                a.put("isRiskIncreasing", attr.isRiskIncreasing());
                a.put("description", attr.getDescription());
                attributions.add(a);
            }
            eval.put("attributions", attributions);
            map.put("fraudEvaluation", eval);
        } else {
            map.put("fraudEvaluation", null);
        }

        return map;
    }

    public static List<Map<String, Object>> toList(List<Transaction> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null) {
            for (Transaction tx : list) {
                result.add(toMap(tx));
            }
        }
        return result;
    }
}
