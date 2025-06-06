// src/main/java/com/nextgenbank/backend/service/TransactionService.java
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

        return transactionRepository.findAllByUserId(userId).stream()
                .map(txn -> {
                    // --- Собираем fromIban и fromName ---
                    String fromIban = "N/A";
                    String fromName = "Bank";
                    if (txn.getFromAccount() != null) {
                        fromIban = txn.getFromAccount().getIBAN();
                        fromName = txn.getFromAccount().getCustomer().getFirstName() + " " +
                                txn.getFromAccount().getCustomer().getLastName();
                    }

                    // --- Собираем toIban и toName ---
                    String toIban = "N/A";
                    String toName = "Unknown";
                    if (txn.getToAccount() != null) {
                        toIban = txn.getToAccount().getIBAN();
                        toName = txn.getToAccount().getCustomer().getFirstName() + " " +
                                txn.getToAccount().getCustomer().getLastName();
                    }

                    // --- Определяем направление (direction) ---
                    boolean isSender   = txn.getFromAccount() != null &&
                            txn.getFromAccount().getCustomer().getUserId().equals(userId);
                    boolean isReceiver = txn.getToAccount()   != null &&
                            txn.getToAccount().getCustomer().getUserId().equals(userId);

                    String direction;
                    if (isSender && isReceiver) {
                        direction = "INTERNAL";
                    } else if (isSender) {
                        direction = "OUTGOING";
                    } else {
                        direction = "INCOMING";
                    }

                    return new TransactionResponseDto(
                            txn.getTransactionId(),
                            txn.getTransactionType(),
                            txn.getAmount(),
                            txn.getTimestamp(),
                            fromIban,
                            fromName,
                            toIban,
                            toName,
                            direction
                    );
                })
                .collect(Collectors.toList());
    }
}
