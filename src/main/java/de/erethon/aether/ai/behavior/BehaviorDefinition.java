package de.erethon.aether.ai.behavior;

import java.util.LinkedHashMap;
import java.util.Map;

public record BehaviorDefinition(
        int tickInterval,
        String initialState,
        Map<String, BehaviorStateDefinition> states
) {

    public BehaviorDefinition {
        states = states == null ? new LinkedHashMap<>() : states;
    }
}

