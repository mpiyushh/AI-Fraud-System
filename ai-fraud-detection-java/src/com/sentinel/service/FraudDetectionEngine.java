package com.sentinel.service;

import com.sentinel.dao.TransactionDAO;
import com.sentinel.dao.UserProfileDAO;
import com.sentinel.exception.DatabaseException;
import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.DetectionResult;
import com.sentinel.model.Transaction;
import com.sentinel.model.UserProfile;
import com.sentinel.model.detectors.CompositeEnsembleDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * High-throughput, multithreaded AI Fraud Detection Orchestrator.
 * Fulfills:
 * - Multithreading & Synchronization (BlockingQueue, ExecutorService, Worker Threads)
 * - OOP Implementation & Collections
 * - Integration with Database DAOs & Supabase REST API
 */
public class FraudDetectionEngine {

    private final CompositeEnsembleDetector ensemble;
    private final TransactionDAO transactionDAO;
    private final UserProfileDAO userProfileDAO;
    private final com.sentinel.dao.ConfigurationDAO configurationDAO;
    private final MetricsAggregator metrics;
    private com.sentinel.model.SystemConfiguration currentConfig;

    // Concurrency constructs
    private final BlockingQueue<Transaction> queue = new LinkedBlockingQueue<>(2000);
    private final ExecutorService workerPool;
    private final List<TransactionListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean isRunning = false;
    private Thread simulationProducerThread;
    private final Random random = new Random();

    public FraudDetectionEngine() {
        this.ensemble = new CompositeEnsembleDetector();
        this.transactionDAO = new TransactionDAO();
        this.userProfileDAO = new UserProfileDAO();
        this.configurationDAO = new com.sentinel.dao.ConfigurationDAO();
        this.metrics = new MetricsAggregator();

        // Load configuration from Supabase
        try {
            this.currentConfig = configurationDAO.findById("DEFAULT");
            if (this.currentConfig != null) {
                ensemble.setBlockThreshold(currentConfig.getBlockThreshold());
                ensemble.setChallengeThreshold(currentConfig.getChallengeThreshold());
            }
        } catch (Exception e) {
            this.currentConfig = new com.sentinel.model.SystemConfiguration();
        }

        // 4 concurrent worker threads evaluating transactions
        this.workerPool = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Sentinel-Worker-" + count++);
                t.setDaemon(true);
                return t;
            }
        });

        // Launch consumer tasks
        for (int i = 0; i < 4; i++) {
            workerPool.submit(this::processQueueLoop);
        }
    }

    public void addListener(TransactionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(TransactionListener listener) {
        listeners.remove(listener);
    }

    public CompositeEnsembleDetector getEnsemble() {
        return ensemble;
    }

    public MetricsAggregator getMetrics() {
        return metrics;
    }

    public TransactionDAO getTransactionDAO() {
        return transactionDAO;
    }

    public UserProfileDAO getUserProfileDAO() {
        return userProfileDAO;
    }

    public com.sentinel.dao.ConfigurationDAO getConfigurationDAO() {
        return configurationDAO;
    }

    public com.sentinel.model.SystemConfiguration getCurrentConfig() {
        return currentConfig;
    }

    /**
     * Updates system configurations and persists to Supabase.
     * Fulfills GUVI: Admin - System Configuration.
     */
    public synchronized void applyConfiguration(com.sentinel.model.SystemConfiguration config) throws Exception {
        this.currentConfig = config;
        ensemble.setBlockThreshold(config.getBlockThreshold());
        ensemble.setChallengeThreshold(config.getChallengeThreshold());
        configurationDAO.save(config);
    }

    public void submitTransaction(Transaction tx) {
        queue.offer(tx);
    }

    /**
     * Synchronously processes a transaction initiated from the User Dashboard.
     * Fulfills GUVI: User - Transaction Monitoring (Input: Details -> Output: Alerts).
     */
    public DetectionResult processTransactionSync(Transaction tx) throws Exception {
        UserProfile profile = userProfileDAO.findById(tx.getUserId());
        if (profile == null) {
            profile = new UserProfile(
                    tx.getUserId(),
                    tx.getUserFullName(),
                    tx.getLocation(),
                    tx.getDeviceHash(),
                    50.0,
                    25.0,
                    1,
                    0,
                    false
            );
        }

        // Evaluate AI
        DetectionResult result = ensemble.analyze(tx, profile);
        tx.setResult(result);

        if (result.isApproved()) {
            profile.recordNewTransaction(tx.getAmount(), tx.getLocation());
            userProfileDAO.save(profile);
        }

        // Persist via Supabase
        transactionDAO.save(tx);

        metrics.recordTransaction(tx.getAmount(), result.isBlocked(), result.isChallenged(), result.getLatencyMs());

        // Notify UI
        MetricsAggregator.MetricsSnapshot snapshot = metrics.getSnapshot();
        for (TransactionListener listener : listeners) {
            listener.onTransactionProcessed(tx);
            listener.onMetricsUpdated(snapshot);
        }

        return result;
    }

    /**
     * Consumer loop executed by worker threads in the pool.
     */
    private void processQueueLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Transaction tx = queue.take(); // Thread-safe blocking take
                evaluateAndPersist(tx);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Worker processing error: " + e.getMessage());
            }
        }
    }

    /**
     * Core processing: AI Inference -> JDBC Persistence -> Metrics -> Listener Notification.
     */
    private void evaluateAndPersist(Transaction tx) {
        try {
            // 1. Fetch user profile from DB (or create temporary baseline)
            UserProfile profile = userProfileDAO.findById(tx.getUserId());
            if (profile == null) {
                profile = new UserProfile(
                        tx.getUserId(),
                        tx.getUserFullName(),
                        tx.getLocation(),
                        tx.getDeviceHash(),
                        50.0,
                        25.0,
                        1,
                        0,
                        false
                );
            }

            // 2. Perform AI Ensemble Inference
            DetectionResult result = ensemble.analyze(tx, profile);
            tx.setResult(result);

            // 3. Update profile baseline if legitimate
            if (result.isApproved()) {
                profile.recordNewTransaction(tx.getAmount(), tx.getLocation());
                userProfileDAO.save(profile);
            }

            // 4. Save evaluated transaction via JDBC
            transactionDAO.save(tx);

            // 5. Update metrics
            metrics.recordTransaction(
                    tx.getAmount(),
                    result.isBlocked(),
                    result.isChallenged(),
                    result.getLatencyMs()
            );

            // 6. Notify observers
            MetricsAggregator.MetricsSnapshot snapshot = metrics.getSnapshot();
            for (TransactionListener listener : listeners) {
                listener.onTransactionProcessed(tx);
                listener.onMetricsUpdated(snapshot);
            }

        } catch (Exception e) {
            System.err.println("Failed to evaluate/persist transaction [" + tx.getTransactionId() + "]: " + e.getMessage());
        }
    }

    /**
     * Starts continuous background simulation of incoming payment transactions.
     */
    public synchronized void startSimulation() {
        if (isRunning) return;
        isRunning = true;

        simulationProducerThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Transaction tx = generateRandomTransaction(false);
                    submitTransaction(tx);
                    Thread.sleep(1800 + random.nextInt(1200)); // ~2 transactions every 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Simulation generator exception: " + e.getMessage());
                }
            }
        }, "Sentinel-Simulation-Producer");

        simulationProducerThread.setDaemon(true);
        simulationProducerThread.start();
    }

    public synchronized void stopSimulation() {
        isRunning = false;
        if (simulationProducerThread != null) {
            simulationProducerThread.interrupt();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Directly injects an adversarial fraud attack vector into the processing queue.
     */
    public void injectSimulatedAttack() {
        try {
            Transaction attackTx = generateRandomTransaction(true);
            submitTransaction(attackTx);
        } catch (Exception e) {
            System.err.println("Attack injection failed: " + e.getMessage());
        }
    }

    private Transaction generateRandomTransaction(boolean isAdversarial) throws InvalidTransactionException {
        String[] users = {"Alexandre V.", "Sarah Jenkins", "Liam O'Connor", "Elena Rostova", "Marcus Chen", "David Miller"};
        String[] userIds = {"USR-101", "USR-102", "USR-103", "USR-104", "USR-105", "USR-106"};
        String[] locations = {"London, UK", "Austin, US", "Dublin, IE", "Toronto, CA", "Singapore, SG", "Berlin, DE"};
        String[] cards = {"Visa •• 8492", "Mastercard •• 1042", "Amex •• 3310", "Virtual •• 9921", "Visa •• 4019"};
        String[] banks = {"Chase", "Wells Fargo", "Barclays", "HSBC", "Citi", "Revolut"};

        int idx = random.nextInt(users.length);
        String txId = "TX-" + (10000 + random.nextInt(90000));
        String userId = isAdversarial ? "USR-BAD" : userIds[idx];
        String userName = isAdversarial ? "Hostile Sybil Ring" : users[idx];
        String card = cards[random.nextInt(cards.length)];
        String bank = isAdversarial ? "Revolut" : banks[random.nextInt(banks.length)];

        double amount = isAdversarial
                ? (800.0 + random.nextDouble() * 4200.0)
                : (15.0 + random.nextDouble() * 180.0);

        String location = isAdversarial ? "Kyiv, UA (Tor Exit)" : locations[idx];
        String deviceHash = isAdversarial ? "DEV_SPOOFED_EMULATOR_01" : "DEV_TRUSTED_" + idx;
        boolean vpnOrTor = isAdversarial;

        return new Transaction(
                txId,
                userId,
                userName,
                card,
                bank,
                Math.round(amount * 100.0) / 100.0,
                "USD",
                System.currentTimeMillis(),
                isAdversarial ? "185.220.101.5" : "192.168.1." + random.nextInt(250),
                location,
                deviceHash,
                vpnOrTor
        );
    }
}
