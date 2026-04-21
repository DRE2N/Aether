package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import org.bukkit.entity.Player;

public class MoveAwayFromAttackerAction extends AetherAction {

    private final double distance;
    private final double speed;

    public MoveAwayFromAttackerAction(double distance, double speed) {
        this.distance = distance;
        this.speed = speed;
    }

    @Override
    public void execute(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        if (attacker == null) {
            return;
        }
        double dx = context.mob().getX() - attacker.getLocation().getX();
        double dz = context.mob().getZ() - attacker.getLocation().getZ();
        double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double targetX = context.mob().getX() + (dx / len) * distance;
        double targetZ = context.mob().getZ() + (dz / len) * distance;
        context.mob().getMoveControl().setWantedPosition(targetX, context.mob().getY(), targetZ, speed);
    }
}

