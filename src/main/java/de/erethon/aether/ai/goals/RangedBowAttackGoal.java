package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import de.erethon.aether.creature.AetherBaseMob;

public class RangedBowAttackGoal extends AEPathfinderGoal {

    double speedMod;
    int attackIntervalMin;
    float attackRadiusSquare;

    public RangedBowAttackGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new AERangedBowAttackGoal((AetherBaseMob) entity, speedMod, attackIntervalMin, attackRadiusSquare);
    }

    @Override
    public void load(String[] args) {
        isMonsterOnly = true;
        speedMod = Double.parseDouble(args[0]);
        attackIntervalMin = Integer.parseInt(args[1]);
        attackRadiusSquare = Float.parseFloat(args[2]);
    }
}
