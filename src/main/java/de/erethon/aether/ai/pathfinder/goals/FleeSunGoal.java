package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalFleeSun;

public class FleeSunGoal extends AEPathfinderGoal {

    double speed;

    public FleeSunGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalFleeSun((EntityCreature) entity, speed);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speed = Double.parseDouble(args[0]);
    }
}
