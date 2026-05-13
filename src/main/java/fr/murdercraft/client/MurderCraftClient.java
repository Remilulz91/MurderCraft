package fr.murdercraft.client;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.client.hud.MurderHud;
import fr.murdercraft.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;

/**
 * Point d'entrée du mod côté client.
 * Initialise le HUD, les écrans GUI et les handlers réseau client.
 */
public class MurderCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MurderCraft.LOGGER.info("[MurderCraft] Initialisation côté client...");

        // Enregistrer les handlers réseau côté client
        ModNetworking.registerClient();

        // Initialiser le HUD (overlay rôle, timer, etc.)
        MurderHud.register();

        MurderCraft.LOGGER.info("[MurderCraft] Client initialisé avec succès !");
    }
}
