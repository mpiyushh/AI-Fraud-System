package com.sentinel.model;

import java.io.Serializable;

/**
 * Domain entity representing an audit log entry for security and analyst actions.
 */
public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long logId;
    private final String transactionId;
    private final String analystName;
    private final String actionTaken;
    private final long timestamp;

    public AuditLog(String transactionId, String analystName, String actionTaken) {
        this(null, transactionId, analystName, actionTaken, System.currentTimeMillis());
    }

    public AuditLog(Long logId, String transactionId, String analystName, String actionTaken, long timestamp) {
        this.logId = logId;
        this.transactionId = transactionId != null ? transactionId : "";
        this.analystName = analystName != null ? analystName : "SYSTEM";
        this.actionTaken = actionTaken != null ? actionTaken : "UNKNOWN";
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAnalystName() {
        return analystName;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%d] Tx:%s | Analyst:%s | Action:%s | Time:%d",
                logId, transactionId, analystName, actionTaken, timestamp);
    }
}
