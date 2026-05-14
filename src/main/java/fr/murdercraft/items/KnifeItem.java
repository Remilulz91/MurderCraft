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
            MurderCraftConfig cfg = MurderCraftConfig.get();

            // [DEBUG] One-shot les mobs si le flag est activé (peu importe le rôle)
            if (cfg.debugAllowMobDamage && !(target instanceof ServerPlayerEntity)) {
                target.damage(target.getDamageSources().playerAttack(attackerPlayer), Float.MAX_VALUE);
                playKnifeSound(attackerPlayer);
                attackerPlayer.sendMessage(Text.translatable("murdercraft.debug.mob_hit", target.getName())
                        .formatted(net.minecraft.util.Formatting.LIGHT_PURPLE), true);
                return true;
            }

            if (gm.isGameActive() && gm.getRoleManager().getRole(attackerPlayer) == Role.MURDERER) {
                if (target instanceof ServerPlayerEntity victim) {
                    // Vérifier que la victime est en jeu et vivante
                    if (gm.getRoleManager().isAlive(victim)) {
                        if (cfg.knifeOneShot) {
                            // One-shot
                            victim.damage(victim.getDamageSources().playerAttack(attackerPlayer), Float.MAX_VALUE);
                        }
                        // Combo de sons pour un "stab" plus brutal
                        playKnifeSound(attackerPlayer);
                        // Particules de sang à l'impact (visible par tous)
                        if (victim.getServerWorld() != null) {
                            victim.getServerWorld().spawnParticles(
                                    net.minecraft.particle.ParticleTypes.DAMAGE_INDICATOR,
                                    victim.getX(), victim.getY() + 1.0, victim.getZ(),
                                    20, 0.3, 0.5, 0.3, 0.1);
                        }
                    }
                }
            } else {
                // Le joueur n'est pas meurtrier — empêcher l'attaque
                attackerPlayer.sendMessage(Text.translatable("murdercraft.knife.not_murderer")
                        .formatted(net.minecraft.util.Formatting.RED), true);
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

    /** Combo de sons "stab" — utilisé pour tous les kills au couteau. */
    private void playKnifeSound(ServerPlayerEntity attacker) {
        attacker.playSoundToPlayer(SoundEvents.ITEM_TRIDENT_HIT, attacker.getSoundCategory(), 0.5f, 1.4f);
        attacker.playSoundToPlayer(SoundEvents.BLOCK_WOOL_BREAK, attacker.getSoundCategory(), 0.6f, 0.7f);
    }
}
