package com.devhub.auth.controller;

import com.devhub.auth.dto.SendOtpRequest;
import com.devhub.auth.dto.VerifyOtpRequest;
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

    @Path( "/resend")
    @POST
    public Response sendOtp(SendOtpRequest request) {
        emailService.sendOtpToVerifyEmail(request.email);
        return Response.ok().build();
    }

    @Path("/verify")
    @POST
    public Response verifyOtp(VerifyOtpRequest request) {
        emailService.verifyOtpToVerifyEmail(request.otp, request.email);
        return Response.ok("Email verified").build();
    }


}
