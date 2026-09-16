package com.freelance.mcq.dto;

public record VerifyOtpRequest(String email, String otp, String deviceName) {}