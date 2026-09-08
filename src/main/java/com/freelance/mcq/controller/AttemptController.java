package com.freelance.mcq.controller;

import com.freelance.mcq.dto.AttemptResponse;
import com.freelance.mcq.dto.SubmitAttemptRequest;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.service.AttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submit(Authentication auth, @RequestBody SubmitAttemptRequest request) {
        User user = (User) auth.getPrincipal();
        try {
            AttemptResponse response = attemptService.submitAttempt(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AttemptResponse> list(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return attemptService.listAttemptsForUser(user);
    }

    @GetMapping("/{attemptId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOne(Authentication auth, @PathVariable UUID attemptId) {
        User user = (User) auth.getPrincipal();
        try {
            return ResponseEntity.ok(attemptService.getAttemptDetail(user, attemptId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}