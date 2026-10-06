package com.sentinel.dao;

import com.sentinel.exception.DatabaseException;
import com.sentinel.model.AuditLog;
import com.sentinel.supabase.SupabaseAuditLogRepository;

import java.util.List;

/**
 * Data Access Object for Audit Logs.
 * Communicates exclusively with Supabase via PostgREST REST API.
 */
public class AuditLogDAO implements GenericDAO<AuditLog, Long> {

    private final SupabaseAuditLogRepository repository;

    public AuditLogDAO() {
        this.repository = new SupabaseAuditLogRepository();
    }

    public AuditLogDAO(SupabaseAuditLogRepository repository) {
        this.repository = repository != null ? repository : new SupabaseAuditLogRepository();
    }

    @Override
    public void save(AuditLog log) throws DatabaseException {
        repository.save(log);
    }

    @Override
    public AuditLog findById(Long logId) throws DatabaseException {
        return repository.findById(logId);
    }

    public List<AuditLog> findByTransactionId(String txId) throws DatabaseException {
        return repository.findByTransactionId(txId);
    }

    @Override
    public List<AuditLog> findAll() throws DatabaseException {
        return repository.findAll();
    }

    public List<AuditLog> getRecentLogs(int limit) throws DatabaseException {
        return repository.getRecentLogs(limit);
    }

    @Override
    public void update(AuditLog entity) throws DatabaseException {
        save(entity);
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        // Audit logs are append-only for security
    }
}
