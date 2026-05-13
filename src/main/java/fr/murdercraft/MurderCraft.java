package fr.murdercraft;

import fr.murdercraft.commands.MurderCommand;
import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.events.CombatEventHandler;
import fr.murdercraft.events.PlayerEventHandler;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.items.ModItems;
import fr.murdercraft.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée principal de MurderCraft.
 * Initialise tous les systèmes côté serveur et commun.
 */
public class MurderCraft implements ModInitializer {

    public static final String MOD_ID = "murdercraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("MurderCraft");

    @Override
    public void onInitialize() {
        LOGGER.info("==============================================");
        LOGGER.info("    MurderCraft - Démarrage du mod");
        LOGGER.info("==============================================");

        // 1. Charger la configuration
        MurderCraftConfig.load();
        LOGGER.info("[MurderCraft] Configuration chargée");

        // 2. Enregistrer les items custom (couteau, pistolet)
        ModItems.register();
        LOGGER.info("[MurderCraft] Items enregistrés");

        // 3. Enregistrer les packets réseau
        ModNetworking.registerServer();
        LOGGER.info("[MurderCraft] Réseau initialisé");

        // 4. Initialiser le gestionnaire de parties
        GameManager.initialize();
        LOGGER.info("[MurderCraft] GameManager initialisé");

        // 5. Enregistrer les commandes /murder
        CommandRegistrationCallback.EVENT.register(MurderCommand::register);
        LOGGER.info("[MurderCraft] Commandes enregistrées");

        // 6. Enregistrer les événements
        PlayerEventHandler.register();
        CombatEventHandler.register();
        LOGGER.info("[MurderCraft] Événements enregistrés");

        LOGGER.info("[MurderCraft] Mod chargé avec succès !");
    }

    /**
     * Crée un Identifier dans le namespace du mod.
     */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
