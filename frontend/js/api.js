/**
 * SENTINEL AI - Frontend API Service Layer
 * Direct interface for communicating with the Java REST API backend on port 8080.
 * 
 * Endpoints:
 * - GET  /api/health               -> System & database health probe
 * - POST /api/transactions          -> Ingest & evaluate financial transaction
 * - GET  /api/transactions          -> Retrieve recent transactions
 * - GET  /api/transactions/{id}     -> Retrieve single transaction by ID
 * - POST /api/transactions/{id}/status -> Update analyst override status
 * - GET  /api/alerts               -> Active fraud alerts (Blocked/Challenged)
 * - GET  /api/metrics              -> Real-time KPIs & operational metrics
 * - GET  /api/users                -> User profiles catalogue
 * - GET  /api/config               -> System detection thresholds
 * - POST /api/simulation/start     -> Start transaction traffic generator
 * - POST /api/simulation/stop      -> Stop transaction traffic generator
 * - POST /api/simulation/attack    -> Inject adversarial threat vector
 */

const SentinelAPI = (() => {
  // Configurable base URL with port 8080 default
  const API_BASE_URL = window.SENTINEL_API_URL || 'http://localhost:8080/api';

  /**
   * Universal HTTP request helper with unified error handling.
   * @param {string} endpoint - API path (e.g. '/transactions')
   * @param {object} options - Fetch options (method, body, headers)
   * @returns {Promise<object>} Parsed ApiResponse { success, message, data, timestamp }
   */
  async function request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const defaultHeaders = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMsg = (data && data.message) ? data.message : `HTTP Error ${response.status}: ${response.statusText}`;
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`[SentinelAPI Error] ${options.method || 'GET'} ${url}:`, err.message);
      throw err;
    }
  }

  return {
    getBaseUrl: () => API_BASE_URL,

    /**
     * Probe system readiness, Supabase connectivity, and engine status.
     * GET /api/health
     */
    checkHealth: async () => {
      return request('/health', { method: 'GET' });
    },

    /**
     * Ingest and evaluate a financial transaction through the AI detection ensemble.
     * POST /api/transactions
     * 
     * @param {object} transaction
     * @param {number} transaction.amount - Required. Strictly positive float/number.
     * @param {string} [transaction.transactionId] - Optional. Unique transaction ID.
     * @param {string} [transaction.userId] - Optional. Customer identity.
     * @param {string} [transaction.userFullName] - Optional. Customer full name.
     * @param {string} [transaction.cardMasked] - Optional. e.g. "Visa •• 4912".
     * @param {string} [transaction.bank] - Optional. Issuing bank name.
     * @param {string} [transaction.currency] - Optional. Currency code (USD, EUR, GBP).
     * @param {string} [transaction.location] - Optional. Transaction origin geo.
     * @param {string} [transaction.ipAddress] - Optional. Client IP address.
     * @param {string} [transaction.deviceHash] - Optional. Device fingerprint.
     * @param {boolean} [transaction.vpnOrTor] - Optional. High-risk proxy simulation flag.
     * @returns {Promise<object>} ApiResponse containing evaluated TransactionDTO with fraudEvaluation.
     */
    submitTransaction: async (transaction) => {
      if (!transaction || typeof transaction.amount !== 'number' || transaction.amount <= 0) {
        throw new Error("Transaction validation error: 'amount' must be a positive number.");
      }

      return request('/transactions', {
        method: 'POST',
        body: JSON.stringify(transaction)
      });
    },

    /**
     * Retrieve recent transaction history from database.
     * GET /api/transactions?limit=N
     */
    getRecentTransactions: async (limit = 50) => {
      return request(`/transactions?limit=${encodeURIComponent(limit)}`, { method: 'GET' });
    },

    /**
     * Retrieve a single transaction by ID.
     * GET /api/transactions/{id}
     */
    getTransactionById: async (txId) => {
      if (!txId) throw new Error("Transaction ID is required.");
      return request(`/transactions/${encodeURIComponent(txId)}`, { method: 'GET' });
    },

    /**
     * Update analyst review status (e.g. APPROVED_OVERRIDE, CONFIRMED_FRAUD).
     * POST /api/transactions/{id}/status
     */
    updateAnalystStatus: async (txId, status, analystName = 'Security Analyst') => {
      if (!txId) throw new Error("Transaction ID is required.");
      if (!status) throw new Error("Status is required.");
      return request(`/transactions/${encodeURIComponent(txId)}/status`, {
        method: 'POST',
        body: JSON.stringify({ status, analystName })
      });
    },

    /**
     * Retrieve active fraud alerts (Auto-blocked and Challenged transactions).
     * GET /api/alerts?limit=N
     */
    getActiveAlerts: async (limit = 50) => {
      return request(`/alerts?limit=${encodeURIComponent(limit)}`, { method: 'GET' });
    },

    /**
     * Retrieve audit log entries.
     * GET /api/audit-logs?limit=N
     */
    getAuditLogs: async (limit = 50) => {
      return request(`/audit-logs?limit=${encodeURIComponent(limit)}`, { method: 'GET' });
    },

    /**
     * Retrieve real-time metrics, KPI statistics, and active model version.
     * GET /api/metrics
     */
    getMetrics: async () => {
      return request('/metrics', { method: 'GET' });
    },

    /**
     * Retrieve user profile catalogue.
     * GET /api/users
     */
    getUsers: async () => {
      return request('/users', { method: 'GET' });
    },

    /**
     * Retrieve system detection configuration thresholds.
     * GET /api/config
     */
    getConfig: async () => {
      return request('/config', { method: 'GET' });
    },

    /**
     * Update system detection configuration thresholds.
     * POST /api/config
     */
    updateConfig: async (config, analystName = 'Admin') => {
      return request('/config', {
        method: 'POST',
        body: JSON.stringify({ ...config, analystName })
      });
    },

    /**
     * Simulation controls: Start live traffic simulation.
     * POST /api/simulation/start
     */
    startSimulation: async () => {
      return request('/simulation/start', { method: 'POST', body: '{}' });
    },

    /**
     * Simulation controls: Stop live traffic simulation.
     * POST /api/simulation/stop
     */
    stopSimulation: async () => {
      return request('/simulation/stop', { method: 'POST', body: '{}' });
    },

    /**
     * Simulation controls: Inject adversarial fraud attack.
     * POST /api/simulation/attack
     */
    injectAttack: async () => {
      return request('/simulation/attack', { method: 'POST', body: '{}' });
    }
  };
})();

// Export globally for browser scripts
if (typeof window !== 'undefined') {
  window.SentinelAPI = SentinelAPI;
}
