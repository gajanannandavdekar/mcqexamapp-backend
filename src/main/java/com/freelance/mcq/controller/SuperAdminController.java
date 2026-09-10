package com.freelance.mcq.controller;

import com.freelance.mcq.entity.AdminActionLog;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.AdminActionLogRepository;
import com.freelance.mcq.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    public SuperAdminController(UserRepository userRepository,AdminActionLogRepository adminActionLogRepository) {
        this.userRepository = userRepository;
        this.adminActionLogRepository=adminActionLogRepository;
    }


    @PostMapping("/users/{userId}/promote-to-admin")
    public ResponseEntity<?> promote(Authentication auth, @PathVariable UUID userId) {
        User actor = (User) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);

        adminActionLogRepository.save(new AdminActionLog(
                actor.getId(), actor.getEmail(), user.getId(), user.getEmail(), "PROMOTE_ADMIN", null
        ));

        return ResponseEntity.ok(Map.of("message", "User promoted to admin"));
    }

    @PostMapping("/users/{userId}/demote-admin")
    public ResponseEntity<?> demote(Authentication auth, @PathVariable UUID userId) {
        User actor = (User) auth.getPrincipal();
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

        adminActionLogRepository.save(new AdminActionLog(
                actor.getId(), actor.getEmail(), user.getId(), user.getEmail(), "DEMOTE_ADMIN", null
        ));

        return ResponseEntity.ok(Map.of("message", "Admin demoted to user"));
    }
    
    
 
    @GetMapping("/logs")
    public List<Map<String, Object>> getLogs() {
        return adminActionLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(log -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", log.getId());
                    m.put("actorEmail", log.getActorEmail());
                    m.put("targetEmail", log.getTargetEmail());
                    m.put("actionType", log.getActionType());
                    m.put("details", log.getDetails());
                    m.put("createdAt", log.getCreatedAt().toString());
                    return m;
                })
                .toList();
    }
    
    
    
    
    
}