package com.devhub.project;

import com.devhub.auth.service.RateLimiter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/** Column widths must match what the API accepts (projects.title was varchar(25) for a 100-char DTO limit). */
@QuarkusTest
class ProjectFieldLimitsTest {

    @Inject
    RateLimiter rateLimiter;

    @BeforeEach
    void reset() {
        rateLimiter.clear();
    }

    @Test
    void projectAcceptsTheFullTitleAndDescription() {
        String title = "t".repeat(100);
        String description = "d".repeat(500);

        given().auth().oauth2(register(uniqueEmail()))
                .contentType(ContentType.JSON)
                .body(Map.of("title", title, "description", description, "priority", "MEDIUM", "status", "PENDING"))
                .when().post("/projects")
                .then()
                .statusCode(201)
                .body("title", equalTo(title))
                .body("description", equalTo(description));
    }

    @Test
    void projectTitleOverTheLimitIsAValidationError() {
        given().auth().oauth2(register(uniqueEmail()))
                .contentType(ContentType.JSON)
                .body(Map.of("title", "t".repeat(101), "description", "", "priority", "MEDIUM", "status", "PENDING"))
                .when().post("/projects")
                .then()
                .statusCode(400)
                .body("errors.title", equalTo("Title must be between 3 and 100 characters"));
    }

    @Test
    void registrationRejectsAnEmailLongerThanTheColumn() {
        // 104 chars, with a local part short enough to pass @Email so only the length check fails
        String email = "e".repeat(40) + "@" + "d".repeat(58) + ".test";

        given().contentType(ContentType.JSON)
                .body(registration(email))
                .when().post("/auth/register")
                .then()
                .statusCode(400)
                .body("errors.email", equalTo("Email must be at most 100 characters"));
    }

    @Test
    void severalViolationsOnOneFieldAreStillAValidationError() {
        // Too long for the column and not a valid address: two violations on "email"
        given().contentType(ContentType.JSON)
                .body(registration("e".repeat(110)))
                .when().post("/auth/register")
                .then()
                .statusCode(400);
    }

    private static String register(String email) {
        return given().contentType(ContentType.JSON)
                .body(registration(email))
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().path("accessToken");
    }

    private static Map<String, String> registration(String email) {
        return Map.of(
                "email", email,
                "username", "u" + UUID.randomUUID().toString().substring(0, 8),
                "password", "a-good-password-1",
                "fullName", "Field Limits Test"
        );
    }

    private static String uniqueEmail() {
        return "fl-" + UUID.randomUUID() + "@example.test";
    }
}
