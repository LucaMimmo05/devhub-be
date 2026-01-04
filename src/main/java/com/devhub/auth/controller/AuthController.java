package com.devhub.auth.controller;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.LoginRequest;
import com.devhub.auth.dto.RegisterRequest;
import com.devhub.auth.service.AuthService;
import com.devhub.auth.util.CookieUtil;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path( "/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AuthService authService;


    @Inject
    CookieUtil cookieUtil;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        NewCookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(authResponse.refreshToken);
        authResponse.refreshToken = null;
        
        return Response.ok(authResponse).cookie(refreshTokenCookie).build();
    }

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);
        NewCookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(authResponse.refreshToken);

        authResponse.refreshToken = null;

        return Response.ok(authResponse).cookie(refreshTokenCookie).build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam("refreshToken") String refreshToken) {
        if (refreshToken == null) {
            throw new NotAuthorizedException("No refresh token found");
        }
        AuthResponse authResponse = authService.refresh(refreshToken);
        
        NewCookie newRefreshTokenCookie = cookieUtil.createRefreshTokenCookie(authResponse.refreshToken);

        authResponse.refreshToken = null;

        return Response.ok(authResponse).cookie(newRefreshTokenCookie).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam("refreshToken") String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        
        NewCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
                
        return Response.ok().cookie(deleteCookie).build();
    }
}
