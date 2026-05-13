package fr.murdercraft.game;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.config.MurderCraftConfig;
import fr.murdercraft.items.ModItems;
import fr.murdercraft.network.ModNetworking;
import fr.murdercraft.roles.Role;
import fr.murdercraft.roles.RoleManager;
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

    /** Démarre une nouvelle partie (manuelle via /murder start ou auto si min joueurs atteint). */
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
        this.phase = GamePhase.STARTING;
        this.countdownTicksLeft = cfg.startCountdownSeconds * 20;
        this.hiddenPistolSpawned = false;

        broadcast(Text.translatable("murdercraft.game.starting")
                .formatted(Formatting.GOLD, Formatting.BOLD));
        return true;
    }

    /** Force l'arrêt d'une partie en cours. */
    public void stopGame(boolean canceled) {
        if (phase == GamePhase.LOBBY) return;
        if (canceled) {
            endGame(WinResult.CANCELED);
        } else {
            endGame(WinResult.DRAW);
        }
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
                case DETECTIVE -> p.getInventory().insertStack(new ItemStack(ModItems.PISTOL));
                default -> { /* INNOCENT : rien */ }
            }
            sendBriefing(p, role);
            ModNetworking.sendRoleAssign(p, role);
        }

        broadcast(Text.translatable("murdercraft.game.started").formatted(Formatting.GREEN, Formatting.BOLD));
    }

    private void sendBriefing(ServerPlayerEntity player, Role role) {
        player.sendMessage(Text.empty(), false);
        player.sendMessage(Text.literal("═══════════════════════").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.translatable("murdercraft.briefing.role_is").append(role.getDisplayName()), false);
        player.sendMessage(role.getObjective().copy().formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("═══════════════════════").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.empty(), false);

        // Son de début
        player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.6f, 1.0f);
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

        ItemStack stack = new ItemStack(ModItems.HIDDEN_PISTOL);
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
        broadcast(Text.empty());
        broadcast(Text.literal("═══════════════════════").formatted(Formatting.GOLD));
        broadcast(result.getDisplayText());
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

        // Reset après 10 sec
        this.gameTicksLeft = 0;
        this.tickCounter = 200; // 10 seconds before going back to lobby

        // Notifier les clients
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                ModNetworking.sendGameState(p, GamePhase.ENDING, 0, result);
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
                    // Temps écoulé : les meurtriers gagnent (puisqu'ils n'ont pas été tués)
                    int murderersAlive = roleManager.countAliveByRole(Role.MURDERER);
                    endGame(murderersAlive > 0 ? WinResult.MURDERERS_WIN : WinResult.INNOCENTS_WIN);
                }
            }
            case ENDING -> {
                tickCounter--;
                if (tickCounter <= 0) {
                    resetToLobby();
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
