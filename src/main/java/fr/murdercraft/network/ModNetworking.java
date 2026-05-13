package fr.murdercraft.network;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.client.MurderCraftClientState;
import fr.murdercraft.game.GamePhase;
import fr.murdercraft.game.WinResult;
import fr.murdercraft.network.payloads.GameStatePayload;
import fr.murdercraft.network.payloads.RoleAssignPayload;
import fr.murdercraft.roles.Role;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Enregistrement des packets réseau du mod et helpers d'envoi.
 */
public class ModNetworking {

    /** À appeler côté serveur (et common) — enregistre les types de payloads. */
    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(RoleAssignPayload.ID, RoleAssignPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GameStatePayload.ID, GameStatePayload.CODEC);
        MurderCraft.LOGGER.info("[Networking] Payloads serveur enregistrés");
    }

    /** À appeler côté client — enregistre les receveurs. */
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(RoleAssignPayload.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                Role role = parseRole(payload.role());
                MurderCraftClientState.setMyRole(role);
                MurderCraft.LOGGER.info("[Client] Mon rôle : {}", role);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GameStatePayload.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                GamePhase phase = parsePhase(payload.phase());
                WinResult win = payload.winResult().isEmpty() ? null : parseWin(payload.winResult());
                MurderCraftClientState.updateGameState(phase, payload.secondsLeft(), win);
            });
        });

        MurderCraft.LOGGER.info("[Networking] Receveurs client enregistrés");
    }

    // === Envoi serveur -> client ===

    public static void sendRoleAssign(ServerPlayerEntity player, Role role) {
        ServerPlayNetworking.send(player, new RoleAssignPayload(role.name()));
    }

    public static void sendGameState(ServerPlayerEntity player, GamePhase phase, int secondsLeft, WinResult win) {
        ServerPlayNetworking.send(player, new GameStatePayload(
                phase.name(),
                secondsLeft,
                win == null ? "" : win.name()
        ));
    }

    // === Parsing helpers ===

    private static Role parseRole(String s) {
        try {
            return Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Role.SPECTATOR;
        }
    }

    private static GamePhase parsePhase(String s) {
        try {
            return GamePhase.valueOf(s);
        } catch (IllegalArgumentException e) {
            return GamePhase.LOBBY;
        }
    }

    private static WinResult parseWin(String s) {
        try {
            return WinResult.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
