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

/**
 * Unit tests for the TransactionService, focusing on ATM business logic.
 * This uses Mockito to test the service in isolation from the database.
 */
@ExtendWith(MockitoExtension.class)
class ATMTest { // Consider renaming to TransactionServiceTest for convention

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;

    /**
     * Sets up a consistent state before each test.
     */
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        testAccount = new Account();
        testAccount.setIBAN("DE123");
        testAccount.setCustomer(testUser);
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    /**
     * Test case for a successful deposit operation.
     */
    @Test
    void performAtmOperation_Deposit_Success() {
        // Arrange: Mock repository calls for a successful scenario.
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act: Perform the deposit.
        BigDecimal amount = new BigDecimal("200.00");
        Transaction result = transactionService.performAtmOperation(testUser, "DE123", amount, TransactionType.DEPOSIT);

        // Assert: Check the new balance and verify that save methods were called.
        assertEquals(0, new BigDecimal("1200.00").compareTo(testAccount.getBalance()));
        assertEquals(TransactionType.DEPOSIT, result.getTransactionType());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Test case for a successful withdrawal operation.
     */
    @Test
    void performAtmOperation_Withdrawal_Success() {
        // Arrange
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        BigDecimal amount = new BigDecimal("300.00");
        Transaction result = transactionService.performAtmOperation(testUser, "DE123", amount, TransactionType.WITHDRAWAL);

        // Assert
        assertEquals(0, new BigDecimal("700.00").compareTo(testAccount.getBalance()));
        assertEquals(TransactionType.WITHDRAWAL, result.getTransactionType());
    }

    /**
     * Test case for a failed withdrawal due to insufficient funds.
     */
    @Test
    void performAtmOperation_Withdrawal_Fails_InsufficientFunds() {
        // Arrange
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        BigDecimal amount = new BigDecimal("1500.00");

        // Act & Assert: Expect an exception and check the message.
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.performAtmOperation(testUser, "DE123", amount, TransactionType.WITHDRAWAL));
        assertEquals("Insufficient funds.", ex.getMessage());

        // Assert: Ensure balance did not change and no data was saved.
        assertEquals(0, new BigDecimal("1000.00").compareTo(testAccount.getBalance()));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    /**
     * Test case where a user tries to access an account they do not own.
     */
    @Test
    void performAtmOperation_Fails_AccessDenied() {
        // Arrange: Create a different user to initiate the operation.
        User anotherUser = new User();
        anotherUser.setUserId(2L);
        when(accountRepository.findById("DE123")).thenReturn(Optional.of(testAccount));
        BigDecimal amount = new BigDecimal("100.00");

        // Act & Assert: Expect an exception when 'anotherUser' tries to access the account.
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.performAtmOperation(anotherUser, "DE123", amount, TransactionType.DEPOSIT));

        assertEquals("Access denied to this account.", ex.getMessage());
    }
}
