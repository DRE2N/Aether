package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class SetCooldownAction extends AetherAction {

    private final String key;
    private final int ticks;

    public SetCooldownAction(String key, int ticks) {
        this.key = key;
        this.ticks = ticks;
    }

    @Override
    public void execute(BehaviorContext context) {
        context.setCooldown(key, ticks);
    }
}

