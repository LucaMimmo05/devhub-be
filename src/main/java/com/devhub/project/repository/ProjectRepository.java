package com.devhub.project.repository;

import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.entity.Project;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectRepository implements PanacheRepositoryBase<Project, UUID> {

    public List<ProjectResponse> getAllUserProjects(UUID userId) {
        List<Project> allProjects = listAll();

        return allProjects.stream()
                .filter(p -> p.owner.id.equals(userId) || p.getMemberIds().contains(userId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse dto = new ProjectResponse();
        dto.id = project.id;
        dto.title = project.title;
        dto.description = project.description;
        dto.imageUrl = project.imgUrl;
        dto.ownerId = project.owner.id.toString();
        dto.memberIds = project.getMemberIds().stream()
                .map(UUID::toString)
                .toList();
        return dto;
    }
}

