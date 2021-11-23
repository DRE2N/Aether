package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NPCInstancing extends PacketListenerAbstract {

    Aether plugin = Aether.getInstance();

    private Set<UUID> instancedNPCs = new HashSet<>();
    private Map<Player, Set<UUID>> visibleNPCs;


    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrappedPacketOutSpawnEntityLiving wrapped = new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket());
            if (!canSee(event.getPlayer(), wrapped.getUUID().get())) {
                event.setCancelled(true);
            }
        }
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
        /*if (visibleNPCs.get(player) == null) {
            visibleNPCs.put(player, new HashSet<>());
        }
        visibleNPCs.computeIfAbsent(player, k -> new HashSet<>());
        visibleNPCs.get(player).add(uuid);


        npc.setUniqueId(uuid);
        try {
            protocol.sendServerPacket(player, npc.getHandle());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }*/
    }

    public void hide(Player player, UUID uuid) {
        /*if (visibleNPCs.get(player) == null) {
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
        }*/
    }
}
