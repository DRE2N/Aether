package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class CreatureInteractEvent extends PlayerEvent {

    private static final HandlerList handlerList = new HandlerList();
    private final ActiveNPC npc;

    public CreatureInteractEvent(Player player, ActiveNPC npc) {
        super(player);
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

