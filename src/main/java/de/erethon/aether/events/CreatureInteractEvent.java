package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CreatureInteractEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final ActiveNPC npc;

    public CreatureInteractEvent(ActiveNPC npc) {
        this.npc = npc;
    }

    public ActiveNPC getNpc() {
        return npc;
    }

    public String getID() {
        return npc.getNpc().getID();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}

