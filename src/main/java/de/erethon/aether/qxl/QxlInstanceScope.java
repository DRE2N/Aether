package de.erethon.aether.qxl;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.livingworld.instancing.InstanceManager;
import de.erethon.questsxl.livingworld.instancing.InstancedArea;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Bridges Aether mobs to QuestsXL's virtual instance membership.
 *
 * <p>The key deliberately uses the persistent instance owner where possible. Runtime instance IDs
 * change when a newly-created instance is loaded again, while the template/owner pair stays stable.</p>
 */
public final class QxlInstanceScope {

    private QxlInstanceScope() {
    }

    public static Set<String> getSpawnScopes(Quester quester, Location spawnLocation) {
        Set<String> scopes = new LinkedHashSet<>();
        if (quester instanceof QPlayer player) {
            addScopeAt(scopes, player, spawnLocation);
            return scopes;
        }
        if (!(quester instanceof QEvent event)) {
            return scopes;
        }

        Set<QPlayer> relevantPlayers = new LinkedHashSet<>(event.getParticipants().keySet());
        if (relevantPlayers.isEmpty()) {
            relevantPlayers.addAll(event.getPlayersInRange());
        }
        if (event.getLastInstigator() != null) {
            relevantPlayers.add(event.getLastInstigator());
        }
        for (QPlayer player : relevantPlayers) {
            addScopeAt(scopes, player, spawnLocation);
        }
        return scopes;
    }

    public static Set<String> getActiveScopesNear(Location location, double range) {
        Set<String> scopes = new LinkedHashSet<>();
        InstanceManager manager = getManager();
        if (manager == null || location.getWorld() == null) {
            return scopes;
        }
        double rangeSquared = range * range;
        BlockPos pos = toBlockPos(location);
        for (InstancedArea instance : manager.getActiveInstances()) {
            if (!isAt(instance, location, pos)) {
                continue;
            }
            boolean participantNearby = instance.getParticipants().stream()
                    .map(QPlayer::getPlayer)
                    .anyMatch(player -> player.isOnline()
                            && player.getWorld().equals(location.getWorld())
                            && player.getLocation().distanceSquared(location) <= rangeSquared);
            if (participantNearby) {
                scopes.add(scopeKey(instance));
            }
        }
        return scopes;
    }

    public static @Nullable String getPlayerScope(org.bukkit.entity.Player player) {
        InstanceManager manager = getManager();
        if (manager == null) {
            return null;
        }
        QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            return null;
        }
        InstancedArea instance = manager.getActiveInstance(qPlayer);
        return instance == null ? null : scopeKey(instance);
    }

    public static @Nullable String getPlayerScopeAt(org.bukkit.entity.Player player, Location location) {
        InstanceManager manager = getManager();
        if (manager == null) {
            return null;
        }
        QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            return null;
        }
        InstancedArea instance = manager.getActiveInstance(qPlayer);
        return instance != null && isAt(instance, location, toBlockPos(location)) ? scopeKey(instance) : null;
    }

    public static boolean canInteract(AetherBaseMob mob, LivingEntity other) {
        String mobScope = mob.getInstanceScope();
        if (other instanceof AetherBaseMob otherMob) {
            return scopesMatch(mobScope, otherMob.getInstanceScope());
        }
        if (other instanceof Player player) {
            return scopesMatch(mobScope, getPlayerScope((org.bukkit.entity.Player) player.getBukkitEntity()));
        }
        return mobScope == null;
    }

    public static boolean canInteract(AetherBaseMob mob, org.bukkit.entity.Entity other) {
        if (other instanceof org.bukkit.entity.Player player) {
            return scopesMatch(mob.getInstanceScope(), getPlayerScope(player));
        }
        if (other instanceof de.erethon.papyrus.entities.CraftCustomMob customMob
                && customMob.getHandle() instanceof AetherBaseMob otherMob) {
            return scopesMatch(mob.getInstanceScope(), otherMob.getInstanceScope());
        }
        return mob.getInstanceScope() == null;
    }

    public static void syncVisibility(AetherBaseMob mob) {
        syncTerrain(mob);
        String scope = mob.getInstanceScope();
        if (mob.isRemoved()) {
            return;
        }
        org.bukkit.entity.Entity entity = mob.getBukkitEntity();
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            String playerScope = getPlayerScope(player);
            if (scope == null) {
                if (playerScope != null) {
                    if (player.canSee(entity)) {
                        mob.rememberInstanceHide(player);
                    }
                    player.hideEntity(Aether.getInstance(), entity);
                } else if (mob.restoreInstanceHide(player)) {
                    player.showEntity(Aether.getInstance(), entity);
                }
                continue;
            }
            boolean visible = scope.equals(playerScope)
                    && (mob.holder == null || mob.holder.checkVisibilityConditions(player));
            if (visible) {
                player.showEntity(Aether.getInstance(), entity);
            } else {
                player.hideEntity(Aether.getInstance(), entity);
            }
        }
    }

    /**
     * Keeps the Papyrus terrain view aligned with the mob's persistent instance scope.
     */
    public static void syncTerrain(AetherBaseMob mob) {
        String scope = mob.getInstanceScope();
        InstancedArea instance = scope == null ? null : findInstance(scope);
        de.erethon.papyrus.world.VirtualTerrainProvider expected =
                instance == null ? null : instance.getVirtualTerrainProvider();
        if (mob.getVirtualTerrainProvider() != expected) {
            mob.setVirtualTerrainProvider(expected);
            mob.getNavigation().stop();
        }
    }

    public static boolean hasNoParticipants(AetherBaseMob mob) {
        String scope = mob.getInstanceScope();
        if (scope == null) {
            return false;
        }
        InstancedArea instance = findInstance(scope);
        return instance == null || instance.isEmpty();
    }

    public static @Nullable AetherBaseMob getAetherMob(org.bukkit.entity.Entity entity) {
        if (entity instanceof de.erethon.papyrus.entities.CraftCustomMob customMob
                && customMob.getHandle() instanceof AetherBaseMob mob) {
            return mob;
        }
        return null;
    }

    public static @Nullable org.bukkit.entity.Entity resolveSource(org.bukkit.entity.Entity damager) {
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof org.bukkit.entity.Entity shooter) {
            return shooter;
        }
        return damager;
    }

    private static void addScopeAt(Set<String> scopes, QPlayer player, Location location) {
        InstanceManager manager = getManager();
        if (manager == null) {
            return;
        }
        InstancedArea instance = manager.getActiveInstance(player);
        if (instance != null && isAt(instance, location, toBlockPos(location))) {
            scopes.add(scopeKey(instance));
        }
    }

    private static boolean isAt(InstancedArea instance, Location location, BlockPos pos) {
        return location.getWorld() != null
                && location.getWorld().equals(instance.getTemplate().getWorld())
                && instance.containsPosition(pos);
    }

    private static BlockPos toBlockPos(Location location) {
        return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static String scopeKey(InstancedArea instance) {
        UUID owner = instance.getOwnerCharacterId();
        if (owner != null) {
            return instance.getTemplate().getId() + ":" + owner;
        }
        return instance.getId();
    }

    private static @Nullable InstancedArea findInstance(String scope) {
        InstanceManager manager = getManager();
        if (manager == null) {
            return null;
        }
        return manager.getActiveInstances().stream()
                .filter(instance -> scope.equals(scopeKey(instance)))
                .findFirst()
                .orElse(null);
    }

    private static boolean scopesMatch(@Nullable String first, @Nullable String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static @Nullable InstanceManager getManager() {
        QuestsXL questsXL = QuestsXL.get();
        return questsXL == null ? null : questsXL.getInstanceManager();
    }
}
