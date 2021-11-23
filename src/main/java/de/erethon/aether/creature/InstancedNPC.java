package de.erethon.aether.creature;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class InstancedNPC extends ActiveNPC {

    Set<Player> viewers = new HashSet<>();

    public InstancedNPC(NPCData npcData, Player... viewers) {
        super(npcData);
        this.viewers.addAll(Arrays.asList(viewers));
    }

    public InstancedNPC(NPCData npcData, Entity entity, Player... viewers) {
        super(npcData);
        this.viewers.addAll(Arrays.asList(viewers));
    }

    public void addViewer(Player player) {
        viewers.add(player);
    }

    public void removeViewer(Player player) {
        viewers.remove(player);
    }

    public Set<Player> getViewers() {
        return viewers;
    }
}
