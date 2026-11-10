package de.erethon.aether.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Melee chase/attack range helpers. Papyrus 26.1 exposes {@link PathfinderMob#isWithinMeleeAttackRange}
 * but not {@code getMeleeAttackRangeSqr} / perceived-distance helpers, so leniency uses a hitbox-based estimate.
 */
public final class MeleeReachUtil {

    /** Chase toward a point inside estimated reach; vanilla range still decides when to stop and swing. */
    public static final double CHASE_INSET_BLOCKS = 0.6;

    private MeleeReachUtil() {
    }

    /**
     * Approximate center-to-center distance where vanilla {@link PathfinderMob#isWithinMeleeAttackRange} becomes true.
     */
    static double approximateMeleeReach(PathfinderMob mob, LivingEntity target) {
        return (mob.getBbWidth() + target.getBbWidth()) * 0.5 + 0.8;
    }

    public static boolean isWithinLenientAttackRange(PathfinderMob mob, LivingEntity target) {
        return mob.isWithinMeleeAttackRange(target);
    }

    public static boolean shouldKeepChasingForMelee(PathfinderMob mob, LivingEntity target) {
        return !mob.isWithinMeleeAttackRange(target);
    }
}
