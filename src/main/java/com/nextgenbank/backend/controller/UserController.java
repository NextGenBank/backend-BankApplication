package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.security.JwtProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public UserController(UserRepository userRepository, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(new UserDto(user));
    }
}
