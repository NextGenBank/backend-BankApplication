// src/main/java/com/nextgenbank/backend/service/ATMService.java
package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ATMService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public ATMService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository     = accountRepository;
        this.userRepository        = userRepository;
    }

    /**
     * INTERNAL: Создание транзакции DEPOSIT (зачисление на счёт).
     *
     * @param initiator текущий пользователь
     * @param toIban    IBAN счёта для депозита
     * @param amount    сумма депозита
     * @return сохранённая сущность Transaction
     */
    @Transactional
    public Transaction doDeposit(User initiator, String toIban, BigDecimal amount) {
        Account toAcc = accountRepository.findById(toIban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + toIban));

        // Предполагаем, что депозит разрешён только на собственный счёт
        if (!toAcc.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to deposit into this account");
        }

        // Увеличиваем баланс
        toAcc.setBalance(toAcc.getBalance().add(amount));
        accountRepository.save(toAcc);

        // Создаём транзакцию и сохраняем
        Transaction tx = new Transaction();
        tx.setFromAccount(null);
        tx.setToAccount(toAcc);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);

        return transactionRepository.save(tx);
    }

    /**
     * INTERNAL: Создание транзакции WITHDRAWAL (снятие со счёта).
     *
     * @param initiator текущий пользователь
     * @param fromIban  IBAN счёта для снятия
     * @param amount    сумма снятия
     * @param bills     количество банкнот (не сохраняется в сущности)
     * @return сохранённая сущность Transaction
     */
    @Transactional
    public Transaction doWithdraw(User initiator, String fromIban, BigDecimal amount, Integer bills) {
        Account fromAcc = accountRepository.findById(fromIban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + fromIban));

        // Проверяем права доступа
        if (!fromAcc.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to withdraw from this account");
        }

        // Проверяем баланс
        if (fromAcc.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Уменьшаем баланс
        fromAcc.setBalance(fromAcc.getBalance().subtract(amount));
        accountRepository.save(fromAcc);

        // Создаём транзакцию и сохраняем
        Transaction tx = new Transaction();
        tx.setFromAccount(fromAcc);
        tx.setToAccount(null);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);
        // Поле bills в сущности не хранится; при необходимости можно добавить соответствующее поле

        return transactionRepository.save(tx);
    }
}
