package de.erethon.aether.creature;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.erethon.aether.Aether;
import de.erethon.aether.tools.packetwrapper.packetwrapper.WrapperPlayServerEntityDestroy;
import de.erethon.aether.tools.packetwrapper.packetwrapper.WrapperPlayServerEntitySound;
import de.erethon.aether.tools.packetwrapper.packetwrapper.WrapperPlayServerSpawnEntity;
import de.erethon.aether.tools.packetwrapper.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class NPCInstancing {

    Aether plugin = Aether.getInstance();
    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();

    private Set<UUID> instancedNPCs = new HashSet<>();
    private Map<Player, Set<UUID>> visibleNPCs = new HashMap<>();

    public NPCInstancing() {
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WrapperPlayServerSpawnEntityLiving wrapper = new WrapperPlayServerSpawnEntityLiving(event.getPacket());
                Player player = event.getPlayer();
                if (instancedNPCs.contains(wrapper.getUniqueId()) && !canSee(player, wrapper.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        });
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_SOUND) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WrapperPlayServerEntitySound wrapper = new WrapperPlayServerEntitySound(event.getPacket());
                Player player = event.getPlayer();
                if (instancedNPCs.contains(wrapper.getEntity(event).getUniqueId()) && !canSee(player, wrapper.getEntity(event).getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        });
    }

    public void addInstanced(UUID uuid) {
        instancedNPCs.add(uuid);
    }

    public void removeInstanced(UUID uuid) {
        instancedNPCs.remove(uuid);
    }

    public boolean canSee(Player player, UUID uuid) {
        return visibleNPCs.get(player).contains(uuid);
    }

    public void show(Player player, UUID uuid) {
        if (visibleNPCs.get(player) == null) {
            visibleNPCs.put(player, new HashSet<>());
        }
        visibleNPCs.computeIfAbsent(player, k -> new HashSet<>());
        visibleNPCs.get(player).add(uuid);

        WrapperPlayServerSpawnEntity npc = new WrapperPlayServerSpawnEntity();
        npc.setUniqueId(uuid);
        try {
            protocol.sendServerPacket(player, npc.getHandle());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void hide(Player player, UUID uuid) {
        if (visibleNPCs.get(player) == null) {
            visibleNPCs.put(player, new HashSet<>());
        }
        visibleNPCs.get(player).remove(uuid);
        WrapperPlayServerEntityDestroy npc = new WrapperPlayServerEntityDestroy();
        int[] ids = new int[1];
        ids[0] = Bukkit.getEntity(uuid).getEntityId();
        npc.setEntityIds(ids);
        try {
            protocol.sendServerPacket(player, npc.getHandle());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
