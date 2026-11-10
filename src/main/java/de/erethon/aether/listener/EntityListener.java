package de.erethon.aether.listener;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.qxl.QxlInstanceScope;
import de.erethon.papyrus.entities.CraftCustomMob;
import de.erethon.papyrus.combat.BossControlEvent;
import de.erethon.spellbook.api.SpellEffectAddEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;
import java.util.Set;

public class EntityListener implements Listener {

    private static final Set<String> BOSS_CONTROL_EFFECTS = Set.of(
            "StunEffect", "FearEffect", "SilenceEffect", "SlowEffect", "BlindnessEffect");

    Aether plugin = Aether.getInstance();

    public EntityListener(Aether plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBossControl(BossControlEvent event) {
        if (!(event.getTarget() instanceof CraftCustomMob mob)
                || !(mob.getHandle() instanceof AetherBaseMob aetherMob)
                || aetherMob.getBossBreakbar() == null) return;
        if (!QxlInstanceScope.canInteract(aetherMob, event.getActor())
                || aetherMob.getData().isInvulnerable()
                || (event.getActor() instanceof Player && aetherMob.getData().isInvulnerableToPlayers())) {
            event.setResult(false, aetherMob.getBossBreakbar().isBroken());
            return;
        }
        boolean accepted = aetherMob.getBossBreakbar().apply(event.getPressure());
        event.setResult(accepted, aetherMob.getBossBreakbar().isBroken());
    }

    @EventHandler
    public void onEntityDeath(SpellEffectAddEvent event) {
        LivingEntity entity = (LivingEntity) event.getTarget();
        if (entity instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (aetherMob.getBossBreakbar() != null
                    && BOSS_CONTROL_EFFECTS.contains(event.getEffect().getClass().getSimpleName())) {
                event.setCancelled(true);
                return;
            }
            if (event.getEffect().getCaster() instanceof Entity caster
                    && !QxlInstanceScope.canInteract(aetherMob, caster)) {
                event.setCancelled(true);
                return;
            }
            if (aetherMob.getData().isInvulnerable()) {
                event.setCancelled(true);
                return;
            }
            if (aetherMob.getData().isInvulnerableToPlayers() && event.getEffect().getCaster() instanceof Player) {
                event.setCancelled(true);;
            }
        }
        if (entity instanceof Player && event.getEffect().getCaster() instanceof Entity caster) {
            AetherBaseMob aetherMob = QxlInstanceScope.getAetherMob(caster);
            if (aetherMob != null && !QxlInstanceScope.canInteract(aetherMob, entity)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityKnockbackEvent event) {
        if (event.getEntity() instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (aetherMob.getBossBreakbar() != null) {
                event.setCancelled(true);
                return;
            }
            if (aetherMob.getData().isInvulnerable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityKnockbackByEntityEvent event) {
        if (event.getEntity() instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (aetherMob.getBossBreakbar() != null) {
                event.setCancelled(true);
                return;
            }
            if (!QxlInstanceScope.canInteract(aetherMob, event.getHitBy())) {
                event.setCancelled(true);
                return;
            }
            if (aetherMob.getData().isInvulnerableToPlayers() && event.getHitBy() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity source = QxlInstanceScope.resolveSource(event.getDamager());
        AetherBaseMob damagedMob = QxlInstanceScope.getAetherMob(event.getEntity());
        if (damagedMob != null && source != null && !QxlInstanceScope.canInteract(damagedMob, source)) {
            event.setCancelled(true);
            return;
        }
        AetherBaseMob attackingMob = source == null ? null : QxlInstanceScope.getAetherMob(source);
        if (attackingMob != null && !QxlInstanceScope.canInteract(attackingMob, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileCollide(ProjectileCollideEvent event) {
        if (!(event.getEntity().getShooter() instanceof Entity shooter)) {
            return;
        }
        AetherBaseMob shootingMob = QxlInstanceScope.getAetherMob(shooter);
        if (shootingMob != null && !QxlInstanceScope.canInteract(shootingMob, event.getCollidedWith())) {
            event.setCancelled(true);
            return;
        }
        AetherBaseMob collidedMob = QxlInstanceScope.getAetherMob(event.getCollidedWith());
        if (collidedMob != null && !QxlInstanceScope.canInteract(collidedMob, shooter)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTrackingStart(PlayerTrackEntityEvent event) {
        if (event.getEntity() instanceof CraftCustomMob mob && mob.getHandle() instanceof AetherBaseMob aetherMob) {
            if (!Objects.equals(aetherMob.getInstanceScope(),
                    QxlInstanceScope.getPlayerScope(event.getPlayer()))) {
                event.setCancelled(true);
                return;
            }
            if (aetherMob.holder != null) {
                event.setCancelled(!aetherMob.holder.checkVisibilityConditions(event.getPlayer()));
            }
        }
    }

}
