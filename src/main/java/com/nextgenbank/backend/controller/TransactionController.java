package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
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


    @GetMapping("/all")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }


    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<TransactionDto>> getCustomerTransactions(@PathVariable Long customerId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCustomerId(customerId));
    }


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
    
    /**
     * GET /api/transactions/pending
     * Returns a list of pending transactions
     */
    @GetMapping("/pending")
    public ResponseEntity<List<TransactionDto>> getPendingTransactions() {
        return ResponseEntity.ok(transactionService.getPendingTransactions());
    }

    @PostMapping("/atm")
    public ResponseEntity<?> createAtmTransaction(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        TransactionType type = dto.getTransactionType();
        if (dto.getAmount() == null || type == null) {
            return ResponseEntity.badRequest().body("Fields 'amount' and 'transactionType' are required.");
        }

        String iban;
        if (type == TransactionType.DEPOSIT) {
            iban = dto.getToIban();
            if (iban == null) return ResponseEntity.badRequest().body("Field 'toIban' is required for deposit.");
        } else if (type == TransactionType.WITHDRAWAL) {
            iban = dto.getFromIban();
            if (iban == null) return ResponseEntity.badRequest().body("Field 'fromIban' is required for withdrawal.");
        } else {
            return ResponseEntity.badRequest().body("Invalid transaction type for ATM operation.");
        }

        try {
            User user = principal.getUser();
            Transaction tx = transactionService.performAtmOperation(user, iban, dto.getAmount(), type);

            TransactionResponseDto responseDto;
            if (tx.getTransactionType() == TransactionType.DEPOSIT) {
                String toName = tx.getToAccount().getCustomer().getFirstName() + " " + tx.getToAccount().getCustomer().getLastName();
                responseDto = new TransactionResponseDto(
                        tx.getTransactionId(), tx.getTransactionType(), tx.getAmount(), tx.getTimestamp(),
                        null, null, tx.getToAccount().getIBAN(), toName, "DEPOSIT"
                );
            } else { // WITHDRAWAL
                String fromName = tx.getFromAccount().getCustomer().getFirstName() + " " + tx.getFromAccount().getCustomer().getLastName();
                responseDto = new TransactionResponseDto(
                        tx.getTransactionId(), tx.getTransactionType(), tx.getAmount(), tx.getTimestamp(),
                        tx.getFromAccount().getIBAN(), fromName, null, null, "WITHDRAWAL"
                );
            }
            return ResponseEntity.ok(responseDto);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}