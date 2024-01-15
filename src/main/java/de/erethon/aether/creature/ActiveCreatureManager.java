package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.tools.NMSUtils;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveCreatureManager {

    private static final char healthBar = '|';

    ConcurrentHashMap<UUID, ActiveNPC> globalNPCs = new ConcurrentHashMap<>(16);
    ConcurrentHashMap<Player, HashMap<String, InstancedNPC>> instancedNPCs = new ConcurrentHashMap<>();

    public ActiveCreatureManager() {
        ambientSoundTask.runTaskTimer(Aether.getInstance(), 20, 40);
    }

    BukkitRunnable ambientSoundTask = new BukkitRunnable() {
        @Override
        public void run() {
            for (ActiveNPC npc : globalNPCs.values())
                npc.playAmbientSound();
            }
        };

    public ActiveNPC get(UUID uuid) {
        return globalNPCs.get(uuid);
    }

    public void remove(UUID uuid) {
        globalNPCs.remove(uuid);
    }


    public InstancedNPC getInstanced(Player player, String id) {
        return instancedNPCs.get(player).get(id);
    }

    public void addActive(Entity entity, ActiveNPC npc) {
        globalNPCs.put(entity.getUniqueId(), npc);
    }


    public ConcurrentHashMap<UUID, ActiveNPC> getGlobalNPCs() {
        return globalNPCs;
    }
}
