package com.devhub.auth.util;

import com.devhub.auth.service.RateLimiter;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@Provider
@RateLimited
@Priority(Priorities.AUTHENTICATION)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final int REQUESTS_PER_IP = 30;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Inject
    RateLimiter rateLimiter;

    @Context
    HttpServerRequest request;

    @ConfigProperty(name = "devhub.rate-limit.trust-forwarded-for", defaultValue = "false")
    boolean trustForwardedFor;

    @Override
    public void filter(ContainerRequestContext context) {
        rateLimiter.check("auth-ip:" + clientIp(), REQUESTS_PER_IP, WINDOW);
    }

    private String clientIp() {
        String forwarded = trustForwardedFor ? request.getHeader("X-Forwarded-For") : null;
        if (forwarded != null && !forwarded.isBlank()) {
            // Earlier entries are client-supplied; the last one is added by our proxy
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.remoteAddress() == null ? "unknown" : request.remoteAddress().hostAddress();
    }
}
