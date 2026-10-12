package com.devhub.github.service;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * The stored GitHub token no longer works (revoked, expired, undecryptable).
 * Deliberately 409, not 401: the frontend treats 401 as "DevHub session expired" and logs the user out.
 */
public class GithubReauthException extends WebApplicationException {
    public GithubReauthException() {
        super("Your GitHub authorization has expired. Reconnect your GitHub account.", Response.Status.CONFLICT);
    }
}
