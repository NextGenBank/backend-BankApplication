package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.RegisterRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.util.IbanGenerator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;

    }

    public User authenticate(String email, String password) {
        // Normalize email
        String normalizedEmail = email.toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return user;
    }

    public void registerUser(RegisterRequestDto request) {
        // Normalize email
        String normalizedEmail = request.getEmail().toLowerCase();

        // Check if email already exists
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check if BSN already exists
        if (userRepository.findByBsnNumber(request.getBsn()).isPresent()) {
            throw new IllegalArgumentException("BSN already registered");
        }

        // Check if phone number already exists
        if (userRepository.findByPhoneNumber(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(normalizedEmail); // Save normalized email
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBsnNumber(request.getBsn());
        user.setPhoneNumber(request.getPhone());
        user.setRole(UserRole.CUSTOMER); // default role
        user.setStatus(UserStatus.PENDING); // default status
        user.setCreatedAt(LocalDateTime.now());

        // Save the user first to get an ID
        User savedUser = userRepository.save(user);
        
        // Create checking and savings accounts for the new user
        // Default transfer limit is 1000
        String transferLimit = "1000";
        createAccount(savedUser, AccountType.CHECKING, transferLimit);
        createAccount(savedUser, AccountType.SAVINGS, transferLimit);
        
        System.out.println("Registered new user with ID: " + savedUser.getUserId() + 
                           " and created accounts with transfer limit: " + transferLimit);
    }

    private void createAccount(User customer, AccountType accountType, String transferLimitStr) {
        // Get a default employee for account creation
        User employee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.EMPLOYEE)
                .findFirst()
                .orElse(null);

        Account account = new Account();
        account.setIBAN(IbanGenerator.generateIban());
        account.setCustomer(customer);
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO); // Start with zero balance
        account.setCreatedBy(employee); // Set the employee who created the account

        // Set transfer limit if provided
        if (transferLimitStr != null && !transferLimitStr.isEmpty()) {
            try {
                BigDecimal transferLimit = new BigDecimal(transferLimitStr);
                account.setAbsoluteTransferLimit(transferLimit);
            } catch (NumberFormatException e) {
                System.out.println("Invalid transfer limit: " + transferLimitStr);
                account.setAbsoluteTransferLimit(BigDecimal.ZERO);
            }
        } else {
            account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        }

        account.setDailyTransferAmount(BigDecimal.ZERO);
        account.setCreatedAt(LocalDateTime.now());

        Account savedAccount = accountRepository.save(account);
        System.out.println("Created " + accountType + " account with IBAN: " + savedAccount.getIBAN());
    }
}