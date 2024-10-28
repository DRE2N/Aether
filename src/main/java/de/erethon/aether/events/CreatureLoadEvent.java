package de.erethon.aether.events;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CreatureLoadEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final NPCData data;
    private final AetherBaseMob mob;

    public CreatureLoadEvent(NPCData data, AetherBaseMob mob) {
        super(!Bukkit.isPrimaryThread());
        this.data = data;
        this.mob = mob;
    }

    public NPCData getData() {
        return data;
    }

    public AetherBaseMob getMob() {
        return mob;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

}
