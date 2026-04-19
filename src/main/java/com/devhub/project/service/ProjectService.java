package com.devhub.project.service;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.ProjectRole;
import com.devhub.common.enums.Status;
import com.devhub.project.dto.ProjectRequest;
import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.entity.Project;
import com.devhub.project.repository.ProjectRepository;
import com.devhub.project.entity.ProjectMember;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
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


    public ProjectResponse getUserProjectById(UUID projectId, String userId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project not found");
        }
        if (!project.owner.user.id.toString().equals(userId) && project.members.stream().noneMatch(pm -> pm.userProfile.user.id.toString().equals(userId))) {
            throw new NotFoundException("Project not found");
        }
        return toResponse(project);
    }


    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest request, UUID userId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) throw new NotFoundException("Project not found");
        if (!project.owner.user.id.equals(userId)) throw new ForbiddenException("Only the owner can edit this project");

        if (request.title != null) project.title = request.title;
        if (request.description != null) project.description = request.description;
        if (request.imageUrl != null) project.imgUrl = request.imageUrl;
        if (request.status != null) project.status = request.status;
        if (request.priority != null) project.priority = request.priority;
        if (request.dueDate != null) project.dueDate = request.dueDate;
        if (request.progress != null) project.progress = request.progress;

        projectRepository.persist(project);
        return toResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) throw new NotFoundException("Project not found");
        if (!project.owner.user.id.equals(userId)) throw new ForbiddenException("Only the owner can delete this project");
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectResponse addMember(UUID projectId, UUID memberProfileId, UUID requestingUserId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) throw new NotFoundException("Project not found");
        if (!project.owner.user.id.equals(requestingUserId)) throw new ForbiddenException("Only the owner can add members");

        boolean alreadyMember = project.members.stream()
                .anyMatch(pm -> pm.userProfile.id.equals(memberProfileId));
        if (alreadyMember) return toResponse(project);

        UserProfile memberProfile = userProfileRepository.findById(memberProfileId);
        if (memberProfile == null) throw new NotFoundException("User profile not found");

        ProjectMember pm = new ProjectMember();
        pm.userProfile = memberProfile;
        pm.project = project;
        pm.role = ProjectRole.MEMBER;
        project.members.add(pm);

        projectRepository.persist(project);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse removeMember(UUID projectId, UUID memberProfileId, UUID requestingUserId) {
        Project project = projectRepository.findById(projectId);
        if (project == null) throw new NotFoundException("Project not found");
        if (!project.owner.user.id.equals(requestingUserId)) throw new ForbiddenException("Only the owner can remove members");

        project.members.removeIf(pm -> pm.userProfile.id.equals(memberProfileId));
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
        dto.dueDate = project.dueDate;
        dto.progress = project.progress != null ? project.progress : 0;
        dto.createdAt = project.getCreatedAt();
        dto.updatedAt = project.getUpdatedAt();

        if (project.owner != null) {
            dto.ownerId = project.owner.user.id.toString();
            dto.ownerProfileId = project.owner.id.toString();
            dto.ownerUsername = project.owner.username;
            dto.ownerAvatarUrl = project.owner.avatarUrl;
        }

        dto.members = project.members.stream().map(pm -> {
            ProjectResponse.MemberSummary ms = new ProjectResponse.MemberSummary();
            ms.profileId = pm.userProfile.id;
            ms.username = pm.userProfile.username;
            ms.firstName = pm.userProfile.firstName;
            ms.lastName = pm.userProfile.lastName;
            ms.avatarUrl = pm.userProfile.avatarUrl;
            ms.role = pm.role != null ? pm.role.name() : "MEMBER";
            return ms;
        }).toList();

        return dto;
    }

}
