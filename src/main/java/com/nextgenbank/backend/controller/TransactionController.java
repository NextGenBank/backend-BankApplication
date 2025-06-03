package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Get all transactions in the system
     */
    @GetMapping
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /**
     * Get all transactions for a specific customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<TransactionDto>> getCustomerTransactions(@PathVariable Long customerId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCustomerId(customerId));
    }

    /**
     * Process a transfer between accounts
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transferFunds(@RequestBody TransferRequestDto transferRequestDto) {
        try {
            TransactionDto transactionDto = transactionService.transferFunds(transferRequestDto);
            return ResponseEntity.ok(Map.of(
                "message", "Transfer completed successfully",
                "transaction", transactionDto
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}