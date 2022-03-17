package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class FleeSunGoal extends AEPathfinderGoal {

    double speed;

    public FleeSunGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.FleeSunGoal((PathfinderMob) entity, speed);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speed = Double.parseDouble(args[0]);
    }
}
