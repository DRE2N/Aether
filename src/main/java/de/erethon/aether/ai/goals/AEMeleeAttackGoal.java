package de.erethon.aether.ai.goals;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Melee attack goal that paths toward the target until {@link MeleeReachUtil} says to stop chasing.
 */
public class AEMeleeAttackGoal extends Goal {

    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private int ticksUntilNextAttack;

    public AEMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (shouldDeferToBehavior()) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean shouldDeferToBehavior() {
        return this.mob instanceof AetherBaseMob base && base.shouldSuppressCombatGoalsFromBehavior();
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);

        boolean canSee = this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target);
        if (MeleeReachUtil.shouldKeepChasingForMelee(this.mob, target) || !canSee) {
            Vec3 approach = computeApproachPosition(target);
            this.mob.getNavigation().moveTo(approach.x, approach.y, approach.z, this.speedModifier);
        } else if (MobSeparationHelper.hasCrowd(this.mob)) {
            Vec3 offset = MobSeparationHelper.separationOffset(this.mob, 1.0);
            this.mob.getNavigation().moveTo(
                    this.mob.getX() + offset.x,
                    this.mob.getY(),
                    this.mob.getZ() + offset.z,
                    this.speedModifier * 0.85
            );
        } else {
            this.mob.getNavigation().stop();
        }

        this.checkAndPerformAttack(target);
    }

    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            ServerLevel level = (ServerLevel) this.mob.level();
            this.mob.doHurtTarget(level, target);
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(20);
    }

    protected boolean canPerformAttack(LivingEntity target) {
        boolean canSee = this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target);
        return this.ticksUntilNextAttack <= 0
                && MeleeReachUtil.isWithinLenientAttackRange(this.mob, target)
                && canSee;
    }

    private Vec3 computeApproachPosition(LivingEntity target) {
        double dist = Math.max(0.001, this.mob.distanceTo(target));
        double nx = (this.mob.getX() - target.getX()) / dist;
        double nz = (this.mob.getZ() - target.getZ()) / dist;
        double stopAt = Math.max(0.1, MeleeReachUtil.approximateMeleeReach(this.mob, target) - MeleeReachUtil.CHASE_INSET_BLOCKS);

        double x = target.getX() + nx * stopAt;
        double z = target.getZ() + nz * stopAt;
        return new Vec3(x, target.getY(), z);
    }
}
