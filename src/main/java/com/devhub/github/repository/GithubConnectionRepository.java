package com.devhub.github.repository;

import com.devhub.github.entity.GithubConnection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GithubConnectionRepository implements PanacheRepositoryBase<GithubConnection, UUID> {

    public Optional<GithubConnection> findByProfileId(UUID profileId) {
        return find("userProfile.id", profileId).firstResultOptional();
    }

    public long deleteByProfileId(UUID profileId) {
        return delete("userProfile.id", profileId);
    }
}
