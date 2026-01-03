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


    public void revokeAllForUser(UUID userId) {
        update("revoked = true where user.id = ?1 and revoked = false", userId);
    }


    public long deleteExpiredOrRevoked() {
        return delete("expiresAt < current_timestamp or revoked = true");
    }


    public boolean hasValidToken(UUID userId) {
        return count("user.id = ?1 and revoked = false and expiresAt > current_timestamp", userId) > 0;
    }
}