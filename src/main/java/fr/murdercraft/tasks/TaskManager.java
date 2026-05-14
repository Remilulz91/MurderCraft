package fr.murdercraft.tasks;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.game.GameManager;
import fr.murdercraft.roles.Role;
import fr.murdercraft.util.TitleUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gère les tâches actives pendant une session.
 *
 * Une tâche apparaît à partir de la manche 3 (configurable via TASK_FIRST_ROUND).
 * Le premier joueur à la compléter obtient une "fenêtre de révélation" de 30s
 * pendant laquelle il peut utiliser /murder task reveal <joueur> pour apprendre
 * le rôle d'un autre joueur.
 */
public class TaskManager {

    /** Manche minimale à partir de laquelle une tâche peut apparaître. */
    public static final int TASK_FIRST_ROUND = 3;

    /** Durée de la fenêtre de révélation (en ticks). */
    public static final int REVEAL_WINDOW_TICKS = 30 * 20; // 30 secondes

    private static final TaskManager INSTANCE = new TaskManager();
    private static final Random RANDOM = new Random();

    public static TaskManager get() {
        return INSTANCE;
    }

    private Task currentTask = null;
    /** Joueurs ayant un droit de révélation pendant : UUID → ticks restants. */
    private final Map<UUID, Integer> pendingReveals = new HashMap<>();

    public Task getCurrentTask() {
        return currentTask;
    }

    public boolean hasActiveTask() {
        return currentTask != null && !currentTask.isCompleted();
    }

    public boolean hasPendingReveal(UUID playerId) {
        return pendingReveals.containsKey(playerId);
    }

    public int getPendingRevealTicks(UUID playerId) {
        return pendingReveals.getOrDefault(playerId, 0);
    }

    /**
     * Démarre une tâche aléatoire au début d'une manche, si applicable.
     * Appelé par GameManager après la transition vers IN_GAME.
     */
    public void onRoundStart(MinecraftServer server, int round) {
        // Cleanup éventuel
        cleanup(server);

        if (round < TASK_FIRST_ROUND) {
            return;
        }

        // Tire une tâche au sort parmi celles disponibles
        currentTask = pickRandomTask();
        if (currentTask == null) return;

        currentTask.onStart(server);

        // Broadcast à tous les participants
        broadcastToParticipants(server, Text.empty());
        broadcastToParticipants(server, Text.literal("═══ TÂCHE ═══").formatted(Formatting.GOLD, Formatting.BOLD));
        broadcastToParticipants(server, currentTask.getDescription().copy().formatted(Formatting.YELLOW));
        broadcastToParticipants(server, Text.translatable("murdercraft.task.reward_hint").formatted(Formatting.GRAY, Formatting.ITALIC));
        broadcastToParticipants(server, Text.literal("═══════════").formatted(Formatting.GOLD, Formatting.BOLD));
        broadcastToParticipants(server, Text.empty());
    }

    /**
     * Appelé chaque tick par GameManager.
     * Vérifie la complétion de la tâche + décrémente les fenêtres de révélation.
     */
    public void tick(MinecraftServer server) {
        if (server == null) return;

        // Vérifie la complétion de la tâche active
        if (hasActiveTask()) {
            UUID winner = currentTask.checkCompletion(server);
            if (winner != null) {
                onTaskCompleted(server, winner);
            }
        }

        // Décrémente les fenêtres de révélation
        if (!pendingReveals.isEmpty()) {
            pendingReveals.entrySet().removeIf(e -> {
                int newTicks = e.getValue() - 1;
                if (newTicks <= 0) {
                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
                    if (p != null) {
                        p.sendMessage(Text.translatable("murdercraft.task.reveal_expired")
                                .formatted(Formatting.GRAY, Formatting.ITALIC), false);
                    }
                    return true;
                }
                e.setValue(newTicks);
                return false;
            });
        }
    }

    private void onTaskCompleted(MinecraftServer server, UUID winnerId) {
        ServerPlayerEntity winner = server.getPlayerManager().getPlayer(winnerId);
        if (winner == null) return;

        // Annonce publique aux participants
        broadcastToParticipants(server,
                Text.translatable("murdercraft.task.completed_by", winner.getName().getString())
                        .formatted(Formatting.GOLD, Formatting.BOLD));

        // Donne au gagnant une fenêtre de révélation
        pendingReveals.put(winnerId, REVEAL_WINDOW_TICKS);

        // Title dramatique au gagnant
        TitleUtil.sendDramaticTitle(winner,
                Text.translatable("murdercraft.task.you_won").formatted(Formatting.GOLD, Formatting.BOLD),
                Text.translatable("murdercraft.task.reveal_granted").formatted(Formatting.YELLOW));

        winner.sendMessage(Text.translatable("murdercraft.task.reveal_usage")
                .formatted(Formatting.YELLOW), false);
        // Son de victoire
        winner.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.MASTER, 1.0f, 1.0f);

        currentTask.cleanup(server);
    }

    /**
     * Tente de révéler le rôle d'un joueur cible au demandeur.
     * Retourne true si la révélation a réussi.
     */
    public boolean tryReveal(ServerPlayerEntity requester, ServerPlayerEntity target) {
        UUID reqId = requester.getUuid();
        if (!pendingReveals.containsKey(reqId)) {
            requester.sendMessage(Text.translatable("murdercraft.task.reveal_no_window")
                    .formatted(Formatting.RED), false);
            return false;
        }
        if (target.getUuid().equals(reqId)) {
            requester.sendMessage(Text.translatable("murdercraft.task.reveal_not_self")
                    .formatted(Formatting.RED), false);
            return false;
        }

        Role role = GameManager.get().getRoleManager().getRole(target);
        requester.sendMessage(Text.literal(""), false);
        requester.sendMessage(Text.literal("═══ INDICE ═══").formatted(Formatting.GOLD, Formatting.BOLD), false);
        requester.sendMessage(Text.translatable("murdercraft.task.reveal_result",
                target.getName().getString())
                .append(role.getDisplayName())
                .formatted(Formatting.WHITE), false);
        requester.sendMessage(Text.literal("═══════════").formatted(Formatting.GOLD, Formatting.BOLD), false);
        requester.sendMessage(Text.literal(""), false);

        // Consomme la fenêtre
        pendingReveals.remove(reqId);
        return true;
    }

    public void cleanup(MinecraftServer server) {
        if (currentTask != null) {
            currentTask.cleanup(server);
            currentTask = null;
        }
        pendingReveals.clear();
    }

    private Task pickRandomTask() {
        Task[] available = {
                new ExplorationTask(),
                new CraftingTask()
        };
        return available[RANDOM.nextInt(available.length)];
    }

    private void broadcastToParticipants(MinecraftServer server, Text text) {
        for (UUID id : GameManager.get().getParticipants()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null) p.sendMessage(text, false);
        }
    }
}
