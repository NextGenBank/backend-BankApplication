package com.nextgenbank.backend.mapper;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;

public class TransactionMapper {

    public static TransactionResponseDto toResponseDto(Transaction txn, Long userId) {
        String fromIban = getIban(txn.getFromAccount());
        String toIban = getIban(txn.getToAccount());

        String fromName = getFullNameOrDefault(txn, true, "Bank");
        String toName = getFullNameOrDefault(txn, false, "Unknown");

        String direction = determineDirection(txn, userId);

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

    private static String getIban(com.nextgenbank.backend.model.Account account) {
        return (account != null) ? account.getIBAN() : "N/A";
    }

    private static String getFullNameOrDefault(Transaction txn, boolean isFrom, String defaultName) {
        var account = isFrom ? txn.getFromAccount() : txn.getToAccount();
        if (account != null && account.getCustomer() != null) {
            return account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName();
        }
        return defaultName;
    }

    private static String determineDirection(Transaction txn, Long userId) {
        Long fromUserId = getUserId(txn.getFromAccount());
        Long toUserId = getUserId(txn.getToAccount());

        if (fromUserId != null && toUserId != null && fromUserId.equals(userId) && toUserId.equals(userId)) {
            return "INTERNAL";
        } else if (fromUserId != null && fromUserId.equals(userId)) {
            return "OUTGOING";
        } else if (toUserId != null && toUserId.equals(userId)) {
            return "INCOMING";
        } else {
            return "UNKNOWN";
        }
    }

    private static Long getUserId(com.nextgenbank.backend.model.Account account) {
        return (account != null && account.getCustomer() != null)
                ? account.getCustomer().getUserId()
                : null;
    }
}
