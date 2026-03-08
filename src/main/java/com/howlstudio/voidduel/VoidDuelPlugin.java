package com.howlstudio.voidduel;

import com.howlstudio.voidduel.command.VoidDuelCommand;
import com.howlstudio.voidduel.game.GameLoop;
import com.howlstudio.voidduel.game.GameState;
import com.howlstudio.voidduel.listener.VoidDuelListener;
import com.howlstudio.voidduel.world.ArenaBuilder;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * Void Duel — Platform Battle for Hytale.
 *
 * 2–8 players spawn on floating stone islands above the void.
 * Every 20 seconds, the outer ring of each platform crumbles into air.
 * The last player standing wins.
 *
 * Category: Experiences (Hytale New Worlds Modding Contest 2026)
 *
 * Commands: /vd [join|leave|status|start|stop]
 *
 * @version 1.0.0
 */
public final class VoidDuelPlugin extends JavaPlugin {

    private GameLoop loop;

    public VoidDuelPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        log("[VoidDuel] Loading Void Duel...");

        GameState   state = new GameState();
        ArenaBuilder arena = new ArenaBuilder(state);
        this.loop         = new GameLoop(state, arena);

        new VoidDuelListener(state, loop).register();
        CommandManager.get().register(new VoidDuelCommand(state, loop));

        loop.start();

        log("[VoidDuel] Ready! §e/vd join §fto play.");
        log("[VoidDuel] Platform radius: " + GameState.PLATFORM_RADIUS
            + " | Crumble interval: " + GameState.CRUMBLE_INTERVAL_SECONDS + "s"
            + " | Min players: " + GameState.MIN_PLAYERS);
    }

    @Override
    protected void shutdown() {
        if (loop != null) loop.stop();
        log("[VoidDuel] Shutdown complete.");
    }

    private void log(String msg) { System.out.println(msg); }
}
