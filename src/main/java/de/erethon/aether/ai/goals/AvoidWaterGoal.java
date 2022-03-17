package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class AvoidWaterGoal extends AEPathfinderGoal {

    double speed;
    int interval;
    boolean checkNoActionTime;

    public AvoidWaterGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new WaterAvoidingRandomStrollGoal((PathfinderMob) entity, speed, interval);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        speed = Double.parseDouble(args[0]);
        interval = Integer.parseInt(args[1]);
        checkNoActionTime = Boolean.parseBoolean(args[2]);
    }
}
