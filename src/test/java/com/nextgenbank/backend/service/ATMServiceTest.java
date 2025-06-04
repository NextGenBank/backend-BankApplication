package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ATMServiceTest {

    private AccountRepository accountRepo;
    private TransactionRepository txRepo;
    private ATMService atmService;

    @BeforeEach
    void setUp() {
        // Создаём моки репозиториев
        accountRepo   = mock(AccountRepository.class);
        txRepo        = mock(TransactionRepository.class);
        // Инициируем сервис с «замоканными»» зависимостями
        atmService    = new ATMService(accountRepo, txRepo);
    }

    @Test
    @DisplayName("Deposit: при правильном балансе баланс увеличивается и создаётся транзакция DEPOSIT")
    void depositShouldIncreaseBalanceAndSaveTransaction() {
        // 1) Подготавливаем фиктивный Account c IBAN "DE123"
        Account dummy = new Account();
        dummy.setIBAN("DE123");
        dummy.setBalance(new BigDecimal("100.00"));

        // 2) Настраиваем accountRepo.findById("DE123") → Optional.of(dummy)
        when(accountRepo.findById("DE123")).thenReturn(Optional.of(dummy));

        // 3) Настраиваем txRepo.save(...) возвращать тот же объект, который сохраняем
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 4) Вызываем метод deposit
        Transaction createdTx = atmService.deposit("DE123", new BigDecimal("50.00"));

        // 5) Проверяем, что accountRepo.save(dummy) вызвано ровно 1 раз
        verify(accountRepo, times(1)).save(dummy);

        // 6) Провалидаем поля транзакции
        assertThat(createdTx.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(createdTx.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(createdTx.getToAccount()).isSameAs(dummy);
        assertThat(createdTx.getFromAccount()).isNull();
        assertThat(createdTx.getTimestamp()).isNotNull();

        // 7) И в конце баланс у dummy стал 150.00
        assertThat(dummy.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Withdraw: при достаточном балансе баланс уменьшается и создаётся транзакция WITHDRAWAL")
    void withdrawShouldDecreaseBalanceAndSaveTransaction() {
        Account dummy = new Account();
        dummy.setIBAN("DE456");
        dummy.setBalance(new BigDecimal("200.00"));

        when(accountRepo.findById("DE456")).thenReturn(Optional.of(dummy));
        when(txRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Вызываем метод withdraw
        Transaction createdTx = atmService.withdraw("DE456", new BigDecimal("75.00"), 3);

        verify(accountRepo, times(1)).save(dummy);

        assertThat(createdTx.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(createdTx.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(createdTx.getFromAccount()).isSameAs(dummy);
        assertThat(createdTx.getToAccount()).isNull();
        assertThat(createdTx.getTimestamp()).isNotNull();

        // Баланс должен стать 125.00
        assertThat(dummy.getBalance()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("Withdraw: если баланс недостаточен, бросается IllegalArgumentException")
    void withdrawShouldThrowWhenInsufficientFunds() {
        Account dummy = new Account();
        dummy.setIBAN("DE789");
        dummy.setBalance(new BigDecimal("20.00"));

        when(accountRepo.findById("DE789")).thenReturn(Optional.of(dummy));

        // Ожидаем, что при попытке снять 50.00 выбросится IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                atmService.withdraw("DE789", new BigDecimal("50.00"), null)
        );
        assertThat(ex.getMessage()).contains("Insufficient funds");

        // При этом accountRepo.save НЕ вызывается, и txRepo.save НЕ вызывается
        verify(accountRepo, never()).save(any(Account.class));
        verify(txRepo, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deposit: если счёт не найден, бросается IllegalArgumentException")
    void depositShouldThrowWhenAccountNotFound() {
        when(accountRepo.findById("UNKNOWN")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                atmService.deposit("UNKNOWN", new BigDecimal("10.00"))
        );
        assertThat(ex.getMessage()).contains("Account not found");

        verify(accountRepo, never()).save(any(Account.class));
        verify(txRepo, never()).save(any(Transaction.class));
    }
}
