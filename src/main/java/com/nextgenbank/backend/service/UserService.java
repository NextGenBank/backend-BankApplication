package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.RegisterRequestDto;
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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        userRepository.save(user);
    }
}
