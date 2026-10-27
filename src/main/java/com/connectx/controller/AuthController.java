package com.connectx.controller;

import com.connectx.dto.LoginRequest;
import com.connectx.dto.RegisterRequest;
import com.connectx.entity.User;
import com.connectx.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        Map<String, Object> response = Map.of(
                "message", "User registered successfully",
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.login(request);

        Map<String, Object> response = Map.of(
                "message", "Login successful",
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}