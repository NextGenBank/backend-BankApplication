package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final UserRepository userRepository;
    private final AccountService accountService;

    @Autowired
    public EmployeeService(UserRepository userRepository, AccountService accountService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
    }

    /**
     * Get all customers in the system
     */
    public List<UserDto> getAllCustomers() {
        return userRepository.findByRole(UserRole.CUSTOMER)
                .stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific customer by ID
     */
    public UserDto getCustomerById(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new RuntimeException("User is not a customer");
        }

        return new UserDto(customer);
    }

    /**
     * Approves a customer and creates their accounts.
     */
    public void approveCustomer(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + customerId));

        if (user.getStatus() == UserStatus.APPROVED) {
            throw new IllegalStateException("User is already approved.");
        }

        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);

        // Create IBAN accounts
        accountService.createAccountsForUser(user);
    }

    /**
     * Rejects a customer.
     */
    public void rejectCustomer(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + customerId));

        if (user.getStatus() == UserStatus.REJECTED) {
            throw new IllegalStateException("User is already rejected.");
        }

        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
    }
}