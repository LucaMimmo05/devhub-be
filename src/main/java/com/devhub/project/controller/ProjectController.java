package com.devhub.project.controller;

import com.devhub.project.dto.ProjectRequest;
import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.service.ProjectService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ProjectController {

    @Inject
    ProjectService projectService;


    @GET
    public List<ProjectResponse> getAllUserProject(@Context SecurityContext ctx, @QueryParam("limit") Integer limit) {
        String userId = ctx.getUserPrincipal().getName();
        return projectService.getAllUserProjects(userId, limit);
    }



    @POST
    public ProjectResponse createProject(@Context SecurityContext ctx,ProjectRequest request) {
        String userId = ctx.getUserPrincipal().getName();
        return projectService.createProject(request, UUID.fromString(userId));
    }
}
