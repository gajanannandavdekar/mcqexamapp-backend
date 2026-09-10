package com.freelance.mcq.controller;

import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final UserRepository userRepository;

    public SuperAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/users/{userId}/promote-to-admin")
    public ResponseEntity<?> promote(@PathVariable UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User promoted to admin"));
    }

    @PostMapping("/users/{userId}/demote-admin")
    public ResponseEntity<?> demote(@PathVariable UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            long superAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.SUPER_ADMIN)
                    .count();
            if (superAdminCount <= 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot demote the only remaining super admin"));
            }
        }
        user.setRole(User.Role.USER);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Admin demoted to user"));
    }
}