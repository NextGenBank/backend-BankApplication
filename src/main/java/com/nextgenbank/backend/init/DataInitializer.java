package com.nextgenbank.backend.init;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
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
        alice.setPassword(passwordEncoder.encode("alice123"));
        alice.setBsnNumber("123456789");
        alice.setPhoneNumber("+1234567890");
        alice.setRole(UserRole.CUSTOMER);
        alice.setStatus(UserStatus.APPROVED);
        alice.setCreatedAt(LocalDateTime.now());

        User bob = new User();
        bob.setFirstName("Bob");
        bob.setLastName("Johnson");
        bob.setEmail("bob@example.com");
        bob.setPassword(passwordEncoder.encode("bob123"));
        bob.setBsnNumber("987654321");
        bob.setPhoneNumber("+0987654321");
        bob.setRole(UserRole.EMPLOYEE);
        bob.setStatus(UserStatus.APPROVED);
        bob.setCreatedAt(LocalDateTime.now());

        User charlie = new User();
        charlie.setFirstName("Charlie");
        charlie.setLastName("Brown");
        charlie.setEmail("charlie@example.com");
        charlie.setPassword(passwordEncoder.encode("charlie123"));
        charlie.setBsnNumber("555444333");
        charlie.setPhoneNumber("+3123456789");
        charlie.setRole(UserRole.CUSTOMER);
        charlie.setStatus(UserStatus.APPROVED);
        charlie.setCreatedAt(LocalDateTime.now());

        userRepository.save(alice);
        userRepository.save(bob);
        userRepository.save(charlie);

        // Create Accounts for Alice
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

        // Create Accounts for Charlie
        Account charlieChecking = new Account();
        charlieChecking.setIBAN("NL22223333444455556666");
        charlieChecking.setCustomer(charlie);
        charlieChecking.setAccountType(AccountType.CHECKING);
        charlieChecking.setBalance(new BigDecimal("2000.00"));
        charlieChecking.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        charlieChecking.setDailyTransferAmount(new BigDecimal("0.00"));
        charlieChecking.setCreatedAt(LocalDateTime.now());
        charlieChecking.setCreatedBy(bob);

        Account charlieSavings = new Account();
        charlieSavings.setIBAN("NL77778888999900001111");
        charlieSavings.setCustomer(charlie);
        charlieSavings.setAccountType(AccountType.SAVINGS);
        charlieSavings.setBalance(new BigDecimal("3000.00"));
        charlieSavings.setAbsoluteTransferLimit(new BigDecimal("10000.00"));
        charlieSavings.setDailyTransferAmount(new BigDecimal("0.00"));
        charlieSavings.setCreatedAt(LocalDateTime.now());
        charlieSavings.setCreatedBy(bob);

        accountRepository.save(aliceAccount);
        accountRepository.save(aliceSavings);
        accountRepository.save(charlieChecking);
        accountRepository.save(charlieSavings);

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

        Transaction txn3 = new Transaction();
        txn3.setFromAccount(aliceAccount);
        txn3.setToAccount(charlieChecking);
        txn3.setAmount(new BigDecimal("200.00"));
        txn3.setTimestamp(LocalDateTime.now());
        txn3.setInitiator(alice);
        txn3.setTransactionType(TransactionType.TRANSFER);

        Transaction txn4 = new Transaction();
        txn4.setFromAccount(charlieChecking);
        txn4.setToAccount(charlieSavings);
        txn4.setAmount(new BigDecimal("300.00"));
        txn4.setTimestamp(LocalDateTime.now());
        txn4.setInitiator(charlie);
        txn4.setTransactionType(TransactionType.TRANSFER);

        transactionRepository.save(txn1);
        transactionRepository.save(txn2);
        transactionRepository.save(txn3);
        transactionRepository.save(txn4);

        System.out.println("Sample data initialised.");
    }
}
