package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;

public class DoorInteractGoal extends AEPathfinderGoal {

    boolean closeDoor;

    public DoorInteractGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new OpenDoorGoal((Mob) entity, closeDoor);
    }

    @Override
    public void load(String[] args) {
        closeDoor = Boolean.parseBoolean(args[0]);
    }
}
