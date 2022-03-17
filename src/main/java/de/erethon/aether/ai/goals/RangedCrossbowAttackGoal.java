package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;

public class RangedCrossbowAttackGoal extends AEPathfinderGoal {

    int attackIntervalMin;
    float attackRadiusSquare;

    public RangedCrossbowAttackGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal<>((Monster & CrossbowAttackMob) entity, attackIntervalMin, attackRadiusSquare);
    }

    @Override
    public void load(String[] args) {
        isMonsterOnly = true;
        attackIntervalMin = Integer.parseInt(args[0]);
        attackRadiusSquare = Float.parseFloat(args[1]);
    }
}
