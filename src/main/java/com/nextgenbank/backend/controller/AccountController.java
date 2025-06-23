package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.ApprovalRequestDto;

import com.nextgenbank.backend.service.AccountService;
import com.nextgenbank.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final UserService userService;
    private final AccountService accountService;

    public AccountController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getByEmailOrThrow(userDetails.getUsername());
        return ResponseEntity.ok(accountService.getAccountsForUser(user));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Account>> getCustomerAccounts(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    /**
     * Get all accounts in the system
     */
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /**
     * Get account by IBAN
     */
    @GetMapping("/{iban}")
    public ResponseEntity<Account> getAccountByIban(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getAccountByIban(iban));
    }

    /**
     * Update absolute transfer limit for an account
     */
    @PutMapping("/limit")
    public ResponseEntity<?> updateAbsoluteTransferLimit(@RequestBody ApprovalRequestDto approvalRequestDto) {
        try {
            System.out.println("Received limit update request: " + approvalRequestDto);

            if (approvalRequestDto.getAccountIban() == null || approvalRequestDto.getAccountIban().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Account IBAN cannot be null or empty"));
            }

            Account account = accountService.updateAbsoluteTransferLimit(
                    approvalRequestDto.getAccountIban(),
                    approvalRequestDto.getAbsoluteTransferLimit()
            );
            return ResponseEntity.ok(Map.of(
                    "message", "Absolute transfer limit updated successfully",
                    "account", account
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get all accounts with customer information
     * Returns accounts with embedded customer data for more efficient frontend rendering
     */
    @GetMapping("/all-with-customers")
    public ResponseEntity<List<Map<String, Object>>> getAllAccountsWithCustomers() {
        List<Account> accounts = accountService.getAllAccounts();
        List<Map<String, Object>> result = accounts.stream()
            .map(account -> {
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("IBAN", account.getIBAN());
                accountData.put("accountType", account.getAccountType());
                accountData.put("balance", account.getBalance());
                accountData.put("absoluteTransferLimit", account.getAbsoluteTransferLimit());
                
                if (account.getCustomer() != null) {
                    accountData.put("customerId", account.getCustomer().getUserId());
                    accountData.put("customerName", account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
                }
                
                return accountData;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}