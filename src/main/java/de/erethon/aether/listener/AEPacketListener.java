package de.erethon.aether.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.*;
import de.erethon.aether.tools.packetwrapper.*;
import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AEPacketListener {

    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    Aether plugin = Aether.getInstance();
    ActiveCreatureManager manager = plugin.getActiveCreatureManager();

    public AEPacketListener() {
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                onEntitySpawn(event);
            }
        });
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_SOUND) {
            @Override
            public void onPacketSending(PacketEvent event) {
                onSound(event);
            }
        });
    }

    public void onSound(PacketEvent event) {
        WrapperPlayServerEntitySound wrapper = new WrapperPlayServerEntitySound(event.getPacket());
        ActiveNPC activeNPC = manager.get(wrapper.getEntity(event).getUniqueId());
        if (activeNPC == null && wrapper.getSoundCategory() != EnumWrappers.SoundCategory.VOICE) {
            return;
        }
        event.setCancelled(true);
    }

    public void onEntitySpawn(PacketEvent event) {
        WrapperPlayServerSpawnEntityLiving wrapper = new WrapperPlayServerSpawnEntityLiving(event.getPacket());
        ActiveNPC activeNPC = manager.get(wrapper.getEntity(event).getUniqueId());
        if (activeNPC == null) {
            return;
        }
        event.setCancelled(true);
        EntityType type = activeNPC.getNpc().getDisplayType();
        if (type == EntityType.PLAYER) {
            UUID uuid = wrapper.getEntity(event).getUniqueId();
            doPlayerStuff(event.getPlayer(), uuid, activeNPC.getNpc().getDisplayName());
            WrapperPlayServerNamedEntitySpawn namedEntitySpawn = new WrapperPlayServerNamedEntitySpawn();
            namedEntitySpawn.setPlayerUUID(uuid);
            namedEntitySpawn.setX(wrapper.getX());
            namedEntitySpawn.setY(wrapper.getY());
            namedEntitySpawn.setZ(wrapper.getZ());
            namedEntitySpawn.setEntityID(wrapper.getEntityID());
            namedEntitySpawn.sendPacket(event.getPlayer());
            return;
        }
        if (!type.isAlive()) {
            WrapperPlayServerSpawnEntity spawnEntity = new WrapperPlayServerSpawnEntity();
            spawnEntity.setType(type);
            spawnEntity.setUniqueId(wrapper.getUniqueId());
            spawnEntity.setEntityID(wrapper.getEntityID());
            spawnEntity.setX(wrapper.getX());
            spawnEntity.setY(wrapper.getY());
            spawnEntity.setZ(wrapper.getZ());
            spawnEntity.setPitch(wrapper.getPitch());
            spawnEntity.setYaw(wrapper.getYaw());
            spawnEntity.setObjectData(0);
            spawnEntity.sendPacket(event.getPlayer());
            return;
        }
        wrapper.setType(type);
        MessageUtil.log("Set type to " + type.name());
        MessageUtil.log(wrapper.getHandle().toString());
        event.setPacket(wrapper.getHandle()); // TODO: Not working with 5.0
    }

    public static void doPlayerStuff(Player player, UUID uuid, String name) {
        Aether plugin = Aether.getInstance();
        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
        info.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        List<PlayerInfoData> dataList = new ArrayList<>(info.getData());
        NPCData npc = plugin.getActiveCreatureManager().get(uuid).getNpc();
        Skin skin = plugin.getSkinCache().get(npc.getSkinLink());
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        if (skin != null) {
            profile.getProperties().get("textures").clear();
            profile.getProperties().put("textures", new WrappedSignedProperty("textures", skin.texture(), skin.signature()));
        }
        PlayerInfoData playerInfoData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name));
        dataList.add(playerInfoData);
        UUID uuid1 = playerInfoData.getProfile().getUUID();
        info.setData(dataList);
        info.sendPacket(player);
        BukkitRunnable runLater = new BukkitRunnable() {
            @Override
            public void run() {
                WrapperPlayServerPlayerInfo remove = new WrapperPlayServerPlayerInfo();
                remove.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                List<PlayerInfoData> dataList = new ArrayList<>(remove.getData());
                WrappedGameProfile removeProfile = new WrappedGameProfile(uuid1, name);
                PlayerInfoData removeData = new PlayerInfoData(removeProfile, 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name));
                dataList.add(removeData);
                remove.setData(dataList);
                remove.sendPacket(player);
            }
        };
        runLater.runTaskLater(Aether.getInstance(), 3);
    }
}