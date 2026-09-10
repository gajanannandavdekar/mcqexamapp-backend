package com.freelance.mcq.repository;

import com.freelance.mcq.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, UUID> {
    List<AdminActionLog> findAllByOrderByCreatedAtDesc();
}