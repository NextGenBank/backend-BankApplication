package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository     = accountRepository;
        this.userRepository        = userRepository;
    }

    /**
     * GET /api/transactions
     * Возвращает историю транзакций (TRANSFER, DEPOSIT, WITHDRAWAL) для данного пользователя,
     * преобразованную в TransactionResponseDto (с полями fromIban, fromName, toIban, toName, direction).
     */
    public List<TransactionResponseDto> getTransactionsForUser(User user) {
        Long userId = user.getUserId();

        return transactionRepository.findAll().stream()
                .filter(txn -> {
                    Account from = txn.getFromAccount();
                    Account to = txn.getToAccount();
                    return (from != null && from.getCustomer().getUserId().equals(userId)) ||
                            (to   != null && to.getCustomer().getUserId().equals(userId));
                })
                .map(txn -> {
                    // --- Собираем fromIban и fromName ---
                    String fromIban = null;
                    String fromName = null;
                    if (txn.getFromAccount() != null) {
                        fromIban = txn.getFromAccount().getIBAN();
                        User sender = txn.getFromAccount().getCustomer();
                        if (sender != null) {
                            fromName = sender.getFirstName() + " " + sender.getLastName();
                        }
                    }

                    // --- Собираем toIban и toName ---
                    String toIban = null;
                    String toName = null;
                    if (txn.getToAccount() != null) {
                        toIban = txn.getToAccount().getIBAN();
                        User receiver = txn.getToAccount().getCustomer();
                        if (receiver != null) {
                            toName = receiver.getFirstName() + " " + receiver.getLastName();
                        }
                    }

                    // --- Определяем направление (direction) ---
                    boolean isSender   = txn.getFromAccount() != null &&
                            txn.getFromAccount().getCustomer().getUserId().equals(userId);
                    boolean isReceiver = txn.getToAccount()   != null &&
                            txn.getToAccount().getCustomer().getUserId().equals(userId);

                    String direction;
                    if (isSender && isReceiver) {
                        direction = "INTERNAL";    // теоретически оба счета принадлежат одному польз., редкий случай
                    } else if (isSender) {
                        direction = "OUTGOING";    // пользователь отправил деньги
                    } else {
                        direction = "INCOMING";    // пользователь получил деньги
                    }

                    return new TransactionResponseDto(
                            txn.getTransactionId(),
                            txn.getTransactionType(),
                            txn.getAmount(),
                            txn.getTimestamp(),
                            fromIban,
                            fromName,
                            toIban,
                            toName,
                            direction
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * INTERNAL: Создание транзакции TRANSFER (снятие с одного счёта -> зачисление на другой).
     */
    @Transactional
    public Transaction doTransfer(User initiator, String fromIban, String toIban, BigDecimal amount) {
        // 1) Нахождение счётов
        Account fromAcc = accountRepository.findById(fromIban)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromIban));
        Account toAcc   = accountRepository.findById(toIban)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + toIban));

        // 2) Проверка прав доступа (инициатор должен быть владельцем fromAcc)
        if (!fromAcc.getCustomer().getUserId().equals(initiator.getUserId())) {
            throw new IllegalArgumentException("Access denied to source account");
        }

        // 3) Проверка баланса
        if (fromAcc.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account");
        }

        // 4) Уменьшаем баланс у отправителя и увеличиваем баланс у получателя
        fromAcc.setBalance(fromAcc.getBalance().subtract(amount));
        toAcc.setBalance(toAcc.getBalance().add(amount));
        accountRepository.save(fromAcc);
        accountRepository.save(toAcc);

        // 5) Создаём запись транзакции и сохраняем
        Transaction tx = new Transaction();
        tx.setFromAccount(fromAcc);
        tx.setToAccount(toAcc);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setTimestamp(LocalDateTime.now());
        tx.setInitiator(initiator);

        return transactionRepository.save(tx);
    }

    /**
     * INTERNAL: Создание транзакции DEPOSIT (зачисление на счёт).
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
