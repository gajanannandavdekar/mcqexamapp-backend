package com.freelance.mcq.service;


import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        Resend resend = new Resend(apiKey);

        String htmlBody = """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                    <h2>Password Reset Code</h2>
                    <p>Use this code to reset your MCQ Exam Prep password. It expires in 10 minutes.</p>
                    <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px; background: #F1F5F9; padding: 16px; text-align: center; border-radius: 8px; margin: 20px 0;">
                        %s
                    </div>
                    <p style="color: #64748B; font-size: 13px;">If you didn't request this, you can safely ignore this email.</p>
                </div>
                """.formatted(otp);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Your password reset code")
                .html(htmlBody)
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }
}
