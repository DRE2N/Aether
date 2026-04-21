package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SetTargetNearestPlayerAction extends AetherAction {

    private final double range;

    public SetTargetNearestPlayerAction(double range) {
        this.range = range;
    }

    @Override
    public void execute(BehaviorContext context) {
        Player nearest = context.mob().level().getNearestPlayer(context.mob().getX(), context.mob().getY(), context.mob().getZ(), range, false);
        if (nearest instanceof ServerPlayer serverPlayer) {
            context.mob().setTarget(serverPlayer);
        }
    }
}

