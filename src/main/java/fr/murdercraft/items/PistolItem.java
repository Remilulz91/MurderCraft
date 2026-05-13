package fr.murdercraft.items;

import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.roles.Role;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/**
 * Pistolet du Justicier.
 * - Clic droit = tirer dans la direction visée
 * - Si le tir touche un MURDERER : kill validé
 * - Si le tir touche un INNOCENT : tueur perd définitivement son arme
 */
public class PistolItem extends Item {

    /** Portée maximale d'un tir (en blocs). */
    private static final double MAX_RANGE = 64.0;
    /** Cooldown entre deux tirs (en ticks). */
    private static final int COOLDOWN_TICKS = 20;

    public PistolItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!(player instanceof ServerPlayerEntity shooter)) {
            return TypedActionResult.success(stack, world.isClient());
        }

        GameManager gm = GameManager.get();
        boolean debugMobDamage = MurderCraftConfig.get().debugAllowMobDamage;

        // En mode debug avec mob damage, on autorise le tir même hors partie
        // (utile pour valider l'animation et le raycast en solo)
        if (!gm.isGameActive() && !debugMobDamage) {
            shooter.sendMessage(Text.translatable("murdercraft.pistol.no_game").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        if (gm.isGameActive()) {
            Role role = gm.getRoleManager().getRole(shooter);
            if (role != Role.DETECTIVE && !debugMobDamage) {
                shooter.sendMessage(Text.translatable("murdercraft.pistol.not_detective").formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }
        }

        // Effets visuels et sonores
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, shooter.getSoundCategory(), 1.0f, 1.5f);

        // Raycast (joueurs + mobs si debug)
        Entity hit = raycast(world, shooter, debugMobDamage);

        if (hit instanceof ServerPlayerEntity victim) {
            if (gm.isGameActive()) {
                handleHit(shooter, victim, gm);
            }
        } else if (hit instanceof LivingEntity mob && debugMobDamage) {
            handleMobHit(shooter, mob);
        }

        // Cooldown
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        return TypedActionResult.success(stack);
    }

    /** [DEBUG] Tue un mob d'un coup avec effets — pour tester en solo. */
    private void handleMobHit(ServerPlayerEntity shooter, LivingEntity mob) {
        if (shooter.getServerWorld() != null) {
            shooter.getServerWorld().spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                    mob.getX(), mob.getY() + 1.0, mob.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
        }
        mob.damage(mob.getDamageSources().playerAttack(shooter), Float.MAX_VALUE);
        shooter.sendMessage(Text.translatable("murdercraft.debug.mob_hit", mob.getName())
                .formatted(Formatting.LIGHT_PURPLE), true);
    }

    private void handleHit(ServerPlayerEntity shooter, ServerPlayerEntity victim, GameManager gm) {
        if (!gm.getRoleManager().isAlive(victim)) return;

        Role victimRole = gm.getRoleManager().getRole(victim);

        // Effet de particules sanguinolentes
        if (shooter.getServerWorld() != null) {
            shooter.getServerWorld().spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                    victim.getX(), victim.getY() + 1.0, victim.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
        }

        // Kill
        victim.damage(victim.getDamageSources().playerAttack(shooter), Float.MAX_VALUE);

        if (victimRole == Role.MURDERER) {
            // Bon tir
            shooter.sendMessage(Text.translatable("murdercraft.pistol.hit_murderer", victim.getName())
                    .formatted(Formatting.GREEN), false);
        } else {
            // Tir sur innocent ou autre — perte du pistolet
            if (MurderCraftConfig.get().detectiveLosesGunOnFriendlyFire) {
                shooter.getInventory().remove(s -> s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL),
                        Integer.MAX_VALUE, shooter.getInventory());
                gm.getRoleManager().markPermanentlyDisarmed(shooter.getUuid());
                shooter.sendMessage(Text.translatable("murdercraft.pistol.friendly_fire")
                        .formatted(Formatting.RED, Formatting.BOLD), false);
            }
        }
    }

    /**
     * Effectue un raycast — par défaut sur les joueurs uniquement.
     * Si includeMobs=true (mode debug), prend aussi en compte tous les LivingEntity.
     */
    private Entity raycast(World world, ServerPlayerEntity shooter, boolean includeMobs) {
        Vec3d origin = shooter.getCameraPosVec(1.0f);
        Vec3d direction = shooter.getRotationVec(1.0f);
        Vec3d end = origin.add(direction.multiply(MAX_RANGE));

        // 1) Vérifier les blocs (pour bloquer le tir)
        HitResult blockHit = world.raycast(new RaycastContext(origin, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, shooter));
        double maxDistSq = blockHit.getType() == HitResult.Type.MISS
                ? origin.squaredDistanceTo(end)
                : origin.squaredDistanceTo(blockHit.getPos());

        // 2) Trouver l'entité cible la plus proche dans la trajectoire
        List<? extends LivingEntity> nearby;
        if (includeMobs) {
            nearby = world.getEntitiesByClass(LivingEntity.class,
                    shooter.getBoundingBox().expand(MAX_RANGE),
                    e -> e != shooter && e.isAlive());
        } else {
            nearby = world.getEntitiesByType(EntityType.PLAYER,
                    shooter.getBoundingBox().expand(MAX_RANGE),
                    p -> p != shooter && p.isAlive());
        }

        Entity closest = null;
        double closestDistSq = maxDistSq;

        for (LivingEntity e : nearby) {
            net.minecraft.util.math.Box box = e.getBoundingBox().expand(0.3);
            var optional = box.raycast(origin, end);
            if (optional.isPresent()) {
                double distSq = origin.squaredDistanceTo(optional.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = e;
                }
            }
        }

        return closest;
    }

    @Override
    public boolean canMine(net.minecraft.block.BlockState state, World world, net.minecraft.util.math.BlockPos pos, PlayerEntity miner) {
        return false;
    }
}
