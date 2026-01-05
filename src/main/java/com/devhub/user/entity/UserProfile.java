package com.devhub.user.entity;

import com.devhub.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    public User user;

    @Column(name = "first_name", nullable = false, length = 100)
    public String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    public String lastName;

    @Column(name = "avatar_url")
    public String avatarUrl;

    public static UserProfile createForUser(User user, String firstName, String lastName) {
        UserProfile profile = new UserProfile();
        profile.user = user;
        profile.firstName = firstName;
        profile.lastName = lastName;
        return profile;
    }
}