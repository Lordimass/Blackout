package com.perl.blackout.world.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongIterator;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.perl.blackout.world.components.CycelStateComponent;

public final class CyclePhase {

    private CyclePhase() {
    }

    public static void applyPhaseToWorld(@Nonnull World world, boolean on) {
        ChunkStore chunkStore = world.getChunkStore();
        Store<ChunkStore> store = chunkStore.getStore();

        LongIterator chunkIterator = chunkStore.getChunkIndexes().iterator();
        while (chunkIterator.hasNext()) {
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIterator.nextLong());
            if (chunkRef == null || !chunkRef.isValid()) {
                continue;
            }

            WorldChunk worldChunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
            BlockComponentChunk blockComponentChunk = store.getComponent(chunkRef, BlockComponentChunk.getComponentType());
            if (worldChunk == null || blockComponentChunk == null) {
                continue;
            }

            IntSet handled = new IntOpenHashSet();

            for (Int2ReferenceMap.Entry<Ref<ChunkStore>> entry : blockComponentChunk.getEntityReferences().int2ReferenceEntrySet()) {
                int blockIndex = entry.getIntKey();
                handled.add(blockIndex);
                CycelStateComponent component = store.getComponent(entry.getValue(), CycelStateComponent.getComponentType());
                if (component != null) {
                    applyAtBlock(world, worldChunk, blockIndex, component.stateFor(on), component.soundIndexFor(on));
                }
            }

            for (Int2ObjectMap.Entry<Holder<ChunkStore>> entry : blockComponentChunk.getEntityHolders().int2ObjectEntrySet()) {
                int blockIndex = entry.getIntKey();
                if (!handled.add(blockIndex)) {
                    continue;
                }
                CycelStateComponent component = entry.getValue().getComponent(CycelStateComponent.getComponentType());
                if (component != null) {
                    applyAtBlock(world, worldChunk, blockIndex, component.stateFor(on), component.soundIndexFor(on));
                }
            }
        }
    }

    static void applyState(@Nonnull ComponentAccessor<ChunkStore> accessor,
            @Nonnull Ref<ChunkStore> chunkRef,
            int blockIndex,
            @Nullable String stateName) {
        if (!chunkRef.isValid()) {
            return;
        }
        WorldChunk worldChunk = accessor.getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
            return;
        }
        applyAtBlock(null, worldChunk, blockIndex, stateName, 0);
    }

    private static void applyAtBlock(@Nullable World world,
            @Nonnull WorldChunk worldChunk,
            int blockIndex,
            @Nullable String stateName,
            int soundIndex) {
        int x = ChunkUtil.xFromBlockInColumn(blockIndex);
        int y = ChunkUtil.yFromBlockInColumn(blockIndex);
        int z = ChunkUtil.zFromBlockInColumn(blockIndex);

        if (stateName != null && !stateName.isEmpty()) {
            BlockType blockType = worldChunk.getBlockType(x, y, z);
            if (blockType != null) {
                worldChunk.setBlockInteractionState(x, y, z, blockType, stateName, false);
            }
        }

        if (world != null && soundIndex > 0) {
            double worldX = ((worldChunk.getX() << 5) | x) + 0.5;
            double worldZ = ((worldChunk.getZ() << 5) | z) + 0.5;
            SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, worldX, y + 0.5, worldZ,
                    world.getEntityStore().getStore());
        }
    }
}
