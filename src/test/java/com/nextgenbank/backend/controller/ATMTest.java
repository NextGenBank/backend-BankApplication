package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ATMTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        testAccount = new Account();
        testAccount.setIBAN("DE123");
        testAccount.setCustomer(testUser);
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void performAtmOperation_Deposit_Success() {
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("200.00");
        Transaction result = transactionService.performAtmOperation(testUser, "DE123", amount, TransactionType.DEPOSIT);

        assertEquals(new BigDecimal("1200.00"), testAccount.getBalance());
        assertEquals(TransactionType.DEPOSIT, result.getTransactionType());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void performAtmOperation_Withdrawal_Success() {
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("300.00");
        Transaction result = transactionService.performAtmOperation(testUser, "DE123", amount, TransactionType.WITHDRAWAL);

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, result.getTransactionType());
    }

    @Test
    void performAtmOperation_Withdrawal_Fails_InsufficientFunds() {
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        BigDecimal amount = new BigDecimal("1500.00");

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.performAtmOperation(testUser, "DE123", amount, TransactionType.WITHDRAWAL));

        assertEquals("Insufficient funds.", ex.getMessage());
        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance()); // Balance should not change
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void performAtmOperation_Fails_AccessDenied() {
        User anotherUser = new User();
        anotherUser.setUserId(2L);
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        BigDecimal amount = new BigDecimal("100.00");

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.performAtmOperation(anotherUser, "DE123", amount, TransactionType.DEPOSIT));

        assertEquals("Access denied to this account.", ex.getMessage());
    }
}