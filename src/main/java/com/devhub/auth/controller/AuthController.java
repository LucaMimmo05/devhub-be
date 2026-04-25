package com.devhub.auth.controller;

import com.devhub.auth.dto.*;
import com.devhub.auth.service.AuthService;
import com.devhub.auth.service.EmailService;
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
    EmailService emailService;

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

        try {
            emailService.sendOtpToVerifyEmail(registerRequest.email);
        } catch (Exception e) {
            // Non blocchiamo la registrazione se l'invio dell'email fallisce
        }

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

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(@Valid SendOtpRequest request) {
        emailService.sendPasswordResetOtp(request.email);
        // Risposta generica per non rivelare se l'email esiste
        return Response.ok().build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        emailService.verifyOtpAndResetPassword(request.email, request.otp, request.newPassword);
        return Response.ok().build();
    }

    @POST
    @Path("/verify-reset-otp")
    public Response verifyResetOtp(@Valid VerifyOtpRequest request) {
        emailService.verifyResetOtp(request.email, request.otp);
        return Response.ok().build();
    }

    @POST
    @Path("/resend-reset-otp")
    public Response resendResetOtp(@Valid SendOtpRequest request) {
        emailService.sendPasswordResetOtp(request.email);
        return Response.ok().build();
    }
}
