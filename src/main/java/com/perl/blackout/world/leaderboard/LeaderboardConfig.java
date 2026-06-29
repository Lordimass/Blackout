package com.perl.blackout.world.leaderboard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Runtime configuration for leaderboard submission.
 *
 * <p>Trust model: the plugin authenticates with a build identity hash compiled
 * into this jar ({@link #PLUGIN_HASH}). The leaderboard server holds the
 * authoritative copy and rejects any submission whose hash doesn't match, so
 * foreign or edited plugins can't push faulty runs. The mod can run on any
 * player's machine (including singleplayer), so submissions go to the public
 * leaderboard URL over HTTPS and the hash is the sole client gate.
 *
 * <p>The only thing the server owner can tweak is {@code url}/{@code
 * heartbeatSeconds} via {@code leaderboard.properties} (or the env vars
 * {@code BLACKOUT_LEADERBOARD_URL} / {@code BLACKOUT_LEADERBOARD_HEARTBEAT}).
 * There is no user-managed secret.
 */
public final class LeaderboardConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String FILE_NAME = "leaderboard.properties";

    // Public leaderboard URL the plugin submits to (the mod runs on players'
    // machines, so this must be reachable from anywhere). Auth is the baked hash.
    private static final String DEFAULT_URL = "https://ev0.tail8bf84a.ts.net";
    // Browser-facing leaderboard URL for the in-chat link a player clicks. Same
    // host today, but kept separate so submit/display can diverge later.
    private static final String DEFAULT_DISPLAY_URL = "https://ev0.tail8bf84a.ts.net";
    private static final long DEFAULT_HEARTBEAT_SECONDS = 30L;

    // Build identity hash of the official plugin, validated server-side. Baked in
    // (compiled), not exposed in any config file. Must match PLUGIN_TRUST_HASH on
    // the leaderboard server.
    private static final String PLUGIN_HASH =
            "0477e6835cb443b34f4b4e076f98f33674b8bf08b40916dc870b10072b689b3f";

    @Nonnull private final String url;
    @Nonnull private final String displayUrl;
    @Nonnull private final String pluginHash;
    private final long heartbeatSeconds;

    private LeaderboardConfig(@Nonnull String url, @Nonnull String displayUrl,
                             @Nonnull String pluginHash, long heartbeatSeconds) {
        this.url = url;
        this.displayUrl = displayUrl;
        this.pluginHash = pluginHash;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    @Nonnull
    public static LeaderboardConfig load(@Nonnull Path dataDirectory) {
        Properties props = new Properties();
        Path file = dataDirectory.resolve(FILE_NAME);
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                LOGGER.atWarning().log("Could not read %s: %s", FILE_NAME, e.getMessage());
            }
        } else {
            writeTemplate(file);
        }

        String url = firstNonBlank(
                System.getenv("BLACKOUT_LEADERBOARD_URL"),
                props.getProperty("url"),
                DEFAULT_URL).trim();
        String displayUrl = firstNonBlank(
                System.getenv("BLACKOUT_LEADERBOARD_DISPLAY_URL"),
                props.getProperty("displayUrl"),
                DEFAULT_DISPLAY_URL).trim();
        long heartbeat = parseLong(firstNonBlank(
                System.getenv("BLACKOUT_LEADERBOARD_HEARTBEAT"),
                props.getProperty("heartbeatSeconds"),
                Long.toString(DEFAULT_HEARTBEAT_SECONDS)));

        return new LeaderboardConfig(
                stripTrailingSlash(url), stripTrailingSlash(displayUrl), PLUGIN_HASH, heartbeat);
    }

    /** Always enabled: the identity hash is baked in, so there is nothing to configure. */
    public boolean isEnabled() {
        return !url.isEmpty() && !pluginHash.isEmpty();
    }

    @Nonnull
    public String getUrl() {
        return url;
    }

    @Nonnull
    public String getDisplayUrl() {
        return displayUrl;
    }

    @Nonnull
    public String getPluginHash() {
        return pluginHash;
    }

    public long getHeartbeatSeconds() {
        return heartbeatSeconds;
    }

    private static void writeTemplate(@Nonnull Path file) {
        String template = """
                # Blackout leaderboard submission.
                # Runs are auto-submitted to the leaderboard when a player finishes.
                # Authentication uses a build identity hash compiled into the plugin
                # (validated server-side) — there is no secret to configure here.
                #
                # url:              base URL the plugin SUBMITS to (the public leaderboard).
                # displayUrl:       public, browser-facing leaderboard URL shown as a clickable
                #                   chat link to players after a run.
                # heartbeatSeconds: when the server is offline, how often to re-check it.
                #
                # Any value may be overridden by an environment variable, which wins:
                #   BLACKOUT_LEADERBOARD_URL, BLACKOUT_LEADERBOARD_DISPLAY_URL, BLACKOUT_LEADERBOARD_HEARTBEAT
                url=%s
                displayUrl=%s
                heartbeatSeconds=%d
                """.formatted(DEFAULT_URL, DEFAULT_DISPLAY_URL, DEFAULT_HEARTBEAT_SECONDS);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, template, StandardCharsets.UTF_8);
            LOGGER.atInfo().log("Wrote leaderboard config template to %s.", file);
        } catch (IOException e) {
            LOGGER.atWarning().log("Could not write %s template: %s", FILE_NAME, e.getMessage());
        }
    }

    @Nonnull
    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static long parseLong(String value) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : DEFAULT_HEARTBEAT_SECONDS;
        } catch (NumberFormatException e) {
            return DEFAULT_HEARTBEAT_SECONDS;
        }
    }

    @Nonnull
    private static String stripTrailingSlash(@Nonnull String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
