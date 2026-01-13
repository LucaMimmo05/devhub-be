package com.devhub.project.dto;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

public class ProjectRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    public String title;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    public String description;

    @NotBlank(message = "Image URL cannot be blank")
    @Size(max = 255, message = "Image URL must be at most 255 characters")
    @URL(message = "Image URL must be a valid URL starting with http or https")
    public String imageUrl;
    
    @NotBlank(message = "Owner ID cannot be blank")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "Owner ID must be a valid UUID")
    @Size(min = 8, max = 36, message = "Owner ID length must be between 8 and 36 characters")
    public String ownerId;

    @Size(min = 1, message = "There must be at least one member")
    public String[] memberIds;

    public Priority priority;

    public Status status;

    public OffsetDateTime dueDate;

    @Min(0)
    @Max(100)
    public Integer progress;

}
