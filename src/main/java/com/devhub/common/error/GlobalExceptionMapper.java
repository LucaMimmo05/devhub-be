package com.devhub.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "VALIDATION_ERROR");

        // Path is "method.param.field": key on the field; several violations on one field keep the first
        var errors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> fieldName(v),
                        ConstraintViolation::getMessage,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        body.put("errors", errors);
        // Clients that only read `message` (the frontend's apiErrorMessage) still get something readable
        errors.values().stream().findFirst().ifPresent(m -> body.put("message", m));

        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }
}