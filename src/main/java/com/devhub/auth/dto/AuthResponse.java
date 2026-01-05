package com.devhub.auth.dto;

import com.devhub.security.jwt.entity.RefreshToken;
import com.devhub.user.entity.UserProfile;

import java.util.UUID;

public class AuthResponse {
    public String accessToken;
    public String refreshToken;
    public UserProfile userProfile;
    public UUID userId;

    public AuthResponse(String accessToken, String refreshToken, UserProfile userProfile, UUID userId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userProfile = userProfile;
        this.userId = userId;
    }
    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
