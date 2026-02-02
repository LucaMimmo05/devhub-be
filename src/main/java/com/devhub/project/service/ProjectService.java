package com.devhub.project.service;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import com.devhub.project.dto.ProjectRequest;
import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.entity.Project;
import com.devhub.project.repository.ProjectRepository;
import com.devhub.project.controller.ProjectMember;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjectService {

    @Inject
    ProjectRepository projectRepository;

    @Inject
    UserProfileRepository userProfileRepository;

    public List<ProjectResponse> getAllUserProjects(String userId, Integer limit) {
        UUID userIdUUID = UUID.fromString(userId);
        List<Project> projects = projectRepository.getAllUserProjectsEntities(userIdUUID); // lista di Project

        List<ProjectResponse> responses = projects.stream()
                .map(this::toResponse)
                .toList();

        if (limit != null && limit > 0 && limit < responses.size()) {
            return responses.subList(0, limit);
        }

        return responses;
    }


    @Transactional
    public ProjectResponse createProject(ProjectRequest request, UUID userId) {
        Project project = toEntity(request, userId);
        projectRepository.persist(project);
        return toResponse(project);
    }




    private Project toEntity(ProjectRequest request, UUID userId) {
        Project project = new Project();
        project.title = request.title;
        project.description = request.description;
        project.imgUrl = request.imageUrl;
        project.dueDate = request.dueDate;
        project.progress = request.progress != null ? request.progress : 0;
        project.createdAt = OffsetDateTime.now();
        project.updatedAt = OffsetDateTime.now();

        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) {
            throw new NotFoundException("UserProfile not found for user");
        }

        project.owner = profile;

        project.status = request.status != null ? request.status : Status.PENDING;
        project.priority = request.priority != null ? request.priority : Priority.MEDIUM;

        if (request.memberIds != null) {
            for (String memberIdStr : request.memberIds) {
                UUID memberId = UUID.fromString(memberIdStr);
                UserProfile memberProfile = userProfileRepository.findById(memberId);
                if (memberProfile != null) {
                    ProjectMember pm = new ProjectMember();
                    pm.userProfile = memberProfile;
                    pm.project = project;
                    project.members.add(pm);
                }
            }
        }

        return project;
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse dto = new ProjectResponse();

        dto.id = project.id;
        dto.title = project.title;
        dto.description = project.description;
        dto.imageUrl = project.imgUrl;
        dto.status = project.status != null ? project.status : Status.PENDING;
        dto.priority = project.priority != null ? project.priority : Priority.MEDIUM;
        dto.dueDate = project.dueDate != null ? project.dueDate : null;
        dto.progress = project.progress != null ? project.progress : 0;
        dto.ownerId = project.owner != null ? project.owner.user.id.toString() : null;
        dto.createdAt = project.getCreatedAt();
        dto.updatedAt = project.getUpdatedAt();

        dto.memberIds = project.members.stream()
                .map(pm -> pm.userProfile.user.id.toString())
                .toList();

        return dto;
    }

}
