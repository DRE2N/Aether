package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.InstancedNPC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final ActiveNPC npc;

    public CreatureDeathEvent(InstancedNPC npc) {
        this.npc = npc;
    }

    public ActiveNPC getNpc() {
        return npc;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
