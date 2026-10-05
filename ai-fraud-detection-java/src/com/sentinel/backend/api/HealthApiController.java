package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.db.DatabaseConnectionFactory;
import com.sun.net.httpserver.HttpExchange;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API Controller for health probes and system readiness.
 * Endpoint:
 * - GET /api/health -> Health status of server, Supabase connection, and AI engine
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

        // Test Database Connectivity
        boolean dbOk = false;
        String dbError = null;
        try (Connection conn = DatabaseConnectionFactory.getInstance().getConnection()) {
            dbOk = conn != null && !conn.isClosed();
        } catch (Exception e) {
            dbError = e.getMessage();
        }

        Map<String, Object> dbStatus = new LinkedHashMap<>();
        dbStatus.put("connected", dbOk);
        dbStatus.put("url", DatabaseConnectionFactory.getInstance().getDbUrl());
        dbStatus.put("isPostgres", DatabaseConnectionFactory.getInstance().isPostgreSql());
        if (dbError != null) dbStatus.put("error", dbError);
        health.put("database", dbStatus);

        // Engine Status
        Map<String, Object> engineStatus = new LinkedHashMap<>();
        engineStatus.put("detectorsActive", fraudService.getEngine().getEnsemble().getDetectors().size());
        engineStatus.put("workerThreads", 4);
        engineStatus.put("simulationRunning", fraudService.isSimulationRunning());
        health.put("engine", engineStatus);

        sendJson(exchange, dbOk ? 200 : 503, ApiResponse.ok("Health check completed", health));
    }
}
