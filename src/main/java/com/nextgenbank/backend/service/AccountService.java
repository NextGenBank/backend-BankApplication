package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.dto.AccountLookupDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get all accounts for a customer
     */
    public List<Account> getAccountsByCustomerId(Long customerId) {
        System.out.println("Getting accounts for customer ID: " + customerId);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<Account> accounts = accountRepository.findByCustomer(customer);

        System.out.println("Found " + accounts.size() + " accounts for customer: " + customer.getFirstName());

        // Log each account
        for (Account account : accounts) {
            System.out.println("Account IBAN: " + account.getIBAN() + ", Type: " + account.getAccountType());
        }

        return accounts;
    }

    /**
     * Get account by IBAN
     */
    public Account getAccountByIban(String iban) {
        return accountRepository.findById(iban)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    /**
     * Get all accounts
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Update the absolute transfer limit for an account
     * Validates that the limit is a positive number
     */
    @Transactional
    public Account updateAbsoluteTransferLimit(String iban, BigDecimal absoluteLimit) {
        System.out.println("Updating absolute transfer limit for IBAN: " + iban + ", new limit: " + absoluteLimit);

        if (iban == null || iban.isEmpty()) {
            throw new IllegalArgumentException("IBAN cannot be null or empty");
        }
        
        // Validate transfer limit
        if (absoluteLimit == null) {
            throw new IllegalArgumentException("Transfer limit cannot be null");
        }
        
        if (absoluteLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transfer limit must be a positive number");
        }

        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with IBAN: " + iban));

        account.setAbsoluteTransferLimit(absoluteLimit);
        Account savedAccount = accountRepository.save(account);

        System.out.println("Updated absolute transfer limit successfully for IBAN: " + iban);
        return savedAccount;
    }

    /**
     * Generates a unique NL IBAN by combining "NL" with a random number.
     * Repeats generation if collision is detected in the database.
     */
    private String generateUniqueIBAN() {
        String iban;
        int maxAttempts = 1000;
        int attempt = 0;

        do {
            if (++attempt > maxAttempts) {
                throw new IllegalStateException("Failed to generate a unique IBAN after multiple attempts.");
            }

            // NL + 2-digit + 18-digit = 22-char IBAN
            iban = "NL" + (int) (Math.random() * 100)
                    + String.format("%018d", (long) (Math.random() * 1_000_000_000_000_000_000L));
        } while (accountRepository.existsById(iban)); // ensure uniqueness

        return iban;
    }

    /**
     * Creates a checking and savings account for a given user.
     * Rolls back transaction if any error occurs.
     */
    @Transactional
    public void createAccountsForUser(User user, User employee) {
        if (employee.getRole() != UserRole.EMPLOYEE) {
            throw new IllegalArgumentException("Only employees can create accounts for users.");
        }

        try {
            Account checking = createAccount(user, employee, AccountType.CHECKING);
            Account savings = createAccount(user, employee, AccountType.SAVINGS);

            accountRepository.save(checking);
            accountRepository.save(savings);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create accounts for user: " + user.getUserId(), e);
        }
    }

    private Account createAccount(User customer, User employee, AccountType type) {
        Account account = new Account();
        account.setIBAN(generateUniqueIBAN());
        account.setCustomer(customer);
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setAbsoluteTransferLimit(new BigDecimal("5000"));
        account.setDailyTransferAmount(BigDecimal.ZERO);
        account.setCreatedBy(employee);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    private AccountLookupDto convertUserToAccountLookupDto(User user, String ibanFilter) {
        List<String> filteredIbans = user.getAccountsOwned().stream()
                .map(Account::getIBAN)
                .filter(accountIban -> ibanFilter == null || ibanFilter.isBlank() ||
                        accountIban.toLowerCase().contains(ibanFilter.toLowerCase()))
                .collect(Collectors.toList());

        return new AccountLookupDto(
                user.getFirstName(),
                user.getLastName(),
                filteredIbans
        );
    }

    public Page<AccountLookupDto> lookupAccounts(String name, String iban, Pageable pageable) {
        Page<User> usersPage = userRepository.findApprovedUsersWithAccounts(name, iban, pageable);

        List<AccountLookupDto> dtoList = usersPage.getContent().stream()
                .map(user -> convertUserToAccountLookupDto(user, iban))
                .filter(dto -> !dto.getIbans().isEmpty())
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, usersPage.getTotalElements());
    }

    public Page<AccountLookupDto> getAllUsersWithIbans(Pageable pageable) {
        Page<User> usersPage = userRepository.findApprovedUsersWithAccounts(null, null, pageable);

        List<AccountLookupDto> dtos = usersPage.getContent().stream()
                .map(user -> convertUserToAccountLookupDto(user, null))
                .toList();

        return new PageImpl<>(dtos, pageable, usersPage.getTotalElements());
    }

    public List<Account> getAccountsForUser(User user) {
        return accountRepository.findByCustomer(user);
    }
}