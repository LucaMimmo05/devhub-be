package com.devhub.github.controller;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.AuthResult;
import com.devhub.auth.util.CookieUtil;
import com.devhub.github.dto.GithubDtos.CallbackRequest;
import com.devhub.github.dto.GithubDtos.ConnectUrl;
import com.devhub.github.service.GithubService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Public "Sign in with GitHub": same session shape as POST /auth/login (access token + refresh cookie). */
@Path("/auth/github")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GithubAuthController {

    @Inject
    GithubService github;

    @Inject
    CookieUtil cookieUtil;

    @GET
    @Path("/url")
    public ConnectUrl url() {
        return github.loginUrl();
    }

    @POST
    public Response login(CallbackRequest request) {
        AuthResult result = github.login(request);
        return Response.ok(new AuthResponse(result.accessToken, result.userProfile))
                .cookie(cookieUtil.createRefreshTokenCookie(result.refreshToken))
                .build();
    }
}
