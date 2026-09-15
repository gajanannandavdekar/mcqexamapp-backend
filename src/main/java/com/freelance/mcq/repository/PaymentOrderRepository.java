package com.freelance.mcq.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.freelance.mcq.entity.PaymentOrder;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
	@Query("SELECT o FROM PaymentOrder o JOIN FETCH o.plan JOIN FETCH o.user WHERE o.razorpayOrderId = :orderId")
    Optional<PaymentOrder> findByRazorpayOrderIdWithDetails(@Param("orderId") String orderId);
}