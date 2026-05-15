package fr.murdercraft.items;

import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.roles.Role;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
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

    /** Nombre maximum de balles dans un pistolet (règle Squeezie). */
    public static final int MAX_AMMO = 2;
    /** Clé NBT pour stocker les balles restantes. */
    private static final String NBT_KEY_AMMO = "murdercraft_ammo";

    public PistolItem(Settings settings) {
        super(settings);
    }

    // === Munitions (persistées via DataComponentTypes.CUSTOM_DATA) ===

    /** Lit le nombre de balles restantes dans ce pistolet. Défaut : MAX_AMMO si jamais initialisé. */
    public static int getAmmo(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return MAX_AMMO;
        NbtCompound nbt = custom.copyNbt();
        if (nbt.contains(NBT_KEY_AMMO)) {
            return nbt.getInt(NBT_KEY_AMMO);
        }
        return MAX_AMMO;
    }

    /** Modifie le nombre de balles. Sauve dans le DataComponent. */
    public static void setAmmo(ItemStack stack, int ammo) {
        NbtComponent existing = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = existing.copyNbt();
        nbt.putInt(NBT_KEY_AMMO, Math.max(0, Math.min(MAX_AMMO, ammo)));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /** Crée un ItemStack avec un nombre de balles donné. */
    public static ItemStack createWithAmmo(net.minecraft.item.Item item, int ammo) {
        ItemStack s = new ItemStack(item);
        setAmmo(s, ammo);
        return s;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        int ammo = getAmmo(stack);
        tooltip.add(Text.translatable("murdercraft.pistol.tooltip.bullets", ammo, MAX_AMMO)
                .formatted(ammo > 0 ? Formatting.GREEN : Formatting.DARK_RED));
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

        // Check munitions (règle Squeezie : 2 balles max par pistolet)
        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            shooter.sendMessage(Text.translatable("murdercraft.pistol.empty").formatted(Formatting.RED, Formatting.BOLD), true);
            shooter.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, shooter.getSoundCategory(), 0.5f, 1.5f);
            player.getItemCooldownManager().set(this, 10); // petit cooldown anti-spam
            return TypedActionResult.fail(stack);
        }
        // Décrément AVANT le tir (un tir consomme une balle quoi qu'il arrive)
        setAmmo(stack, ammo - 1);
        int remainingAmmo = ammo - 1;

        // Effets visuels et sonores — combo de 2 sons pour un meilleur "bang"
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), shooter.getSoundCategory(), 0.4f, 2.0f);
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, shooter.getSoundCategory(), 1.0f, 1.2f);

        // Muzzle flash : particules de fumée devant le shooter
        if (shooter.getServerWorld() != null) {
            Vec3d eye = shooter.getCameraPosVec(1.0f);
            Vec3d dir = shooter.getRotationVec(1.0f);
            Vec3d muzzle = eye.add(dir.multiply(0.6)).add(0, -0.2, 0);
            shooter.getServerWorld().spawnParticles(ParticleTypes.SMOKE,
                    muzzle.x, muzzle.y, muzzle.z, 8, 0.05, 0.05, 0.05, 0.05);
            shooter.getServerWorld().spawnParticles(ParticleTypes.FLAME,
                    muzzle.x, muzzle.y, muzzle.z, 3, 0.02, 0.02, 0.02, 0.02);
        }

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

        // Affiche les balles restantes en action bar (au-dessus de l'hotbar)
        Formatting ammoColor = remainingAmmo > 0 ? Formatting.YELLOW : Formatting.DARK_RED;
        shooter.sendMessage(Text.translatable("murdercraft.pistol.bullets_remaining", remainingAmmo, MAX_AMMO)
                .formatted(ammoColor), true);

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
            // Bon tir — feedback fort
            shooter.sendMessage(Text.translatable("murdercraft.pistol.hit_murderer", victim.getName())
                    .formatted(Formatting.GREEN), false);
            shooter.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, shooter.getSoundCategory(), 0.6f, 1.5f);
        } else {
            // Tir sur innocent ou autre — le pistolet tombe au sol (ramassable par innocents seulement)
            if (MurderCraftConfig.get().detectiveLosesGunOnFriendlyFire) {
                // Récupérer les balles restantes du pistolet du shooter pour préserver le compteur
                int currentAmmo = MAX_AMMO;
                for (int i = 0; i < shooter.getInventory().size(); i++) {
                    ItemStack s = shooter.getInventory().getStack(i);
                    if (s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL)) {
                        currentAmmo = getAmmo(s);
                        break;
                    }
                }

                shooter.getInventory().remove(s -> s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL),
                        Integer.MAX_VALUE, shooter.getInventory());
                gm.getRoleManager().markPermanentlyDisarmed(shooter.getUuid());
                // Le justicier redevient innocent (perte du rôle)
                gm.getRoleManager().setRole(shooter.getUuid(), Role.INNOCENT);
                // Drop le pistolet au sol avec ses balles restantes
                gm.dropHiddenPistolAt(shooter, currentAmmo);
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
