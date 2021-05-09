package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalDoorOpen;

public class DoorInteractGoal extends AEPathfinderGoal {

    boolean closeDoor;

    public DoorInteractGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalDoorOpen(entity, closeDoor);
    }

    @Override
    public void load(String[] args) {
        closeDoor = Boolean.parseBoolean(args[0]);
    }
}
