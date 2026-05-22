package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.aether.ai.behavior.CombatGoalCompat;
import de.erethon.aether.ai.goals.AEMeleeAttackGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Moves toward the current target until {@link net.minecraft.world.entity.Mob#isWithinMeleeAttackRange}.
 * Deferred when {@link de.erethon.aether.ai.behavior.CombatGoalCompat} says goals own movement.
 */
public class MoveToTargetAction extends AetherAction {

    private final double speed;

    public MoveToTargetAction(double speed) {
        this.speed = speed;
    }

    @Override
    public boolean runsBeforeGoals() {
        return true;
    }

    @Override
    public void execute(BehaviorContext context) {
        if (CombatGoalCompat.shouldDeferMovementToGoals(context.mob())) {
            return;
        }

        LivingEntity target = context.target();
        if (target == null || !(context.mob() instanceof PathfinderMob mob)) {
            return;
        }

        if (mob.isWithinMeleeAttackRange(target)) {
            return;
        }

        mob.getNavigation().moveTo(target, speed);
    }
}
