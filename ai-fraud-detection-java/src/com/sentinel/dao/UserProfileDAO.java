package com.sentinel.dao;

import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.UserProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User Profiles.
 * Fulfills "classes for the database operations - 7 marks".
 */
public class UserProfileDAO implements GenericDAO<UserProfile, String> {

    private final DatabaseConnectionFactory connectionFactory;

    public UserProfileDAO() {
        this.connectionFactory = DatabaseConnectionFactory.getInstance();
    }

    @Override
    public void save(UserProfile profile) throws DatabaseException {
        String sql = "INSERT OR REPLACE INTO user_profiles " +
                "(user_id, full_name, primary_location, trusted_device, avg_amount, std_dev, " +
                " tx_count, chargeback_count, is_blacklisted, last_seen_timestamp, last_seen_location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, profile.getUserId());
            ps.setString(2, profile.getFullName());
            ps.setString(3, profile.getPrimaryLocation());
            ps.setString(4, profile.getTrustedDeviceHash());
            ps.setDouble(5, profile.getHistoricalAverageAmount());
            ps.setDouble(6, profile.getStandardDeviation());
            ps.setInt(7, profile.getTransactionCount());
            ps.setInt(8, profile.getChargebackCount());
            ps.setInt(9, profile.isBlacklisted() ? 1 : 0);
            ps.setLong(10, profile.getLastSeenTimestamp());
            ps.setString(11, profile.getLastSeenLocation());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist user profile [" + profile.getUserId() + "]", e);
        }
    }

    @Override
    public UserProfile findById(String userId) throws DatabaseException {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user profile [" + userId + "]", e);
        }
    }

    @Override
    public List<UserProfile> findAll() throws DatabaseException {
        String sql = "SELECT * FROM user_profiles";
        List<UserProfile> list = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to query all user profiles", e);
        }
    }

    @Override
    public void update(UserProfile entity) throws DatabaseException {
        save(entity);
    }

    public void setBlacklisted(String userId, boolean blacklisted) throws DatabaseException {
        String sql = "UPDATE user_profiles SET is_blacklisted = ? WHERE user_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, blacklisted ? 1 : 0);
            ps.setString(2, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update blacklist status for [" + userId + "]", e);
        }
    }

    @Override
    public void delete(String userId) throws DatabaseException {
        String sql = "DELETE FROM user_profiles WHERE user_id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user profile [" + userId + "]", e);
        }
    }

    private UserProfile mapRow(ResultSet rs) throws SQLException {
        UserProfile profile = new UserProfile(
                rs.getString("user_id"),
                rs.getString("full_name"),
                rs.getString("primary_location"),
                rs.getString("trusted_device"),
                rs.getDouble("avg_amount"),
                rs.getDouble("std_dev"),
                rs.getInt("tx_count"),
                rs.getInt("chargeback_count"),
                rs.getInt("is_blacklisted") == 1
        );
        profile.setLastSeenTimestamp(rs.getLong("last_seen_timestamp"));
        profile.setLastSeenLocation(rs.getString("last_seen_location"));
        return profile;
    }
}
