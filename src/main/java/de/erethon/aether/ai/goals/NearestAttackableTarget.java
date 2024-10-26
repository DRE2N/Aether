package de.erethon.aether.ai.goals;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.GoalClass;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Optional;

public class NearestAttackableTarget extends AEPathfinderGoal {

    Class target;
    boolean checkVisibility;

    public NearestAttackableTarget() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>((Mob) entity, target, checkVisibility, false);
    }

    @Override
    public void load(String[] args) {
        Optional<EntityType<?>> byName = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse("minecraft:" + args[0].toLowerCase()));
        if (byName.isPresent()) {
            EntityType<?> entityType = byName.get();
            target = entityType.getClass();
        } else {
            Aether.addException("NearestAttackableTargetGoal",  "Could not find entity type " + args[0], "Please check your entity type name.", null);
            return;
        }
        checkVisibility = Boolean.parseBoolean(args[1]);
    }
}
