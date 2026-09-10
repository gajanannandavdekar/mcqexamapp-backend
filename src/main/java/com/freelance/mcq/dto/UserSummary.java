package com.freelance.mcq.dto;

import java.util.UUID;

public record UserSummary(UUID id, String email, String fullName, boolean isPremium, boolean premiumForever, String premiumExpiresAt, String role) {}