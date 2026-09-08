package com.freelance.mcq.dto;


import java.util.List;
import java.util.UUID;

public record QuestionResponse(UUID id, String text, List<String> options, String difficulty, String topic) {}