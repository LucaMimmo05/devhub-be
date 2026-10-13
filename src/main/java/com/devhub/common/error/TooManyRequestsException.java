package com.devhub.common.error;

import java.time.Duration;

/** Answered with 429; {@code retryAfter} (nullable) becomes the Retry-After header. */
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
