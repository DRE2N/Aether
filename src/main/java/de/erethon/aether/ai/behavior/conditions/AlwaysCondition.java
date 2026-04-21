package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class AlwaysCondition extends AetherCondition {

    @Override
    public boolean check(BehaviorContext context) {
        return true;
    }
}

