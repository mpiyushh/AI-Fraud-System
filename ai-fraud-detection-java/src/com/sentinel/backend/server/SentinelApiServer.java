package com.sentinel.backend.server;

import com.sentinel.backend.api.*;
import com.sentinel.backend.service.FraudBackendService;
import com.sentinel.service.FraudDetectionEngine;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded REST API Server for Sentinel AI Fraud Detection System.
 * Hosts endpoints on port 8080 (or configurable via PORT environment variable).
 */
public class SentinelApiServer {

    private final int port;
    private final FraudBackendService fraudService;
    private HttpServer server;
    private ExecutorService requestThreadPool;

    public SentinelApiServer(int port, FraudDetectionEngine engine) {
        this.port = port > 0 ? port : resolvePort();
        this.fraudService = new FraudBackendService(engine);
    }

    public SentinelApiServer(int port) {
        this(port, new FraudDetectionEngine());
    }

    public SentinelApiServer() {
        this(resolvePort(), new FraudDetectionEngine());
    }

    public static int resolvePort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }

    public synchronized void start() throws IOException {
        if (server != null) return;

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register REST API Endpoints
        server.createContext("/api/health", new HealthApiController(fraudService));
        server.createContext("/api/transactions", new TransactionApiController(fraudService));
        server.createContext("/api/users", new UserApiController(fraudService));
        server.createContext("/api/alerts", new AlertsApiController(fraudService));
        server.createContext("/api/audit-logs", new AlertsApiController(fraudService));
        server.createContext("/api/metrics", new MetricsApiController(fraudService));
        server.createContext("/api/config", new ConfigApiController(fraudService));
        server.createContext("/api/simulation", new SimulationApiController(fraudService));

        // Multi-threaded HTTP Request Executor
        requestThreadPool = Executors.newFixedThreadPool(16);
        server.setExecutor(requestThreadPool);

        server.start();
        System.out.println("[API SERVER] Sentinel AI REST API Server running on port " + port);
        System.out.println("[API SERVER] Base URL: " + getBaseUrl());
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
        }
        if (requestThreadPool != null) {
            requestThreadPool.shutdownNow();
            requestThreadPool = null;
        }
        System.out.println("[API SERVER] Server stopped cleanly.");
    }

    public int getPort() {
        return server != null ? server.getAddress().getPort() : port;
    }

    public String getBaseUrl() {
        return "http://localhost:" + getPort();
    }

    public FraudBackendService getFraudService() {
        return fraudService;
    }
}
