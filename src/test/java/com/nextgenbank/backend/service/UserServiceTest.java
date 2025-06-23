package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.RegisterRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private AccountRepository accountRepository;


    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder, accountRepository);
    }

    // Test for registering a user successfully
    @Test
    void registerUser_withValidInput_shouldSaveUser() {
        //fake registration request
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("Alice");
        request.setLastName("Smith");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setBsn("123456789");
        request.setPhone("0612345678");

        //simulate that email, bsn, and phone numbera re not yet in the db
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty()); //the database lookup found nothing
        when(userRepository.findByBsnNumber("123456789")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("0612345678")).thenReturn(Optional.empty());
        //simulate password encoding
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // simulate saving the user returns a user with ID (to prevent NullPointerException)
        User mockSavedUser = new User();
        mockSavedUser.setUserId(1L); // so that savedUser.getUserId() in the service doesn't throw an error
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        //call the actual method im using
        userService.registerUser(request);

        //verify that save() was called once
        verify(userRepository, times(1)).save(any(User.class));
    }


    // Test for duplicate email
    @Test
    void registerUser_withExistingEmail_shouldThrowException() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request)); //() means the function takes no parameters
    }

    // Test for valid login
    @Test
    void authenticate_withValidCredentials_shouldReturnUser() {
        String email = "alice@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        User result = userService.authenticate(email, rawPassword);

        assertEquals(user, result);
    }

    // Test for wrong password
    @Test
    void authenticate_withWrongPassword_shouldThrowBadCredentialsException() {
        String email = "alice@example.com";

        User user = new User();
        user.setEmail(email);
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.authenticate(email, "wrongPassword"));
    }

    // Test for unknown email
    @Test
    void authenticate_withUnknownEmail_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.authenticate("unknown@example.com", "anyPassword"));
    }
}
