package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.dto.SystemConfigDTO;
import com.sentinel.backend.dto.TransactionDTO;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.model.AuditLog;
import com.sentinel.model.Transaction;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Fraud Alerts and Audit Logs.
 * Endpoints:
 * - GET /api/alerts      -> Flagged / Suspicious transactions requiring attention
 * - GET /api/audit-logs  -> Historical audit log records
 */
public class AlertsApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;

    public AlertsApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        if (!"GET".equals(method)) {
            sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
            return;
        }

        Map<String, String> params = parseQueryParams(exchange);
        int limit = 50;
        if (params.containsKey("limit")) {
            try {
                limit = Integer.parseInt(params.get("limit"));
            } catch (NumberFormatException ignored) {}
        }

        if (path.contains("/audit-logs")) {
            List<AuditLog> logs = fraudService.getAuditLogs(limit);
            List<Map<String, Object>> dtos = SystemConfigDTO.auditLogsToList(logs);
            sendJson(exchange, 200, ApiResponse.ok("Audit logs retrieved", dtos));
        } else {
            List<Transaction> alerts = fraudService.getActiveAlerts(limit);
            List<Map<String, Object>> dtos = TransactionDTO.toList(alerts);
            sendJson(exchange, 200, ApiResponse.ok("Active fraud alerts retrieved", dtos));
        }
    }
}
