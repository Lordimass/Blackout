package com.perl.blackout.defensive;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class DefensivePlugin  extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DefensivePlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Blackout Defensive %s initializing...", init.getPluginManifest().getVersion());
    }

    @Override
    protected void setup() {
    }
}
