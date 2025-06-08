package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.service.EmployeeService;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {
    private final UserRepository userRepository;
    private final EmployeeService employeeService;
    private final TransactionService transactionService;

    public EmployeeController(AccountRepository accountRepository,
                              UserRepository userRepository, EmployeeService employeeService, TransactionService transactionService) {
        this.userRepository   = userRepository;
        this.employeeService = employeeService;
        this.transactionService = transactionService;
    }

    /**
     * Get all customers
     */
    @GetMapping("/customers")
    public ResponseEntity<List<UserDto>> getAllCustomers() {
        return ResponseEntity.ok(employeeService.getAllCustomers());
    }

    /**
     * Get a specific customer by ID
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<UserDto> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(employeeService.getCustomerById(customerId));
    }

    /**
     * Employee-initiated fund transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transferFunds(@RequestBody TransferRequestDto transferRequestDto) {
        try {
            return ResponseEntity.ok(Map.of(
                    "message", "Transfer completed successfully",
                    "transaction", transactionService.transferFunds(transferRequestDto)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get role = CUSTOMER with the requested status
    @GetMapping("/status")
    public ResponseEntity<List<UserDto>> getAccountsByStatus(@RequestParam UserStatus status) {
        List<User> customers = userRepository.findByRoleAndStatus(UserRole.CUSTOMER, status);

        List<UserDto> result = customers.stream()
                .map(UserDto::new)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/approve/{customerId}")
    public ResponseEntity<?> approveCustomer(@PathVariable Long customerId) {
        employeeService.approveCustomer(customerId);
        return ResponseEntity.ok(Map.of("message", "Customer approved successfully"));
    }

    @PutMapping("/reject/{customerId}")
    public ResponseEntity<?> rejectCustomer(@PathVariable Long customerId) {
        employeeService.rejectCustomer(customerId);
        return ResponseEntity.ok(Map.of("message", "Customer rejected successfully"));
    }
}