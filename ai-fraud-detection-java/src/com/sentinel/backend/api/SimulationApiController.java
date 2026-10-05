package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.service.FraudBackendService;
import com.sun.net.httpserver.HttpExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API Controller for live traffic simulation and adversarial attack injection.
 * Endpoints:
 * - GET  /api/simulation/status  -> Check simulation stream status
 * - POST /api/simulation/start   -> Start background stream
 * - POST /api/simulation/stop    -> Stop background stream
 * - POST /api/simulation/attack  -> Inject simulated adversarial attack
 */
public class SimulationApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;

    public SimulationApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        if (path.endsWith("/start") && "POST".equals(method)) {
            fraudService.startSimulation();
            sendJson(exchange, 200, ApiResponse.ok("Background transaction traffic simulation started", getStatusMap()));
        } else if (path.endsWith("/stop") && "POST".equals(method)) {
            fraudService.stopSimulation();
            sendJson(exchange, 200, ApiResponse.ok("Background transaction traffic simulation stopped", getStatusMap()));
        } else if (path.endsWith("/attack") && "POST".equals(method)) {
            fraudService.injectSimulatedAttack();
            sendJson(exchange, 200, ApiResponse.ok("Adversarial attack vector injected into detection queue", getStatusMap()));
        } else if ("GET".equals(method)) {
            sendJson(exchange, 200, ApiResponse.ok("Simulation status", getStatusMap()));
        } else {
            sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
        }
    }

    private Map<String, Object> getStatusMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("isSimulationRunning", fraudService.isSimulationRunning());
        return map;
    }
}
