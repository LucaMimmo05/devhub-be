package com.devhub.security.jwt;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    public String generateAccessToken(String subject, String role) {

        return Jwt.issuer("devhub")
                .subject(subject)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(4))
                .sign();
    }
}
