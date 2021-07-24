package de.erethon.aether.listener;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.events.CreatureInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListener implements Listener {

    Aether plugin = Aether.getInstance();
    ActiveCreatureManager manager = plugin.getActiveCreatureManager();

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ActiveNPC npc = manager.get(event.getRightClicked().getUniqueId());
        if (npc == null) {
            return;
        }
        CreatureInteractEvent interactEvent = new CreatureInteractEvent(npc);
        Bukkit.getPluginManager().callEvent(interactEvent);
    }
}
