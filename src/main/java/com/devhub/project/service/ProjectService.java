package com.devhub.project.service;

import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.repository.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjectService {

    @Inject
    ProjectRepository projectRepository;

    public List<ProjectResponse> getAllUserProjects(String userId){
        UUID userIdUUID = UUID.fromString(userId);
        return projectRepository.getAllUserProjects(userIdUUID);
    }
}
