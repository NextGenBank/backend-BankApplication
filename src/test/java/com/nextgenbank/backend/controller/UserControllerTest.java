package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.security.JwtProvider;
import com.nextgenbank.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    private JwtProvider jwtProvider;
    private UserController userController;
    private UserService userService;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        userService = mock(UserService.class);
        userController = new UserController(jwtProvider, userService);
    }

    @Test
    void getCurrentUser_shouldReturnUserDto() {
        // Arrange
        String testEmail = "alice@example.com";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(testEmail);

        User user = new User();
        user.setUserId(1L);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail(testEmail);
        user.setPhoneNumber("0612345678");
        user.setBsnNumber("123456789");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.APPROVED);

        when(userService.getByEmailOrThrow(testEmail)).thenReturn(user);

        // Act
        ResponseEntity<UserDto> response = userController.getCurrentUser(auth);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Alice", response.getBody().getFirstName());
        assertEquals("Smith", response.getBody().getLastName());
        assertEquals(testEmail, response.getBody().getEmail());
    }

    @Test
    void getCurrentUser_shouldThrowIfUserNotFound() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("notfound@example.com");

        when(userService.getByEmailOrThrow("notfound@example.com"))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userController.getCurrentUser(auth));
    }
}
