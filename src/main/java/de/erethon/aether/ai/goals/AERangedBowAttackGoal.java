package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;

/**
 * Bow goal that yields to behavior retreat/kite states ({@code move_away_from_attacker} on {@code onTick}).
 */
public class AERangedBowAttackGoal extends net.minecraft.world.entity.ai.goal.RangedBowAttackGoal<AetherBaseMob> {

    private final AetherBaseMob aetherMob;

    public AERangedBowAttackGoal(AetherBaseMob mob, double speedModifier, int attackIntervalMin, float attackRadiusSqr) {
        super(mob, speedModifier, attackIntervalMin, attackRadiusSqr);
        this.aetherMob = mob;
    }

    @Override
    public boolean canUse() {
        if (aetherMob.shouldSuppressCombatGoalsFromBehavior()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (aetherMob.shouldSuppressCombatGoalsFromBehavior()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
