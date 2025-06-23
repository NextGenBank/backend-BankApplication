package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.dto.MessageResponseDto;
import com.nextgenbank.backend.model.dto.RegisterRequestDto;
import com.nextgenbank.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterControllerTest {  // Removed 'public' modifier (not needed for tests)

    private UserService userService;
    private RegisterController registerController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        registerController = new RegisterController(userService);
    }

    @Test
    void register_shouldCallUserServiceAndReturnSuccess() {
        // Arrange
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("Alice");
        request.setLastName("Smith");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setBsn("123456789");
        request.setPhone("0612345678");

        // Act
        ResponseEntity<?> response = registerController.register(request);

        // Assert
        verify(userService, times(1)).registerUser(request);
        assertEquals(200, response.getStatusCodeValue());

        // Check if response body is MessageResponseDto and has correct message
        assertTrue(response.getBody() instanceof MessageResponseDto);
        assertEquals("Registration successful", ((MessageResponseDto) response.getBody()).getMessage());
    }
}