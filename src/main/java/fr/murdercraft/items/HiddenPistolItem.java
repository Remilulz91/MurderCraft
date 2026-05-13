package fr.murdercraft.items;

import fr.murdercraft.game.GameManager;
import fr.murdercraft.roles.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Pistolet caché : ramassé au sol par un INNOCENT, le promeut en DETECTIVE.
 * Quand ramassé, il se transforme automatiquement en PISTOL standard et
 * le joueur change de rôle.
 *
 * Hérite de PistolItem pour réutiliser la logique de tir si jamais utilisé.
 */
public class HiddenPistolItem extends PistolItem {

    public HiddenPistolItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        // Si quelqu'un l'utilise sans avoir été promu (shouldn't happen, but safety)
        if (player instanceof ServerPlayerEntity sp) {
            GameManager gm = GameManager.get();
            Role role = gm.getRoleManager().getRole(sp);
            if (role == Role.INNOCENT) {
                gm.promoteToDetective(sp);
                sp.sendMessage(Text.translatable("murdercraft.hidden_pistol.promoted")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                return TypedActionResult.success(player.getStackInHand(hand));
            }
        }
        return super.use(world, player, hand);
    }
}
