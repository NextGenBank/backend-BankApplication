package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        accountRepository = Mockito.mock(AccountRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        transactionService = new TransactionService(transactionRepository, accountRepository, userRepository);
    }

    @Test
    void shouldReturnAllTransactionsForUser() {
        Transaction txn = mockTransaction(1L);
        Page<Transaction> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findAllByUserIdWithFilters(
                eq(1L), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, null, null, null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(txn.getTransactionId(), result.getContent().get(0).getTransactionId());
    }

    @Test
    void shouldFilterByIban() {
        Transaction txn = mockTransaction(2L);
        Page<Transaction> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findAllByUserIdWithFilters(
                eq(1L), eq("NL01IBAN"), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, "NL01IBAN", null, null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFilterByName() {
        Transaction txn = mockTransaction(3L);
        Page<Transaction> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findAllByUserIdWithFilters(
                eq(1L), any(), eq("smith"), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, null, "smith", null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFilterByDirection() {
        Transaction txn = mockTransaction(4L);
        Page<Transaction> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findAllByUserIdWithFilters(
                eq(1L), any(), any(), eq("INCOMING"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, null, null, "INCOMING", null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFilterByAmount() {
        Transaction txn = mockTransaction(5L);
        Page<Transaction> page = new PageImpl<>(List.of(txn));

        when(transactionRepository.findAllByUserIdWithFilters(
                eq(1L), any(), any(), any(), any(), any(), eq(new BigDecimal("100.00")), eq("EQUAL"), any(Pageable.class)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, null, null, null, null, null, new BigDecimal("100.00"), "EQUAL", PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }

    private Transaction mockTransaction(Long id) {
        Transaction t = new Transaction();
        t.setTransactionId(id);
        t.setAmount(new BigDecimal("100.00"));
        t.setTimestamp(LocalDateTime.now());

        Account from = new Account();
        from.setIBAN("FROM_IBAN");
        from.setCustomer(new User());

        Account to = new Account();
        to.setIBAN("TO_IBAN");
        to.setCustomer(new User());

        t.setFromAccount(from);
        t.setToAccount(to);
        t.setTransactionType(TransactionType.TRANSFER);

        return t;
    }
}
