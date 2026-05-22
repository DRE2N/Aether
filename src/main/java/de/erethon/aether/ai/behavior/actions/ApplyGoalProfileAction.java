package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;

public class ApplyGoalProfileAction extends AetherAction {

    private final String profileId;

    public ApplyGoalProfileAction(String profileId) {
        this.profileId = profileId;
    }

    @Override
    public boolean runsBeforeGoals() {
        return true;
    }

    @Override
    public void execute(BehaviorContext context) {
        context.mob().applyGoalProfile(profileId);
    }
}

