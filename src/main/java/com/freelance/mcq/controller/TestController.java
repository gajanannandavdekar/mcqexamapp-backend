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

        if (test.isPremium()) {
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

        return ResponseEntity.ok(questions);
    }
}
