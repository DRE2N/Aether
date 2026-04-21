package de.erethon.aether.ai.behavior;

import java.util.ArrayList;
import java.util.List;

public record BehaviorTransitionDefinition(
        String toState,
        int priority,
        int minStateTicks,
        List<AetherCondition> conditions
) {

    public BehaviorTransitionDefinition {
        conditions = conditions == null ? new ArrayList<>() : conditions;
    }
}

