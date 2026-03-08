package com.howlstudio.voidduel.command;

import com.howlstudio.voidduel.game.GameLoop;
import com.howlstudio.voidduel.game.GameState;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import java.util.Arrays;

/**
 * /vd command — Void Duel interface.
 *
 *   /vd join    — Join the lobby
 *   /vd leave   — Leave the lobby or spectate
 *   /vd status  — Show current game state
 *   /vd start   — Force start (admin)
 *   /vd stop    — Force stop (admin)
 */
public class VoidDuelCommand extends AbstractPlayerCommand {

    private final GameState state;
    private final GameLoop loop;

    public VoidDuelCommand(GameState state, GameLoop loop) {
        super("vd", "Void Duel — fight on crumbling platforms above the void!");
        this.state = state;
        this.loop  = loop;
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {

        String input = ctx.getInputString().trim();
        String[] parts = input.split("\\s+");
        String[] args  = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        if (args.length == 0 || args[0].equalsIgnoreCase("join")) {
            doJoin(playerRef);
        } else {
            switch (args[0].toLowerCase()) {
                case "leave"  -> doLeave(playerRef);
                case "status" -> doStatus(playerRef);
                case "start"  -> doForceStart(playerRef);
                case "stop"   -> doStop(playerRef);
                default       -> sendHelp(playerRef);
            }
        }
    }

    private void doJoin(PlayerRef playerRef) {
        if (state.getPhase() != GameState.Phase.LOBBY) {
            send(playerRef, "§c[Void Duel] §fA match is already in progress. Wait for the next round!");
            return;
        }

        // Already in the lobby?
        for (PlayerRef p : state.getPlayers()) {
            if (p.getUuid().equals(playerRef.getUuid())) {
                send(playerRef, "§7[Void Duel] §fYou're already in the lobby ("
                    + state.getPlayers().size() + "/" + GameState.MAX_PLAYERS + ").");
                return;
            }
        }

        if (!state.addPlayer(playerRef)) {
            send(playerRef, "§c[Void Duel] §fLobby is full (" + GameState.MAX_PLAYERS + " players max).");
            return;
        }

        int count = state.getPlayers().size();
        broadcast("§6[Void Duel] §e" + playerRef.getUsername()
            + " §fjoined! §7(" + count + "/" + GameState.MAX_PLAYERS + ")");

        if (count < GameState.MIN_PLAYERS) {
            send(playerRef, "§7Waiting for §e" + (GameState.MIN_PLAYERS - count)
                + " §7more player(s) to start.");
        }
    }

    private void doLeave(PlayerRef playerRef) {
        boolean was = state.getPlayers().stream().anyMatch(p -> p.getUuid().equals(playerRef.getUuid()));
        if (!was) { send(playerRef, "§7You're not in the current match."); return; }

        state.removePlayer(playerRef);
        send(playerRef, "§6[Void Duel] §fYou left the match.");
        broadcast("§7[Void Duel] §e" + playerRef.getUsername() + " §7left.");
    }

    private void doStatus(PlayerRef playerRef) {
        send(playerRef, "§8══ §6Void Duel Status §8══");
        send(playerRef, "§7Phase:   §f" + state.getPhase().name());
        send(playerRef, "§7Players: §f" + state.getPlayers().size() + "/" + GameState.MAX_PLAYERS);
        send(playerRef, "§7Alive:   §f" + state.aliveCount());
        if (state.getPhase() == GameState.Phase.ACTIVE) {
            send(playerRef, "§7Platform: §fRadius §c" + state.getCurrentPlatformRadius()
                + " §7(crumble in §c" + state.getSecondsUntilCrumble() + "s§7)");
        }
        if (state.getWinnerName() != null) {
            send(playerRef, "§7Last winner: §e" + state.getWinnerName());
        }
    }

    private void doForceStart(PlayerRef playerRef) {
        // Simple permission check: name-based for now
        if (state.getPhase() != GameState.Phase.LOBBY) {
            send(playerRef, "§cCan only force-start from LOBBY phase.");
            return;
        }
        if (state.getPlayers().size() < GameState.MIN_PLAYERS) {
            send(playerRef, "§cNeed at least " + GameState.MIN_PLAYERS + " players.");
            return;
        }
        state.setPhase(GameState.Phase.COUNTDOWN);
        broadcast("§6[Void Duel] §fForce-started by §e" + playerRef.getUsername() + "§f!");
    }

    private void doStop(PlayerRef playerRef) {
        state.reset();
        broadcast("§c[Void Duel] §fMatch stopped by §e" + playerRef.getUsername() + "§f.");
    }

    private void sendHelp(PlayerRef playerRef) {
        send(playerRef, "§8══ §6VOID DUEL §8══");
        send(playerRef, "§7Fight for survival on crumbling platforms above the void!");
        send(playerRef, "§e/vd join §7— Join the lobby");
        send(playerRef, "§e/vd leave §7— Leave");
        send(playerRef, "§e/vd status §7— Match status");
        send(playerRef, "§7Need §e" + GameState.MIN_PLAYERS + "–" + GameState.MAX_PLAYERS + " §7players to start.");
    }

    private void send(PlayerRef ref, String text) { ref.sendMessage(Message.raw(text)); }

    private void broadcast(String text) {
        Message msg = Message.raw(text);
        for (PlayerRef p : state.getPlayers()) {
            try { p.sendMessage(msg); } catch (Exception ignored) {}
        }
    }
}
