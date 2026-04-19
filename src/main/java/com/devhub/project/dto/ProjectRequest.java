package com.devhub.project.dto;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class ProjectRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    public String title;

    @Size(max = 500, message = "Description must be at most 500 characters")
    public String description;

    @Size(max = 255, message = "Image URL must be at most 255 characters")
    public String imageUrl;

    public String ownerId;

    public String[] memberIds;

    public Priority priority;

    public Status status;

    public OffsetDateTime dueDate;

}
