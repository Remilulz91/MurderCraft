package fr.murdercraft.items;

import fr.murdercraft.MurderCraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

/**
 * Enregistre tous les items custom du mod.
 */
public class ModItems {

    /** Couteau du meurtrier — un coup = kill. */
    public static final Item KNIFE = register(
            "knife",
            new KnifeItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.RARE)
                    .fireproof())
    );

    /** Pistolet du justicier — tir à distance, perd si tue innocent. */
    public static final Item PISTOL = register(
            "pistol",
            new PistolItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof())
    );

    /** Pistolet caché (drop pour innocent qui le récupère => devient Detective). */
    public static final Item HIDDEN_PISTOL = register(
            "hidden_pistol",
            new HiddenPistolItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof())
    );

    /** Mystery Token : item de récompense des tâches (donne le droit à un indice). */
    public static final Item MYSTERY_TOKEN = register(
            "mystery_token",
            new Item(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof())
    );

    private static Item register(String id, Item item) {
        return Registry.register(
                Registries.ITEM,
                MurderCraft.id(id),
                item
        );
    }

    public static void register() {
        MurderCraft.LOGGER.info("[ModItems] {} items enregistrés", 3);

        // Ajouter au groupe d'items "Outils & Utilitaires" pour le mode créatif
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(KNIFE);
            entries.add(PISTOL);
            entries.add(HIDDEN_PISTOL);
            entries.add(MYSTERY_TOKEN);
        });
    }
}
