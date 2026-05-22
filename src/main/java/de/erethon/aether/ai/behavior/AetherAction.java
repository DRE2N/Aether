package de.erethon.aether.ai.behavior;

public abstract class AetherAction {

    /**
     * When true, runs before vanilla/Aether goals in the same tick so movement and targeting
     * are ready for {@link de.erethon.aether.ai.goals.AEMeleeAttackGoal} and similar goals.
     * Spell casts and retreat movement run after goals so they do not clobber melee attacks.
     */
    public boolean runsBeforeGoals() {
        return false;
    }

    public abstract void execute(BehaviorContext context);
}

