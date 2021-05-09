package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalRandomStroll;

public class RandomStrollGoal extends AEPathfinderGoal {

    double speed;
    int interval;
    boolean checkNoActionTime;

    public RandomStrollGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalRandomStroll((EntityCreature) entity, speed, interval, checkNoActionTime);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speed = Double.parseDouble(args[0]);
        interval = Integer.parseInt(args[1]);
        checkNoActionTime = Boolean.parseBoolean(args[2]);
    }
}
