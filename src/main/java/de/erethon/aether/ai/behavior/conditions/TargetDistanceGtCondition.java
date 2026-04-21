package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.world.entity.LivingEntity;

public class TargetDistanceGtCondition extends AetherCondition {

    private final double minDistanceSquared;

    public TargetDistanceGtCondition(double minDistance) {
        this.minDistanceSquared = minDistance * minDistance;
    }

    @Override
    public boolean check(BehaviorContext context) {
        LivingEntity target = context.target();
        return target == null || context.mob().distanceToSqr(target) >= minDistanceSquared;
    }
}

