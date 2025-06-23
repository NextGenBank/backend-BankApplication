package com.nextgenbank.backend.model;

public enum AmountFilterOperation {
    EQUAL("eq"),
    LESS_THAN("lt"),
    GREATER_THAN("gt");

    private final String dbValue;

    AmountFilterOperation(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AmountFilterOperation fromString(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase()) {
            case "EQUAL", "EQ" -> EQUAL;
            case "LESS", "LT", "LESS_THAN" -> LESS_THAN;
            case "GREATER", "GT", "GREATER_THAN" -> GREATER_THAN;
            default -> null;
        };
    }
}