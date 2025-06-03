package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionResponseDto> getTransactionsForUser(User user) {
        Long userId = user.getUserId();

        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(txn -> {
                    Account from = txn.getFromAccount();
                    Account to = txn.getToAccount();

                    return (from != null && from.getCustomer().getUserId().equals(userId)) ||
                            (to != null && to.getCustomer().getUserId().equals(userId));
                })
                .collect(Collectors.toList());

        return transactions.stream()
                .map(txn -> new TransactionResponseDto(
                        txn.getTransactionId(),
                        txn.getTransactionType(),
                        txn.getAmount(),
                        txn.getTimestamp(),
                        txn.getFromAccount() != null ? txn.getFromAccount().getIBAN() : null,
                        txn.getToAccount() != null ? txn.getToAccount().getIBAN() : null
                ))
                .collect(Collectors.toList());
    }

    public void switchFunds(User user, SwitchFundsRequestDto request) {
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customers can switch funds.");
        }

        List<Account> accounts = user.getAccountsOwned();

        Account main = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.CHECKING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Checking account not found"));

        Account savings = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.SAVINGS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Savings account not found"));

        Account from;
        Account to;

        if ("MAIN".equalsIgnoreCase(request.getFrom())) {
            from = main;
            to = savings;
        } else if ("SAVINGS".equalsIgnoreCase(request.getFrom())) {
            from = savings;
            to = main;
        } else {
            throw new IllegalArgumentException("Invalid source account: " + request.getFrom());
        }

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiator(user);

        transactionRepository.save(transaction);
    }


}
