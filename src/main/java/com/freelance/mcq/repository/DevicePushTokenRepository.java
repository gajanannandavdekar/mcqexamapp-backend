package com.freelance.mcq.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.freelance.mcq.entity.DevicePushToken;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {
    Optional<DevicePushToken> findByPushToken(String pushToken);
    List<DevicePushToken> findByUserId(UUID userId);

    @Query("SELECT DISTINCT t.pushToken FROM DevicePushToken t")
    List<String> findAllTokens();

    @Query("SELECT DISTINCT t.pushToken FROM DevicePushToken t WHERE t.user.isPremium = true")
    List<String> findAllPremiumUserTokens();

    void deleteByPushToken(String pushToken);
}