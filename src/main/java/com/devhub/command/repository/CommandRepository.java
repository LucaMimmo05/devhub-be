package com.devhub.command.repository;

import com.devhub.command.entity.Command;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CommandRepository implements PanacheRepositoryBase<Command, UUID> {

    public List<Command> findByUser(UUID userProfileId, String category, String search) {
        List<String> conditions = new ArrayList<>();
        Parameters params = Parameters.with("profileId", userProfileId);
        conditions.add("userProfile.id = :profileId");

        if (category != null && !category.isBlank()) {
            conditions.add("lower(category) = lower(:category)");
            params.and("category", category);
        }
        if (search != null && !search.isBlank()) {
            conditions.add("(lower(title) like :search or lower(command) like :search or lower(description) like :search)");
            params.and("search", "%" + search.toLowerCase() + "%");
        }

        return list(String.join(" and ", conditions) + " order by createdAt desc", params);
    }
}
