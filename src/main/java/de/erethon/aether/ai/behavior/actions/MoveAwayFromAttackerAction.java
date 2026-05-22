package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.world.entity.PathfinderMob;
import org.bukkit.entity.Player;

public class MoveAwayFromAttackerAction extends AetherAction {

    private final double distance;
    private final double speed;

    public MoveAwayFromAttackerAction(double distance, double speed) {
        this.distance = distance;
        this.speed = speed;
    }

    @Override
    public boolean runsBeforeGoals() {
        return true;
    }

    @Override
    public void execute(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        if (attacker == null || !(context.mob() instanceof PathfinderMob mob)) {
            return;
        }
        double dx = mob.getX() - attacker.getLocation().getX();
        double dz = mob.getZ() - attacker.getLocation().getZ();
        double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double targetX = mob.getX() + (dx / len) * distance;
        double targetZ = mob.getZ() + (dz / len) * distance;
        mob.getNavigation().moveTo(targetX, mob.getY(), targetZ, speed);
    }
}
