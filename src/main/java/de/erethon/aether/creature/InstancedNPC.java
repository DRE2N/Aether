package de.erethon.aether.creature;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class InstancedNPC extends ActiveNPC {

    List<Player> viewers = new ArrayList<>();

    public InstancedNPC(NPC npc, Player... viewers) {
        super(npc);
        this.viewers.addAll(Arrays.asList(viewers));
    }

    public InstancedNPC(NPC npc, Entity entity, Player... viewers) {
        super(npc);
        this.viewers.addAll(Arrays.asList(viewers));
    }

    public void addViewer(Player player) {
        viewers.add(player);
    }

    public void removeViewer(Player player) {
        viewers.remove(player);
    }

    public List<Player> getViewers() {
        return viewers;
    }
}
