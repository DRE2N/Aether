package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.world.entity.LivingEntity;

public class TargetDistanceLtCondition extends AetherCondition {

    private final double maxDistanceSquared;

    public TargetDistanceLtCondition(double maxDistance) {
        this.maxDistanceSquared = maxDistance * maxDistance;
    }

    @Override
    public boolean check(BehaviorContext context) {
        LivingEntity target = context.target();
        return target != null && context.mob().distanceToSqr(target) <= maxDistanceSquared;
    }
}

