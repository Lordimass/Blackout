package com.perl.blackout.world.leaderboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One completed run, ready to send to the leaderboard.
 *
 * <p>The {@link #getHash() hash} is a content hash of the run (player + the
 * recorded timings). The same finished run therefore always produces the same
 * hash, which is what lets the server recognise a resend and skip duplicates,
 * while two genuinely different runs get different hashes.
 */
public final class LeaderboardSubmission {

    private static final long ABSENT = -1L;

    @Nonnull private final String hash;
    @Nonnull private final String playerName;
    private final long totalMs;
    private final long backroomsMs;
    private final long poolsMs;
    private final long garageMs;

    private LeaderboardSubmission(@Nonnull String hash, @Nonnull String playerName,
                                 long totalMs, long backroomsMs, long poolsMs, long garageMs) {
        this.hash = hash;
        this.playerName = playerName;
        this.totalMs = totalMs;
        this.backroomsMs = backroomsMs;
        this.poolsMs = poolsMs;
        this.garageMs = garageMs;
    }

    /**
     * Build a submission from the timer's recorded instants. Returns {@code null}
     * if the data is incomplete or non-positive (nothing worth submitting).
     */
    @Nullable
    public static LeaderboardSubmission fromRun(@Nonnull String playerUuid,
                                                @Nonnull String playerName,
                                                @Nullable Instant startedAt,
                                                @Nullable Instant officeClearedAt,
                                                @Nullable Instant poolClearedAt,
                                                @Nullable Instant garageClearedAt) {
        long backrooms = millisBetween(startedAt, officeClearedAt);
        long pools = millisBetween(officeClearedAt, poolClearedAt);
        long garage = millisBetween(poolClearedAt, garageClearedAt);
        long total = millisBetween(startedAt, garageClearedAt);
        if (total <= 0L) {
            return null;
        }

        String name = playerName.isBlank() ? "Unknown" : playerName.trim();
        long startedEpoch = startedAt != null ? startedAt.toEpochMilli() : 0L;
        String material = String.join("|",
                playerUuid,
                Long.toString(startedEpoch),
                Long.toString(backrooms),
                Long.toString(pools),
                Long.toString(garage),
                Long.toString(total));
        return new LeaderboardSubmission(sha256(material), name, total, backrooms, pools, garage);
    }

    @Nonnull
    public String getHash() {
        return hash;
    }

    private boolean hasSplits() {
        return backroomsMs >= 0L && poolsMs >= 0L && garageMs >= 0L;
    }

    /** JSON body for {@code POST /api/plugin/runs}. */
    @Nonnull
    public String toJsonBody() {
        StringBuilder sb = new StringBuilder(160);
        sb.append('{');
        sb.append("\"player_name\":\"").append(jsonEscape(playerName)).append("\",");
        sb.append("\"total_ms\":").append(totalMs).append(',');
        if (hasSplits()) {
            sb.append("\"backrooms_ms\":").append(backroomsMs).append(',');
            sb.append("\"pools_ms\":").append(poolsMs).append(',');
            sb.append("\"garage_ms\":").append(garageMs).append(',');
        }
        sb.append("\"client_hash\":\"").append(hash).append('"');
        sb.append('}');
        return sb.toString();
    }

    /** One line for the on-disk retry queue. Name is base64 so it can't break the format. */
    @Nonnull
    public String toLine() {
        String encodedName = Base64.getEncoder().encodeToString(playerName.getBytes(StandardCharsets.UTF_8));
        return String.join("\t",
                hash,
                Long.toString(totalMs),
                Long.toString(backroomsMs),
                Long.toString(poolsMs),
                Long.toString(garageMs),
                encodedName);
    }

    /** Parse a line written by {@link #toLine()}. Returns {@code null} on malformed input. */
    @Nullable
    public static LeaderboardSubmission fromLine(@Nonnull String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            String hash = parts[0];
            long total = Long.parseLong(parts[1]);
            long backrooms = Long.parseLong(parts[2]);
            long pools = Long.parseLong(parts[3]);
            long garage = Long.parseLong(parts[4]);
            String name = new String(Base64.getDecoder().decode(parts[5]), StandardCharsets.UTF_8);
            if (hash.isBlank()) {
                return null;
            }
            return new LeaderboardSubmission(hash, name, total, backrooms, pools, garage);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static long millisBetween(@Nullable Instant start, @Nullable Instant end) {
        if (start == null || end == null) {
            return ABSENT;
        }
        return Math.max(0L, Duration.between(start, end).toMillis());
    }

    @Nonnull
    private static String sha256(@Nonnull String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM; this never happens.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Nonnull
    private static String jsonEscape(@Nonnull String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
