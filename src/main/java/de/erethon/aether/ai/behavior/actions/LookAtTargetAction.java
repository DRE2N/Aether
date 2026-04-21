package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import net.minecraft.world.entity.LivingEntity;

public class LookAtTargetAction extends AetherAction {

    private final float maxYaw;
    private final float maxPitch;

    public LookAtTargetAction(float maxYaw, float maxPitch) {
        this.maxYaw = maxYaw;
        this.maxPitch = maxPitch;
    }

    @Override
    public void execute(BehaviorContext context) {
        LivingEntity target = context.target();
        if (target != null) {
            context.mob().lookAt(target, maxYaw, maxPitch);
        }
    }
}

