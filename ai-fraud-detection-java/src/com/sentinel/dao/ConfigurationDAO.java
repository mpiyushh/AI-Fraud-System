package com.sentinel.dao;

import com.sentinel.db.DatabaseConnectionFactory;
import com.sentinel.exception.DatabaseException;
import com.sentinel.model.SystemConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for System Configurations.
 * Fulfills Admin System Configuration persistence via JDBC.
 */
public class ConfigurationDAO implements GenericDAO<SystemConfiguration, String> {

    private final DatabaseConnectionFactory connectionFactory;

    public ConfigurationDAO() {
        this.connectionFactory = DatabaseConnectionFactory.getInstance();
    }

    @Override
    public void save(SystemConfiguration config) throws DatabaseException {
        String sql = "INSERT OR REPLACE INTO system_configurations " +
                "(config_key, block_threshold, challenge_threshold, velocity_limit, max_amount, tor_block, model_version) " +
                "VALUES ('DEFAULT', ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, config.getBlockThreshold());
            ps.setInt(2, config.getChallengeThreshold());
            ps.setInt(3, config.getVelocityBurstLimit());
            ps.setDouble(4, config.getMaxAmountCeiling());
            ps.setInt(5, config.isTorHardBlock() ? 1 : 0);
            ps.setString(6, config.getActiveModelVersion());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist system configuration", e);
        }
    }

    @Override
    public SystemConfiguration findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM system_configurations WHERE config_key = 'DEFAULT'";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new SystemConfiguration(
                        rs.getInt("block_threshold"),
                        rs.getInt("challenge_threshold"),
                        rs.getInt("velocity_limit"),
                        rs.getDouble("max_amount"),
                        rs.getInt("tor_block") == 1,
                        rs.getString("model_version")
                );
            }
            return new SystemConfiguration(); // default fallback

        } catch (SQLException e) {
            throw new DatabaseException("Failed to load system configuration", e);
        }
    }

    @Override
    public List<SystemConfiguration> findAll() throws DatabaseException {
        List<SystemConfiguration> list = new ArrayList<>();
        list.add(findById("DEFAULT"));
        return list;
    }

    @Override
    public void update(SystemConfiguration config) throws DatabaseException {
        save(config);
    }

    @Override
    public void delete(String id) throws DatabaseException {
        // Kept for interface compliance
    }
}
