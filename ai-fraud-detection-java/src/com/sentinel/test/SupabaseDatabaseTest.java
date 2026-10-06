package com.sentinel.test;

import com.sentinel.dao.AuditLogDAO;
import com.sentinel.dao.ConfigurationDAO;
import com.sentinel.dao.TransactionDAO;
import com.sentinel.dao.UserProfileDAO;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.AuditLog;
import com.sentinel.model.DetectionResult;
import com.sentinel.model.SystemConfiguration;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;
import com.sentinel.supabase.SupabaseClient;
import com.sentinel.supabase.SupabaseConfig;

import java.util.Collections;
import java.util.List;

/**
 * End-to-End Live Verification Test Suite for Supabase REST API & DAOs.
 */
public class SupabaseDatabaseTest {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  SENTINEL AI // Supabase Live REST API & Repository Test Suite      ");
        System.out.println("=====================================================================");

        int passed = 0;
        int total = 7;

        try {
            // 1. Check Configuration Status
            System.out.println("\n[TEST 1/7] Verifying Supabase Configuration...");
            if (!SupabaseConfig.isConfigured()) {
                throw new RuntimeException("Supabase is not configured! Please provide SUPABASE_URL and SUPABASE_KEY.");
            }
            System.out.println("  --> Supabase Project URL: " + SupabaseConfig.getSupabaseUrl());
            passed++;

            // 2. Test GET /rest/v1/user_profiles
            System.out.println("\n[TEST 2/7] Testing GET /rest/v1/user_profiles (Live Query)...");
            UserProfileDAO userProfileDao = new UserProfileDAO();
            List<UserProfile> profiles = userProfileDao.findAll();
            System.out.println("  --> Successfully queried user_profiles table. Found: " + profiles.size() + " profile(s).");
            for (UserProfile p : profiles) {
                System.out.println("      * " + p.getUserId() + " - " + p.getFullName() + " (" + p.getPrimaryLocation() + ")");
            }
            passed++;

            // 3. Test Upsert UserProfile (POST /rest/v1/user_profiles?on_conflict=user_id)
            System.out.println("\n[TEST 3/7] Testing Upsert UserProfile (USR-101 Alexandre V.)...");
            UserProfile testProfile = new UserProfile(
                    "USR-101",
                    "Alexandre V.",
                    "London, UK",
                    "DEV_MACBOOK_PRO_9",
                    420.0,
                    80.0,
                    14,
                    0,
                    false
            );
            userProfileDao.save(testProfile);
            UserProfile fetchedProfile = userProfileDao.findById("USR-101");
            if (fetchedProfile == null || !"USR-101".equals(fetchedProfile.getUserId())) {
                throw new RuntimeException("Failed to upsert and retrieve USR-101!");
            }
            System.out.println("  --> Successfully persisted and retrieved user profile: " + fetchedProfile.getFullName());
            passed++;

            // 4. Test Transaction Submission & AI Scoring Persistence (POST /rest/v1/transactions)
            System.out.println("\n[TEST 4/7] Testing Ingestion & Persistence of Transaction (POST /rest/v1/transactions)...");
            TransactionDAO transactionDao = new TransactionDAO();
            String testTxId = "TX-LIVE-TEST-" + (System.currentTimeMillis() % 10000);
            Transaction tx = new Transaction(
                    testTxId,
                    "USR-101",
                    "Alexandre V.",
                    "Visa •• 8492",
                    "Chase",
                    120.0,
                    "USD",
                    System.currentTimeMillis(),
                    "192.168.1.50",
                    "London, UK",
                    "DEV_MACBOOK_PRO_9",
                    false
            );
            DetectionResult result = new DetectionResult(
                    5,
                    DetectionResult.Verdict.APPROVED,
                    "Clean Behavioral Signature",
                    Collections.emptyList(),
                    8.5
            );
            tx.setResult(result);
            tx.setAnalystStatus("AUTO_APPROVED");

            transactionDao.save(tx);
            System.out.println("  --> Successfully persisted transaction [" + testTxId + "] to Supabase.");
            passed++;

            // 5. Test Transaction Retrieval by ID and History (GET /rest/v1/transactions)
            System.out.println("\n[TEST 5/7] Testing Retrieval of Transaction by ID and Recent History...");
            Transaction fetchedTx = transactionDao.findById(testTxId);
            if (fetchedTx == null) {
                throw new RuntimeException("Could not find persisted transaction [" + testTxId + "] in Supabase!");
            }
            System.out.println("  --> Retrieved: " + fetchedTx.getTransactionId() + " | Amount: $" + fetchedTx.getAmount() + " | Verdict: " + (fetchedTx.getResult() != null ? fetchedTx.getResult().getVerdict() : "N/A"));

            List<Transaction> recentTxs = transactionDao.getRecentTransactions(10);
            System.out.println("  --> Retrieved " + recentTxs.size() + " recent transactions from Supabase.");
            passed++;

            // 6. Test System Configuration Persistence (POST /rest/v1/system_configurations)
            System.out.println("\n[TEST 6/7] Testing System Configuration Upsert (POST /rest/v1/system_configurations)...");
            ConfigurationDAO configDao = new ConfigurationDAO();
            SystemConfiguration cfg = new SystemConfiguration(80, 55, 3, 10000.0, true, "v5.0-Supabase-Ensemble");
            configDao.save(cfg);
            SystemConfiguration loadedCfg = configDao.findById("DEFAULT");
            System.out.println("  --> Active Config: Block Threshold=" + loadedCfg.getBlockThreshold() + " | Model=" + loadedCfg.getActiveModelVersion());
            passed++;

            // 7. Test Audit Log Recording (POST /rest/v1/audit_logs)
            System.out.println("\n[TEST 7/7] Testing Audit Log Recording (POST /rest/v1/audit_logs)...");
            AuditLogDAO auditDao = new AuditLogDAO();
            AuditLog auditLog = new AuditLog(testTxId, "Lead Security Analyst", "APPROVED_VERIFIED", System.currentTimeMillis());
            auditDao.save(auditLog);
            System.out.println("  --> Audit log recorded successfully for tx: " + testTxId);
            passed++;

            System.out.println("\n=====================================================================");
            System.out.println("  ALL " + passed + "/" + total + " LIVE SUPABASE REST API OPERATIONS SUCCEEDED!");
            System.out.println("=====================================================================");

        } catch (Exception e) {
            System.err.println("\n[TEST FAILURE] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
