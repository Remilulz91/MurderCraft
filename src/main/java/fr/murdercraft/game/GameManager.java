package fr.murdercraft.game;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.items.ModItems;
import fr.murdercraft.items.PistolItem;
import fr.murdercraft.network.ModNetworking;
import fr.murdercraft.roles.Role;
import fr.murdercraft.roles.RoleManager;
import fr.murdercraft.tasks.TaskManager;
import fr.murdercraft.util.TitleUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.Heightmap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Gestionnaire de partie : singleton côté serveur.
 *
 * Maintient l'état global :
 *   - Phase actuelle (LOBBY, STARTING, IN_GAME, ENDING)
 *   - Joueurs inscrits
 *   - Rôles via RoleManager
 *   - Timer
 *   - Conditions de victoire
 */
public class GameManager {

    private static final GameManager INSTANCE = new GameManager();

    public static GameManager get() {
        return INSTANCE;
    }

    public static void initialize() {
        // Tick chaque serveur pour gérer le timer & les conditions de victoire
        ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onServerTick);
    }

    // === État ===
    private GamePhase phase = GamePhase.LOBBY;
    private MinecraftServer server;
    private final Set<UUID> participants = new HashSet<>();
    private final RoleManager roleManager = new RoleManager();
    private int tickCounter = 0;
    private int countdownTicksLeft = 0;
    private int gameTicksLeft = 0;
    private boolean hiddenPistolSpawned = false;
    private long startedAt = 0;
    /** Si true, les conditions de victoire ne sont pas vérifiées (mode test). */
    private boolean debugMode = false;

    // === Session multi-manches ===
    private int currentRound = 0;
    private int sessionMurdererWins = 0;
    private int sessionInnocentWins = 0;
    private boolean inSession = false;
    private boolean awaitingNextRound = false;

    /** Cooldown anti-spam pour l'éjection de pistolet (par joueur). */
    private final Map<UUID, Long> lastPistolEjection = new HashMap<>();
    private static final long EJECTION_COOLDOWN_MS = 3000;

    /** Expiration de l'immunité aux dégâts après spawn (UUID → timestamp ms). */
    private final Map<UUID, Long> safeSpawnExpiration = new HashMap<>();

    /** Sauvegarde de la world border pour restauration en fin de session. */
    private double savedBorderCenterX = 0;
    private double savedBorderCenterZ = 0;
    private double savedBorderSize = 0;
    private boolean borderWasModified = false;

    public int getCurrentRound() {
        return currentRound;
    }

    public int getSessionMurdererWins() {
        return sessionMurdererWins;
    }

    public int getSessionInnocentWins() {
        return sessionInnocentWins;
    }

    public boolean isInSession() {
        return inSession;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public boolean isGameActive() {
        return phase == GamePhase.IN_GAME;
    }

    public int getGameTicksLeft() {
        return gameTicksLeft;
    }

    public int getCountdownTicksLeft() {
        return countdownTicksLeft;
    }

    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    /** Inscrit un joueur dans la partie (lobby). */
    public boolean addParticipant(ServerPlayerEntity player) {
        if (phase != GamePhase.LOBBY) return false;
        if (this.server == null) this.server = player.getServer();
        boolean added = participants.add(player.getUuid());
        if (added) {
            broadcast(Text.translatable("murdercraft.lobby.joined", player.getName().getString())
                    .formatted(Formatting.GREEN));
        }
        return added;
    }

    public boolean removeParticipant(ServerPlayerEntity player) {
        boolean removed = participants.remove(player.getUuid());
        if (removed && phase == GamePhase.LOBBY) {
            broadcast(Text.translatable("murdercraft.lobby.left", player.getName().getString())
                    .formatted(Formatting.YELLOW));
        }
        return removed;
    }

    /**
     * Démarre une nouvelle session de plusieurs manches (4 par défaut).
     * Vérifie le nombre min de joueurs et lance la première manche.
     */
    public boolean startGame(MinecraftServer server) {
        if (phase != GamePhase.LOBBY) {
            return false;
        }

        MurderCraftConfig cfg = MurderCraftConfig.get();
        cfg.validate();

        if (participants.size() < cfg.minPlayers) {
            broadcast(Text.translatable("murdercraft.error.not_enough_players",
                    participants.size(), cfg.minPlayers).formatted(Formatting.RED));
            return false;
        }

        this.server = server;

        // Initialise la session
        this.inSession = true;
        this.currentRound = 0;
        this.sessionMurdererWins = 0;
        this.sessionInnocentWins = 0;
        this.awaitingNextRound = false;

        broadcast(Text.translatable("murdercraft.session.started", cfg.maxRounds)
                .formatted(Formatting.GOLD, Formatting.BOLD));

        // Configure la world border de partie
        setupWorldBorder();

        return startNextRound();
    }

    /** Démarre la manche suivante dans la session. */
    private boolean startNextRound() {
        MurderCraftConfig cfg = MurderCraftConfig.get();
        currentRound++;

        if (currentRound > cfg.maxRounds) {
            endSession();
            return false;
        }

        this.phase = GamePhase.STARTING;
        this.countdownTicksLeft = cfg.startCountdownSeconds * 20;
        this.hiddenPistolSpawned = false;
        this.awaitingNextRound = false;

        broadcast(Text.translatable("murdercraft.round.starting", currentRound, cfg.maxRounds)
                .formatted(Formatting.GOLD, Formatting.BOLD));

        // Title à tous : "Round X/Y"
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                TitleUtil.sendTitle(p,
                        Text.translatable("murdercraft.hud.round", currentRound, cfg.maxRounds)
                                .formatted(Formatting.GOLD, Formatting.BOLD),
                        Text.translatable("murdercraft.round.preparing").formatted(Formatting.YELLOW),
                        10, 50, 15);
            }
        }
        return true;
    }

    /** Termine la session en cours et affiche le vainqueur final. */
    private void endSession() {
        broadcast(Text.empty());
        broadcast(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD));
        broadcast(Text.translatable("murdercraft.session.over").formatted(Formatting.GOLD, Formatting.BOLD));
        broadcast(Text.translatable("murdercraft.session.score",
                sessionInnocentWins, sessionMurdererWins).formatted(Formatting.WHITE));

        Text winnerText;
        if (sessionInnocentWins > sessionMurdererWins) {
            winnerText = Text.translatable("murdercraft.win.innocents").formatted(Formatting.GREEN, Formatting.BOLD);
        } else if (sessionMurdererWins > sessionInnocentWins) {
            winnerText = Text.translatable("murdercraft.win.murderers").formatted(Formatting.DARK_RED, Formatting.BOLD);
        } else {
            winnerText = Text.translatable("murdercraft.win.draw").formatted(Formatting.YELLOW, Formatting.BOLD);
        }
        broadcast(Text.translatable("murdercraft.session.final_winner").append(winnerText));
        broadcast(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD));
        broadcast(Text.empty());

        this.inSession = false;
        this.tickCounter = 200; // 10s avant retour lobby complet
        this.phase = GamePhase.ENDING;
        this.awaitingNextRound = false;
    }

    /**
     * Force l'arrêt d'une partie en cours — RESET IMMÉDIAT vers le lobby.
     * Contrairement à un endGame naturel, on ne fait pas le countdown de 10s.
     */
    public void stopGame(boolean canceled) {
        if (phase == GamePhase.LOBBY) return;

        WinResult result = canceled ? WinResult.CANCELED : WinResult.DRAW;
        broadcast(Text.empty());
        broadcast(Text.literal("═══════════════════════").formatted(Formatting.GOLD));
        broadcast(result.getDisplayText());
        broadcast(Text.literal("═══════════════════════").formatted(Formatting.GOLD));
        broadcast(Text.empty());

        // Reset immédiat (pas de countdown ENDING)
        resetToLobby();
    }

    private List<ServerPlayerEntity> resolveParticipants() {
        List<ServerPlayerEntity> result = new ArrayList<>();
        if (server == null) return result;
        for (UUID id : participants) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) result.add(p);
        }
        return result;
    }

    private void beginInGame() {
        List<ServerPlayerEntity> players = resolveParticipants();
        roleManager.assignRoles(players);
        finalizeStart(players);
    }

    /**
     * Finalise le démarrage : timer + distribution items + briefing.
     * Extrait pour pouvoir être appelé après une assignation custom (debug).
     */
    private void finalizeStart(List<ServerPlayerEntity> players) {
        MurderCraftConfig cfg = MurderCraftConfig.get();
        this.gameTicksLeft = cfg.gameDurationSeconds * 20;
        this.startedAt = System.currentTimeMillis();
        this.phase = GamePhase.IN_GAME;

        // Distribution des items + briefing
        for (ServerPlayerEntity p : players) {
            Role role = roleManager.getRole(p);
            p.getInventory().clear();
            switch (role) {
                case MURDERER -> p.getInventory().insertStack(new ItemStack(ModItems.KNIFE));
                case DETECTIVE -> p.getInventory().insertStack(PistolItem.createWithAmmo(ModItems.PISTOL, PistolItem.MAX_AMMO));
                default -> { /* INNOCENT : rien */ }
            }
            sendBriefing(p, role);
            ModNetworking.sendRoleAssign(p, role);
        }

        broadcast(Text.translatable("murdercraft.game.started").formatted(Formatting.GREEN, Formatting.BOLD));

        // Téléporte les joueurs aléatoirement (avec immunité) — fait après distribution
        teleportPlayersRandomly(players);

        // Démarre une tâche si c'est la manche 3+ (système de Phase B)
        TaskManager.get().onRoundStart(server, currentRound);
    }

    // ============================================================
    // === API DEBUG (exposée pour la commande /murder debug ...) ===
    // ============================================================

    /**
     * Démarre une partie IMMÉDIATEMENT en bypassant toutes les vérifications
     * (countdown, min players, conditions de victoire). Si forcedRole != null,
     * le rôle du caller est forcé après l'assignation initiale.
     *
     * Si une partie est déjà en cours/ENDING : reset automatique avant le start.
     * Active le debugMode qui DÉSACTIVE les conditions de victoire — la partie
     * ne s'arrêtera que via /murder stop ou /murder debug endwith.
     */
    public boolean debugStart(MinecraftServer server, ServerPlayerEntity caller, Role forcedRole) {
        this.server = server;

        // Force reset si une partie est en cours / en ENDING
        if (phase != GamePhase.LOBBY) {
            resetToLobby();
        }

        // Auto-ajout du caller s'il n'est pas déjà dans le lobby
        if (caller != null && !participants.contains(caller.getUuid())) {
            participants.add(caller.getUuid());
        }
        if (participants.isEmpty()) return false;

        // Active le mode debug — empêche les conditions de victoire de fermer la partie
        this.debugMode = true;

        List<ServerPlayerEntity> players = resolveParticipants();
        roleManager.assignRoles(players);

        // Override du rôle du caller si demandé
        if (caller != null && forcedRole != null) {
            roleManager.setRole(caller.getUuid(), forcedRole);
        }

        this.hiddenPistolSpawned = false;
        finalizeStart(players);

        broadcast(Text.translatable("murdercraft.debug.started").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        return true;
    }

    /** Change le rôle d'un joueur en cours de partie + lui redonne l'item correspondant. */
    public boolean setPlayerRoleAndItems(ServerPlayerEntity player, Role role) {
        if (!isGameActive()) return false;
        roleManager.setRole(player.getUuid(), role);

        player.getInventory().clear();
        switch (role) {
            case MURDERER -> player.getInventory().insertStack(new ItemStack(ModItems.KNIFE));
            case DETECTIVE -> player.getInventory().insertStack(PistolItem.createWithAmmo(ModItems.PISTOL, PistolItem.MAX_AMMO));
            default -> { /* INNOCENT : rien */ }
        }

        ModNetworking.sendRoleAssign(player, role);
        return true;
    }

    /** Spawn immédiatement un pistolet caché à 2 blocs devant le joueur. */
    public boolean forceSpawnHiddenPistolNear(ServerPlayerEntity player) {
        if (player == null) return false;
        var pos = player.getPos().add(2, 1, 0);
        ItemStack stack = new ItemStack(ModItems.HIDDEN_PISTOL);
        ItemEntity drop = new ItemEntity(player.getServerWorld(), pos.x, pos.y, pos.z, stack);
        drop.setNeverDespawn();
        drop.setGlowing(true);
        player.getServerWorld().spawnEntity(drop);
        hiddenPistolSpawned = true;
        return true;
    }

    /** Force la fin de la partie avec un résultat donné. */
    public void forceEndGame(WinResult result) {
        if (phase == GamePhase.LOBBY) return;
        endGame(result);
    }

    // ============================================================
    // === RÈGLES PISTOLET (Phase A) ==============================
    // ============================================================

    /**
     * Vérification périodique : applique les règles autour des pistolets.
     *   - Un MEURTRIER avec un pistolet → on lui retire et on le drop au sol (HIDDEN_PISTOL)
     *   - Un INNOCENT avec un HIDDEN_PISTOL → promotion immédiate en JUSTICIER
     */
    private void checkInventoryRules() {
        if (server == null) return;
        for (UUID id : participants) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null || !roleManager.isAlive(p)) continue;

            Role role = roleManager.getRole(p);

            // Innocent qui a ramassé le pistolet caché → promotion auto, préserve les balles
            if (role == Role.INNOCENT && hasHiddenPistol(p) && !roleManager.isPermanentlyDisarmed(p.getUuid())) {
                int ammo = getAnyPistolAmmo(p);
                removeAllPistols(p);
                p.getInventory().insertStack(PistolItem.createWithAmmo(ModItems.PISTOL, ammo));
                promoteToDetective(p);
            }
            // Meurtrier avec un pistolet → on lui retire, préserve les balles dans le drop
            else if (role == Role.MURDERER && hasAnyPistol(p)) {
                int ammo = getAnyPistolAmmo(p);
                removeAllPistols(p);
                long now = System.currentTimeMillis();
                Long last = lastPistolEjection.get(p.getUuid());
                if (last == null || now - last > EJECTION_COOLDOWN_MS) {
                    throwHiddenPistolAwayFrom(p, ammo);
                    lastPistolEjection.put(p.getUuid(), now);
                    p.sendMessage(Text.translatable("murdercraft.murderer.cannot_take_pistol")
                            .formatted(Formatting.RED), true);
                }
                // Sinon : retrait silencieux pour éviter le spam (le pistolet déjà jeté est encore au sol)
            }
        }
    }

    /** Drop un HIDDEN_PISTOL au sol à la position du joueur, avec un nombre de balles donné. */
    public void dropHiddenPistolAt(ServerPlayerEntity player, int ammo) {
        if (player == null || player.getServerWorld() == null) return;
        ItemStack stack = PistolItem.createWithAmmo(ModItems.HIDDEN_PISTOL, ammo);
        ItemEntity drop = new ItemEntity(player.getServerWorld(),
                player.getX(), player.getY() + 0.5, player.getZ(), stack);
        drop.setNeverDespawn();
        drop.setGlowing(true);
        drop.setPickupDelay(20); // 1s grace period
        player.getServerWorld().spawnEntity(drop);
    }

    /** Variante sans ammo : pistolet plein (utile pour les anciens appels ou tests). */
    public void dropHiddenPistolAt(ServerPlayerEntity player) {
        dropHiddenPistolAt(player, PistolItem.MAX_AMMO);
    }

    /**
     * Jette le pistolet AU LOIN du joueur (meurtrier auto-éjection).
     * Préserve le nombre de balles.
     */
    public void throwHiddenPistolAwayFrom(ServerPlayerEntity player, int ammo) {
        if (player == null || player.getServerWorld() == null) return;
        var dir = player.getRotationVec(1.0f).normalize();
        var throwPos = player.getEyePos().add(dir.multiply(0.8));
        ItemStack stack = PistolItem.createWithAmmo(ModItems.HIDDEN_PISTOL, ammo);
        ItemEntity drop = new ItemEntity(player.getServerWorld(),
                throwPos.x, throwPos.y, throwPos.z, stack);
        drop.setVelocity(dir.x * 0.6, 0.3, dir.z * 0.6);
        drop.setNeverDespawn();
        drop.setGlowing(true);
        drop.setPickupDelay(60);
        player.getServerWorld().spawnEntity(drop);
    }

    private boolean hasHiddenPistol(ServerPlayerEntity p) {
        return p.getInventory().containsAny(s -> s.isOf(ModItems.HIDDEN_PISTOL));
    }

    private boolean hasAnyPistol(ServerPlayerEntity p) {
        return p.getInventory().containsAny(s -> s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL));
    }

    private void removeAllPistols(ServerPlayerEntity p) {
        p.getInventory().remove(
                s -> s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL),
                Integer.MAX_VALUE, p.getInventory());
    }

    /** Lit les balles restantes du premier pistolet trouvé dans l'inventaire. */
    private int getAnyPistolAmmo(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL)) {
                return PistolItem.getAmmo(s);
            }
        }
        return PistolItem.MAX_AMMO;
    }

    // ============================================================
    // === SPAWN ALÉATOIRE + IMMUNITÉ + WORLD BORDER ==============
    // ============================================================

    /** Téléporte tous les joueurs d'une liste à des positions aléatoires terrestres. */
    private void teleportPlayersRandomly(List<ServerPlayerEntity> players) {
        MurderCraftConfig cfg = MurderCraftConfig.get();
        if (!cfg.randomSpawnEnabled) return;
        if (server == null) return;

        net.minecraft.server.world.ServerWorld world = server.getOverworld();
        if (world == null) return;

        net.minecraft.util.math.BlockPos spawnCenter = world.getSpawnPos();
        int radius = cfg.randomSpawnRadius;
        int height = cfg.randomSpawnHeight;

        for (ServerPlayerEntity p : players) {
            net.minecraft.util.math.BlockPos target = findValidLandSpawn(world, spawnCenter, radius, 60);
            if (target == null) {
                MurderCraft.LOGGER.warn("[GameManager] No valid land spawn found for {}, using world spawn", p.getName().getString());
                target = spawnCenter;
            }
            double tx = target.getX() + 0.5;
            double ty = target.getY() + height; // Spawn en hauteur
            double tz = target.getZ() + 0.5;

            // Téléporte
            p.requestTeleport(tx, ty, tz);

            // Active l'immunité aux dégâts
            markSafeSpawn(p);
        }

        broadcast(Text.translatable("murdercraft.spawn.teleported")
                .formatted(Formatting.AQUA));
    }

    /** Cherche une position de spawn valide (sol terrestre, pas eau). */
    private net.minecraft.util.math.BlockPos findValidLandSpawn(
            net.minecraft.server.world.ServerWorld world,
            net.minecraft.util.math.BlockPos center,
            int radius, int maxAttempts) {
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < maxAttempts; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            int dist = 30 + rand.nextInt(Math.max(1, radius - 30)); // évite le centre exact
            int dx = (int) (Math.cos(angle) * dist);
            int dz = (int) (Math.sin(angle) * dist);
            net.minecraft.util.math.BlockPos surface = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING,
                    center.add(dx, 0, dz));

            // Vérifier biome — exclure océan/rivière/etc.
            var biome = world.getBiome(surface);
            if (biome.isIn(BiomeTags.IS_OCEAN)
                    || biome.isIn(BiomeTags.IS_DEEP_OCEAN)
                    || biome.isIn(BiomeTags.IS_RIVER)) {
                continue;
            }

            // Vérifier que le bloc en-dessous est solide (sol)
            var below = world.getBlockState(surface.down());
            if (!below.isSolidBlock(world, surface.down())) continue;

            // Pas dans un fluide (eau/lave)
            if (!world.getFluidState(surface).isEmpty()) continue;

            return surface;
        }
        return null;
    }

    /** Active l'immunité aux dégâts pour ce joueur pendant N secondes. */
    public void markSafeSpawn(ServerPlayerEntity player) {
        long expiresAt = System.currentTimeMillis()
                + (MurderCraftConfig.get().spawnImmunitySeconds * 1000L);
        safeSpawnExpiration.put(player.getUuid(), expiresAt);
    }

    /** Indique si un joueur est encore dans sa fenêtre d'immunité. */
    public boolean isInSafeSpawn(UUID playerId) {
        Long expires = safeSpawnExpiration.get(playerId);
        if (expires == null) return false;
        if (System.currentTimeMillis() > expires) {
            safeSpawnExpiration.remove(playerId);
            return false;
        }
        return true;
    }

    /** Renvoie le nombre de secondes restantes d'immunité (0 si expirée). */
    public int getSafeSpawnSecondsLeft(UUID playerId) {
        Long expires = safeSpawnExpiration.get(playerId);
        if (expires == null) return 0;
        long left = expires - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }

    /** Configure la world border carrée centrée sur /setworldspawn. */
    private void setupWorldBorder() {
        MurderCraftConfig cfg = MurderCraftConfig.get();
        if (!cfg.useWorldBorder) return;
        if (server == null) return;

        net.minecraft.server.world.ServerWorld world = server.getOverworld();
        if (world == null) return;

        WorldBorder border = world.getWorldBorder();

        // Sauvegarde l'état actuel pour le restaurer en fin de session
        if (!borderWasModified) {
            savedBorderCenterX = border.getCenterX();
            savedBorderCenterZ = border.getCenterZ();
            savedBorderSize = border.getSize();
            borderWasModified = true;
        }

        net.minecraft.util.math.BlockPos spawnPos = world.getSpawnPos();
        border.setCenter(spawnPos.getX() + 0.5, spawnPos.getZ() + 0.5);
        border.setSize(cfg.worldBorderSize);
        MurderCraft.LOGGER.info("[GameManager] World border set: center=({},{}), size={}",
                spawnPos.getX(), spawnPos.getZ(), cfg.worldBorderSize);
    }

    /** Affiche le compte à rebours d'immunité en action bar (pour ceux qui sont protégés). */
    private void showSafeSpawnCountdown() {
        if (server == null || safeSpawnExpiration.isEmpty()) return;
        for (UUID id : safeSpawnExpiration.keySet()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null) continue;
            int secondsLeft = getSafeSpawnSecondsLeft(id);
            if (secondsLeft > 0) {
                p.sendMessage(Text.translatable("murdercraft.spawn.immunity_countdown", secondsLeft)
                        .formatted(Formatting.AQUA, Formatting.BOLD), true);
            } else {
                p.sendMessage(Text.translatable("murdercraft.spawn.immunity_expired")
                        .formatted(Formatting.YELLOW), true);
            }
        }
    }

    /** Restaure la world border à sa configuration d'avant-session. */
    private void restoreWorldBorder() {
        if (!borderWasModified) return;
        if (server == null) return;

        net.minecraft.server.world.ServerWorld world = server.getOverworld();
        if (world == null) return;

        WorldBorder border = world.getWorldBorder();
        border.setCenter(savedBorderCenterX, savedBorderCenterZ);
        border.setSize(savedBorderSize);
        borderWasModified = false;
        MurderCraft.LOGGER.info("[GameManager] World border restored");
    }

    /**
     * Awareness des meurtriers : chaque meurtrier voit des particules SOUL au-dessus
     * de la tête des autres meurtriers. Ces particules sont envoyées en privé via
     * spawnParticles(ServerPlayerEntity, ...) — invisibles pour les autres joueurs.
     */
    private void sendMurdererAwareness() {
        if (server == null) return;
        List<UUID> murderers = roleManager.getPlayersByRole(Role.MURDERER);
        if (murderers.size() < 2) return;

        for (UUID viewerId : murderers) {
            ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerId);
            if (viewer == null || !roleManager.isAlive(viewer)) continue;
            for (UUID otherId : murderers) {
                if (otherId.equals(viewerId)) continue;
                ServerPlayerEntity other = server.getPlayerManager().getPlayer(otherId);
                if (other == null || !roleManager.isAlive(other)) continue;
                // Particules SOUL en privé — visible uniquement par le viewer
                viewer.getServerWorld().spawnParticles(
                        viewer,
                        ParticleTypes.SOUL_FIRE_FLAME,
                        true,
                        other.getX(), other.getY() + 2.4, other.getZ(),
                        3,
                        0.2, 0.1, 0.2,
                        0.01
                );
            }
        }
    }

    /** Indique si le mode debug est activé (pour la commande /murder debug info). */
    public boolean isDebugMode() {
        return debugMode;
    }

    private void sendBriefing(ServerPlayerEntity player, Role role) {
        // Title dramatique : ton rôle
        TitleUtil.sendDramaticTitle(player,
                Text.translatable("murdercraft.briefing.title").formatted(Formatting.WHITE, Formatting.BOLD),
                role.getDisplayName().copy().formatted(Formatting.BOLD));

        // Message détaillé en chat (objectif)
        player.sendMessage(Text.empty(), false);
        player.sendMessage(Text.literal("═══════════════════════").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.translatable("murdercraft.briefing.role_is").append(role.getDisplayName()), false);
        player.sendMessage(role.getObjective().copy().formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("═══════════════════════").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.empty(), false);

        // Son selon le rôle pour une meilleure ambiance
        switch (role) {
            case MURDERER -> player.playSoundToPlayer(SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.MASTER, 0.4f, 1.8f);
            case DETECTIVE -> player.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 0.8f, 1.0f);
            default -> player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.6f, 1.0f);
        }
    }

    /** Promeut un innocent en détective (cas du pistolet caché ramassé). */
    public void promoteToDetective(ServerPlayerEntity player) {
        if (!isGameActive()) return;
        if (roleManager.getRole(player) != Role.INNOCENT) return;
        if (roleManager.isPermanentlyDisarmed(player.getUuid())) return;

        roleManager.setRole(player.getUuid(), Role.DETECTIVE);
        ModNetworking.sendRoleAssign(player, Role.DETECTIVE);

        broadcast(Text.translatable("murdercraft.event.new_detective", player.getName().getString())
                .formatted(Formatting.BLUE, Formatting.BOLD));
    }

    /** Spawn le pistolet caché à un emplacement aléatoire autour du centre. */
    private void spawnHiddenPistol() {
        if (hiddenPistolSpawned || server == null) return;
        ServerWorld world = server.getOverworld();
        if (world == null) return;

        // Pour l'instant : spawn sur un joueur aléatoire à distance moyenne
        List<ServerPlayerEntity> alive = new ArrayList<>();
        for (UUID id : roleManager.getAlive()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null && roleManager.getRole(p) == Role.INNOCENT) {
                alive.add(p);
            }
        }
        if (alive.isEmpty()) return;

        ServerPlayerEntity randomPlayer = alive.get(new Random().nextInt(alive.size()));
        Vec3d pos = randomPlayer.getPos().add(
                (Math.random() - 0.5) * 30, 1.0, (Math.random() - 0.5) * 30);

        ItemStack stack = PistolItem.createWithAmmo(ModItems.HIDDEN_PISTOL, PistolItem.MAX_AMMO);
        ItemEntity drop = new ItemEntity(randomPlayer.getServerWorld(), pos.x, pos.y, pos.z, stack);
        drop.setNeverDespawn();
        drop.setGlowing(true);
        randomPlayer.getServerWorld().spawnEntity(drop);

        hiddenPistolSpawned = true;
        broadcast(Text.translatable("murdercraft.event.hidden_pistol_dropped")
                .formatted(Formatting.GOLD, Formatting.BOLD, Formatting.ITALIC));
    }

    /** Vérifie les conditions de victoire et termine la partie le cas échéant. */
    private void checkWinConditions() {
        // En mode debug, on ne vérifie rien — la partie ne s'arrête que manuellement
        if (debugMode) return;

        int murderersAlive = roleManager.countAliveByRole(Role.MURDERER);
        int innocentsAlive = roleManager.countAliveByRole(Role.INNOCENT);
        int detectivesAlive = roleManager.countAliveByRole(Role.DETECTIVE);
        int goodGuysAlive = innocentsAlive + detectivesAlive;

        if (murderersAlive == 0) {
            endGame(WinResult.INNOCENTS_WIN);
        } else if (goodGuysAlive == 0) {
            endGame(WinResult.MURDERERS_WIN);
        }
    }

    private void endGame(WinResult result) {
        this.phase = GamePhase.ENDING;

        // Mise à jour des stats de session
        if (inSession) {
            if (result == WinResult.INNOCENTS_WIN) sessionInnocentWins++;
            if (result == WinResult.MURDERERS_WIN) sessionMurdererWins++;
        }

        broadcast(Text.empty());
        broadcast(Text.literal("═══════════════════════").formatted(Formatting.GOLD));
        broadcast(result.getDisplayText());
        if (inSession) {
            MurderCraftConfig cfg = MurderCraftConfig.get();
            broadcast(Text.translatable("murdercraft.round.result",
                    currentRound, cfg.maxRounds,
                    sessionInnocentWins, sessionMurdererWins).formatted(Formatting.AQUA));
        }
        broadcast(Text.literal("═══════════════════════").formatted(Formatting.GOLD));
        broadcast(Text.empty());

        // Révéler les rôles
        broadcast(Text.translatable("murdercraft.end.reveal_roles").formatted(Formatting.YELLOW));
        if (server != null) {
            for (UUID id : roleManager.getAllPlayers()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                if (p != null) {
                    Role r = roleManager.getRole(p);
                    broadcast(Text.literal(" • ").append(p.getName()).append(" → ").append(r.getDisplayName()));
                }
            }
        }

        this.gameTicksLeft = 0;

        // Décider du temps avant la prochaine étape
        MurderCraftConfig cfg = MurderCraftConfig.get();
        if (inSession && currentRound < cfg.maxRounds && !debugMode) {
            // Encore des manches à jouer → délai inter-manche puis nouvelle manche
            this.tickCounter = cfg.interRoundSeconds * 20;
            this.awaitingNextRound = true;
        } else {
            // Dernière manche OU mode debug OU pas en session → retour lobby
            this.tickCounter = 200; // 10s
            this.awaitingNextRound = false;
        }

        // Notifier les clients + envoyer un title dramatique
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                ModNetworking.sendGameState(p, GamePhase.ENDING, 0, result);
                TitleUtil.sendDramaticTitle(p,
                        result.getDisplayText(),
                        Text.translatable("murdercraft.round.result.subtitle",
                                currentRound, cfg.maxRounds,
                                sessionInnocentWins, sessionMurdererWins).formatted(Formatting.AQUA));

                // Son selon le résultat
                switch (result) {
                    case INNOCENTS_WIN -> p.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
                    case MURDERERS_WIN -> p.playSoundToPlayer(SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.MASTER, 0.6f, 1.5f);
                    case DRAW, CANCELED -> p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 1.0f, 0.5f);
                }
            }
        }
    }

    private void resetToLobby() {
        this.phase = GamePhase.LOBBY;
        this.participants.clear();
        this.roleManager.clear();
        this.gameTicksLeft = 0;
        this.countdownTicksLeft = 0;
        this.hiddenPistolSpawned = false;
        this.tickCounter = 0;
        this.debugMode = false;
        // Reset session
        this.inSession = false;
        this.currentRound = 0;
        this.sessionMurdererWins = 0;
        this.sessionInnocentWins = 0;
        this.awaitingNextRound = false;

        // Reset tasks
        TaskManager.get().cleanup(server);

        // Reset cooldown éjection pistolet
        lastPistolEjection.clear();

        // Reset immunités + restore world border
        safeSpawnExpiration.clear();
        restoreWorldBorder();

        // Restaurer les joueurs
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.getInventory().remove(s -> s.isOf(ModItems.KNIFE) || s.isOf(ModItems.PISTOL) || s.isOf(ModItems.HIDDEN_PISTOL),
                        Integer.MAX_VALUE, p.getInventory());
                ModNetworking.sendGameState(p, GamePhase.LOBBY, 0, null);
            }
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (this.server == null) this.server = server;
        tickCounter++;

        switch (phase) {
            case STARTING -> {
                countdownTicksLeft--;
                // Toutes les secondes
                if (countdownTicksLeft % 20 == 0) {
                    int secLeft = countdownTicksLeft / 20;
                    if (secLeft > 0 && secLeft <= 5) {
                        broadcast(Text.translatable("murdercraft.countdown.seconds", secLeft).formatted(Formatting.GOLD, Formatting.BOLD));
                    } else if (secLeft == 10) {
                        broadcast(Text.translatable("murdercraft.countdown.ten_sec").formatted(Formatting.GOLD));
                    }
                }
                if (countdownTicksLeft <= 0) {
                    beginInGame();
                }
            }
            case IN_GAME -> {
                gameTicksLeft--;

                // Spawn du pistolet caché
                int elapsedSec = (MurderCraftConfig.get().gameDurationSeconds * 20 - gameTicksLeft) / 20;
                if (!hiddenPistolSpawned && elapsedSec >= MurderCraftConfig.get().hiddenPistolSpawnDelaySeconds) {
                    spawnHiddenPistol();
                }

                // Vérification d'inventaire 2x par seconde (règles pistolet)
                if (gameTicksLeft % 10 == 0) {
                    checkInventoryRules();
                }

                // Awareness meurtriers : toutes les 2s, particules privées entre meurtriers
                if (gameTicksLeft % 40 == 0) {
                    sendMurdererAwareness();
                }

                // Action bar countdown pour l'immunité au spawn (chaque seconde)
                if (gameTicksLeft % 20 == 0) {
                    showSafeSpawnCountdown();
                }

                // Tick TaskManager (vérifie complétion + décrémente fenêtres de révélation)
                TaskManager.get().tick(server);

                // Mise à jour du HUD client toutes les secondes
                if (gameTicksLeft % 20 == 0) {
                    if (server != null) {
                        for (UUID id : participants) {
                            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                            if (p != null) {
                                ModNetworking.sendGameState(p, GamePhase.IN_GAME, gameTicksLeft / 20, null);
                            }
                        }
                    }
                }

                checkWinConditions();

                if (phase == GamePhase.IN_GAME && gameTicksLeft <= 0) {
                    // Temps écoulé : les meurtriers ont échoué à tuer tout le monde
                    // → les innocents gagnent (règle officielle GMod Murder)
                    endGame(WinResult.INNOCENTS_WIN);
                }
            }
            case ENDING -> {
                tickCounter--;
                if (tickCounter <= 0) {
                    if (awaitingNextRound) {
                        // Lance la manche suivante (garde les participants)
                        startNextRound();
                    } else if (inSession) {
                        // Dernière manche : finalise la session
                        endSession();
                    } else {
                        resetToLobby();
                    }
                }
            }
            default -> {
                // LOBBY : rien à faire, attente
            }
        }
    }

    private void broadcast(Text text) {
        if (server == null) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(text, false);
        }
    }
}
