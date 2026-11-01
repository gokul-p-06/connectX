package com.connectx.controller;

import com.connectx.dto.UserResponse;
import com.connectx.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(
            Authentication authentication) {

        return Map.of(
                "message", "Authenticated successfully",
                "userId", authentication.getName()
        );
    }

    @GetMapping
    public List<UserResponse> getAllUsers(
            Authentication authentication) {

        Long currentUserId =
                Long.valueOf(authentication.getName());

        return userRepository.findAll()
                .stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername()
                ))
                .toList();
    }
}