package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.dto.UserProfileDTO;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.model.UserProfile;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for User Profile and Identity Management.
 * Endpoints:
 * - GET  /api/users                  -> List all user profiles
 * - GET  /api/users/{id}             -> Get user profile by ID
 * - POST /api/users                  -> Create or update user profile
 * - PUT  /api/users/{id}             -> Update user profile
 * - POST /api/users/{id}/blacklist   -> Update blacklist status
 */
public class UserApiController extends BaseApiHandler {

    private final FraudBackendService fraudService;

    public UserApiController(FraudBackendService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        // Strip prefix: /api/users
        String subPath = path.substring("/api/users".length());
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }

        if (subPath.isEmpty()) {
            if ("GET".equals(method)) {
                handleGetAllUsers(exchange);
            } else if ("POST".equals(method)) {
                handleSaveUser(exchange, null);
            } else {
                sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
            }
            return;
        }

        if (subPath.contains("/blacklist")) {
            String userId = subPath.substring(0, subPath.indexOf("/blacklist"));
            if ("POST".equals(method) || "PUT".equals(method)) {
                handleBlacklistToggle(exchange, userId);
            } else {
                sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
            }
            return;
        }

        // Single user by ID
        String userId = subPath;
        if ("GET".equals(method)) {
            handleGetUserById(exchange, userId);
        } else if ("PUT".equals(method) || "POST".equals(method)) {
            handleSaveUser(exchange, userId);
        } else {
            sendJson(exchange, 405, ApiResponse.error("Method Not Allowed: " + method));
        }
    }

    private void handleGetAllUsers(HttpExchange exchange) throws Exception {
        List<UserProfile> list = fraudService.getAllUsers();
        List<Map<String, Object>> dtos = UserProfileDTO.toList(list);
        sendJson(exchange, 200, ApiResponse.ok("User profiles retrieved", dtos));
    }

    private void handleGetUserById(HttpExchange exchange, String userId) throws Exception {
        UserProfile profile = fraudService.getUserProfile(userId);
        if (profile == null) {
            sendJson(exchange, 404, ApiResponse.error("User profile not found: " + userId));
            return;
        }
        sendJson(exchange, 200, ApiResponse.ok("User profile retrieved", UserProfileDTO.toMap(profile)));
    }

    private void handleSaveUser(HttpExchange exchange, String pathUserId) throws Exception {
        Map<String, Object> body = parseJsonBody(exchange);

        String userId = pathUserId != null ? pathUserId : (String) body.get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            sendJson(exchange, 400, ApiResponse.error("Field 'userId' is required"));
            return;
        }

        String fullName = (String) body.get("fullName");
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = "Customer " + userId;
        }

        String primaryLocation = (String) body.get("primaryLocation");
        String trustedDevice = (String) body.get("trustedDeviceHash");
        if (trustedDevice == null) {
            trustedDevice = (String) body.get("trustedDevice");
        }

        double avgAmount = 50.0;
        Object avgObj = body.get("historicalAverageAmount");
        if (avgObj == null) avgObj = body.get("avgAmount");
        if (avgObj instanceof Number) {
            avgAmount = ((Number) avgObj).doubleValue();
        }

        double stdDev = 20.0;
        Object stdObj = body.get("standardDeviation");
        if (stdObj == null) stdObj = body.get("stdDev");
        if (stdObj instanceof Number) {
            stdDev = ((Number) stdObj).doubleValue();
        }

        int txCount = 1;
        Object countObj = body.get("transactionCount");
        if (countObj == null) countObj = body.get("txCount");
        if (countObj instanceof Number) {
            txCount = ((Number) countObj).intValue();
        }

        int chargebacks = 0;
        Object cbObj = body.get("chargebackCount");
        if (cbObj instanceof Number) {
            chargebacks = ((Number) cbObj).intValue();
        }

        boolean blacklisted = false;
        Object blObj = body.get("isBlacklisted");
        if (blObj == null) blObj = body.get("blacklisted");
        if (blObj instanceof Boolean) {
            blacklisted = (Boolean) blObj;
        }

        UserProfile profile = new UserProfile(
                userId, fullName, primaryLocation, trustedDevice,
                avgAmount, stdDev, txCount, chargebacks, blacklisted
        );

        fraudService.saveUserProfile(profile);

        sendJson(exchange, 200, ApiResponse.ok("User profile saved successfully", UserProfileDTO.toMap(profile)));
    }

    private void handleBlacklistToggle(HttpExchange exchange, String userId) throws Exception {
        Map<String, Object> body = parseJsonBody(exchange);
        boolean blacklisted = true;
        Object blObj = body.get("isBlacklisted");
        if (blObj == null) blObj = body.get("blacklisted");
        if (blObj instanceof Boolean) {
            blacklisted = (Boolean) blObj;
        }

        String analystName = (String) body.get("analystName");
        boolean updated = fraudService.setUserBlacklistStatus(userId, blacklisted, analystName);
        if (!updated) {
            sendJson(exchange, 404, ApiResponse.error("User not found: " + userId));
            return;
        }

        UserProfile profile = fraudService.getUserProfile(userId);
        sendJson(exchange, 200, ApiResponse.ok("User blacklist status updated to: " + blacklisted, UserProfileDTO.toMap(profile)));
    }
}
