package com.freelance.mcq.dto;

import java.util.List;

public record CreateQuestionRequest(
    String text,
    List<String> options,
    int correctOptionIndex,
    String explanation,
    String difficulty,
    String topic
) {}