package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class RandomChanceCondition extends AetherCondition {

    private final double chancePercent;

    public RandomChanceCondition(double chancePercent) {
        this.chancePercent = chancePercent;
    }

    @Override
    public boolean check(BehaviorContext context) {
        return context.mob().getRandom().nextDouble() * 100 <= chancePercent;
    }
}

