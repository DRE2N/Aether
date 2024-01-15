package de.erethon.aether.listener;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.events.CreatureInteractEvent;
import de.erethon.aether.network.AetherPacketHandler;
import de.erethon.bedrock.chat.MessageUtil;
import io.netty.channel.ChannelPipeline;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.EquipmentSlot;

import static io.papermc.paper.network.ChannelInitializeListenerHolder.addListener;

public class PlayerListener implements Listener {

    Aether plugin = Aether.getInstance();
    ActiveCreatureManager manager = plugin.getActiveCreatureManager();

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        CraftPlayer bukkitPlayer = (CraftPlayer) event.getPlayer();
        ServerPlayer serverPlayer = bukkitPlayer.getHandle();
        AetherPacketHandler packetHandler = new AetherPacketHandler(plugin, serverPlayer);
        ChannelPipeline pipeline = serverPlayer.connection.connection.channel.pipeline();
        pipeline.addAfter("packet_handler", "aether_handler", packetHandler);
        MessageUtil.log("Added handler to " + pipeline.names());
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ActiveNPC npc = manager.get(event.getRightClicked().getUniqueId());
        if (npc == null) {
            return;
        }
        new CreatureInteractEvent(event.getPlayer(), npc).callEvent();
    }
}
