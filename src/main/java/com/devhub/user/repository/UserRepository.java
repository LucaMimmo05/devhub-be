package com.devhub.user.repository;

import com.devhub.user.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    /** Accounts whose deletion was requested at or before {@code cutoff}. */
    public List<UUID> findDueForPurge(OffsetDateTime cutoff) {
        return find("deletionRequestedAt <= ?1", cutoff).stream().map(user -> user.id).toList();
    }
}