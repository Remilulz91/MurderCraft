package fr.murdercraft.tasks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Une tâche/mission qui apparaît pendant une manche.
 * Le premier joueur à la compléter reçoit un indice (rôle d'un autre joueur).
 *
 * Cycle de vie :
 *   1. start(server)        — préparation (spawn d'items, broadcast, etc.)
 *   2. checkCompletion() x tick — vérifie si quelqu'un a terminé la tâche
 *   3. cleanup(server)      — quand la manche se termine OU quand quelqu'un l'a complétée
 */
public interface Task {

    /** Identifiant interne unique (ex: "exploration", "crafting"). */
    String getId();

    /** Texte de description affiché aux joueurs au début de la tâche. */
    Text getDescription();

    /** Appelé une fois au début de la tâche. */
    void onStart(MinecraftServer server);

    /**
     * Appelé périodiquement (toutes les 10 ticks ≈ 2 fois/seconde).
     * Retourne l'UUID du joueur qui a terminé, ou null si personne n'a fini.
     */
    UUID checkCompletion(MinecraftServer server);

    /** Appelé quand la tâche se termine (succès OU fin de manche). */
    void cleanup(MinecraftServer server);

    /** Indique si cette tâche est terminée (un gagnant a été déterminé). */
    boolean isCompleted();
}
