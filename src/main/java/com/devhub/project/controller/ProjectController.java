package com.devhub.project.controller;

import com.devhub.project.dto.ProjectRequest;
import com.devhub.project.dto.ProjectResponse;
import com.devhub.project.service.ProjectService;
import com.devhub.task.dto.TaskResponse;
import com.devhub.task.service.TaskService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ProjectController {

    @Inject
    ProjectService projectService;

    @Inject
    TaskService taskService;

    @GET
    public List<ProjectResponse> getAllUserProject(@Context SecurityContext ctx, @QueryParam("limit") Integer limit) {
        String userId = ctx.getUserPrincipal().getName();
        return projectService.getAllUserProjects(userId, limit);
    }

    @POST
    public Response createProject(@Context SecurityContext ctx, ProjectRequest request) {
        String userId = ctx.getUserPrincipal().getName();
        ProjectResponse response = projectService.createProject(request, UUID.fromString(userId));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{projectId}")
    public ProjectResponse getUserProjectById(@Context SecurityContext ctx, @PathParam("projectId") UUID projectId) {
        String userId = ctx.getUserPrincipal().getName();
        return projectService.getUserProjectById(projectId, userId);
    }

    @PUT
    @Path("/{projectId}")
    public ProjectResponse updateProject(@Context SecurityContext ctx, @PathParam("projectId") UUID projectId, ProjectRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return projectService.updateProject(projectId, request, userId);
    }

    @DELETE
    @Path("/{projectId}")
    public Response deleteProject(@Context SecurityContext ctx, @PathParam("projectId") UUID projectId) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        projectService.deleteProject(projectId, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{projectId}/members")
    public ProjectResponse addMember(@Context SecurityContext ctx, @PathParam("projectId") UUID projectId, Map<String, String> body) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UUID memberProfileId = UUID.fromString(body.get("profileId"));
        return projectService.addMember(projectId, memberProfileId, userId);
    }

    @DELETE
    @Path("/{projectId}/members/{memberProfileId}")
    public ProjectResponse removeMember(@Context SecurityContext ctx, @PathParam("projectId") UUID projectId, @PathParam("memberProfileId") UUID memberProfileId) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return projectService.removeMember(projectId, memberProfileId, userId);
    }

    @GET
    @Path("/{projectId}/tasks")
    public List<TaskResponse> getProjectTasks(
            @Context SecurityContext ctx,
            @PathParam("projectId") UUID projectId,
            @QueryParam("status") com.devhub.common.enums.Status status,
            @QueryParam("priority") com.devhub.common.enums.Priority priority,
            @QueryParam("search") String search) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return taskService.getProjectTasks(projectId, userId, status, priority, search);
    }
}
