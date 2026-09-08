package com.freelance.mcq.repository;


import com.freelance.mcq.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByTestId(UUID testId);
}