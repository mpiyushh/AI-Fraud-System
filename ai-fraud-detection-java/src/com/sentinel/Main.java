package com.sentinel;

import com.sentinel.exception.DatabaseException;
import com.sentinel.service.FraudDetectionEngine;
import com.sentinel.supabase.SupabaseClient;
import com.sentinel.supabase.SupabaseConfig;
import com.sentinel.ui.FraudDetectionDashboard;
import com.sentinel.ui.LoginDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Main application entry point for the Sentinel AI Fraud Detection System.
 * Connects directly to Supabase via HTTPS REST API and launches the application gateway.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("   SENTINEL AI // Autonomous Fraud Defense Matrix v5.0   ");
        System.out.println("==========================================================");

        // 1. Verify Supabase Configuration
        System.out.println("[BOOT] Connecting to Supabase API (HTTPS REST Client)...");
        if (!SupabaseConfig.isConfigured()) {
            String errorMsg = SupabaseConfig.getMissingConfigMessage();
            System.err.println("\n[SUPABASE CONFIGURATION ERROR] " + errorMsg);
            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            errorMsg,
                            "Supabase Configuration Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(1);
                });
            } else {
                System.exit(1);
            }
            return;
        }

        // Test Supabase connectivity
        try {
            boolean connected = SupabaseClient.getInstance().checkConnection();
            if (connected) {
                System.out.println("[BOOT] Supabase API Endpoint Connected: " + SupabaseConfig.getSupabaseUrl());
            } else {
                System.out.println("[BOOT] Supabase API configured: " + SupabaseConfig.getSupabaseUrl());
            }
        } catch (Exception e) {
            System.out.println("[BOOT WARNING] Supabase API probe: " + e.getMessage());
        }

        // 2. Instantiate Multithreaded Fraud Detection Engine
        System.out.println("[BOOT] Starting AI Ensemble & Worker Thread Pool...");
        FraudDetectionEngine engine = new FraudDetectionEngine();
        System.out.println("[BOOT] Neural Ensemble Online: 4 Sub-Detectors Active.");

        // 3. Show Login Dialog, then route to correct dashboard on EDT
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> launchAuthFlow(engine));
        } else {
            System.out.println("[BOOT] Running in headless mode. Engine active.");
        }
    }

    /**
     * Shows the login dialog and, on successful authentication, opens the
     * role-appropriate dashboard. Calling this recursively enables logout/switch.
     */
    public static void launchAuthFlow(FraudDetectionEngine engine) {
        LoginDialog login = new LoginDialog(null);
        login.setVisible(true);

        String role = login.getAuthenticatedRole();
        if (role == null) {
            // User cancelled — exit cleanly
            System.out.println("[AUTH] Login cancelled. Exiting.");
            System.exit(0);
            return;
        }

        String username = login.getLoggedInUsername();
        String card     = login.getSelectedCard();
        String bank     = login.getSelectedBank();

        System.out.println("[AUTH] Authenticated: role=" + role + " user=" + username + " card=" + card);

        FraudDetectionDashboard dashboard = new FraudDetectionDashboard(engine, role, username, card, bank);
        dashboard.setVisible(true);
        System.out.println("[BOOT] Swing GUI Dashboard launched for role: " + role);
    }
}
