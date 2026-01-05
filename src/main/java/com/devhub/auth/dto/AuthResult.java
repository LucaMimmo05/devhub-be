package com.devhub.auth.dto;

import com.devhub.user.entity.UserProfile;
import java.util.UUID;

public class AuthResult {
    public String accessToken;
    public String refreshToken;
    public UserProfile userProfile;

    public AuthResult(String accessToken, String refreshToken, UserProfile userProfile) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userProfile = userProfile;
    }

    public AuthResult(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
