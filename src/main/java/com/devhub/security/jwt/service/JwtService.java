package com.devhub.security.jwt.service;

import com.devhub.security.jwt.entity.RefreshToken;
import com.devhub.user.entity.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtService {

    public String generateAccessToken(String role, UUID userId) {

        return Jwt.issuer("devhub")
                .subject(String.valueOf(userId))
                .groups(Set.of(role))
                .claim("userId", userId)
                .expiresIn(Duration.ofHours(4))
                .sign();
    }

    public RefreshToken generateRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.user = user;
        refreshToken.token = UUID.randomUUID().toString();
        refreshToken.expiresAt = Instant.now().plus(Duration.ofDays(30));

        return refreshToken;
    }
}
