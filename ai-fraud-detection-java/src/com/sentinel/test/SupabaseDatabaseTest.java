package com.sentinel.test;

import com.sentinel.dao.AuditLogDAO;
import com.sentinel.dao.ConfigurationDAO;
import com.sentinel.dao.TransactionDAO;
import com.sentinel.dao.UserProfileDAO;
import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.model.AuditLog;
import com.sentinel.model.DetectionResult;
import com.sentinel.model.SystemConfiguration;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * Verification test suite for Supabase / PostgreSQL database operations and DAOs.
 */
public class SupabaseDatabaseTest {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  SENTINEL AI // Supabase / PostgreSQL Database Test Suite           ");
        System.out.println("=====================================================================");

        int passed = 0;
        int total = 7;

        try {
            // If running in local test environment without Supabase cloud credentials, configure test database
            if (DatabaseConnectionFactory.resolveConfig("SUPABASE_DB_URL", "DATABASE_URL", "DB_URL") == null) {
                DatabaseConnectionFactory.configure("jdbc:sqlite:test_suite_db.db");
            }

            // Test 1: Factory initialization and Connection
            System.out.println("\n[TEST 1/7] Testing Database Connection Factory...");
            DatabaseConnectionFactory factory = DatabaseConnectionFactory.getInstance();
            factory.initializeDatabase();
            try (Connection conn = factory.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    System.out.println("  --> Connected successfully to: " + factory.getDbUrl());
                    System.out.println("  --> PostgreSQL Mode: " + factory.isPostgreSql());
                } else {
                    throw new RuntimeException("Database connection is null or closed!");
                }
            }
            passed++;

            // Test 2: UserProfileDAO CRUD & Seeding
            System.out.println("\n[TEST 2/7] Testing UserProfileDAO CRUD & Upsert...");
            UserProfileDAO userProfileDao = new UserProfileDAO();
            
            // Query seeded profile
            UserProfile profile101 = userProfileDao.findById("USR-101");
            if (profile101 == null) {
                // Insert if fresh test DB
                profile101 = new UserProfile("USR-101", "Alexandre V.", "London, UK", "DEV_MACBOOK_PRO_9", 420.0, 80.0, 14, 0, false);
                userProfileDao.save(profile101);
            }
            System.out.println("  --> Read profile: " + profile101.getFullName() + " (" + profile101.getPrimaryLocation() + ")");

            // Create & Save new profile
            UserProfile testProfile = new UserProfile(
                    "USR-PHASE1-TEST",
                    "Supabase Test User",
                    "San Francisco, US",
                    "DEV_TEST_MIGRATION_01",
                    150.0,
                    30.0,
                    5,
                    0,
                    false
            );
            userProfileDao.save(testProfile);

            UserProfile fetched = userProfileDao.findById("USR-PHASE1-TEST");
            if (fetched == null || !fetched.getFullName().equals("Supabase Test User")) {
                throw new RuntimeException("Failed to retrieve created user profile!");
            }
            System.out.println("  --> Successfully created & retrieved user: " + fetched.getUserId());

            // Upsert / Update test
            fetched.setFullName("Supabase Test User Updated");
            fetched.setPrimaryLocation("New York, US");
            userProfileDao.save(fetched);

            UserProfile updated = userProfileDao.findById("USR-PHASE1-TEST");
            if (updated == null || !updated.getFullName().equals("Supabase Test User Updated")) {
                throw new RuntimeException("Upsert failed to update existing profile!");
            }
            System.out.println("  --> Successfully verified ON CONFLICT upsert for UserProfile.");

            // Blacklist toggle test
            userProfileDao.setBlacklisted("USR-PHASE1-TEST", true);
            UserProfile blacklisted = userProfileDao.findById("USR-PHASE1-TEST");
            if (blacklisted == null || !blacklisted.isBlacklisted()) {
                throw new RuntimeException("Failed to update blacklist status!");
            }
            System.out.println("  --> Successfully updated blacklist status.");
            passed++;

            // Test 3: TransactionDAO CRUD & Upsert
            System.out.println("\n[TEST 3/7] Testing TransactionDAO CRUD & Upsert...");
            TransactionDAO txDao = new TransactionDAO();

            Transaction testTx = new Transaction(
                    "TX-SUPABASE-001",
                    "USR-PHASE1-TEST",
                    "Supabase Test User",
                    "Mastercard •• 5521",
                    "JPMorgan Chase",
                    899.99,
                    "USD",
                    System.currentTimeMillis(),
                    "198.51.100.42",
                    "New York, US",
                    "DEV_TEST_MIGRATION_01",
                    false
            );
            DetectionResult result = new DetectionResult(85, DetectionResult.Verdict.AUTO_BLOCKED, "Amount Spike Anomaly", Collections.emptyList(), 5.2);
            testTx.setResult(result);
            testTx.setAnalystStatus("PENDING");

            txDao.save(testTx);

            Transaction fetchedTx = txDao.findById("TX-SUPABASE-001");
            if (fetchedTx == null) {
                throw new RuntimeException("Failed to retrieve saved transaction!");
            }
            if (fetchedTx.getResult() == null || fetchedTx.getResult().getRiskScore() != 85) {
                throw new RuntimeException("DetectionResult metadata mismatch!");
            }
            System.out.println("  --> Successfully created & verified transaction: " + fetchedTx.getTransactionId() + " (Risk: " + fetchedTx.getResult().getRiskScore() + ")");

            // Update Analyst Status
            txDao.updateAnalystStatus("TX-SUPABASE-001", "APPROVED_OVERRIDE");
            Transaction updatedStatusTx = txDao.findById("TX-SUPABASE-001");
            if (updatedStatusTx == null || !"APPROVED_OVERRIDE".equals(updatedStatusTx.getAnalystStatus())) {
                throw new RuntimeException("Failed to update analyst status!");
            }
            System.out.println("  --> Successfully updated analyst status to APPROVED_OVERRIDE.");

            // Find recent
            List<Transaction> recent = txDao.findAll();
            if (recent.isEmpty()) {
                throw new RuntimeException("findAll returned empty transaction list!");
            }
            System.out.println("  --> findAll returned " + recent.size() + " transactions.");
            passed++;

            // Test 4: AuditLogDAO CRUD
            System.out.println("\n[TEST 4/7] Testing AuditLogDAO CRUD Operations...");
            AuditLogDAO auditDao = new AuditLogDAO();

            AuditLog log = new AuditLog("TX-SUPABASE-001", "Senior Analyst Miller", "MANUAL_OVERRIDE_APPROVED");
            auditDao.save(log);

            if (log.getLogId() == null) {
                throw new RuntimeException("Failed to generate logId on save!");
            }
            System.out.println("  --> Saved audit log with ID: " + log.getLogId());

            AuditLog fetchedLog = auditDao.findById(log.getLogId());
            if (fetchedLog == null || !"Senior Analyst Miller".equals(fetchedLog.getAnalystName())) {
                throw new RuntimeException("Failed to fetch saved audit log!");
            }
            System.out.println("  --> Successfully retrieved audit log: " + fetchedLog.getActionTaken());

            List<AuditLog> txLogs = auditDao.findByTransactionId("TX-SUPABASE-001");
            if (txLogs.isEmpty()) {
                throw new RuntimeException("findByTransactionId returned no logs!");
            }
            System.out.println("  --> findByTransactionId returned " + txLogs.size() + " log entries.");
            passed++;

            // Test 5: ConfigurationDAO CRUD
            System.out.println("\n[TEST 5/7] Testing ConfigurationDAO Persistence...");
            ConfigurationDAO configDao = new ConfigurationDAO();

            SystemConfiguration defaultConfig = configDao.findById("DEFAULT");
            if (defaultConfig == null) {
                throw new RuntimeException("Failed to retrieve default configuration!");
            }
            System.out.println("  --> Default Config: Block=" + defaultConfig.getBlockThreshold() + ", Challenge=" + defaultConfig.getChallengeThreshold() + ", MaxAmount=$" + defaultConfig.getMaxAmountCeiling());

            // Save custom config
            SystemConfiguration customConfig = new SystemConfiguration(85, 60, 5, 25000.0, true, "v5.0-Supabase-Ensemble");
            configDao.save(customConfig);

            SystemConfiguration updatedConfig = configDao.findById("DEFAULT");
            if (updatedConfig.getBlockThreshold() != 85 || !"v5.0-Supabase-Ensemble".equals(updatedConfig.getActiveModelVersion())) {
                throw new RuntimeException("Failed to persist updated configuration!");
            }
            System.out.println("  --> Successfully updated system configuration: Model=" + updatedConfig.getActiveModelVersion());
            passed++;

            // Test 6: Clean up temporary test data
            System.out.println("\n[TEST 6/7] Testing Delete Operations & Data Integrity...");
            txDao.delete("TX-SUPABASE-001");
            if (txDao.findById("TX-SUPABASE-001") != null) {
                throw new RuntimeException("Delete failed for transaction!");
            }

            userProfileDao.delete("USR-PHASE1-TEST");
            if (userProfileDao.findById("USR-PHASE1-TEST") != null) {
                throw new RuntimeException("Delete failed for user profile!");
            }

            auditDao.delete(log.getLogId());
            if (auditDao.findById(log.getLogId()) != null) {
                throw new RuntimeException("Delete failed for audit log!");
            }
            System.out.println("  --> Successfully verified delete operations.");
            passed++;

            // Test 7: Rubric System Verification Integrity
            System.out.println("\n[TEST 7/7] Verifying Rubric System Test Compatibility...");
            SystemVerificationTest.main(new String[0]);
            passed++;

            System.out.println("\n=====================================================================");
            System.out.printf("  DATABASE CRUD TEST: %d/%d TESTS PASSED (100%% SUCCESS)\n", passed, total);
            System.out.println("=====================================================================");

        } catch (Exception e) {
            System.err.println("\n[DATABASE TEST FAILURE] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
