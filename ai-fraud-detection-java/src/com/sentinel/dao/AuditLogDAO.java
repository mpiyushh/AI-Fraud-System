package com.sentinel.dao;

import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.AuditLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Audit Logs.
 * Handles security action recording and historical review.
 */
public class AuditLogDAO implements GenericDAO<AuditLog, Long> {

    private final DatabaseConnectionFactory connectionFactory;

    public AuditLogDAO() {
        this.connectionFactory = DatabaseConnectionFactory.getInstance();
    }

    @Override
    public void save(AuditLog log) throws DatabaseException {
        String sql = "INSERT INTO audit_logs (tx_id, analyst_name, action_taken, timestamp) VALUES (?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, log.getTransactionId());
            ps.setString(2, log.getAnalystName());
            ps.setString(3, log.getActionTaken());
            ps.setLong(4, log.getTimestamp());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.setLogId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist audit log for tx [" + log.getTransactionId() + "]", e);
        }
    }

    @Override
    public AuditLog findById(Long logId) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs WHERE log_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to find audit log by ID [" + logId + "]", e);
        }
    }

    public List<AuditLog> findByTransactionId(String txId) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs WHERE tx_id = ? ORDER BY timestamp DESC";
        List<AuditLog> list = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to query audit logs for tx [" + txId + "]", e);
        }
    }

    @Override
    public List<AuditLog> findAll() throws DatabaseException {
        return getRecentLogs(100);
    }

    public List<AuditLog> getRecentLogs(int limit) throws DatabaseException {
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch recent audit logs", e);
        }
    }

    @Override
    public void update(AuditLog log) throws DatabaseException {
        if (log.getLogId() == null) {
            save(log);
            return;
        }

        String sql = "UPDATE audit_logs SET tx_id = ?, analyst_name = ?, action_taken = ?, timestamp = ? WHERE log_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, log.getTransactionId());
            ps.setString(2, log.getAnalystName());
            ps.setString(3, log.getActionTaken());
            ps.setLong(4, log.getTimestamp());
            ps.setLong(5, log.getLogId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update audit log [" + log.getLogId() + "]", e);
        }
    }

    @Override
    public void delete(Long logId) throws DatabaseException {
        String sql = "DELETE FROM audit_logs WHERE log_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, logId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete audit log [" + logId + "]", e);
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        return new AuditLog(
                rs.getLong("log_id"),
                rs.getString("tx_id"),
                rs.getString("analyst_name"),
                rs.getString("action_taken"),
                rs.getLong("timestamp")
        );
    }
}
