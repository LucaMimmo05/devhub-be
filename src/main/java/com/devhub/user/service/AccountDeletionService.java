package com.devhub.user.service;

import com.devhub.auth.service.EmailService;
import com.devhub.security.jwt.repository.RefreshTokenRepository;
import com.devhub.user.entity.AccountDeletion;
import com.devhub.user.entity.User;
import com.devhub.user.repository.AccountDeletionRepository;
import com.devhub.user.repository.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Deferred account deletion: a request only schedules it, signing in during the grace period cancels it,
 * and {@link #purgeExpired()} deletes the account for good once the period has passed.
 * Every step is logged and recorded in {@code account_deletions}.
 */
@ApplicationScoped
public class AccountDeletionService {

    private static final Logger LOG = Logger.getLogger(AccountDeletionService.class);

    @Inject
    UserRepository userRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    AccountDeletionRepository accountDeletionRepository;

    @Inject
    UserService userService;

    @Inject
    EmailService emailService;

    @ConfigProperty(name = "devhub.account-deletion.grace-period", defaultValue = "P7D")
    Duration gracePeriod;

    private record PendingDeletion(String email, OffsetDateTime deleteOn) {}

    /** Schedules the deletion of the account, ends its sessions and emails the date; returns that date. */
    public OffsetDateTime requestDeletion(UUID userId) {
        PendingDeletion scheduled = QuarkusTransaction.requiringNew().call(() -> markForDeletion(userId));

        // After the commit, so the email never announces a deletion that was rolled back
        try {
            emailService.sendAccountDeletionScheduled(scheduled.email(), scheduled.deleteOn());
        } catch (Exception e) {
            LOG.warnf(e, "Account deletion email not sent for user %s", userId);
        }
        return scheduled.deleteOn();
    }

    private PendingDeletion markForDeletion(UUID userId) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // A second request keeps the original date
        if (user.deletionRequestedAt == null) {
            OffsetDateTime now = OffsetDateTime.now();
            user.deletionRequestedAt = now;
            accountDeletionRepository.persist(AccountDeletion.requested(userId, now, AccountDeletion.SOURCE_USER));
            LOG.infof("Account deletion requested: user %s, purge after %s", userId, now.plus(gracePeriod));
        }

        refreshTokenRepository.delete("user.id", userId);
        return new PendingDeletion(user.email, user.deletionRequestedAt.plus(gracePeriod));
    }

    /**
     * Called when the user signs in (password or GitHub) inside the caller's transaction.
     * Returns true when a pending deletion was cancelled.
     */
    public boolean cancelIfPending(User user) {
        if (user.deletionRequestedAt == null) {
            return false;
        }

        user.deletionRequestedAt = null;
        accountDeletionRepository.findOpen(user.id).ifPresent(d -> d.cancelledAt = OffsetDateTime.now());
        LOG.infof("Account deletion cancelled by sign-in: user %s", user.id);
        return true;
    }

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void purgeExpired() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(gracePeriod);
        List<UUID> due = QuarkusTransaction.requiringNew().call(() -> userRepository.findDueForPurge(cutoff));

        for (UUID userId : due) {
            try {
                QuarkusTransaction.requiringNew().run(() -> purge(userId, cutoff));
            } catch (Exception e) {
                LOG.errorf(e, "Account purge failed: user %s (will retry next run)", userId);
            }
        }
    }

    private void purge(UUID userId, OffsetDateTime cutoff) {
        // Re-check inside the transaction: the user may have signed in since the query
        User user = userRepository.findById(userId);
        if (user == null || user.deletionRequestedAt == null || user.deletionRequestedAt.isAfter(cutoff)) {
            return;
        }

        accountDeletionRepository.findOpen(userId).ifPresent(d -> d.purgedAt = OffsetDateTime.now());
        userService.deleteUser(userId);
        LOG.infof("Account purged: user %s (deletion requested %s)", userId, user.deletionRequestedAt);
    }
}
