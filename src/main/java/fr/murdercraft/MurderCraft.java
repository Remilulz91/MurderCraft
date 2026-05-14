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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Main entry point of MurderCraft.
 * Initializes all server-side and common-side systems.
 */
public class MurderCraft implements ModInitializer {

    public static final String MOD_ID = "murdercraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("MurderCraft");

    // === Build type detection (set by Gradle's processResources) ===
    private static final boolean IS_DEBUG_BUILD;
    static {
        boolean debug = false;
        try (InputStream is = MurderCraft.class.getResourceAsStream("/murdercraft.build.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                debug = "debug".equalsIgnoreCase(p.getProperty("build.type", "public").trim());
            }
        } catch (IOException ignored) { }
        IS_DEBUG_BUILD = debug;
    }

    /** Returns true if this JAR was built as the debug variant. */
    public static boolean isDebugBuild() {
        return IS_DEBUG_BUILD;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("==============================================");
        LOGGER.info("    MurderCraft - Starting up ({} build)",
                IS_DEBUG_BUILD ? "DEBUG" : "PUBLIC");
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
