package fr.murdercraft.events;

import fr.murdercraft.game.GameManager;
import fr.murdercraft.items.ModItems;
import fr.murdercraft.network.ModNetworking;
import fr.murdercraft.roles.Role;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Listeners pour les événements de combat et les interactions avec joueurs.
 * Notamment : promotion automatique quand un INNOCENT ramasse le pistolet caché.
 */
public class CombatEventHandler {

    public static void register() {
        // Empêcher les non-meurtriers d'attaquer avec le couteau
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            GameManager gm = GameManager.get();
            if (!gm.isGameActive()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

            ItemStack held = sp.getStackInHand(hand);
            if (held.isOf(ModItems.KNIFE)) {
                Role role = gm.getRoleManager().getRole(sp);
                if (role != Role.MURDERER) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // Interagir (clic droit) sur entité — empêcher si pas le bon rôle
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Tout pass — le pistolet utilise USE Item, pas USE entity
            return ActionResult.PASS;
        });
    }

    /**
     * Appelé quand un joueur ramasse un item — vérifie si c'est le pistolet caché.
     * Cette méthode est invoquée via mixin ou par les events de pickup standard.
     */
    public static void onItemPickup(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isOf(ModItems.HIDDEN_PISTOL)) {
            GameManager gm = GameManager.get();
            if (gm.isGameActive() && gm.getRoleManager().getRole(player) == Role.INNOCENT) {
                gm.promoteToDetective(player);
                // Remplace le HIDDEN_PISTOL par un PISTOL standard
                stack.decrement(stack.getCount());
                player.getInventory().insertStack(new ItemStack(ModItems.PISTOL));
            }
        }
    }
}
