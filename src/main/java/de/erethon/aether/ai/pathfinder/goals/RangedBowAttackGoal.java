package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class RangedBowAttackGoal extends AEPathfinderGoal {

    double speedMod;
    int attackIntervalMin;
    float attackRadiusSquare;

    public RangedBowAttackGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new net.minecraft.world.entity.ai.goal.RangedBowAttackGoal<>((Monster & RangedAttackMob) entity, speedMod, attackIntervalMin, attackRadiusSquare);
    }

    @Override
    public void load(String[] args) {
        isMonsterOnly = true;
        speedMod = Double.parseDouble(args[0]);
        attackIntervalMin = Integer.parseInt(args[1]);
        attackRadiusSquare = Float.parseFloat(args[2]);
    }
}
