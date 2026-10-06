package com.sentinel.test;

import com.sentinel.exception.DatabaseException;
import com.sentinel.supabase.SupabaseClient;
import com.sentinel.supabase.SupabaseConfig;

/**
 * Validates Supabase API configuration loading, URL resolution,
 * URL normalization preventing PGRST125, and error handling.
 */
public class DatabaseConfigurationTest {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  SENTINEL AI // Supabase API Configuration & URL Normalization Test ");
        System.out.println("=====================================================================");

        int passed = 0;

        // 1. Verify Missing Configuration Error Handling
        System.out.println("\n[CHECK 1/5] Testing strict missing configuration error handling...");
        SupabaseConfig.configure(null, null);
        try {
            if (!SupabaseConfig.isConfigured()) {
                SupabaseClient.getInstance().get("transactions", "select=*");
                throw new RuntimeException("Expected DatabaseException when Supabase configuration is missing!");
            }
        } catch (DatabaseException e) {
            String msg = e.getMessage();
            System.out.println("  --> Caught expected configuration error: " + msg);
            if (!msg.contains("Supabase configuration is missing") ||
                !msg.contains("SUPABASE_URL") ||
                !msg.contains("SUPABASE_KEY")) {
                throw new RuntimeException("Error message did not contain required guidance text: " + msg);
            }
            passed++;
        }

        // 2. Test URL Normalization & PGRST125 Prevention
        System.out.println("\n[CHECK 2/5] Testing Supabase REST URL normalization for all input variations...");
        String expectedUrl = "https://xyzproject.supabase.co/rest/v1/user_profiles";

        // Case A: Standard root URL
        SupabaseConfig.configure("https://xyzproject.supabase.co", "sample_key");
        String urlA = SupabaseClient.buildUrl("user_profiles", null);
        if (!expectedUrl.equals(urlA)) {
            throw new RuntimeException("Case A failed! Expected: " + expectedUrl + " but got: " + urlA);
        }
        System.out.println("  --> Case A (Standard Root): " + urlA);

        // Case B: URL with trailing slash
        SupabaseConfig.configure("https://xyzproject.supabase.co/", "sample_key");
        String urlB = SupabaseClient.buildUrl("/user_profiles", null);
        if (!expectedUrl.equals(urlB)) {
            throw new RuntimeException("Case B failed! Expected: " + expectedUrl + " but got: " + urlB);
        }
        System.out.println("  --> Case B (Trailing Slash & Leading Slash): " + urlB);

        // Case C: URL ending with /rest/v1
        SupabaseConfig.configure("https://xyzproject.supabase.co/rest/v1", "sample_key");
        String urlC = SupabaseClient.buildUrl("user_profiles", null);
        if (!expectedUrl.equals(urlC)) {
            throw new RuntimeException("Case C failed! Expected: " + expectedUrl + " but got: " + urlC);
        }
        System.out.println("  --> Case C (Base with /rest/v1 suffix): " + urlC);

        // Case D: URL ending with /rest/v1/ and table passed with rest/v1/ prefix
        SupabaseConfig.configure("https://xyzproject.supabase.co/rest/v1/", "sample_key");
        String urlD = SupabaseClient.buildUrl("rest/v1/user_profiles", null);
        if (!expectedUrl.equals(urlD)) {
            throw new RuntimeException("Case D failed! Expected: " + expectedUrl + " but got: " + urlD);
        }
        System.out.println("  --> Case D (Both Base and Table with /rest/v1): " + urlD);

        // Case E: With Query Parameters
        String urlE = SupabaseClient.buildUrl("user_profiles", "user_id=eq.USR-101&select=*");
        String expectedWithQuery = "https://xyzproject.supabase.co/rest/v1/user_profiles?user_id=eq.USR-101&select=*";
        if (!expectedWithQuery.equals(urlE)) {
            throw new RuntimeException("Case E failed! Expected: " + expectedWithQuery + " but got: " + urlE);
        }
        System.out.println("  --> Case E (With Query String): " + urlE);
        passed++;

        // 3. Test Client Request Construction
        System.out.println("\n[CHECK 3/5] Testing SupabaseClient singleton instance...");
        SupabaseClient client = SupabaseClient.getInstance();
        if (client == null) {
            throw new RuntimeException("SupabaseClient instance is null!");
        }
        System.out.println("  --> SupabaseClient active and ready for PostgREST calls.");
        passed++;

        // 4. Test Error Cleanliness (No Secret Leak)
        System.out.println("\n[CHECK 4/5] Testing secret protection in error messages...");
        try {
            client.get("invalid_endpoint_probe", "");
        } catch (DatabaseException e) {
            if (e.getMessage().contains("sample_key")) {
                throw new RuntimeException("Error message leaked sensitive API key!");
            }
            System.out.println("  --> Verified API key is protected and not leaked in exception.");
        }
        passed++;

        // 5. Test Upsert Conflict Query Construction
        System.out.println("\n[CHECK 5/5] Testing PostgREST Upsert URL on conflict parameter...");
        String upsertUrl = SupabaseClient.buildUrl("transactions", "on_conflict=tx_id");
        if (!"https://xyzproject.supabase.co/rest/v1/transactions?on_conflict=tx_id".equals(upsertUrl)) {
            throw new RuntimeException("Upsert URL failed! Got: " + upsertUrl);
        }
        System.out.println("  --> Upsert URL verified: " + upsertUrl);
        passed++;

        // Reset to clean state
        SupabaseConfig.reset();

        System.out.println("\n=====================================================================");
        System.out.println("  ALL " + passed + "/5 SUPABASE CONFIGURATION CHECKS PASSED SUCCESSFULLY!    ");
        System.out.println("=====================================================================");
    }
}
