package com.devhub.auth.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class CookieUtil {

    public NewCookie createRefreshTokenCookie(String token) {
        return new NewCookie.Builder("refreshToken")
                .value(token)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .maxAge(30 * 24 * 60 * 60)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }

    public NewCookie deleteRefreshTokenCookie() {
        return new NewCookie.Builder("refreshToken")
                .value("")
                .path("/")
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }
}
