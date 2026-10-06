package com.sentinel.supabase;

import com.sentinel.exception.DatabaseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * High-performance, zero-dependency Supabase HTTPS REST Client.
 * Communicates directly with Supabase Data API PostgREST endpoints (https://<PROJECT_REF>.supabase.co/rest/v1/...).
 */
public class SupabaseClient {

    private static SupabaseClient instance;
    private final HttpClient httpClient;

    private SupabaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    /**
     * Builds a normalized, fully-qualified PostgREST REST endpoint URL:
     * Format: https://<PROJECT_REF>.supabase.co/rest/v1/<TABLE_NAME>[?<QUERY>]
     */
    public static String buildUrl(String tableOrPath, String queryParams) {
        String rawBase = SupabaseConfig.getSupabaseUrl();
        if (rawBase == null || rawBase.trim().isEmpty()) {
            return null;
        }

        // 1. Normalize Base URL (strip trailing slashes and any trailing /rest/v1)
        String base = rawBase.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/rest/v1")) {
            base = base.substring(0, base.length() - "/rest/v1".length());
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        // 2. Normalize Table / Resource Name (strip leading slashes and any leading rest/v1/)
        String table = (tableOrPath != null) ? tableOrPath.trim() : "";
        while (table.startsWith("/")) {
            table = table.substring(1);
        }
        if (table.startsWith("rest/v1/")) {
            table = table.substring("rest/v1/".length());
        }
        while (table.startsWith("/")) {
            table = table.substring(1);
        }

        // 3. Assemble root endpoint: https://<project>.supabase.co/rest/v1/<table_name>
        StringBuilder urlBuilder = new StringBuilder(base);
        urlBuilder.append("/rest/v1/").append(table);

        // 4. Append Query Parameters if present
        if (queryParams != null && !queryParams.trim().isEmpty()) {
            String query = queryParams.trim();
            if (query.startsWith("?")) {
                urlBuilder.append(query);
            } else {
                urlBuilder.append("?").append(query);
            }
        }

        return urlBuilder.toString();
    }

    /**
     * Executes a GET request against a Supabase table.
     *
     * @param table       Table name (e.g. "transactions", "user_profiles")
     * @param queryParams PostgREST query parameters (e.g. "select=*&order=tx_timestamp.desc&limit=50")
     * @return JSON response body string
     */
    public String get(String table, String queryParams) throws DatabaseException {
        ensureConfigured();
        String url = buildUrl(table, queryParams);

        HttpRequest request = createRequestBuilder(url)
                .GET()
                .build();

        return sendRequest(request, "GET " + table);
    }

    /**
     * Executes an INSERT request against a Supabase table.
     *
     * @param table       Table name
     * @param jsonPayload JSON object or array string
     * @return JSON response body string
     */
    public String post(String table, String jsonPayload) throws DatabaseException {
        ensureConfigured();
        String url = buildUrl(table, null);

        HttpRequest request = createRequestBuilder(url)
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload != null ? jsonPayload : "{}"))
                .build();

        return sendRequest(request, "POST " + table);
    }

    /**
     * Executes an UPSERT (insert or update on conflict) request against a Supabase table.
     *
     * @param table       Table name
     * @param jsonPayload JSON object or array string
     * @param onConflict  Conflict target column (e.g. "tx_id", "user_id", "config_key")
     * @return JSON response body string
     */
    public String upsert(String table, String jsonPayload, String onConflict) throws DatabaseException {
        ensureConfigured();
        String query = (onConflict != null && !onConflict.trim().isEmpty()) ? "on_conflict=" + onConflict.trim() : null;
        String url = buildUrl(table, query);

        HttpRequest request = createRequestBuilder(url)
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload != null ? jsonPayload : "{}"))
                .build();

        return sendRequest(request, "UPSERT " + table);
    }

    /**
     * Executes a PATCH (update) request against a Supabase table.
     *
     * @param table       Table name
     * @param queryParams Filter parameters (e.g. "tx_id=eq.TX-100")
     * @param jsonPayload Partial JSON object containing fields to update
     * @return JSON response body string
     */
    public String patch(String table, String queryParams, String jsonPayload) throws DatabaseException {
        ensureConfigured();
        String url = buildUrl(table, queryParams);

        HttpRequest request = createRequestBuilder(url)
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPayload != null ? jsonPayload : "{}"))
                .build();

        return sendRequest(request, "PATCH " + table);
    }

    /**
     * Executes a DELETE request against a Supabase table.
     *
     * @param table       Table name
     * @param queryParams Filter parameters (e.g. "tx_id=eq.TX-100")
     * @return JSON response body string
     */
    public String delete(String table, String queryParams) throws DatabaseException {
        ensureConfigured();
        String url = buildUrl(table, queryParams);

        HttpRequest request = createRequestBuilder(url)
                .header("Prefer", "return=representation")
                .DELETE()
                .build();

        return sendRequest(request, "DELETE " + table);
    }

    /**
     * Verifies connectivity to the Supabase API.
     */
    public boolean checkConnection() throws DatabaseException {
        try {
            get("system_configurations", "select=config_key&limit=1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpRequest.Builder createRequestBuilder(String url) {
        String key = SupabaseConfig.getSupabaseKey();
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private String sendRequest(HttpRequest request, String operation) throws DatabaseException {
        // Safe diagnostic logging: method and URL without credentials
        System.out.println("[SUPABASE API] " + request.method() + " " + request.uri());

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                return response.body();
            }

            String body = response.body();
            String errorDetails = (body != null && !body.isEmpty()) ? body : "HTTP " + status;
            throw new DatabaseException("Supabase API request failed [" + operation + "] (Status " + status + "): " + errorDetails);

        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException("Failed to communicate with Supabase API [" + operation + "]: " + e.getMessage(), e);
        }
    }

    private void ensureConfigured() throws DatabaseException {
        if (!SupabaseConfig.isConfigured()) {
            throw new DatabaseException(SupabaseConfig.getMissingConfigMessage());
        }
    }
}
