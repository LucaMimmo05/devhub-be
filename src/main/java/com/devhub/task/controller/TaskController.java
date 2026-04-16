package com.devhub.task.controller;

import com.devhub.task.dto.TaskRequest;
import com.devhub.task.dto.TaskResponse;
import com.devhub.task.service.TaskService;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

@Path("/tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class TaskController {

    @Inject
    TaskService taskService;

    @Inject
    UserProfileRepository userProfileRepository;

    @GET
    public List<TaskResponse> getMyTasks(@Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");
        return taskService.getUserTasks(profile.id);
    }

    @POST
    public Response createTask(@Context SecurityContext ctx, @Valid TaskRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        TaskResponse response = taskService.createTask(request, userId);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{taskId}")
    public TaskResponse updateTask(@Context SecurityContext ctx, @PathParam("taskId") UUID taskId, TaskRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return taskService.updateTask(taskId, request, userId);
    }

    @DELETE
    @Path("/{taskId}")
    public Response deleteTask(@Context SecurityContext ctx, @PathParam("taskId") UUID taskId) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        taskService.deleteTask(taskId, userId);
        return Response.noContent().build();
    }
}
