package com.devhub.support;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/** Creates verified, signed-in users through the public API (codes read from the mock mailbox). */
@ApplicationScoped
public class TestUsers {

    public static final String PASSWORD = "a-good-password-1";
    private static final Pattern OTP = Pattern.compile(">(\\d{6})</span>");

    @Inject
    MockMailbox mailbox;

    public record TestUser(String email, String username, UUID profileId, String accessToken) {}

    public TestUser create() {
        String email = "user-" + UUID.randomUUID() + "@example.test";
        String username = "u" + UUID.randomUUID().toString().substring(0, 8);
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "username", username, "password", PASSWORD, "fullName", "Test User"))
                .when().post("/auth/register").then().statusCode(200);

        List<Mail> mails = mailbox.getMailsSentTo(email);
        Matcher m = OTP.matcher(mails.get(mails.size() - 1).getHtml());
        if (!m.find()) throw new IllegalStateException("no verification code sent to " + email);
        given().contentType(ContentType.JSON).body(Map.of("email", email, "otp", m.group(1)))
                .when().post("/email/verify").then().statusCode(200);

        Response login = given().contentType(ContentType.JSON).body(Map.of("email", email, "password", PASSWORD))
                .when().post("/auth/login");
        login.then().statusCode(200);
        String token = login.path("accessToken");

        String profileId = given().auth().oauth2(token).when().get("/users/me")
                .then().statusCode(200).extract().path("id");
        return new TestUser(email, username, UUID.fromString(profileId), token);
    }
}
