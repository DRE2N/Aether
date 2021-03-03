package de.erethon.mobsxl.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.mobsxl.MobsXL;
import de.erethon.mobsxl.tools.packetwrapper.*;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class NPCManager implements Listener {

    MobsXL plugin = MobsXL.getInstance();
    List<NPC> loadedNPCs = new ArrayList<>();
    Map<Integer, EntityType> typeMap = new HashMap<>();

    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    NamespacedKey key = new NamespacedKey(plugin, "mxl");
    HashMap<UUID, ActiveNPC> uuids = new HashMap<>();

    public NPCManager() {
        MessageUtil.log("Adding packet listener...");
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WrapperPlayServerSpawnEntityLiving wrapper = new WrapperPlayServerSpawnEntityLiving(event.getPacket());
                Player player = event.getPlayer();
                Entity entity = wrapper.getEntity(event);
                wrapper.setType(EntityType.PLAYER);
                destroyEntity(event.getPlayer(), entity.getUniqueId(), entity.getEntityId());
                spawnPlayerEntity(event.getPlayer(), wrapper.getUniqueId(), wrapper.getEntityID(), wrapper.getEntity(event).getLocation());
                WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
                info.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                List<PlayerInfoData> dataList = new ArrayList<>(info.getData());
                for (UUID uuid : uuids.keySet()) {
                    if (player.getWorld().getEntity(uuid) == null) {
                        continue;
                    }
                    WrappedGameProfile profile = new WrappedGameProfile(uuid, "Test");
                    // Set skin
                    PlayerInfoData playerInfoData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(""));
                    dataList.add(playerInfoData);
                }
                info.setData(dataList);
                info.sendPacket(player);
            }
        });
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                event.setPacket(sendInfo(event.getPlayer(), event.getPacket()).getHandle());
            }
        });
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_SOUND) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WrapperPlayServerEntitySound packet = new WrapperPlayServerEntitySound(event.getPacket());
                if (uuids.keySet().contains(packet.getEntity(event).getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        });
    }

    /*public void addEntity(UUID uuid) {
        uuids.add(uuid);
    }*/

    @EventHandler
    public void chunkLoadEvent(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
        info.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        List<PlayerInfoData> dataList = new ArrayList<>(info.getData());
        for (UUID uuid : uuids.keySet()) {
            if (player.getWorld().getEntity(uuid) == null) {
                continue;
            }
            WrappedGameProfile profile = new WrappedGameProfile(uuid, "Test");
            // Set skin
            PlayerInfoData playerInfoData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(" "));
            dataList.add(playerInfoData);
        }
        info.setData(dataList);
        info.sendPacket(player);
    }


    public void createNPC(Player player, Location location, String name) {
        Entity entity = location.getWorld().spawnEntity(location, EntityType.PIG, CreatureSpawnEvent.SpawnReason.CUSTOM);
        entity.setCustomName(name);
        entity.setSilent(true);
        //uuids.add(entity.getUniqueId());
        BukkitRunnable asyncNetworkTask = new BukkitRunnable() {
            @Override
            public void run() {
                destroyEntity(player, entity.getUniqueId(), entity.getEntityId());
                WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
                info.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                List<PlayerInfoData> dataList = new ArrayList<>(info.getData());
                for (UUID uuid : uuids.keySet()) {
                    if (location.getWorld().getEntity(uuid) == null) {
                        continue;
                    }
                    String n = location.getWorld().getEntity(uuid).getCustomName();
                    PlayerInfoData playerInfoData = new PlayerInfoData(new WrappedGameProfile(uuid, n), 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(""));
                    dataList.add(playerInfoData);
                }
                info.setData(dataList);
                info.sendPacket(player);
                spawnPlayerEntity(player, entity.getUniqueId(), entity.getEntityId(), location);
            }
        };
        asyncNetworkTask.runTaskLaterAsynchronously(plugin, 1);

    }

    public void destroyEntity(Player player, UUID uuid, int entityID) {
        WrapperPlayServerEntityDestroy packet = new WrapperPlayServerEntityDestroy();
        int[] ids = {entityID, entityID};
        packet.setEntityIds(ids);
        packet.sendPacket(player);
    }

    public WrapperPlayServerPlayerInfo sendInfo(Player player, PacketContainer packetContainer) {
        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(packetContainer);
        info.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        List<PlayerInfoData> dataList = new ArrayList<>(info.getData());
        for (UUID uuid : uuids.keySet()) {
            if (player.getWorld().getEntity(uuid) == null) {
                continue;
            }
            PlayerInfoData playerInfoData = new PlayerInfoData(new WrappedGameProfile(uuid, "Test"), 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(""));
            dataList.add(playerInfoData);
        }
        info.setData(dataList);
        return info;
    }

    public void spawnPlayerEntity(Player player, UUID uuid, int entityID, Location loc) {
        WrapperPlayServerNamedEntitySpawn npc = new WrapperPlayServerNamedEntitySpawn();
        npc.setPlayerUUID(uuid);
        npc.setX(loc.getX());
        npc.setY(loc.getY());
        npc.setZ(loc.getZ());
        npc.setEntityID(entityID);
        npc.sendPacket(player);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            if (container.isEmpty()) {
                continue;
            }
            if (!container.has(key, PersistentDataType.STRING)) {
                continue;
            }
            String idString = container.get(key, PersistentDataType.STRING);
            if (idString == null) {
                continue;
            }
            NPC npc = getNPCByIDString(idString);
            if (npc == null) {
                continue;
            }
            if (typeMap.containsKey(npc.getID())) {
                continue;
            }
            register(npc);
            UUID uuid = entity.getUniqueId();
            ActiveNPC activeNPC = new ActiveNPC(npc, entity);
            uuids.put(uuid, activeNPC);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        List<ActiveNPC> toRemove = new ArrayList<>();
        for (Entity entity : event.getChunk().getEntities()) {
            uuids.remove(entity.getUniqueId());
        }
    }

    public void loadFiles() {
        for (File file : MobsXL.MOBDATA.listFiles()) {
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            for (String section : fileConfiguration.getKeys(false)) {
                NPC npc = new NPC(fileConfiguration.getConfigurationSection(section));
                npc.load();
                register(npc);
            }
        }
    }

    public void register(NPC npc) {
        typeMap.put(npc.getID(), npc.getDisplayType());
    }

    public NPC getNPCByIDString(String string) {
        String[] split = string.split("\\:");
        for (NPC npc : loadedNPCs) {
            String id = Integer.toString(npc.getID());
            if (id.equals(split[0])) {
                return npc;
            }
        }
        return null;
    }

    public ActiveNPC getActiveNPC(UUID uuid) {
        return uuids.get(uuid);
    }

    public NamespacedKey getKey() {
        return key;
    }

    public String getIDString(NPC npc) {
        return npc.getID() + ":" + System.currentTimeMillis();
    }

    public List<NPC> getLoadedNPCs() {
        return loadedNPCs;
    }

    public HashMap<UUID, ActiveNPC> getUuids() {
        return uuids;
    }
}
