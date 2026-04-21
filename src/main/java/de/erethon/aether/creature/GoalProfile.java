package de.erethon.aether.creature;

import de.erethon.aether.ai.goals.AEPathfinderGoal;

import java.util.HashSet;
import java.util.Set;

public record GoalProfile(
        Set<AEPathfinderGoal> goals,
        Set<AEPathfinderGoal> targets
) {

    public GoalProfile {
        goals = goals == null ? new HashSet<>() : goals;
        targets = targets == null ? new HashSet<>() : targets;
    }
}

