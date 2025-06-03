package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.AtmTransactionDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.service.ATMService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/atm")
public class ATMController {
    private final ATMService atmService;
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;

    public ATMController(ATMService atmService,
                         AccountRepository accountRepo,
                         UserRepository userRepo) {
        this.atmService = atmService;
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
    }

    /**
     * Депозит:
     *  - Принимаем JSON:
     *    {
     *      "iban": "DE1234567890",
     *      "amount": 100.00
     *    }
     *  - В Authentication.getName() получаем email текущего пользователя.
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AtmTransactionDto dto,
                                     Authentication auth) {
        // Проверяем обязательные поля
        if (dto.getIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("Поля iban и amount обязательны");
        }

        // 1) Найти текущего пользователя по email
        String currentEmail = auth.getName();
        Optional<User> userOpt = userRepo.findByEmail(currentEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Пользователь не найден");
        }
        User currentUser = userOpt.get();

        // 2) Проверить, что счет существует и принадлежит текущему пользователю
        Optional<Account> accOpt = accountRepo.findById(dto.getIban());
        if (accOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account acc = accOpt.get();

        if (!acc.getCustomer().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(403).body("У вас нет доступа к этому счету");
        }

        // 3) Выполнить депозит, передавая IBAN (String) и сумму
        Transaction tx;
        try {
            tx = atmService.deposit(dto.getIban(), dto.getAmount());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        return ResponseEntity.ok(tx);
    }

    /**
     * Снятие:
     *  - Принимаем JSON:
     *    {
     *      "iban": "DE1234567890",
     *      "amount": 50.00,
     *      "bills": 10    // опционально
     *    }
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody AtmTransactionDto dto,
                                      Authentication auth) {
        // Проверяем обязательные поля
        if (dto.getIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("Поля iban и amount обязательны");
        }

        // 1) Найти текущего пользователя по email
        String currentEmail = auth.getName();
        Optional<User> userOpt = userRepo.findByEmail(currentEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Пользователь не найден");
        }
        User currentUser = userOpt.get();

        // 2) Проверить, что счет существует и принадлежит текущему пользователю
        Optional<Account> accOpt = accountRepo.findById(dto.getIban());
        if (accOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account acc = accOpt.get();

        if (!acc.getCustomer().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(403).body("У вас нет доступа к этому счету");
        }

        // 3) Выполнить снятие, передавая IBAN (String), сумму и bills (Integer)
        Transaction tx;
        try {
            tx = atmService.withdraw(dto.getIban(), dto.getAmount(), dto.getBills());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        return ResponseEntity.ok(tx);
    }
}
