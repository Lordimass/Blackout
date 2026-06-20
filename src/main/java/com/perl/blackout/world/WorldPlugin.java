package com.perl.blackout.world;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.perl.blackout.world.commands.BlackoutCommand;
import com.perl.blackout.world.components.BunkerHatchComponent;
import com.perl.blackout.world.components.CycleStateComponent;
import com.perl.blackout.world.craft.CraftAltarBreakHandler;
import com.perl.blackout.world.craft.CraftAltarBreathingHandler;
import com.perl.blackout.world.craft.CraftAltarPlacementHandler;
import com.perl.blackout.world.resources.WorldCycleStateResource;
import com.perl.blackout.world.systems.CycleStateRefSystem;

public class WorldPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public WorldPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Blackout World %s initializing...", init.getPluginManifest().getVersion());
    }

    @Override
    protected void setup() {
        getEntityStoreRegistry().registerSystem(new CraftAltarPlacementHandler());
        getEntityStoreRegistry().registerSystem(new CraftAltarBreakHandler());
        getEntityStoreRegistry().registerSystem(new CraftAltarBreathingHandler());

        LOGGER.atInfo().log("Craft altar system ready.");

        ComponentType<ChunkStore, CycleStateComponent> cycleComponentType =
                getChunkStoreRegistry().registerComponent(CycleStateComponent.class, "BlackoutCycleState", CycleStateComponent.CODEC);
        CycleStateComponent.setComponentType(cycleComponentType);

        ComponentType<ChunkStore, BunkerHatchComponent> bunkerHatchComponentType =
                getChunkStoreRegistry().registerComponent(BunkerHatchComponent.class, "BlackoutBunkerHatch", BunkerHatchComponent.CODEC);
        BunkerHatchComponent.setComponentType(bunkerHatchComponentType);

        ResourceType<EntityStore, WorldCycleStateResource> cycleResourceType =
                getEntityStoreRegistry().registerResource(WorldCycleStateResource.class, "BlackoutWorldCycleState", WorldCycleStateResource.CODEC);
        WorldCycleStateResource.setResourceType(cycleResourceType);

        getChunkStoreRegistry().registerSystem(new CycleStateRefSystem());

        getCommandRegistry().registerCommand(new BlackoutCommand());

        LOGGER.atInfo().log("World cycle state system ready.");
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }
}
