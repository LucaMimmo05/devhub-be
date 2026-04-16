package com.devhub.task.dto;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TaskRequest {

    @NotBlank
    @Size(max = 100)
    public String title;

    @Size(max = 500)
    public String description;

    public Status status;

    public Priority priority;

    public OffsetDateTime dueDate;

    public UUID projectId;

    public UUID assignedToProfileId;
}
