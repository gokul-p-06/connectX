package com.connectx;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    @Test
    void passwordShouldBeHashedAndVerifiedCorrectly() {

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String rawPassword = "ConnectX@123";

        String hashedPassword = passwordEncoder.encode(rawPassword);

        assertFalse(hashedPassword.equals(rawPassword));

        assertTrue(
                passwordEncoder.matches(rawPassword, hashedPassword)
        );

        assertFalse(
                passwordEncoder.matches(
                        "WrongPassword@123",
                        hashedPassword
                )
        );
    }
}