package com.howlstudio.voidduel.game;

import com.howlstudio.voidduel.world.ArenaBuilder;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Void Duel game loop.
 *
 * Runs on a 1-second tick scheduler:
 *
 *  LOBBY:      Wait for players. /vd join to enter.
 *  COUNTDOWN:  10-second countdown, build platforms, teleport players.
 *  ACTIVE:     Players fight. Crumble timer ticks down.
 *  CRUMBLE:    Remove one ring of blocks. Broadcast warning. Check survivors.
 *  ENDED:      Announce winner, schedule restart.
 */
public class GameLoop {

    private final GameState state;
    private final ArenaBuilder arena;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;

    /** Last known Y positions of players to detect void fall. */
    private static final int VOID_THRESHOLD = 40; // below this Y = dead

    public GameLoop(GameState state, ArenaBuilder arena) {
        this.state = state;
        this.arena = arena;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "void-duel-loop");
            t.setDaemon(true);
            return t;
        });
        tickTask = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
        System.out.println("[VoidDuel] Game loop started.");
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
    }

    // ── Main Tick ─────────────────────────────────────────────────────────────

    private void tick() {
        try {
            switch (state.getPhase()) {
                case LOBBY     -> tickLobby();
                case COUNTDOWN -> tickCountdown();
                case ACTIVE    -> tickActive();
                case ENDED     -> {} // nothing — waiting for reset
            }
        } catch (Exception e) {
            System.err.println("[VoidDuel] Tick error: " + e.getMessage());
        }
    }

    private void tickLobby() {
        int players = state.getPlayers().size();
        if (players >= GameState.MIN_PLAYERS) {
            broadcast("§6[Void Duel] §f" + players + " players ready. Starting in §e"
                + GameState.COUNTDOWN_SECONDS + "§f seconds! §7(/vd join to play)");
            state.setPhase(GameState.Phase.COUNTDOWN);
        }
    }

    private void tickCountdown() {
        state.tickCountdown();
        int secs = state.getCountdownSeconds();

        if (secs == GameState.COUNTDOWN_SECONDS - 1) {
            // Build platforms right away
            World world = getWorld();
            if (world != null) {
                arena.buildArena(world);
                teleportPlayers();
            }
            broadcast("§6[Void Duel] §fPlatforms built! Brace yourselves...");
        }

        if (secs <= 5 && secs > 0) {
            broadcast("§e[Void Duel] §f" + secs + "...");
        }

        if (state.countdownDone()) {
            // Mark everyone alive
            for (PlayerRef p : state.getPlayers()) {
                state.markAlive(p.getUuid());
            }
            broadcast("§c§l[VOID DUEL] §r§aFIGHT! §7Platforms crumble in §e"
                + GameState.CRUMBLE_INTERVAL_SECONDS + "s§7.");
            state.setPhase(GameState.Phase.ACTIVE);
        }
    }

    private void tickActive() {
        // Check for void falls
        checkVoidFalls();

        // Check win condition
        if (state.aliveCount() <= 1) {
            endGame();
            return;
        }

        // Tick crumble timer
        state.tickCrumble();

        // Announce warning at 5 seconds
        if (state.getSecondsUntilCrumble() == 5) {
            broadcast("§c[Void Duel] §fEdges crumble in §c5 seconds§f!");
        }

        if (state.crumbleDue()) {
            doCrumble();
            state.resetCrumble();

            // After crumble, re-check survival
            if (state.aliveCount() <= 1) {
                endGame();
            }
        }
    }

    // ── Game Events ───────────────────────────────────────────────────────────

    private void checkVoidFalls() {
        World world = getWorld();
        if (world == null) return;

        for (PlayerRef player : state.getPlayers()) {
            if (!state.isAlive(player.getUuid())) continue;

            Transform t = player.getTransform();
            if (t == null) continue;

            Vector3d pos = t.getPosition();
            if (pos == null) continue;

            if (pos.y < VOID_THRESHOLD) {
                killPlayer(player, null);
            }
        }
    }

    private void killPlayer(PlayerRef victim, PlayerRef killer) {
        if (!state.isAlive(victim.getUuid())) return;

        state.markDead(victim.getUuid());
        int remaining = state.aliveCount();

        String deathMsg;
        if (killer != null) {
            state.addKill(killer.getUuid());
            deathMsg = "§c☠ §e" + victim.getUsername() + " §7was sent into the void by §e"
                + killer.getUsername() + "§7! §f(" + remaining + " left)";
        } else {
            deathMsg = "§c☠ §e" + victim.getUsername() + " §7fell into the void! §f(" + remaining + " left)";
        }

        broadcast(deathMsg);

        // Teleport dead player to spectate point
        try {
            victim.updatePosition(getWorld(),
                new Transform(0, GameState.PLATFORM_Y + 20, 0), null);
        } catch (Exception ignored) {}
    }

    public void handlePlayerDeath(PlayerRef victim, PlayerRef killer) {
        killPlayer(victim, killer);
        if (state.aliveCount() <= 1) endGame();
    }

    private void doCrumble() {
        World world = getWorld();
        if (world == null) return;

        int newRadius = state.getCurrentPlatformRadius();
        if (newRadius <= 0) {
            broadcast("§c[Void Duel] §fPlatforms fully collapsed!");
            return;
        }

        // Warn then crumble
        broadcast("§c§l[CRUMBLE!] §r§fOuter ring falling!");

        // Remove the ring
        arena.crumbleRing(world, newRadius);
        state.shrinkPlatform();

        int remaining = state.getCurrentPlatformRadius();
        if (remaining > 0) {
            broadcast("§7Platform radius: §c" + remaining + " §7blocks remaining. Next crumble in §e"
                + GameState.CRUMBLE_INTERVAL_SECONDS + "s§7.");
        } else {
            broadcast("§c§l[VOID DUEL] §r§fPlatforms are GONE. Final seconds!");
        }
    }

    private void endGame() {
        state.setPhase(GameState.Phase.ENDED);

        // Find winner
        PlayerRef winner = null;
        for (PlayerRef p : state.getPlayers()) {
            if (state.isAlive(p.getUuid())) { winner = p; break; }
        }

        if (winner != null) {
            state.setWinner(winner.getUuid(), winner.getUsername());
            broadcast("§6§l★ WINNER ★ §r§e" + winner.getUsername()
                + " §fsurvives the void! §aKills: " + state.getKills(winner.getUuid()));
        } else {
            broadcast("§6[Void Duel] §fAll players fell — it's a draw!");
        }

        // Show scoreboard
        broadcast("§8══ §6MATCH RESULTS §8══");
        for (PlayerRef p : state.getPlayers()) {
            broadcast("§e" + p.getUsername() + " §7— Kills: §a" + state.getKills(p.getUuid()));
        }

        // Schedule reset in 15 seconds
        scheduler.schedule(this::resetMatch, 15, TimeUnit.SECONDS);
    }

    private void resetMatch() {
        World world = getWorld();
        if (world != null) arena.clearArena(world);
        state.reset();
        broadcast("§6[Void Duel] §fMatch reset! Use §e/vd join §fto play again.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void teleportPlayers() {
        List<PlayerRef> players = state.getPlayers();
        World world = getWorld();
        if (world == null) return;

        for (int i = 0; i < players.size(); i++) {
            PlayerRef p = players.get(i);
            Vector3i center = state.getPlatformCenter(i);
            try {
                Transform spawnPos = new Transform(
                    center.getX(), center.getY() + 1, center.getZ()
                );
                p.updatePosition(world, spawnPos, null);
            } catch (Exception e) {
                System.err.println("[VoidDuel] Failed to teleport " + p.getUsername() + ": " + e.getMessage());
            }
        }
    }

    private World getWorld() {
        try {
            return Universe.get().getWorld(state.getWorldName());
        } catch (Exception e) {
            return Universe.get().getDefaultWorld();
        }
    }

    private void broadcast(String message) {
        try {
            Message msg = Message.raw(message);
            for (PlayerRef p : state.getPlayers()) {
                try { p.sendMessage(msg); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
