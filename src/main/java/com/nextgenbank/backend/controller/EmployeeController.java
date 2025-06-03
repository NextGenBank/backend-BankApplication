package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.AccountDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee/accounts")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public EmployeeController(AccountRepository accountRepository,
                              UserRepository userRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userRepository   = userRepository;
        this.userService = userService;
    }

    // GET …/status?status=PENDING  (APPROVED, REJECTED)
    @GetMapping("/status")
    public ResponseEntity<List<AccountDto>> getAccountsByStatus(
            @RequestParam UserStatus status) {

        List<User> customers = userRepository.findByStatus(status);
        List<Account> accounts = accountRepository.findByCustomerIn(customers);

        List<AccountDto> result = accounts.stream()
                .map(AccountDto::new)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/customers/{id}/approve")
    public ResponseEntity<Void> approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/customers/{id}/reject")
    public ResponseEntity<Void> rejectUser(@PathVariable Long id) {
        userService.rejectUser(id);
        return ResponseEntity.ok().build();
    }

}