package com.devhub.github.service;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/** GitHub OAuth app settings (GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_REDIRECT_URI, GITHUB_TOKEN_KEY). */
@ConfigMapping(prefix = "github")
public interface GithubConfig {

    Optional<String> clientId();

    Optional<String> clientSecret();

    /** Where GitHub sends the user back: the frontend callback page, which forwards the code to the API. */
    @WithDefault("http://localhost:5173/github/callback")
    String redirectUri();

    /** Encryption key material for stored tokens; defaults to the client secret when unset. */
    Optional<String> tokenKey();

    /** Requested scopes: profile, verified email (for "Sign in with GitHub") and repo read (private repos need "repo"). */
    @WithDefault("read:user user:email repo")
    String scopes();

    default boolean configured() {
        return clientId().filter(s -> !s.isBlank()).isPresent() && clientSecret().filter(s -> !s.isBlank()).isPresent();
    }
}
