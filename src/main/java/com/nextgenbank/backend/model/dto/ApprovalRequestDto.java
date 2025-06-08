package com.nextgenbank.backend.model.dto;

import java.math.BigDecimal;

public class ApprovalRequestDto {
    private Long userId;
    private String accountIban;
    private BigDecimal absoluteTransferLimit;

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountIban() {
        return accountIban;
    }

    public void setAccountIban(String accountIban) {
        this.accountIban = accountIban;
    }

    public BigDecimal getAbsoluteTransferLimit() {
        return absoluteTransferLimit;
    }

    public void setAbsoluteTransferLimit(BigDecimal absoluteTransferLimit) {
        this.absoluteTransferLimit = absoluteTransferLimit;
    }

    @Override
    public String toString() {
        return "ApprovalRequestDto{" +
                "userId=" + userId +
                ", accountIban='" + accountIban + '\'' +
                ", absoluteTransferLimit=" + absoluteTransferLimit +
                '}';
    }
}