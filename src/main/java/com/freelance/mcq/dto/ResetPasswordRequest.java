package com.freelance.mcq.dto;

public record ResetPasswordRequest(String email, String otp, String newPassword) {

}
