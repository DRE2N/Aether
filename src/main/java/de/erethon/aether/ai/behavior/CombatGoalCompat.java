package de.erethon.aether.ai.behavior;

import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.ai.goals.MeleeAttackGoal;
import de.erethon.aether.ai.goals.RangedBowAttackGoal;
import de.erethon.aether.ai.goals.RangedCrossbowAttackGoal;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import net.minecraft.world.entity.LivingEntity;

/**
 * Helpers so {@link de.erethon.aether.ai.behavior.MobBehaviorController} does not fight
 * configured {@link de.erethon.aether.ai.goals.AEMeleeAttackGoal} / ranged goals.
 * Mobs without {@code ai.behavior} only use goals and work correctly; behavior actions must defer.
 */
public final class CombatGoalCompat {

    private CombatGoalCompat() {
    }

    public static boolean hasMeleeAttackGoal(AetherBaseMob mob) {
        return hasGoalType(mob.getData(), MeleeAttackGoal.class);
    }

    public static boolean hasRangedAttackGoal(AetherBaseMob mob) {
        return hasGoalType(mob.getData(), RangedBowAttackGoal.class)
                || hasGoalType(mob.getData(), RangedCrossbowAttackGoal.class);
    }

    public static boolean hasCombatAttackGoal(AetherBaseMob mob) {
        return hasMeleeAttackGoal(mob) || hasRangedAttackGoal(mob);
    }

    /**
     * {@code onTick} spell casts fight goal-based chase/attack; event-driven casts (onAttack, onEnter, …) still run.
     */
    public static boolean shouldDeferTickSpellToGoals(AetherBaseMob mob, BehaviorTrigger trigger) {
        return trigger == BehaviorTrigger.TICK && hasCombatAttackGoal(mob);
    }

    public static boolean shouldDeferMovementToGoals(AetherBaseMob mob) {
        if (mob.shouldSuppressCombatGoalsFromBehavior()) {
            return false;
        }
        return hasCombatAttackGoal(mob);
    }

    public static boolean shouldDeferCombatGoalsToBehavior(AetherBaseMob mob) {
        return mob.shouldSuppressCombatGoalsFromBehavior();
    }

    private static boolean hasGoalType(NPCData data, Class<?> goalType) {
        if (data == null) {
            return false;
        }
        for (AEPathfinderGoal goal : data.getGoals()) {
            if (goalType.isInstance(goal)) {
                return true;
            }
        }
        return false;
    }
}
