package com.sentinel.supabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration provider for Supabase REST API integration.
 * Reads SUPABASE_URL and SUPABASE_KEY from JVM properties, OS environment variables,
 * or discovered .env files.
 */
public class SupabaseConfig {

    private static final Map<String, String> dotEnvProperties = new HashMap<>();
    private static String customUrl = null;
    private static String customKey = null;

    private static boolean overrideActive = false;

    static {
        loadDotEnvFile();
    }

    public static synchronized void configure(String url, String key) {
        overrideActive = true;
        customUrl = url;
        customKey = key;
    }

    public static synchronized void reset() {
        overrideActive = false;
        customUrl = null;
        customKey = null;
        dotEnvProperties.clear();
        loadDotEnvFile();
    }

    /**
     * Resolves SUPABASE_URL.
     */
    public static String getSupabaseUrl() {
        if (overrideActive) {
            return (customUrl != null && !customUrl.trim().isEmpty()) ? cleanUrl(customUrl.trim()) : null;
        }
        String url = resolveProperty("SUPABASE_URL", "VITE_SUPABASE_URL", "NEXT_PUBLIC_SUPABASE_URL");
        return url != null ? cleanUrl(url.trim()) : null;
    }

    /**
     * Resolves SUPABASE_KEY (supports SUPABASE_KEY, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY).
     */
    public static String getSupabaseKey() {
        if (overrideActive) {
            return (customKey != null && !customKey.trim().isEmpty()) ? customKey.trim() : null;
        }
        return resolveProperty(
                "SUPABASE_KEY",
                "SUPABASE_ANON_KEY",
                "SUPABASE_SERVICE_ROLE_KEY",
                "VITE_SUPABASE_ANON_KEY",
                "NEXT_PUBLIC_SUPABASE_ANON_KEY"
        );
    }

    public static boolean isConfigured() {
        String url = getSupabaseUrl();
        String key = getSupabaseKey();
        return url != null && !url.trim().isEmpty() && key != null && !key.trim().isEmpty();
    }

    public static String getMissingConfigMessage() {
        return "Supabase configuration is missing. Please configure SUPABASE_URL and SUPABASE_KEY in a .env file or system environment variables.";
    }

    private static String cleanUrl(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/rest/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/rest/v1".length());
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String resolveProperty(String... names) {
        for (String name : names) {
            // 1. JVM property
            String prop = System.getProperty(name);
            if (prop != null && !prop.trim().isEmpty()) {
                return prop.trim();
            }

            // 2. OS environment variable
            String env = System.getenv(name);
            if (env != null && !env.trim().isEmpty()) {
                return env.trim();
            }

            // 3. .env file map
            String fromEnvFile = dotEnvProperties.get(name);
            if (fromEnvFile != null && !fromEnvFile.trim().isEmpty()) {
                return fromEnvFile.trim();
            }
        }
        return null;
    }

    private static void loadDotEnvFile() {
        List<File> candidates = new ArrayList<>();

        // Explicit property
        String explicit = System.getProperty("env.file");
        if (explicit != null && !explicit.trim().isEmpty()) {
            candidates.add(new File(explicit.trim()));
        }

        // Working directory & subdirs
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            candidates.add(new File(userDir, ".env"));
            candidates.add(new File(userDir, "ai-fraud-detection-java/.env"));
            candidates.add(new File(userDir, "../.env"));
        }

        // Code source location
        try {
            URI codeUri = SupabaseConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File codeFile = new File(codeUri);
            File codeDir = codeFile.isDirectory() ? codeFile : codeFile.getParentFile();
            if (codeDir != null) {
                candidates.add(new File(codeDir, ".env"));
                candidates.add(new File(codeDir.getParentFile(), ".env"));
                candidates.add(new File(codeDir.getParentFile(), "ai-fraud-detection-java/.env"));
            }
        } catch (Exception ignored) {}

        // Standard relative paths
        candidates.add(new File(".env"));
        candidates.add(new File("../.env"));
        candidates.add(new File("ai-fraud-detection-java/.env"));

        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isFile()) {
                parseEnvFile(candidate);
                System.out.println("[CONFIG] Successfully loaded Supabase API environment from: " + candidate.getAbsolutePath());
                break;
            }
        }
    }

    private static void parseEnvFile(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
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
        } catch (Exception e) {
            System.err.println("[CONFIG WARNING] Failed to parse .env file: " + e.getMessage());
        }
    }
}
