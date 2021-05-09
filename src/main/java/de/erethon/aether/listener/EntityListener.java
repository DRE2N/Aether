package de.erethon.aether.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.profile.PreFillProfileEvent;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.commons.chat.MessageUtil;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class EntityListener implements Listener {

    Aether plugin = Aether.getInstance();
    ActiveCreatureManager creatures = plugin.getActiveCreatureManager();

    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (creatures.get(event.getEntity().getUniqueId()) != null) {
            return;
        }
        if (event.getEntity().getPersistentDataContainer().isEmpty()) {
            return;
        }
        if (!event.getEntity().getPersistentDataContainer().has(plugin.getKey(), PersistentDataType.STRING)) {
            return;
        }
        ActiveNPC activeNPC = new ActiveNPC(event.getEntity());
        creatures.addActive(event.getEntity(), activeNPC);
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
        creatures.updateHealthbarPosition(event.getEntity());
        activeNPC.setAttacked(false);
    }

    @EventHandler
    public void onEntityDamageGeneric(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
        creatures.showHealth(event.getEntity());
        activeNPC.setAttacked(true);
    }

    @EventHandler
    public void onDmgByEntity(EntityDamageByEntityEvent event) {
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
        creatures.showHealth(event.getEntity());
        activeNPC.setAttacked(true);
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Sound sound = activeNPC.getNpc().getHurtSound();
            if (sound != null) {
                player.playSound(event.getEntity().getLocation(), sound, SoundCategory.VOICE, 1.0f, 1.0f);
            }
        }
    }

    // Combat actions
    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        //event.setTarget(null);
    }

    @EventHandler
    public void onPathfind(EntityPathfindEvent event) {
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
        event.setDeathSound(activeNPC.getNpc().getDeathSound());
        event.setDroppedExp(activeNPC.getNpc().getDropXP());
        event.getDrops().clear();
        event.getDrops().addAll(activeNPC.getNpc().getLoot());
        event.setDeathSoundCategory(SoundCategory.VOICE);
        creatures.removeHealthbar(event.getEntity());
        creatures.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        ActiveNPC activeNPC = creatures.get(event.getEntity().getUniqueId());
        if (activeNPC == null) {
            return;
        }
        creatures.removeHealthbar(event.getEntity());
    }

}
