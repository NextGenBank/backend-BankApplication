package com.nextgenbank.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private String IBAN;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    private BigDecimal balance;
    private BigDecimal absoluteTransferLimit;
    private BigDecimal dailyTransferAmount;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Bidirectional mappings
    @OneToMany(mappedBy = "fromAccount")
    @JsonIgnore
    private List<Transaction> outgoingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "toAccount")
    @JsonIgnore
    private List<Transaction> incomingTransactions = new ArrayList<>();

    // Getters and Setters
    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAbsoluteTransferLimit() {
        return absoluteTransferLimit;
    }

    public void setAbsoluteTransferLimit(BigDecimal absoluteTransferLimit) {
        this.absoluteTransferLimit = absoluteTransferLimit;
    }

    public BigDecimal getDailyTransferAmount() {
        return dailyTransferAmount;
    }

    public void setDailyTransferAmount(BigDecimal dailyTransferAmount) {
        this.dailyTransferAmount = dailyTransferAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<Transaction> getOutgoingTransactions() {
        return outgoingTransactions;
    }
    public void setOutgoingTransactions(List<Transaction> outgoingTransactions) {
        this.outgoingTransactions = outgoingTransactions;
    }

    public List<Transaction> getIncomingTransactions() {
        return incomingTransactions;
    }
    public void setIncomingTransactions(List<Transaction> incomingTransactions) {
        this.incomingTransactions = incomingTransactions;
    }

}