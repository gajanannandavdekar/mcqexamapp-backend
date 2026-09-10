package com.freelance.mcq.controller;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//Add these imports:
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.freelance.mcq.dto.BulkCreateQuestionsRequest;
import com.freelance.mcq.dto.CreateSubjectRequest;
import com.freelance.mcq.dto.CreateTestRequest;
import com.freelance.mcq.dto.GrantPremiumRequest;
import com.freelance.mcq.dto.UserSummary;
import com.freelance.mcq.entity.AdminActionLog;
import com.freelance.mcq.entity.MockTest;
import com.freelance.mcq.entity.Question;
import com.freelance.mcq.entity.Subject;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.AdminActionLogRepository;
import com.freelance.mcq.repository.DevicePushTokenRepository;
import com.freelance.mcq.repository.MockTestRepository;
import com.freelance.mcq.repository.QuestionRepository;
import com.freelance.mcq.repository.SubjectRepository;
import com.freelance.mcq.repository.UserRepository;
import com.freelance.mcq.service.NotificationService;



@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SubjectRepository subjectRepository;
    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final DevicePushTokenRepository devicePushTokenRepository;
    private final NotificationService notificationService;

    public AdminController(SubjectRepository subjectRepository, MockTestRepository mockTestRepository,
                            QuestionRepository questionRepository,UserRepository userRepository,AdminActionLogRepository adminActionLogRepository,DevicePushTokenRepository devicePushTokenRepository,NotificationService notificationService) {
        this.subjectRepository = subjectRepository;
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
        this.userRepository=userRepository;
        this.adminActionLogRepository=adminActionLogRepository;
        this.devicePushTokenRepository=devicePushTokenRepository;
        this.notificationService=notificationService;
        
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
    public ResponseEntity<?> uploadQuestionsCsv(@PathVariable String testKey, @RequestParam("file") MultipartFile file) {
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
    
    
    


    @GetMapping("/users")
    public List<UserSummary> searchUsers(@RequestParam(required = false, defaultValue = "") String search) {
        return userRepository.searchUsers(search).stream()
                .map(u -> new UserSummary(
                        u.getId(), u.getEmail(), u.getFullName() != null ? u.getFullName() : "",
                        u.isPremiumActive(),
                        u.isPremium() && u.getPremiumExpiresAt() == null,
                        u.getPremiumExpiresAt() != null ? u.getPremiumExpiresAt().toString() : null,
                        u.getRole().name()
                ))
                .toList();
    }


    @PostMapping("/users/{userId}/grant-premium")
    public ResponseEntity<?> grantPremium(Authentication auth, @PathVariable UUID userId, @RequestBody GrantPremiumRequest req) {
        User actor = (User) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        if (req.durationDays() != null && req.durationDays() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "durationDays must be positive"));
        }

        OffsetDateTime base = (user.getPremiumExpiresAt() != null && user.getPremiumExpiresAt().isAfter(OffsetDateTime.now()))
                ? user.getPremiumExpiresAt()
                : OffsetDateTime.now();

        user.setPremium(true);
        user.setPremiumExpiresAt(req.durationDays() != null ? base.plusDays(req.durationDays()) : null);
        userRepository.save(user);

        String detail = req.durationDays() != null
                ? "Granted " + req.durationDays() + " days, new expiry: " + user.getPremiumExpiresAt()
                : "Granted premium forever";
        adminActionLogRepository.save(new AdminActionLog(
                actor.getId(), actor.getEmail(), user.getId(), user.getEmail(), "GRANT_PREMIUM", detail
        ));

        return ResponseEntity.ok(Map.of(
                "message", "Premium granted",
                "premiumExpiresAt", user.getPremiumExpiresAt() != null ? user.getPremiumExpiresAt().toString() : "forever"
        ));
    }

    @PostMapping("/users/{userId}/revoke-premium")
    public ResponseEntity<?> revokePremium(Authentication auth, @PathVariable UUID userId) {
        User actor = (User) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        user.setPremium(false);
        user.setPremiumExpiresAt(null);
        userRepository.save(user);

        adminActionLogRepository.save(new AdminActionLog(
                actor.getId(), actor.getEmail(), user.getId(), user.getEmail(), "REVOKE_PREMIUM", null
        ));

        return ResponseEntity.ok(Map.of("message", "Premium revoked"));
    }   
    

    @PostMapping("/tests/{testKey}/publish")
    public ResponseEntity<?> publishTest(@PathVariable String testKey) {
        MockTest test = mockTestRepository.findByTestKeyWithSubject(testKey).orElse(null);
        if (test == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Test not found"));
        }
        if (test.isPublished()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Test is already published"));
        }
        if (questionRepository.findByTestId(test.getId()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Cannot publish a test with no questions"));
        }

        test.setPublished(true);
        mockTestRepository.save(test);

        List<String> tokens = test.isPremium()
                ? devicePushTokenRepository.findAllPremiumUserTokens()
                : devicePushTokenRepository.findAllTokens();

        String title = test.isPremium() ? "New Premium Test Available!" : "New Test Available!";
        String body = test.getTitle() + " in " + test.getSubject().getTitle() + " is now live.";

        notificationService.sendToTokens(tokens, title, body);

        return ResponseEntity.ok(Map.of("message", "Test published and notifications sent", "notifiedCount", tokens.size()));
    }
    
}