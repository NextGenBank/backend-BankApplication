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
import com.nextgenbank.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {
    private final UserRepository userRepository;
    private final EmployeeService employeeService;
    private final TransactionService transactionService;
    private final UserService userService;

    public EmployeeController(AccountRepository accountRepository,
                              UserRepository userRepository, EmployeeService employeeService, TransactionService transactionService, UserService userService) {
        this.userRepository   = userRepository;
        this.employeeService = employeeService;
        this.transactionService = transactionService;
        this.userService = userService;
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
        List<User> customers = userRepository.findByRoleAndStatus(UserRole.CUSTOMER, status);

        List<UserDto> result = customers.stream()
                .map(UserDto::new)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/approve/{customerId}")
    public ResponseEntity<?> approveCustomer(
            @PathVariable Long customerId,
            Authentication authentication
    ) {
        try {
            String email = authentication.getName();
            User employee = userService.getByEmailOrThrow(email);

            employeeService.approveCustomer(customerId, employee);

            return ResponseEntity.ok(Map.of("message", "Customer approved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error while approving customer."));
        }
    }

    @PutMapping("/reject/{customerId}")
    public ResponseEntity<?> rejectCustomer(@PathVariable Long customerId) {
        try {
            employeeService.rejectCustomer(customerId);
            return ResponseEntity.ok(Map.of("message", "Customer rejected successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error while rejecting customer."));
        }
    }
}