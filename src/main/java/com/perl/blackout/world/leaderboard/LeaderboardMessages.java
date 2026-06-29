package com.perl.blackout.world.leaderboard;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.Message;

/** Shared chat messages for leaderboard opt-in, so the join prompt and the
 * {@code /leaderboard} command stay consistent. */
public final class LeaderboardMessages {

    private LeaderboardMessages() {
    }

    @Nonnull
    public static Message prefix() {
        return Message.raw("Blackout Leaderboard").color("#F2D16B").bold(true);
    }

    /** The opt-in consent prompt shown to a player who hasn't decided yet. */
    @Nonnull
    public static Message consentPrompt(@Nonnull String displayUrl) {
        Message body = Message.join(
                prefix(),
                Message.raw("\nSubmit your run times to the public leaderboard? ").color("#C9D1D9"),
                Message.raw("Only your in-game name and times are sent.").color("#9CA3AF"),
                Message.raw("\nType ").color("#C9D1D9"),
                Message.raw("/leaderboard optin").color("#A7F3D0").bold(true),
                Message.raw(" to participate, or ").color("#C9D1D9"),
                Message.raw("/leaderboard optout").color("#F87171").bold(true),
                Message.raw(" to decline.").color("#C9D1D9")
        );
        if (!displayUrl.isEmpty()) {
            body = Message.join(
                    body,
                    Message.raw("\n").color("#C9D1D9"),
                    Message.raw("Click here to view the Leaderboard").color("#7DD3FC").bold(true).link(displayUrl)
            );
        }
        return body;
    }
}
