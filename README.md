# Void Duel 🗡️

**A platform battle experience for Hytale — 2026 New Worlds Modding Contest Entry**

> *Fight for survival on crumbling islands above the void. Last player standing wins.*

---

## What Is Void Duel?

2–8 players spawn on floating stone platforms above an endless void. Every 20 seconds, the outer ring of each platform crumbles into air. The platforms shrink. The void grows closer. The last player standing wins.

It's a game of aggression, positioning, and desperation — simple to understand, chaotic to play.

---

## Gameplay

1. Players type `/vd join` to enter the lobby
2. Match auto-starts when 2+ players join (or admin forces start)
3. Each player spawns on their own floating platform
4. Every **20 seconds**: the outer ring of all platforms crumbles
5. Players who fall into the void are eliminated
6. **Last one standing wins**
7. Match resets automatically after 15 seconds

### Platform Progression

```
Round 1: [████████████████] Radius 8
Round 2: [██████████████  ] Radius 7
Round 3: [████████████    ] Radius 6
...
Round 8: [████            ] Radius 1 — PANIC
Round 9: Platform gone. All remaining players fall.
```

---

## Commands

| Command | Description |
|---------|-------------|
| `/vd join` | Join the lobby |
| `/vd leave` | Leave the match |
| `/vd status` | Current match status |
| `/vd start` | Force start (admin) |
| `/vd stop` | Reset the match (admin) |

---

## Installation

1. Drop `VoidDuelPlugin-1.0.0.jar` into your Hytale server's `mods/` directory
2. Restart the server
3. Players can immediately join with `/vd join`

No configuration needed. Works out of the box.

---

## Technical Details

- **Language:** Java 21 (compatible with Hytale JDK 25 runtime)
- **API:** Hytale Server Plugin API (`com.hypixel.hytale`)
- **Events:** `PlayerReadyEvent`, `PlayerDisconnectEvent`
- **Block manipulation:** `BlockAccessor.setBlock()` for platform crumbling
- **Scheduling:** `ScheduledExecutorService` for game loop (1-second ticks)
- **Architecture:** Stateless plugin — GameState, GameLoop, ArenaBuilder, Command, Listener

### Key Classes

| Class | Purpose |
|-------|---------|
| `VoidDuelPlugin` | Plugin entry point, wires everything together |
| `GameState` | All mutable match state (phase, players, platform radius) |
| `GameLoop` | 1-second tick loop — countdown, crumble, death detection, win condition |
| `ArenaBuilder` | Builds/crumbles floating platforms via `BlockAccessor` |
| `VoidDuelCommand` | `/vd` command handler |
| `VoidDuelListener` | Player join/leave event handling |

---

## Building From Source

```bash
# Requires: JDK 21+, Gradle 8+, HytaleServer.jar in libs/
./gradlew jar
# Output: build/libs/VoidDuelPlugin-1.0.0.jar
```

> **Note:** `HytaleServer.jar` is not included in this repo (too large, proprietary).
> Copy it from your Hytale server installation into `libs/` before building.

---

## Contest Submission

- **Category:** Experiences
- **Contest:** Hytale New Worlds Modding Contest 2026
- **CurseForge:** [Coming Soon]
- **Developer:** HowlStudio

---

*Built with the Hytale Plugin API. All game logic runs server-side.*
