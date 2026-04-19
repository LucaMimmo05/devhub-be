package com.devhub.task.repository;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import com.devhub.task.entity.Task;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskRepository implements PanacheRepositoryBase<Task, UUID> {

    public List<Task> findByAssignedUserId(UUID userProfileId, Status status, Priority priority, String search) {
        List<String> conditions = new ArrayList<>();
        Parameters params = Parameters.with("profileId", userProfileId);

        conditions.add("assignedTo.id = :profileId");

        if (status != null) {
            conditions.add("status = :status");
            params.and("status", status);
        }
        if (priority != null) {
            conditions.add("priority = :priority");
            params.and("priority", priority);
        }
        if (search != null && !search.isBlank()) {
            conditions.add("lower(title) like :search");
            params.and("search", "%" + search.toLowerCase() + "%");
        }

        return list(String.join(" and ", conditions), params);
    }

    public List<Task> findByProjectId(UUID projectId, Status status, Priority priority, String search) {
        List<String> conditions = new ArrayList<>();
        Parameters params = Parameters.with("projectId", projectId);

        conditions.add("project.id = :projectId");

        if (status != null) {
            conditions.add("status = :status");
            params.and("status", status);
        }
        if (priority != null) {
            conditions.add("priority = :priority");
            params.and("priority", priority);
        }
        if (search != null && !search.isBlank()) {
            conditions.add("lower(title) like :search");
            params.and("search", "%" + search.toLowerCase() + "%");
        }

        return list(String.join(" and ", conditions), params);
    }
}
