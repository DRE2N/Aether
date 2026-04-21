package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.aether.qxl.AetherHolder;

public class QxlSpawnCondition extends AetherCondition {

    @Override
    public boolean check(BehaviorContext context) {
        AetherHolder holder = context.holder();
        return holder != null && holder.checkSpawnConditions();
    }
}

