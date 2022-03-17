package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class RandomStrollGoal extends AEPathfinderGoal {

    double speed;
    int interval;
    boolean checkNoActionTime;

    public RandomStrollGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.RandomStrollGoal((PathfinderMob) entity, speed, interval, checkNoActionTime);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speed = Double.parseDouble(args[0]);
        interval = Integer.parseInt(args[1]);
        checkNoActionTime = Boolean.parseBoolean(args[2]);
    }
}
