package com.sentinel.db;

import com.sentinel.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection factory managing JDBC connectivity for Supabase PostgreSQL
 * with fallback support for local SQLite.
 *
 * Configurable via Environment Variables:
 * - SUPABASE_DB_URL or DB_URL or DATABASE_URL
 * - SUPABASE_DB_USER or DB_USER or DATABASE_USER
 * - SUPABASE_DB_PASSWORD or DB_PASSWORD or DATABASE_PASSWORD
 */
public class DatabaseConnectionFactory {

    private static final String DEFAULT_SQLITE_URL = "jdbc:sqlite:sentinel_fraud.db";
    private static DatabaseConnectionFactory instance;

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final boolean isPostgres;

    private DatabaseConnectionFactory(String customUrl, String user, String password) {
        String resolvedUrl = customUrl;
        String resolvedUser = user;
        String resolvedPassword = password;

        if (resolvedUrl == null || resolvedUrl.trim().isEmpty()) {
            // Check environment variables for Supabase / PostgreSQL
            String envUrl = getEnv("SUPABASE_DB_URL", "DB_URL", "DATABASE_URL", "POSTGRES_URL");
            String envUser = getEnv("SUPABASE_DB_USER", "DB_USER", "DATABASE_USER", "POSTGRES_USER");
            String envPass = getEnv("SUPABASE_DB_PASSWORD", "DB_PASSWORD", "DATABASE_PASSWORD", "POSTGRES_PASSWORD");

            if (envUrl != null && !envUrl.trim().isEmpty()) {
                resolvedUrl = formatJdbcUrl(envUrl.trim());
                resolvedUser = envUser;
                resolvedPassword = envPass;
            } else {
                resolvedUrl = DEFAULT_SQLITE_URL;
            }
        } else {
            resolvedUrl = formatJdbcUrl(resolvedUrl.trim());
        }

        this.dbUrl = resolvedUrl;
        this.dbUser = resolvedUser;
        this.dbPassword = resolvedPassword;
        this.isPostgres = this.dbUrl.startsWith("jdbc:postgresql");

        loadDriver();
        initializeDatabase();
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

    private static String getEnv(String... names) {
        for (String name : names) {
            String val = System.getenv(name);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
            String prop = System.getProperty(name);
            if (prop != null && !prop.trim().isEmpty()) {
                return prop.trim();
            }
        }
        return null;
    }

    private static String formatJdbcUrl(String rawUrl) {
        if (rawUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }
        if (rawUrl.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgresql://".length());
        }
        if (!rawUrl.startsWith("jdbc:")) {
            if (rawUrl.contains("postgresql") || rawUrl.contains("supabase") || rawUrl.contains("5432") || rawUrl.contains("6543")) {
                return "jdbc:postgresql://" + rawUrl;
            }
        }
        return rawUrl;
    }

    private void loadDriver() {
        try {
            if (isPostgres) {
                Class.forName("org.postgresql.Driver");
            } else {
                Class.forName("org.sqlite.JDBC");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("WARNING: JDBC driver not found on classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a JDBC Connection to the configured database.
     */
    public Connection getConnection() throws DatabaseException {
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
            throw new DatabaseException("Failed to establish JDBC database connection to: " + dbUrl, e);
        }
    }

    /**
     * Initializes database tables with database-specific DDL.
     */
    public void initializeDatabase() {
        if (isPostgres) {
            initializePostgresDatabase();
        } else {
            initializeSqliteDatabase();
        }
    }

    private void initializePostgresDatabase() {
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
        } catch (Exception e) {
            System.err.println("Supabase PostgreSQL bootstrap notice: " + e.getMessage());
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

    private void initializeSqliteDatabase() {
        String createTransactionsTable =
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "  tx_id TEXT PRIMARY KEY," +
                "  user_id TEXT NOT NULL," +
                "  user_name TEXT NOT NULL," +
                "  card_masked TEXT NOT NULL," +
                "  bank TEXT DEFAULT 'Chase'," +
                "  amount REAL NOT NULL," +
                "  currency TEXT NOT NULL," +
                "  tx_timestamp INTEGER NOT NULL," +
                "  ip_address TEXT NOT NULL," +
                "  location TEXT NOT NULL," +
                "  device_hash TEXT NOT NULL," +
                "  is_vpn_tor INTEGER NOT NULL," +
                "  risk_score INTEGER," +
                "  verdict TEXT," +
                "  dominant_threat TEXT," +
                "  analyst_status TEXT DEFAULT 'PENDING'" +
                ");";

        String createUserProfilesTable =
                "CREATE TABLE IF NOT EXISTS user_profiles (" +
                "  user_id TEXT PRIMARY KEY," +
                "  full_name TEXT NOT NULL," +
                "  primary_location TEXT," +
                "  trusted_device TEXT," +
                "  avg_amount REAL DEFAULT 50.0," +
                "  std_dev REAL DEFAULT 20.0," +
                "  tx_count INTEGER DEFAULT 1," +
                "  chargeback_count INTEGER DEFAULT 0," +
                "  is_blacklisted INTEGER DEFAULT 0," +
                "  last_seen_timestamp INTEGER," +
                "  last_seen_location TEXT" +
                ");";

        String createAuditTable =
                "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "  log_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  tx_id TEXT NOT NULL," +
                "  analyst_name TEXT NOT NULL," +
                "  action_taken TEXT NOT NULL," +
                "  timestamp INTEGER NOT NULL" +
                ");";

        String createConfigTable =
                "CREATE TABLE IF NOT EXISTS system_configurations (" +
                "  config_key TEXT PRIMARY KEY," +
                "  block_threshold INTEGER," +
                "  challenge_threshold INTEGER," +
                "  velocity_limit INTEGER," +
                "  max_amount REAL," +
                "  tor_block INTEGER," +
                "  model_version TEXT" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTransactionsTable);
            stmt.execute(createUserProfilesTable);
            stmt.execute(createAuditTable);
            stmt.execute(createConfigTable);
            seedSqliteInitialProfiles(conn);
        } catch (Exception e) {
            System.err.println("Database bootstrap notice: " + e.getMessage());
        }
    }

    private void seedSqliteInitialProfiles(Connection conn) {
        String seedSQL =
                "INSERT OR IGNORE INTO user_profiles VALUES " +
                "('USR-101', 'Alexandre V.', 'London, UK', 'DEV_MACBOOK_PRO_9', 420.0, 80.0, 14, 0, 0, 1600000000000, 'London, UK'), " +
                "('USR-102', 'Sarah Jenkins', 'Austin, US', 'DEV_IPHONE_15_PRO', 45.0, 15.0, 48, 0, 0, 1600000000000, 'Austin, US'), " +
                "('USR-103', 'Liam O''Connor', 'Dublin, IE', 'DEV_IPAD_AIR_2', 120.0, 40.0, 8, 0, 0, 1600000000000, 'Dublin, IE'), " +
                "('USR-BAD', 'Botnet Cluster Alpha', 'Unknown', 'DEV_SPOOFED_EMULATOR_01', 5000.0, 200.0, 0, 3, 1, 1600000000000, 'Tor Exit Node');";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(seedSQL);
        } catch (SQLException ignored) {
            // Seed already present
        }
    }
}
