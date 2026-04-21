package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class HasTargetCondition extends AetherCondition {

    @Override
    public boolean check(BehaviorContext context) {
        return context.target() != null;
    }
}

