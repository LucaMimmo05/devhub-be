package com.devhub.auth.controller;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.AuthResult;
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
        AuthResult result = authService.login(loginRequest);
        NewCookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(result.refreshToken);
        
        AuthResponse response = new AuthResponse(result.accessToken, result.userProfile);
        
        return Response.ok(response).cookie(refreshTokenCookie).build();
    }

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest registerRequest) {
        AuthResult result = authService.register(registerRequest);
        NewCookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(result.refreshToken);

        AuthResponse response = new AuthResponse(result.accessToken, result.userProfile);

        return Response.ok(response).cookie(refreshTokenCookie).build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam("refreshToken") String refreshToken) {
        if (refreshToken == null) {
            throw new NotAuthorizedException("No refresh token found");
        }
        AuthResult result = authService.refresh(refreshToken);
        
        NewCookie newRefreshTokenCookie = cookieUtil.createRefreshTokenCookie(result.refreshToken);

        AuthResponse response = new AuthResponse(result.accessToken, result.userProfile);

        return Response.ok(response).cookie(newRefreshTokenCookie).build();
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
