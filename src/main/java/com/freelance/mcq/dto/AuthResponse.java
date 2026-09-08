package com.freelance.mcq.dto;


public record AuthResponse(
    String accessToken,
    String refreshToken,
    String email,
    String fullName,
    boolean isPremium
) {}
