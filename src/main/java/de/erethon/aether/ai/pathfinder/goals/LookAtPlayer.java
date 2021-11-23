package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;

public class LookAtPlayer extends AEPathfinderGoal {

    float lookDistance;
    float probability;

    public LookAtPlayer() {
        goalClass = GoalClass.LOOK;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new LookAtPlayerGoal((Mob) entity, Player.class, lookDistance, probability);
    }

    @Override
    public void load(String[] args) {
        lookDistance = Float.parseFloat(args[0]);
        probability = Float.parseFloat(args[1]);
    }
}
