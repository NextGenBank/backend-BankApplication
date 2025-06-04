package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.security.CurrentUser;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
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

    /**
     * GET /api/transactions
     * Возвращает историю транзакций для текущего пользователя.
     */
    @GetMapping
    public List<TransactionResponseDto> getUserTransactions(@CurrentUser UserPrincipal principal) {
        User user = principal.getUser();
        List<Transaction> txs = transactionService.getTransactionsForUser(user);
        return txs.stream()
                .map(tx -> new TransactionResponseDto(
                        tx.getTransactionId(),
                        tx.getTransactionType(),
                        tx.getAmount(),
                        tx.getTimestamp(),
                        tx.getFromAccount() != null ? tx.getFromAccount().getIBAN() : null,
                        tx.getToAccount()   != null ? tx.getToAccount().getIBAN()   : null
                ))
                .toList();
    }

    /**
     * POST /api/transactions/transfer
     * Тело запроса:
     * {
     *   "fromIban": "NL1234567890",
     *   "toIban":   "NL09876543210987654321",
     *   "amount":   500.00
     * }
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> createTransfer(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        if (dto.getFromIban() == null || dto.getToIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("fromIban, toIban and amount are required");
        }
        try {
            Transaction tx = transactionService.doTransfer(
                    principal.getUser(),
                    dto.getFromIban(),
                    dto.getToIban(),
                    dto.getAmount()
            );
            return ResponseEntity.ok(new TransactionResponseDto(
                    tx.getTransactionId(),
                    tx.getTransactionType(),
                    tx.getAmount(),
                    tx.getTimestamp(),
                    tx.getFromAccount().getIBAN(),
                    tx.getToAccount().getIBAN()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * POST /api/transactions/deposit
     * Тело запроса:
     * {
     *   "toIban": "NL09876543210987654321",
     *   "amount": 1000.00
     * }
     */
    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        if (dto.getToIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("toIban and amount are required");
        }
        try {
            Transaction tx = transactionService.doDeposit(
                    principal.getUser(),
                    dto.getToIban(),
                    dto.getAmount()
            );
            return ResponseEntity.ok(new TransactionResponseDto(
                    tx.getTransactionId(),
                    tx.getTransactionType(),
                    tx.getAmount(),
                    tx.getTimestamp(),
                    null,
                    tx.getToAccount().getIBAN()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * POST /api/transactions/withdraw
     * Тело запроса:
     * {
     *   "fromIban": "NL1234567890",
     *   "amount": 200.00,
     *   "bills": 50    // опционально
     * }
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> createWithdraw(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        if (dto.getFromIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("fromIban and amount are required");
        }
        try {
            Transaction tx = transactionService.doWithdraw(
                    principal.getUser(),
                    dto.getFromIban(),
                    dto.getAmount(),
                    dto.getBills()
            );
            return ResponseEntity.ok(new TransactionResponseDto(
                    tx.getTransactionId(),
                    tx.getTransactionType(),
                    tx.getAmount(),
                    tx.getTimestamp(),
                    tx.getFromAccount().getIBAN(),
                    null
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
