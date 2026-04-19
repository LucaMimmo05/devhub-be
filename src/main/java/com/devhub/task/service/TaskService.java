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

        boolean isMember = project.owner.user.id.equals(requestingUserId) ||
                project.members.stream().anyMatch(pm -> pm.userProfile.user.id.equals(requestingUserId));
        if (!isMember) throw new ForbiddenException("Access denied");

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
        task.project = project;

        if (request.assignedToProfileId != null) {
            UserProfile assigned = userProfileRepository.findById(request.assignedToProfileId);
            task.assignedTo = assigned;
        } else {
            task.assignedTo = profile;
        }

        task.createdBy = profile;

        taskRepository.persist(task);
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
        if (request.dueDate != null) task.dueDate = request.dueDate;

        if (request.assignedToProfileId != null) {
            UserProfile assigned = userProfileRepository.findById(request.assignedToProfileId);
            if (assigned != null) task.assignedTo = assigned;
        }

        taskRepository.persist(task);
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
