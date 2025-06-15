package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.mapper.TransactionMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.security.Principal;
import com.nextgenbank.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public TransactionController(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String amountFilter,
            Principal principal
    ) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> pageResult = transactionService.getFilteredTransactionsForUser(
                user.getUserId(), iban, name, type, startDate, endDate, amount, amountFilter, pageable
        );

        Page<TransactionResponseDto> dtoPage = pageResult.map(txn ->
                TransactionMapper.toResponseDto(txn, user.getUserId()));

        return ResponseEntity.ok(dtoPage);
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
}