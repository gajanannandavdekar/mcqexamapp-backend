package com.freelance.mcq.controller;

import com.freelance.mcq.dto.ChangePasswordRequest;
import com.freelance.mcq.dto.UpdateProfileRequest;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> me(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "isPremium", user.isPremium(),
                "role", user.getRole().name()
        );
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody UpdateProfileRequest req) {
        User user = (User) auth.getPrincipal();

        if (req.fullName() == null || req.fullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name cannot be empty"));
        }

        //User fresh = userRepository.findById(user.getId()).orElseThrow();
        //fresh.setFullName(req.fullName().trim());
        //userRepository.save(fresh);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "isPremium", user.isPremiumActive(),
                "role", user.getRole().name()
        ));
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody ChangePasswordRequest req) {
        User user = (User) auth.getPrincipal();
        User fresh = userRepository.findById(user.getId()).orElseThrow();

        if (fresh.getPasswordHash() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "This account has no password set (signed in via Google)"));
        }

        if (req.currentPassword() == null || !passwordEncoder.matches(req.currentPassword(), fresh.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Current password is incorrect"));
        }

        String strengthError = validatePasswordStrength(req.newPassword());
        if (strengthError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", strengthError));
        }

        if (passwordEncoder.matches(req.newPassword(), fresh.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be different from your current password"));
        }

        fresh.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(fresh);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private String validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Password must contain at least one symbol";
        }
        return null;
    }
}