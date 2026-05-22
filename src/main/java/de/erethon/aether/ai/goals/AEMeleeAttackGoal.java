package de.erethon.aether.ai.goals;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Melee attack goal that paths toward the target until {@link Mob#isWithinMeleeAttackRange} is true.
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

        if (this.mob.isWithinMeleeAttackRange(target)) {
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().moveTo(target, this.speedModifier);
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
                && this.mob.isWithinMeleeAttackRange(target)
                && canSee;
    }
}
