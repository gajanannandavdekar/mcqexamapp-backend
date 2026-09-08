package com.freelance.mcq.controller;


import com.freelance.mcq.dto.AuthResponse;
import com.freelance.mcq.dto.ForgotPasswordRequest;
import com.freelance.mcq.dto.LoginRequest;
import com.freelance.mcq.dto.RegisterRequest;
import com.freelance.mcq.dto.ResetPasswordRequest;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.UserRepository;
import com.freelance.mcq.security.JwtService;
import com.freelance.mcq.service.EmailService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService=emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        String strengthError = validatePasswordStrength(req.password());
        if (strengthError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", strengthError));
        }

        User user = new User(
                req.email(),
                passwordEncoder.encode(req.password()),
                req.fullName(),
                User.AuthProvider.LOCAL
        );

        userRepository.save(user);

        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user == null || user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        Claims claims = jwtService.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token type"));
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);

        // Always return the same generic message, whether or not the email exists —
        // prevents using this endpoint to check which emails are registered.
        if (user != null) {
            String otp = generateOtp();
            user.setResetOtpHash(passwordEncoder.encode(otp));
            user.setResetOtpExpiresAt(OffsetDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                emailService.sendOtpEmail(user.getEmail(), otp);
            } catch (Exception e) {
                // Log it, but still return success to the client — don't leak email-sending failures
                System.err.println("Failed to send OTP email: " + e.getMessage());
            }
        }
    
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset code has been sent."));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user == null || user.getResetOtpHash() == null || user.getResetOtpExpiresAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or expired reset code"));
        }

        if (user.getResetOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Reset code has expired"));
        }

        if (!passwordEncoder.matches(req.otp(), user.getResetOtpHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid reset code"));
        }

        String strengthError = validatePasswordStrength(req.newPassword());
        if (strengthError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", strengthError));
        }

        // NEW — reject if new password matches the current one
        if (user.getPasswordHash() != null && passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "New password must be different from your current password"));
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setResetOtpHash(null);
        user.setResetOtpExpiresAt(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // always 6 digits, 100000-999999
        return String.valueOf(otp);
    }
    
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName(), user.isPremium());
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
        return null; // null means valid
    }
    
    
}