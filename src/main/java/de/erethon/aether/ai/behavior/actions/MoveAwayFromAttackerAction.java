package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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
        if (!(context.mob() instanceof PathfinderMob mob)) {
            return;
        }

        LivingEntity fleeFrom = resolveFleeTarget(context);
        if (fleeFrom == null) {
            return;
        }

        double dx = mob.getX() - fleeFrom.getX();
        double dz = mob.getZ() - fleeFrom.getZ();
        double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double targetX = mob.getX() + (dx / len) * distance;
        double targetZ = mob.getZ() + (dz / len) * distance;
        mob.getNavigation().moveTo(targetX, mob.getY(), targetZ, speed);
    }

    private static LivingEntity resolveFleeTarget(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        if (attacker instanceof CraftPlayer craftPlayer) {
            return craftPlayer.getHandle();
        }
        return context.target();
    }
}
