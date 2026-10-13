package com.devhub.user;

import com.devhub.auth.service.RateLimiter;
import com.devhub.user.entity.AccountDeletion;
import com.devhub.user.entity.User;
import com.devhub.user.repository.AccountDeletionRepository;
import com.devhub.user.repository.UserRepository;
import com.devhub.user.service.AccountDeletionService;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AccountDeletionTest {

    private static final Pattern OTP = Pattern.compile(">(\\d{6})</span>");
    private static final String PASSWORD = "a-good-password-1";

    @Inject
    MockMailbox mailbox;

    @Inject
    RateLimiter rateLimiter;

    @Inject
    AccountDeletionService accountDeletionService;

    @Inject
    UserRepository userRepository;

    @Inject
    AccountDeletionRepository accountDeletionRepository;

    @BeforeEach
    void reset() {
        rateLimiter.clear();
        mailbox.clear();
    }

    @Test
    void deletionIsScheduledAndSigningInCancelsIt() {
        TestUser user = verifiedUser();
        TestUser other = verifiedUser();

        String scheduledFor = given().auth().oauth2(user.accessToken)
                .when().delete("/users/me")
                .then().statusCode(200)
                .extract().path("deletionScheduledFor");
        OffsetDateTime deleteOn = OffsetDateTime.parse(scheduledFor);
        assertTrue(deleteOn.isAfter(OffsetDateTime.now().plusDays(6)), "about 7 days ahead: " + deleteOn);

        List<Mail> mails = mailbox.getMailsSentTo(user.email);
        assertEquals("Your DevHub account will be deleted", mails.get(mails.size() - 1).getSubject());

        // Sessions are closed and the account is hidden from member search
        given().contentType(ContentType.JSON).cookie("refreshToken", user.refreshToken).when().post("/auth/refresh").then().statusCode(401);
        given().auth().oauth2(other.accessToken).queryParam("username", user.username)
                .when().get("/users/search").then().statusCode(200).body("$", empty());

        // Signing in keeps the account
        login(user).then().statusCode(200).body("deletionCancelled", is(true));
        login(user).then().statusCode(200).body("deletionCancelled", is(false));
        given().auth().oauth2(other.accessToken).queryParam("username", user.username)
                .when().get("/users/search").then().statusCode(200).body("$", hasSize(1));

        AccountDeletion record = QuarkusTransaction.requiringNew().call(() ->
                accountDeletionRepository.find("userId", user.id).firstResult());
        assertNotNull(record.cancelledAt);
        assertNull(record.purgedAt);
        assertNull(QuarkusTransaction.requiringNew().call(() -> userRepository.findById(user.id).deletionRequestedAt));
    }

    @Test
    void purgeWaitsForTheGracePeriodThenDeletesEverything() {
        TestUser user = verifiedUser();
        given().auth().oauth2(user.accessToken).contentType(ContentType.JSON)
                .body(Map.of("title", "Doomed project", "description", "", "priority", "LOW", "status", "PENDING"))
                .when().post("/projects").then().statusCode(201);
        given().auth().oauth2(user.accessToken).when().delete("/users/me").then().statusCode(200);

        accountDeletionService.purgeExpired();
        assertNotNull(QuarkusTransaction.requiringNew().call(() -> userRepository.findById(user.id)),
                "still inside the grace period");

        // Move the request past the grace period
        QuarkusTransaction.requiringNew().run(() -> {
            User u = userRepository.findById(user.id);
            u.deletionRequestedAt = OffsetDateTime.now().minusDays(8);
        });
        accountDeletionService.purgeExpired();

        assertNull(QuarkusTransaction.requiringNew().call(() -> userRepository.findById(user.id)));
        AccountDeletion record = QuarkusTransaction.requiringNew().call(() ->
                accountDeletionRepository.find("userId", user.id).firstResult());
        assertNotNull(record.purgedAt);
        login(user).then().statusCode(401);
    }

    private record TestUser(UUID id, String email, String username, String accessToken, String refreshToken) {}

    private TestUser verifiedUser() {
        String email = "del-" + UUID.randomUUID() + "@example.test";
        String username = "d" + UUID.randomUUID().toString().substring(0, 8);
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "username", username, "password", PASSWORD, "fullName", "Deletion Test"))
                .when().post("/auth/register").then().statusCode(200);

        List<Mail> mails = mailbox.getMailsSentTo(email);
        Matcher m = OTP.matcher(mails.get(mails.size() - 1).getHtml());
        assertTrue(m.find());
        given().contentType(ContentType.JSON).body(Map.of("email", email, "otp", m.group(1)))
                .when().post("/email/verify").then().statusCode(200);

        Response login = given().contentType(ContentType.JSON).body(Map.of("email", email, "password", PASSWORD))
                .when().post("/auth/login");
        login.then().statusCode(200);
        UUID id = QuarkusTransaction.requiringNew().call(() -> userRepository.findByEmail(email).orElseThrow().id);
        return new TestUser(id, email, username, login.path("accessToken"), login.getCookie("refreshToken"));
    }

    private static Response login(TestUser user) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", user.email, "password", PASSWORD))
                .when().post("/auth/login");
    }
}
