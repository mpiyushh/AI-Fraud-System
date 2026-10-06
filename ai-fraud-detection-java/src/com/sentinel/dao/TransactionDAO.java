package com.sentinel.dao;

import com.sentinel.exception.DatabaseException;
import com.sentinel.model.Transaction;
import com.sentinel.supabase.SupabaseTransactionRepository;

import java.util.List;

/**
 * Data Access Object for Transactions.
 * Communicates exclusively with Supabase via PostgREST REST API.
 */
public class TransactionDAO implements GenericDAO<Transaction, String> {

    private final SupabaseTransactionRepository repository;

    public TransactionDAO() {
        this.repository = new SupabaseTransactionRepository();
    }

    public TransactionDAO(SupabaseTransactionRepository repository) {
        this.repository = repository != null ? repository : new SupabaseTransactionRepository();
    }

    @Override
    public void save(Transaction tx) throws DatabaseException {
        repository.save(tx);
    }

    @Override
    public Transaction findById(String txId) throws DatabaseException {
        return repository.findById(txId);
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        return repository.findAll();
    }

    public List<Transaction> getRecentTransactions(int limit) throws DatabaseException {
        return repository.getRecentTransactions(limit);
    }

    @Override
    public void update(Transaction tx) throws DatabaseException {
        repository.save(tx);
    }

    public void updateAnalystStatus(String txId, String status) throws DatabaseException {
        repository.updateAnalystStatus(txId, status);
    }

    @Override
    public void delete(String txId) throws DatabaseException {
        repository.delete(txId);
    }
}
