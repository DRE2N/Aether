package de.erethon.aether.listener;

import de.erethon.aether.Aether;
import de.erethon.aether.animation.AnimationUtils;
import de.erethon.aether.animation.EntityAnimation;
import de.erethon.aether.creature.NPCManager;
import de.erethon.commons.chat.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListener implements Listener {

    Aether plugin = Aether.getInstance();
    NPCManager manager = plugin.getNpcManager();

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        AnimationUtils.sendAnimation(event.getRightClicked(), event.getPlayer(), EntityAnimation.DEATH);
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        for (Entity entity : event.getPlayer().getNearbyEntities(50, 50, 50)) {
            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;
                mob.getPathfinder().moveTo(event.getClickedBlock().getLocation(), 0.5);
            }
        }
    }
}
