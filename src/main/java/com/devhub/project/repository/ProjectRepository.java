package com.devhub.project.repository;

import com.devhub.project.entity.Project;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectRepository implements PanacheRepositoryBase<Project, UUID> {
    public List<Project> getAllUserProjectsEntities(UUID userId) {
        List<Project> allProjects = listAll();

        return allProjects.stream()
                .filter(p -> (p.owner != null && p.owner.user != null && p.owner.user.id.equals(userId))
                        || (p.getMemberIds() != null && p.getMemberIds().contains(userId)))
                .collect(Collectors.toList());
    }
}

