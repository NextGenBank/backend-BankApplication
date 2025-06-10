package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

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
     */
    @Transactional
    public Account updateAbsoluteTransferLimit(String iban, BigDecimal absoluteLimit) {
        System.out.println("Updating absolute transfer limit for IBAN: " + iban + ", new limit: " + absoluteLimit);

        if (iban == null || iban.isEmpty()) {
            throw new IllegalArgumentException("IBAN cannot be null or empty");
        }

        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + iban));

        account.setAbsoluteTransferLimit(absoluteLimit);
        Account savedAccount = accountRepository.save(account);

        System.out.println("Updated absolute transfer limit successfully for IBAN: " + iban);
        return savedAccount;
    }

    private String generateUniqueIBAN() {
        String iban;
        do {
            // NL IBAN pattern
            iban = "NL" + (int)(Math.random() * 100) + String.format("%018d", (long)(Math.random() * 1_000_000_000_000_000_000L));
        } while (accountRepository.existsById(iban)); // Make sure it's unique
        return iban;
    }

    @Transactional
    public void createAccountsForUser(User user) {
        Account checking = new Account();
        checking.setIBAN(generateUniqueIBAN());
        checking.setCustomer(user);
        checking.setAccountType(AccountType.CHECKING);
        checking.setBalance(BigDecimal.ZERO);
        checking.setAbsoluteTransferLimit(new BigDecimal("5000"));

        Account savings = new Account();
        savings.setIBAN(generateUniqueIBAN());
        savings.setCustomer(user);
        savings.setAccountType(AccountType.SAVINGS);
        savings.setBalance(BigDecimal.ZERO);
        savings.setAbsoluteTransferLimit(new BigDecimal("5000"));

        accountRepository.save(checking);
        accountRepository.save(savings);
    }
}