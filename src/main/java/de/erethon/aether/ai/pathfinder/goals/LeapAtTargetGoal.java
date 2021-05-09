package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalLeapAtTarget;

public class LeapAtTargetGoal extends AEPathfinderGoal {

    float yd;

    public LeapAtTargetGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalLeapAtTarget(entity, 2);
    }

    @Override
    public void load(String[] args) {
        yd = Float.parseFloat(args[0]);
    }
}
