package com.nextgenbank.backend.init;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DataInitializer(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create Users
        User alice = new User();
        alice.setFirstName("Alice");
        alice.setLastName("Smith");
        alice.setEmail("alice@example.com");
        // !!!!!!!!!
        alice.setPassword("alice123");  // Make sure to change this once password hash is implemented
        // !!!!!!!!!
        alice.setBsnNumber("123456789");
        alice.setPhoneNumber("+1234567890");
        alice.setRole(UserRole.CUSTOMER);
        alice.setStatus(UserStatus.APPROVED);
        alice.setCreatedAt(LocalDateTime.now());

        User bob = new User();
        bob.setFirstName("Bob");
        bob.setLastName("Johnson");
        bob.setEmail("bob@example.com");
        // !!!!!!!!!
        bob.setPassword("bob123"); // Make sure to change this once password hash is implemented
        // !!!!!!!!!
        bob.setBsnNumber("987654321");
        bob.setPhoneNumber("+0987654321");
        bob.setRole(UserRole.EMPLOYEE);
        bob.setStatus(UserStatus.APPROVED);
        bob.setCreatedAt(LocalDateTime.now());

        userRepository.save(alice);
        userRepository.save(bob);

        // Create Accounts
        Account aliceAccount = new Account();
        aliceAccount.setIBAN("NL12345678901234567890");
        aliceAccount.setCustomer(alice);
        aliceAccount.setAccountType(AccountType.CHECKING);
        aliceAccount.setBalance(new BigDecimal("1000.00"));
        aliceAccount.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        aliceAccount.setDailyTransferAmount(new BigDecimal("0.00"));
        aliceAccount.setCreatedAt(LocalDateTime.now());
        aliceAccount.setCreatedBy(bob);

        Account aliceSavings = new Account();
        aliceSavings.setIBAN("NL09876543210987654321");
        aliceSavings.setCustomer(alice);
        aliceSavings.setAccountType(AccountType.SAVINGS);
        aliceSavings.setBalance(new BigDecimal("5000.00"));
        aliceSavings.setAbsoluteTransferLimit(new BigDecimal("10000.00"));
        aliceSavings.setDailyTransferAmount(new BigDecimal("0.00"));
        aliceSavings.setCreatedAt(LocalDateTime.now());
        aliceSavings.setCreatedBy(bob);

        accountRepository.save(aliceAccount);
        accountRepository.save(aliceSavings);

        // Create Transactions
        Transaction txn1 = new Transaction();
        txn1.setFromAccount(null);  // deposit, so no from account
        txn1.setToAccount(aliceAccount);
        txn1.setAmount(new BigDecimal("1000.00"));
        txn1.setTimestamp(LocalDateTime.now());
        txn1.setInitiator(alice);
        txn1.setTransactionType(TransactionType.DEPOSIT);

        Transaction txn2 = new Transaction();
        txn2.setFromAccount(aliceAccount);
        txn2.setToAccount(aliceSavings);
        txn2.setAmount(new BigDecimal("500.00"));
        txn2.setTimestamp(LocalDateTime.now());
        txn2.setInitiator(alice);
        txn2.setTransactionType(TransactionType.TRANSFER);

        transactionRepository.save(txn1);
        transactionRepository.save(txn2);

        System.out.println("Sample data initialised.");
    }
}