package com.devhub.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "VALIDATION_ERROR");

        var errors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString().split("\\.")[1],
                        ConstraintViolation::getMessage
                ));

        body.put("errors", errors);

        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}