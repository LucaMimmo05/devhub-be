package com.devhub.user.repository;

import com.devhub.user.entity.AccountDeletion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountDeletionRepository implements PanacheRepositoryBase<AccountDeletion, UUID> {

    /** The request still waiting for a cancellation or a purge, if any. */
    public Optional<AccountDeletion> findOpen(UUID userId) {
        return find("userId = ?1 and cancelledAt is null and purgedAt is null order by requestedAt desc", userId)
                .firstResultOptional();
    }
}
