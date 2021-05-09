package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalRandomLookaround;

public class RandomLookAroundGoal extends AEPathfinderGoal {

    public RandomLookAroundGoal() {
        goalClass = GoalClass.LOOK;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalRandomLookaround(entity);
    }
}
