package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class HurtByTarget extends AEPathfinderGoal {

    public HurtByTarget() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal((PathfinderMob) entity);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
    }
}
