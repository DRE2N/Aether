package de.erethon.aether.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.profile.PreFillProfileEvent;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.InstancedNPC;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.commons.chat.MessageUtil;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
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
        if (event.getDamager() instanceof Player player) {
            Sound sound = activeNPC.getNpc().getHurtSound();
            if (sound != null) {
                player.playSound(event.getEntity().getLocation(), sound, SoundCategory.VOICE, 1.0f, 1.0f);
            }
        }
    }

    // Combat actions
    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        ActiveNPC own = creatures.get(event.getEntity().getUniqueId());
        ActiveNPC target = creatures.get(event.getTarget().getUniqueId());
        if (own == null || target == null) {
            return;
        }
        if (own.getNpc().getFaction().equalsIgnoreCase(target.getNpc().getFaction())) {
            event.setCancelled(true);
        }
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
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), activeNPC.getNpc().getShootSound(), 1, 1);
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
        event.getDrops().clear();
        if (activeNPC instanceof InstancedNPC instanced) {
            for (ItemStack itemStack : instanced.getNpc().getLoot()) {
                Entity entity = event.getEntity().getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.DROPPED_ITEM);
                Item item = (Item) entity;
                item.setItemStack(itemStack);
                if (instanced.getViewers() == null || instanced.getViewers().isEmpty()) {
                    return;
                }
                item.setOwner(instanced.getViewers().get(0).getUniqueId());
            }
            event.setDeathSound(null);
            for (Player player : instanced.getViewers()) {
                player.playSound(event.getEntity().getLocation(), instanced.getNpc().getDeathSound(), SoundCategory.VOICE, 1, 1);
            }
            InstancedCreatureDeathEvent instancedCreatureDeathEvent = new InstancedCreatureDeathEvent(instanced);
            Bukkit.getPluginManager().callEvent(instancedCreatureDeathEvent);
        } else {
            event.setDeathSound(activeNPC.getNpc().getDeathSound());
            event.setDroppedExp(activeNPC.getNpc().getDropXP());
            event.getDrops().addAll(activeNPC.getNpc().getLoot());
            event.setDeathSoundCategory(SoundCategory.VOICE);
            CreatureDeathEvent creatureDeathEvent = new CreatureDeathEvent(activeNPC);
            Bukkit.getPluginManager().callEvent(creatureDeathEvent);
        }
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
