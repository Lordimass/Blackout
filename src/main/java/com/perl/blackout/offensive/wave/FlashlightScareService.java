package com.perl.blackout.offensive.wave;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

/**
 * Scares Seekers that are near a player holding a lit flashlight.
 *
 * <p>The Seeker role cannot detect a held item's on/off state by itself: the flashlight's On/Off is
 * an item-state swap (see the mod's {@code SetStateInteraction}, which calls {@code withState}), and
 * NPC sight filters only match by item id — and the on-state id isn't resolvable when the role is
 * built. So the detection happens here in Java and is pushed onto each Seeker as a single boolean
 * role flag (slot {@link #FLASHLIGHT_FLAG_SLOT}) that the role's FSM reads through {@code Flag}
 * sensors to enter and leave its Scared state.
 *
 * <p>"Lit flashlight" = the held item id belongs to the flashlight family and its resolved item emits
 * light. Only the On state variant carries a {@code Light}, so {@code getItem().getLight() != null}
 * means the flashlight is switched on.
 *
 * <p>All methods must run on the world thread (they mutate role flags), same as {@link ObjectiveService}.
 */
final class FlashlightScareService {

    /**
     * Flag slot the Seeker role uses for "a lit flashlight is near me". The Seeker role declares
     * exactly one flag (referenced by name in its FSM {@code Flag} sensors), so it is allocated slot
     * 0. Keep this in sync with the {@code "Name": "FlashlightNear"} flag in Template_The_Seeker.json.
     */
    static final int FLASHLIGHT_FLAG_SLOT = 0;

    /** Base flashlight item id. State variants are generated as contained ids under this id. */
    private static final String FLASHLIGHT_ITEM_ID = "BO_Flashlight";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long DIAG_INTERVAL_MS = 2000L;

    private final double scareRangeSq;
    private long lastDiagMs = 0L;

    FlashlightScareService(double scareRange) {
        this.scareRangeSq = scareRange * scareRange;
    }

    /**
     * Sets each tracked Seeker's flashlight flag based on whether a player holding a lit flashlight is
     * within range of it. Must run on the world thread.
     */
    void apply(@Nonnull WaveGame game, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        long now = System.currentTimeMillis();
        boolean diag = now - lastDiagMs >= DIAG_INTERVAL_MS;
        if (diag) {
            lastDiagMs = now;
        }

        List<Vector3d> litPlayers = litFlashlightPlayerPositions(store, world, diag);

        int total = 0;
        int flagged = 0;
        synchronized (game.getEnemies()) {
            for (Ref<EntityStore> enemy : game.getEnemies()) {
                if (enemy == null || !enemy.isValid()) {
                    continue;
                }
                NPCEntity npc = store.getComponent(enemy, NPCEntity.getComponentType());
                if (npc == null) {
                    continue;
                }
                Role role = npc.getRole();
                if (role == null) {
                    continue;
                }
                total++;
                boolean scared = isNearAny(WavePlayers.positionOf(store, enemy), litPlayers);
                if (scared) {
                    flagged++;
                }
                try {
                    role.setFlag(FLASHLIGHT_FLAG_SLOT, scared);
                } catch (RuntimeException ex) {
                    if (diag) {
                        LOGGER.atWarning().withCause(ex).log("setFlag(%s) failed on a Seeker", FLASHLIGHT_FLAG_SLOT);
                    }
                }
            }
        }

        if (diag) {
            LOGGER.atInfo().log("[FlashlightScare] litPlayers=%s seekers=%s flagged=%s",
                    litPlayers.size(), total, flagged);
        }
    }

    private List<Vector3d> litFlashlightPlayerPositions(Store<EntityStore> store, World world, boolean diag) {
        List<Vector3d> out = new ArrayList<>();
        for (Ref<EntityStore> playerRef : WavePlayers.refs(world)) {
            if (!isHoldingLitFlashlight(store, playerRef, diag)) {
                continue;
            }
            Vector3d pos = WavePlayers.positionOf(store, playerRef);
            if (pos != null) {
                out.add(pos);
            }
        }
        return out;
    }

    private boolean isHoldingLitFlashlight(Store<EntityStore> store, Ref<EntityStore> playerRef, boolean diag) {
        ItemStack held = InventoryComponent.getItemInHand(store, playerRef);
        if (held == null) {
            if (diag) {
                LOGGER.atInfo().log("[FlashlightScare] held=<none>");
            }
            return false;
        }
        String id = held.getItemId();
        Item item = held.getItem();
        boolean hasLight = item != null && item.getLight() != null;
        boolean flashlightFamily = isFlashlightItemId(id);
        if (diag) {
            LOGGER.atInfo().log("[FlashlightScare] held id=%s durability=%s flashlightFamily=%s hasLight=%s",
                    id, held.getDurability(), flashlightFamily, hasLight);
        }
        if (!flashlightFamily) {
            return false;
        }
        // Only the On state variant carries a Light; the off/base flashlight has none.
        return hasLight;
    }

    private boolean isFlashlightItemId(@Nullable String id) {
        if (id == null) {
            return false;
        }
        String normalized = id.startsWith("*") ? id.substring(1) : id;
        return normalized.equals(FLASHLIGHT_ITEM_ID) || normalized.startsWith(FLASHLIGHT_ITEM_ID + "_");
    }

    private boolean isNearAny(@Nullable Vector3d enemyPos, List<Vector3d> players) {
        if (enemyPos == null) {
            return false;
        }
        for (Vector3d player : players) {
            if (enemyPos.distanceSquared(player) <= scareRangeSq) {
                return true;
            }
        }
        return false;
    }
}
