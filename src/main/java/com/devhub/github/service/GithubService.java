package com.devhub.github.service;

import com.devhub.auth.dto.AuthResult;
import com.devhub.auth.service.AuthService;
import com.devhub.github.dto.GithubDtos.*;
import com.devhub.github.entity.GithubConnection;
import com.devhub.github.repository.GithubConnectionRepository;
import com.devhub.user.entity.User;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import com.devhub.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class GithubService {

    private static final String MODE_CONNECT = "connect";
    private static final String MODE_LOGIN = "login";
    private static final Pattern REPO_NAME = Pattern.compile("^[A-Za-z0-9-]{1,39}/[A-Za-z0-9._-]{1,100}$");

    @Inject GithubConfig config;
    @Inject GithubSecrets secrets;
    @Inject GithubApiClient api;
    @Inject GithubConnectionRepository connections;
    @Inject UserProfileRepository profiles;
    @Inject UserRepository users;
    @Inject AuthService authService;

    // ── Connection ────────────────────────────────────────────────────────────

    public Status status(UUID userId) {
        UserProfile profile = profile(userId);
        return connections.findByProfileId(profile.id)
                .map(c -> new Status(config.configured(), true, c.login, c.name, c.avatarUrl,
                        "https://github.com/" + c.login, String.valueOf(c.createdAt)))
                .orElse(new Status(config.configured(), false, null, null, null, null, null));
    }

    public ConnectUrl connectUrl(UUID userId) {
        return new ConnectUrl(authorizeUrl(MODE_CONNECT, profile(userId).id.toString()));
    }

    /** Public: start "Sign in with GitHub". */
    public ConnectUrl loginUrl() {
        return new ConnectUrl(authorizeUrl(MODE_LOGIN, "anonymous"));
    }

    @Transactional
    public Status connect(UUID userId, CallbackRequest request) {
        UserProfile profile = profile(userId);
        secrets.verifyState(request.state(), MODE_CONNECT, profile.id.toString());
        GithubAccount account = exchange(request.code());

        // One GitHub account can back only one DevHub profile.
        connections.find("githubId", account.id()).firstResultOptional()
                .filter(other -> !other.userProfile.id.equals(profile.id))
                .ifPresent(other -> {
                    throw new WebApplicationException(
                            "This GitHub account is already connected to another DevHub account.", Response.Status.CONFLICT);
                });

        GithubConnection connection = connections.findByProfileId(profile.id).orElseGet(GithubConnection::new);
        connection.userProfile = profile;
        apply(connection, account);
        connections.persist(connection);
        connections.flush();
        return status(userId);
    }

    /**
     * "Sign in with GitHub": logs in the DevHub account already linked to this GitHub account, or creates
     * a new one (verified email from GitHub, already connected). Never links to an existing DevHub account
     * by email: whoever controls a GitHub account with that address could otherwise take it over.
     */
    @Transactional
    public AuthResult login(CallbackRequest request) {
        secrets.verifyState(request.state(), MODE_LOGIN, "anonymous");
        GithubAccount account = exchange(request.code());

        Optional<GithubConnection> existing = connections.find("githubId", account.id()).firstResultOptional();
        if (existing.isPresent()) {
            GithubConnection connection = existing.get();
            apply(connection, account); // refresh token + profile data
            connections.persist(connection);
            return authService.issueSession(connection.userProfile.user);
        }

        String email = primaryVerifiedEmail(account.token());
        if (users.findByEmail(email).isPresent()) {
            throw new WebApplicationException(
                    "A DevHub account with " + email + " already exists. Sign in with your password, "
                            + "then connect GitHub from the GitHub page.", Response.Status.CONFLICT);
        }

        User user = User.createNew(email, uniqueUsername(account.login()), UUID.randomUUID().toString());
        // No password the user knows: they can set one later with "Forgot password".
        user.passwordHash = BcryptUtil.bcryptHash(UUID.randomUUID().toString());
        user.isEmailVerified = true;
        users.persist(user);

        String[] name = splitName(account.name(), account.login());
        UserProfile profile = UserProfile.createForUser(user, name[0], name[1],
                Optional.ofNullable(account.avatarUrl()).orElse(""), user.username);
        profiles.persist(profile);

        GithubConnection connection = new GithubConnection();
        connection.userProfile = profile;
        apply(connection, account);
        connections.persist(connection);

        return authService.issueSession(user);
    }

    @Transactional
    public void disconnect(UUID userId) {
        UserProfile profile = profile(userId);
        connections.findByProfileId(profile.id).ifPresent(c -> {
            try {
                String token = secrets.decrypt(c.encryptedToken);
                api.evict(token);
                api.revokeGrant(config.clientId().orElseThrow(), config.clientSecret().orElseThrow(), token);
            } catch (RuntimeException ignored) {
                // Revocation is best effort; the local link is removed regardless.
            }
            connections.delete(c);
        });
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public List<Repo> repos(UUID userId) {
        String token = token(userId);
        List<Repo> out = new ArrayList<>();
        // Up to 300 repos, most recently pushed first; enough for a personal dashboard.
        for (int page = 1; page <= 3; page++) {
            JsonNode batch = api.get(token, "/user/repos?per_page=100&sort=pushed&affiliation=owner,collaborator,organization_member&page=" + page);
            batch.forEach(r -> out.add(toRepo(r)));
            if (batch.size() < 100) break;
        }
        return out;
    }

    public RepoDetail repo(UUID userId, String owner, String name) {
        String full = requireRepoName(owner + "/" + name);
        String token = token(userId);
        Repo repo = toRepo(api.get(token, "/repos/" + full));

        Map<String, Long> languages = new LinkedHashMap<>();
        api.get(token, "/repos/" + full + "/languages").fields()
                .forEachRemaining(e -> languages.put(e.getKey(), e.getValue().asLong()));

        List<Commit> commits = new ArrayList<>();
        api.get(token, "/repos/" + full + "/commits?per_page=15").forEach(c -> commits.add(toCommit(c, full)));

        List<Issue> issues = new ArrayList<>();
        // The issues endpoint also returns PRs; keep real issues only.
        api.get(token, "/repos/" + full + "/issues?state=open&per_page=30").forEach(i -> {
            if (!i.has("pull_request")) issues.add(toIssue(i));
        });

        List<PullRequest> pulls = new ArrayList<>();
        api.get(token, "/repos/" + full + "/pulls?state=open&per_page=15").forEach(p -> pulls.add(new PullRequest(
                p.path("number").asInt(), p.path("title").asText(),
                p.path("user").path("login").asText(null), p.path("user").path("avatar_url").asText(null),
                p.path("draft").asBoolean(false), p.path("created_at").asText(null), p.path("html_url").asText(null))));

        return new RepoDetail(repo, languages, commits, issues.size() > 20 ? issues.subList(0, 20) : issues, pulls);
    }

    /** The user's own latest commits across their most recently pushed repositories. */
    public List<Commit> recentCommits(UUID userId) {
        String token = token(userId);
        String login = connection(userId).login;
        String since = OffsetDateTime.now().minusDays(60).toString();
        List<Commit> all = new ArrayList<>();
        repos(userId).stream()
                .filter(r -> !r.archived() && r.pushedAt() != null)
                .limit(6)
                .forEach(r -> {
                    try {
                        api.get(token, "/repos/" + r.fullName() + "/commits?per_page=10&author="
                                + URLEncoder.encode(login, StandardCharsets.UTF_8) + "&since=" + URLEncoder.encode(since, StandardCharsets.UTF_8))
                                .forEach(c -> all.add(toCommit(c, r.fullName())));
                    } catch (WebApplicationException e) {
                        if (e instanceof GithubReauthException) throw e;
                        // One unreadable repo (e.g. SSO-protected org) shouldn't hide the others.
                    }
                });
        all.sort(Comparator.comparing(Commit::date, Comparator.nullsLast(Comparator.reverseOrder())));
        return all.size() > 30 ? all.subList(0, 30) : all;
    }

    /** The contribution calendar shown on GitHub profiles (last 12 months). */
    public Contributions contributions(UUID userId) {
        String query = """
                query { viewer { contributionsCollection { contributionCalendar {
                  totalContributions
                  weeks { contributionDays { date contributionCount contributionLevel } }
                } } } }""";
        JsonNode calendar = api.graphql(token(userId), query, Map.of())
                .path("viewer").path("contributionsCollection").path("contributionCalendar");
        List<ContributionWeek> weeks = new ArrayList<>();
        calendar.path("weeks").forEach(w -> {
            List<ContributionDay> days = new ArrayList<>();
            w.path("contributionDays").forEach(d -> days.add(new ContributionDay(
                    d.path("date").asText(), d.path("contributionCount").asInt(), level(d.path("contributionLevel").asText()))));
            weeks.add(new ContributionWeek(days));
        });
        return new Contributions(calendar.path("totalContributions").asInt(), weeks);
    }

    /** Validates "owner/name" before it's stored on a project or put into a GitHub URL. */
    public static String requireRepoName(String fullName) {
        if (fullName == null || !REPO_NAME.matcher(fullName).matches()) {
            throw new BadRequestException("Repository must look like owner/name.");
        }
        return fullName;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private record GithubAccount(long id, String login, String name, String avatarUrl, String token, String scopes) {}

    private String authorizeUrl(String mode, String subject) {
        if (!config.configured()) {
            throw new WebApplicationException("GitHub integration isn't configured on this server.", Response.Status.SERVICE_UNAVAILABLE);
        }
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + enc(config.clientId().orElseThrow())
                + "&redirect_uri=" + enc(config.redirectUri())
                + "&scope=" + enc(config.scopes())
                + "&state=" + enc(secrets.signState(mode, subject))
                + "&allow_signup=true";
    }

    private GithubAccount exchange(String code) {
        if (!config.configured()) {
            throw new WebApplicationException("GitHub integration isn't configured on this server.", Response.Status.SERVICE_UNAVAILABLE);
        }
        if (code == null || code.isBlank()) throw new BadRequestException("Missing GitHub authorization code.");
        JsonNode tokenResponse = api.exchangeCode(config.clientId().orElseThrow(), config.clientSecret().orElseThrow(),
                code, config.redirectUri());
        String token = tokenResponse.path("access_token").asText(null);
        if (token == null) {
            String reason = tokenResponse.path("error_description").asText("GitHub didn't return an access token.");
            throw new BadRequestException(reason);
        }
        JsonNode me = api.get(token, "/user");
        return new GithubAccount(me.path("id").asLong(), me.path("login").asText(), me.path("name").asText(null),
                me.path("avatar_url").asText(null), token, tokenResponse.path("scope").asText(""));
    }

    private void apply(GithubConnection c, GithubAccount a) {
        c.githubId = a.id();
        c.login = a.login();
        c.name = a.name();
        c.avatarUrl = a.avatarUrl();
        c.scopes = a.scopes();
        c.encryptedToken = secrets.encrypt(a.token());
    }

    private String primaryVerifiedEmail(String token) {
        JsonNode emails = api.get(token, "/user/emails");
        String fallback = null;
        for (JsonNode e : emails) {
            if (!e.path("verified").asBoolean(false)) continue;
            if (e.path("primary").asBoolean(false)) return e.path("email").asText();
            if (fallback == null) fallback = e.path("email").asText();
        }
        if (fallback != null) return fallback;
        throw new BadRequestException("Your GitHub account has no verified email address. Verify one on GitHub, or sign up with email.");
    }

    /** GitHub login → DevHub username (3–20 chars, unique). */
    private String uniqueUsername(String login) {
        String base = login.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (base.length() < 3) base = (base + "dev").substring(0, Math.max(3, base.length()));
        if (base.length() > 17) base = base.substring(0, 17);
        String candidate = base;
        for (int i = 2; users.find("username", candidate).firstResultOptional().isPresent()
                || profiles.find("username", candidate).firstResultOptional().isPresent(); i++) {
            candidate = base + i;
        }
        return candidate;
    }

    private static String[] splitName(String name, String login) {
        if (name == null || name.isBlank()) return new String[]{login, "-"};
        String[] parts = name.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : "-"};
    }

    private UserProfile profile(UUID userId) {
        UserProfile profile = profiles.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");
        return profile;
    }

    private GithubConnection connection(UUID userId) {
        return connections.findByProfileId(profile(userId).id)
                .orElseThrow(() -> new WebApplicationException("Connect your GitHub account first.", Response.Status.CONFLICT));
    }

    private String token(UUID userId) {
        return secrets.decrypt(connection(userId).encryptedToken);
    }

    private static Repo toRepo(JsonNode r) {
        List<String> topics = new ArrayList<>();
        r.path("topics").forEach(t -> topics.add(t.asText()));
        return new Repo(
                r.path("id").asLong(), r.path("name").asText(), r.path("full_name").asText(),
                r.path("owner").path("login").asText(), r.path("description").asText(null),
                r.path("private").asBoolean(), r.path("fork").asBoolean(), r.path("archived").asBoolean(),
                r.path("language").asText(null), r.path("stargazers_count").asInt(), r.path("forks_count").asInt(),
                r.path("subscribers_count").asInt(r.path("watchers_count").asInt()), r.path("open_issues_count").asInt(),
                r.path("default_branch").asText(null), r.path("html_url").asText(null),
                blankToNull(r.path("homepage").asText(null)), topics,
                r.path("pushed_at").asText(null), r.path("updated_at").asText(null), r.path("created_at").asText(null),
                r.path("size").asInt(), r.path("license").path("spdx_id").asText(null));
    }

    private static Commit toCommit(JsonNode c, String repo) {
        String sha = c.path("sha").asText();
        String message = c.path("commit").path("message").asText("");
        int nl = message.indexOf('\n');
        return new Commit(sha, sha.length() > 7 ? sha.substring(0, 7) : sha, nl > 0 ? message.substring(0, nl) : message, repo,
                c.path("author").path("login").asText(null),
                c.path("commit").path("author").path("name").asText(null),
                c.path("author").path("avatar_url").asText(null),
                c.path("commit").path("author").path("date").asText(null),
                c.path("html_url").asText(null));
    }

    private static Issue toIssue(JsonNode i) {
        List<Label> labels = new ArrayList<>();
        i.path("labels").forEach(l -> labels.add(new Label(l.path("name").asText(), l.path("color").asText(null))));
        return new Issue(i.path("number").asInt(), i.path("title").asText(), i.path("state").asText(),
                i.path("user").path("login").asText(null), i.path("user").path("avatar_url").asText(null), labels,
                i.path("comments").asInt(), i.path("created_at").asText(null), i.path("updated_at").asText(null),
                i.path("html_url").asText(null));
    }

    private static int level(String l) {
        return switch (l) {
            case "FIRST_QUARTILE" -> 1;
            case "SECOND_QUARTILE" -> 2;
            case "THIRD_QUARTILE" -> 3;
            case "FOURTH_QUARTILE" -> 4;
            default -> 0;
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
