package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AccountRepository accountRepo;
    private final UserRepository userRepo;

    public DashboardController(AccountRepository accountRepo,
                               UserRepository userRepo) {
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
    }

    /**
     * GET /api/dashboard/accounts
     * Возвращает список всех аккаунтов (Account) текущего залогиненного пользователя.
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getMyAccounts(Authentication auth) {
        String currentEmail = auth.getName();
        Optional<User> userOpt = userRepo.findByEmail(currentEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User currentUser = userOpt.get();

        List<Account> accounts = accountRepo.findByCustomer(currentUser);
        return ResponseEntity.ok(accounts);
    }

    /**
     * GET /api/dashboard/account/{iban}
     * Возвращает один конкретный аккаунт (Account) текущего пользователя по IBAN,
     * чтобы фронтенд мог получить только его баланс, например.
     */
    @GetMapping("/account/{iban}")
    public ResponseEntity<?> getMyAccountByIban(@PathVariable String iban,
                                                Authentication auth) {
        String currentEmail = auth.getName();
        Optional<User> userOpt = userRepo.findByEmail(currentEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User currentUser = userOpt.get();

        Optional<Account> accOpt = accountRepo.findById(iban);
        if (accOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account acc = accOpt.get();

        if (!acc.getCustomer().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(403).body("Out fo Service");
        }

        return ResponseEntity.ok(acc);
    }
}
