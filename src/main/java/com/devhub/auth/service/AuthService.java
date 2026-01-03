package com.devhub.auth.service;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.LoginRequest;
import com.devhub.security.jwt.JwtService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {


    @Inject
    JwtService jwtService;

    public AuthResponse login(LoginRequest request) {

        // TODO DB
        // Check if user exist
        // Check if email and password match
        // Check isActive / isEmailVerified
        String token =jwtService.generateAccessToken(request.email, "USER");
        return new AuthResponse(token);
    }
}
