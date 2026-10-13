package com.devhub.user.dto;

import java.time.OffsetDateTime;

/** Answer to DELETE /users/me: when the account will be purged unless the user signs in again. */
public record AccountDeletionResponse(OffsetDateTime deletionScheduledFor) {
}
