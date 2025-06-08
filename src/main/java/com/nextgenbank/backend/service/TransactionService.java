package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public List<TransactionResponseDto> getTransactionsForUser(User user) {
        Long userId = user.getUserId();

        return transactionRepository.findAll().stream()
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

    /**
     * Get all transactions in the system
     */
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(TransactionDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions for a specific customer
     */
    public List<TransactionDto> getTransactionsByCustomerId(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return transactionRepository.findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(customer, customer)
                .stream()
                .map(TransactionDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Process a transfer between two accounts
     */
    @Transactional
    public TransactionDto transferFunds(TransferRequestDto transferRequestDto) {
        logger.info("Processing transfer request: {}", transferRequestDto);

        // Get the source account
        Account fromAccount = accountRepository.findById(transferRequestDto.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Source account not found"));
        logger.info("From account: {} (Type: {}, Balance: {}, Limit: {})",
                fromAccount.getIBAN(), fromAccount.getAccountType(),
                fromAccount.getBalance(), fromAccount.getAbsoluteTransferLimit());

        // Get the destination account
        Account toAccount = accountRepository.findById(transferRequestDto.getToAccount())
                .orElseThrow(() -> new RuntimeException("Destination account not found"));
        logger.info("To account: {} (Type: {}, Balance: {})",
                toAccount.getIBAN(), toAccount.getAccountType(), toAccount.getBalance());

        // Get the initiator
        User initiator = userRepository.findById(transferRequestDto.getInitiatorId())
                .orElseThrow(() -> new RuntimeException("Initiator not found"));
        logger.info("Initiator: {} (Role: {})", initiator.getEmail(), initiator.getRole());

        BigDecimal amount = transferRequestDto.getAmount();
        logger.info("Transfer amount: {}", amount);

        // Validate the transfer
        validateTransfer(fromAccount, toAccount, amount);

        // Update account balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // Save updated account balances
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Create and save the transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiator(initiator);
        transaction.setTransactionType(TransactionType.TRANSFER);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return new TransactionDto(savedTransaction);
    }

    /**
     * Validate a transfer to ensure it meets all requirements
     */
    private void validateTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        logger.info("Validating transfer: from={}, to={}, amount={}",
                fromAccount.getIBAN(), toAccount.getIBAN(), amount);

        // Check if accounts are different
        if (fromAccount.getIBAN().equals(toAccount.getIBAN())) {
            logger.warn("Transfer rejected: Cannot transfer to the same account");
            throw new RuntimeException("Cannot transfer to the same account");
        }

        // Check if the amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Transfer rejected: Amount must be positive, got {}", amount);
            throw new RuntimeException("Transfer amount must be positive");
        }

        // Check if the source account has sufficient funds
        BigDecimal balanceAfterTransfer = fromAccount.getBalance().subtract(amount);
        logger.info("Current balance: {}, Amount: {}, Balance after transfer would be: {}",
                fromAccount.getBalance(), amount, balanceAfterTransfer);

        if (balanceAfterTransfer.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Transfer rejected: Insufficient funds. Balance: {}, Amount: {}",
                    fromAccount.getBalance(), amount);
            throw new RuntimeException("Insufficient funds");
        }

        // Check if the transfer would go below absolute transfer limit
        if (fromAccount.getAbsoluteTransferLimit() != null) {
            // The absolute transfer limit is the MINIMUM balance allowed after transfer
            logger.info("Checking transfer limit: Current balance: {}, Transfer amount: {}, " +
                            "Balance after transfer: {}, Absolute limit: {}",
                    fromAccount.getBalance(), amount, balanceAfterTransfer,
                    fromAccount.getAbsoluteTransferLimit());

            // Only fail if balance after transfer would be BELOW the limit
            if (balanceAfterTransfer.compareTo(fromAccount.getAbsoluteTransferLimit()) < 0) {
                logger.warn("Transfer rejected: Would go below transfer limit. " +
                                "Balance after transfer: {}, Limit: {}",
                        balanceAfterTransfer, fromAccount.getAbsoluteTransferLimit());
                throw new RuntimeException("Transfer would go below the absolute transfer limit of " +
                        fromAccount.getAbsoluteTransferLimit());
            }
        }
    }
}
