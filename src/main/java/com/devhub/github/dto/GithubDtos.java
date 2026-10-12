package com.devhub.github.dto;

import java.util.List;
import java.util.Map;

/** Response/request shapes of the /github API, trimmed to what the frontend shows. */
public final class GithubDtos {

    private GithubDtos() {}

    public record Status(
            boolean configured,
            boolean connected,
            String login,
            String name,
            String avatarUrl,
            String profileUrl,
            String connectedAt) {}

    public record ConnectUrl(String url) {}

    public record CallbackRequest(String code, String state) {}

    public record Repo(
            long id,
            String name,
            String fullName,
            String owner,
            String description,
            boolean isPrivate,
            boolean fork,
            boolean archived,
            String language,
            int stars,
            int forks,
            int watchers,
            int openIssues,
            String defaultBranch,
            String htmlUrl,
            String homepage,
            List<String> topics,
            String pushedAt,
            String updatedAt,
            String createdAt,
            int sizeKb,
            String license) {}

    public record Commit(
            String sha,
            String shortSha,
            String message,
            String repo,
            String authorLogin,
            String authorName,
            String authorAvatarUrl,
            String date,
            String htmlUrl) {}

    public record Label(String name, String color) {}

    public record Issue(
            int number,
            String title,
            String state,
            String authorLogin,
            String authorAvatarUrl,
            List<Label> labels,
            int comments,
            String createdAt,
            String updatedAt,
            String htmlUrl) {}

    public record PullRequest(
            int number,
            String title,
            String authorLogin,
            String authorAvatarUrl,
            boolean draft,
            String createdAt,
            String htmlUrl) {}

    public record RepoDetail(
            Repo repo,
            /** Bytes of code per language, as GitHub reports them. */
            Map<String, Long> languages,
            List<Commit> commits,
            List<Issue> issues,
            List<PullRequest> pulls) {}

    public record ContributionDay(String date, int count, int level) {}

    public record ContributionWeek(List<ContributionDay> days) {}

    public record Contributions(int total, List<ContributionWeek> weeks) {}
}
