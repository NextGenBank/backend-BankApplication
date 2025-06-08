package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.security.CurrentUser;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponseDto> getUserTransactions(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort
    ) {
        return transactionService.getTransactionsForUser(principal.getUser(), iban, name, type, sort);
    }
}
