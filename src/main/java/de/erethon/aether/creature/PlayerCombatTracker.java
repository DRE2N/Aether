package de.erethon.aether.creature;

import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerCombatTracker {

    public static final int MAX_ATTACKERS = 4;
    private static final PlayerCombatTracker INSTANCE = new PlayerCombatTracker();

    private final Map<UUID, Set<AetherBaseMob>> attackersByPlayer = new HashMap<>();
    private final Map<UUID, UUID> playerByAttacker = new HashMap<>();
    private final Map<UUID, Windup> windupsByPlayer = new HashMap<>();
    private final Map<UUID, UUID> windupPlayerByAttacker = new HashMap<>();

    private PlayerCombatTracker() {
    }

    public static PlayerCombatTracker getInstance() {
        return INSTANCE;
    }

    public boolean canEngage(UUID playerId, AetherBaseMob mob) {
        Set<AetherBaseMob> attackers = attackersByPlayer.get(playerId);
        if (attackers == null) {
            return true;
        }
        attackers.removeIf(attacker -> {
            if (attacker.isAlive() && attacker.getTarget() != null
                    && playerId.equals(attacker.getTarget().getUUID())) {
                return false;
            }
            playerByAttacker.remove(attacker.getUUID(), playerId);
            releaseWindup(attacker.getUUID());
            return true;
        });
        if (attackers.isEmpty()) {
            attackersByPlayer.remove(playerId);
        }
        return attackers.contains(mob) || attackers.size() < MAX_ATTACKERS;
    }

    /** Update only after the server and target event have accepted the actual target. */
    public void updateTarget(AetherBaseMob mob, UUID playerId) {
        if (java.util.Objects.equals(playerByAttacker.get(mob.getUUID()), playerId)) {
            return;
        }
        unregister(mob.getUUID());
        if (playerId != null) {
            playerByAttacker.put(mob.getUUID(), playerId);
            attackersByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(mob);
        }
    }

    public boolean tryBeginWindup(UUID playerId, UUID mobId, int duration) {
        Windup current = windupsByPlayer.get(playerId);
        int now = MinecraftServer.currentTick;
        if (current != null) {
            if (now < current.expiresAt()) {
                return false;
            }
            releaseWindup(current.attackerId());
        }

        releaseWindup(mobId);
        windupsByPlayer.put(playerId, new Windup(mobId, now + duration + 2));
        windupPlayerByAttacker.put(mobId, playerId);
        return true;
    }

    public void releaseWindup(UUID mobId) {
        UUID playerId = windupPlayerByAttacker.remove(mobId);
        if (playerId != null) {
            windupsByPlayer.remove(playerId);
        }
    }

    public void unregister(UUID mobId) {
        releaseWindup(mobId);
        UUID playerId = playerByAttacker.remove(mobId);
        if (playerId == null) {
            return;
        }
        Set<AetherBaseMob> attackers = attackersByPlayer.get(playerId);
        if (attackers != null) {
            attackers.removeIf(attacker -> mobId.equals(attacker.getUUID()));
            if (attackers.isEmpty()) {
                attackersByPlayer.remove(playerId);
            }
        }
    }

    private record Windup(UUID attackerId, int expiresAt) {
    }
}
