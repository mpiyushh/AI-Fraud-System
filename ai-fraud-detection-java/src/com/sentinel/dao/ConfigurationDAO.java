package com.sentinel.dao;

import com.sentinel.exception.DatabaseException;
import com.sentinel.model.SystemConfiguration;
import com.sentinel.supabase.SupabaseConfigurationRepository;

import java.util.List;

/**
 * Data Access Object for System Configurations.
 * Communicates exclusively with Supabase via PostgREST REST API.
 */
public class ConfigurationDAO implements GenericDAO<SystemConfiguration, String> {

    private final SupabaseConfigurationRepository repository;

    public ConfigurationDAO() {
        this.repository = new SupabaseConfigurationRepository();
    }

    public ConfigurationDAO(SupabaseConfigurationRepository repository) {
        this.repository = repository != null ? repository : new SupabaseConfigurationRepository();
    }

    @Override
    public void save(SystemConfiguration config) throws DatabaseException {
        repository.save(config);
    }

    @Override
    public SystemConfiguration findById(String id) throws DatabaseException {
        return repository.findById(id);
    }

    @Override
    public List<SystemConfiguration> findAll() throws DatabaseException {
        return repository.findAll();
    }

    @Override
    public void update(SystemConfiguration config) throws DatabaseException {
        repository.save(config);
    }

    @Override
    public void delete(String id) throws DatabaseException {
        // Not supported for system configuration
    }
}
