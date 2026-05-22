package de.erethon.aether.creature;

import org.bukkit.event.entity.EntityTargetEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerCombatTracker {

    public static final int MAX_ATTACKERS = 4;
    private static final long SLOT_TIMEOUT_MS = 15_000;

    private static final PlayerCombatTracker INSTANCE = new PlayerCombatTracker();

    private final Map<UUID, LinkedHashMap<UUID, AttackerEntry>> activeCombat = new HashMap<>();

    private static class AttackerEntry {
        final AetherBaseMob mob;
        final long joinedAt;

        AttackerEntry(AetherBaseMob mob) {
            this.mob = mob;
            this.joinedAt = System.currentTimeMillis();
        }
    }

    private PlayerCombatTracker() {}

    public static PlayerCombatTracker getInstance() {
        return INSTANCE;
    }

    public boolean canEngage(UUID playerUUID, UUID mobUUID) {
        LinkedHashMap<UUID, AttackerEntry> attackers = activeCombat.get(playerUUID);
        if (attackers == null || attackers.isEmpty()) return true;
        return attackers.containsKey(mobUUID) || attackers.size() < MAX_ATTACKERS;
    }

    public void register(UUID playerUUID, AetherBaseMob mob) {
        activeCombat.computeIfAbsent(playerUUID, k -> new LinkedHashMap<>())
                .put(mob.getUUID(), new AttackerEntry(mob));
    }

    /** Retaliation mobs always get a slot, bypassing the cap. */
    public void forceRegister(UUID playerUUID, AetherBaseMob mob) {
        activeCombat.computeIfAbsent(playerUUID, k -> new LinkedHashMap<>())
                .put(mob.getUUID(), new AttackerEntry(mob));
    }

    /**
     * Evict mobs whose slot has timed out, forcing them to back off and making room for new attackers.
     * Call this before canEngage checks so freed slots are immediately available.
     */
    public void evictExpired(UUID playerUUID) {
        LinkedHashMap<UUID, AttackerEntry> attackers = activeCombat.get(playerUUID);
        if (attackers == null) return;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, AttackerEntry>> it = attackers.entrySet().iterator();
        while (it.hasNext()) {
            AttackerEntry entry = it.next().getValue();
            if (now - entry.joinedAt > SLOT_TIMEOUT_MS) {
                it.remove();
                entry.mob.setCombatTarget(null, EntityTargetEvent.TargetReason.FORGOT_TARGET);
            }
        }
        if (attackers.isEmpty()) activeCombat.remove(playerUUID);
    }

    public void unregister(UUID mobUUID) {
        Iterator<Map.Entry<UUID, LinkedHashMap<UUID, AttackerEntry>>> it = activeCombat.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LinkedHashMap<UUID, AttackerEntry>> entry = it.next();
            entry.getValue().remove(mobUUID);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }
}
