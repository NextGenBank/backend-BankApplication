package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {
    private final UserRepository userRepository;
    private final EmployeeService employeeService;

    public EmployeeController(AccountRepository accountRepository,
                              UserRepository userRepository, EmployeeService employeeService) {
        this.userRepository   = userRepository;
        this.employeeService = employeeService;
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