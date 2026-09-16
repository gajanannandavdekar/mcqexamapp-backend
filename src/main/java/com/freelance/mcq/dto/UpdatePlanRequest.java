package com.freelance.mcq.dto;

public record UpdatePlanRequest(String title, int durationDays, int priceInPaise, boolean isActive) {}