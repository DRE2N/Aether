package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalRestrictSun;

public class RestrictSunGoal extends AEPathfinderGoal {

    public RestrictSunGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalRestrictSun((EntityCreature) entity);
    }

    @Override
    public void load(String[] args) {
        isMonsterOnly = true;
    }
}
