package com.nextgenbank.backend.model.dto;

import java.math.BigDecimal;

public class SwitchFundsRequestDto {
    private String from;
    private BigDecimal amount;

    public SwitchFundsRequestDto(String checking, BigDecimal bigDecimal) {
        this.from = checking;
        this.amount = bigDecimal;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}