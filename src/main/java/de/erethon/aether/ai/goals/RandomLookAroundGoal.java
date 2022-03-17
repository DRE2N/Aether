package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class RandomLookAroundGoal extends AEPathfinderGoal {

    public RandomLookAroundGoal() {
        goalClass = GoalClass.LOOK;
    }

    @Override
    public Goal get(LivingEntity entity) {
            return new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal((Mob) entity);
    }
}
