package com.sentinel.exception;

/**
 * Root domain exception for the AI Fraud Detection System.
 * Demonstrates OOP Exception Handling hierarchy.
 */
public class FraudSystemException extends Exception {
    
    private final String errorCode;

    public FraudSystemException(String message) {
        super(message);
        this.errorCode = "ERR_FRAUD_GENERIC";
    }

    public FraudSystemException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ERR_FRAUD_GENERIC";
    }

    public FraudSystemException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
