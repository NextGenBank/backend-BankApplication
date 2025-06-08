package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsResponseDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.security.CurrentUser;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/switch")
    public ResponseEntity<?> switchFunds(@CurrentUser UserPrincipal principal,
                                         @RequestBody SwitchFundsRequestDto request) {
        try {
            SwitchFundsResponseDto responseDto = transactionService.switchFunds(principal.getUser(), request);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error occurred."));
        }
    }
}