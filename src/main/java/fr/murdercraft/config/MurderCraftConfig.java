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
    public int maxPlayers = 32;

    /** Durée d'une manche en secondes (par défaut : 15 minutes — comme GMod Murder). */
    public int gameDurationSeconds = 900;

    /** Durée de la phase de préparation au début (countdown) en secondes. */
    public int startCountdownSeconds = 10;

    /** Délai avant le spawn du 2ème pistolet caché (en secondes depuis le début). */
    public int hiddenPistolSpawnDelaySeconds = 60;

    /** Nombre de manches dans une session (par défaut : 4 — comme GMod Murder). */
    public int maxRounds = 4;

    /** Délai entre deux manches en secondes (pour pause + transition). */
    public int interRoundSeconds = 15;

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

    // === Spawn aléatoire au début de chaque manche ===

    /** Activer la téléportation aléatoire des joueurs au début de chaque manche. */
    public boolean randomSpawnEnabled = true;

    /** Rayon de la zone de spawn aléatoire (en blocs, depuis le world spawn). */
    public int randomSpawnRadius = 200;

    /** Hauteur supplémentaire au-dessus du sol pour le spawn (pour les biomes vallonnés). */
    public int randomSpawnHeight = 25;

    /** Durée de l'immunité aux dégâts après spawn aléatoire (en secondes). */
    public int spawnImmunitySeconds = 8;

    // === World border de partie ===

    /** Activer la world border de partie (centrée sur /setworldspawn). */
    public boolean useWorldBorder = true;

    /** Taille de la world border en blocs (côté du carré). */
    public int worldBorderSize = 500;

    // === Interface ===

    /** Afficher le HUD personnalisé (rôle, timer, etc.) ? */
    public boolean showCustomHud = true;

    /** Afficher les sous-titres et indices contextuels ? */
    public boolean showSubtitles = true;

    // === DEBUG (disabled by default in public builds, enabled in debug builds) ===

    /**
     * [DEBUG] Allows weapons (knife, pistol) to deal lethal damage to mobs
     * (not just players). Useful for testing in solo. Disable for real matches.
     * Default depends on build type (public = false, debug = true).
     */
    public boolean debugAllowMobDamage = MurderCraft.isDebugBuild();

    /**
     * [DEBUG] Enables /murder debug ... commands. DISABLE for production servers
     * — otherwise any OP could cheat with debug commands.
     * Default depends on build type (public = false, debug = true).
     */
    public boolean enableDebugCommands = MurderCraft.isDebugBuild();

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
                MurderCraft.LOGGER.info("[Config] Configuration loaded from {}", CONFIG_PATH);
            } else {
                save();
                MurderCraft.LOGGER.info("[Config] Default configuration created");
            }
        } catch (IOException e) {
            MurderCraft.LOGGER.error("[Config] Loading error: {}", e.getMessage());
        }

        // SECURITY: in PUBLIC builds, debug flags are FORCED to false at runtime
        // regardless of what the config file contains. This prevents anyone from
        // bypassing the public/debug build distinction by editing the JSON.
        // To use debug features, the DEBUG build must be installed instead.
        if (!MurderCraft.isDebugBuild()) {
            boolean wasModified = false;
            if (INSTANCE.enableDebugCommands) {
                INSTANCE.enableDebugCommands = false;
                wasModified = true;
            }
            if (INSTANCE.debugAllowMobDamage) {
                INSTANCE.debugAllowMobDamage = false;
                wasModified = true;
            }
            if (wasModified) {
                MurderCraft.LOGGER.warn("[Config] ⚠ Debug flags found in config file but this is a PUBLIC build —");
                MurderCraft.LOGGER.warn("[Config] ⚠ they are IGNORED. To use debug features, install the DEBUG build.");
            }
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
