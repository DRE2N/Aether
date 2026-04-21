package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class CooldownReadyCondition extends AetherCondition {

    private final String key;

    public CooldownReadyCondition(String key) {
        this.key = key;
    }

    @Override
    public boolean check(BehaviorContext context) {
        return context.isCooldownReady(key);
    }
}

