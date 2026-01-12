package com.devhub.project.controller;

import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.service.ProjectService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ProjectController {

    @Inject
    ProjectService projectService;


    @GET
    public List<ProjectResponse> getAllUserProject(@Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        return projectService.getAllUserProjects(userId);
    }
}
