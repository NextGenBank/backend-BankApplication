package com.nextgenbank.backend.mapper;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;

public class TransactionMapper {

    public static TransactionResponseDto toResponseDto(Transaction txn, Long userId) {
        String fromIban = txn.getFromAccount() != null ? txn.getFromAccount().getIBAN() : "N/A";
        String toIban = txn.getToAccount() != null ? txn.getToAccount().getIBAN() : "N/A";

        String fromName = "Bank";
        if (txn.getFromAccount() != null && txn.getFromAccount().getCustomer() != null) {
            fromName = txn.getFromAccount().getCustomer().getFirstName() + " " +
                    txn.getFromAccount().getCustomer().getLastName();
        }

        String toName = "Unknown";
        if (txn.getToAccount() != null && txn.getToAccount().getCustomer() != null) {
            toName = txn.getToAccount().getCustomer().getFirstName() + " " +
                    txn.getToAccount().getCustomer().getLastName();
        }

        String direction = "UNKNOWN";

        Long fromUserId = (txn.getFromAccount() != null && txn.getFromAccount().getCustomer() != null)
                ? txn.getFromAccount().getCustomer().getUserId()
                : null;
        Long toUserId = (txn.getToAccount() != null && txn.getToAccount().getCustomer() != null)
                ? txn.getToAccount().getCustomer().getUserId()
                : null;

        if (fromUserId != null && fromUserId.equals(userId)) {
            direction = "OUTGOING";
        } else if (toUserId != null && toUserId.equals(userId)) {
            direction = "INCOMING";
        }

        if (fromUserId != null && toUserId != null && fromUserId.equals(userId) && toUserId.equals(userId)) {
            direction = "INTERNAL";
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
    }
}