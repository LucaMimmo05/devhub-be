package com.devhub.task.service;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import com.devhub.project.entity.Project;
import com.devhub.project.repository.ProjectRepository;
import com.devhub.task.dto.TaskRequest;
import com.devhub.task.dto.TaskResponse;
import com.devhub.task.entity.Task;
import com.devhub.task.repository.TaskRepository;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TaskService {

    @Inject
    TaskRepository taskRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    UserProfileRepository userProfileRepository;

    public List<TaskResponse> getUserTasks(UUID userProfileId, Status status, Priority priority, String search) {
        return taskRepository.findByAssignedUserId(userProfileId, status, priority, search)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TaskResponse> getProjectTasks(UUID projectId, UUID requestingUserId, Status status, Priority priority, String search) {
        Project project = projectRepository.findById(projectId);
        if (project == null) throw new NotFoundException("Project not found");

        if (!isOwnerOrMember(project, requestingUserId)) throw new ForbiddenException("Access denied");

        return taskRepository.findByProjectId(projectId, status, priority, search)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request, UUID requestingUserId) {
        UserProfile profile = userProfileRepository.find("user.id", requestingUserId).firstResult();
        if (profile == null) throw new NotFoundException("UserProfile not found");

        Task task = new Task();
        task.title = request.title;
        task.description = request.description;
        task.status = request.status != null ? request.status : Status.PENDING;
        task.priority = request.priority != null ? request.priority : Priority.MEDIUM;
        task.dueDate = request.dueDate;

        Project project = projectRepository.findById(request.projectId);
        if (project == null) throw new NotFoundException("Project not found");
        if (!isOwnerOrMember(project, requestingUserId)) {
            throw new ForbiddenException("Only the project owner or members can add tasks");
        }
        task.project = project;

        task.assignedTo = request.assignedToProfileId != null
                ? requireAssignable(project, request.assignedToProfileId)
                : profile;

        task.createdBy = profile;

        taskRepository.persist(task);
        // Timestamps are set on flush; flush so the response carries them.
        taskRepository.flush();
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, TaskRequest request, UUID requestingUserId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) throw new NotFoundException("Task not found");

        boolean isCreator = task.createdBy != null && task.createdBy.user.id.equals(requestingUserId);
        boolean isProjectOwner = task.project != null && task.project.owner.user.id.equals(requestingUserId);
        if (!isCreator && !isProjectOwner) {
            throw new ForbiddenException("Access denied");
        }

        if (request.title != null) task.title = request.title;
        if (request.description != null) task.description = request.description;
        if (request.status != null) task.status = request.status;
        if (request.priority != null) task.priority = request.priority;
        if (request.clearDueDate) task.dueDate = null;
        else if (request.dueDate != null) task.dueDate = request.dueDate;

        if (request.assignedToProfileId != null) {
            task.assignedTo = requireAssignable(task.project, request.assignedToProfileId);
        }

        taskRepository.persist(task);
        // Timestamps are set on flush; flush so the response carries them.
        taskRepository.flush();
        return toResponse(task);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID requestingUserId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) throw new NotFoundException("Task not found");

        boolean isCreator = task.createdBy != null && task.createdBy.user.id.equals(requestingUserId);
        boolean isProjectOwner = task.project != null && task.project.owner.user.id.equals(requestingUserId);

        if (!isCreator && !isProjectOwner) throw new ForbiddenException("Access denied");

        taskRepository.delete(task);
    }

    private static boolean isOwnerOrMember(Project project, UUID userId) {
        return project.owner.user.id.equals(userId)
                || project.members.stream().anyMatch(pm -> pm.userProfile.user.id.equals(userId));
    }

    /** Tasks can only be assigned to the project owner or one of its members. */
    private static UserProfile requireAssignable(Project project, UUID profileId) {
        if (project == null) {
            throw new BadRequestException("Tasks without a project can't be reassigned");
        }
        if (project.owner.id.equals(profileId)) {
            return project.owner;
        }
        return project.members.stream()
                .map(pm -> pm.userProfile)
                .filter(member -> member.id.equals(profileId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Assignee must be the project owner or a member"));
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse dto = new TaskResponse();
        dto.id = task.id;
        dto.title = task.title;
        dto.description = task.description;
        dto.status = task.status;
        dto.priority = task.priority;
        dto.dueDate = task.dueDate;
        dto.createdAt = task.getCreatedAt();
        dto.updatedAt = task.getUpdatedAt();

        if (task.project != null) {
            dto.projectId = task.project.id;
            dto.projectTitle = task.project.title;
        }
        if (task.assignedTo != null) {
            dto.assignedToProfileId = task.assignedTo.id;
            dto.assignedToUsername = task.assignedTo.username;
        }
        if (task.createdBy != null) {
            dto.createdByProfileId = task.createdBy.id;
        }
        return dto;
    }
}
