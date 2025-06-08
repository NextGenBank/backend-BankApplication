package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.service.ATMService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ATMServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ATMService atmService;

    @Test
    void testDoDeposit_Success() {
        User user = new User(); user.setUserId(1L);
        Account account = new Account();
        account.setIBAN("NL123");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCustomer(user);

        Mockito.when(accountRepository.findById("NL123")).thenReturn(Optional.of(account));
        Mockito.when(transactionRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        Transaction tx = atmService.doDeposit(user, "NL123", BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(1500), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, tx.getTransactionType());
        assertEquals(account, tx.getToAccount());
    }

    @Test
    void testDoWithdraw_InsufficientFunds() {
        User user = new User(); user.setUserId(1L);
        Account acc = new Account(); acc.setBalance(BigDecimal.valueOf(100)); acc.setCustomer(user);
        acc.setIBAN("NL123");

        Mockito.when(accountRepository.findById("NL123")).thenReturn(Optional.of(acc));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> atmService.doWithdraw(user, "NL123", BigDecimal.valueOf(200), null)
        );

        assertEquals("Insufficient funds", ex.getMessage());
    }

    // Добавьте тесты на доступ запрещён, аккаунт не найден и успешный withdraw
}
