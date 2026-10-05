package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.dto.SystemConfigDTO;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.model.SystemConfiguration;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * REST API Controller for Admin System Configuration & Detection Thresholds.
 * Endpoints:
 * - GET  /api/config -> Current system configuration parameters
 * - POST /api/config -> Update system thresholds & active model version
 * - PUT  /api/config -> Update system thresholds & active model version
 */
public class ConfigApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;

    public ConfigApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            SystemConfiguration config = fraudService.getSystemConfig();
            if (config == null) {
                config = new SystemConfiguration();
            }
            sendJson(exchange, 200, ApiResponse.ok("System configuration retrieved", SystemConfigDTO.toMap(config)));
        } else if ("POST".equals(method) || "PUT".equals(method)) {
            Map<String, Object> body = parseJsonBody(exchange);

            int blockThreshold = 80;
            Object btObj = body.get("blockThreshold");
            if (btObj instanceof Number) blockThreshold = ((Number) btObj).intValue();

            int challengeThreshold = 55;
            Object ctObj = body.get("challengeThreshold");
            if (ctObj instanceof Number) challengeThreshold = ((Number) ctObj).intValue();

            int velocityLimit = 3;
            Object vlObj = body.get("velocityBurstLimit");
            if (vlObj == null) vlObj = body.get("velocityLimit");
            if (vlObj instanceof Number) velocityLimit = ((Number) vlObj).intValue();

            double maxAmount = 10000.0;
            Object maObj = body.get("maxAmountCeiling");
            if (maObj == null) maObj = body.get("maxAmount");
            if (maObj instanceof Number) maxAmount = ((Number) maObj).doubleValue();

            boolean torHardBlock = true;
            Object torObj = body.get("torHardBlock");
            if (torObj == null) torObj = body.get("torBlock");
            if (torObj instanceof Boolean) torHardBlock = (Boolean) torObj;

            String modelVersion = (String) body.get("activeModelVersion");
            if (modelVersion == null) modelVersion = (String) body.get("modelVersion");
            if (modelVersion == null || modelVersion.trim().isEmpty()) {
                modelVersion = "v4.9-Neural-Ensemble";
            }

            SystemConfiguration newConfig = new SystemConfiguration(
                    blockThreshold, challengeThreshold, velocityLimit,
                    maxAmount, torHardBlock, modelVersion
            );

            String analystName = (String) body.get("analystName");
            fraudService.updateSystemConfig(newConfig, analystName);

            sendJson(exchange, 200, ApiResponse.ok("System configuration updated successfully", SystemConfigDTO.toMap(newConfig)));
        } else {
            sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
        }
    }
}
