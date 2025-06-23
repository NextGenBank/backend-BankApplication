package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.MessageResponseDto;
import com.nextgenbank.backend.model.dto.RegisterRequestDto;
import com.nextgenbank.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto request) {
        userService.registerUser(request);
        return ResponseEntity.ok(new MessageResponseDto("Registration successful"));
    }
}
