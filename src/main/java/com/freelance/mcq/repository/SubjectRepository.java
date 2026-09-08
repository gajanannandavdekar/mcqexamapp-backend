package com.freelance.mcq.repository;


import com.freelance.mcq.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findBySubjectKey(String subjectKey);
}
