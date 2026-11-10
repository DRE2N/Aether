package de.erethon.aether.events;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class CreatureDeathEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final NPCData npc;
    private final LivingEntity killer;
    private final AetherBaseMob mob;
    private final Set<UUID> contributors;

    public CreatureDeathEvent(NPCData npc, @Nullable LivingEntity killer, AetherBaseMob mob) {
        this(npc, killer, mob, Set.of());
    }

    public CreatureDeathEvent(NPCData npc, @Nullable LivingEntity killer, AetherBaseMob mob, Set<UUID> contributors) {
        this.npc = npc;
        this.killer = killer;
        this.mob = mob;
        this.contributors = Set.copyOf(contributors);
    }

    public Set<UUID> getContributors() {
        return contributors;
    }

    public NPCData getNpc() {
        return npc;
    }

    public @Nullable LivingEntity getKiller() {
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
