package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.dto.ApprovalRequestDto;
import com.nextgenbank.backend.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Get all accounts for a customer
     */
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
}