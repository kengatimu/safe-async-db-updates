package com.bishop.application.enums;

public enum TransactionStatus {

    INITIALIZED("001", "Transaction initialized"),
    PENDING("002", "Transaction is in progress"),
    SUCCESS("00", "Transaction completed successfully"),
    FAILURE("01", "Transaction failed"),
    TIMEOUT("-3", "Transaction timed out");

    private final String code;
    private final String description;

    TransactionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
