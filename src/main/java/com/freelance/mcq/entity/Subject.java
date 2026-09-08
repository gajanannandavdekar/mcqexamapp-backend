package com.freelance.mcq.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subject_key", nullable = false, unique = true)
    private String subjectKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String icon;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockTest> tests = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Subject() {
    }

    public Subject(String subjectKey, String title, String icon) {
        this.subjectKey = subjectKey;
        this.title = title;
        this.icon = icon;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getSubjectKey() { return subjectKey; }
    public void setSubjectKey(String subjectKey) { this.subjectKey = subjectKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public List<MockTest> getTests() { return tests; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
