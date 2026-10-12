package com.devhub.auth.repository;

import com.devhub.auth.entity.OtpCode;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OtpCodeRepository implements PanacheRepositoryBase<OtpCode, UUID> {

    public Optional<OtpCode> findValid(String email, String type) {
        return find("email = ?1 and type = ?2 and expiresAt > ?3", email, type, Instant.now())
                .firstResultOptional();
    }

    public long deleteByEmailAndType(String email, String type) {
        return delete("email = ?1 and type = ?2", email, type);
    }

    public long deleteExpired() {
        return delete("expiresAt < ?1", Instant.now());
    }
}
