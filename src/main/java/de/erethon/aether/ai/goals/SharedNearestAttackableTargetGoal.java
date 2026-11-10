package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.HashMap;
import java.util.Map;

/**
 * Shares recent target search results between nearby Aether mobs of the same faction.
 *
 * Candidate validity is still checked for every consumer, so dead, out-of-range, invisible, or
 * newly friendly targets are rejected without waiting for the cache to expire.
 */
public final class SharedNearestAttackableTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    private static final int CACHE_TICKS = 20;
    private static final int CELL_SHIFT = 4;
    private static final Map<SearchKey, LivingEntity> TARGETS = new HashMap<>();
    private static int targetEpoch = Integer.MIN_VALUE;

    private final AetherBaseMob aetherMob;
    private final Class<T> sharedTargetType;
    private final boolean sharedMustSee;
    private final int searchInterval;
    private int nextSearchTick;

    public SharedNearestAttackableTargetGoal(
            AetherBaseMob mob,
            Class<T> targetType,
            int randomInterval,
            boolean mustSee
    ) {
        super(mob, targetType, randomInterval, mustSee, false, mob::testSpellbookTeam);
        this.aetherMob = mob;
        this.sharedTargetType = targetType;
        this.sharedMustSee = mustSee;
        this.searchInterval = Math.max(1, this.randomInterval);
        this.nextSearchTick = MinecraftServer.currentTick + Math.floorMod(mob.getId(), this.searchInterval);
    }

    @Override
    public boolean canUse() {
        int currentTick = MinecraftServer.currentTick;
        if (currentTick < this.nextSearchTick) {
            return false;
        }
        this.nextSearchTick = currentTick + this.searchInterval;
        this.findTarget();
        return this.target != null;
    }

    @Override
    protected void findTarget() {
        int epoch = Math.floorDiv(MinecraftServer.currentTick, CACHE_TICKS);
        if (epoch != targetEpoch) {
            TARGETS.clear();
            targetEpoch = epoch;
        }

        ServerLevel level = getServerLevel(this.mob);
        double followDistance = this.getFollowDistance();
        SearchKey key = new SearchKey(
                level,
                this.aetherMob.getCombatFactionId(),
                this.sharedTargetType,
                this.sharedMustSee,
                Double.doubleToLongBits(followDistance),
                this.mob.getBlockX() >> CELL_SHIFT,
                this.mob.getBlockY() >> CELL_SHIFT,
                this.mob.getBlockZ() >> CELL_SHIFT
        );

        LivingEntity candidate = TARGETS.get(key);
        if (candidate != null && this.isReusable(level, candidate, followDistance)) {
            this.target = candidate;
            return;
        }

        super.findTarget();
        // A failed search is specific to this mob's position, visibility and quest scope.
        if (this.target != null) {
            TARGETS.put(key, this.target);
        }
    }

    private boolean isReusable(ServerLevel level, LivingEntity candidate, double followDistance) {
        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }
        if (this.mob.distanceToSqr(candidate) > followDistance * followDistance) {
            return false;
        }
        return this.targetConditions.range(followDistance).test(level, this.mob, candidate);
    }

    private record SearchKey(
            ServerLevel level,
            String faction,
            Class<? extends LivingEntity> targetType,
            boolean mustSee,
            long followDistance,
            int cellX,
            int cellY,
            int cellZ
    ) {
    }

}
