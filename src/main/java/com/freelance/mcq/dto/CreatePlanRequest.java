package com.freelance.mcq.dto;

public record CreatePlanRequest(String planKey, String title, int durationDays, int priceInPaise) {}