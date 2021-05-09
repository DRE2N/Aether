package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.InstancedNPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InstancedCreatureArriveAtPointEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final InstancedNPC npc;
    private final Location point;

    public InstancedCreatureArriveAtPointEvent(InstancedNPC npc, Location point) {
        this.npc = npc;
        this.point = point;
    }

    public InstancedNPC getNpc() {
        return npc;
    }

    public List<Player> getViewers() {
        return npc.getViewers();
    }

    public Location getPoint() {
        return point;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
