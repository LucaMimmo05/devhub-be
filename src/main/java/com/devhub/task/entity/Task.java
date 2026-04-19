package com.devhub.task.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.common.enums.Priority;
import com.devhub.common.enums.Status;
import com.devhub.project.entity.Project;
import com.devhub.user.entity.UserProfile;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Column(name = "title", nullable = false, length = 100)
    public String title;

    @Column(name = "description", length = 500)
    public String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public Status status = Status.PENDING;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    public Priority priority = Priority.MEDIUM;

    @Column(name = "due_date")
    public OffsetDateTime dueDate;

    @ManyToOne
    @JoinColumn(name = "project_id")
    public Project project;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id")
    public UserProfile assignedTo;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    public UserProfile createdBy;
}
