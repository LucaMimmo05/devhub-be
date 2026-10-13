package com.devhub.auth.dto;

import com.devhub.user.entity.UserProfile;

import java.util.UUID;

public class AuthResponse {
    public String accessToken;
    public UserProfile userProfile;
    /** True when this sign-in cancelled a pending account deletion (the client tells the user). */
    public boolean deletionCancelled;

    public AuthResponse(String accessToken, UserProfile userProfile) {
        this.accessToken = accessToken;
        this.userProfile = userProfile;
    }

    public static AuthResponse of(AuthResult result) {
        AuthResponse response = new AuthResponse(result.accessToken, result.userProfile);
        response.deletionCancelled = result.deletionCancelled;
        return response;
    }

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
