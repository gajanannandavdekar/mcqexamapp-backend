package com.freelance.mcq.dto;

import java.util.Map;

public record SubmitAttemptRequest(
    String testKey,
    Map<String, ResponseItem> responses
) {
    public record ResponseItem(Integer selectedOptionIndex, boolean isMarkedForReview) {}
}