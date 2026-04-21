package de.erethon.aether.ai.behavior.conditions;

import de.erethon.aether.ai.behavior.AetherCondition;
import de.erethon.aether.ai.behavior.BehaviorContext;
import org.bukkit.entity.Player;

public class AttackerDistanceLtCondition extends AetherCondition {

    private final double maxDistanceSquared;

    public AttackerDistanceLtCondition(double maxDistance) {
        this.maxDistanceSquared = maxDistance * maxDistance;
    }

    @Override
    public boolean check(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        return attacker != null && attacker.getLocation().distanceSquared(context.mob().getBukkitEntity().getLocation()) <= maxDistanceSquared;
    }
}

