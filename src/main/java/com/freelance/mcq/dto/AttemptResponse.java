package com.freelance.mcq.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AttemptResponse(
	    UUID id,
	    UUID testId,
	    String testKey,
	    String testTitle,
	    UUID subjectId,
	    String subjectTitle,
	    int durationMinutes,
	    OffsetDateTime completedAt,
	    int score,
	    int totalQuestions,
	    double percentage,
	    int correctCount,
	    int incorrectCount,
	    int unansweredCount,
	    Map<String, Object> difficultyStats,
	    List<Map<String, Object>> weakTopics,
	    List<Map<String, Object>> questions,
	    Map<String, Object> responses
	) {}
