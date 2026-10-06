package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.supabase.SupabaseClient;
import com.sentinel.supabase.SupabaseConfig;
import com.sun.net.httpserver.HttpExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API Controller for health probes and system readiness.
 * Endpoint:
 * - GET /api/health -> Health status of server, Supabase API connection, and AI engine
 */
public class HealthApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;
    private final long startTime = System.currentTimeMillis();

    public HealthApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "Sentinel AI Fraud Detection Matrix");
        health.put("uptimeSeconds", (System.currentTimeMillis() - startTime) / 1000);

        // Test Supabase API Connectivity
        boolean supabaseOk = false;
        String supabaseError = null;
        try {
            supabaseOk = SupabaseConfig.isConfigured() && SupabaseClient.getInstance().checkConnection();
        } catch (Exception e) {
            supabaseError = e.getMessage();
        }

        Map<String, Object> dbStatus = new LinkedHashMap<>();
        dbStatus.put("connected", supabaseOk);
        dbStatus.put("configured", SupabaseConfig.isConfigured());
        dbStatus.put("service", "Supabase REST API");
        dbStatus.put("url", SupabaseConfig.getSupabaseUrl());
        if (supabaseError != null) dbStatus.put("error", supabaseError);
        health.put("database", dbStatus);

        // Engine Status
        Map<String, Object> engineStatus = new LinkedHashMap<>();
        engineStatus.put("detectorsActive", fraudService.getEngine().getEnsemble().getDetectors().size());
        engineStatus.put("workerThreads", 4);
        engineStatus.put("simulationRunning", fraudService.isSimulationRunning());
        health.put("engine", engineStatus);

        sendJson(exchange, 200, ApiResponse.ok("Health check completed", health));
    }
}
