package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsResponseDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;


    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
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

    @Transactional
    public SwitchFundsResponseDto switchFunds(User user, SwitchFundsRequestDto request){

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customers can switch funds.");
        }

        Account checking = accountRepository
                .findByCustomerUserIdAndAccountType(user.getUserId(), AccountType.CHECKING)
                .orElseThrow(() -> new IllegalStateException("Checking account not found"));

        Account savings = accountRepository
                .findByCustomerUserIdAndAccountType(user.getUserId(), AccountType.SAVINGS)
                .orElseThrow(() -> new IllegalStateException("Savings account not found"));


        Account from, to;

        if ("CHECKING".equalsIgnoreCase(request.getFrom())) {
            from = checking;
            to = savings;
        } else if ("SAVINGS".equalsIgnoreCase(request.getFrom())) {
            from = savings;
            to = checking;
        } else {
            throw new IllegalArgumentException("Invalid source account: " + request.getFrom());
        }

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        accountRepository.save(from);
        accountRepository.save(to);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiator(user);

        transactionRepository.save(transaction);

        return new SwitchFundsResponseDto(checking.getBalance(), savings.getBalance());
    }
}