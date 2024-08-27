package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class CreatureInteractEvent extends PlayerEvent {

    private static final HandlerList handlerList = new HandlerList();
    private final AetherBaseMob mob;
    private final NPCData data;

    public CreatureInteractEvent(Player player, AetherBaseMob mob, NPCData data) {
        super(player);
        this.mob = mob;
        this.data = data;
    }

    public AetherBaseMob getMob() {
        return mob;
    }

    public NPCData getData() {
        return data;
    }

    public String getID() {
        return mob.getData().getID();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}

