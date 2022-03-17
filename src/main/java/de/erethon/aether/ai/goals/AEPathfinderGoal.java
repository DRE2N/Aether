package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public abstract class AEPathfinderGoal {

    boolean isCreatureOnly = false;
    boolean isMonsterOnly = false;
    public GoalClass goalClass;
    int prio;

    public Goal get(LivingEntity entity) {
        return null;
    }

    public int getPrio() {
        return prio;
    }

    public void setPrio(int i) {
        prio = i;
    }

    public void load(String[] args) {
    }
}

