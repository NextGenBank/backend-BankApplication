package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public CustomerService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Get customer by ID
     */
    public User getCustomerById(Long customerId) {
        return userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    /**
     * Get all accounts for a customer
     */
    public List<Account> getCustomerAccounts(Long customerId) {
        User customer = getCustomerById(customerId);
        return accountRepository.findByCustomer(customer);
    }
}