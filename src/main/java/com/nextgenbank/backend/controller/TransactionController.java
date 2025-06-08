package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.security.CurrentUser;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /api/transactions
     * Возвращает историю транзакций для текущего пользователя.
     */
    @GetMapping
    public List<TransactionResponseDto> getUserTransactions(@CurrentUser UserPrincipal principal) {
        return transactionService.getTransactionsForUser(principal.getUser());
    }

    /**
     * GET /api/transactions/all
     * Возвращает список всех транзакций (DTO).
     */
    @GetMapping("/all")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /**
     * GET /api/transactions/customer/{customerId}
     * Возвращает все транзакции заданного клиента.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<TransactionDto>> getCustomerTransactions(@PathVariable Long customerId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCustomerId(customerId));
    }

    /**
     * POST /api/transactions/transfer
     * Обрабатывает перевод средств между счетами.
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
