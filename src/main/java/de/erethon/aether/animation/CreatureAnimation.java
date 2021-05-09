package de.erethon.aether.animation;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.events.CreatureAnimationPartFinishEvent;
import org.bukkit.Bukkit;

public abstract class CreatureAnimation {

    public abstract void animate(ActiveNPC npc);

    private void finish(ActiveNPC npc) {
        CreatureAnimationPartFinishEvent event = new CreatureAnimationPartFinishEvent(npc);
        Bukkit.getPluginManager().callEvent(event);
    }
}
