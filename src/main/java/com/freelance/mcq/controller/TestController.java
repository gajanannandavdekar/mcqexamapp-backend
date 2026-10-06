package com.freelance.mcq.controller;


import com.freelance.mcq.dto.QuestionResponse;
import com.freelance.mcq.entity.MockTest;
import com.freelance.mcq.repository.MockTestRepository;
import com.freelance.mcq.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;

    public TestController(MockTestRepository mockTestRepository, QuestionRepository questionRepository) {
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
    }
    
    @GetMapping("/{testKey}/questions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getQuestions(@PathVariable String testKey) {
        MockTest test = mockTestRepository.findByTestKey(testKey).orElse(null);
        if (test == null || !test.isPublished()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
        }

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        
        
        if (!isAdmin && test.isLiveTest()) {
            String status = test.getLiveStatus();
            if (!"ACTIVE".equals(status)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", status.equals("UPCOMING") ? "This test hasn't started yet" : "This test has ended",
                        "liveStatus", status,
                        "scheduledStartAt", test.getScheduledStartAt() != null ? test.getScheduledStartAt().toString() : null,
                        "scheduledEndAt", test.getScheduledEndAt() != null ? test.getScheduledEndAt().toString() : null
                ));
            }
        }

        if (!isAdmin && test.isPremium()) {
            boolean isPremiumUser = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PREMIUM"));

            if (!isPremiumUser) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "This test requires a premium subscription"));
            }
        }

        List<QuestionResponse> questions = questionRepository.findByTestId(test.getId()).stream()
                .map(q -> new QuestionResponse(q.getId(), q.getText(), q.getOptions(), q.getDifficulty(), q.getTopic()))
                .toList();

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("questions", questions);
        response.put("isLiveTest", test.isLiveTest());
        response.put("scheduledEndAt", test.getScheduledEndAt() != null ? test.getScheduledEndAt().toString() : null);

        return ResponseEntity.ok(response);
    }
    
    
 
    @GetMapping("/live")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getLiveTests() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime horizon = now.plusHours(24);

        return mockTestRepository.findPublishedLiveTestsWithSubject().stream()
                .filter(t -> {
                    String status = t.getLiveStatus();
                    if ("ACTIVE".equals(status)) return true;
                    return "UPCOMING".equals(status)
                            && t.getScheduledStartAt() != null
                            && t.getScheduledStartAt().isBefore(horizon);
                })
                .sorted(Comparator.comparing((MockTest t) ->
                        t.getScheduledStartAt() != null ? t.getScheduledStartAt() : now))
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("testKey", t.getTestKey());
                    m.put("title", t.getTitle());
                    m.put("subjectKey", t.getSubject().getSubjectKey());
                    m.put("subjectTitle", t.getSubject().getTitle());
                    m.put("durationMinutes", t.getDurationMinutes());
                    m.put("isPremium", t.isPremium());
                    m.put("scheduledStartAt", t.getScheduledStartAt() != null ? t.getScheduledStartAt().toString() : null);
                    m.put("scheduledEndAt", t.getScheduledEndAt() != null ? t.getScheduledEndAt().toString() : null);
                    return m;
                })
                .toList();
    }
    
    
    
    
    
    
    
    
}
