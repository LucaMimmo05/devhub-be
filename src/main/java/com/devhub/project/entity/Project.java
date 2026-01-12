package com.devhub.project.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import com.devhub.projectmember.entity.ProjectMember;
import com.devhub.user.entity.UserProfile;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {
    @Column(name = "title", nullable = false, length = 25)
    public String title;

    @Column(name = "description", nullable = false, length = 255)
    public String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public Status status;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    public Priority priority;

    @Column(name = "due_date")
    public OffsetDateTime dueDate;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    public UserProfile owner;

    @Column(name = "img_url")
    public String imgUrl;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ProjectMember> members = new HashSet<>();


    public Set<UUID> getMemberIds() {
        return members.stream()
                .map(m -> m.userProfile.id)
                .collect(Collectors.toSet());
    }
}
