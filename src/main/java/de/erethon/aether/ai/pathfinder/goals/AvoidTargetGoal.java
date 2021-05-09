package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.*;

import java.util.Optional;

public class AvoidTargetGoal extends AEPathfinderGoal {

    double walkSpeedModifier;
    double sprintSpeedModifier;
    float maxDist;
    Class toAvoid;

    public AvoidTargetGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalAvoidTarget((EntityCreature) entity, toAvoid, maxDist, walkSpeedModifier, sprintSpeedModifier);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        Optional<EntityTypes<?>> byName = EntityTypes.getByName(args[0]);
        if (byName.isPresent()) {
            EntityTypes<?> entityType = byName.get();
            toAvoid = entityType.getClass();
        }
        maxDist = Float.parseFloat(args[1]);
        walkSpeedModifier = Double.parseDouble(args[2]);
        sprintSpeedModifier = Double.parseDouble(args[3]);

    }
}
