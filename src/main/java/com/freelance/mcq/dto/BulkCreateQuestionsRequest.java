package com.freelance.mcq.dto;

import java.util.List;

public record BulkCreateQuestionsRequest(List<CreateQuestionRequest> questions) {}