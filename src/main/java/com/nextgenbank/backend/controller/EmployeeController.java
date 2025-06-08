package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.service.EmployeeService;
import com.nextgenbank.backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TransactionService transactionService;

    @Autowired
    public EmployeeController(EmployeeService employeeService, TransactionService transactionService) {
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

    /**
     * Approve a customer
     */
    @PutMapping("/approve/{customerId}")
    public ResponseEntity<?> approveCustomer(@PathVariable Long customerId) {
        try {
            employeeService.approveCustomer(customerId);
            return ResponseEntity.ok(Map.of("message", "Customer approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}