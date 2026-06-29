package com.perl.blackout.world.commands;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.perl.blackout.world.leaderboard.LeaderboardMessages;
import com.perl.blackout.world.leaderboard.LeaderboardOptIn;

/** {@code /leaderboard optin} and {@code /leaderboard optout} — records a player's
 * consent choice for leaderboard submission. Usable by any player. */
public final class LeaderboardOptCommand extends AbstractPlayerCommand {

    private final boolean optInValue;
    @Nonnull private final LeaderboardOptIn optIn;

    @SuppressWarnings("deprecation")
    public LeaderboardOptCommand(@Nonnull String name, boolean optInValue, @Nonnull LeaderboardOptIn optIn) {
        super(name, optInValue
                ? "Submit your Blackout run times to the public leaderboard"
                : "Stop submitting your Blackout run times");
        setPermissionGroup(GameMode.Adventure); // available to everyone
        this.optInValue = optInValue;
        this.optIn = optIn;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> es,
                           @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef player,
                           @Nonnull World world) {
        if (optInValue) {
            optIn.setOptedIn(player.getUuid());
            ctx.sendMessage(Message.join(
                    LeaderboardMessages.prefix(),
                    Message.raw(" You're in — your future runs will be submitted to the leaderboard. ")
                            .color("#A7F3D0"),
                    Message.raw("Use /leaderboard optout to stop anytime.").color("#9CA3AF")
            ));
        } else {
            optIn.setOptedOut(player.getUuid());
            ctx.sendMessage(Message.join(
                    LeaderboardMessages.prefix(),
                    Message.raw(" You've opted out — your runs will not be submitted. ").color("#F87171"),
                    Message.raw("Use /leaderboard optin to join later.").color("#9CA3AF")
            ));
        }
    }
}
