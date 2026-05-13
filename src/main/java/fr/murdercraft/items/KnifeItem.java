package fr.murdercraft.items;

import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.roles.Role;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Couteau du Meurtrier.
 * - Un coup = kill (configurable)
 * - Ne peut pas être utilisé par les non-meurtriers (effet visuel mais aucun dégât)
 */
public class KnifeItem extends Item {

    public KnifeItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity attackerPlayer) {
            GameManager gm = GameManager.get();
            if (gm.isGameActive() && gm.getRoleManager().getRole(attackerPlayer) == Role.MURDERER) {
                if (target instanceof ServerPlayerEntity victim) {
                    // Vérifier que la victime est en jeu et vivante
                    if (gm.getRoleManager().isAlive(victim)) {
                        if (MurderCraftConfig.get().knifeOneShot) {
                            // One-shot
                            victim.damage(victim.getDamageSources().playerAttack(attackerPlayer), Float.MAX_VALUE);
                        }
                        // Son discret pour le meurtrier
                        attackerPlayer.playSoundToPlayer(SoundEvents.BLOCK_LAVA_EXTINGUISH, attackerPlayer.getSoundCategory(), 0.4f, 1.5f);
                    }
                }
            } else {
                // Le joueur n'est pas meurtrier — empêcher l'attaque
                attackerPlayer.sendMessage(Text.translatable("murdercraft.knife.not_murderer").formatted(net.minecraft.util.Formatting.RED), true);
                return false;
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean canMine(net.minecraft.block.BlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, PlayerEntity miner) {
        // Le couteau ne casse pas les blocs
        return false;
    }
}
