package fr.murdercraft.roles;

import fr.murdercraft.MurderCraft;
import fr.murdercraft.config.MurderCraftConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Gère l'attribution et la consultation des rôles des joueurs pendant une partie.
 */
public class RoleManager {

    private final Map<UUID, Role> roles = new HashMap<>();
    private final Set<UUID> alive = new HashSet<>();
    /** Suit qui a perdu son arme de justicier (perte définitive pour la game). */
    private final Set<UUID> permanentlyDisarmed = new HashSet<>();

    public void clear() {
        roles.clear();
        alive.clear();
        permanentlyDisarmed.clear();
    }

    /**
     * Distribue les rôles aléatoirement parmi les joueurs.
     */
    public void assignRoles(List<ServerPlayerEntity> players) {
        clear();
        MurderCraftConfig cfg = MurderCraftConfig.get();

        List<ServerPlayerEntity> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        int idx = 0;

        // 1. Meurtriers
        for (int i = 0; i < cfg.murdererCount && idx < shuffled.size(); i++, idx++) {
            ServerPlayerEntity p = shuffled.get(idx);
            roles.put(p.getUuid(), Role.MURDERER);
            alive.add(p.getUuid());
            MurderCraft.LOGGER.info("[RoleManager] {} -> MURDERER", p.getName().getString());
        }

        // 2. Justiciers (Detective)
        for (int i = 0; i < cfg.detectiveCount && idx < shuffled.size(); i++, idx++) {
            ServerPlayerEntity p = shuffled.get(idx);
            roles.put(p.getUuid(), Role.DETECTIVE);
            alive.add(p.getUuid());
            MurderCraft.LOGGER.info("[RoleManager] {} -> DETECTIVE", p.getName().getString());
        }

        // 3. Innocents (tout le reste)
        while (idx < shuffled.size()) {
            ServerPlayerEntity p = shuffled.get(idx);
            roles.put(p.getUuid(), Role.INNOCENT);
            alive.add(p.getUuid());
            MurderCraft.LOGGER.info("[RoleManager] {} -> INNOCENT", p.getName().getString());
            idx++;
        }
    }

    public Role getRole(UUID playerId) {
        return roles.getOrDefault(playerId, Role.SPECTATOR);
    }

    public Role getRole(ServerPlayerEntity player) {
        return getRole(player.getUuid());
    }

    /**
     * Change le rôle d'un joueur (utilisé quand un innocent devient justicier).
     */
    public void setRole(UUID playerId, Role role) {
        roles.put(playerId, role);
    }

    public boolean isAlive(UUID playerId) {
        return alive.contains(playerId);
    }

    public boolean isAlive(ServerPlayerEntity player) {
        return isAlive(player.getUuid());
    }

    public void markDead(UUID playerId) {
        alive.remove(playerId);
    }

    public Set<UUID> getAlive() {
        return Collections.unmodifiableSet(alive);
    }

    public Set<UUID> getAllPlayers() {
        return Collections.unmodifiableSet(roles.keySet());
    }

    public int countAliveByRole(Role role) {
        int count = 0;
        for (UUID id : alive) {
            if (roles.get(id) == role) count++;
        }
        return count;
    }

    public List<UUID> getPlayersByRole(Role role) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            if (e.getValue() == role) result.add(e.getKey());
        }
        return result;
    }

    public void markPermanentlyDisarmed(UUID playerId) {
        permanentlyDisarmed.add(playerId);
    }

    public boolean isPermanentlyDisarmed(UUID playerId) {
        return permanentlyDisarmed.contains(playerId);
    }
}
