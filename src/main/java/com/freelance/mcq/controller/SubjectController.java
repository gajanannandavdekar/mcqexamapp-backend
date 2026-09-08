package com.freelance.mcq.controller;

import com.freelance.mcq.dto.SubjectResponse;
import com.freelance.mcq.dto.TestResponse;
import com.freelance.mcq.entity.Subject;
import com.freelance.mcq.repository.MockTestRepository;
import com.freelance.mcq.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final MockTestRepository mockTestRepository;

    public SubjectController(SubjectRepository subjectRepository, MockTestRepository mockTestRepository) {
        this.subjectRepository = subjectRepository;
        this.mockTestRepository = mockTestRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SubjectResponse> listSubjects() {
        return subjectRepository.findAll().stream()
                .map(s -> new SubjectResponse(
                        s.getId(),
                        s.getSubjectKey(),
                        s.getTitle(),
                        s.getIcon(),
                        (int) mockTestRepository.countBySubjectId(s.getId())
                ))
                .toList();
    }

    @GetMapping("/{subjectKey}/tests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listTestsForSubject(@PathVariable String subjectKey) {
        Subject subject = subjectRepository.findBySubjectKey(subjectKey).orElse(null);
        if (subject == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
        }

        List<TestResponse> tests = mockTestRepository.findBySubjectId(subject.getId()).stream()
                .map(t -> new TestResponse(t.getId(), t.getTestKey(), t.getTitle(), t.getQuestionsCount(), t.getDurationMinutes(), t.isPremium()))
                .toList();

        return ResponseEntity.ok(tests);
    }
}