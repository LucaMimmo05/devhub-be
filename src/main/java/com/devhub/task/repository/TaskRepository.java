package com.devhub.task.repository;

import com.devhub.task.entity.Task;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskRepository implements PanacheRepositoryBase<Task, UUID> {

    public List<Task> findByAssignedUserId(UUID userProfileId) {
        return list("assignedTo.id", userProfileId);
    }

    public List<Task> findByProjectId(UUID projectId) {
        return list("project.id", projectId);
    }
}
