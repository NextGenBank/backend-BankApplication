package com.nextgenbank.backend.service;


import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ATMService {
    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    public ATMService(AccountRepository accountRepo, TransactionRepository txRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    @Transactional
    public Transaction deposit(String iban, BigDecimal amount) {
        Account acc = accountRepo.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        acc.setBalance(acc.getBalance().add(amount));
        accountRepo.save(acc);

        Transaction tx = new Transaction();
        tx.setToAccount(acc);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setTimestamp(LocalDateTime.now());

        return txRepo.save(tx);
    }

    @Transactional
    public Transaction withdraw(String iban, BigDecimal amount, Integer bills) {
        Account acc = accountRepo.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (acc.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        acc.setBalance(acc.getBalance().subtract(amount));
        accountRepo.save(acc);

        Transaction tx = new Transaction();
        tx.setFromAccount(acc);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setTimestamp(LocalDateTime.now());

        return txRepo.save(tx);
    }
}
