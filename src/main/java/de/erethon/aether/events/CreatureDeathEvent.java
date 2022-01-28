package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.InstancedNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final ActiveNPC npc;
    private final Player killer;

    public CreatureDeathEvent(ActiveNPC npc, Player killer) {
        this.npc = npc;
        this.killer = killer;
    }

    public ActiveNPC getNpc() {
        return npc;
    }

    public Player getKiller() {
        return killer;
    } // can be null

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
