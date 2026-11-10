package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.aether.combat.BossBreakbar;

/** Opens the authored control window on a boss state transition or cast windup. */
public final class OpenBreakbarAction extends AetherAction {

    @Override
    public void execute(BehaviorContext context) {
        BossBreakbar bar = context.mob().getBossBreakbar();
        if (bar != null && !bar.isActive() && !bar.isBroken()) bar.openWindow();
    }
}
