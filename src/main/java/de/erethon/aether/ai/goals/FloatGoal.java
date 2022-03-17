package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class FloatGoal extends AEPathfinderGoal {

    public FloatGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.FloatGoal((Mob) entity);
    }

}
