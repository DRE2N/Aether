package de.erethon.mobsxl;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.mobsxl.npc.ActiveNPC;
import de.erethon.mobsxl.npc.NPCManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    MobsXL plugin = MobsXL.getInstance();
    NPCManager manager = plugin.getNpcManager();

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        UUID uuid = entity.getUniqueId();
        if (!manager.getUuids().containsKey(uuid)) {
            return;
        }
        ActiveNPC npc = manager.getActiveNPC(uuid);
        MessageUtil.sendMessage(event.getPlayer(), "ID: " + npc.getNpc().getID() + " Name: " + npc.getNpc().getDisplayName());
        MobsXL.debug("Rightclick on " + npc.getNpc().getID() + " (UUID: " + uuid + ")");
    }
}
