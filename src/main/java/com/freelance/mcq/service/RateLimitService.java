package com.freelance.mcq.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }

    public boolean tryConsume(String key, int capacity, Duration period) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(capacity, period));
        return bucket.tryConsume(1);
    }

    // Specific, named limits for each endpoint we care about
    public boolean allowLogin(String ip) {
        return tryConsume("login:" + ip, 30, Duration.ofMinutes(15));
    }

    public boolean allowForgotPassword(String ip) {
        return tryConsume("forgot:" + ip, 3, Duration.ofHours(1));
    }

    public boolean allowResetPassword(String ip) {
        return tryConsume("reset:" + ip, 5, Duration.ofMinutes(15));
    }

    public boolean allowRegister(String ip) {
        return tryConsume("register:" + ip, 3, Duration.ofHours(1));
    }
}
