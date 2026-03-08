package com.howlstudio.voidduel.game;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds all mutable state for one Void Duel match.
 *
 * A match goes through:
 *   LOBBY → COUNTDOWN → ACTIVE → CRUMBLE → (repeat ACTIVE/CRUMBLE) → ENDED
 *
 * Platform crumble schedule:
 *   Every CRUMBLE_INTERVAL_TICKS ticks, one ring of outer blocks is removed.
 *   Players on removed blocks fall to their death.
 */
public class GameState {

    public enum Phase { LOBBY, COUNTDOWN, ACTIVE, ENDED }

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final int MIN_PLAYERS       = 2;
    public static final int MAX_PLAYERS       = 8;
    public static final int COUNTDOWN_SECONDS = 10;
    public static final int CRUMBLE_INTERVAL_SECONDS = 20; // 20s between crumble rings
    public static final int PLATFORM_RADIUS   = 8;  // blocks from center
    public static final int PLATFORM_Y        = 64;

    // One platform per player slot, arranged in a circle
    public static final int PLATFORM_SPACING  = 30; // blocks between platform centers

    // ── State ─────────────────────────────────────────────────────────────────
    private Phase phase = Phase.LOBBY;

    /** Players in the lobby / match. */
    private final List<PlayerRef> players = new CopyOnWriteArrayList<>();

    /** Players currently alive (still on a platform). */
    private final Set<UUID> alivePlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Kill-death tracking for the match. */
    private final Map<UUID, Integer> kills    = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> deaths   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> placement = new ConcurrentHashMap<>(); // finish position

    /** Current outer radius that's still intact. Starts at PLATFORM_RADIUS, shrinks to 0. */
    private int currentPlatformRadius = PLATFORM_RADIUS;

    /** Seconds remaining in countdown. */
    private int countdownSeconds = COUNTDOWN_SECONDS;

    /** Seconds until next crumble event. */
    private int secondsUntilCrumble = CRUMBLE_INTERVAL_SECONDS;

    /** Winner UUID (set when game ends). */
    private UUID winnerUuid;
    private String winnerName;

    /** World name for this match. */
    private String worldName = "void_duel";

    /** Center of the main arena. All platforms radiate from here. */
    private final Vector3i arenaCenter = new Vector3i(0, PLATFORM_Y, 0);

    private int placeCounter = 1;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Phase getPhase()                  { return phase; }
    public void setPhase(Phase p)            { phase = p; }

    public List<PlayerRef> getPlayers()      { return players; }
    public boolean addPlayer(PlayerRef p) {
        if (players.size() >= MAX_PLAYERS) return false;
        if (phase != Phase.LOBBY) return false;
        players.add(p);
        return true;
    }
    public void removePlayer(PlayerRef p)    { players.remove(p); alivePlayers.remove(p.getUuid()); }

    public Set<UUID> getAlivePlayers()       { return alivePlayers; }
    public void markAlive(UUID uuid)         { alivePlayers.add(uuid); }
    public void markDead(UUID uuid) {
        if (alivePlayers.remove(uuid)) {
            deaths.merge(uuid, 1, Integer::sum);
            placement.put(uuid, placeCounter++);
        }
    }
    public boolean isAlive(UUID uuid)        { return alivePlayers.contains(uuid); }
    public int aliveCount()                  { return alivePlayers.size(); }

    public void addKill(UUID uuid)           { kills.merge(uuid, 1, Integer::sum); }
    public int getKills(UUID uuid)           { return kills.getOrDefault(uuid, 0); }
    public int getDeaths(UUID uuid)          { return deaths.getOrDefault(uuid, 0); }

    public int getCurrentPlatformRadius()    { return currentPlatformRadius; }
    public boolean shrinkPlatform() {
        if (currentPlatformRadius <= 0) return false;
        currentPlatformRadius--;
        return true;
    }

    public int getCountdownSeconds()         { return countdownSeconds; }
    public void tickCountdown()              { if (countdownSeconds > 0) countdownSeconds--; }
    public boolean countdownDone()           { return countdownSeconds <= 0; }

    public int getSecondsUntilCrumble()      { return secondsUntilCrumble; }
    public void tickCrumble()                { if (secondsUntilCrumble > 0) secondsUntilCrumble--; }
    public boolean crumbleDue()              { return secondsUntilCrumble <= 0; }
    public void resetCrumble()               { secondsUntilCrumble = CRUMBLE_INTERVAL_SECONDS; }

    public UUID getWinnerUuid()              { return winnerUuid; }
    public String getWinnerName()            { return winnerName; }
    public void setWinner(UUID uuid, String name) { winnerUuid = uuid; winnerName = name; }

    public String getWorldName()             { return worldName; }
    public Vector3i getArenaCenter()         { return arenaCenter; }

    /** Get the center of a player's platform by their index (0-based). */
    public Vector3i getPlatformCenter(int playerIndex) {
        int count = players.size();
        double angle = (2 * Math.PI * playerIndex) / count;
        int x = (int) (PLATFORM_SPACING * Math.cos(angle));
        int z = (int) (PLATFORM_SPACING * Math.sin(angle));
        return new Vector3i(x, PLATFORM_Y, z);
    }

    /** Get the platform center for a specific player. */
    public Vector3i getPlatformForPlayer(UUID uuid) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUuid().equals(uuid)) return getPlatformCenter(i);
        }
        return arenaCenter;
    }

    public void reset() {
        phase               = Phase.LOBBY;
        players.clear();
        alivePlayers.clear();
        kills.clear();
        deaths.clear();
        placement.clear();
        currentPlatformRadius = PLATFORM_RADIUS;
        countdownSeconds    = COUNTDOWN_SECONDS;
        secondsUntilCrumble = CRUMBLE_INTERVAL_SECONDS;
        winnerUuid          = null;
        winnerName          = null;
        placeCounter        = 1;
    }
}
