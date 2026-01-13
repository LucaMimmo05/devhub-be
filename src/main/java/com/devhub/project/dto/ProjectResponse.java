package com.devhub.project.dto;

import com.devhub.common.entity.BaseEntity;
import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;

import java.time.OffsetDateTime;
import java.util.List;

public class ProjectResponse extends BaseEntity {
    public String title;
    public String description;
    public String imageUrl;
    public String ownerId;
    public List<String> memberIds;
    public Status status;
    public Priority priority;
    public OffsetDateTime dueDate;
    public Integer progress;

}

