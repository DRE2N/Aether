package de.erethon.aether.ai.pathfinder.goals;

import de.erethon.aether.ai.pathfinder.GoalClass;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.PathfinderGoal;

public abstract class AEPathfinderGoal {

    boolean isCreatureOnly = false;
    boolean isMonsterOnly = false;
    public GoalClass goalClass;
    int prio;

    public PathfinderGoal get(EntityInsentient entity) {
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

