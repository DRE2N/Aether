package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class HealthAbovePercentCondition extends AetherCondition {

    private final double threshold;

    public HealthAbovePercentCondition(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean check(BehaviorContext context) {
        return context.healthPercent() >= threshold;
    }
}

