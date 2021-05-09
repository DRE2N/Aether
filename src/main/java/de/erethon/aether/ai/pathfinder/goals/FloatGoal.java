package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalFloat;

public class FloatGoal extends AEPathfinderGoal {

    public FloatGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalFloat(entity);
    }

}
