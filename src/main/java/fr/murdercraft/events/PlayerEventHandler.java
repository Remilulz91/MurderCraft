package fr.murdercraft.events;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.items.ModItems;
import fr.murdercraft.items.PistolItem;
import fr.murdercraft.roles.Role;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Listeners pour les événements joueurs : déconnexion, ramassage d'item, mort.
 */
public class PlayerEventHandler {

    public static void register() {
        // Déconnexion -> retirer de la partie si en cours
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            GameManager gm = GameManager.get();
            gm.removeParticipant(p);
            if (gm.isGameActive()) {
                gm.getRoleManager().markDead(p.getUuid());
            }
            MurderCraft.LOGGER.info("[Events] {} déconnecté", p.getName().getString());
        });

        // Empêcher tous les joueurs de casser des blocs avec couteau/pistolet
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            ItemStack held = player.getMainHandStack();
            if (held.isOf(ModItems.KNIFE) || held.isOf(ModItems.PISTOL) || held.isOf(ModItems.HIDDEN_PISTOL)) {
                return false;
            }
            return true;
        });

        // Player respawn during game = spectator
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            GameManager gm = GameManager.get();
            if (gm.isGameActive() && gm.getParticipants().contains(newPlayer.getUuid())) {
                gm.getRoleManager().markDead(newPlayer.getUuid());
                newPlayer.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
            }
        });

        // Mort d'un joueur — règles spéciales pour le justicier (drop du pistolet au sol)
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            GameManager gm = GameManager.get();
            if (!gm.isGameActive()) return true;
            if (!gm.getParticipants().contains(player.getUuid())) return true;

            Role role = gm.getRoleManager().getRole(player);
            if (role == Role.DETECTIVE) {
                // Récupère les balles restantes du pistolet AVANT de le retirer
                int ammo = PistolItem.MAX_AMMO;
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack s = player.getInventory().getStack(i);
                    if (s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL)) {
                        ammo = PistolItem.getAmmo(s);
                        break;
                    }
                }
                // Retire le pistolet de l'inventaire avant le drop normal
                player.getInventory().remove(
                        s -> s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL),
                        Integer.MAX_VALUE, player.getInventory());
                // Drop un HIDDEN_PISTOL au sol avec ses balles restantes
                gm.dropHiddenPistolAt(player, ammo);
            }
            return true; // Autorise la mort à se poursuivre normalement
        });
    }
}
