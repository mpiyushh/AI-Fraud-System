package com.sentinel.service;

import com.sentinel.model.Transaction;

/**
 * Observer interface for listening to real-time evaluation events.
 * Demonstrates Interface-based event decoupling in OOP.
 */
public interface TransactionListener {

    /**
     * Fired when a transaction has been screened and evaluated by the AI ensemble.
     */
    void onTransactionProcessed(Transaction transaction);

    /**
     * Fired when system metrics are updated.
     */
    void onMetricsUpdated(MetricsAggregator.MetricsSnapshot snapshot);
}
