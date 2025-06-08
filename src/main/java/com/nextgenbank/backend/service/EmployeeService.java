package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final UserRepository userRepository;

    @Autowired
    public EmployeeService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
     * Approve a customer
     */
    @Transactional
    public void approveCustomer(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new RuntimeException("User is not a customer");
        }

        if (customer.getStatus() != UserStatus.PENDING) {
            throw new RuntimeException("Customer is not pending approval");
        }

        customer.setStatus(UserStatus.APPROVED);
        userRepository.save(customer);
    }
}