package de.erethon.aether.events;

import de.erethon.aether.creature.InstancedNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class InstancedCreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final InstancedNPC npc;
    private final Player killer;

    public InstancedCreatureDeathEvent(InstancedNPC npc, Player killer) {
        this.npc = npc;
        this.killer = killer;
    }

    public InstancedNPC getNpc() {
        return npc;
    }

    public Set<Player> getViewers() {
        return npc.getViewers();
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
