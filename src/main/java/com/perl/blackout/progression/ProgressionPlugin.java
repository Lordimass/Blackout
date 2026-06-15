package com.perl.blackout.progression;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class ProgressionPlugin  extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ProgressionPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Blackout Progression %s initializing...", init.getPluginManifest().getVersion());
    }

    @Override
    protected void setup() {
    }
}
