package com.devhub.common.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class TooManyRequestsExceptionMapper implements ExceptionMapper<TooManyRequestsException> {

    @Override
    public Response toResponse(TooManyRequestsException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "RATE_LIMITED");
        body.put("message", exception.getMessage());
        body.put("status", Response.Status.TOO_MANY_REQUESTS.getStatusCode());

        Response.ResponseBuilder response = Response.status(Response.Status.TOO_MANY_REQUESTS).entity(body);
        if (exception.getRetryAfter() != null) {
            response.header("Retry-After", Math.max(1, exception.getRetryAfter().toSeconds()));
        }
        return response.build();
    }
}
