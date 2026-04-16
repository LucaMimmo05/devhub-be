package com.devhub.task.dto;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TaskResponse {

    public UUID id;
    public String title;
    public String description;
    public Status status;
    public Priority priority;
    public OffsetDateTime dueDate;
    public UUID projectId;
    public String projectTitle;
    public UUID assignedToProfileId;
    public String assignedToUsername;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
