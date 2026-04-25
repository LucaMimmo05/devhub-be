package com.devhub.auth.service;

import com.devhub.common.error.AuthException;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;

@ApplicationScoped
public class EmailService {


    @Inject
    Mailer mailer;

    @Inject
    AuthService authService;

    @Inject
    OtpService otpService;

    @Inject
    @Location("confirmEmail.html")
    Template confirmEmailTemplate;

    @Inject
    @Location("resetPassword.html")
    Template resetPasswordTemplate;

    public void sendOtpToVerifyEmail(String toEmail) {
        if (!authService.isEmailExists(toEmail)) {
            throw new AuthException("Email does not exist: " + toEmail);
        }

        if(otpService.hasActiveEmailVerificationRequest(toEmail)) {
            throw new AuthException("A reset request is already in progress for this email");
        }
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
        boolean isValid = otpService.verifyOtpWithType(email, otp, "EMAIL_CONFIRMATION");

        if (!isValid) {
            throw new AuthException("Invalid OTP");
        }

        authService.markEmailAsVerified(email);
    }

    public void sendPasswordResetOtp(String toEmail) {
        if (!authService.isEmailExists(toEmail)) {
            // Non riveliamo se l'email esiste o meno per sicurezza
            return;
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

    public void verifyOtpAndResetPassword(String email, String otp, @NotBlank String newPassword) {
        boolean isValid = otpService.verifyOtpWithType(email, otp, "PASSWORD_RESET");

        if (!isValid) {
            throw new AuthException("Invalid or expired reset code");
        }

        authService.resetPassword(email, newPassword);
    }
}

