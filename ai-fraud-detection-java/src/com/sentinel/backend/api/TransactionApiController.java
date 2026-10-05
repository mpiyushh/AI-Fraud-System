package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.dto.TransactionDTO;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.model.Transaction;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Transactions and Fraud Evaluation.
 * Endpoints:
 * - POST /api/transactions               -> Ingest & evaluate transaction (sync)
 * - POST /api/transactions/evaluate      -> Ingest & evaluate transaction (sync)
 * - GET  /api/transactions               -> List recent transactions (from Supabase)
 * - GET  /api/transactions/{id}          -> Get transaction by ID
 * - POST /api/transactions/{id}/status   -> Update analyst review status & write audit log
 * - PATCH /api/transactions/{id}/status  -> Update analyst review status & write audit log
 */
public class TransactionApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;

    public TransactionApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        // Strip prefix: /api/transactions
        String subPath = path.substring("/api/transactions".length());
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }

        if (subPath.isEmpty() || "evaluate".equals(subPath)) {
            if ("POST".equals(method)) {
                handleTransactionSubmission(exchange);
            } else if ("GET".equals(method)) {
                handleGetRecentTransactions(exchange);
            } else {
                sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
            }
            return;
        }

        // Subpath could be: "{txId}" or "{txId}/status"
        if (subPath.contains("/status")) {
            String txId = subPath.substring(0, subPath.indexOf("/status"));
            if ("POST".equals(method) || "PATCH".equals(method) || "PUT".equals(method)) {
                handleUpdateAnalystStatus(exchange, txId);
            } else {
                sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
            }
            return;
        }

        // Single transaction by ID
        String txId = subPath;
        if ("GET".equals(method)) {
            handleGetTransactionById(exchange, txId);
        } else {
            sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
        }
    }

    private void handleTransactionSubmission(HttpExchange exchange) throws Exception {
        Map<String, Object> body = parseJsonBody(exchange);
        Transaction tx = fraudService.submitAndEvaluateTransaction(body);
        Map<String, Object> data = TransactionDTO.toMap(tx);

        sendJson(exchange, 201, ApiResponse.ok("Transaction evaluated successfully", data));
    }

    private void handleGetRecentTransactions(HttpExchange exchange) throws Exception {
        Map<String, String> query = parseQueryParams(exchange);
        int limit = 50;
        if (query.containsKey("limit")) {
            try {
                limit = Integer.parseInt(query.get("limit"));
            } catch (NumberFormatException ignored) {}
        }

        List<Transaction> list = fraudService.getRecentTransactions(limit);
        List<Map<String, Object>> dtos = TransactionDTO.toList(list);

        sendJson(exchange, 200, ApiResponse.ok("Recent transactions retrieved", dtos));
    }

    private void handleGetTransactionById(HttpExchange exchange, String txId) throws Exception {
        Transaction tx = fraudService.getTransactionById(txId);
        if (tx == null) {
            sendJson(exchange, 404, ApiResponse.error("Transaction not found: " + txId));
            return;
        }

        sendJson(exchange, 200, ApiResponse.ok("Transaction retrieved", TransactionDTO.toMap(tx)));
    }

    private void handleUpdateAnalystStatus(HttpExchange exchange, String txId) throws Exception {
        Map<String, Object> body = parseJsonBody(exchange);
        String status = (String) body.get("status");
        if (status == null || status.trim().isEmpty()) {
            sendJson(exchange, 400, ApiResponse.error("Field 'status' is required"));
            return;
        }

        String analystName = (String) body.get("analystName");
        boolean updated = fraudService.updateAnalystStatus(txId, status.trim(), analystName);
        if (!updated) {
            sendJson(exchange, 404, ApiResponse.error("Transaction not found: " + txId));
            return;
        }

        Transaction tx = fraudService.getTransactionById(txId);
        sendJson(exchange, 200, ApiResponse.ok("Analyst status updated to " + status, TransactionDTO.toMap(tx)));
    }
}
