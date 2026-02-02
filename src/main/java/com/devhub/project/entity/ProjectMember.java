package com.devhub.project.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.common.enums.ProjectRole;
import com.devhub.user.entity.UserProfile;
import jakarta.persistence.*;

@Entity
@Table(name = "project_members")
public class ProjectMember extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    public UserProfile userProfile;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    public ProjectRole role;
}
