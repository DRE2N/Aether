package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

public class SetTargetLastAttackerAction extends AetherAction {

    @Override
    public boolean runsBeforeGoals() {
        return true;
    }

    @Override
    public void execute(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        if (attacker instanceof CraftPlayer craftPlayer) {
            LivingEntity handle = craftPlayer.getHandle();
            if (handle != context.mob().getTarget()) {
                context.mob().setCombatTarget(handle, EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY);
            }
        }
    }
}

