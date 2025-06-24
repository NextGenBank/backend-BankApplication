package com.nextgenbank.backend.model.dto;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;

import java.math.BigDecimal;

public class AccountDto {
    private String iban;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal absoluteTransferLimit;
    private BigDecimal dailyTransferAmount;

    public AccountDto(Account account) {
        this.iban = account.getIBAN();
        this.accountType = account.getAccountType();
        this.balance = account.getBalance();
        this.absoluteTransferLimit = account.getAbsoluteTransferLimit();
        this.dailyTransferAmount = account.getDailyTransferAmount();
    }

    // Getters
    public String getIban() {
        return iban;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }
    
    public BigDecimal getAbsoluteTransferLimit() {
        return absoluteTransferLimit;
    }
    
    public BigDecimal getDailyTransferAmount() {
        return dailyTransferAmount;
    }
}
