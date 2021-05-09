package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalHurtByTarget;

public class HurtByTargetGoal extends AEPathfinderGoal {

    public HurtByTargetGoal() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalHurtByTarget((EntityCreature) entity);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
    }
}
