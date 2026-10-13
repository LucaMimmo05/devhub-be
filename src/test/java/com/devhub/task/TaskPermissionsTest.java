package com.devhub.task;

import com.devhub.auth.service.RateLimiter;
import com.devhub.support.TestUsers;
import com.devhub.support.TestUsers.TestUser;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/** Regression tests for roadmap P0 4b: tasks could be added to, and assigned within, any project. */
@QuarkusTest
class TaskPermissionsTest {

    @Inject
    RateLimiter rateLimiter;

    @Inject
    TestUsers users;

    TestUser owner;
    TestUser member;
    TestUser stranger;
    String projectId;

    @BeforeEach
    void setUp() {
        rateLimiter.clear();
        owner = users.create();
        member = users.create();
        stranger = users.create();

        projectId = given().auth().oauth2(owner.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("title", "Shared project", "description", "", "priority", "MEDIUM", "status", "PENDING"))
                .when().post("/projects").then().statusCode(201)
                .extract().path("id");
        given().auth().oauth2(owner.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("profileId", member.profileId().toString()))
                .when().post("/projects/" + projectId + "/members").then().statusCode(200);
    }

    @Test
    void strangerCannotAddTasksToSomeoneElsesProject() {
        createTask(stranger, null)
                .statusCode(403)
                .body("message", equalTo("Only the project owner or members can add tasks"));
    }

    @Test
    void memberCanAddTasksAssignedToThemselvesByDefault() {
        createTask(member, null)
                .statusCode(201)
                .body("assignedToProfileId", equalTo(member.profileId().toString()));
    }

    @Test
    void tasksCanOnlyBeAssignedToTheOwnerOrMembers() {
        createTask(owner, stranger.profileId()).statusCode(400);
        createTask(owner, UUID.randomUUID()).statusCode(400);

        String taskId = createTask(owner, member.profileId())
                .statusCode(201)
                .body("assignedToProfileId", equalTo(member.profileId().toString()))
                .extract().path("id");

        updateAssignee(owner, taskId, stranger.profileId()).statusCode(400);
        updateAssignee(owner, taskId, owner.profileId())
                .statusCode(200)
                .body("assignedToProfileId", equalTo(owner.profileId().toString()));
    }

    private ValidatableResponse createTask(TestUser as, UUID assignee) {
        Map<String, Object> body = new HashMap<>(Map.of("title", "A task", "projectId", projectId));
        if (assignee != null) body.put("assignedToProfileId", assignee.toString());
        return given().auth().oauth2(as.accessToken()).contentType(ContentType.JSON)
                .body(body).when().post("/tasks").then();
    }

    private ValidatableResponse updateAssignee(TestUser as, String taskId, UUID assignee) {
        return given().auth().oauth2(as.accessToken()).contentType(ContentType.JSON)
                .body(Map.of("title", "A task", "projectId", projectId, "assignedToProfileId", assignee.toString()))
                .when().put("/tasks/" + taskId).then();
    }
}
