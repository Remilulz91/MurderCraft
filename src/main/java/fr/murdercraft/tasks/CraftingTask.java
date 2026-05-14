package fr.murdercraft.tasks;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.game.GameManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Tâche CRAFTING : un item-cible est annoncé. Premier joueur à l'avoir dans son
 * inventaire gagne. Pour éviter qu'un joueur "triche" en l'ayant déjà, on prend
 * un snapshot du compte au début, et on vérifie l'augmentation.
 */
public class CraftingTask implements Task {

    private static final Random RANDOM = new Random();

    /** Items possibles à crafter (recettes faciles et accessibles). */
    private static final Item[] POSSIBLE_TARGETS = {
            Items.TORCH,
            Items.LADDER,
            Items.BREAD,
            Items.BOOKSHELF,
            Items.CRAFTING_TABLE,
            Items.CHEST,
            Items.WOODEN_SWORD,
            Items.BOWL,
            Items.STICK,
            Items.PAPER,
    };

    private Item target;
    private final Map<UUID, Integer> initialCounts = new HashMap<>();
    private boolean completed = false;

    @Override
    public String getId() {
        return "crafting";
    }

    @Override
    public Text getDescription() {
        return Text.translatable("murdercraft.task.crafting.description",
                Text.translatable(target.getTranslationKey()));
    }

    @Override
    public void onStart(MinecraftServer server) {
        target = POSSIBLE_TARGETS[RANDOM.nextInt(POSSIBLE_TARGETS.length)];

        // Snapshot du compte initial pour chaque participant
        for (UUID id : GameManager.get().getParticipants()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) {
                initialCounts.put(id, countItem(p, target));
            }
        }

        MurderCraft.LOGGER.info("[CraftingTask] Item cible : {}", target);
    }

    @Override
    public UUID checkCompletion(MinecraftServer server) {
        if (completed || target == null) return null;
        for (UUID id : GameManager.get().getParticipants()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null) continue;
            int now = countItem(p, target);
            int before = initialCounts.getOrDefault(id, 0);
            if (now > before) {
                completed = true;
                return id;
            }
        }
        return null;
    }

    @Override
    public void cleanup(MinecraftServer server) {
        initialCounts.clear();
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    private int countItem(ServerPlayerEntity p, Item item) {
        int total = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(item)) {
                total += s.getCount();
            }
        }
        return total;
    }
}
