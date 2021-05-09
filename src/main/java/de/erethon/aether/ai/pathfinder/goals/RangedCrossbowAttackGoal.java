package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.*;

public class RangedCrossbowAttackGoal extends AEPathfinderGoal {

    int attackIntervalMin;
    float attackRadiusSquare;

    public RangedCrossbowAttackGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalCrossbowAttack((EntityMonster) entity, attackIntervalMin, attackRadiusSquare);
    }

    @Override
    public void load(String[] args) {
        isMonsterOnly = true;
        attackIntervalMin = Integer.parseInt(args[0]);
        attackRadiusSquare = Float.parseFloat(args[1]);
    }
}
