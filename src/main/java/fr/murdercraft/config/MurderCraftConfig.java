package fr.murdercraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.murdercraft.MurderCraft;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration centrale de MurderCraft.
 * Sauvegardée dans config/murdercraft.json.
 *
 * Toutes les valeurs peuvent être modifiées via :
 * - L'écran de config in-game (Mod Menu + Cloth Config)
 * - La commande /murder config set <option> <valeur>
 * - Directement dans le fichier config/murdercraft.json
 */
public class MurderCraftConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("murdercraft.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static MurderCraftConfig INSTANCE = new MurderCraftConfig();

    // === Paramètres de la partie ===

    /** Nombre minimum de joueurs requis pour lancer une partie. */
    public int minPlayers = 5;

    /** Nombre maximum de joueurs par partie (0 = illimité). */
    public int maxPlayers = 16;

    /** Durée d'une partie en secondes (par défaut : 8 minutes). */
    public int gameDurationSeconds = 480;

    /** Durée de la phase de préparation au début (countdown) en secondes. */
    public int startCountdownSeconds = 10;

    /** Délai avant le spawn du 2ème pistolet caché (en secondes depuis le début). */
    public int hiddenPistolSpawnDelaySeconds = 60;

    // === Rôles ===

    /** Nombre de meurtriers par partie. */
    public int murdererCount = 2;

    /** Nombre de justiciers par partie (au début). */
    public int detectiveCount = 1;

    // === Comportement gameplay ===

    /** Un coup de couteau tue en un coup ? */
    public boolean knifeOneShot = true;

    /** Le justicier perd-il définitivement son pistolet s'il tue un innocent ? */
    public boolean detectiveLosesGunOnFriendlyFire = true;

    /** Afficher les noms des joueurs au-dessus de leur tête pendant la partie ? */
    public boolean showPlayerNamesIngame = false;

    /** Téléporter les joueurs au lobby à la fin de la partie ? */
    public boolean teleportToLobbyAfterGame = false;

    /** Coordonnées du lobby (x, y, z) si teleportToLobbyAfterGame est activé. */
    public double[] lobbyCoordinates = {0.0, 64.0, 0.0};

    // === Interface ===

    /** Afficher le HUD personnalisé (rôle, timer, etc.) ? */
    public boolean showCustomHud = true;

    /** Afficher les sous-titres et indices contextuels ? */
    public boolean showSubtitles = true;

    // === DEBUG (à désactiver pour les releases publiques) ===

    /**
     * [DEBUG] Permet aux armes (couteau, pistolet) d'infliger des dégâts létaux
     * aux mobs (et pas uniquement aux joueurs). Utile pour tester le comportement
     * en solo. À DÉSACTIVER pour les vraies parties.
     */
    public boolean debugAllowMobDamage = false;

    // === Méthodes ===

    public static MurderCraftConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                MurderCraftConfig loaded = GSON.fromJson(json, MurderCraftConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
                MurderCraft.LOGGER.info("[Config] Configuration chargée depuis {}", CONFIG_PATH);
            } else {
                save();
                MurderCraft.LOGGER.info("[Config] Configuration par défaut créée");
            }
        } catch (IOException e) {
            MurderCraft.LOGGER.error("[Config] Erreur lors du chargement : {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            MurderCraft.LOGGER.error("[Config] Erreur lors de la sauvegarde : {}", e.getMessage());
        }
    }

    /**
     * Valide la cohérence des paramètres et corrige si besoin.
     * Appelé au démarrage d'une partie.
     */
    public boolean validate() {
        int minRequired = murdererCount + detectiveCount + 1; // au moins 1 innocent
        if (minPlayers < minRequired) {
            MurderCraft.LOGGER.warn("[Config] minPlayers ({}) < requis ({}), correction automatique", minPlayers, minRequired);
            minPlayers = minRequired;
            save();
        }
        return true;
    }
}
