package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalLookAtPlayer;

public class LookAtPlayer extends AEPathfinderGoal {

    float lookDistance;
    float probability;

    public LookAtPlayer() {
        goalClass = GoalClass.LOOK;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalLookAtPlayer(entity, EntityHuman.class, lookDistance, probability);
    }

    @Override
    public void load(String[] args) {
        lookDistance = Float.parseFloat(args[0]);
        probability = Float.parseFloat(args[1]);
    }
}
