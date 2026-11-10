package de.erethon.aether.ai.goals;

import de.erethon.aether.creature.AetherBaseMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps {@link AetherBaseMob} instances from stacking on the same melee point around a target.
 */
public final class MobSeparationHelper {

    static final double SEPARATION_RADIUS = 1.8;
    private static final int MIN_CROWD_NEIGHBORS = 2;
    private static final int CACHE_TICKS = 10;
    private static final int CELL_SHIFT = 4;
    private static final int CELL_SIZE = 1 << CELL_SHIFT;
    private static final Map<CellKey, List<AetherBaseMob>> CELL_CANDIDATES = new HashMap<>();
    private static int candidateEpoch = Integer.MIN_VALUE;

    private MobSeparationHelper() {
    }

    /**
     * @return horizontal offset (x, z) to step away from nearby mobs; zero vector if no crowd
     */
    public static Vec3 separationOffset(Mob mob, double pushDistance) {
        AABB area = mob.getBoundingBox().inflate(SEPARATION_RADIUS);
        List<AetherBaseMob> candidates = getCellCandidates(mob);
        int neighborCount = 0;
        double avgX = 0;
        double avgZ = 0;
        for (AetherBaseMob neighbor : candidates) {
            if (neighbor == mob || !neighbor.isAlive() || !neighbor.getBoundingBox().intersects(area)) {
                continue;
            }
            neighborCount++;
            avgX += neighbor.getX();
            avgZ += neighbor.getZ();
        }
        if (neighborCount < MIN_CROWD_NEIGHBORS) {
            return Vec3.ZERO;
        }

        avgX /= neighborCount;
        avgZ /= neighborCount;

        double dx = mob.getX() - avgX;
        double dz = mob.getZ() - avgZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= len;
            dz /= len;
        }

        return new Vec3(dx * pushDistance, 0, dz * pushDistance);
    }

    private static List<AetherBaseMob> getCellCandidates(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return List.of();
        }

        int epoch = Math.floorDiv(MinecraftServer.currentTick, CACHE_TICKS);
        if (epoch != candidateEpoch) {
            CELL_CANDIDATES.clear();
            candidateEpoch = epoch;
        }

        int cellX = mob.getBlockX() >> CELL_SHIFT;
        int cellY = mob.getBlockY() >> CELL_SHIFT;
        int cellZ = mob.getBlockZ() >> CELL_SHIFT;
        CellKey key = new CellKey(level, cellX, cellY, cellZ);
        return CELL_CANDIDATES.computeIfAbsent(key, ignored -> {
            double minX = cellX * CELL_SIZE;
            double minY = cellY * CELL_SIZE;
            double minZ = cellZ * CELL_SIZE;
            AABB cellBounds = new AABB(
                    minX,
                    minY,
                    minZ,
                    minX + CELL_SIZE,
                    minY + CELL_SIZE,
                    minZ + CELL_SIZE
            ).inflate(SEPARATION_RADIUS);
            return level.getEntitiesOfClass(AetherBaseMob.class, cellBounds, AetherBaseMob::isAlive);
        });
    }

    private record CellKey(
            ServerLevel level,
            int x,
            int y,
            int z
    ) {
    }
}
