package com.freelance.mcq.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_push_tokens")
public class DevicePushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "push_token", nullable = false, unique = true)
    private String pushToken;

    @Column
    private String platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    public DevicePushToken() {}

    public DevicePushToken(User user, String pushToken, String platform) {
        this.user = user;
        this.pushToken = pushToken;
        this.platform = platform;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        lastUsedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }
    public String getPlatform() { return platform; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}