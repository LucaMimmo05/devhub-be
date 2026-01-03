package com.devhub.user.entity;

import com.devhub.common.entity.BaseEntity;
import com.devhub.common.enums.UserRole;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    public String email;

    @Column(unique = true, nullable = false, length = 50)
    public String username;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    public UserRole role = UserRole.USER;

    @Column(name = "is_email_verified")
    public boolean isEmailVerified = false;

    @Column(name = "is_active")
    public boolean isActive = true;


    public static User createNew(String email, String username, String password) {
        User user = new User();
        user.email = email;
        user.username = username;
        user.passwordHash = BcryptUtil.bcryptHash(password);
        user.role = UserRole.USER;
        user.isActive = true;
        user.isEmailVerified = false;
        return user;
    }
}