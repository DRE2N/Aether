package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Optional;

public class NearestAttackableTargetGoal extends AEPathfinderGoal {

    Class target;
    boolean alertSameTypeMobs;

    public NearestAttackableTargetGoal() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>((Mob) entity, target, alertSameTypeMobs);
    }

    @Override
    public void load(String[] args) {
        Optional<EntityType<?>> byName = EntityType.byString(args[0]);
        if (byName.isPresent()) {
            EntityType<?> entityType = byName.get();
            target = entityType.getClass();
        }
        alertSameTypeMobs = Boolean.parseBoolean(args[1]);
    }
}
