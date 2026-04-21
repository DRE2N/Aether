package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import org.bukkit.entity.Player;

public class AttackerDistanceGtCondition extends AetherCondition {

    private final double minDistanceSquared;

    public AttackerDistanceGtCondition(double minDistance) {
        this.minDistanceSquared = minDistance * minDistance;
    }

    @Override
    public boolean check(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        return attacker == null || attacker.getLocation().distanceSquared(context.mob().getBukkitEntity().getLocation()) >= minDistanceSquared;
    }
}

