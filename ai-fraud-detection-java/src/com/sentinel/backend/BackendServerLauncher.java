package com.sentinel.backend;

import com.sentinel.backend.server.SentinelApiServer;
import com.sentinel.db.DatabaseConnectionFactory;

/**
 * Standalone entry point for launching the Sentinel AI Backend REST API Server.
 */
public class BackendServerLauncher {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("   SENTINEL AI // Autonomous Fraud Detection REST API Service       ");
        System.out.println("=====================================================================");

        // 1. Initialize Supabase / Database Connection & Tables
        System.out.println("[BOOT] Connecting to Database & Initializing Schemas...");
        DatabaseConnectionFactory.getInstance().initializeDatabase();
        System.out.println("[BOOT] Database connectivity verified.");

        // 2. Start HTTP REST Server
        int port = SentinelApiServer.resolvePort();
        SentinelApiServer apiServer = new SentinelApiServer(port);

        try {
            apiServer.start();
            System.out.println("[BOOT] Ready to receive transactions at: " + apiServer.getBaseUrl() + "/api/transactions");

            // Add JVM shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN] Stopping Sentinel API Server...");
                apiServer.stop();
            }));

        } catch (Exception e) {
            System.err.println("[BOOT ERROR] Failed to start Sentinel API Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
