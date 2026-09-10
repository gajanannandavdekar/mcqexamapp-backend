package com.freelance.mcq.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_action_logs")
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "target_email", nullable = false)
    private String targetEmail;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AdminActionLog() {}

    public AdminActionLog(UUID actorId, String actorEmail, UUID targetId, String targetEmail, String actionType, String details) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.targetId = targetId;
        this.targetEmail = targetEmail;
        this.actionType = actionType;
        this.details = details;
    }

    @PrePersist
    void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public UUID getTargetId() { return targetId; }
    public String getTargetEmail() { return targetEmail; }
    public String getActionType() { return actionType; }
    public String getDetails() { return details; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}