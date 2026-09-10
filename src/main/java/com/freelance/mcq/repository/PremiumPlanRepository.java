package com.freelance.mcq.repository;

import com.freelance.mcq.entity.PremiumPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PremiumPlanRepository extends JpaRepository<PremiumPlan, UUID> {
    List<PremiumPlan> findByIsActiveTrue();
}