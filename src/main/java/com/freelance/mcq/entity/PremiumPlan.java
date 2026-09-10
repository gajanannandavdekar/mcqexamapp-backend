package com.freelance.mcq.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "premium_plans")
public class PremiumPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_key", nullable = false, unique = true)
    private String planKey;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "price_in_paise", nullable = false)
    private int priceInPaise;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PremiumPlan() {}

    @PrePersist
    void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public String getPlanKey() { return planKey; }
    public void setPlanKey(String planKey) { this.planKey = planKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public int getPriceInPaise() { return priceInPaise; }
    public void setPriceInPaise(int priceInPaise) { this.priceInPaise = priceInPaise; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}