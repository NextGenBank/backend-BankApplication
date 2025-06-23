package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsResponseDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.specification.TransactionSpecification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.nextgenbank.backend.mapper.TransactionMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public Page<Transaction> getFilteredTransactionsForUser(
            Long userId,
            String iban,
            String name,
            String direction,
            String startDate,
            String endDate,
            BigDecimal amount,
            String amountFilter,
            Pageable pageable
    ) {
        return transactionRepository.findAllByUserIdWithFilters(
                userId, iban, name, direction, startDate, endDate, amount, amountFilter, pageable
        );
    }

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
    @org.springframework.transaction.annotation.Transactional
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

        // Check if the transfer would exceed the absolute transfer limit
        if (fromAccount.getAbsoluteTransferLimit() != null) {
            // The absolute transfer limit is the MAXIMUM amount that can be transferred daily
            BigDecimal dailyTransferAmount = fromAccount.getDailyTransferAmount() != null ? 
                    fromAccount.getDailyTransferAmount() : BigDecimal.ZERO;
            
            // Calculate new daily transfer amount if this transfer goes through
            BigDecimal newDailyTransferAmount = dailyTransferAmount.add(amount);
            
            logger.info("Checking transfer limit: Current daily transfer: {}, This transfer: {}, " +
                         "Total would be: {}, Daily limit: {}",
                    dailyTransferAmount, amount, newDailyTransferAmount,
                    fromAccount.getAbsoluteTransferLimit());
            
            // Only fail if this transfer would exceed the daily limit
            if (newDailyTransferAmount.compareTo(fromAccount.getAbsoluteTransferLimit()) > 0) {
                logger.warn("Transfer rejected: Would exceed daily transfer limit. " +
                            "Current daily amount: {}, This transfer: {}, Daily limit: {}",
                        dailyTransferAmount, amount, fromAccount.getAbsoluteTransferLimit());
                
                // Calculate remaining amount allowed today
                BigDecimal remainingAllowed = fromAccount.getAbsoluteTransferLimit().subtract(dailyTransferAmount);
                if (remainingAllowed.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Daily transfer limit reached. No more transfers allowed today.");
                } else {
                    throw new RuntimeException("Transfer would exceed daily limit. Maximum transfer allowed today: " + 
                            remainingAllowed);
                }
            }
            
            // Update the daily transfer amount for this account
            fromAccount.setDailyTransferAmount(newDailyTransferAmount);
        }
    }

    /**
     * Switches funds between a user's checking and savings account.
     * Only customers are allowed to perform this operation.
     */
    @Transactional
    public SwitchFundsResponseDto switchFunds(User user, SwitchFundsRequestDto request) {
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customers can switch funds.");
        }

        Account checking = accountRepository
                .findByCustomerUserIdAndAccountType(user.getUserId(), AccountType.CHECKING)
                .orElseThrow(() -> new IllegalStateException("Checking account not found for user."));

        Account savings = accountRepository
                .findByCustomerUserIdAndAccountType(user.getUserId(), AccountType.SAVINGS)
                .orElseThrow(() -> new IllegalStateException("Savings account not found for user."));

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

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance in the " + request.getFrom().toLowerCase() + " account.");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        accountRepository.save(from);
        accountRepository.save(to);

        // Log the transaction
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

    /**
     * Get pending transactions in the system
     * For the dashboard metrics
     */
    public List<TransactionDto> getPendingTransactions() {
        // Currently just using transactions with amount = 0 as a proxy for pending
        // This should be replaced with actual pending transaction logic
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getAmount().compareTo(BigDecimal.ZERO) == 0)
                .map(TransactionDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public Transaction performAtmOperation(User initiator, String iban, BigDecimal amount, TransactionType type) {
        if (type != TransactionType.DEPOSIT && type != TransactionType.WITHDRAWAL) {
            throw new IllegalArgumentException("Invalid transaction type for ATM operation.");
        }

        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + iban));

        if (!account.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to this account.");
        }

        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setTransactionType(type);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);

        if (type == TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance().add(amount));
            tx.setToAccount(account);
        } else { // WITHDRAWAL
            if (account.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient funds.");
            }
            account.setBalance(account.getBalance().subtract(amount));
            tx.setFromAccount(account);
        }

        accountRepository.save(account);
        return transactionRepository.save(tx);
    }
}