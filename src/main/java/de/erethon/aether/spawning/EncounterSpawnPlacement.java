package de.erethon.aether.spawning;

import de.erethon.aether.ai.goals.MeleeAttackGoal;
import de.erethon.aether.ai.goals.RangedBowAttackGoal;
import de.erethon.aether.ai.goals.RangedCrossbowAttackGoal;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.factions.Factions;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;
import java.util.function.Predicate;

/** Terrain and population checks shared by public natural and authored incidents. */
public final class EncounterSpawnPlacement {

    public static final int MAX_NEARBY_HOSTILES = 4;
    public static final int BUDGET_RADIUS = 20;

    private EncounterSpawnPlacement() {
    }

    public static boolean fits(ServerLevel level, AetherBaseMob mob) {
        AABB bounds = mob.getBoundingBox();
        return level.hasChunksAt((int) Math.floor(bounds.minX), (int) Math.floor(bounds.minZ),
                        (int) Math.floor(bounds.maxX), (int) Math.floor(bounds.maxZ))
                && level.getWorldBorder().isWithinBounds(bounds)
                && level.noCollision(mob, bounds) && !level.containsAnyLiquid(bounds);
    }

    static int publicHostiles(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(BUDGET_RADIUS, 8, BUDGET_RADIUS);
        return level.getEntitiesOfClass(AetherBaseMob.class, area, mob -> mob.isAlive()
                && mob.getInstanceScope() == null && isHostile(mob.getData())).size();
    }

    public static boolean placeIncidentMob(ServerLevel level, AetherBaseMob mob, Location origin) {
        boolean hostile = isHostile(mob.getData());
        var regionManager = Bukkit.getPluginManager().isPluginEnabled("Factions")
                ? Factions.get().getRegionManager() : null;
        Region region = regionManager == null ? null : regionManager.getRegionByLocation(origin);
        if (hostile && region != null && (region.getMode() == RegionMode.SAFE_ZONE || region.getMode() == RegionMode.PVP)) {
            return false;
        }
        if (hostile && publicHostiles(level, BlockPos.containing(origin.getX(), origin.getY(), origin.getZ()))
                >= MAX_NEARBY_HOSTILES) {
            return false;
        }

        return placeAtAuthoredPoint(level, mob, origin, location ->
                regionManager == null || Objects.equals(region, regionManager.getRegionByLocation(location)));
    }

    public static boolean placeAtAuthoredPoint(ServerLevel level, AetherBaseMob mob, Location origin,
                                                Predicate<Location> withinSite) {
        // Prefer the bound point, then look for nearby footing without changing floors.
        for (int radius = 0; radius <= 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                        continue;
                    }
                    double targetX = origin.getX() + x;
                    double targetZ = origin.getZ() + z;
                    BlockPos pos = BlockPos.containing(targetX, origin.getY(), targetZ);
                    if (!level.isPositionEntityTicking(pos)) {
                        continue;
                    }
                    mob.snapTo(targetX, origin.getY(), targetZ, origin.getYaw(), origin.getPitch());
                    if (fits(level, mob) && level.loadedAndEntityCanStandOn(pos.below(), mob)
                            && withinSite.test(mob.getBukkitEntity().getLocation())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean isHostile(NPCData data) {
        return data.getMobCategoryOverride() == MobCategory.MONSTER && !data.isInvulnerable()
                && ((!data.hasConfiguredGoals() && !data.hasConfiguredTargets())
                || isRanged(data) || data.getGoals().stream().anyMatch(MeleeAttackGoal.class::isInstance));
    }

    static boolean isRanged(NPCData data) {
        return data.getGoals().stream().anyMatch(goal ->
                goal instanceof RangedBowAttackGoal || goal instanceof RangedCrossbowAttackGoal);
    }
}
