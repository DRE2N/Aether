package de.erethon.aether.listener;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.papyrus.entities.CraftCustomMob;
import de.erethon.spellbook.api.SpellEffectAddEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityListener implements Listener {

    Aether plugin = Aether.getInstance();

    public EntityListener(Aether plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDeath(SpellEffectAddEvent event) {
        LivingEntity entity = (LivingEntity) event.getTarget();
        if (entity instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (aetherMob.getData().isInvulnerable()) {
                event.setCancelled(true);
                return;
            }
            if (aetherMob.getData().isInvulnerableToPlayers() && event.getEffect().getCaster() instanceof Player) {
                event.setCancelled(true);;
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityKnockbackEvent event) {
        if (event.getEntity() instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (aetherMob.getData().isInvulnerable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityKnockbackByEntityEvent event) {
        if (event.getEntity() instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (aetherMob.getData().isInvulnerableToPlayers() && event.getHitBy() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

}
