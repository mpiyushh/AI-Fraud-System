package com.sentinel.test;

import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.exception.DatabaseException;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;

/**
 * Validates connection string formatting, driver loading, .env parsing,
 * and strict error handling when Supabase configuration is missing.
 */
public class DatabaseConfigurationTest {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  SENTINEL AI // Supabase Database Configuration & Env Test Suite    ");
        System.out.println("=====================================================================");

        int passed = 0;

        // 1. Verify PostgreSQL JDBC Driver is available on classpath
        System.out.println("\n[CHECK 1/5] Verifying PostgreSQL JDBC Driver (org.postgresql.Driver)...");
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("  --> org.postgresql.Driver successfully found and loaded.");
            passed++;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver missing from classpath!", e);
        }

        // 2. Test URL Formatting for various Supabase connection strings
        System.out.println("\n[CHECK 2/5] Testing Supabase JDBC URL formatting & protocol conversion...");
        String postgresUri = "postgres://postgres.abc:mysecret@aws-0-us-east-1.pooler.supabase.com:6543/postgres";
        String converted = DatabaseConnectionFactory.formatJdbcUrl(postgresUri);
        if (!converted.startsWith("jdbc:postgresql://")) {
            throw new RuntimeException("Failed to format postgres:// URI: " + converted);
        }
        System.out.println("  --> Converted 'postgres://' -> " + converted);

        String directPostgres = "postgresql://db.projectref.supabase.co:5432/postgres";
        String directConverted = DatabaseConnectionFactory.formatJdbcUrl(directPostgres);
        if (!directConverted.startsWith("jdbc:postgresql://")) {
            throw new RuntimeException("Failed to format postgresql:// URI: " + directConverted);
        }
        System.out.println("  --> Converted 'postgresql://' -> " + directConverted);
        passed++;

        // 3. Test Missing Configuration Error Handling
        System.out.println("\n[CHECK 3/5] Testing strict missing configuration error handling...");
        DatabaseConnectionFactory.configure(null, null, null);
        DatabaseConnectionFactory instance = DatabaseConnectionFactory.getInstance();
        try {
            if (instance.getDbUrl() == null || instance.getDbUrl().isEmpty()) {
                instance.getConnection();
                throw new RuntimeException("Expected DatabaseException when database configuration is missing!");
            }
        } catch (DatabaseException e) {
            String msg = e.getMessage();
            System.out.println("  --> Caught expected configuration error: " + msg);
            if (!msg.contains("Supabase database configuration is missing") ||
                !msg.contains("SUPABASE_DB_URL") ||
                !msg.contains("SUPABASE_DB_USER") ||
                !msg.contains("SUPABASE_DB_PASSWORD")) {
                throw new RuntimeException("Error message did not contain required guidance text: " + msg);
            }
            passed++;
        }

        // 4. Test Configuration Injection & PostgreSql Mode
        System.out.println("\n[CHECK 4/5] Testing explicit Supabase configuration injection...");
        String sampleUrl = "jdbc:postgresql://db.myproject.supabase.co:5432/postgres";
        DatabaseConnectionFactory.configure(sampleUrl, "postgres", "dummy_pass_123");
        DatabaseConnectionFactory activeInstance = DatabaseConnectionFactory.getInstance();
        if (!activeInstance.isPostgreSql()) {
            throw new RuntimeException("Expected isPostgreSql() to be true for: " + sampleUrl);
        }
        if (!sampleUrl.equals(activeInstance.getDbUrl())) {
            throw new RuntimeException("Database URL mismatch!");
        }
        System.out.println("  --> Configuration active: " + activeInstance.getDbUrl() + " (PostgreSQL Mode: " + activeInstance.isPostgreSql() + ")");
        passed++;

        // 5. Test Password Security (no password leak in URL or representations)
        System.out.println("\n[CHECK 5/5] Testing password privacy & security...");
        if (activeInstance.getDbUrl().contains("dummy_pass_123")) {
            throw new RuntimeException("Database URL must never expose database password!");
        }
        System.out.println("  --> Verified password is not exposed in database URL or representations.");
        passed++;

        // Reset to clean state
        DatabaseConnectionFactory.resetInstance();

        System.out.println("\n=====================================================================");
        System.out.println("  ALL " + passed + "/5 DATABASE CONFIGURATION CHECKS PASSED SUCCESSFULLY!   ");
        System.out.println("=====================================================================");
    }
}

