package com.nextgenbank.backend.model.dto;

import java.math.BigDecimal;


public class AtmTransactionDto {
    private String iban;
    private BigDecimal amount;
    private Integer bills;

    public AtmTransactionDto() {
    }

    public AtmTransactionDto(String iban, BigDecimal amount, Integer bills) {
        this.iban = iban;
        this.amount = amount;
        this.bills = bills;
    }

    // --- Getters / Setters ---
    public String getIban() {
        return iban;
    }
    public void setIban(String iban) {
        this.iban = iban;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getBills() {
        return bills;
    }
    public void setBills(Integer bills) {
        this.bills = bills;
    }
}
