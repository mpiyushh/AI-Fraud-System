package com.sentinel.dao;

import com.sentinel.exception.DatabaseException;
import com.sentinel.model.UserProfile;
import com.sentinel.supabase.SupabaseUserProfileRepository;

import java.util.List;

/**
 * Data Access Object for User Profiles.
 * Communicates exclusively with Supabase via PostgREST REST API.
 */
public class UserProfileDAO implements GenericDAO<UserProfile, String> {

    private final SupabaseUserProfileRepository repository;

    public UserProfileDAO() {
        this.repository = new SupabaseUserProfileRepository();
    }

    public UserProfileDAO(SupabaseUserProfileRepository repository) {
        this.repository = repository != null ? repository : new SupabaseUserProfileRepository();
    }

    @Override
    public void save(UserProfile profile) throws DatabaseException {
        repository.save(profile);
    }

    @Override
    public UserProfile findById(String userId) throws DatabaseException {
        return repository.findById(userId);
    }

    @Override
    public List<UserProfile> findAll() throws DatabaseException {
        return repository.findAll();
    }

    @Override
    public void update(UserProfile entity) throws DatabaseException {
        repository.save(entity);
    }

    public void setBlacklisted(String userId, boolean blacklisted) throws DatabaseException {
        repository.setBlacklisted(userId, blacklisted);
    }

    @Override
    public void delete(String userId) throws DatabaseException {
        repository.delete(userId);
    }
}
