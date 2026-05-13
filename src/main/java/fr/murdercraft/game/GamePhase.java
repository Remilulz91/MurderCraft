package fr.murdercraft.game;

/**
 * Les différentes phases d'une partie MurderCraft.
 */
public enum GamePhase {
    /** Aucune partie en cours, attente que des joueurs rejoignent. */
    LOBBY,
    /** Compte à rebours avant le début (nombre min de joueurs atteint). */
    STARTING,
    /** Partie en cours. */
    IN_GAME,
    /** Partie terminée, affichage du résultat. */
    ENDING
}
