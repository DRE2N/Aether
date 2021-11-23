package de.erethon.aether.listener;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.*;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketEvent;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import io.github.retrooper.packetevents.utils.npc.NPC;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AEPacketListener extends PacketListenerAbstract {

    Aether plugin = Aether.getInstance();
    ActiveCreatureManager manager = plugin.getActiveCreatureManager();

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            onEntitySpawn(new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket()), event.getPlayer());
        }
    }


    /*public void onSound(PacketEvent event) {
        WrapperPlayServerEntitySound wrapper = new WrapperPlayServerEntitySound(event.getPacket());
        ActiveNPC activeNPC = manager.get(wrapper.getEntity(event).getUniqueId());
        if (activeNPC == null && wrapper.getSoundCategory() != EnumWrappers.SoundCategory.VOICE) {
            return;
        }
        event.setCancelled(true);
    }*/

    public void onEntitySpawn(WrappedPacketOutSpawnEntityLiving wrapper, Player player) {
        ActiveNPC activeNPC = manager.get(wrapper.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
        EntityType type = activeNPC.getNpc().getDisplayType();
        if (type == EntityType.PLAYER) {
            NPC npc = new NPC(activeNPC.getNpc().getDisplayName());
            for (Player p : activeNPC.getViewers()) {
                npc.spawn(p);
            }
            PacketEvents.get().getServerUtils().getNPCManager().registerNPC(npc);
        }
        wrapper.setEntityType(type);
    }
            /*if (!type.isAlive()) {
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
            event.setCancelled(true);
            return;
        }
        wrapper.setType(type);
        event.setPacket(wrapper.getHandle());
    }

    public static void doPlayerStuff(Player player, UUID uuid, String name) {
        Aether plugin = Aether.getInstance();
        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
        info.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        List<PlayerInfoData> dataList = new ArrayList<>(info.getData());
        NPCData npcData = plugin.getActiveCreatureManager().get(uuid).getNpc();
        Skin skin = plugin.getSkinCache().get(npcData.getSkinID());
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
        runLater.runTaskLater(Aether.getInstance(), 3);*/

}
