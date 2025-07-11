package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.security.JwtProvider;
import com.nextgenbank.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final JwtProvider jwtProvider;
    private final UserService userService;

    public UserController(JwtProvider jwtProvider, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getByEmailOrThrow(email);
        return ResponseEntity.ok(new UserDto(user));
    }
}