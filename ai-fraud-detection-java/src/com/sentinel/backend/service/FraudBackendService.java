package com.sentinel.backend.service;

import com.sentinel.dao.AuditLogDAO;
import com.sentinel.dao.ConfigurationDAO;
import com.sentinel.dao.TransactionDAO;
import com.sentinel.dao.UserProfileDAO;
import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.AuditLog;
import com.sentinel.model.DetectionResult;
import com.sentinel.model.SystemConfiguration;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;
import com.sentinel.service.FraudDetectionEngine;
import com.sentinel.service.MetricsAggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Backend Service Layer connecting REST API Controllers to
 * the FraudDetectionEngine and Supabase DAOs.
 */
public class FraudBackendService {

    private final FraudDetectionEngine engine;
    private final TransactionDAO transactionDAO;
    private final UserProfileDAO userProfileDAO;
    private final ConfigurationDAO configurationDAO;
    private final AuditLogDAO auditLogDAO;
    private final Random random = new Random();

    public FraudBackendService(FraudDetectionEngine engine) {
        this.engine = engine != null ? engine : new FraudDetectionEngine();
        this.transactionDAO = this.engine.getTransactionDAO();
        this.userProfileDAO = this.engine.getUserProfileDAO();
        this.configurationDAO = this.engine.getConfigurationDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    public FraudDetectionEngine getEngine() {
        return engine;
    }

    /**
     * Ingests, validates, and runs AI fraud evaluation on a transaction request.
     * Persists transaction and evaluated verdict to Supabase via TransactionDAO.
     */
    public Transaction submitAndEvaluateTransaction(Map<String, Object> req) throws Exception {
        if (req == null || req.isEmpty()) {
            throw new IllegalArgumentException("Request payload cannot be empty");
        }

        // Validate Amount
        Object amountObj = req.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Field 'amount' is required");
        }
        double amount;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else {
            amount = Double.parseDouble(String.valueOf(amountObj));
        }
        if (amount <= 0) {
            throw new InvalidTransactionException(null, "Amount must be strictly positive: " + amount);
        }

        // Extract or default fields
        String txId = (String) req.get("transactionId");
        if (txId == null || txId.trim().isEmpty()) {
            txId = (String) req.get("txId");
        }
        if (txId == null || txId.trim().isEmpty()) {
            txId = "TX-" + System.currentTimeMillis() + "-" + (100 + random.nextInt(900));
        }

        String userId = (String) req.get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            userId = "USR-ANON";
        }

        String userFullName = (String) req.get("userFullName");
        if (userFullName == null || userFullName.trim().isEmpty()) {
            userFullName = (String) req.get("userName");
        }
        if (userFullName == null || userFullName.trim().isEmpty()) {
            // Attempt to resolve name from UserProfile
            UserProfile profile = userProfileDAO.findById(userId);
            userFullName = (profile != null) ? profile.getFullName() : "Unknown Customer";
        }

        String cardMasked = (String) req.get("cardMasked");
        if (cardMasked == null || cardMasked.trim().isEmpty()) {
            cardMasked = "Visa •• 0000";
        }

        String bank = (String) req.get("bank");
        if (bank == null || bank.trim().isEmpty()) {
            bank = "Chase";
        }

        String currency = (String) req.get("currency");
        if (currency == null || currency.trim().isEmpty()) {
            currency = "USD";
        }

        long timestamp = System.currentTimeMillis();
        Object tsObj = req.get("timestamp");
        if (tsObj instanceof Number) {
            timestamp = ((Number) tsObj).longValue();
        }

        String ipAddress = (String) req.get("ipAddress");
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            ipAddress = "127.0.0.1";
        }

        String location = (String) req.get("location");
        if (location == null || location.trim().isEmpty()) {
            location = "Unknown Location";
        }

        String deviceHash = (String) req.get("deviceHash");
        if (deviceHash == null || deviceHash.trim().isEmpty()) {
            deviceHash = "DEV_WEB_CLIENT";
        }

        boolean vpnOrTor = false;
        Object vpnObj = req.get("vpnOrTor");
        if (vpnObj instanceof Boolean) {
            vpnOrTor = (Boolean) vpnObj;
        } else if (vpnObj != null) {
            vpnOrTor = Boolean.parseBoolean(String.valueOf(vpnObj));
        }

        // Build Transaction Model
        Transaction tx = new Transaction(
                txId, userId, userFullName, cardMasked, bank,
                amount, currency, timestamp, ipAddress, location, deviceHash, vpnOrTor
        );

        // Run AI Fraud Detection Engine (Evaluates rules/ensemble + persists to Supabase)
        engine.processTransactionSync(tx);

        return tx;
    }

    public List<Transaction> getRecentTransactions(int limit) throws Exception {
        int l = limit > 0 ? limit : 50;
        return transactionDAO.getRecentTransactions(l);
    }

    public Transaction getTransactionById(String txId) throws Exception {
        if (txId == null || txId.trim().isEmpty()) return null;
        return transactionDAO.findById(txId.trim());
    }

    public boolean updateAnalystStatus(String txId, String status, String analystName) throws Exception {
        Transaction tx = transactionDAO.findById(txId);
        if (tx == null) {
            return false;
        }

        transactionDAO.updateAnalystStatus(txId, status);

        // Record Audit Log in Supabase
        String analyst = (analystName != null && !analystName.trim().isEmpty()) ? analystName : "Security Analyst";
        AuditLog log = new AuditLog(txId, analyst, "STATUS_CHANGE_" + status);
        auditLogDAO.save(log);

        return true;
    }

    public List<UserProfile> getAllUsers() throws Exception {
        return userProfileDAO.findAll();
    }

    public UserProfile getUserProfile(String userId) throws Exception {
        return userProfileDAO.findById(userId);
    }

    public void saveUserProfile(UserProfile profile) throws Exception {
        userProfileDAO.save(profile);
    }

    public boolean setUserBlacklistStatus(String userId, boolean blacklisted, String analystName) throws Exception {
        UserProfile profile = userProfileDAO.findById(userId);
        if (profile == null) {
            return false;
        }

        userProfileDAO.setBlacklisted(userId, blacklisted);

        // Record Audit Log
        String analyst = (analystName != null && !analystName.trim().isEmpty()) ? analystName : "Security Analyst";
        AuditLog log = new AuditLog(userId, analyst, blacklisted ? "USER_BLACKLISTED" : "USER_UNBLACKLISTED");
        auditLogDAO.save(log);

        return true;
    }

    public List<Transaction> getActiveAlerts(int limit) throws Exception {
        List<Transaction> all = transactionDAO.getRecentTransactions(limit > 0 ? limit : 100);
        List<Transaction> alerts = new ArrayList<>();
        for (Transaction tx : all) {
            if (tx.getResult() != null) {
                DetectionResult.Verdict v = tx.getResult().getVerdict();
                if (v == DetectionResult.Verdict.AUTO_BLOCKED ||
                    v == DetectionResult.Verdict.CHALLENGE_3DS) {
                    alerts.add(tx);
                }
            }
        }
        return alerts;
    }

    public List<AuditLog> getAuditLogs(int limit) throws Exception {
        return auditLogDAO.getRecentLogs(limit > 0 ? limit : 100);
    }

    public MetricsAggregator.MetricsSnapshot getMetricsSnapshot() {
        return engine.getMetrics().getSnapshot();
    }

    public SystemConfiguration getSystemConfig() {
        return engine.getCurrentConfig();
    }

    public void updateSystemConfig(SystemConfiguration config, String analystName) throws Exception {
        engine.applyConfiguration(config);

        String analyst = (analystName != null && !analystName.trim().isEmpty()) ? analystName : "Admin";
        AuditLog log = new AuditLog("CONFIG", analyst, "UPDATED_SYSTEM_CONFIGURATION: Model=" + config.getActiveModelVersion());
        auditLogDAO.save(log);
    }

    public void startSimulation() {
        engine.startSimulation();
    }

    public void stopSimulation() {
        engine.stopSimulation();
    }

    public boolean isSimulationRunning() {
        return engine.isRunning();
    }

    public void injectSimulatedAttack() {
        engine.injectSimulatedAttack();
    }
}
