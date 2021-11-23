package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class LeapAtTargetGoal extends AEPathfinderGoal {

    float yd;

    public LeapAtTargetGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal((Mob) entity, 2);
    }

    @Override
    public void load(String[] args) {
        yd = Float.parseFloat(args[0]);
    }
}
