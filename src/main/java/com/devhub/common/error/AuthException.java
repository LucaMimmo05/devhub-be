package com.devhub.common.error;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
