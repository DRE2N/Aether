package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Optional;

public class AEAvoidTargetGoal extends AEPathfinderGoal {

    double walkSpeedModifier;
    double sprintSpeedModifier;
    float maxDist;
    Class toAvoid;

    public AEAvoidTargetGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new AvoidEntityGoal<>((PathfinderMob) entity, toAvoid, maxDist, walkSpeedModifier, sprintSpeedModifier);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        Optional<EntityType<?>> byName = Registry.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(args[0].toLowerCase()));
        if (byName.isPresent()) {
            EntityType<?> entityType = byName.get();
            toAvoid = entityType.getClass();
        }
        maxDist = Float.parseFloat(args[1]);
        walkSpeedModifier = Double.parseDouble(args[2]);
        sprintSpeedModifier = Double.parseDouble(args[3]);

    }
}
