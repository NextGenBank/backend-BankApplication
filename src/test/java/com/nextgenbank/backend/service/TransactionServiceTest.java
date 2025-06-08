package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        transactionService = new TransactionService(transactionRepository);
    }

    @Test
    void getTransactionsForUser_shouldReturnAllMatchingTransactions() {
        User user = createUser(1L, "Alice", "Smith");
        Account fromAccount = createAccount("IBAN1", user);
        Account toAccount = createAccount("IBAN2", user);

        Transaction txn = createTransaction(1L, fromAccount, toAccount, new BigDecimal("100.00"), LocalDateTime.now());
        when(transactionRepository.findAll()).thenReturn(List.of(txn));

        List<TransactionResponseDto> result = transactionService.getTransactionsForUser(user, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("INTERNAL", result.get(0).direction());
    }

    @Test
    void getTransactionsForUser_shouldFilterByIban() {
        User user = createUser(1L, "Alice", "Smith");
        Account fromAccount = createAccount("FILTER-MATCH", user);
        Account toAccount = createAccount("OTHER", user);

        Transaction txn = createTransaction(2L, fromAccount, toAccount, new BigDecimal("50.00"), LocalDateTime.now());
        when(transactionRepository.findAll()).thenReturn(List.of(txn));

        List<TransactionResponseDto> result = transactionService.getTransactionsForUser(user, "FILTER-MATCH", null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void getTransactionsForUser_shouldFilterByName() {
        User alice = createUser(1L, "Alice", "Smith");
        User bob = createUser(2L, "Bob", "Brown");
        Account from = createAccount("FROM", alice);
        Account to = createAccount("TO", bob);

        Transaction txn = createTransaction(3L, from, to, new BigDecimal("75.00"), LocalDateTime.now());
        when(transactionRepository.findAll()).thenReturn(List.of(txn));

        List<TransactionResponseDto> result = transactionService.getTransactionsForUser(alice, null, "bob", null, null);

        assertEquals(1, result.size());
        assertEquals("Bob Brown", result.get(0).toName());
    }

    @Test
    void getTransactionsForUser_shouldFilterByType() {
        User user = createUser(1L, "Alice", "Smith");
        User receiver = createUser(2L, "John", "Doe");
        Account from = createAccount("FROM", user);
        Account to = createAccount("TO", receiver);

        Transaction txn = createTransaction(4L, from, to, new BigDecimal("80.00"), LocalDateTime.now());
        when(transactionRepository.findAll()).thenReturn(List.of(txn));

        List<TransactionResponseDto> result = transactionService.getTransactionsForUser(user, null, null, "outgoing", null);

        assertEquals(1, result.size());
        assertEquals("OUTGOING", result.get(0).direction());
    }

    @Test
    void getTransactionsForUser_shouldSortByAmount() {
        User user = createUser(1L, "Alice", "Smith");
        Account from = createAccount("FROM", user);
        Account to = createAccount("TO", user);

        Transaction txn1 = createTransaction(5L, from, to, new BigDecimal("100.00"), LocalDateTime.now());
        Transaction txn2 = createTransaction(6L, from, to, new BigDecimal("200.00"), LocalDateTime.now());
        when(transactionRepository.findAll()).thenReturn(List.of(txn1, txn2));

        List<TransactionResponseDto> result = transactionService.getTransactionsForUser(user, null, null, null, "amount");

        assertEquals(2, result.size());
        assertTrue(result.get(0).amount().compareTo(result.get(1).amount()) > 0);
    }

    private User createUser(Long id, String firstName, String lastName) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private Account createAccount(String iban, User owner) {
        Account account = new Account();
        account.setIBAN(iban);
        account.setCustomer(owner);
        return account;
    }

    private Transaction createTransaction(Long id, Account from, Account to, BigDecimal amount, LocalDateTime time) {
        Transaction txn = new Transaction();
        txn.setTransactionId(id);
        txn.setFromAccount(from);
        txn.setToAccount(to);
        txn.setAmount(amount);
        txn.setTimestamp(time);
        txn.setTransactionType(TransactionType.TRANSFER);
        return txn;
    }
}
