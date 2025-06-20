package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Get all customers in the system (non-paginated version for backwards compatibility)
     */
    public List<UserDto> getAllCustomers() {
        return userRepository.findByRole(UserRole.CUSTOMER)
                .stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all customers with pagination support
     */
    public Page<UserDto> getAllCustomersPaginated(Pageable pageable) {
        Page<User> customerPage = userRepository.findByRole(UserRole.CUSTOMER, pageable);
        return customerPage.map(UserDto::new);
    }

    /**
     * Get customers by status with pagination
     */
    public Page<UserDto> getCustomersByStatusPaginated(UserStatus status, Pageable pageable) {
        Page<User> customerPage = userRepository.findByRoleAndStatus(UserRole.CUSTOMER, status, pageable);
        return customerPage.map(UserDto::new);
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
     * @return The approved customer DTO
     */
    @Transactional
    public UserDto approveCustomer(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + customerId));

        if (user.getStatus() == UserStatus.APPROVED) {
            throw new IllegalStateException("User is already approved.");
        }

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customers can be approved.");
        }

        user.setStatus(UserStatus.APPROVED);
        User savedUser = userRepository.save(user);

        // Create IBAN accounts
        accountService.createAccountsForUser(savedUser);
        
        return new UserDto(savedUser);
    }

    /**
     * Rejects a customer.
     * @return The rejected customer DTO
     */
    @Transactional
    public UserDto rejectCustomer(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + customerId));

        if (user.getStatus() == UserStatus.REJECTED) {
            throw new IllegalStateException("User is already rejected.");
        }

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customers can be rejected.");
        }

        user.setStatus(UserStatus.REJECTED);
        User savedUser = userRepository.save(user);
        
        return new UserDto(savedUser);
    }
}