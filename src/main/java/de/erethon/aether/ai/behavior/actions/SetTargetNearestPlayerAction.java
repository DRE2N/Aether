package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.aether.creature.PlayerCombatTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityTargetEvent;

public class SetTargetNearestPlayerAction extends AetherAction {

    private final double range;

    public SetTargetNearestPlayerAction(double range) {
        this.range = range;
    }

    @Override
    public boolean runsBeforeGoals() {
        return true;
    }

    @Override
    public void execute(BehaviorContext context) {
        Player nearest = context.mob().level().getNearestPlayer(context.mob().getX(), context.mob().getY(), context.mob().getZ(), range, false);
        if (nearest instanceof ServerPlayer serverPlayer) {
            PlayerCombatTracker.getInstance().evictExpired(serverPlayer.getUUID());
            if (serverPlayer != context.mob().getTarget()) {
                context.mob().setCombatTarget(serverPlayer, EntityTargetEvent.TargetReason.CLOSEST_PLAYER);
            }
        }
    }
}

