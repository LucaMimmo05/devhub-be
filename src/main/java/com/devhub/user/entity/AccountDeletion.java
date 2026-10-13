package com.devhub.user.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit trail of account deletions: one row per request, closed by a cancellation or a purge.
 * Holds ids and dates only, and no FK, so it outlives the deleted account.
 */
@Entity
@Table(name = "account_deletions")
public class AccountDeletion extends PanacheEntityBase {

    public static final String SOURCE_USER = "USER";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "requested_at", nullable = false)
    public OffsetDateTime requestedAt;

    @Column(name = "cancelled_at")
    public OffsetDateTime cancelledAt;

    @Column(name = "purged_at")
    public OffsetDateTime purgedAt;

    @Column(nullable = false, length = 20)
    public String source;

    public static AccountDeletion requested(UUID userId, OffsetDateTime at, String source) {
        AccountDeletion deletion = new AccountDeletion();
        deletion.userId = userId;
        deletion.requestedAt = at;
        deletion.source = source;
        return deletion;
    }
}
