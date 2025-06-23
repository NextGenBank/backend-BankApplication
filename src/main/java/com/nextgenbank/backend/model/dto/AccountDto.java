package com.nextgenbank.backend.model.dto;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;

import java.math.BigDecimal;

public class AccountDto {
    private String iban;
    private AccountType accountType;
    private BigDecimal balance;

    public AccountDto(Account account) {
        this.iban = account.getIBAN();
        this.accountType = account.getAccountType();
        this.balance = account.getBalance();
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
}