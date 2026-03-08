package com.howlstudio.voidduel.world;

import com.howlstudio.voidduel.game.GameState;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Builds and manages Void Duel platforms.
 *
 * Each player gets a circular floating stone platform above the void.
 * Platforms are built during match setup and crumbled ring-by-ring as the game progresses.
 *
 * Block types (Hytale string IDs):
 *  - Platform surface: "stone" (solid footing)
 *  - Platform edge:    "cobblestone" (visual indicator of danger zone)
 *  - Air:              "air" (crumble target)
 */
public class ArenaBuilder {

    private static final String PLATFORM_BLOCK  = "stone";
    private static final String EDGE_BLOCK      = "cobblestone";
    private static final String AIR             = "air";

    private final GameState state;

    public ArenaBuilder(GameState state) {
        this.state = state;
    }

    /**
     * Build all platforms for the current player set.
     * Called when match transitions from LOBBY → COUNTDOWN.
     */
    public void buildArena(World world) {
        int playerCount = state.getPlayers().size();
        for (int i = 0; i < playerCount; i++) {
            Vector3i center = state.getPlatformCenter(i);
            buildPlatform(world, center, GameState.PLATFORM_RADIUS);
        }
        System.out.println("[VoidDuel] Built " + playerCount + " platforms (radius="
            + GameState.PLATFORM_RADIUS + ")");
    }

    /**
     * Crumble the outermost ring of all platforms.
     * Called on each crumble tick.
     *
     * @return the new radius after crumbling, or -1 if platforms are fully gone.
     */
    public int crumbleRing(World world, int currentRadius) {
        if (currentRadius <= 0) return -1;

        int playerCount = state.getPlayers().size();
        for (int i = 0; i < playerCount; i++) {
            Vector3i center = state.getPlatformCenter(i);
            removeRing(world, center, currentRadius);
        }

        return currentRadius - 1;
    }

    /**
     * Clear all platforms (end of match cleanup).
     */
    public void clearArena(World world) {
        int playerCount = state.getPlayers().size();
        int radius = GameState.PLATFORM_RADIUS;
        for (int i = 0; i < playerCount; i++) {
            Vector3i center = state.getPlatformCenter(i);
            for (int r = 0; r <= radius; r++) {
                removeRing(world, center, r);
            }
        }
    }

    // ── Block operations ──────────────────────────────────────────────────────

    private void buildPlatform(World world, Vector3i center, int radius) {
        BlockAccessor accessor = getAccessor(world, center);
        if (accessor == null) return;

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > radius) continue;

                // Outer ring = cobblestone (visual warning)
                String block = (dist > radius - 1.5) ? EDGE_BLOCK : PLATFORM_BLOCK;
                accessor.setBlock(cx + x, cy, cz + z, block);

                // Clear the block above (make sure players can walk)
                accessor.setBlock(cx + x, cy + 1, cz + z, AIR);
                accessor.setBlock(cx + x, cy + 2, cz + z, AIR);
            }
        }
    }

    private void removeRing(World world, Vector3i center, int radius) {
        BlockAccessor accessor = getAccessor(world, center);
        if (accessor == null) return;

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                // Only remove the exact ring at this radius
                if (dist <= radius && dist > radius - 1) {
                    accessor.setBlock(cx + x, cy, cz + z, AIR);
                }
            }
        }
    }

    private BlockAccessor getAccessor(World world, Vector3i center) {
        try {
            // Get chunk at center position and use it as BlockAccessor
            long chunkKey = chunkKey(center.getX() >> 4, center.getZ() >> 4);
            BlockAccessor accessor = world.getChunkIfLoaded(chunkKey);
            return accessor; // May be null if chunk not loaded
        } catch (Exception e) {
            System.err.println("[VoidDuel] Failed to get block accessor: " + e.getMessage());
            return null;
        }
    }

    private long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    /**
     * Get list of crumble positions for a given radius ring (for sound/particle effects).
     */
    public List<Vector3i> getCrumblePositions(Vector3i center, int radius) {
        List<Vector3i> positions = new ArrayList<>();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= radius && dist > radius - 1) {
                    positions.add(new Vector3i(cx + x, cy, cz + z));
                }
            }
        }
        return positions;
    }
}
