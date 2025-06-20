package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountService(accountRepository, userRepository);
    }

    @Test
    void shouldGetAccountsByCustomerId() {
        // Given
        Long customerId = 1L;
        User customer = createTestUser(customerId, UserRole.CUSTOMER);
        List<Account> expectedAccounts = Arrays.asList(
                createTestAccount("NL01IBAN1", customer, AccountType.CHECKING),
                createTestAccount("NL01IBAN2", customer, AccountType.SAVINGS)
        );
        
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(accountRepository.findByCustomer(customer)).thenReturn(expectedAccounts);
        
        // When
        List<Account> result = accountService.getAccountsByCustomerId(customerId);
        
        // Then
        assertEquals(2, result.size());
        assertEquals("NL01IBAN1", result.get(0).getIBAN());
        assertEquals("NL01IBAN2", result.get(1).getIBAN());
        verify(accountRepository).findByCustomer(customer);
    }

    @Test
    void shouldGetAccountByIban() {
        // Given
        String iban = "NL01IBAN1";
        Account expectedAccount = createTestAccount(iban, createTestUser(1L, UserRole.CUSTOMER), AccountType.CHECKING);
        
        when(accountRepository.findById(iban)).thenReturn(Optional.of(expectedAccount));
        
        // When
        Account result = accountService.getAccountByIban(iban);
        
        // Then
        assertEquals(iban, result.getIBAN());
        assertEquals(AccountType.CHECKING, result.getAccountType());
    }

    @Test
    void shouldGetAllAccounts() {
        // Given
        List<Account> expectedAccounts = Arrays.asList(
                createTestAccount("NL01IBAN1", createTestUser(1L, UserRole.CUSTOMER), AccountType.CHECKING),
                createTestAccount("NL01IBAN2", createTestUser(1L, UserRole.CUSTOMER), AccountType.SAVINGS),
                createTestAccount("NL01IBAN3", createTestUser(2L, UserRole.CUSTOMER), AccountType.CHECKING)
        );
        
        when(accountRepository.findAll()).thenReturn(expectedAccounts);
        
        // When
        List<Account> result = accountService.getAllAccounts();
        
        // Then
        assertEquals(3, result.size());
        verify(accountRepository).findAll();
    }

    @Test
    void shouldUpdateTransferLimit() {
        // Given
        String iban = "NL01IBAN1";
        BigDecimal newLimit = new BigDecimal("3000.00");
        Account account = createTestAccount(iban, createTestUser(1L, UserRole.CUSTOMER), AccountType.CHECKING);
        account.setAbsoluteTransferLimit(new BigDecimal("1000.00"));
        
        when(accountRepository.findById(iban)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        
        // When
        Account result = accountService.updateAbsoluteTransferLimit(iban, newLimit);
        
        // Then
        assertEquals(newLimit, result.getAbsoluteTransferLimit());
        verify(accountRepository).save(account);
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        String iban = "NL01IBAN1";
        when(accountRepository.findById(iban)).thenReturn(Optional.empty());
        
        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
                accountService.updateAbsoluteTransferLimit(iban, new BigDecimal("1000.00")));
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void shouldThrowExceptionWhenNegativeTransferLimit() {
        // Given
        String iban = "NL01IBAN1";
        BigDecimal negativeLimit = new BigDecimal("-1000.00");
        
        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
                accountService.updateAbsoluteTransferLimit(iban, negativeLimit));
        assertTrue(exception.getMessage().contains("Transfer limit must be a positive number"));
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void shouldCreateAccountsForUser() {
        // Given
        User customer = createTestUser(1L, UserRole.CUSTOMER);
        User employee = createTestUser(2L, UserRole.EMPLOYEE);
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(customer, employee));
        // Use the any() matcher for the IBAN since it's generated randomly
        when(accountRepository.existsById(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        accountService.createAccountsForUser(customer);
        
        // Then
        // Verify that save was called twice (for checking and savings accounts)
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    private User createTestUser(Long id, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName("Test");
        user.setLastName("User" + id);
        user.setEmail("test" + id + "@example.com");
        user.setRole(role);
        return user;
    }

    private Account createTestAccount(String iban, User customer, AccountType type) {
        Account account = new Account();
        account.setIBAN(iban);
        account.setCustomer(customer);
        account.setAccountType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}