package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.aether.qxl.AetherHolder;
import org.bukkit.entity.Player;

public class QxlVisibilityCondition extends AetherCondition {

    @Override
    public boolean check(BehaviorContext context) {
        AetherHolder holder = context.holder();
        if (holder == null) {
            return false;
        }
        Player reference = context.lastAttacker();
        return reference != null && holder.checkVisibilityConditions(reference);
    }
}

