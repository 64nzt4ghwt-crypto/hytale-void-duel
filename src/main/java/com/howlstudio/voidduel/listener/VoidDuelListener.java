package com.howlstudio.voidduel.listener;

import com.howlstudio.voidduel.game.GameLoop;
import com.howlstudio.voidduel.game.GameState;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Handles player connect/disconnect for Void Duel.
 * Removes players who disconnect mid-match.
 */
public class VoidDuelListener {

    private final GameState state;
    private final GameLoop loop;

    public VoidDuelListener(GameState state, GameLoop loop) {
        this.state = state;
        this.loop  = loop;
    }

    public void register() {
        var bus = HytaleServer.get().getEventBus();
        bus.registerGlobal(PlayerReadyEvent.class,     this::onJoin);
        bus.registerGlobal(PlayerDisconnectEvent.class, this::onLeave);
    }

    @SuppressWarnings("deprecation")
    private void onJoin(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        PlayerRef ref = player.getPlayerRef();
        if (ref == null) return;

        // Welcome message
        ref.sendMessage(Message.raw(
            "§6[Void Duel] §fWelcome! Type §e/vd join §fto fight on crumbling platforms. §7Last one standing wins!"
        ));
    }

    private void onLeave(PlayerDisconnectEvent event) {
        PlayerRef ref = event.getPlayerRef();
        if (ref == null) return;

        boolean inMatch = state.getPlayers().stream()
            .anyMatch(p -> p.getUuid().equals(ref.getUuid()));

        if (inMatch) {
            state.removePlayer(ref);
            System.out.println("[VoidDuel] " + ref.getUsername() + " disconnected — removed from match.");
        }
    }
}
