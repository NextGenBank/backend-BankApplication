package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsResponseDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
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

    // Default transfer limit if not set
    private static final BigDecimal DEFAULT_TRANSFER_LIMIT = new BigDecimal("1000.00");

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
            Pageable pageable) {

        validateDirectionParameter(direction);
        String normalizedAmountFilter = normalizeAmountFilter(amount, amountFilter);
        LocalDate parsedStartDate = safeParseDate(startDate);
        LocalDate parsedEndDate = safeParseDate(endDate);

        LocalDateTime startOfDay = parsedStartDate != null ? parsedStartDate.atStartOfDay() : null;
        LocalDateTime endOfDay = parsedEndDate != null ? parsedEndDate.atTime(23, 59, 59) : null;

        BigDecimal normalizedAmount = normalizeAmount(amount);

        if ((startDate != null && parsedStartDate == null) || (endDate != null && parsedEndDate == null)) {
            logger.warn("One of the date filters could not be parsed correctly. startDate={}, endDate={}", startDate, endDate);
            return Page.empty(pageable);
        }

        return transactionRepository.findAllByUserIdWithFilters(
                userId,
                iban,
                name,
                direction,
                startOfDay,
                endOfDay,
                normalizedAmount,
                normalizedAmountFilter,
                pageable
        );
    }

    private void validateDirectionParameter(String direction) {
        if (direction != null && !direction.isEmpty() && !isValidTransactionDirection(direction)) {
            throw new IllegalArgumentException("Invalid transaction direction: " + direction);
        }
    }

    private String normalizeAmountFilter(BigDecimal amount, String amountFilter) {
        if (amount == null) {
            return null;
        }

        AmountFilterOperation operation = AmountFilterOperation.fromString(amountFilter);
        return operation != null ? operation.getDbValue() : "eq";
    }

    private LocalDate safeParseDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse date: {}", dateString);
            return null;
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount.stripTrailingZeros() : null;
    }

    private boolean isValidTransactionDirection(String direction) {
        try {
            TransactionDirection.valueOf(direction.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private LocalDate parseDate(String dateStr) {
        return dateStr != null ? LocalDate.parse(dateStr) : null;
    }

    /**
     * Get all transactions with pagination
     */
    public Page<TransactionDto> getAllTransactionsPaginated(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAllByOrderByTimestampDesc(pageable);
        return transactions.map(TransactionDto::new);
    }

    /**
     * Get all transactions (non-paginated, for backward compatibility)
     */
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(TransactionDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions for a specific customer with pagination
     */
    public Page<TransactionDto> getTransactionsByCustomerIdPaginated(Long customerId, Pageable pageable) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Page<Transaction> transactionsPage = transactionRepository
                .findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(
                        customer, customer, pageable);

        return transactionsPage.map(TransactionDto::new);
    }

    /**
     * Get transactions for a specific customer (non-paginated, for backward compatibility)
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
     * Process a transfer specifically initiated by an employee
     * Adds additional security checks
     */
    @org.springframework.transaction.annotation.Transactional
    public TransactionDto processEmployeeTransfer(TransferRequestDto transferRequest) {
        // Verify the initiator is an employee
        User initiator = userRepository.findById(transferRequest.getInitiatorId())
                .orElseThrow(() -> new RuntimeException("Initiator not found"));

        if (initiator.getRole() != UserRole.EMPLOYEE) {
            logger.warn("Security violation: Non-employee {} attempted to use employee transfer endpoint",
                    initiator.getEmail());
            throw new SecurityException("Only employees can initiate transfers through this endpoint");
        }

        return transferFunds(transferRequest);
    }

    /**
     * Process a transfer between two accounts
     * This is the core transfer logic used by both customer and employee-initiated transfers
     */
    @org.springframework.transaction.annotation.Transactional
    public TransactionDto transferFunds(TransferRequestDto transferRequest) {
        logger.info("Processing transfer request: {}", transferRequest);

        // Get the source account
        Account sourceAccount = accountRepository.findById(transferRequest.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        logger.info("Source account: {} (Type: {}, Balance: {}, Limit: {})",
                sourceAccount.getIBAN(), sourceAccount.getAccountType(),
                sourceAccount.getBalance(), sourceAccount.getAbsoluteTransferLimit());

        // Get the destination account
        Account destinationAccount = accountRepository.findById(transferRequest.getToAccount())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));
        logger.info("Destination account: {} (Type: {}, Balance: {})",
                destinationAccount.getIBAN(), destinationAccount.getAccountType(),
                destinationAccount.getBalance());

        // Get the initiator
        User initiator = userRepository.findById(transferRequest.getInitiatorId())
                .orElseThrow(() -> new IllegalArgumentException("Initiator not found"));
        logger.info("Initiator: {} (Role: {})", initiator.getEmail(), initiator.getRole());

        BigDecimal transferAmount = transferRequest.getAmount();
        logger.info("Transfer amount: {}", transferAmount);

        // Validate the transfer
        validateTransfer(sourceAccount, destinationAccount, transferAmount);

        // Update account balances
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(transferAmount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(transferAmount));

        // Save updated account balances
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        // Create and save the transaction with description
        Transaction transaction = new Transaction();
        transaction.setFromAccount(sourceAccount);
        transaction.setToAccount(destinationAccount);
        transaction.setAmount(transferAmount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiator(initiator);
        transaction.setTransactionType(TransactionType.TRANSFER);
        // Add description if provided
        if (transferRequest.getDescription() != null && !transferRequest.getDescription().trim().isEmpty()) {
            // Add description handling logic here if needed
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        return new TransactionDto(savedTransaction);
    }

    /**
     * Validate a transfer to ensure it meets all requirements
     */
    private void validateTransfer(Account sourceAccount, Account destinationAccount, BigDecimal amount) {
        logger.info("Validating transfer: from={}, to={}, amount={}",
                sourceAccount.getIBAN(), destinationAccount.getIBAN(), amount);

        // Check if accounts are different
        if (sourceAccount.getIBAN().equals(destinationAccount.getIBAN())) {
            logger.warn("Transfer rejected: Cannot transfer to the same account");
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Check if the amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Transfer rejected: Amount must be positive, got {}", amount);
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Check if the source account has sufficient funds
        BigDecimal balanceAfterTransfer = sourceAccount.getBalance().subtract(amount);
        logger.info("Current balance: {}, Amount: {}, Balance after transfer would be: {}",
                sourceAccount.getBalance(), amount, balanceAfterTransfer);

        if (balanceAfterTransfer.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Transfer rejected: Insufficient funds. Balance: {}, Amount: {}",
                    sourceAccount.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Check if the transfer would exceed the absolute transfer limit
        BigDecimal transferLimit = sourceAccount.getAbsoluteTransferLimit() != null ?
                sourceAccount.getAbsoluteTransferLimit() : DEFAULT_TRANSFER_LIMIT;

        // The absolute transfer limit is the MAXIMUM amount that can be transferred daily
        BigDecimal dailyTransferAmount = sourceAccount.getDailyTransferAmount() != null ?
                sourceAccount.getDailyTransferAmount() : BigDecimal.ZERO;

        // Calculate new daily transfer amount if this transfer goes through
        BigDecimal newDailyTransferAmount = dailyTransferAmount.add(amount);

        logger.info("Checking transfer limit: Current daily transfer: {}, This transfer: {}, " +
                     "Total would be: {}, Daily limit: {}",
                dailyTransferAmount, amount, newDailyTransferAmount, transferLimit);

        // Only fail if this transfer would exceed the daily limit
        if (newDailyTransferAmount.compareTo(transferLimit) > 0) {
            logger.warn("Transfer rejected: Would exceed daily transfer limit. " +
                        "Current daily amount: {}, This transfer: {}, Daily limit: {}",
                    dailyTransferAmount, amount, transferLimit);

            // Calculate remaining amount allowed today
            BigDecimal remainingAllowed = transferLimit.subtract(dailyTransferAmount);
            if (remainingAllowed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Daily transfer limit reached. No more transfers allowed today.");
            } else {
                throw new IllegalArgumentException("Transfer would exceed daily limit. Maximum transfer allowed today: " +
                        remainingAllowed);
            }
        }

        // Update the daily transfer amount for this account
        sourceAccount.setDailyTransferAmount(newDailyTransferAmount);
    }

    /**
     * Update an account's transfer limit
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateTransferLimit(String accountIban, BigDecimal newLimit) {
        logger.info("Updating transfer limit for account {}: new limit = {}", accountIban, newLimit);

        if (newLimit == null || newLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transfer limit must be a positive number or zero");
        }

        Account account = accountRepository.findById(accountIban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountIban));

        account.setAbsoluteTransferLimit(newLimit);
        accountRepository.save(account);

        logger.info("Transfer limit updated for account {}", accountIban);
    }

    /**
     * Switches funds between a user's checking and savings account.
     * Only customers are allowed to perform this operation.
     */
    @Transactional
    public SwitchFundsResponseDto switchFunds(User user, SwitchFundsRequestDto request) {
        validateCustomer(user);
        validateAmount(request.getAmount());

        Account checking = getAccount(user, AccountType.CHECKING);
        Account savings  = getAccount(user, AccountType.SAVINGS);

        Account from = getSourceAccount(request.getFrom(), checking, savings);
        Account to   = (from == checking) ? savings : checking;

        ensureSufficientFunds(from, request.getAmount(), request.getFrom());

        transferFunds(from, to, request.getAmount());
        logTransaction(user, from, to, request.getAmount());

        return new SwitchFundsResponseDto(checking.getBalance(), savings.getBalance());
    }

    private void validateCustomer(User user) {
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customers can switch funds.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }
    }

    private Account getAccount(User user, AccountType type) {
        return accountRepository
                .findByCustomerUserIdAndAccountType(user.getUserId(), type)
                .orElseThrow(() -> new IllegalStateException(type + " account not found for user."));
    }

    private Account getSourceAccount(String fromType, Account checking, Account savings) {
        return switch (fromType.toUpperCase()) {
            case "CHECKING" -> checking;
            case "SAVINGS" -> savings;
            default -> throw new IllegalArgumentException("Invalid source account: " + fromType);
        };
    }

    private void ensureSufficientFunds(Account from, BigDecimal amount, String sourceType) {
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance in the " + sourceType.toLowerCase() + " account.");
        }
    }

    private void transferFunds(Account from, Account to, BigDecimal amount) {
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);
    }

    private void logTransaction(User user, Account from, Account to, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(user);
        transactionRepository.save(tx);
    }

    /**
     * Get pending transactions in the system with pagination
     */
    public Page<TransactionDto> getPendingTransactionsPaginated(Pageable pageable) {
        // Currently just using transactions with amount = 0 as a proxy for pending
        // This should be replaced with actual pending transaction logic
        List<TransactionDto> pendingTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getAmount().compareTo(BigDecimal.ZERO) == 0)
                .map(TransactionDto::new)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), pendingTransactions.size());

        List<TransactionDto> pageContent = pendingTransactions.subList(start, end);
        return new PageImpl<>(pageContent, pageable, pendingTransactions.size());
    }

    /**
     * Get pending transactions (non-paginated, for backward compatibility)
     */
    public List<TransactionDto> getPendingTransactions() {
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