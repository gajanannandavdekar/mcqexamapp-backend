package com.freelance.mcq.service;

import com.freelance.mcq.dto.AttemptResponse;
import com.freelance.mcq.dto.SubmitAttemptRequest;
import com.freelance.mcq.entity.*;
import com.freelance.mcq.repository.MockTestRepository;
import com.freelance.mcq.repository.QuestionRepository;
import com.freelance.mcq.repository.TestAttemptRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AttemptService {

    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    //private final SubjectRepository subjectRepository;

    public AttemptService(MockTestRepository mockTestRepository, QuestionRepository questionRepository,
                           TestAttemptRepository testAttemptRepository
                           ) {
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
        this.testAttemptRepository = testAttemptRepository;
        //this.subjectRepository=subjectRepository;
    }

    public AttemptResponse submitAttempt(User user, SubmitAttemptRequest request) {
    	MockTest test = mockTestRepository.findByTestKeyWithSubject(request.testKey())
    	        .orElseThrow(() -> new NoSuchElementException("Test not found"));

        if (test.isPremium() && !user.isPremiumActive()) {
            throw new SecurityException("This test requires a premium subscription");
        }

        List<Question> questions = questionRepository.findByTestId(test.getId());
        Map<String, SubmitAttemptRequest.ResponseItem> userResponses = request.responses() != null
                ? request.responses() : Map.of();

        int correctCount = 0, incorrectCount = 0, unansweredCount = 0;

        Map<String, Map<String, Integer>> difficultyStats = new LinkedHashMap<>();
        difficultyStats.put("easy", new HashMap<>(Map.of("total", 0, "correct", 0)));
        difficultyStats.put("medium", new HashMap<>(Map.of("total", 0, "correct", 0)));
        difficultyStats.put("hard", new HashMap<>(Map.of("total", 0, "correct", 0)));

        Map<String, int[]> topicStats = new LinkedHashMap<>(); // topic -> [total, correct]

        Map<String, Object> responsesForStorage = new LinkedHashMap<>();
        List<Map<String, Object>> questionsFull = new ArrayList<>();

        for (Question q : questions) {
            SubmitAttemptRequest.ResponseItem resp = userResponses.get(q.getId().toString());
            Integer selectedIdx = resp != null ? resp.selectedOptionIndex() : null;
            boolean isCorrect = selectedIdx != null && selectedIdx.equals(q.getCorrectOptionIndex());

            if (selectedIdx == null) {
                unansweredCount++;
            } else if (isCorrect) {
                correctCount++;
            } else {
                incorrectCount++;
            }

            String difficulty = q.getDifficulty() != null ? q.getDifficulty() : "medium";
            Map<String, Integer> dStat = difficultyStats.get(difficulty);
            dStat.put("total", dStat.get("total") + 1);
            if (isCorrect) dStat.put("correct", dStat.get("correct") + 1);

            String topic = q.getTopic() != null ? q.getTopic() : "General";
            int[] tStat = topicStats.computeIfAbsent(topic, k -> new int[2]);
            tStat[0]++;
            if (isCorrect) tStat[1]++;

            Map<String, Object> responseEntry = new LinkedHashMap<>();
            responseEntry.put("selectedOptionIndex", selectedIdx);
            responseEntry.put("isMarkedForReview", resp != null && resp.isMarkedForReview());
            responseEntry.put("status", selectedIdx == null ? "UNANSWERED" : "ANSWERED");
            responsesForStorage.put(q.getId().toString(), responseEntry);

            Map<String, Object> qFull = new LinkedHashMap<>();
            qFull.put("id", q.getId());
            qFull.put("text", q.getText());
            qFull.put("options", q.getOptions());
            qFull.put("correctOptionIndex", q.getCorrectOptionIndex());
            qFull.put("explanation", q.getExplanation());
            qFull.put("difficulty", difficulty);
            qFull.put("topic", topic);
            questionsFull.add(qFull);
        }

        List<Map<String, Object>> weakTopics = new ArrayList<>();
        for (var entry : topicStats.entrySet()) {
            int total = entry.getValue()[0];
            int correct = entry.getValue()[1];
            int pct = total > 0 ? Math.round((correct * 100f) / total) : 0;
            if (pct < 60) {
                Map<String, Object> wt = new LinkedHashMap<>();
                wt.put("topic", entry.getKey());
                wt.put("total", total);
                wt.put("correct", correct);
                wt.put("percentage", pct);
                weakTopics.add(wt);
            }
        }

        int totalQuestions = questions.size();
        double percentage = totalQuestions > 0 ? Math.round((correctCount * 10000.0) / totalQuestions) / 100.0 : 0.0;
        Subject subject = test.getSubject();
        String subjectTitle = subject.getTitle();
        UUID subjectId = subject.getId();
        	
        TestAttempt attempt = new TestAttempt();
        attempt.setUser(user);
        attempt.setTest(test);
        attempt.setSubject(subject);
        attempt.setScore(correctCount);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setPercentage(BigDecimal.valueOf(percentage));
        attempt.setCorrectCount(correctCount);
        attempt.setIncorrectCount(incorrectCount);
        attempt.setUnansweredCount(unansweredCount);
        attempt.setDifficultyStats(Map.copyOf(difficultyStats));
        attempt.setWeakTopics(weakTopics);
        attempt.setResponses(responsesForStorage);

        testAttemptRepository.save(attempt);

        return new AttemptResponse(
                attempt.getId(),
                test.getId(),
                test.getTestKey(),
                test.getTitle(),
                subjectId,
                subjectTitle,
                test.getDurationMinutes(),
                attempt.getCompletedAt(),
                correctCount,
                totalQuestions,
                percentage,
                correctCount,
                incorrectCount,
                unansweredCount,
                Map.copyOf(difficultyStats),
                weakTopics,
                questionsFull,
                responsesForStorage
        );    
    
    }

    
    public List<AttemptResponse> listAttemptsForUser(User user) {
        return testAttemptRepository.findByUserIdWithDetails(user.getId()).stream()
                .map(a -> toResponse(a, null, null))
                .toList();
    }

    public AttemptResponse getAttemptDetail(User user, UUID attemptId) {
        TestAttempt attempt = testAttemptRepository.findByIdWithDetails(attemptId)
                .orElseThrow(() -> new NoSuchElementException("Attempt not found"));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not your attempt");
        }

        List<Question> questions = questionRepository.findByTestId(attempt.getTest().getId());
        List<Map<String, Object>> questionsFull = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> qFull = new LinkedHashMap<>();
            qFull.put("id", q.getId());
            qFull.put("text", q.getText());
            qFull.put("options", q.getOptions());
            qFull.put("correctOptionIndex", q.getCorrectOptionIndex());
            qFull.put("explanation", q.getExplanation());
            qFull.put("difficulty", q.getDifficulty());
            qFull.put("topic", q.getTopic());
            questionsFull.add(qFull);
        }

        return toResponse(attempt, questionsFull, attempt.getResponses());
    }
        
    @SuppressWarnings("unchecked")
    private AttemptResponse toResponse(TestAttempt a, List<Map<String, Object>> questionsFull, Map<String, Object> responses) {
        return new AttemptResponse(
                a.getId(),
                a.getTest().getId(),
                a.getTest().getTestKey(),
                a.getTest().getTitle(),
                a.getSubject().getId(),
                a.getSubject().getTitle(),
                a.getTest().getDurationMinutes(),
                a.getCompletedAt(),
                a.getScore(),
                a.getTotalQuestions(),
                a.getPercentage().doubleValue(),
                a.getCorrectCount(),
                a.getIncorrectCount(),
                a.getUnansweredCount(),
                a.getDifficultyStats(),
                (List<Map<String, Object>>) (Object) a.getWeakTopics(),
                questionsFull,
                responses
        );
    }
}