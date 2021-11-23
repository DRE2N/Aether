package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class MeleeAttackGoal extends AEPathfinderGoal {

    double speedModifier;
    boolean followTargetIfNotSeen;

    public MeleeAttackGoal() {
        goalClass = GoalClass.LOOK;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.MeleeAttackGoal((PathfinderMob) entity, speedModifier, followTargetIfNotSeen);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speedModifier = Double.parseDouble(args[0]);
        followTargetIfNotSeen = Boolean.parseBoolean(args[1]);
    }
}
