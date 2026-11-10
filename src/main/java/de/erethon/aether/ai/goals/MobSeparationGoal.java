package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.server.MinecraftServer;
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
    private static final double SPEED_MODIFIER = 1.3;
    private static final int CHECK_INTERVAL = 10;

    private final Mob mob;
    private Vec3 wantedPosition = Vec3.ZERO;

    public MobSeparationGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (Math.floorMod(MinecraftServer.currentTick + mob.getId(), CHECK_INTERVAL) != 0) {
            return false;
        }
        if (mob.getTarget() != null) {
            return false;
        }
        if (mob instanceof AetherBaseMob base && base.shouldSuppressCombatGoalsFromBehavior()) {
            return false;
        }

        Vec3 offset = MobSeparationHelper.separationOffset(mob, MOVE_DISTANCE);
        if (offset.lengthSqr() < 1.0E-6) {
            return false;
        }
        wantedPosition = mob.position().add(offset);
        return true;
    }

    @Override
    public void start() {
        mob.getMoveControl().setWantedPosition(wantedPosition.x, wantedPosition.y, wantedPosition.z, SPEED_MODIFIER);
    }
}
