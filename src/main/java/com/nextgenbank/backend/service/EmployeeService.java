package com.nextgenbank.backend.service;

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
    public void approveCustomer(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);
    }

    /**
     * Reject a customer
     */
    public void rejectCustomer(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
    }
}