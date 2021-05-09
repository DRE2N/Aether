package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalNearestAttackableTarget;

import java.util.Optional;

public class NearestAttackableTargetGoal extends AEPathfinderGoal {

    Class target;
    boolean alertSameTypeMobs;

    public NearestAttackableTargetGoal() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalNearestAttackableTarget(entity, target, alertSameTypeMobs);
    }

    @Override
    public void load(String[] args) {
        Optional<EntityTypes<?>> byName = EntityTypes.getByName(args[0]);
        if (byName.isPresent()) {
            EntityTypes<?> entityType = byName.get();
            target = entityType.getClass();
        }
        alertSameTypeMobs = Boolean.parseBoolean(args[1]);
    }
}
