package com.perl.blackout.world.craft;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreathingCheckEvent;
import com.hypixel.hytale.server.core.modules.entity.component.BreathingComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.perl.blackout.offensive.wave.WaveGameManager;

/**
 * The crafting machine target NPC intentionally lives inside a solid bench block. Keep the normal
 * bench health/damage behavior, but never let the breathing system treat that overlap as suffocation.
 */
public final class CraftAltarBreathingHandler extends EntityEventSystem<EntityStore, BreathingCheckEvent> {

    public CraftAltarBreathingHandler() {
        super(BreathingCheckEvent.class);
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreathingCheckEvent event) {
        NPCEntity npc = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null || !WaveGameManager.BENCH_NPC_ROLE.equals(npc.getRoleName())) {
            return;
        }

        event.setCanBreathe(true);
        BreathingComponent breathing = archetypeChunk.getComponent(index, BreathingComponent.getComponentType());
        if (breathing != null) {
            breathing.setSuffocating(false);
        }
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), BreathingComponent.getComponentType());
    }
}
