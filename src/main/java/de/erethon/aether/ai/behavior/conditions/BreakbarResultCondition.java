package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.aether.combat.BossBreakbar;

/** Lets an authored boss state branch to its broken or missed-cast response. */
public final class BreakbarResultCondition extends AetherCondition {

    private final boolean broken;

    public BreakbarResultCondition(boolean broken) {
        this.broken = broken;
    }

    @Override
    public boolean check(BehaviorContext context) {
        BossBreakbar bar = context.mob().getBossBreakbar();
        return bar != null && (broken ? bar.isBroken() : bar.hasFailed());
    }
}
