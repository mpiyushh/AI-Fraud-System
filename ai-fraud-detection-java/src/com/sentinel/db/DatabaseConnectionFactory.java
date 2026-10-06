package com.sentinel.db;

import com.sentinel.exception.DatabaseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton database connection factory managing JDBC connectivity for Supabase PostgreSQL.
 * Strictly enforces Supabase PostgreSQL runtime configuration via .env file or environment variables.
 *
 * Configurable via:
 * - SUPABASE_DB_URL / DATABASE_URL / DB_URL / POSTGRES_URL
 * - SUPABASE_DB_USER / DATABASE_USER / DB_USER / POSTGRES_USER
 * - SUPABASE_DB_PASSWORD / DATABASE_PASSWORD / DB_PASSWORD / POSTGRES_PASSWORD
 */
public class DatabaseConnectionFactory {

    private static DatabaseConnectionFactory instance;
    private static final Map<String, String> dotEnvProperties = new HashMap<>();

    static {
        loadDotEnvFile();
    }

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final boolean isPostgres;

    private DatabaseConnectionFactory(String customUrl, String user, String password) {
        String resolvedUrl = customUrl;
        String resolvedUser = user;
        String resolvedPassword = password;

        if (resolvedUrl == null || resolvedUrl.trim().isEmpty()) {
            // Check environment variables and .env file for Supabase / PostgreSQL
            String envUrl = resolveConfig("SUPABASE_DB_URL", "DATABASE_URL", "DB_URL", "POSTGRES_URL");
            String envUser = resolveConfig("SUPABASE_DB_USER", "DATABASE_USER", "DB_USER", "POSTGRES_USER");
            String envPass = resolveConfig("SUPABASE_DB_PASSWORD", "DATABASE_PASSWORD", "DB_PASSWORD", "POSTGRES_PASSWORD");

            if (envUrl != null && !envUrl.trim().isEmpty()) {
                resolvedUrl = formatJdbcUrl(envUrl.trim());
                resolvedUser = envUser;
                resolvedPassword = envPass;
            }
        } else {
            resolvedUrl = formatJdbcUrl(resolvedUrl.trim());
        }

        this.dbUrl = resolvedUrl;
        this.dbUser = resolvedUser;
        this.dbPassword = resolvedPassword;
        this.isPostgres = this.dbUrl != null && this.dbUrl.startsWith("jdbc:postgresql");
    }

    public static synchronized DatabaseConnectionFactory getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionFactory(null, null, null);
        }
        return instance;
    }

    public static synchronized void configure(String customUrl) {
        instance = new DatabaseConnectionFactory(customUrl, null, null);
    }

    public static synchronized void configure(String customUrl, String user, String password) {
        instance = new DatabaseConnectionFactory(customUrl, user, password);
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public boolean isPostgreSql() {
        return isPostgres;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    /**
     * Resolves configuration property in order: System Property -> OS Env Var -> .env File.
     */
    public static String resolveConfig(String... names) {
        for (String name : names) {
            // 1. JVM System Property (-DNAME=val)
            String prop = System.getProperty(name);
            if (prop != null && !prop.trim().isEmpty()) {
                return prop.trim();
            }

            // 2. OS Environment Variable
            String env = System.getenv(name);
            if (env != null && !env.trim().isEmpty()) {
                return env.trim();
            }

            // 3. .env file property
            String fromDotEnv = dotEnvProperties.get(name);
            if (fromDotEnv != null && !fromDotEnv.trim().isEmpty()) {
                return fromDotEnv.trim();
            }
        }
        return null;
    }

    /**
     * Reloads .env configuration file if updated.
     */
    public static synchronized void reloadEnvironment() {
        dotEnvProperties.clear();
        loadDotEnvFile();
        resetInstance();
    }

    /**
     * Scans for .env file across standard project root and runtime locations.
     */
    private static void loadDotEnvFile() {
        String[] candidatePaths = {
            ".env",
            "../.env",
            "ai-fraud-detection-java/.env",
            System.getProperty("user.dir") + File.separator + ".env",
            System.getProperty("user.dir") + File.separator + ".." + File.separator + ".env"
        };

        for (String path : candidatePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                parseEnvFile(file);
                System.out.println("[CONFIG] Loaded Supabase credentials from: " + file.getAbsolutePath());
                break;
            }
        }
    }

    private static void parseEnvFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx > 0) {
                    String key = trimmed.substring(0, eqIdx).trim();
                    String val = trimmed.substring(eqIdx + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        if (val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                    }
                    dotEnvProperties.put(key, val);
                }
            }
        } catch (IOException e) {
            System.err.println("[CONFIG WARNING] Failed to read .env file at " + file.getPath() + ": " + e.getMessage());
        }
    }

    public static String formatJdbcUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("postgres://")) {
            return "jdbc:postgresql://" + trimmed.substring("postgres://".length());
        }
        if (trimmed.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + trimmed.substring("postgresql://".length());
        }
        if (!trimmed.startsWith("jdbc:")) {
            if (trimmed.contains("postgresql") || trimmed.contains("supabase") || trimmed.contains("5432") || trimmed.contains("6543")) {
                return "jdbc:postgresql://" + trimmed;
            }
        }
        return trimmed;
    }

    private void loadDriver() throws DatabaseException {
        try {
            if (isPostgres || (dbUrl != null && dbUrl.contains("postgresql"))) {
                Class.forName("org.postgresql.Driver");
            } else if (dbUrl != null && dbUrl.startsWith("jdbc:sqlite")) {
                Class.forName("org.sqlite.JDBC");
            } else {
                Class.forName("org.postgresql.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("PostgreSQL JDBC driver (org.postgresql.Driver) not found on classpath. " +
                    "Ensure lib/postgresql-42.7.3.jar is included in runtime classpath.", e);
        }
    }

    /**
     * Obtains a JDBC Connection to Supabase PostgreSQL.
     */
    public Connection getConnection() throws DatabaseException {
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            throw new DatabaseException(
                "Supabase database configuration is missing. " +
                "Please configure the required environment variables (SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD) " +
                "in a .env file or system environment variables."
            );
        }

        loadDriver();

        try {
            Connection conn;
            if (dbUser != null && !dbUser.isEmpty()) {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword != null ? dbPassword : "");
            } else {
                conn = DriverManager.getConnection(dbUrl);
            }
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish JDBC database connection to Supabase PostgreSQL: " + dbUrl, e);
        }
    }

    /**
     * Initializes Supabase database tables with PostgreSQL DDL.
     */
    public void initializeDatabase() throws DatabaseException {
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            throw new DatabaseException(
                "Supabase database configuration is missing. " +
                "Please configure the required environment variables (SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD) " +
                "in a .env file or system environment variables."
            );
        }

        if (isPostgres) {
            initializePostgresDatabase();
        }
    }

    private void initializePostgresDatabase() throws DatabaseException {
        String createTransactionsTable =
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "  tx_id VARCHAR(64) PRIMARY KEY," +
                "  user_id VARCHAR(64) NOT NULL," +
                "  user_name VARCHAR(255) NOT NULL," +
                "  card_masked VARCHAR(64) NOT NULL," +
                "  bank VARCHAR(64) DEFAULT 'Chase'," +
                "  amount DOUBLE PRECISION NOT NULL," +
                "  currency VARCHAR(10) NOT NULL DEFAULT 'USD'," +
                "  tx_timestamp BIGINT NOT NULL," +
                "  ip_address VARCHAR(45) NOT NULL," +
                "  location VARCHAR(255) NOT NULL," +
                "  device_hash VARCHAR(255) NOT NULL," +
                "  is_vpn_tor BOOLEAN NOT NULL DEFAULT FALSE," +
                "  risk_score INTEGER," +
                "  verdict VARCHAR(64)," +
                "  dominant_threat VARCHAR(255)," +
                "  analyst_status VARCHAR(64) DEFAULT 'PENDING'," +
                "  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createUserProfilesTable =
                "CREATE TABLE IF NOT EXISTS user_profiles (" +
                "  user_id VARCHAR(64) PRIMARY KEY," +
                "  full_name VARCHAR(255) NOT NULL," +
                "  primary_location VARCHAR(255)," +
                "  trusted_device VARCHAR(255)," +
                "  avg_amount DOUBLE PRECISION DEFAULT 50.0," +
                "  std_dev DOUBLE PRECISION DEFAULT 20.0," +
                "  tx_count INTEGER DEFAULT 1," +
                "  chargeback_count INTEGER DEFAULT 0," +
                "  is_blacklisted BOOLEAN DEFAULT FALSE," +
                "  last_seen_timestamp BIGINT," +
                "  last_seen_location VARCHAR(255)," +
                "  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createAuditTable =
                "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "  log_id BIGSERIAL PRIMARY KEY," +
                "  tx_id VARCHAR(64) NOT NULL," +
                "  analyst_name VARCHAR(255) NOT NULL," +
                "  action_taken VARCHAR(255) NOT NULL," +
                "  timestamp BIGINT NOT NULL," +
                "  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createConfigTable =
                "CREATE TABLE IF NOT EXISTS system_configurations (" +
                "  config_key VARCHAR(64) PRIMARY KEY DEFAULT 'DEFAULT'," +
                "  block_threshold INTEGER DEFAULT 80," +
                "  challenge_threshold INTEGER DEFAULT 55," +
                "  velocity_limit INTEGER DEFAULT 3," +
                "  max_amount DOUBLE PRECISION DEFAULT 10000.0," +
                "  tor_block BOOLEAN DEFAULT TRUE," +
                "  model_version VARCHAR(128) DEFAULT 'v4.8-Neural-Ensemble'," +
                "  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTransactionsTable);
            stmt.execute(createUserProfilesTable);
            stmt.execute(createAuditTable);
            stmt.execute(createConfigTable);
            seedPostgresInitialProfiles(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize Supabase PostgreSQL database tables: " + e.getMessage(), e);
        }
    }

    private void seedPostgresInitialProfiles(Connection conn) {
        String seedSQL =
                "INSERT INTO user_profiles (user_id, full_name, primary_location, trusted_device, avg_amount, std_dev, tx_count, chargeback_count, is_blacklisted, last_seen_timestamp, last_seen_location) " +
                "VALUES " +
                "('USR-101', 'Alexandre V.', 'London, UK', 'DEV_MACBOOK_PRO_9', 420.0, 80.0, 14, 0, FALSE, 1600000000000, 'London, UK'), " +
                "('USR-102', 'Sarah Jenkins', 'Austin, US', 'DEV_IPHONE_15_PRO', 45.0, 15.0, 48, 0, FALSE, 1600000000000, 'Austin, US'), " +
                "('USR-103', 'Liam O''Connor', 'Dublin, IE', 'DEV_IPAD_AIR_2', 120.0, 40.0, 8, 0, FALSE, 1600000000000, 'Dublin, IE'), " +
                "('USR-BAD', 'Botnet Cluster Alpha', 'Unknown', 'DEV_SPOOFED_EMULATOR_01', 5000.0, 200.0, 0, 3, TRUE, 1600000000000, 'Tor Exit Node') " +
                "ON CONFLICT (user_id) DO NOTHING;";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(seedSQL);
        } catch (SQLException ignored) {
            // Seed already present
        }
    }
}
