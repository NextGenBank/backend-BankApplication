package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.AccountDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountController(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<List<AccountDto>> getMyAccounts(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Account> accounts = accountRepository.findByCustomer(user);
        List<AccountDto> result = accounts.stream().map(AccountDto::new).toList();
        return ResponseEntity.ok(result);
    }
}
