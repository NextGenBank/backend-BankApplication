package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionResponseDto> getTransactionsForUser(User user, String iban, String name, String type, String sort) {
        Long userId = user.getUserId();

        List<TransactionResponseDto> transactions = transactionRepository.findAll().stream()
                .filter(txn -> {
                    Account from = txn.getFromAccount();
                    Account to = txn.getToAccount();
                    return (from != null && from.getCustomer().getUserId().equals(userId)) ||
                            (to != null && to.getCustomer().getUserId().equals(userId));
                })
                .map(txn -> {
                    String fromIban = "N/A";
                    String fromName = "Bank";
                    if (txn.getFromAccount() != null) {
                        fromIban = txn.getFromAccount().getIBAN();
                        User sender = txn.getFromAccount().getCustomer();
                        fromName = sender.getFirstName() + " " + sender.getLastName();
                    }

                    String toIban = "N/A";
                    String toName = "Unknown";
                    if (txn.getToAccount() != null) {
                        toIban = txn.getToAccount().getIBAN();
                        User receiver = txn.getToAccount().getCustomer();
                        toName = receiver.getFirstName() + " " + receiver.getLastName();
                    }

                    boolean isSender = txn.getFromAccount() != null &&
                            txn.getFromAccount().getCustomer().getUserId().equals(userId);
                    boolean isReceiver = txn.getToAccount() != null &&
                            txn.getToAccount().getCustomer().getUserId().equals(userId);

                    String direction = isSender && isReceiver ? "INTERNAL"
                            : isSender ? "OUTGOING" : "INCOMING";

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
                .filter(dto -> {
                    if (iban != null && !iban.isBlank()) {
                        return dto.fromIban().equalsIgnoreCase(iban) || dto.toIban().equalsIgnoreCase(iban);
                    }
                    return true;
                })
                .filter(dto -> {
                    if (name != null && !name.isBlank()) {
                        String fullName = (dto.fromName() + " " + dto.toName()).toLowerCase();
                        return fullName.contains(name.toLowerCase());
                    }
                    return true;
                })
                .filter(dto -> {
                    if (type != null && !type.isBlank()) {
                        return dto.direction().equalsIgnoreCase(type);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Sorting logic
        if (sort != null && !sort.isBlank()) {
            switch (sort.toLowerCase()) {
                case "recent" -> transactions.sort(Comparator.comparing(TransactionResponseDto::timestamp).reversed());
                case "amount" -> transactions.sort(Comparator.comparing(TransactionResponseDto::amount).reversed());
                case "type" -> transactions.sort(Comparator.comparing(t -> t.transactionType().name()));
            }
        }

        return transactions;
    }
}
