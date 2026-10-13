package com.devhub.auth;

import com.devhub.auth.service.RateLimiter;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthRateLimitTest {

    private static final Pattern OTP = Pattern.compile(">(\\d{6})</span>");

    @Inject
    MockMailbox mailbox;

    @Inject
    RateLimiter rateLimiter;

    @BeforeEach
    void reset() {
        rateLimiter.clear();
        mailbox.clear();
    }

    @Test
    void wrongCodesBelowTheLimitKeepTheCodeValid() {
        String email = register();
        String otp = lastOtp(email);

        for (int i = 0; i < 4; i++) {
            post("/email/verify", Map.of("email", email, "otp", wrong(otp))).statusCode(401);
        }
        post("/email/verify", Map.of("email", email, "otp", otp)).statusCode(200);
    }

    @Test
    void fifthWrongCodeInvalidatesTheCode() {
        String email = register();
        String otp = lastOtp(email);

        for (int i = 0; i < 4; i++) {
            post("/email/verify", Map.of("email", email, "otp", wrong(otp))).statusCode(401);
        }
        post("/email/verify", Map.of("email", email, "otp", wrong(otp)))
                .statusCode(429)
                .body("message", equalTo("OTP_LOCKED"));

        // The right code no longer works: a new one has to be requested
        post("/email/verify", Map.of("email", email, "otp", otp)).statusCode(401);
    }

    @Test
    void resetCodeCheckCountsAttemptsToo() {
        String email = register();
        post("/auth/forgot-password", Map.of("email", email)).statusCode(200);
        String otp = lastOtp(email);

        for (int i = 0; i < 4; i++) {
            post("/auth/verify-reset-otp", Map.of("email", email, "otp", wrong(otp))).statusCode(401);
        }
        post("/auth/verify-reset-otp", Map.of("email", email, "otp", wrong(otp))).statusCode(429);

        post("/auth/reset-password", Map.of("email", email, "otp", otp, "newPassword", "another-password-1"))
                .statusCode(401);
    }

    @Test
    void codeSendsAreLimitedPerEmail() {
        String email = register(); // sends the first code

        post("/email/resend", Map.of("email", email)).statusCode(200);
        post("/email/resend", Map.of("email", email)).statusCode(200);
        post("/email/resend", Map.of("email", email))
                .statusCode(429)
                .header("Retry-After", notNullValue())
                .body("message", equalTo("TOO_MANY_ATTEMPTS"));
    }

    @Test
    void loginsAreLimitedPerEmail() {
        String email = register();

        for (int i = 0; i < 10; i++) {
            post("/auth/login", Map.of("email", email, "password", "wrong-password")).statusCode(401);
        }
        post("/auth/login", Map.of("email", email, "password", "wrong-password")).statusCode(429);
    }

    @Test
    void authEndpointsAreLimitedPerIp() {
        for (int i = 0; i < 30; i++) {
            post("/auth/login", Map.of("email", uniqueEmail(), "password", "wrong-password")).statusCode(401);
        }
        post("/auth/login", Map.of("email", uniqueEmail(), "password", "wrong-password")).statusCode(429);
    }

    private String register() {
        String email = uniqueEmail();
        String username = "u" + UUID.randomUUID().toString().substring(0, 8);
        post("/auth/register", Map.of(
                "email", email,
                "username", username,
                "password", "a-good-password-1",
                "fullName", "Rate Limit Test"
        )).statusCode(200);
        return email;
    }

    private String lastOtp(String email) {
        List<Mail> mails = mailbox.getMailsSentTo(email);
        assertTrue(mails != null && !mails.isEmpty(), "no email sent to " + email);
        Matcher m = OTP.matcher(mails.get(mails.size() - 1).getHtml());
        assertTrue(m.find(), "no code in the email");
        return m.group(1);
    }

    private static String wrong(String otp) {
        return String.format("%06d", (Integer.parseInt(otp) + 1) % 1_000_000);
    }

    private static String uniqueEmail() {
        return "rl-" + UUID.randomUUID() + "@example.test";
    }

    private static ValidatableResponse post(String path, Map<String, String> body) {
        return given().contentType(ContentType.JSON).body(body).when().post(path).then();
    }
}
