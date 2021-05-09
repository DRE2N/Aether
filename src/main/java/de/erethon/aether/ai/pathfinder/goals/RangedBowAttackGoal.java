package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityMonster;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalBowShoot;

public class RangedBowAttackGoal extends AEPathfinderGoal {

    double speedMod;
    int attackIntervalMin;
    float attackRadiusSquare;

    public RangedBowAttackGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public PathfinderGoal get(EntityInsentient entity) {
        return new PathfinderGoalBowShoot((EntityMonster) entity, speedMod, attackIntervalMin, attackRadiusSquare);
    }

    @Override
    public void load(String[] args) {
        isMonsterOnly = true;
        speedMod = Double.parseDouble(args[0]);
        attackIntervalMin = Integer.parseInt(args[1]);
        attackRadiusSquare = Float.parseFloat(args[2]);
    }
}
