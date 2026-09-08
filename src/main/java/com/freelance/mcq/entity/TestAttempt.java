package com.freelance.mcq.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "test_attempts")
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private MockTest test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    @Column(nullable = false)
    private int score;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private int incorrectCount;

    @Column(name = "unanswered_count", nullable = false)
    private int unansweredCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_stats", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> difficultyStats;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weak_topics", nullable = false, columnDefinition = "jsonb")
    private Object weakTopics; // List<Map<String,Object>> at runtime

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> responses;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public TestAttempt() {
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        if (completedAt == null) completedAt = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public MockTest getTest() { return test; }
    public void setTest(MockTest test) { this.test = test; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getIncorrectCount() { return incorrectCount; }
    public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }
    public int getUnansweredCount() { return unansweredCount; }
    public void setUnansweredCount(int unansweredCount) { this.unansweredCount = unansweredCount; }
    public Map<String, Object> getDifficultyStats() { return difficultyStats; }
    public void setDifficultyStats(Map<String, Object> difficultyStats) { this.difficultyStats = difficultyStats; }
    public Object getWeakTopics() { return weakTopics; }
    public void setWeakTopics(Object weakTopics) { this.weakTopics = weakTopics; }
    public Map<String, Object> getResponses() { return responses; }
    public void setResponses(Map<String, Object> responses) { this.responses = responses; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
