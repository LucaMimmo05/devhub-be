package com.devhub.user.controller;

import com.devhub.user.dto.UpdateProfileRequest;
import com.devhub.user.dto.UserProfileResponse;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class UserController {

    @Inject
    UserProfileRepository userProfileRepository;

    @GET
    @Path("/me")
    public UserProfileResponse getMe(@Context SecurityContext ctx) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");
        return toResponse(profile);
    }

    @PUT
    @Path("/me")
    @Transactional
    public UserProfileResponse updateMe(@Context SecurityContext ctx, @Valid UpdateProfileRequest request) {
        UUID userId = UUID.fromString(ctx.getUserPrincipal().getName());
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");

        if (request.firstName != null && !request.firstName.isBlank()) profile.firstName = request.firstName;
        if (request.lastName != null && !request.lastName.isBlank()) profile.lastName = request.lastName;
        if (request.avatarUrl != null) profile.avatarUrl = request.avatarUrl;

        userProfileRepository.persist(profile);
        return toResponse(profile);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        UserProfileResponse dto = new UserProfileResponse();
        dto.id = profile.id;
        dto.firstName = profile.firstName;
        dto.lastName = profile.lastName;
        dto.username = profile.username;
        dto.avatarUrl = profile.avatarUrl;
        return dto;
    }
}
