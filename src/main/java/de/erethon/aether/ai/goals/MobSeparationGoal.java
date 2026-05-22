package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fallback separation when no higher-priority MOVE goal is active (e.g. no combat target).
 * During melee, spacing is applied inside {@link AEMeleeAttackGoal} instead.
 */
public class MobSeparationGoal extends Goal {

    private static final double MOVE_DISTANCE = 2.5;

    private final Mob mob;

    public MobSeparationGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) {
            return false;
        }
        if (mob instanceof AetherBaseMob base && base.shouldSuppressCombatGoalsFromBehavior()) {
            return false;
        }
        return MobSeparationHelper.hasCrowd(mob);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        Vec3 offset = MobSeparationHelper.separationOffset(mob, MOVE_DISTANCE);
        if (offset.lengthSqr() < 1.0E-6) {
            return;
        }
        mob.getNavigation().moveTo(mob.getX() + offset.x, mob.getY(), mob.getZ() + offset.z, 1.3);
    }
}
