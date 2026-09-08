package com.freelance.mcq.dto;


import java.util.UUID;

public record SubjectResponse(UUID id, String subjectKey, String title, String icon, int testCount) {}
