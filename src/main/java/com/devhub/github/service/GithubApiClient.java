package com.devhub.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin GitHub REST/GraphQL client. Responses are cached for a minute per token+URL so
 * browsing between pages doesn't burn the 5000 req/h rate limit.
 */
@ApplicationScoped
public class GithubApiClient {

    private static final Logger LOG = Logger.getLogger(GithubApiClient.class);
    private static final String API = "https://api.github.com";
    private static final long CACHE_TTL_MS = 60_000;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(long at, JsonNode body) {}

    @Inject
    ObjectMapper mapper;

    public JsonNode get(String token, String path) {
        String key = fingerprint(token) + " GET " + path;
        Cached hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < CACHE_TTL_MS) return hit.body();

        HttpRequest request = base(token, URI.create(API + path)).GET().build();
        JsonNode body = send(request);
        cache.put(key, new Cached(System.currentTimeMillis(), body));
        if (cache.size() > 2_000) cache.clear(); // crude bound; entries are cheap to refetch
        return body;
    }

    public JsonNode graphql(String token, String query, Map<String, Object> variables) {
        try {
            String payload = mapper.writeValueAsString(Map.of("query", query, "variables", variables));
            String key = fingerprint(token) + " GQL " + payload;
            Cached hit = cache.get(key);
            if (hit != null && System.currentTimeMillis() - hit.at() < CACHE_TTL_MS) return hit.body();

            HttpRequest request = base(token, URI.create(API + "/graphql"))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .header("Content-Type", "application/json")
                    .build();
            JsonNode body = send(request);
            if (body.hasNonNull("errors")) {
                LOG.warnf("GitHub GraphQL error: %s", body.get("errors"));
                throw new WebApplicationException("GitHub returned an error for this request.", Response.Status.BAD_GATEWAY);
            }
            cache.put(key, new Cached(System.currentTimeMillis(), body.get("data")));
            return body.get("data");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Exchanges an OAuth code for an access token. Returns the token response (access_token, scope, error…). */
    public JsonNode exchangeCode(String clientId, String clientSecret, String code, String redirectUri) {
        String form = "client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret)
                + "&code=" + enc(code) + "&redirect_uri=" + enc(redirectUri);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return send(request);
    }

    /** Best-effort revocation of the user's grant when they disconnect. */
    public void revokeGrant(String clientId, String clientSecret, String token) {
        try {
            String basic = java.util.Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(API + "/applications/" + clientId + "/grant"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Basic " + basic)
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("access_token", token))))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            LOG.warn("Could not revoke GitHub grant", e);
        }
    }

    public void evict(String token) {
        String prefix = fingerprint(token) + " ";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private HttpRequest.Builder base(String token, URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "DevHub")
                .header("Authorization", "Bearer " + token);
    }

    private JsonNode send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new WebApplicationException("Couldn't reach GitHub. Try again in a moment.", Response.Status.BAD_GATEWAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebApplicationException("Request to GitHub was interrupted.", Response.Status.BAD_GATEWAY);
        }

        int status = response.statusCode();
        if (status == 401) throw new GithubReauthException();
        if (status == 404) throw new NotFoundException("Not found on GitHub, or your account can't access it.");
        if (status == 403 || status == 429) {
            boolean rateLimited = status == 429
                    || "0".equals(response.headers().firstValue("x-ratelimit-remaining").orElse(""));
            throw rateLimited
                    ? new WebApplicationException("GitHub rate limit reached. Try again in a few minutes.", 429)
                    : new WebApplicationException("GitHub denied access to this resource.", 403);
        }
        if (status == 409 && request.uri().getPath().endsWith("/commits")) {
            // Empty repository: GitHub answers 409 to the commits endpoint.
            return mapper.createArrayNode();
        }
        if (status >= 400) {
            LOG.warnf("GitHub %s %s -> %d: %s", request.method(), request.uri(), status, response.body());
            throw new WebApplicationException("GitHub returned an error (" + status + ").", Response.Status.BAD_GATEWAY);
        }
        try {
            String body = response.body();
            return body == null || body.isBlank() ? mapper.createObjectNode() : mapper.readTree(body);
        } catch (java.io.IOException e) {
            throw new WebApplicationException("Unexpected response from GitHub.", Response.Status.BAD_GATEWAY);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String fingerprint(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
