package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class StateTimeAtLeastCondition extends AetherCondition {

    private final int minTicks;

    public StateTimeAtLeastCondition(int minTicks) {
        this.minTicks = Math.max(0, minTicks);
    }

    @Override
    public boolean check(BehaviorContext context) {
        return context.stateTicks() >= minTicks;
    }
}

