package com.devhub.security.jwt.repository;

import com.devhub.security.jwt.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, UUID> {


    public Optional<RefreshToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public long deleteExpired() {
        return delete("expiresAt < current_timestamp");
    }

    public boolean hasValidToken(UUID userId) {
        return count("user.id = ?1 and expiresAt > current_timestamp", userId) > 0;
    }
}