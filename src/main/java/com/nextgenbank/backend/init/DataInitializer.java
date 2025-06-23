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

        // Alice (with transactions)
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

        // Bob (admin)
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

        // Charlie (with transactions)
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

        // New User (NO transactions)
        User newUser = new User();
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword(passwordEncoder.encode("newpass123"));
        newUser.setBsnNumber("111222333");
        newUser.setPhoneNumber("+31612345678");
        newUser.setRole(UserRole.CUSTOMER);
        newUser.setStatus(UserStatus.APPROVED);
        newUser.setCreatedAt(LocalDateTime.now());

        // Pending Users
        User penderson = new User();
        penderson.setFirstName("Penderson");
        penderson.setLastName("Pendington");
        penderson.setEmail("penderson@example.com");
        penderson.setPassword(passwordEncoder.encode("penderson123"));
        penderson.setBsnNumber("456123789");
        penderson.setPhoneNumber("+4561237890");
        penderson.setRole(UserRole.CUSTOMER);
        penderson.setStatus(UserStatus.PENDING);
        penderson.setCreatedAt(LocalDateTime.now());

        User kevin = new User();
        kevin.setFirstName("Kevin");
        kevin.setLastName("Doe");
        kevin.setEmail("kevin@example.com");
        kevin.setPassword(passwordEncoder.encode("kevin123"));
        kevin.setBsnNumber("789654123");
        kevin.setPhoneNumber("+7896541230");
        kevin.setRole(UserRole.CUSTOMER);
        kevin.setStatus(UserStatus.PENDING);
        kevin.setCreatedAt(LocalDateTime.now());

        userRepository.save(alice);
        userRepository.save(penderson);
        userRepository.save(kevin);
        userRepository.save(bob);
        userRepository.save(charlie);
        userRepository.save(newUser);

        // Alice's accounts
        Account aliceChecking = new Account();
        aliceChecking.setIBAN("NL12345678901234567890");
        aliceChecking.setCustomer(alice);
        aliceChecking.setAccountType(AccountType.CHECKING);
        aliceChecking.setBalance(new BigDecimal("1000.00"));
        aliceChecking.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        aliceChecking.setDailyTransferAmount(BigDecimal.ZERO);
        aliceChecking.setCreatedAt(LocalDateTime.now());
        aliceChecking.setCreatedBy(bob);

        Account aliceSavings = new Account();
        aliceSavings.setIBAN("NL09876543210987654321");
        aliceSavings.setCustomer(alice);
        aliceSavings.setAccountType(AccountType.SAVINGS);
        aliceSavings.setBalance(new BigDecimal("5000.00"));
        aliceSavings.setAbsoluteTransferLimit(new BigDecimal("10000.00"));
        aliceSavings.setDailyTransferAmount(BigDecimal.ZERO);
        aliceSavings.setCreatedAt(LocalDateTime.now());
        aliceSavings.setCreatedBy(bob);

        // Charlie's accounts
        Account charlieChecking = new Account();
        charlieChecking.setIBAN("NL22223333444455556666");
        charlieChecking.setCustomer(charlie);
        charlieChecking.setAccountType(AccountType.CHECKING);
        charlieChecking.setBalance(new BigDecimal("2000.00"));
        charlieChecking.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        charlieChecking.setDailyTransferAmount(BigDecimal.ZERO);
        charlieChecking.setCreatedAt(LocalDateTime.now());
        charlieChecking.setCreatedBy(bob);

        Account charlieSavings = new Account();
        charlieSavings.setIBAN("NL77778888999900001111");
        charlieSavings.setCustomer(charlie);
        charlieSavings.setAccountType(AccountType.SAVINGS);
        charlieSavings.setBalance(new BigDecimal("3000.00"));
        charlieSavings.setAbsoluteTransferLimit(new BigDecimal("10000.00"));
        charlieSavings.setDailyTransferAmount(BigDecimal.ZERO);
        charlieSavings.setCreatedAt(LocalDateTime.now());
        charlieSavings.setCreatedBy(bob);

        // New User's account (no transactions)
        Account newUserAccount = new Account();
        newUserAccount.setIBAN("NL00001111222233334444");
        newUserAccount.setCustomer(newUser);
        newUserAccount.setAccountType(AccountType.CHECKING);
        newUserAccount.setBalance(new BigDecimal("250.00"));
        newUserAccount.setAbsoluteTransferLimit(new BigDecimal("1000.00"));
        newUserAccount.setDailyTransferAmount(BigDecimal.ZERO);
        newUserAccount.setCreatedAt(LocalDateTime.now());
        newUserAccount.setCreatedBy(bob);

        accountRepository.save(aliceChecking);
        accountRepository.save(aliceSavings);
        accountRepository.save(charlieChecking);
        accountRepository.save(charlieSavings);
        accountRepository.save(newUserAccount);

        // Transactions (only for Alice and Charlie)
        Transaction txn1 = new Transaction();
        txn1.setFromAccount(null);
        txn1.setToAccount(aliceChecking);
        txn1.setAmount(new BigDecimal("1000.00"));
        txn1.setTimestamp(LocalDateTime.now());
        txn1.setInitiator(alice);
        txn1.setTransactionType(TransactionType.DEPOSIT);

        Transaction txn2 = new Transaction();
        txn2.setFromAccount(aliceChecking);
        txn2.setToAccount(aliceSavings);
        txn2.setAmount(new BigDecimal("500.00"));
        txn2.setTimestamp(LocalDateTime.now());
        txn2.setInitiator(alice);
        txn2.setTransactionType(TransactionType.TRANSFER);

        Transaction txn3 = new Transaction();
        txn3.setFromAccount(aliceChecking);
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

        // Dana (only incoming transactions)
        User dana = new User();
        dana.setFirstName("Dana");
        dana.setLastName("White");
        dana.setEmail("dana@example.com");
        dana.setPassword(passwordEncoder.encode("dana123"));
        dana.setBsnNumber("999888777");
        dana.setPhoneNumber("+31600000000");
        dana.setRole(UserRole.CUSTOMER);
        dana.setStatus(UserStatus.APPROVED);
        dana.setCreatedAt(LocalDateTime.now());
        userRepository.save(dana);

        // Dana's checking account
        Account danaAccount = new Account();
        danaAccount.setIBAN("NL44556677889900112233");
        danaAccount.setCustomer(dana);
        danaAccount.setAccountType(AccountType.CHECKING);
        danaAccount.setBalance(new BigDecimal("1000.00"));
        danaAccount.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        danaAccount.setDailyTransferAmount(BigDecimal.ZERO);
        danaAccount.setCreatedAt(LocalDateTime.now());
        danaAccount.setCreatedBy(bob);
        accountRepository.save(danaAccount);

        // One incoming transaction for Dana
        Transaction txnDana = new Transaction();
        txnDana.setFromAccount(aliceChecking);
        txnDana.setToAccount(danaAccount);
        txnDana.setAmount(new BigDecimal("250.00"));
        txnDana.setTimestamp(LocalDateTime.now());
        txnDana.setInitiator(alice);
        txnDana.setTransactionType(TransactionType.TRANSFER);
        transactionRepository.save(txnDana);

        System.out.println("Sample data initialized with new user (no transactions)");
    }
}