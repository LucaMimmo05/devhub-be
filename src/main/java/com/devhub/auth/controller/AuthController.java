package com.devhub.auth.controller;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.LoginRequest;
import com.devhub.auth.dto.RegisterRequest;
import com.devhub.auth.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path( "/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;


    @POST
    @Path("/login")
    public AuthResponse login(@Valid LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @POST
    @Path("/register")
    public AuthResponse register(@Valid RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }
}
