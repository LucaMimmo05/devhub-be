package com.devhub.project.dto;

import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ProjectResponse {

    public UUID id;
    public String title;
    public String description;
    public String imageUrl;
    public String ownerId;
    public String ownerProfileId;
    public String ownerUsername;
    public String ownerAvatarUrl;
    public List<MemberSummary> members;
    public Status status;
    public Priority priority;
    public OffsetDateTime dueDate;
    public Integer progress;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;

    public static class MemberSummary {
        public UUID profileId;
        public String username;
        public String firstName;
        public String lastName;
        public String avatarUrl;
        public String role;
    }
}
