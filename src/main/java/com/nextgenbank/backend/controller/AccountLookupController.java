package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.AccountLookupDto;
import com.nextgenbank.backend.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountLookupController {

    private final AccountService accountService;

    public AccountLookupController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookupIBAN(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String iban,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            List<AccountLookupDto> results = accountService.lookupAccounts(name, iban);

            if (results.isEmpty()) {
                return ResponseEntity.status(404).body("No users found");
            }

            // pagination
            int totalItems = results.size();
            int totalPages = (int) Math.ceil((double) totalItems / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalItems);

            if (fromIndex > totalItems) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<AccountLookupDto> paginatedResults = results.subList(fromIndex, toIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("content", paginatedResults);
            response.put("totalPages", totalPages);
            response.put("totalElements", totalItems);
            response.put("page", page);
            response.put("size", paginatedResults.size());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all-iban-users")
    public ResponseEntity<?> getAllUsersWithIbans() {
        List<AccountLookupDto> results = accountService.getAllUsersWithIbans();
        return ResponseEntity.ok(results);
    }
}
