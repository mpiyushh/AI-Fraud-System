package com.sentinel.backend.api;

import com.sentinel.backend.dto.ApiResponse;
import com.sentinel.backend.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base HTTP Handler providing JSON formatting, CORS headers,
 * request parsing, and error handling for REST controllers.
 */
public abstract class BaseApiHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            processRequest(exchange);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, ApiResponse.error("Bad Request: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, ApiResponse.error("Internal Server Error: " + e.getMessage()));
        }
    }

    protected abstract void processRequest(HttpExchange exchange) throws Exception;

    protected void sendJson(HttpExchange exchange, int statusCode, ApiResponse response) throws IOException {
        byte[] bytes = response.toJson().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    protected Map<String, Object> parseJsonBody(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        if (body.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return JsonUtils.parseObject(body);
    }

    protected Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isEmpty()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                params.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else if (idx > 0) {
                params.put(pair.substring(0, idx), "");
            }
        }
        return params;
    }
}
