package com.perl.blackout.world.components;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class BunkerHatchComponent implements Component<ChunkStore> {

    @Nonnull
    public static final BuilderCodec<BunkerHatchComponent> CODEC =
            BuilderCodec.builder(BunkerHatchComponent.class, BunkerHatchComponent::new).build();

    private static ComponentType<ChunkStore, BunkerHatchComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, BunkerHatchComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, BunkerHatchComponent> getComponentType() {
        return componentType;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        return new BunkerHatchComponent();
    }
}
