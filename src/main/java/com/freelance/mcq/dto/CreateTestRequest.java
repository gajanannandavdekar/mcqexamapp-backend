package com.freelance.mcq.dto;

public record CreateTestRequest(
	    String subjectKey,
	    String testKey,
	    String title,
	    int durationMinutes,
	    boolean isPremium,
	    boolean isLiveTest,
	    String scheduledStartAt,
	    String scheduledEndAt
	) {}