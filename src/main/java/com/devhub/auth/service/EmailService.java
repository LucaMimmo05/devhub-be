package com.devhub.auth.service;

import com.devhub.common.error.AuthException;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
}

