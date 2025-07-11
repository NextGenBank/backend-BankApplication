package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsResponseDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldHandleNullAmountFilter() {
        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, null, null, null, null, null, new BigDecimal("100.00"), null, PageRequest.of(0, 10));
        // Verify default "eq" is used
    }

    @Test
    void shouldReturnEmptyPageWhenNoResults() {
        when(transactionRepository.findAllByUserIdWithFilters(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<Transaction> result = transactionService.getFilteredTransactionsForUser(
                1L, null, null, null, null, null, new BigDecimal("999.99"), "EQUAL", PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }
    
    @Test
    void shouldGetAllTransactionsPaginated() {
        // Given
        List<Transaction> transactions = Arrays.asList(
            mockTransaction(1L),
            mockTransaction(2L)
        );
        Page<Transaction> page = new PageImpl<>(transactions);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(transactionRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(page);
        
        // When
        Page<TransactionDto> result = transactionService.getAllTransactionsPaginated(pageable);
        
        // Then
        assertEquals(2, result.getTotalElements());
        verify(transactionRepository).findAllByOrderByTimestampDesc(pageable);
    }
    
    @Test
    void shouldGetCustomerTransactionsPaginated() {
        // Given
        Long customerId = 1L;
        User customer = new User();
        customer.setUserId(customerId);
        customer.setRole(UserRole.CUSTOMER);
        
        List<Transaction> transactions = Arrays.asList(
            mockTransaction(1L),
            mockTransaction(2L)
        );
        Page<Transaction> page = new PageImpl<>(transactions);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(transactionRepository.findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(
            eq(customer), eq(customer), eq(pageable))).thenReturn(page);
        
        // When
        Page<TransactionDto> result = transactionService.getTransactionsByCustomerIdPaginated(customerId, pageable);
        
        // Then
        assertEquals(2, result.getTotalElements());
        verify(transactionRepository).findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(
            eq(customer), eq(customer), eq(pageable));
    }
    
    @Test
    void shouldUpdateTransferLimit() {
        // Given
        String accountIban = "NL12BANK1234567890";
        BigDecimal newLimit = new BigDecimal("2000.00");
        
        Account account = new Account();
        account.setIBAN(accountIban);
        account.setAbsoluteTransferLimit(new BigDecimal("1000.00"));
        
        when(accountRepository.findById(accountIban)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        // When
        transactionService.updateTransferLimit(accountIban, newLimit);
        
        // Then
        verify(accountRepository).findById(accountIban);
        verify(accountRepository).save(account);
        assertEquals(newLimit, account.getAbsoluteTransferLimit());
    }
    
    @Test
    void shouldThrowExceptionForNegativeTransferLimit() {
        // Given
        String accountIban = "NL12BANK1234567890";
        BigDecimal negativeLimit = new BigDecimal("-1000.00");
        
        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> transactionService.updateTransferLimit(accountIban, negativeLimit));
        
        assertTrue(exception.getMessage().contains("Transfer limit must be a positive number"));
        verify(accountRepository, never()).save(any());
    }
    

    @Test
    void shouldThrowExceptionWhenNonEmployeeInitiatesEmployeeTransfer() {
        // Given
        TransferRequestDto transferRequest = new TransferRequestDto();
        transferRequest.setInitiatorId(1L);
        
        User customer = new User();
        customer.setUserId(1L);
        customer.setRole(UserRole.CUSTOMER);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        
        // When & Then
        Exception exception = assertThrows(SecurityException.class, 
            () -> transactionService.processEmployeeTransfer(transferRequest));
        
        assertTrue(exception.getMessage().contains("Only employees can initiate transfers"));
        verify(accountRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
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

    @Test
    void shouldSwitchFundsBetweenAccounts() {
        User user = new User();
        user.setUserId(1L);
        user.setRole(UserRole.CUSTOMER);

        Account checking = new Account();
        checking.setIBAN("CHECKING123");
        checking.setBalance(new BigDecimal("1000"));
        checking.setAccountType(AccountType.CHECKING);
        checking.setCustomer(user);

        Account savings = new Account();
        savings.setIBAN("SAVINGS123");
        savings.setBalance(new BigDecimal("500"));
        savings.setAccountType(AccountType.SAVINGS);
        savings.setCustomer(user);

        when(accountRepository.findByCustomerUserIdAndAccountType(1L, AccountType.CHECKING))
                .thenReturn(Optional.of(checking));

        when(accountRepository.findByCustomerUserIdAndAccountType(1L, AccountType.SAVINGS))
                .thenReturn(Optional.of(savings));

        SwitchFundsRequestDto request = new SwitchFundsRequestDto("CHECKING", new BigDecimal("200"));
        SwitchFundsResponseDto response = transactionService.switchFunds(user, request);

        assertEquals(new BigDecimal("800"), response.getCheckingBalance());
        assertEquals(new BigDecimal("700"), response.getSavingsBalance());
    }
}