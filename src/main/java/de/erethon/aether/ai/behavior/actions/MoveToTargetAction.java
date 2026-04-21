package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.world.entity.LivingEntity;

public class MoveToTargetAction extends AetherAction {

    private final double speed;

    public MoveToTargetAction(double speed) {
        this.speed = speed;
    }

    @Override
    public void execute(BehaviorContext context) {
        LivingEntity target = context.target();
        if (target == null) {
            return;
        }
        context.mob().getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
    }
}

