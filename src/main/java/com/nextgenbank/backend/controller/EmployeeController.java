package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.ErrorResponseDto;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.service.EmployeeService;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {
    private final UserRepository userRepository;
    private final EmployeeService employeeService;
    private final TransactionService transactionService;

    public EmployeeController(AccountRepository accountRepository,
                              UserRepository userRepository, EmployeeService employeeService, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.employeeService = employeeService;
        this.transactionService = transactionService;
    }

    /**
     * Get all customers with pagination
     */
    @GetMapping("/customers")
    public ResponseEntity<Page<UserDto>> getAllCustomers(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        try {
            return ResponseEntity.ok(employeeService.getAllCustomersPaginated(pageable));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching customers: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific customer by ID
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<UserDto> getCustomerById(@PathVariable Long customerId) {
        try {
            return ResponseEntity.ok(employeeService.getCustomerById(customerId));
        } catch (RuntimeException e) {
            throw new RuntimeException("Error fetching customer: " + e.getMessage(), e);
        }
    }

    /**
     * Employee-initiated fund transfer with enhanced security
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> performTransfer(
            @RequestBody TransferRequestDto transferRequest) {
        try {
            // The authorization check is now in the service
            TransactionDto completedTransaction = transactionService.processEmployeeTransfer(transferRequest);
            return ResponseEntity.ok(completedTransaction);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid transfer request: " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new RuntimeException("Authorization error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get customers by status with pagination
     */
    @GetMapping("/status/paginated")
    public ResponseEntity<Page<UserDto>> getCustomersByStatusPaginated(
            @RequestParam UserStatus status,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        try {
            return ResponseEntity.ok(employeeService.getCustomersByStatusPaginated(status, pageable));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching customers by status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get customers by status (non-paginated for backward compatibility)
     */
    @GetMapping("/status")
    public ResponseEntity<List<UserDto>> getAccountsByStatus(@RequestParam UserStatus status) {
        try {
            List<User> customers = userRepository.findByRoleAndStatus(UserRole.CUSTOMER, status);
            List<UserDto> result = customers.stream()
                    .map(UserDto::new)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching customers by status: " + e.getMessage(), e);
        }
    }

    /**
     * Approve a customer account
     */
    @PutMapping("/approve/{customerId}")
    public ResponseEntity<UserDto> approveCustomer(@PathVariable Long customerId) {
        try {
            UserDto approvedCustomer = employeeService.approveCustomer(customerId);
            return ResponseEntity.ok(approvedCustomer);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error approving customer: " + e.getMessage(), e);
        }
    }

    /**
     * Reject a customer account
     */
    @PutMapping("/reject/{customerId}")
    public ResponseEntity<UserDto> rejectCustomer(@PathVariable Long customerId) {
        try {
            UserDto rejectedCustomer = employeeService.rejectCustomer(customerId);
            return ResponseEntity.ok(rejectedCustomer);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error rejecting customer: " + e.getMessage(), e);
        }
    }
}