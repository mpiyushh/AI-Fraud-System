package com.sentinel.exception;

/**
 * Thrown when database operations or JDBC connectivity encounters a failure.
 */
public class DatabaseException extends FraudSystemException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super("ERR_DATABASE_FAILURE", message, cause);
    }
}
