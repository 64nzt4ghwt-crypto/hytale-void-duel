# Void Duel 🌀

**Hytale New Worlds Modding Contest 2026 — Experiences Category**

> *Fight for survival on crumbling platforms above the void. Last player standing wins.*

---

## What Is It?

Void Duel is a multiplayer platform battle minigame for Hytale servers.

2–8 players each spawn on their own circular stone island floating high above the void. Every 20 seconds, the **outer ring of every platform crumbles into air**. Players who fall off the edge plunge into the void. Last one standing wins.

Simple concept. Brutal execution. Incredible spectator experience.

---

## How to Play

1. Join the server and type `/vd join`
2. Wait for 2+ players, then the countdown begins
3. Platforms build automatically, players teleport to their islands
4. Fight, dodge, and survive
5. Every 20 seconds: **the edge falls**
6. Platform shrinks ring by ring until it's gone
7. Last player alive wins

---

## Commands

| Command | Description |
|---------|-------------|
| `/vd join` | Join the lobby |
| `/vd leave` | Leave |
| `/vd status` | Current match state |
| `/vd start` | Force start (admin) |
| `/vd stop` | Force stop (admin) |

---

## Installation

Drop `VoidDuelPlugin-1.0.0.jar` into your server's `mods/` directory and restart.

No config needed. Works on any world.

---

## Building From Source

```bash
git clone https://github.com/64nzt4ghwt-crypto/hytale-void-duel
cd hytale-void-duel

# Compile (requires JDK 21+)
javac --release 21 \
  -cp "libs/HytaleServer.jar" \
  -d build/classes \
  $(find src -name "*.java")

jar cfm VoidDuelPlugin-1.0.0.jar MANIFEST.MF -C build/classes .
```

Or via Gradle (requires JDK compatible with Gradle 8.13):
```bash
./gradlew jar
```

---

## Game Design Notes

- **Platform radius:** 8 blocks from center
- **Players:** 2 minimum, 8 maximum
- **Crumble interval:** 20 seconds
- **Player spacing:** 30 blocks between platforms
- **Death:** Fall below Y=40 → eliminated
- **Win condition:** Last player with `isAlive` status

---

## Technical Stack

- Java 21 (compiled with `--release 21` for Hytale compatibility)
- Hytale Plugin API (`JavaPlugin`, ECS event system, `BlockAccessor.setBlock()`)
- `ScheduledExecutorService` for 1-second game loop ticks
- No external dependencies beyond the Hytale server JAR

---

## License

MIT — use it, fork it, build on it.

---

*Built by HowlStudio for the Hytale New Worlds Modding Contest 2026*
*Contest page: https://hytale.curseforge.com/newworldscontest/*
