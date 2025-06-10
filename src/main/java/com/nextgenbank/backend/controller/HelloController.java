package com.nextgenbank.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "✅ Backend is up and running!";
    }
}
