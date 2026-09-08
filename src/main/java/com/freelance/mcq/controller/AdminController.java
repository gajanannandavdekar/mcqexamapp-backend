package com.freelance.mcq.controller;

import com.freelance.mcq.dto.*;
import com.freelance.mcq.entity.MockTest;
import com.freelance.mcq.entity.Question;
import com.freelance.mcq.entity.Subject;
import com.freelance.mcq.repository.MockTestRepository;
import com.freelance.mcq.repository.QuestionRepository;
import com.freelance.mcq.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
//Add these imports:
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;



@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SubjectRepository subjectRepository;
    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;

    public AdminController(SubjectRepository subjectRepository, MockTestRepository mockTestRepository,
                            QuestionRepository questionRepository) {
        this.subjectRepository = subjectRepository;
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
    }

    // ---------- SUBJECTS ----------

    @PostMapping("/subjects")
    public ResponseEntity<?> createSubject(@RequestBody CreateSubjectRequest req) {
        if (subjectRepository.findBySubjectKey(req.subjectKey()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "subjectKey already exists"));
        }
        Subject subject = new Subject(req.subjectKey(), req.title(), req.icon());
        subjectRepository.save(subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", subject.getId(), "subjectKey", subject.getSubjectKey()));
    }

    @PutMapping("/subjects/{subjectKey}")
    public ResponseEntity<?> updateSubject(@PathVariable String subjectKey, @RequestBody CreateSubjectRequest req) {
        Subject subject = subjectRepository.findBySubjectKey(subjectKey).orElse(null);
        if (subject == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
        }
        subject.setTitle(req.title());
        subject.setIcon(req.icon());
        subjectRepository.save(subject);
        return ResponseEntity.ok(Map.of("id", subject.getId(), "subjectKey", subject.getSubjectKey()));
    }

    @DeleteMapping("/subjects/{subjectKey}")
    public ResponseEntity<?> deleteSubject(@PathVariable String subjectKey) {
        Subject subject = subjectRepository.findBySubjectKey(subjectKey).orElse(null);
        if (subject == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
        }
        subjectRepository.delete(subject); // cascades to tests/questions per your schema's ON DELETE CASCADE
        return ResponseEntity.noContent().build();
    }

    // ---------- TESTS ----------

    @PostMapping("/tests")
    public ResponseEntity<?> createTest(@RequestBody CreateTestRequest req) {
        Subject subject = subjectRepository.findBySubjectKey(req.subjectKey()).orElse(null);
        if (subject == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
        }
        if (mockTestRepository.findByTestKey(req.testKey()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "testKey already exists"));
        }

        MockTest test = new MockTest(subject, req.testKey(), req.title(), 0, req.durationMinutes(), req.isPremium());
        mockTestRepository.save(test);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", test.getId(), "testKey", test.getTestKey()));
    }

    @PutMapping("/tests/{testKey}")
    public ResponseEntity<?> updateTest(@PathVariable String testKey, @RequestBody CreateTestRequest req) {
        MockTest test = mockTestRepository.findByTestKey(testKey).orElse(null);
        if (test == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
        }
        test.setTitle(req.title());
        test.setDurationMinutes(req.durationMinutes());
        test.setPremium(req.isPremium());
        mockTestRepository.save(test);
        return ResponseEntity.ok(Map.of("id", test.getId(), "testKey", test.getTestKey()));
    }

    @DeleteMapping("/tests/{testKey}")
    public ResponseEntity<?> deleteTest(@PathVariable String testKey) {
        MockTest test = mockTestRepository.findByTestKey(testKey).orElse(null);
        if (test == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
        }
        mockTestRepository.delete(test);
        return ResponseEntity.noContent().build();
    }

    // ---------- QUESTIONS ----------

    @PostMapping("/tests/{testKey}/questions")
    public ResponseEntity<?> addQuestions(@PathVariable String testKey, @RequestBody BulkCreateQuestionsRequest req) {
        MockTest test = mockTestRepository.findByTestKey(testKey).orElse(null);
        if (test == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
        }

        List<Question> saved = req.questions().stream()
                .map(q -> questionRepository.save(new Question(
                        test, q.text(), q.options(), q.correctOptionIndex(),
                        q.explanation(), q.difficulty(), q.topic()
                )))
                .toList();

        // Keep questionsCount in sync
        test.setQuestionsCount(questionRepository.findByTestId(test.getId()).size());
        mockTestRepository.save(test);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("added", saved.size()));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable UUID questionId) {
        if (!questionRepository.existsById(questionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found"));
        }
        questionRepository.deleteById(questionId);
        return ResponseEntity.noContent().build();
    }
    
 

    @PostMapping("/tests/{testKey}/questions/upload-csv")
    public ResponseEntity<?> uploadQuestionsCsv(@PathVariable String testKey, @RequestParam("file12") MultipartFile file) {
        MockTest test = mockTestRepository.findByTestKey(testKey).orElse(null);
        if (test == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
        }

        List<Map<String, Object>> errors = new ArrayList<>();
        List<Question> toSave = new ArrayList<>();

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    String text = record.get("text");
                    List<String> options = List.of(
                            record.get("option1"),
                            record.get("option2"),
                            record.get("option3"),
                            record.get("option4")
                    );
                    int correctOptionIndex = Integer.parseInt(record.get("correctOptionIndex").trim());
                    String explanation = record.isMapped("explanation") ? record.get("explanation") : null;
                    String difficulty = record.isMapped("difficulty") && !record.get("difficulty").isBlank()
                            ? record.get("difficulty") : "medium";
                    String topic = record.isMapped("topic") && !record.get("topic").isBlank()
                            ? record.get("topic") : "General";

                    if (text == null || text.isBlank()) {
                        errors.add(Map.of("row", rowNum, "error", "Missing question text"));
                        continue;
                    }
                    if (correctOptionIndex < 0 || correctOptionIndex > 3) {
                        errors.add(Map.of("row", rowNum, "error", "correctOptionIndex must be 0-3"));
                        continue;
                    }

                    toSave.add(new Question(test, text, options, correctOptionIndex, explanation, difficulty, topic));
                } catch (Exception rowEx) {
                    errors.add(Map.of("row", rowNum, "error", "Malformed row: " + rowEx.getMessage()));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not parse CSV: " + e.getMessage()));
        }

        questionRepository.saveAll(toSave);

        test.setQuestionsCount(questionRepository.findByTestId(test.getId()).size());
        mockTestRepository.save(test);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "added", toSave.size(),
                "errors", errors
        ));
    }
    
    
    
}