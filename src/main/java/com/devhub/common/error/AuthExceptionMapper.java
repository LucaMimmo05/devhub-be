package com.devhub.common.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class AuthExceptionMapper implements ExceptionMapper<AuthException> {

    @Override
    public Response toResponse(AuthException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "AUTH_ERROR");
        body.put("message", exception.getMessage());

        return Response.status(Response.Status.UNAUTHORIZED).entity(body).build();
    }
}