package com.sentinel.backend;

import com.sentinel.backend.server.SentinelApiServer;
import com.sentinel.supabase.SupabaseClient;
import com.sentinel.supabase.SupabaseConfig;

/**
 * Standalone entry point for launching the Sentinel AI Backend REST API Server.
 */
public class BackendServerLauncher {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("   SENTINEL AI // Autonomous Fraud Detection REST API Service       ");
        System.out.println("=====================================================================");

        try {
            // 1. Initialize Supabase API Connection
            System.out.println("[BOOT] Connecting to Supabase API (HTTPS REST Client)...");
            if (!SupabaseConfig.isConfigured()) {
                System.err.println("[BOOT ERROR] " + SupabaseConfig.getMissingConfigMessage());
                System.exit(1);
                return;
            }

            try {
                SupabaseClient.getInstance().checkConnection();
                System.out.println("[BOOT] Supabase API Endpoint Connected: " + SupabaseConfig.getSupabaseUrl());
            } catch (Exception e) {
                System.out.println("[BOOT NOTICE] Supabase configured: " + SupabaseConfig.getSupabaseUrl());
            }

            // 2. Start HTTP REST Server
            int port = SentinelApiServer.resolvePort();
            SentinelApiServer apiServer = new SentinelApiServer(port);

            apiServer.start();
            System.out.println("[BOOT] Ready to receive transactions at: " + apiServer.getBaseUrl() + "/api/transactions");

            // Add JVM shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN] Stopping Sentinel API Server...");
                apiServer.stop();
            }));

        } catch (Exception e) {
            System.err.println("[BOOT ERROR] Failed to start Sentinel Backend: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
