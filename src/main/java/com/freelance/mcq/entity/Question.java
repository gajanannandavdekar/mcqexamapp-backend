package com.freelance.mcq.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private MockTest test;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "correct_option_index", nullable = false)
    private int correctOptionIndex;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "difficulty",length = 15)
    private String difficulty;

    @Column
    private String topic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Question() {
    }

    public Question(MockTest test, String text, List<String> options, int correctOptionIndex,
                     String explanation, String difficulty, String topic) {
        this.test = test;
        this.text = text;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.topic = topic;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public MockTest getTest() { return test; }
    public void setTest(MockTest test) { this.test = test; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(int correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}