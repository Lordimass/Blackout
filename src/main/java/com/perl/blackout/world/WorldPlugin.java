package com.perl.blackout.world;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class WorldPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public WorldPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Blackout World %s initializing...", init.getPluginManifest().getVersion());
    }

    @Override
    protected void setup() {
    }
}
