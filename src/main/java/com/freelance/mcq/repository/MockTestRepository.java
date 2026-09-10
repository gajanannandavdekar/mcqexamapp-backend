package com.freelance.mcq.repository;


import com.freelance.mcq.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestRepository extends JpaRepository<MockTest, UUID> {
    Optional<MockTest> findByTestKey(String testKey);
    List<MockTest> findBySubjectId(UUID subjectId);
    long countBySubjectId(UUID subjectId);
    long countBySubjectIdAndIsPublishedTrue(UUID subjectId);
    
    @Query("SELECT t FROM MockTest t JOIN FETCH t.subject WHERE t.testKey = :testKey")
    Optional<MockTest> findByTestKeyWithSubject(@Param("testKey") String testKey);
    
}
