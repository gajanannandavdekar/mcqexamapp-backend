package com.freelance.mcq.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.freelance.mcq.entity.TestAttempt;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {

	@Query("SELECT a FROM TestAttempt a JOIN FETCH a.test JOIN FETCH a.subject JOIN FETCH a.user WHERE a.user.id = :userId ORDER BY a.completedAt DESC")
    List<TestAttempt> findByUserIdWithDetails(@Param("userId") UUID userId);

    @Query("SELECT a FROM TestAttempt a JOIN FETCH a.test JOIN FETCH a.subject JOIN FETCH a.user WHERE a.id = :id")
    Optional<TestAttempt> findByIdWithDetails(@Param("id") UUID id);
}
