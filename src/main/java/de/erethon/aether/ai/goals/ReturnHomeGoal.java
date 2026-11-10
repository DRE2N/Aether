package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.EnumSet;

/** Returns an anchored mob after it has chased beyond its home and the surrounding buffer. */
public final class ReturnHomeGoal extends Goal {

    private static final int CHASE_BUFFER = 8;
    private static final int MIN_LEASH_RADIUS = 12;

    private final AetherBaseMob mob;
    private boolean returning;
    private int nextPathAttempt;

    public ReturnHomeGoal(AetherBaseMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!mob.hasHome() || mob.shouldSuppressCombatGoalsFromBehavior()) {
            return false;
        }
        int radius = Math.max(MIN_LEASH_RADIUS, mob.getHomeRadius() + CHASE_BUFFER);
        return mob.distanceToSqr(Vec3.atBottomCenterOf(mob.getHomePosition())) > (double) radius * radius;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.hasHome() && !mob.shouldSuppressCombatGoalsFromBehavior() && !mob.isWithinHome()
                && mob.distanceToSqr(Vec3.atBottomCenterOf(mob.getHomePosition())) > 4;
    }

    @Override
    public void start() {
        returning = true;
        mob.setCombatTarget(null, EntityTargetEvent.TargetReason.FORGOT_TARGET);
        mob.setAggressive(false);
        mob.getNavigation().stop();
        nextPathAttempt = mob.tickCount;
    }

    @Override
    public void tick() {
        if (mob.tickCount < nextPathAttempt || !mob.getNavigation().isDone()) {
            return;
        }

        Vec3 home = Vec3.atBottomCenterOf(mob.getHomePosition());
        if (!mob.getNavigation().moveTo(home.x, home.y, home.z, 1.2)) {
            Vec3 waypoint = DefaultRandomPos.getPosTowards(mob, 12, 5, home, Math.PI / 2);
            if (waypoint != null) {
                mob.getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 1.2);
            }
        }
        nextPathAttempt = mob.tickCount + 40;
    }

    @Override
    public void stop() {
        returning = false;
        mob.getNavigation().stop();
    }

    public boolean blocksTargeting() {
        return returning || canUse();
    }
}
