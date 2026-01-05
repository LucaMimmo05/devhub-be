package com.devhub.auth.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class CookieUtil {

    public NewCookie createRefreshTokenCookie(String token) {
        return new NewCookie.Builder("refreshToken")
                .value(token)
                .path("/auth")
                .httpOnly(true)
                .secure(false)
                .maxAge(7 * 24 * 60 * 60)
                .build();
    }

    public NewCookie deleteRefreshTokenCookie() {
        return new NewCookie.Builder("refreshToken")
                .value("")
                .path("/auth")
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .build();
    }
}
