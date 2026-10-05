package com.sentinel.test;

import com.sentinel.db.DatabaseConnectionFactory;

/**
 * Validates connection string formatting and driver loading for Supabase / PostgreSQL.
 */
public class DatabaseConfigurationTest {

    public static void main(String[] args) {
        System.out.println("[TEST] Verifying PostgreSQL / Supabase Driver and Configuration logic...");

        // 1. Verify PostgreSQL JDBC Driver is available on classpath
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("  --> org.postgresql.Driver successfully found and loaded.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver missing from classpath!", e);
        }

        // 2. Test PostgreSQL JDBC URL detection and configuration
        String supabaseSampleUrl = "jdbc:postgresql://db.projectref.supabase.co:5432/postgres";
        DatabaseConnectionFactory.configure(supabaseSampleUrl, "postgres", "secret_pass_test");
        DatabaseConnectionFactory instance = DatabaseConnectionFactory.getInstance();

        if (!instance.isPostgreSql()) {
            throw new RuntimeException("Expected isPostgreSql() to be true for: " + supabaseSampleUrl);
        }
        if (!supabaseSampleUrl.equals(instance.getDbUrl())) {
            throw new RuntimeException("Database URL mismatch!");
        }

        System.out.println("  --> Supabase PostgreSQL configuration verified: " + instance.getDbUrl());

        // Reset to default for local safety
        DatabaseConnectionFactory.resetInstance();
        System.out.println("  --> DatabaseConfigurationTest: ALL PASSED.");
    }
}
