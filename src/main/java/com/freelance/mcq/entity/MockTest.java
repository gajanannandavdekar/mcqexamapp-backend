package com.freelance.mcq.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mock_tests")
public class MockTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "test_key", nullable = false, unique = true)
    private String testKey;

    @Column(nullable = false)
    private String title;

    @Column(name = "questions_count", nullable = false)
    private int questionsCount;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;
    
    public MockTest() {
    }

    public MockTest(Subject subject, String testKey, String title, int questionsCount, int durationMinutes, boolean isPremium) {
        this.subject = subject;
        this.testKey = testKey;
        this.title = title;
        this.questionsCount = questionsCount;
        this.durationMinutes = durationMinutes;
        this.isPremium = isPremium;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getTestKey() { return testKey; }
    public void setTestKey(String testKey) { this.testKey = testKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getQuestionsCount() { return questionsCount; }
    public void setQuestionsCount(int questionsCount) { this.questionsCount = questionsCount; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public List<Question> getQuestions() { return questions; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }
    
}