package de.erethon.aether.ai.goals;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.teams.SpellbookTeam;
import de.erethon.spellbook.teams.TeamManager;
import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Short-lived cache for the stable team relation checks performed while many nearby Aether mobs
 * evaluate the same candidates. This cache is only accessed from entity goal ticks.
 */
public final class AetherTargetingCache {

    private static final int CACHE_TICKS = 5;
    private static final Map<RelationKey, Boolean> RELATIONS = new HashMap<>();
    private static int relationEpoch = Integer.MIN_VALUE;

    private AetherTargetingCache() {
    }

    public static boolean canAttack(LivingEntity attacker, LivingEntity target) {
        TeamManager teamManager = Spellbook.getInstance().getTeamManager();
        SpellbookTeam attackerTeam = teamManager.getTeam(attacker);
        SpellbookTeam targetTeam = teamManager.getTeam(target);
        if (attackerTeam == null || targetTeam == null) {
            // An unteamed player is still an enemy of a teamed mob. Let Spellbook decide,
            // and avoid caching this result while the entity's team may still be assigned.
            return Spellbook.canAttack(attacker, target);
        }

        int epoch = Math.floorDiv(MinecraftServer.currentTick, CACHE_TICKS);
        if (epoch != relationEpoch) {
            RELATIONS.clear();
            relationEpoch = epoch;
        }

        RelationKey key = new RelationKey(attackerTeam, target.getUniqueId(), targetTeam);
        Boolean cached = RELATIONS.get(key);
        if (cached != null) {
            return cached;
        }

        boolean canAttack = Spellbook.canAttack(attacker, target);
        RELATIONS.put(key, canAttack);
        return canAttack;
    }

    private record RelationKey(
            SpellbookTeam attackerTeam,
            UUID targetId,
            SpellbookTeam targetTeam
    ) {
    }
}
