package de.erethon.mobsxl.npc;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.erethon.mobsxl.MobsXL;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

public class ActiveNPC {

    MobsXL plugin = MobsXL.getInstance();
    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    NPCManager npcs = plugin.getNpcManager();
    Entity baseEntity;
    NPC npc;

    public ActiveNPC(NPC npc) {
        this.npc = npc;
    }
    public ActiveNPC(NPC npc, Entity entity) {
        this.npc = npc;
        this.baseEntity = entity;
    }


    public void spawn(Location location) {
        baseEntity = location.getWorld().spawnEntity(location, npc.getBaseType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        baseEntity.getPersistentDataContainer().set(npcs.getKey(), PersistentDataType.STRING, npcs.getIDString(npc));
        if (npc.getDisplayName() != null) {
            baseEntity.setCustomName(npc.getDisplayName());
            baseEntity.setCustomNameVisible(true);
        }
    }

    public MobsXL getPlugin() {
        return plugin;
    }

    public void setPlugin(MobsXL plugin) {
        this.plugin = plugin;
    }

    public ProtocolManager getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolManager protocol) {
        this.protocol = protocol;
    }

    public NPCManager getNpcs() {
        return npcs;
    }

    public void setNpcs(NPCManager npcs) {
        this.npcs = npcs;
    }

    public Entity getBaseEntity() {
        return baseEntity;
    }

    public void setBaseEntity(Entity baseEntity) {
        this.baseEntity = baseEntity;
    }

    public NPC getNpc() {
        return npc;
    }

    public void setNpc(NPC npc) {
        this.npc = npc;
    }
}
