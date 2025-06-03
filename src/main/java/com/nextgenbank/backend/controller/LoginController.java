package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.LoginRequestDto;
import com.nextgenbank.backend.model.dto.LoginResponseDto;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.security.JwtProvider;
import com.nextgenbank.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import com.nextgenbank.backend.model.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    public LoginController(UserService userService, JwtProvider jwtProvider) {
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());

        String token = jwtProvider.generateToken(user.getEmail());
        UserDto userDto = new UserDto(user);

        return ResponseEntity.ok(new LoginResponseDto(token, userDto));
    }

}
