package com.devhub.note.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.user.entity.UserProfile;
import jakarta.persistence.*;

@Entity
@Table(name = "notes")
public class Note extends BaseEntity {

    @Column(name = "title", nullable = false, length = 100)
    public String title;

    @Column(name = "content", columnDefinition = "TEXT")
    public String content;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    public UserProfile userProfile;
}
