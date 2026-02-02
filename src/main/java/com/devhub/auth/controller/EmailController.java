package com.devhub.auth.controller;

import com.devhub.auth.dto.VerifyEmailRequest;
import com.devhub.auth.service.EmailService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path( "/email")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmailController {


    @Inject
    EmailService emailService;

    @Path( "/verify")
    @POST
    public Response verifyEmail(VerifyEmailRequest request) {
        emailService.confirmEmail(request.email);
        return Response.ok().build();
    }
}
