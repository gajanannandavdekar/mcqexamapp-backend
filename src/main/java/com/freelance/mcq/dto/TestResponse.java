package com.freelance.mcq.dto;


import java.util.UUID;

public record TestResponse(UUID id, String testKey, String title, int questionsCount, int durationMinutes, boolean isPremium) {}
