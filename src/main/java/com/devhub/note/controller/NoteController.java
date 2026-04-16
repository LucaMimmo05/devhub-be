package com.devhub.note.controller;

import com.devhub.note.dto.NoteRequest;
import com.devhub.note.dto.NoteResponse;
import com.devhub.note.service.NoteService;
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

@Path("/notes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class NoteController {

    @Inject
    NoteService noteService;

    @GET
    public List<NoteResponse> getMyNotes(@Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return noteService.getUserNotes(userId);
    }

    @POST
    public Response createNote(@Context SecurityContext ctx, @Valid NoteRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        NoteResponse response = noteService.createNote(request, userId);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{noteId}")
    public NoteResponse updateNote(@Context SecurityContext ctx, @PathParam("noteId") UUID noteId, @Valid NoteRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        return noteService.updateNote(noteId, request, userId);
    }

    @DELETE
    @Path("/{noteId}")
    public Response deleteNote(@Context SecurityContext ctx, @PathParam("noteId") UUID noteId) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        noteService.deleteNote(noteId, userId);
        return Response.noContent().build();
    }
}
