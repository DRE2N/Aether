package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import org.bukkit.event.entity.EntityTargetEvent;

public class ClearTargetAction extends AetherAction {

    @Override
    public void execute(BehaviorContext context) {
        if (context.mob().getTarget() != null) {
            context.mob().setCombatTarget(null, EntityTargetEvent.TargetReason.FORGOT_TARGET);
        }
    }
}

