package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import de.erethon.aether.ai.goals.custom.EnemyTeamTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class EnemyTeamTarget extends AEPathfinderGoal {

    private boolean checkVisibility;
    private boolean checkNavigation;
    private int range;

    public EnemyTeamTarget() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public Goal get(LivingEntity entity) {
        return new EnemyTeamTargetGoal((Mob) entity, checkVisibility, checkNavigation, range);
    }

    @Override
    public void load(String[] args) {
        checkVisibility = Boolean.parseBoolean(args[1]);
        checkNavigation = Boolean.parseBoolean(args[2]);
        range = Integer.parseInt(args[3]);
    }
}
