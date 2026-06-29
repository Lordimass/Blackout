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

/** {@code /leaderboard} — shows the player their current opt-in status and how to
 * change it. {@code /leaderboard optin|optout} record the choice. Usable by any
 * player. */
public final class LeaderboardCommand extends AbstractPlayerCommand {

    @Nonnull private final LeaderboardOptIn optIn;
    @Nonnull private final String displayUrl;

    @SuppressWarnings("deprecation")
    public LeaderboardCommand(@Nonnull LeaderboardOptIn optIn, @Nonnull String displayUrl) {
        super("leaderboard", "View and change your Blackout leaderboard opt-in");
        setPermissionGroup(GameMode.Adventure); // available to everyone
        this.optIn = optIn;
        this.displayUrl = displayUrl;
        addSubCommand(new LeaderboardOptCommand("optin", true, optIn));
        addSubCommand(new LeaderboardOptCommand("optout", false, optIn));
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> es,
                           @Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef player,
                           @Nonnull World world) {
        String state = switch (optIn.status(player.getUuid())) {
            case OPTED_IN -> "opted IN";
            case OPTED_OUT -> "opted OUT";
            case UNDECIDED -> "not decided yet";
        };
        ctx.sendMessage(Message.join(
                LeaderboardMessages.prefix(),
                Message.raw(" You are currently ").color("#C9D1D9"),
                Message.raw(state).color("#F2D16B").bold(true),
                Message.raw(".\nUse ").color("#C9D1D9"),
                Message.raw("/leaderboard optin").color("#A7F3D0").bold(true),
                Message.raw(" or ").color("#C9D1D9"),
                Message.raw("/leaderboard optout").color("#F87171").bold(true),
                Message.raw(" to change it.").color("#C9D1D9")
        ));
        if (!displayUrl.isEmpty()) {
            ctx.sendMessage(Message.raw("Click here to view the Leaderboard")
                    .color("#7DD3FC").bold(true).link(displayUrl));
        }
    }
}
