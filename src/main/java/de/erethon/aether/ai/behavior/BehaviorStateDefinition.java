package de.erethon.aether.ai.behavior;

import java.util.ArrayList;
import java.util.List;

public record BehaviorStateDefinition(
        String id,
        List<AetherAction> onEnter,
        List<AetherAction> onExit,
        List<AetherAction> onTick,
        List<AetherAction> onDamaged,
        List<AetherAction> onAttack,
        List<AetherAction> onTarget,
        List<AetherAction> onDeath,
        List<BehaviorTransitionDefinition> transitions
) {

    public BehaviorStateDefinition {
        onEnter = onEnter == null ? new ArrayList<>() : onEnter;
        onExit = onExit == null ? new ArrayList<>() : onExit;
        onTick = onTick == null ? new ArrayList<>() : onTick;
        onDamaged = onDamaged == null ? new ArrayList<>() : onDamaged;
        onAttack = onAttack == null ? new ArrayList<>() : onAttack;
        onTarget = onTarget == null ? new ArrayList<>() : onTarget;
        onDeath = onDeath == null ? new ArrayList<>() : onDeath;
        transitions = transitions == null ? new ArrayList<>() : transitions;
    }
}

