package com.devhub.auth.service;

import com.devhub.auth.service.OtpService.OtpCheck;
import com.devhub.common.error.AuthException;
import com.devhub.common.error.TooManyRequestsException;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

@ApplicationScoped
public class EmailService {


    @Inject
    Mailer mailer;

    @Inject
    AuthService authService;

    @Inject
    OtpService otpService;

    @Inject
    RateLimiter rateLimiter;

    private static final int OTP_SENDS_PER_EMAIL = 3;
    private static final Duration OTP_SEND_WINDOW = Duration.ofMinutes(15);

    @Inject
    @Location("confirmEmail.html")
    Template confirmEmailTemplate;

    @Inject
    @Location("resetPassword.html")
    Template resetPasswordTemplate;

    public void sendOtpToVerifyEmail(String toEmail) {
        rateLimiter.check("otp-send:" + RateLimiter.normalize(toEmail), OTP_SENDS_PER_EMAIL, OTP_SEND_WINDOW);
        if (!authService.isEmailExists(toEmail)) {
            throw new AuthException("Email does not exist: " + toEmail);
        }

        // Sovrascriviamo sempre l'OTP esistente: permette il resend senza errori
        String otp = otpService.generateOtpWithType(toEmail, "EMAIL_CONFIRMATION");
        String bodyHtml = confirmEmailTemplate
                .data("otp", otp)
                .render();

        Mail email = Mail.withHtml(
                toEmail,
                "Your Verification Code",
                bodyHtml
        );

        mailer.send(email);
    }

    public void verifyOtpToVerifyEmail(String otp, String email) {
        requireValid(otpService.verifyOtpWithType(email, otp, "EMAIL_CONFIRMATION"), "Invalid OTP");

        authService.markEmailAsVerified(email);
    }

    public void sendPasswordResetOtp(String toEmail) {
        rateLimiter.check("otp-send:" + RateLimiter.normalize(toEmail), OTP_SENDS_PER_EMAIL, OTP_SEND_WINDOW);
        if (!authService.isEmailExists(toEmail)) {
            throw new AuthException("No account found with that email address");
        }

        String otp = otpService.generateOtpWithType(toEmail, "PASSWORD_RESET");
        String bodyHtml = resetPasswordTemplate
                .data("otp", otp)
                .render();

        Mail email = Mail.withHtml(
                toEmail,
                "Reset your DevHub password",
                bodyHtml
        );

        mailer.send(email);
    }

    public void verifyResetOtp(String email, String otp) {
        requireValid(otpService.peekOtpWithType(email, otp, "PASSWORD_RESET"), "Invalid or expired reset code");
    }

    public void verifyOtpAndResetPassword(String email, String otp, @NotBlank String newPassword) {
        requireValid(otpService.verifyOtpWithType(email, otp, "PASSWORD_RESET"), "Invalid or expired reset code");

        authService.resetPassword(email, newPassword);
    }

    private static void requireValid(OtpCheck check, String invalidMessage) {
        switch (check) {
            case VALID -> { }
            // Codice cancellato dopo troppi errori: va richiesto un nuovo codice, non c'è un tempo di attesa
            case LOCKED -> throw new TooManyRequestsException("OTP_LOCKED", null);
            case INVALID -> throw new AuthException(invalidMessage);
        }
    }
}
