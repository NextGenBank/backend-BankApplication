package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.mapper.TransactionMapper;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.SwitchFundsRequestDto;
import com.nextgenbank.backend.model.dto.SwitchFundsResponseDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
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
            Principal principal
//            HttpServletRequest request // Full HTTP request metadata
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

    @PostMapping("/atm")
    public ResponseEntity<?> createAtmTransaction(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        try {
            // Step 1: Validate request and extract necessary data
            String iban = validateAndGetIbanForAtm(dto);
            User user = principal.getUser();

            // Step 2: Call the business logic in the service layer
            Transaction completedTransaction = transactionService.performAtmOperation(
                    user, iban, dto.getAmount(), dto.getTransactionType()
            );

            // Step 3: Build and return the successful response
            TransactionResponseDto responseDto = buildAtmTransactionResponse(completedTransaction);
            return ResponseEntity.ok(responseDto);

        } catch (IllegalArgumentException ex) {
            // A single catch block for all validation and business logic errors
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Validates the incoming TransactionDto for an ATM operation and returns the relevant IBAN.
     *
     * @throws IllegalArgumentException if validation fails.
     * @return The IBAN to be used for the transaction.
     */
    private String validateAndGetIbanForAtm(TransactionDto dto) {
        if (dto.getAmount() == null || dto.getTransactionType() == null) {
            throw new IllegalArgumentException("Fields 'amount' and 'transactionType' are required.");
        }

        return switch (dto.getTransactionType()) {
            case DEPOSIT -> {
                if (dto.getToIban() == null) {
                    throw new IllegalArgumentException("Field 'toIban' is required for deposit.");
                }
                yield dto.getToIban();
            }
            case WITHDRAWAL -> {
                if (dto.getFromIban() == null) {
                    throw new IllegalArgumentException("Field 'fromIban' is required for withdrawal.");
                }
                yield dto.getFromIban();
            }
            default -> throw new IllegalArgumentException("Invalid transaction type for ATM operation.");
        };
    }

    /**
     * Builds the final TransactionResponseDto from a completed Transaction entity.
     *
     * @param tx The completed transaction from the service.
     * @return A user-facing DTO for the response.
     */
    private TransactionResponseDto buildAtmTransactionResponse(Transaction tx) {
        if (tx.getTransactionType() == TransactionType.DEPOSIT) {
            String toName = tx.getToAccount().getCustomer().getFirstName() + " " + tx.getToAccount().getCustomer().getLastName();
            return new TransactionResponseDto(
                    tx.getTransactionId(), tx.getTransactionType(), tx.getAmount(), tx.getTimestamp(),
                    null, null, tx.getToAccount().getIBAN(), toName, "DEPOSIT"
            );
        } else { // WITHDRAWAL
            String fromName = tx.getFromAccount().getCustomer().getFirstName() + " " + tx.getFromAccount().getCustomer().getLastName();
            return new TransactionResponseDto(
                    tx.getTransactionId(), tx.getTransactionType(), tx.getAmount(), tx.getTimestamp(),
                    tx.getFromAccount().getIBAN(), fromName, null, null, "WITHDRAWAL"
            );
        }
    }
}