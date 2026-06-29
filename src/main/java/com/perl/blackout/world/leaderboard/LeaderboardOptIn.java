package com.perl.blackout.world.leaderboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Per-player consent for leaderboard submission.
 *
 * <p>Submission is strictly opt-in: a run is only sent for players who have
 * explicitly chosen {@link Status#OPTED_IN}. Choices are persisted to a small
 * properties file in the plugin data directory (keyed by player UUID) so a
 * player is asked only once.
 */
public final class LeaderboardOptIn {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILE_NAME = "leaderboard-optin.properties";

    public enum Status { UNDECIDED, OPTED_IN, OPTED_OUT }

    private final Object lock = new Object();
    @Nonnull private final Path file;
    @Nonnull private final Properties choices = new Properties();

    public LeaderboardOptIn(@Nonnull Path dataDirectory) {
        this.file = dataDirectory.resolve(FILE_NAME);
        load();
    }

    @Nonnull
    public Status status(@Nonnull UUID uuid) {
        synchronized (lock) {
            String value = choices.getProperty(uuid.toString());
            if ("in".equals(value)) {
                return Status.OPTED_IN;
            }
            if ("out".equals(value)) {
                return Status.OPTED_OUT;
            }
            return Status.UNDECIDED;
        }
    }

    public boolean isOptedIn(@Nonnull UUID uuid) {
        return status(uuid) == Status.OPTED_IN;
    }

    public void setOptedIn(@Nonnull UUID uuid) {
        set(uuid, "in");
    }

    public void setOptedOut(@Nonnull UUID uuid) {
        set(uuid, "out");
    }

    private void set(@Nonnull UUID uuid, @Nonnull String value) {
        synchronized (lock) {
            choices.setProperty(uuid.toString(), value);
            save();
        }
    }

    private void load() {
        synchronized (lock) {
            if (!Files.exists(file)) {
                return;
            }
            try (InputStream in = Files.newInputStream(file)) {
                choices.load(in);
            } catch (IOException e) {
                LOGGER.atWarning().log("Could not read %s: %s", FILE_NAME, e.getMessage());
            }
        }
    }

    /** Caller holds {@link #lock}. */
    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                choices.store(out, "Blackout leaderboard opt-in choices (player-uuid = in|out)");
            }
        } catch (IOException e) {
            LOGGER.atWarning().log("Could not write %s: %s", FILE_NAME, e.getMessage());
        }
    }
}
