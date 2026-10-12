package com.devhub.github.controller;

import com.devhub.github.dto.GithubDtos.*;
import com.devhub.github.service.GithubService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

/** The signed-in user's GitHub connection and the data read through it. */
@Path("/github")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class GithubController {

    @Inject
    GithubService github;

    private static UUID userId(SecurityContext ctx) {
        return UUID.fromString(ctx.getUserPrincipal().getName());
    }

    @GET
    @Path("/status")
    public Status status(@Context SecurityContext ctx) {
        return github.status(userId(ctx));
    }

    @GET
    @Path("/connect")
    public ConnectUrl connect(@Context SecurityContext ctx) {
        return github.connectUrl(userId(ctx));
    }

    @POST
    @Path("/callback")
    public Status callback(@Context SecurityContext ctx, CallbackRequest request) {
        return github.connect(userId(ctx), request);
    }

    @DELETE
    @Path("/connection")
    public Response disconnect(@Context SecurityContext ctx) {
        github.disconnect(userId(ctx));
        return Response.noContent().build();
    }

    @GET
    @Path("/repos")
    public List<Repo> repos(@Context SecurityContext ctx) {
        return github.repos(userId(ctx));
    }

    @GET
    @Path("/repos/{owner}/{name}")
    public RepoDetail repo(@Context SecurityContext ctx, @PathParam("owner") String owner, @PathParam("name") String name) {
        return github.repo(userId(ctx), owner, name);
    }

    @GET
    @Path("/commits")
    public List<Commit> commits(@Context SecurityContext ctx) {
        return github.recentCommits(userId(ctx));
    }

    @GET
    @Path("/inbox")
    public Inbox inbox(@Context SecurityContext ctx) {
        return github.inbox(userId(ctx));
    }

    @GET
    @Path("/contributions")
    public Contributions contributions(@Context SecurityContext ctx) {
        return github.contributions(userId(ctx));
    }
}
