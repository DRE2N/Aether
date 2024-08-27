package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.InstancedNPC;
import de.erethon.aether.creature.NPCData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final NPCData npc;
    private final Player killer;
    private final AetherBaseMob mob;

    public CreatureDeathEvent(NPCData npc, Player killer, AetherBaseMob mob) {
        this.npc = npc;
        this.killer = killer;
        this.mob = mob;
    }

    public NPCData getNpc() {
        return npc;
    }

    public Player getKiller() {
        return killer;
    } // can be null

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
