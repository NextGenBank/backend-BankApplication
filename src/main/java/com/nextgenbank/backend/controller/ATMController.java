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

import java.util.HashMap;
import java.util.Map;
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

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AtmTransactionDto dto,
                                     Authentication auth) {
        try {
            // Проверяем обязательные поля
            if (dto.getIban() == null || dto.getAmount() == null) {
                return ResponseEntity.badRequest().body("IBAN and amount are required");
            }

            // Находим пользователя
            String currentEmail = auth.getName();
            Optional<User> userOpt = userRepo.findByEmail(currentEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("User not found");
            }

            // Проверяем счет
            Optional<Account> accOpt = accountRepo.findById(dto.getIban());
            if (accOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Account acc = accOpt.get();
            if (!acc.getCustomer().getUserId().equals(userOpt.get().getUserId())) {
                return ResponseEntity.status(403).body("Access denied to this account");
            }

            // Выполняем депозит
            Transaction tx = atmService.deposit(dto.getIban(), dto.getAmount());

            // Возвращаем обновленные данные
            Map<String, Object> response = new HashMap<>();
            response.put("transaction", tx);
            response.put("newBalance", acc.getBalance());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody AtmTransactionDto dto,
                                      Authentication auth) {
        try {
            // Проверяем обязательные поля
            if (dto.getIban() == null || dto.getAmount() == null) {
                return ResponseEntity.badRequest().body("IBAN and amount are required");
            }

            // Находим пользователя
            String currentEmail = auth.getName();
            Optional<User> userOpt = userRepo.findByEmail(currentEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("User not found");
            }

            // Проверяем счет
            Optional<Account> accOpt = accountRepo.findById(dto.getIban());
            if (accOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Account acc = accOpt.get();
            if (!acc.getCustomer().getUserId().equals(userOpt.get().getUserId())) {
                return ResponseEntity.status(403).body("Access denied to this account");
            }

            // Выполняем снятие
            Transaction tx = atmService.withdraw(dto.getIban(), dto.getAmount(), dto.getBills());

            // Возвращаем обновленные данные
            Map<String, Object> response = new HashMap<>();
            response.put("transaction", tx);
            response.put("newBalance", acc.getBalance());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}
