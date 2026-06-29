package com.perl.blackout.world.leaderboard;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Sends finished runs to the leaderboard, durably.
 *
 * <p>Flow for every run:
 * <ol>
 *   <li>The run is queued (in memory and persisted to a text file) so it
 *       survives a crash/restart.</li>
 *   <li>Before sending we ask the server whether that hash already exists and
 *       is approved — if so we drop it (no resend).</li>
 *   <li>Otherwise we POST it. On success it leaves the queue; the server itself
 *       de-duplicates by hash, so a resend can never create a duplicate.</li>
 *   <li>If the server is unreachable we start a heartbeat that polls
 *       {@code /api/health} and re-flushes the whole queue once it returns.</li>
 *   <li>Any other failure simply leaves the run in the file to be retried the
 *       next time anything triggers a flush.</li>
 * </ol>
 *
 * <p>All network and file work happens on a single daemon thread, so it never
 * touches or blocks the game thread.
 */
public final class LeaderboardClient {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String QUEUE_FILE_NAME = "pending-submissions.txt";

    private final boolean enabled;
    @Nonnull private final String baseUrl;
    @Nonnull private final String displayUrl;
    @Nonnull private final String pluginHash;
    private final long heartbeatSeconds;
    @Nonnull private final Path queueFile;

    @Nonnull private final HttpClient http;
    @Nonnull private final ScheduledExecutorService executor;

    private final Object lock = new Object();                 // guards pending + heartbeat + file
    private final List<LeaderboardSubmission> pending = new ArrayList<>();
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    @Nullable private ScheduledFuture<?> heartbeat;           // non-null while polling an offline server

    public LeaderboardClient(@Nonnull LeaderboardConfig config, @Nonnull Path dataDirectory) {
        this.enabled = config.isEnabled();
        this.baseUrl = config.getUrl();
        this.displayUrl = config.getDisplayUrl();
        this.pluginHash = config.getPluginHash();
        this.heartbeatSeconds = config.getHeartbeatSeconds();
        this.queueFile = dataDirectory.resolve(QUEUE_FILE_NAME);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "blackout-leaderboard");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Load any runs left over from a previous session and try to flush them. */
    public void start() {
        if (!enabled) {
            LOGGER.atInfo().log("Blackout leaderboard submission is disabled (no leaderboard url configured).");
            return;
        }
        LOGGER.atInfo().log("Blackout leaderboard submission enabled -> %s", baseUrl);
        runOnExecutor(() -> {
            loadQueue();
            flush();
        });
    }

    /** Queue a finished run and kick off a flush. Safe to call from the game thread. */
    public void submit(@Nullable LeaderboardSubmission submission) {
        if (!enabled || submission == null) {
            return;
        }
        synchronized (lock) {
            for (LeaderboardSubmission existing : pending) {
                if (existing.getHash().equals(submission.getHash())) {
                    scheduleFlush();   // already queued; just make sure it gets sent
                    return;
                }
            }
            pending.add(submission);
            saveQueue();
        }
        LOGGER.atInfo().log("Queued run %s for leaderboard submission.", submission.getHash());
        scheduleFlush();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    /** Public, browser-facing leaderboard URL for the in-chat link shown to players. */
    @Nonnull
    public String getDisplayUrl() {
        return displayUrl;
    }

    // ----------------------------------------------------------------- internals

    private void scheduleFlush() {
        runOnExecutor(this::flush);
    }

    private void runOnExecutor(@Nonnull Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // Executor is shutting down; nothing to do.
        }
    }

    private void flush() {
        if (!flushing.compareAndSet(false, true)) {
            return;   // a flush is already in progress on this thread's queue
        }
        try {
            List<LeaderboardSubmission> snapshot;
            synchronized (lock) {
                snapshot = new ArrayList<>(pending);
            }
            for (LeaderboardSubmission submission : snapshot) {
                switch (process(submission)) {
                    case DONE -> remove(submission);
                    case SERVER_DOWN -> {
                        startHeartbeat();
                        return;   // stop; the heartbeat re-flushes once the server returns
                    }
                    case RETRY_LATER -> {
                        // leave it queued; a future submit / heartbeat will retry
                    }
                }
            }
        } finally {
            flushing.set(false);
        }
    }

    private enum Result { DONE, SERVER_DOWN, RETRY_LATER }

    @Nonnull
    private Result process(@Nonnull LeaderboardSubmission submission) {
        // 1) Already on the server and approved? Then never resend.
        try {
            HttpResponse<String> check = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/api/plugin/runs/" + submission.getHash()))
                            .timeout(Duration.ofSeconds(10))
                            .header("X-Plugin-Hash", pluginHash)
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            int status = check.statusCode();
            if (status == 200 && isApproved(check.body())) {
                LOGGER.atInfo().log("Run %s already approved on the server; not resending.", submission.getHash());
                return Result.DONE;
            }
            if (status == 503) {
                LOGGER.atWarning().log("Leaderboard endpoint disabled (503); will retry later.");
                return Result.RETRY_LATER;
            }
            if (status == 401) {
                LOGGER.atWarning().log("Leaderboard rejected the API key (401); will retry later.");
                return Result.RETRY_LATER;
            }
            // 200-but-not-approved or 404 -> fall through and (re)submit.
        } catch (IOException e) {
            LOGGER.atWarning().log("Leaderboard unreachable while checking %s: %s", submission.getHash(), e.getMessage());
            return Result.SERVER_DOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.RETRY_LATER;
        }

        // 2) Submit. The server de-duplicates on the hash, so this is safe to repeat.
        try {
            HttpResponse<String> post = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/api/plugin/runs"))
                            .timeout(Duration.ofSeconds(15))
                            .header("Content-Type", "application/json")
                            .header("X-Plugin-Hash", pluginHash)
                            .POST(HttpRequest.BodyPublishers.ofString(submission.toJsonBody(), StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            int status = post.statusCode();
            if (status == 200 || status == 201) {
                LOGGER.atInfo().log("Submitted run %s to leaderboard (HTTP %d).", submission.getHash(), status);
                return Result.DONE;
            }
            if (status >= 500) {
                LOGGER.atWarning().log("Leaderboard server error (HTTP %d) on %s; will retry.", status, submission.getHash());
                return Result.SERVER_DOWN;
            }
            LOGGER.atWarning().log("Leaderboard rejected run %s (HTTP %d): %s",
                    submission.getHash(), status, post.body());
            return Result.RETRY_LATER;
        } catch (IOException e) {
            LOGGER.atWarning().log("Leaderboard unreachable while sending %s: %s", submission.getHash(), e.getMessage());
            return Result.SERVER_DOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.RETRY_LATER;
        }
    }

    private void startHeartbeat() {
        synchronized (lock) {
            if (heartbeat != null) {
                return;   // already polling
            }
            LOGGER.atInfo().log("Leaderboard offline; polling every %ds until it returns.", heartbeatSeconds);
            heartbeat = executor.scheduleWithFixedDelay(
                    this::heartbeatTick, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
        }
    }

    private void stopHeartbeat() {
        synchronized (lock) {
            if (heartbeat != null) {
                heartbeat.cancel(false);
                heartbeat = null;
            }
        }
    }

    private void heartbeatTick() {
        if (!ping()) {
            return;
        }
        LOGGER.atInfo().log("Leaderboard is back online; flushing queued runs.");
        stopHeartbeat();
        flush();
    }

    private boolean ping() {
        try {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/api/health"))
                            .timeout(Duration.ofSeconds(10))
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void remove(@Nonnull LeaderboardSubmission submission) {
        synchronized (lock) {
            pending.removeIf(s -> s.getHash().equals(submission.getHash()));
            saveQueue();
        }
    }

    /** Persist the queue. Caller must hold {@link #lock}. */
    private void saveQueue() {
        try {
            Files.createDirectories(queueFile.getParent());
            List<String> lines = new ArrayList<>(pending.size());
            for (LeaderboardSubmission submission : pending) {
                lines.add(submission.toLine());
            }
            Files.write(queueFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to persist leaderboard queue: %s", e.getMessage());
        }
    }

    /** Load the queue from disk. Runs on the executor thread before the first flush. */
    private void loadQueue() {
        synchronized (lock) {
            pending.clear();
            if (!Files.exists(queueFile)) {
                return;
            }
            try {
                int skipped = 0;
                for (String line : Files.readAllLines(queueFile, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    LeaderboardSubmission submission = LeaderboardSubmission.fromLine(line);
                    if (submission != null) {
                        pending.add(submission);
                    } else {
                        skipped++;
                    }
                }
                if (!pending.isEmpty() || skipped > 0) {
                    LOGGER.atInfo().log("Loaded %d queued leaderboard run(s) from disk (%d unreadable).",
                            pending.size(), skipped);
                }
            } catch (IOException e) {
                LOGGER.atWarning().log("Failed to read leaderboard queue: %s", e.getMessage());
            }
        }
    }

    private static boolean isApproved(@Nullable String body) {
        if (body == null) {
            return false;
        }
        return body.replace(" ", "").contains("\"status\":\"approved\"");
    }
}
