package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class TestUserController {

    private final UserRepository userRepository;

    public TestUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/user-exists")
    public ResponseEntity<Boolean> checkUserExists(@RequestParam String email) {
        return ResponseEntity.ok(userRepository.findByEmail(email).isPresent());
    }

    // ✅ NEW endpoint for showing user data
    @GetMapping("/get-user")
    public ResponseEntity<?> getUserDetails(@RequestParam String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
