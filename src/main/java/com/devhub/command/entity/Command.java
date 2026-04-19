package com.devhub.command.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.user.entity.UserProfile;
import jakarta.persistence.*;

@Entity
@Table(name = "commands")
public class Command extends BaseEntity {

    @Column(name = "title", nullable = false, length = 100)
    public String title;

    @Column(name = "command", nullable = false, columnDefinition = "TEXT")
    public String command;

    @Column(name = "description", length = 500)
    public String description;

    @Column(name = "category", nullable = false, length = 50)
    public String category;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    public UserProfile userProfile;
}
