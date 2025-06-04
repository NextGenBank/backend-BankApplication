package com.nextgenbank.backend.model.dto;

import java.math.BigDecimal;

public class TransactionDto {
    private String fromIban;   // Для TRANSFER и WITHDRAWAL
    private String toIban;     // Для TRANSFER и DEPOSIT
    private BigDecimal amount;
    private Integer bills;     // Опционально для WITHDRAWAL

    public TransactionDto() { }

    public String getFromIban() {
        return fromIban;
    }
    public void setFromIban(String fromIban) {
        this.fromIban = fromIban;
    }

    public String getToIban() {
        return toIban;
    }
    public void setToIban(String toIban) {
        this.toIban = toIban;
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
