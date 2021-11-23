package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.InstancedNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class InstancedCreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final InstancedNPC npc;

    public InstancedCreatureDeathEvent(InstancedNPC npc) {
        this.npc = npc;
    }

    public InstancedNPC getNpc() {
        return npc;
    }

    public Set<Player> getViewers() {
        return npc.getViewers();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
