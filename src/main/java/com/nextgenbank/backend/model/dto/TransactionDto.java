package com.nextgenbank.backend.model.dto;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.UserRole;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class TransactionDto {
    private String fromIban;
    private String toIban;
    private BigDecimal amount;
    private Integer bills;
    private Long transactionId;
    private String fromAccount;
    private String toAccount;
    private String timestamp;
    private String userInitiating;
    private UserRole userRole;
    private TransactionType transactionType;


    public TransactionDto() { }

    public TransactionDto(Transaction transaction) {
        this.transactionId = transaction.getTransactionId();

        if (transaction.getFromAccount() != null) {
            this.fromAccount = transaction.getFromAccount().getIBAN();
        }

        if (transaction.getToAccount() != null) {
            this.toAccount = transaction.getToAccount().getIBAN();
        }

        this.amount = transaction.getAmount();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm");
        this.timestamp = transaction.getTimestamp().format(formatter);

        if (transaction.getInitiator() != null) {
            this.userInitiating = transaction.getInitiator().getFirstName() + " " + transaction.getInitiator().getLastName();
            this.userRole = transaction.getInitiator().getRole();
        }

        this.transactionType = transaction.getTransactionType();
    }



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
    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }


    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserInitiating() {
        return userInitiating;
    }

    public void setUserInitiating(String userInitiating) {
        this.userInitiating = userInitiating;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
}