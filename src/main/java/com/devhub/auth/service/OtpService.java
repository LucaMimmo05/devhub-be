package com.devhub.auth.service;

import com.devhub.auth.entity.OtpCode;
import com.devhub.auth.repository.OtpCodeRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class OtpService {

    @Inject
    OtpCodeRepository otpCodeRepository;

    private static final SecureRandom random = new SecureRandom();
    private static final Duration OTP_EXPIRATION = Duration.ofMinutes(5);
    public static final int MAX_ATTEMPTS = 5;

    public enum OtpCheck { VALID, INVALID, LOCKED }


    @Transactional
    public String generateOtpWithType(String email, String type) {
        int otpNumber = random.nextInt(1_000_000);
        String otp = String.format("%06d", otpNumber);

        // Bulk delete eseguito subito: evita la violazione del vincolo unique (email, type) al persist
        otpCodeRepository.deleteByEmailAndType(email, type);

        OtpCode code = new OtpCode();
        code.email = email;
        code.type = type;
        code.codeHash = BcryptUtil.bcryptHash(otp);
        code.expiresAt = Instant.now().plus(OTP_EXPIRATION);
        otpCodeRepository.persist(code);

        return otp;
    }


    /** Verifica l'OTP senza eliminarlo (usato per lo step intermedio di conferma). */
    @Transactional
    public OtpCheck peekOtpWithType(String email, String otp, String type) {
        return check(email, otp, type, false);
    }

    @Transactional
    public OtpCheck verifyOtpWithType(String email, String otp, String type) {
        return check(email, otp, type, true);
    }

    /**
     * Ogni codice sbagliato incrementa {@code attempts}; al MAX_ATTEMPTS-esimo il codice viene cancellato
     * e ne serve uno nuovo. Restituisce un esito invece di lanciare: un'eccezione annullerebbe l'incremento.
     */
    private OtpCheck check(String email, String otp, String type, boolean consume) {
        Optional<OtpCode> found = otpCodeRepository.findValid(email, type);
        if (found.isEmpty()) {
            return OtpCheck.INVALID;
        }

        OtpCode code = found.get();
        if (BcryptUtil.matches(otp, code.codeHash)) {
            if (consume) {
                otpCodeRepository.delete(code);
            }
            return OtpCheck.VALID;
        }

        code.attempts++;
        if (code.attempts >= MAX_ATTEMPTS) {
            otpCodeRepository.delete(code);
            return OtpCheck.LOCKED;
        }
        return OtpCheck.INVALID;
    }

    public boolean hasActiveEmailVerificationRequest(String email) {
        return otpCodeRepository.findValid(email, "EMAIL_CONFIRMATION").isPresent();
    }

    @Scheduled(every = "1h")
    @Transactional
    void cleanExpiredOtps() {
        otpCodeRepository.deleteExpired();
    }
}
