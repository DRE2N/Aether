package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.*;

public class RandomSwimGoal extends AEPathfinderGoal {

    double speed;
    int interval;

    public RandomSwimGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalRandomSwim((EntityCreature) entity, speed, interval);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speed = Double.parseDouble(args[0]);
        interval = Integer.parseInt(args[1]);
    }
}
