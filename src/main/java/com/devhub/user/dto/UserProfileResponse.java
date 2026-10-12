package com.devhub.user.dto;

import java.util.UUID;

public class UserProfileResponse {
    public UUID id;
    public String firstName;
    public String lastName;
    public String username;
    public String avatarUrl;
    /** Only filled for the signed-in user's own profile (GET /users/me), never in search results. */
    public String email;
}
