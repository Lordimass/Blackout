package com.perl.blackout.offensive;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class OffensivePlugin  extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public OffensivePlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Blackout Offensive %s initializing...", init.getPluginManifest().getVersion());
    }

    @Override
    protected void setup() {
    }
}
