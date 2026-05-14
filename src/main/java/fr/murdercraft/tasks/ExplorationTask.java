package fr.murdercraft.tasks;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.items.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Tâche EXPLORATION : un coffre lumineux apparaît à proximité aléatoire d'un joueur,
 * contenant un Mystery Token. Le premier joueur qui ramasse le token gagne la tâche.
 *
 * - Distance : 15-40 blocs d'un joueur aléatoire
 * - Le coffre est marqué visuellement (un block_marker / particles)
 * - Le token est aussi posé en plus du coffre (au cas où le coffre est cassé)
 */
public class ExplorationTask implements Task {

    private static final Random RANDOM = new Random();
    private boolean completed = false;
    private BlockPos chestPos = null;
    private ItemEntity tokenEntity = null;

    @Override
    public String getId() {
        return "exploration";
    }

    @Override
    public Text getDescription() {
        return Text.translatable("murdercraft.task.exploration.description");
    }

    @Override
    public void onStart(MinecraftServer server) {
        // Choisit un joueur participant au hasard pour décider de la zone de spawn
        List<ServerPlayerEntity> participants = new ArrayList<>();
        for (UUID id : GameManager.get().getParticipants()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) participants.add(p);
        }
        if (participants.isEmpty()) return;

        ServerPlayerEntity anchor = participants.get(RANDOM.nextInt(participants.size()));
        ServerWorld world = anchor.getServerWorld();
        if (world == null) return;

        // Cherche une position valide (15-40 blocs autour, sol solide)
        BlockPos pos = findSpawnPosition(world, anchor.getBlockPos(), 15, 40);
        if (pos == null) {
            MurderCraft.LOGGER.warn("[ExplorationTask] Aucune position valide trouvée");
            return;
        }

        // Place un coffre + remplit avec le mystery token
        world.setBlockState(pos, Blocks.CHEST.getDefaultState());
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setStack(13, new ItemStack(ModItems.MYSTERY_TOKEN)); // slot central
        }
        chestPos = pos;

        // Spawn aussi un token au-dessus pour la visibilité (et au cas où le coffre soit cassé)
        ItemStack stack = new ItemStack(ModItems.MYSTERY_TOKEN);
        ItemEntity drop = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, stack);
        drop.setNeverDespawn();
        drop.setGlowing(true);
        world.spawnEntity(drop);
        tokenEntity = drop;

        MurderCraft.LOGGER.info("[ExplorationTask] Coffre + token spawnés en {}, {}, {}",
                pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public UUID checkCompletion(MinecraftServer server) {
        if (completed) return null;
        // Vérifie si un participant a le token dans son inventaire
        for (UUID id : GameManager.get().getParticipants()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null) continue;
            if (p.getInventory().containsAny(s -> s.isOf(ModItems.MYSTERY_TOKEN))) {
                // Retire le token (consommé par la complétion)
                p.getInventory().remove(s -> s.isOf(ModItems.MYSTERY_TOKEN), Integer.MAX_VALUE, p.getInventory());
                completed = true;
                return id;
            }
        }
        return null;
    }

    @Override
    public void cleanup(MinecraftServer server) {
        // Supprime le coffre et le token éventuels (nettoyage)
        if (chestPos != null && server != null) {
            ServerWorld world = server.getOverworld();
            if (world != null && world.getBlockState(chestPos).isOf(Blocks.CHEST)) {
                world.setBlockState(chestPos, Blocks.AIR.getDefaultState());
            }
        }
        if (tokenEntity != null && !tokenEntity.isRemoved()) {
            tokenEntity.discard();
        }
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    /** Trouve une position de surface dans un rayon donné. */
    private BlockPos findSpawnPosition(ServerWorld world, BlockPos origin, int minR, int maxR) {
        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            int dist = minR + RANDOM.nextInt(maxR - minR);
            int dx = (int) (Math.cos(angle) * dist);
            int dz = (int) (Math.sin(angle) * dist);
            BlockPos candidate = world.getTopPosition(
                    net.minecraft.world.Heightmap.Type.WORLD_SURFACE,
                    origin.add(dx, 0, dz));
            if (world.getBlockState(candidate.down()).isSolid()
                    && world.getBlockState(candidate).isAir()
                    && world.getBlockState(candidate.up()).isAir()) {
                return candidate;
            }
        }
        return null;
    }
}
