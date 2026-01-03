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

    public String generateAccessToken(String subject, String role) {

        return Jwt.issuer("devhub")
                .subject(subject)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(4))
                .sign();
    }

    public RefreshToken generateRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.user = user;
        refreshToken.token = UUID.randomUUID().toString();
        refreshToken.expiresAt = Instant.now().plus(Duration.ofDays(30));
        refreshToken.revoked = false;

        return refreshToken;
    }
}
