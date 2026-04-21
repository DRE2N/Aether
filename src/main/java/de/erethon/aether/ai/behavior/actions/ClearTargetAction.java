package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class ClearTargetAction extends AetherAction {

    @Override
    public void execute(BehaviorContext context) {
        context.mob().setTarget(null);
    }
}

