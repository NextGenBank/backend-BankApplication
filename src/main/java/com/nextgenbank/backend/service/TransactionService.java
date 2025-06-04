package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

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

}
