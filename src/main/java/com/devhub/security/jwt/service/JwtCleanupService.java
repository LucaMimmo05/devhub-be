package com.devhub.security.jwt.service;

import com.devhub.security.jwt.repository.RefreshTokenRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;

public class JwtCleanupService {

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    void cleanExpiredTokens() {
        long deleted = refreshTokenRepository.delete("expiresAt < ?1", Instant.now());
    }
}
