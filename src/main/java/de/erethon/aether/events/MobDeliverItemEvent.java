package de.erethon.aether.events;

import de.erethon.hephaestus.items.HItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MobDeliverItemEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private Player player;
    private String mobID;
    private HItem item;
    private int amount;

    public MobDeliverItemEvent(Player player, String mobID, HItem item, int amount) {
        this.player = player;
        this.mobID = mobID;
        this.item = item;
        this.amount = amount;
    }

    public HItem getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Player getPlayer() {
        return player;
    }

    public String getMobID() {
        return mobID;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
