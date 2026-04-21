package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class SetTargetLastAttackerAction extends AetherAction {

    @Override
    public void execute(BehaviorContext context) {
        Player attacker = context.lastAttacker();
        if (attacker instanceof CraftPlayer craftPlayer) {
            context.mob().setTarget(craftPlayer.getHandle());
        }
    }
}

