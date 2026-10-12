package com.devhub.github.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.user.entity.UserProfile;
import jakarta.persistence.*;

/** A DevHub profile's linked GitHub account. One per profile; the OAuth token is stored encrypted. */
@Entity
@Table(name = "github_connections")
public class GithubConnection extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    public UserProfile userProfile;

    @Column(name = "github_id", nullable = false)
    public Long githubId;

    @Column(name = "github_login", nullable = false, length = 100)
    public String login;

    @Column(name = "github_name", length = 255)
    public String name;

    @Column(name = "github_avatar_url", length = 500)
    public String avatarUrl;

    /** AES-GCM encrypted OAuth access token (see TokenCipher). */
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    public String encryptedToken;

    @Column(name = "scopes", length = 255)
    public String scopes;
}
