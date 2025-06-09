package com.nextgenbank.backend.model.dto;

import java.math.BigDecimal;

public class SwitchFundsResponseDto {
    private BigDecimal checkingBalance;
    private BigDecimal savingsBalance;

    public SwitchFundsResponseDto(BigDecimal checkingBalance, BigDecimal savingsBalance) {
        this.checkingBalance = checkingBalance;
        this.savingsBalance = savingsBalance;
    }

    public BigDecimal getCheckingBalance() {
        return checkingBalance;
    }

    public BigDecimal getSavingsBalance() {
        return savingsBalance;
    }
}

