package de.erethon.aether.events;

import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.InstancedNPC;
import de.erethon.aether.creature.NPCData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final NPCData npc;
    private final LivingEntity killer;
    private final AetherBaseMob mob;

    public CreatureDeathEvent(NPCData npc, LivingEntity killer, AetherBaseMob mob) {
        this.npc = npc;
        this.killer = killer;
        this.mob = mob;
    }

    public NPCData getNpc() {
        return npc;
    }

    public LivingEntity getKiller() {
        return killer;
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
