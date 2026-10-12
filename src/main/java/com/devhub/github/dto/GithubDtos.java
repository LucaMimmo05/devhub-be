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
            String branch,
            List<Label> labels,
            String createdAt,
            String updatedAt,
            String htmlUrl) {}

    public record Release(String tag, String name, String publishedAt, String htmlUrl) {}

    /** A GitHub Actions run. status: queued|in_progress|completed; conclusion: success|failure|cancelled|skipped… */
    public record WorkflowRun(
            long id,
            String name,
            String title,
            String status,
            String conclusion,
            String branch,
            String event,
            String createdAt,
            String htmlUrl) {}

    public record Contributor(String login, String avatarUrl, int contributions) {}

    public record RepoDetail(
            Repo repo,
            /** Bytes of code per language, as GitHub reports them. */
            Map<String, Long> languages,
            List<Commit> commits,
            List<Issue> issues,
            List<PullRequest> pulls,
            /** README as Markdown, null when the repository has none. */
            String readme,
            Release latestRelease,
            List<WorkflowRun> runs,
            List<Contributor> contributors,
            /** Commits per week over the last year, oldest first; empty while GitHub is still computing it. */
            List<Integer> weeklyCommits) {}

    /** An open pull request or issue from the viewer's inbox. */
    public record InboxItem(
            /** "PR" or "ISSUE". */
            String kind,
            int number,
            String title,
            String repo,
            String htmlUrl,
            String authorLogin,
            String authorAvatarUrl,
            List<Label> labels,
            int comments,
            boolean draft,
            /** PRs: APPROVED | CHANGES_REQUESTED | REVIEW_REQUIRED | null. */
            String reviewDecision,
            /** PRs: SUCCESS | FAILURE | PENDING | null (no checks). */
            String checks,
            int additions,
            int deletions,
            String createdAt,
            String updatedAt) {}

    public record Inbox(List<InboxItem> reviewRequests, List<InboxItem> pullRequests, List<InboxItem> issues) {}

    public record ContributionDay(String date, int count, int level) {}

    public record ContributionWeek(List<ContributionDay> days) {}

    public record ContributionTotals(int commits, int pullRequests, int reviews, int issues, int privateContributions) {}

    public record RepoContribution(String repo, int commits) {}

    public record Contributions(
            int total,
            List<ContributionWeek> weeks,
            ContributionTotals totals,
            /** Repositories with the most commits by the viewer this year. */
            List<RepoContribution> topRepos) {}
}
