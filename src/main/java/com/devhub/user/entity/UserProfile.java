package com.devhub.user.entity;

import com.devhub.command.entity.Command;
import com.devhub.common.entity.BaseEntity;
import com.devhub.note.entity.Note;
import com.devhub.project.entity.Project;
import com.devhub.project.entity.ProjectMember;
import com.devhub.task.entity.Task;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

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
    public String avatarUrl = "";

    @Column(name = "username", length = 50, unique = true)
    public String username;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    public List<Note> notes = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    public List<Command> commands = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    public List<Project> ownedProjects = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    public List<ProjectMember> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    public List<Task> createdTasks = new ArrayList<>();

    public static UserProfile createForUser(User user, String firstName, String lastName, String avatarUrl, String username) {
        UserProfile profile = new UserProfile();
        profile.user = user;
        profile.firstName = firstName;
        profile.lastName = lastName;
        profile.avatarUrl = avatarUrl;
        profile.username = username;
        return profile;
    }
}