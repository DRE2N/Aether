package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityCreature;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalMeleeAttack;

public class MeleeAttackGoal extends AEPathfinderGoal {

    double speedModifier;
    boolean followTargetIfNotSeen;

    public MeleeAttackGoal() {
        goalClass = GoalClass.LOOK;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalMeleeAttack((EntityCreature) entity, speedModifier, followTargetIfNotSeen);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speedModifier = Double.parseDouble(args[0]);
        followTargetIfNotSeen = Boolean.parseBoolean(args[1]);
    }
}
