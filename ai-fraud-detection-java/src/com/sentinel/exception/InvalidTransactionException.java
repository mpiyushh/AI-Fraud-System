package com.sentinel.exception;

/**
 * Thrown when an incoming transaction has invalid fields, tampered signatures,
 * or negative amounts.
 */
public class InvalidTransactionException extends FraudSystemException {

    private final String transactionId;

    public InvalidTransactionException(String transactionId, String reason) {
        super("ERR_INVALID_TRANSACTION", "Transaction [" + transactionId + "] is invalid: " + reason, null);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
