package com.sentinel.test;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.server.SentinelApiServer;
import com.sentinel.backend.util.JsonUtils;
import com.sentinel.service.FraudDetectionEngine;
import com.sentinel.supabase.SupabaseConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-End Automated Integration Test Suite for Sentinel AI REST API Backend.
 */
public class BackendIntegrationTest {

    private static String baseUrl;

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  SENTINEL AI // Backend REST API & Integration Test Suite           ");
        System.out.println("=====================================================================");

        int passed = 0;
        int total = 10;
        SentinelApiServer server = null;

        try {
            // 1. Boot up backend server on test port
            int testPort = 8089;
            FraudDetectionEngine engine = new FraudDetectionEngine();
            server = new SentinelApiServer(testPort, engine);
            server.start();
            baseUrl = server.getBaseUrl();
            System.out.println("[BOOT] Test server started at: " + baseUrl);

            // Test 1: Health Probe
            System.out.println("\n[TEST 1/10] Testing GET /api/health...");
            Map<String, Object> healthResp = httpGet("/api/health");
            if (!Boolean.TRUE.equals(healthResp.get("success"))) {
                throw new RuntimeException("Health check returned failure: " + healthResp);
            }
            System.out.println("  --> Health probe returned: " + healthResp.get("message"));
            passed++;

            // Test 2: Clean Transaction Submission & AI Scoring
            System.out.println("\n[TEST 2/10] Testing POST /api/transactions (Clean Transaction)...");
            Map<String, Object> cleanTxReq = new LinkedHashMap<>();
            cleanTxReq.put("transactionId", "TX-API-CLEAN-001");
            cleanTxReq.put("userId", "USR-101");
            cleanTxReq.put("userFullName", "Alexandre V.");
            cleanTxReq.put("cardMasked", "Visa •• 8492");
            cleanTxReq.put("bank", "Chase");
            cleanTxReq.put("amount", 85.0);
            cleanTxReq.put("currency", "USD");
            cleanTxReq.put("location", "London, UK");
            cleanTxReq.put("deviceHash", "DEV_MACBOOK_PRO_9");
            cleanTxReq.put("vpnOrTor", false);

            Map<String, Object> cleanResp = httpPost("/api/transactions", cleanTxReq);
            if (!Boolean.TRUE.equals(cleanResp.get("success"))) {
                throw new RuntimeException("Transaction submission failed: " + cleanResp);
            }
            Map<String, Object> cleanData = (Map<String, Object>) cleanResp.get("data");
            Map<String, Object> cleanEval = (Map<String, Object>) cleanData.get("fraudEvaluation");
            int cleanRisk = ((Number) cleanEval.get("riskScore")).intValue();
            String cleanVerdict = (String) cleanEval.get("verdict");
            System.out.println("  --> Ingested TX-API-CLEAN-001 | Risk: " + cleanRisk + " | Verdict: " + cleanVerdict);
            if (!"APPROVED".equals(cleanVerdict)) {
                throw new RuntimeException("Expected clean transaction to be APPROVED, got: " + cleanVerdict);
            }
            passed++;

            // Test 3: Attack Transaction Ingestion & Threat Attribution
            System.out.println("\n[TEST 3/10] Testing POST /api/transactions (Adversarial Vector)...");
            Map<String, Object> attackReq = new LinkedHashMap<>();
            attackReq.put("transactionId", "TX-API-ATTACK-002");
            attackReq.put("userId", "USR-BAD");
            attackReq.put("userFullName", "Botnet Cluster Alpha");
            attackReq.put("cardMasked", "Virtual •• 4019");
            attackReq.put("bank", "Revolut");
            attackReq.put("amount", 4800.0);
            attackReq.put("currency", "USD");
            attackReq.put("location", "Kyiv, UA (Tor Exit)");
            attackReq.put("deviceHash", "DEV_SPOOFED_EMULATOR_01");
            attackReq.put("vpnOrTor", true);

            Map<String, Object> attackResp = httpPost("/api/transactions", attackReq);
            Map<String, Object> attackData = (Map<String, Object>) attackResp.get("data");
            Map<String, Object> attackEval = (Map<String, Object>) attackData.get("fraudEvaluation");
            int attackRisk = ((Number) attackEval.get("riskScore")).intValue();
            String attackVerdict = (String) attackEval.get("verdict");
            String threat = (String) attackEval.get("dominantThreat");
            System.out.println("  --> Ingested TX-API-ATTACK-002 | Risk: " + attackRisk + " | Verdict: " + attackVerdict + " | Threat: " + threat);
            if (!"AUTO_BLOCKED".equals(attackVerdict)) {
                throw new RuntimeException("Expected attack transaction to be AUTO_BLOCKED, got: " + attackVerdict);
            }
            passed++;

            // Test 4: Query Transaction History from Supabase
            System.out.println("\n[TEST 4/10] Testing GET /api/transactions & GET /api/transactions/{id}...");
            Map<String, Object> listResp = httpGet("/api/transactions?limit=10");
            List<?> txList = (List<?>) listResp.get("data");
            System.out.println("  --> Retrieved " + txList.size() + " recent transactions from database.");
            if (txList.isEmpty()) {
                throw new RuntimeException("Expected transaction history to contain persisted transactions");
            }

            Map<String, Object> singleTxResp = httpGet("/api/transactions/TX-API-CLEAN-001");
            Map<String, Object> singleTx = (Map<String, Object>) singleTxResp.get("data");
            if (singleTx == null || !"TX-API-CLEAN-001".equals(singleTx.get("transactionId"))) {
                throw new RuntimeException("Failed to fetch single transaction by ID");
            }
            System.out.println("  --> Successfully queried transaction by ID: " + singleTx.get("transactionId"));
            passed++;

            // Test 5: Update Analyst Status & Audit Log
            System.out.println("\n[TEST 5/10] Testing POST /api/transactions/{id}/status & Audit Logging...");
            Map<String, Object> statusReq = new LinkedHashMap<>();
            statusReq.put("status", "APPROVED_OVERRIDE");
            statusReq.put("analystName", "Lead Analyst Miller");

            Map<String, Object> statusResp = httpPost("/api/transactions/TX-API-ATTACK-002/status", statusReq);
            Map<String, Object> updatedTx = (Map<String, Object>) statusResp.get("data");
            if (!"APPROVED_OVERRIDE".equals(updatedTx.get("analystStatus"))) {
                throw new RuntimeException("Failed to update analyst status");
            }
            System.out.println("  --> Updated TX-API-ATTACK-002 analystStatus to: " + updatedTx.get("analystStatus"));
            passed++;

            // Test 6: User Profiles & Blacklist Management
            System.out.println("\n[TEST 6/10] Testing GET /api/users & POST /api/users/{id}/blacklist...");
            Map<String, Object> usersResp = httpGet("/api/users");
            List<?> userList = (List<?>) usersResp.get("data");
            System.out.println("  --> Retrieved " + userList.size() + " user profiles.");

            Map<String, Object> blacklistReq = new LinkedHashMap<>();
            blacklistReq.put("isBlacklisted", true);
            blacklistReq.put("analystName", "Security Officer Vance");
            Map<String, Object> blResp = httpPost("/api/users/USR-102/blacklist", blacklistReq);
            Map<String, Object> blProfile = (Map<String, Object>) blResp.get("data");
            if (!Boolean.TRUE.equals(blProfile.get("isBlacklisted"))) {
                throw new RuntimeException("Failed to update user blacklist status");
            }
            System.out.println("  --> Successfully set USR-102 blacklist status to: true");
            passed++;

            // Test 7: Fraud Alerts & Audit Logs Queries
            System.out.println("\n[TEST 7/10] Testing GET /api/alerts & GET /api/audit-logs...");
            Map<String, Object> alertsResp = httpGet("/api/alerts");
            List<?> alertsList = (List<?>) alertsResp.get("data");
            System.out.println("  --> Active fraud alerts count: " + alertsList.size());

            Map<String, Object> auditResp = httpGet("/api/audit-logs");
            List<?> auditList = (List<?>) auditResp.get("data");
            System.out.println("  --> Audit logs count: " + auditList.size());
            if (auditList.isEmpty()) {
                throw new RuntimeException("Expected audit logs to be recorded");
            }
            passed++;

            // Test 8: Real-Time Metrics API
            System.out.println("\n[TEST 8/10] Testing GET /api/metrics...");
            Map<String, Object> metricsResp = httpGet("/api/metrics");
            Map<String, Object> metricsData = (Map<String, Object>) metricsResp.get("data");
            System.out.println("  --> Total Screened: " + metricsData.get("totalScreened"));
            System.out.println("  --> Blocked Capital: $" + metricsData.get("blockedCapital"));
            System.out.println("  --> Avg Latency: " + metricsData.get("avgLatencyMs") + " ms");
            passed++;

            // Test 9: System Configuration Management
            System.out.println("\n[TEST 9/10] Testing GET /api/config & POST /api/config...");
            Map<String, Object> configReq = new LinkedHashMap<>();
            configReq.put("blockThreshold", 82);
            configReq.put("challengeThreshold", 58);
            configReq.put("velocityBurstLimit", 4);
            configReq.put("maxAmountCeiling", 15000.0);
            configReq.put("torHardBlock", true);
            configReq.put("activeModelVersion", "v4.9-Neural-Ensemble-REST");
            configReq.put("analystName", "Admin Systems");

            Map<String, Object> configResp = httpPost("/api/config", configReq);
            Map<String, Object> configData = (Map<String, Object>) configResp.get("data");
            if (!"v4.9-Neural-Ensemble-REST".equals(configData.get("activeModelVersion"))) {
                throw new RuntimeException("Failed to update system configuration");
            }
            System.out.println("  --> Configuration updated: Block=" + configData.get("blockThreshold") + ", Model=" + configData.get("activeModelVersion"));
            passed++;

            // Test 10: Traffic Simulation API Controls
            System.out.println("\n[TEST 10/10] Testing POST /api/simulation/start & /stop...");
            Map<String, Object> simStart = httpPost("/api/simulation/start", new LinkedHashMap<>());
            System.out.println("  --> Simulation start response: " + simStart.get("message"));

            Map<String, Object> simAttack = httpPost("/api/simulation/attack", new LinkedHashMap<>());
            System.out.println("  --> Injected attack response: " + simAttack.get("message"));

            Map<String, Object> simStop = httpPost("/api/simulation/stop", new LinkedHashMap<>());
            System.out.println("  --> Simulation stop response: " + simStop.get("message"));
            passed++;

            System.out.println("\n=====================================================================");
            System.out.printf("  BACKEND INTEGRATION TEST: %d/%d TESTS PASSED (100%% SUCCESS)\n", passed, total);
            System.out.println("=====================================================================");

        } catch (Exception e) {
            System.err.println("\n[BACKEND TEST FAILURE] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private static Map<String, Object> httpGet(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
        String json = readStream(is);
        return JsonUtils.parseObject(json);
    }

    private static Map<String, Object> httpPost(String path, Map<String, Object> body) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        String jsonBody = JsonUtils.toJson(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
        String json = readStream(is);
        return JsonUtils.parseObject(json);
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return "{}";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
