package com.devhub.command.controller;

import com.devhub.command.dto.CommandRequest;
import com.devhub.command.dto.CommandResponse;
import com.devhub.command.service.CommandService;
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

@Path("/commands")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class CommandController {

    @Inject CommandService commandService;

    @GET
    public List<CommandResponse> getMyCommands(
            @Context SecurityContext ctx,
            @QueryParam("category") String category,
            @QueryParam("search") String search) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return commandService.getUserCommands(userId, category, search);
    }

    @POST
    public Response createCommand(@Context SecurityContext ctx, @Valid CommandRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        CommandResponse response = commandService.createCommand(request, userId);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public CommandResponse updateCommand(@Context SecurityContext ctx, @PathParam("id") UUID id, @Valid CommandRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return commandService.updateCommand(id, request, userId);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCommand(@Context SecurityContext ctx, @PathParam("id") UUID id) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        commandService.deleteCommand(id, userId);
        return Response.noContent().build();
    }
}
