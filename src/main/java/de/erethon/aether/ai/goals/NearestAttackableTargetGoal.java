package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Bukkit;

import java.util.Arrays;
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
        Optional<EntityType<?>> byName = Registry.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(args[0].toLowerCase()));
        if (byName.isPresent()) {
            EntityType<?> entityType = byName.get();
            target = entityType.getClass();
        }
        alertSameTypeMobs = Boolean.parseBoolean(args[1]);
    }
}
