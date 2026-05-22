package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class MobSeparationGoal extends Goal {

    private static final double SEPARATION_RADIUS = 1.8;
    private static final double MOVE_DISTANCE = 2.5;
    private static final int MIN_CROWD_COUNT = 2;
    /** Do not override chase/attack pathing while closing on the current target. */
    private static final double COMBAT_CHASE_RANGE = 20.0;

    private final Mob mob;

    public MobSeparationGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || getCrowdCount() < MIN_CROWD_COUNT) {
            return false;
        }
        return mob.distanceTo(mob.getTarget()) > COMBAT_CHASE_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        List<AetherBaseMob> neighbors = getNeighbors();
        if (neighbors.isEmpty()) return;

        double avgX = 0, avgZ = 0;
        for (AetherBaseMob neighbor : neighbors) {
            avgX += neighbor.getX();
            avgZ += neighbor.getZ();
        }
        avgX /= neighbors.size();
        avgZ /= neighbors.size();

        double dx = mob.getX() - avgX;
        double dz = mob.getZ() - avgZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= len;
            dz /= len;
        }

        double targetX = mob.getX() + dx * MOVE_DISTANCE;
        double targetZ = mob.getZ() + dz * MOVE_DISTANCE;
        mob.getNavigation().moveTo(targetX, mob.getY(), targetZ, 1.3);
    }

    private int getCrowdCount() {
        return getNeighbors().size();
    }

    private List<AetherBaseMob> getNeighbors() {
        AABB area = mob.getBoundingBox().inflate(SEPARATION_RADIUS);
        return mob.level().getEntitiesOfClass(AetherBaseMob.class, area, e -> e != mob);
    }
}
