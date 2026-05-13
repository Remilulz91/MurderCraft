package fr.murdercraft.client;

import fr.murdercraft.game.GamePhase;
import fr.murdercraft.game.WinResult;
import fr.murdercraft.roles.Role;

/**
 * État côté client : ce que le joueur sait de sa partie.
 * Mis à jour par les packets reçus du serveur.
 */
public class MurderCraftClientState {

    private static Role myRole = Role.SPECTATOR;
    private static GamePhase currentPhase = GamePhase.LOBBY;
    private static int secondsLeft = 0;
    private static WinResult lastWinResult = null;
    private static long winResultShownAt = 0;
    private static int currentRound = 0;
    private static int maxRounds = 4;

    public static int getCurrentRound() {
        return currentRound;
    }

    public static int getMaxRounds() {
        return maxRounds;
    }

    public static Role getMyRole() {
        return myRole;
    }

    public static void setMyRole(Role role) {
        myRole = role;
    }

    public static GamePhase getPhase() {
        return currentPhase;
    }

    public static int getSecondsLeft() {
        return secondsLeft;
    }

    public static WinResult getLastWinResult() {
        return lastWinResult;
    }

    public static long getWinResultShownAt() {
        return winResultShownAt;
    }

    public static void updateGameState(GamePhase phase, int seconds, WinResult winResult,
                                        int round, int maxR) {
        currentPhase = phase;
        secondsLeft = seconds;
        currentRound = round;
        maxRounds = maxR;
        if (winResult != null) {
            lastWinResult = winResult;
            winResultShownAt = System.currentTimeMillis();
        }
        if (phase == GamePhase.LOBBY) {
            myRole = Role.SPECTATOR;
            currentRound = 0;
        }
    }
}
