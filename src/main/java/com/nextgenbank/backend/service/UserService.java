package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.RegisterRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        // normalize emaill;it converts email to lowercase to ensure consistency
        String normalizedEmail = email.toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        //pasword check; it compares raw password with encrypted DB password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return user;
    }

    private void validateUniqueFields(RegisterRequestDto request, String normalizedEmail) {
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.findByBsnNumber(request.getBsn()).isPresent()) {
            throw new IllegalArgumentException("BSN already registered");
        }

        if (userRepository.findByPhoneNumber(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }
    }

    public void registerUser(RegisterRequestDto request) {
        // Normalize email
        String normalizedEmail = request.getEmail().toLowerCase();

        validateUniqueFields(request, normalizedEmail);

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

        System.out.println("Registered new user with ID: " + savedUser.getUserId() +
                ". Pending approval by employee.");
    }

    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}