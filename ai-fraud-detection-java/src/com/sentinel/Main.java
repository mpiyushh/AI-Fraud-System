package com.sentinel;

import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.service.FraudDetectionEngine;
import com.sentinel.ui.FraudDetectionDashboard;
import com.sentinel.ui.LoginDialog;

import javax.swing.SwingUtilities;

/**
 * Main application entry point for the Sentinel AI Fraud Detection System.
 * Launches the Login Gateway before the dashboard for role-based access control.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("   SENTINEL AI // Autonomous Fraud Defense Matrix v4.9   ");
        System.out.println("==========================================================");

        // 1. Initialize SQLite Database & Bootstrap Tables via JDBC
        System.out.println("[BOOT] Initializing JDBC Database Connection & Schemas...");
        DatabaseConnectionFactory.getInstance().initializeDatabase();
        System.out.println("[BOOT] Database Tables (transactions, user_profiles, audit_logs) Ready.");

        // 2. Instantiate Multithreaded Fraud Detection Engine
        System.out.println("[BOOT] Starting AI Ensemble & Worker Thread Pool...");
        FraudDetectionEngine engine = new FraudDetectionEngine();
        System.out.println("[BOOT] Neural Ensemble Online: 4 Sub-Detectors Active.");

        // 3. Show Login Dialog, then route to correct dashboard on EDT
        SwingUtilities.invokeLater(() -> launchAuthFlow(engine));
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
