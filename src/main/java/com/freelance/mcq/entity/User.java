package com.freelance.mcq.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @Column(name = "premium_since")
    private OffsetDateTime premiumSince;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "reset_otp_hash")
    private String resetOtpHash;

    @Column(name = "reset_otp_expires_at")
    private OffsetDateTime resetOtpExpiresAt;
    
    public enum Role { USER, ADMIN }
    
    
    public User() {
    }

    public User(String email, String passwordHash, String fullName, AuthProvider authProvider) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.authProvider = authProvider;
        this.isPremium = false;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public OffsetDateTime getPremiumSince() { return premiumSince; }
    public void setPremiumSince(OffsetDateTime premiumSince) { this.premiumSince = premiumSince; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public enum AuthProvider { LOCAL, GOOGLE }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public String getResetOtpHash() { return resetOtpHash; }
    public void setResetOtpHash(String resetOtpHash) { this.resetOtpHash = resetOtpHash; }
    public OffsetDateTime getResetOtpExpiresAt() { return resetOtpExpiresAt; }
    public void setResetOtpExpiresAt(OffsetDateTime resetOtpExpiresAt) { this.resetOtpExpiresAt = resetOtpExpiresAt; }
    
    
}