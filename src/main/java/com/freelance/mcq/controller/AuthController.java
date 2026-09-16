package com.freelance.mcq.controller;


import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelance.mcq.dto.AuthResponse;
import com.freelance.mcq.dto.ForgotPasswordRequest;
import com.freelance.mcq.dto.LoginRequestWithDevice;
import com.freelance.mcq.dto.RegisterRequest;
import com.freelance.mcq.dto.ResetPasswordRequest;
import com.freelance.mcq.dto.VerifyOtpRequest;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.UserRepository;
import com.freelance.mcq.security.JwtService;
import com.freelance.mcq.service.EmailService;
import com.freelance.mcq.service.RateLimitService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,EmailService emailService,RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService=emailService;
        this.rateLimitService=rateLimitService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req,HttpServletRequest request) {
        
    	if (!rateLimitService.allowRegister(getClientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please try again in a few minutes."));
        }
    	
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

        return ResponseEntity.ok(buildAuthResponse(user,req.deviceName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestWithDevice req,HttpServletRequest request) {
    	if (!rateLimitService.allowLogin(getClientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please try again in a few minutes."));
        }
        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user != null && user.isCurrentlyLocked()) {
            long minutesLeft = java.time.Duration.between(OffsetDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "ACCOUNT_LOCKED",
                    "message", "Too many failed attempts. Try again in " + minutesLeft + " minute(s)."
            ));
        }
        
        
        if (user == null || user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
        	if (user != null) {
                handleFailedLogin(user);
            }
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        
        if (user.getFailedLoginAttempts() > 0) {
        	user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
        
     // Successful password verification passed the checks above.
     // Now branch: admin-tier accounts need a second factor before real tokens are issued.
     if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
         String otp = generateOtp(); // reuses the same method already built for forgot-password
         user.setResetOtpHash(passwordEncoder.encode(otp)); // reusing the same OTP storage fields
         user.setResetOtpExpiresAt(OffsetDateTime.now().plusMinutes(10));
         userRepository.save(user);

         try {
             emailService.sendOtpEmail(user.getEmail(), otp);
         } catch (Exception e) {
             System.err.println("Failed to send 2FA OTP email: " + e.getMessage());
         }

         return ResponseEntity.ok(Map.of(
                 "requires2FA", true,
                 "email", user.getEmail()
         ));
     }


        
        if (user.getCurrentSessionId() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "ALREADY_LOGGED_IN",
                    "message", "This account is already signed in on another device.",
                    "deviceName", user.getCurrentSessionDevice() != null ? user.getCurrentSessionDevice() : "Unknown device",
                    "lastActive", user.getCurrentSessionLastActive() != null ? user.getCurrentSessionLastActive().toString() : null
            ));
        }

        return ResponseEntity.ok(buildAuthResponse(user, req.deviceName()));
    }

    @PostMapping("/login-force")
    public ResponseEntity<?> loginForce(@Valid @RequestBody LoginRequestWithDevice req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user == null || user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        // Deliberately overwrite the existing session — this is the explicit override
        return ResponseEntity.ok(buildAuthResponse(user, req.deviceName()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }

        Claims claims = jwtService.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token type"));
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        String tokenSessionId = claims.get("sid", String.class);
        if (tokenSessionId == null || !tokenSessionId.equals(user.getCurrentSessionId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "SESSION_INVALIDATED", "message", "Your account was signed in on another device."));
        }

        user.setCurrentSessionLastActive(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), tokenSessionId);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), tokenSessionId);
        return ResponseEntity.ok(new AuthResponse(accessToken, newRefreshToken, user.getEmail(), user.getFullName(), user.isPremium()));
    }


    	


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req,HttpServletRequest request) {
    	if (!rateLimitService.allowForgotPassword(getClientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please try again later."));
        }
        User user = userRepository.findByEmail(req.email()).orElse(null);

        // Always return the same generic message, whether or not the email exists —
        // prevents using this endpoint to check which emails are registered.
        if (user != null) {
            String otp = generateOtp();
            user.setResetOtpHash(passwordEncoder.encode(otp));
            user.setResetOtpExpiresAt(OffsetDateTime.now().plusMinutes(10));
            user.setOtpAttempts(0);
            userRepository.save(user);

            try {
                emailService.sendOtpEmail(user.getEmail(), otp);
            } catch (Exception e) {
                
                System.err.println("Failed to send OTP email: " + e.getMessage());
            }
        }
    
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset code has been sent."));
    }
    
    @PostMapping("/reset-password")
    	
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req,HttpServletRequest request) {
    	if (!rateLimitService.allowResetPassword(getClientIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many attempts. Please try again in a few minutes."));
        }
    	
        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user == null || user.getResetOtpHash() == null || user.getResetOtpExpiresAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or expired reset code"));
        }

        if (user.getResetOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Reset code has expired"));
        }

        
        if (user.getOtpAttempts() >= 5) {
            user.setResetOtpHash(null);
            user.setResetOtpExpiresAt(null);
            user.setOtpAttempts(0);
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Too many incorrect attempts. Please request a new code."));
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

    
    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@RequestBody VerifyOtpRequest req, HttpServletRequest request) {
        if (!rateLimitService.allowResetPassword(getClientIp(request))) { // reuse the same limiter — same abuse pattern (OTP brute-force)
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many attempts. Please try again in a few minutes."));
        }

        User user = userRepository.findByEmail(req.email()).orElse(null);

        if (user == null || user.getResetOtpHash() == null || user.getResetOtpExpiresAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or expired code"));
        }

        if (user.getResetOtpExpiresAt().isBefore(OffsetDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Code has expired"));
        }

        
        if (user.getOtpAttempts() >= 5) {
            user.setResetOtpHash(null);
            user.setResetOtpExpiresAt(null);
            user.setOtpAttempts(0);
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Too many incorrect attempts. Please log in again to receive a new code."));
        }
        
        
        
        if (!passwordEncoder.matches(req.otp(), user.getResetOtpHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid code"));
        }

        // OTP correct — clear it (single-use) and complete login properly
        user.setResetOtpHash(null);
        user.setResetOtpExpiresAt(null);
        user.setOtpAttempts(0);
        userRepository.save(user);

        if (user.getCurrentSessionId() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "ALREADY_LOGGED_IN",
                    "message", "This account is already signed in on another device.",
                    "deviceName", user.getCurrentSessionDevice() != null ? user.getCurrentSessionDevice() : "Unknown device",
                    "lastActive", user.getCurrentSessionLastActive() != null ? user.getCurrentSessionLastActive().toString() : null
            ));
        }

        return ResponseEntity.ok(buildAuthResponse(user, req.deviceName()));
    }
    
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(Authentication auth) {
        User user = (User) auth.getPrincipal();
        User freshUser = userRepository.findById(user.getId()).orElseThrow();
        freshUser.setCurrentSessionId(null);
        freshUser.setCurrentSessionDevice(null);
        freshUser.setCurrentSessionLastActive(null);
        userRepository.save(freshUser);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
    
    
    
    
    
    
    
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // always 6 digits, 100000-999999
        return String.valueOf(otp);
    }
    
    private AuthResponse buildAuthResponse(User user, String deviceName) {
        String sessionId = java.util.UUID.randomUUID().toString();
        user.setCurrentSessionId(sessionId);
        user.setCurrentSessionDevice(deviceName != null ? deviceName : "Unknown device");
        user.setCurrentSessionLastActive(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), sessionId);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), sessionId);
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
    
    
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= 5) {
            int lockoutCount = user.getLockoutCount() + 1;
            user.setLockoutCount(lockoutCount);

            Duration lockDuration = switch (Math.min(lockoutCount, 3)) {
                case 1 -> Duration.ofMinutes(15);
                case 2 -> Duration.ofHours(1);
                default -> Duration.ofHours(24);
            };

            user.setLockedUntil(OffsetDateTime.now().plus(lockDuration));
            user.setFailedLoginAttempts(0); // reset counter, lockout itself is now the active deterrent
        }

        userRepository.save(user);
    }
    
    
    
    
}