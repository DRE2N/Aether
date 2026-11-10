package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.ModelledMob;
import de.erethon.aether.creature.PlayerCombatTracker;
import de.erethon.aether.qxl.QxlInstanceScope;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.EnumSet;

/** Owns the approach, windup and recovery of an ordinary melee strike. */
public class AEMeleeAttackGoal extends Goal {

    private static final int WINDUP_TICKS = 10;
    private static final int RECOVERY_TICKS = 10;
    private static final double ATTACK_ARC_DOT = 0.5;

    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private LivingEntity committedTarget;
    private Vec3 attackDirection;
    private int strikeAt;
    private int recoverUntil;
    private int nextPathAttempt;
    private Vec3 lastPathTarget;

    public AEMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target)
                && (!(mob instanceof AetherBaseMob base) || !base.shouldSuppressCombatGoalsFromBehavior());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        lastPathTarget = null;
        nextPathAttempt = 0;
    }

    @Override
    public void stop() {
        cancelWindup();
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        int now = MinecraftServer.currentTick;
        if (committedTarget != null) {
            if (target != committedTarget || !canUse() || now > strikeAt + 1) {
                cancelWindup();
            } else if (now >= strikeAt) {
                resolveStrike(target);
            }
            return;
        }
        if (target == null || now < recoverUntil) {
            return;
        }

        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        boolean inReach = mob.isWithinMeleeAttackRange(target);
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (inReach && canSee && beginWindup(target)) {
            return;
        }

        if (!canSee && !followingTargetEvenIfNotSeen) {
            return;
        }
        if (!inReach || !canSee) {
            chase(target);
        } else {
            mob.getNavigation().stop();
            lastPathTarget = null;
            Vec3 offset = MobSeparationHelper.separationOffset(mob, 1.0);
            if (offset.lengthSqr() >= 1.0E-6) {
                mob.getMoveControl().setWantedPosition(mob.getX() + offset.x, mob.getY(),
                        mob.getZ() + offset.z, speedModifier * 0.85);
            }
        }
    }

    private boolean beginWindup(LivingEntity target) {
        if (target instanceof Player player && !PlayerCombatTracker.getInstance()
                .tryBeginWindup(player.getUUID(), mob.getUUID(), WINDUP_TICKS)) {
            return false;
        }

        committedTarget = target;
        attackDirection = horizontalDirectionTo(target);
        strikeAt = MinecraftServer.currentTick + WINDUP_TICKS;
        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
        mob.lookAt(target, 180.0F, 180.0F);
        var entity = mob.getBukkitEntity();
        var cueLocation = entity.getLocation().add(0, mob.getEyeHeight() + 0.25, 0);
        for (var viewer : entity.getTrackedBy()) {
            if (viewer.canSee(entity) && viewer.getLocation().distanceSquared(cueLocation) <= 24 * 24
                    && (!(mob instanceof AetherBaseMob base) || QxlInstanceScope.canInteract(base, viewer))) {
                viewer.playSound(entity, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.75F, 0.65F);
                viewer.spawnParticle(Particle.CRIT, cueLocation, 4, 0.15, 0.1, 0.15, 0);
            }
        }
        return true;
    }

    private void resolveStrike(LivingEntity target) {
        Vec3 direction = horizontalDirectionTo(target);
        boolean inArc = attackDirection.lengthSqr() == 0 || direction.lengthSqr() == 0
                || attackDirection.dot(direction) >= ATTACK_ARC_DOT;
        cancelWindup();
        mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        if (inArc && mob.isWithinMeleeAttackRange(target) && mob.getSensing().hasLineOfSight(target)
                && (!(mob instanceof AetherBaseMob base) || base.isValidCombatTarget(target))) {
            mob.doHurtTarget((ServerLevel) mob.level(), target);
        } else if (mob instanceof ModelledMob modelledMob) {
            modelledMob.playAttackAnimation();
        }
    }

    private void cancelWindup() {
        if (committedTarget == null) {
            return;
        }
        PlayerCombatTracker.getInstance().releaseWindup(mob.getUUID());
        committedTarget = null;
        recoverUntil = MinecraftServer.currentTick + RECOVERY_TICKS;
        lastPathTarget = null;
    }

    private void chase(LivingEntity target) {
        if (mob.tickCount < nextPathAttempt
                || (!mob.getNavigation().isDone() && lastPathTarget != null
                && lastPathTarget.distanceToSqr(target.position()) < 1.0)) {
            return;
        }

        Vec3 direction = horizontalDirectionTo(target);
        double stopAt = Math.max(0.1, MeleeReachUtil.approximateMeleeReach(mob, target) - MeleeReachUtil.CHASE_INSET_BLOCKS);
        Vec3 approach = target.position().subtract(direction.scale(stopAt));
        boolean foundPath = mob.getNavigation().moveTo(approach.x, approach.y, approach.z, speedModifier);
        lastPathTarget = target.position();
        nextPathAttempt = mob.tickCount + (foundPath ? 5 : 15) + Math.floorMod(mob.getId(), 4);
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        return new Vec3(target.getX() - mob.getX(), 0, target.getZ() - mob.getZ()).normalize();
    }
}
