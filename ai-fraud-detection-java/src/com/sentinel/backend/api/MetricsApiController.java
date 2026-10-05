package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.dto.SystemConfigDTO;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.service.MetricsAggregator;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * REST API Controller for Real-Time System Metrics & KPIs.
 * Endpoints:
 * - GET /api/metrics -> Real-time operational metrics & detection statistics
 */
public class MetricsApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;

    public MetricsApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();
        if (!"GET".equals(method)) {
            sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
            return;
        }

        MetricsAggregator.MetricsSnapshot snapshot = fraudService.getMetricsSnapshot();
        String modelVersion = fraudService.getSystemConfig() != null
                ? fraudService.getSystemConfig().getActiveModelVersion()
                : "v4.8-Neural-Ensemble";

        Map<String, Object> data = SystemConfigDTO.metricsToMap(snapshot, modelVersion);
        sendJson(exchange, 200, ApiResponse.ok("System metrics retrieved", data));
    }
}
