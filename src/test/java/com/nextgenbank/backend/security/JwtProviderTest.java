package com.nextgenbank.backend.security;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class JwtProviderTest {

    private JwtProvider jwtProvider;

    private final String secret = "mysecretkeymysecretkey1234567890"; // 32+ chars

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(secret);
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    @Test
    void generateToken_shouldIncludeEmailAndRole() {
        User user = createTestUser();
        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractAllClaims(token);

        assertEquals("alice@example.com", claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role"));
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        User user = createTestUser();
        String token = jwtProvider.generateToken(user);

        String email = jwtProvider.extractEmail(token);

        assertEquals("alice@example.com", email);
    }

    @Test
    void extractAllClaims_shouldReturnClaims() {
        User user = createTestUser();
        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractAllClaims(token);

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertEquals("CUSTOMER", claims.get("role"));
    }

    @Test
    void token_shouldExpireIn24Hours() {
        User user = createTestUser();
        String token = jwtProvider.generateToken(user);

        Claims claims = jwtProvider.extractAllClaims(token);

        Date now = new Date();
        long diff = claims.getExpiration().getTime() - now.getTime();

        assertTrue(diff <= 86400000 && diff > 86300000, "Token should expire in ~24h");
    }
}
