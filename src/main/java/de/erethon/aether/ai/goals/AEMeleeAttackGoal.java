package de.erethon.aether.ai.goals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Melee attack goal that keeps the mob at STANDOFF_DISTANCE from the target
 * rather than pushing all the way into it like the vanilla goal does.
 */
public class AEMeleeAttackGoal extends Goal {

    private static final double STANDOFF_DISTANCE = 0.75d;
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;

    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private long lastCanUseCheck;

    public AEMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long time = this.mob.level().getGameTime();
        if (time - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }
        this.lastCanUseCheck = time;
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        this.path = this.mob.getNavigation().createPath(target, 0);
        return this.path != null || this.mob.isWithinMeleeAttackRange(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) return false;
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        LivingEntity target = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.mob.setTarget(null);
        }
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
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

        boolean hasLos = this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target);
        boolean targetMoved = target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0;
        boolean shouldRecalc = this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0
                || targetMoved
                || this.mob.getRandom().nextFloat() < 0.05F;

        if (hasLos && this.ticksUntilNextPathRecalculation <= 0 && shouldRecalc) {
            this.pathedTargetX = target.getX();
            this.pathedTargetY = target.getY();
            this.pathedTargetZ = target.getZ();
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);

            double distSqr = this.mob.distanceToSqr(target);
            if (distSqr > 1024.0) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (distSqr > 256.0) {
                this.ticksUntilNextPathRecalculation += 5;
            }

            double dist = Math.sqrt(distSqr);
            if (dist > STANDOFF_DISTANCE) {
                // Navigate to a position STANDOFF_DISTANCE from the target, along the mob→target axis
                double nx = (this.mob.getX() - target.getX()) / dist;
                double nz = (this.mob.getZ() - target.getZ()) / dist;
                double standoffX = target.getX() + nx * STANDOFF_DISTANCE;
                double standoffZ = target.getZ() + nz * STANDOFF_DISTANCE;
                if (!this.mob.getNavigation().moveTo(standoffX, target.getY(), standoffZ, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
            } else {
                this.mob.getNavigation().stop();
            }

            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(target);
    }

    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(getServerLevel(this.mob), target);
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(20);
    }

    protected boolean canPerformAttack(LivingEntity target) {
        return this.ticksUntilNextAttack <= 0
                && this.mob.isWithinMeleeAttackRange(target)
                && this.mob.getSensing().hasLineOfSight(target);
    }
}
