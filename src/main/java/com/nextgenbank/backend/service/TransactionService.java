package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository     = accountRepository;
        this.userRepository        = userRepository;
    }

    /**
     * Возвращает историю транзакций (TRANSFER, DEPOSIT, WITHDRAWAL) для данного пользователя.
     */
    public List<Transaction> getTransactionsForUser(User user) {
        Long userId = user.getUserId();
        return transactionRepository.findAllByUserId(userId);
    }

    @Transactional
    public Transaction doTransfer(User initiator, String fromIban, String toIban, BigDecimal amount) {
        Account fromAcc = accountRepository.findById(fromIban)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromIban));
        Account toAcc = accountRepository.findById(toIban)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + toIban));

        if (!fromAcc.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to source account");
        }
        if (fromAcc.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account");
        }

        fromAcc.setBalance(fromAcc.getBalance().subtract(amount));
        toAcc.setBalance(toAcc.getBalance().add(amount));
        accountRepository.save(fromAcc);
        accountRepository.save(toAcc);

        Transaction tx = new Transaction();
        tx.setFromAccount(fromAcc);
        tx.setToAccount(toAcc);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);

        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction doDeposit(User initiator, String toIban, BigDecimal amount) {
        Account toAcc = accountRepository.findById(toIban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + toIban));

        if (!toAcc.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to deposit into this account");
        }

        toAcc.setBalance(toAcc.getBalance().add(amount));
        accountRepository.save(toAcc);

        Transaction tx = new Transaction();
        tx.setFromAccount(null);
        tx.setToAccount(toAcc);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);

        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction doWithdraw(User initiator, String fromIban, BigDecimal amount, Integer bills) {
        Account fromAcc = accountRepository.findById(fromIban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + fromIban));

        if (!fromAcc.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to withdraw from this account");
        }
        if (fromAcc.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        fromAcc.setBalance(fromAcc.getBalance().subtract(amount));
        accountRepository.save(fromAcc);

        Transaction tx = new Transaction();
        tx.setFromAccount(fromAcc);
        tx.setToAccount(null);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);

        return transactionRepository.save(tx);
    }
}
