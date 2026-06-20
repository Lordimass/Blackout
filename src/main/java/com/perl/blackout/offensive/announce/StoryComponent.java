package com.perl.blackout.offensive.announce;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class StoryComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, StoryComponent> componentType;

    public static void setComponentType(ComponentType<EntityStore, StoryComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, StoryComponent> getComponentType() {
        return componentType;
    }

    private int currentIndex;
    private float elapsedSeconds;

    public int getCurrentIndex() {
        return this.currentIndex;
    }

    public void advance() {
        this.currentIndex++;
    }

    public float getElapsedSeconds() {
        return this.elapsedSeconds;
    }

    public void addElapsed(float dt) {
        this.elapsedSeconds += dt;
    }

    public void reset() {
        this.currentIndex = 0;
        this.elapsedSeconds = 0.0f;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        StoryComponent copy = new StoryComponent();
        copy.currentIndex = this.currentIndex;
        copy.elapsedSeconds = this.elapsedSeconds;
        return copy;
    }
}
