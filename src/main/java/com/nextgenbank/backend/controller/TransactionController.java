package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.mapper.TransactionMapper;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.dto.*;
import com.nextgenbank.backend.security.CurrentUser;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.TransactionService;
import com.nextgenbank.backend.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    /**
     * Get filtered transactions for the current user with pagination
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponseDto>> getTransactions(
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String amountFilter,
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            Principal principal,
            HttpServletRequest request
    ) {
        try {
            User user = userService.getByEmailOrThrow(principal.getName());

            Page<Transaction> pageResult = transactionService.getFilteredTransactionsForUser(
                    user.getUserId(), iban, name, type, startDate, endDate, amount, amountFilter, pageable
            );

            Page<TransactionResponseDto> dtoPage = pageResult.map(txn ->
                    TransactionMapper.toResponseDto(txn, user.getUserId()));

            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Get all transactions with pagination for employee view
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        try {
            return ResponseEntity.ok(transactionService.getAllTransactionsPaginated(pageable));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching all transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Get transactions for a specific customer with pagination
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<TransactionDto>> getCustomerTransactions(
            @PathVariable Long customerId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        try {
            return ResponseEntity.ok(transactionService.getTransactionsByCustomerIdPaginated(customerId, pageable));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching customer transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Process a transfer between accounts
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferFunds(
            @RequestBody TransferRequestDto transferRequest) {
        try {
            TransactionDto transactionDto = transactionService.transferFunds(transferRequest);
            return ResponseEntity.ok(transactionDto);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid transfer request: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new RuntimeException("Authorization error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Switch funds between accounts for a customer
     */
    @PostMapping("/switch")
    public ResponseEntity<SwitchFundsResponseDto> switchFunds(
            @CurrentUser UserPrincipal principal,
            @RequestBody SwitchFundsRequestDto request) {
        try {
            SwitchFundsResponseDto responseDto = transactionService.switchFunds(principal.getUser(), request);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid switch request: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Switch failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get pending transactions with pagination
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<TransactionDto>> getPendingTransactions(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        try {
            return ResponseEntity.ok(transactionService.getPendingTransactionsPaginated(pageable));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching pending transactions: " + e.getMessage(), e);
        }
    }
}