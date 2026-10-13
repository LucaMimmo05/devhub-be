package com.devhub.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_codes", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "type"}))
public class OtpCode extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public OffsetDateTime createdAt;

    @Column(nullable = false)
    public String email;

    @Column(nullable = false, length = 50)
    public String type;

    @Column(name = "code_hash", nullable = false)
    public String codeHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    /** Wrong codes entered so far; the code is invalidated at OtpService.MAX_ATTEMPTS. */
    @ColumnDefault("0")
    @Column(nullable = false)
    public int attempts;
}
