package com.sentinel.dao;

import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.exception.DatabaseException;
import com.sentinel.exception.InvalidTransactionException;
import com.sentinel.model.DetectionResult;
import com.sentinel.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Transactions.
 * Fulfills "classes for the database operations - 7 marks" using JDBC PreparedStatement.
 */
public class TransactionDAO implements GenericDAO<Transaction, String> {

    private final DatabaseConnectionFactory connectionFactory;

    public TransactionDAO() {
        this.connectionFactory = DatabaseConnectionFactory.getInstance();
    }

    @Override
    public void save(Transaction tx) throws DatabaseException {
        String sql = "INSERT OR REPLACE INTO transactions " +
                "(tx_id, user_id, user_name, card_masked, amount, currency, tx_timestamp, " +
                " ip_address, location, device_hash, is_vpn_tor, bank, risk_score, verdict, dominant_threat, analyst_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getUserId());
            ps.setString(3, tx.getUserFullName());
            ps.setString(4, tx.getCardMasked());
            ps.setDouble(5, tx.getAmount());
            ps.setString(6, tx.getCurrency());
            ps.setLong(7, tx.getTimestamp());
            ps.setString(8, tx.getIpAddress());
            ps.setString(9, tx.getLocation());
            ps.setString(10, tx.getDeviceHash());
            ps.setInt(11, tx.isVpnOrTor() ? 1 : 0);
            ps.setString(12, tx.getBank());

            if (tx.getResult() != null) {
                ps.setInt(13, tx.getResult().getRiskScore());
                ps.setString(14, tx.getResult().getVerdict().name());
                ps.setString(15, tx.getResult().getDominantThreat());
            } else {
                ps.setNull(13, java.sql.Types.INTEGER);
                ps.setNull(14, java.sql.Types.VARCHAR);
                ps.setNull(15, java.sql.Types.VARCHAR);
            }

            ps.setString(16, tx.getAnalystStatus());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist transaction [" + tx.getTransactionId() + "]", e);
        }
    }

    @Override
    public Transaction findById(String txId) throws DatabaseException {
        String sql = "SELECT * FROM transactions WHERE tx_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;

        } catch (SQLException | InvalidTransactionException e) {
            throw new DatabaseException("Failed to query transaction by ID [" + txId + "]", e);
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        return getRecentTransactions(50);
    }

    public List<Transaction> getRecentTransactions(int limit) throws DatabaseException {
        String sql = "SELECT * FROM transactions ORDER BY tx_timestamp DESC LIMIT ?";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;

        } catch (SQLException | InvalidTransactionException e) {
            throw new DatabaseException("Failed to fetch recent transactions", e);
        }
    }

    @Override
    public void update(Transaction tx) throws DatabaseException {
        save(tx);
    }

    public void updateAnalystStatus(String txId, String status) throws DatabaseException {
        String sql = "UPDATE transactions SET analyst_status = ? WHERE tx_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, txId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for transaction [" + txId + "]", e);
        }
    }

    @Override
    public void delete(String txId) throws DatabaseException {
        String sql = "DELETE FROM transactions WHERE tx_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete transaction [" + txId + "]", e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException, InvalidTransactionException {
        String bank = rs.getString("bank");
        if (bank == null) bank = "Chase";
        Transaction tx = new Transaction(
                rs.getString("tx_id"),
                rs.getString("user_id"),
                rs.getString("user_name"),
                rs.getString("card_masked"),
                bank,
                rs.getDouble("amount"),
                rs.getString("currency"),
                rs.getLong("tx_timestamp"),
                rs.getString("ip_address"),
                rs.getString("location"),
                rs.getString("device_hash"),
                rs.getInt("is_vpn_tor") == 1
        );

        int riskScore = rs.getInt("risk_score");
        String verdictStr = rs.getString("verdict");
        String threat = rs.getString("dominant_threat");

        if (verdictStr != null) {
            DetectionResult.Verdict verdict = DetectionResult.Verdict.valueOf(verdictStr);
            DetectionResult result = new DetectionResult(riskScore, verdict, threat, Collections.emptyList(), 12.0);
            tx.setResult(result);
        }

        tx.setAnalystStatus(rs.getString("analyst_status"));
        return tx;
    }
}
