package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.AccountLookupDto;
import com.nextgenbank.backend.service.AccountService;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountLookupController {

    private final AccountService accountService;

    @Autowired
    public AccountLookupController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<Page<AccountLookupDto>> lookupAccounts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String iban,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(accountService.lookupAccounts(name, iban, pageable));
    }

    @GetMapping("/lookup/all")
    public ResponseEntity<Page<AccountLookupDto>> getAllUsersWithIbans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(accountService.getAllUsersWithIbans(pageable));
    }
}
