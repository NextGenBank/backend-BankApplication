package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.LoginRequestDto;
import com.nextgenbank.backend.model.dto.LoginResponseDto;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.security.JwtProvider;
import com.nextgenbank.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoginControllerTest {

    private UserService userService;
    private JwtProvider jwtProvider;
    private LoginController loginController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtProvider = mock(JwtProvider.class);
        loginController = new LoginController(userService, jwtProvider);
    }

    @Test
    void login_shouldReturnTokenAndUserDto() {
        // Arrange
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setFirstName("Alice");
        mockUser.setLastName("Smith");
        mockUser.setEmail("test@example.com");
        mockUser.setPhoneNumber("0612345678");
        mockUser.setBsnNumber("123456789");
        mockUser.setRole(UserRole.CUSTOMER);
        mockUser.setStatus(UserStatus.APPROVED);

        when(userService.authenticate("test@example.com", "password123")).thenReturn(mockUser);
        when(jwtProvider.generateToken(mockUser)).thenReturn("mock-jwt-token");

        // Act
        ResponseEntity<?> response = loginController.login(loginRequest);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertInstanceOf(LoginResponseDto.class, response.getBody());

        LoginResponseDto body = (LoginResponseDto) response.getBody();
        assertEquals("mock-jwt-token", body.getToken());
        assertEquals("Alice", body.getUser().getFirstName());
        assertEquals("test@example.com", body.getUser().getEmail());
    }
}
