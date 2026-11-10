package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.aether.creature.ModelledMob;
import de.erethon.aether.qxl.QxlInstanceScope;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static de.erethon.aether.spawning.EncounterSpawnPlacement.BUDGET_RADIUS;
import static de.erethon.aether.spawning.EncounterSpawnPlacement.MAX_NEARBY_HOSTILES;
import static de.erethon.aether.spawning.EncounterSpawnPlacement.isHostile;
import static de.erethon.aether.spawning.EncounterSpawnPlacement.isRanged;

/** Uses a Papyrus-approved spawn as the anchor of an occasional public encounter. */
final class NaturalEncounterSpawner {

    private static final int MIN_PLAYER_DISTANCE = 24;
    private static final int MAX_PLAYER_DISTANCE = 96;
    private static final int REFILL_TICKS = 600;
    private static final int PLACEMENT_ATTEMPTS = 8;

    private final Aether plugin;
    private final RegionManager regions;
    private final Map<Cell, Encounter> encounters = new HashMap<>();
    private int nextCleanup;

    NaturalEncounterSpawner(Aether plugin, RegionManager regions) {
        this.plugin = plugin;
        this.regions = regions;
    }

    boolean allowsNaturalSpawn(ServerLevel level, BlockPos pos) {
        int now = MinecraftServer.currentTick;
        if (now >= nextCleanup) {
            encounters.values().removeIf(encounter -> encounter.expired(now));
            nextCleanup = now + 100;
        }
        Encounter encounter = nearbyEncounter(level, pos, BUDGET_RADIUS);
        if (encounter != null && !encounter.pending && !encounter.hasLivingMembers() && !encounter.expired(now)) {
            return false;
        }
        return occupiedSlots(level, pos) < MAX_NEARBY_HOSTILES;
    }

    void tryPlan(ServerLevel level, AetherBaseMob anchor, Region region, WeightedList<NPCData> pool, int mobLevel) {
        NPCData leader = anchor.getData();
        BlockPos origin = anchor.blockPosition();
        Cell cell = Cell.at(level, origin);
        if (!isEligible(leader)
                || level.getRandom().nextInt(4) != 0
                || nearbyEncounter(level, origin, BUDGET_RADIUS) != null
                || occupiedSlots(level, origin) > MAX_NEARBY_HOSTILES - 2) {
            return;
        }

        long seed = level.getRandom().nextLong();
        RandomSource random = RandomSource.create(seed);
        WeightedList.Builder<NPCData> compatible = WeightedList.builder();
        boolean rangedLeader = isRanged(leader);
        for (var entry : pool.unwrap()) {
            NPCData candidate = entry.value();
            if (isEligible(candidate) && Objects.equals(leader.getFaction(), candidate.getFaction())
                    && (!rangedLeader || !isRanged(candidate))) {
                compatible.add(candidate, entry.weight());
            }
        }

        NPCData companion = compatible.build().getRandom(random).orElse(null);
        if (companion == null) {
            return;
        }

        BlockPos placement = findPlacement(level, anchor, companion, region, random, seed);
        if (placement == null) {
            debug(seed, leader, companion, "no suitable nearby position");
            return;
        }

        Encounter encounter = new Encounter(level, anchor.getUUID(), origin, placement);
        encounters.put(cell, encounter);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> finishEncounter(level, anchor, region, companion, placement, mobLevel, cell, encounter, seed));
    }

    private void finishEncounter(ServerLevel level, AetherBaseMob anchor, Region region, NPCData companion,
                                 BlockPos pos, int mobLevel, Cell cell, Encounter encounter, long seed) {
        encounter.pending = false;
        try {
            // Papyrus and CreatureSpawnEvent decide whether the anchor is actually admitted.
            if (level.getEntity(anchor.getUUID()) != anchor || !anchor.isAlive()
                    || occupiedSlots(level, pos) >= MAX_NEARBY_HOSTILES
                    || !canPlace(level, anchor, companion, region, pos, seed)) {
                debug(seed, anchor.getData(), companion, "anchor or placement no longer available");
                return;
            }

            AetherBaseMob member = NaturalSpawningListener.createMob(level, companion, mobLevel);
            Vec3 location = Vec3.atBottomCenterOf(pos);
            member.snapTo(location);
            member.applyRelativeHome();
            if (member.isRemoved() || !EncounterSpawnPlacement.fits(level, member)) {
                member.discard();
                debug(seed, anchor.getData(), companion, "companion hitbox does not fit");
                return;
            }
            if (!level.addFreshEntity(member, CreatureSpawnEvent.SpawnReason.NATURAL)) {
                debug(seed, anchor.getData(), companion, "companion spawn rejected");
                return;
            }

            level.getChunkSource().chunkMap.updatePlayerMobTypeMap(member);
            QxlInstanceScope.syncVisibility(member);
            if (!anchor.hasHome()) {
                anchor.setHomeTo(anchor.blockPosition(), 8);
            }
            if (!member.hasHome()) {
                member.setHomeTo(anchor.blockPosition(), 8);
            }
            encounter.companion = member.getUUID();
            debug(seed, anchor.getData(), companion, "spawned at " + pos.toShortString());
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Could not spawn encounter companion " + companion.getID() + ": " + e.getMessage());
        } finally {
            if (encounter.companion == null) {
                encounters.remove(cell, encounter);
            }
        }
    }

    private BlockPos findPlacement(ServerLevel level, AetherBaseMob anchor, NPCData data,
                                   Region region, RandomSource random, long seed) {
        BlockPos origin = anchor.blockPosition();
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            int dx = random.nextInt(11) - 5;
            int dz = random.nextInt(11) - 5;
            if (dx * dx + dz * dz < 9 || dx * dx + dz * dz > 36) {
                continue;
            }
            BlockPos pos = origin.offset(dx, 0, dz);
            if (canPlace(level, anchor, data, region, pos, seed)) {
                return pos;
            }
        }
        return null;
    }

    private boolean canPlace(ServerLevel level, AetherBaseMob anchor, NPCData data, Region region, BlockPos pos, long seed) {
        Vec3 point = Vec3.atBottomCenterOf(pos);
        AABB bounds = data.getNaturalSpawnType().getSpawnAABB(point);
        BlockPos origin = anchor.blockPosition();
        if (!level.isPositionEntityTicking(pos) || !level.getWorldBorder().isWithinBounds(bounds)
                || !level.hasChunksAt(Math.min(origin.getX(), pos.getX()) - 2, Math.min(origin.getZ(), pos.getZ()) - 2,
                        Math.max(origin.getX(), pos.getX()) + 2, Math.max(origin.getZ(), pos.getZ()) + 2)
                || !Objects.equals(region, regions.getRegionByLocation(
                        new Location(level.getWorld(), point.x, point.y, point.z)))
                || !level.getBiome(pos).equals(level.getBiome(origin))
                || !SpawnPlacements.isSpawnPositionOk(data.getNaturalSpawnType(), level, pos)
                || !SpawnPlacements.checkSpawnRules(data.getNaturalSpawnType(), level, EntitySpawnReason.NATURAL,
                        pos, RandomSource.create(seed ^ pos.asLong()))
                || !level.noCollision(null, bounds) || level.containsAnyLiquid(bounds)) {
            return false;
        }

        boolean publicPlayerNearby = false;
        for (var player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            double distance = player.distanceToSqr(point);
            if (distance < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE) {
                return false;
            }
            if (distance <= MAX_PLAYER_DISTANCE * MAX_PLAYER_DISTANCE
                    && QxlInstanceScope.getPlayerScope(player.getBukkitEntity()) == null) {
                publicPlayerNearby = true;
            }
        }
        if (!publicPlayerNearby) {
            return false;
        }

        // A clear corridor with footing in the middle avoids pairs split by a wall or drop.
        Vec3 midpoint = point.lerp(anchor.position(), 0.5);
        if (!SpawnPlacements.isSpawnPositionOk(data.getNaturalSpawnType(), level, BlockPos.containing(midpoint))) {
            return false;
        }
        double height = isRanged(data) ? 1.4 : 0.35;
        ClipContext corridor = new ClipContext(point.add(0, height, 0), anchor.position().add(0, height, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, anchor);
        return level.clip(corridor).getType() == HitResult.Type.MISS;
    }

    private int occupiedSlots(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(BUDGET_RADIUS, 8, BUDGET_RADIUS);
        int existing = EncounterSpawnPlacement.publicHostiles(level, pos);
        if (encounters.isEmpty()) {
            return existing;
        }
        int reserved = 0;
        Cell center = Cell.at(level, pos);
        for (int x = -1; x <= 1; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Encounter encounter = encounters.get(center.offset(x, y, z));
                    if (encounter != null && encounter.pending && area.contains(Vec3.atBottomCenterOf(encounter.placement))) {
                        reserved++;
                    }
                }
            }
        }
        return existing + reserved;
    }

    private Encounter nearbyEncounter(ServerLevel level, BlockPos pos, int radius) {
        if (encounters.isEmpty()) {
            return null;
        }
        Cell center = Cell.at(level, pos);
        Encounter local = encounters.get(center);
        if (local != null) {
            return local;
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Encounter encounter = encounters.get(center.offset(x, y, z));
                    if (encounter != null && encounter.origin.distSqr(pos) <= radius * radius) {
                        return encounter;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isEligible(NPCData data) {
        return data.getMobCategoryOverride() == MobCategory.MONSTER && !data.isPersistent()
                && !ModelledMob.class.isAssignableFrom(data.getEntityClass())
                && !data.isInstancable() && !data.isInvulnerable() && !data.hasBossBreakbar()
                && data.getBehaviorDefinition() == null && isHostile(data);
    }

    private void debug(long seed, NPCData anchor, NPCData companion, String outcome) {
        if (Aether.AETHER_DEBUG_MODE) {
            plugin.getLogger().info("Natural encounter " + seed + " [" + anchor.getID() + ", "
                    + companion.getID() + "] " + outcome);
        }
    }

    private record Cell(ServerLevel level, int x, int y, int z) {
        Cell offset(int dx, int dy, int dz) {
            return new Cell(level, x + dx, y + dy, z + dz);
        }

        static Cell at(ServerLevel level, BlockPos pos) {
            return new Cell(level, pos.getX() >> 5, pos.getY() >> 4, pos.getZ() >> 5);
        }
    }

    private static final class Encounter {
        private final ServerLevel level;
        private final UUID anchor;
        private final BlockPos origin;
        private final BlockPos placement;
        private UUID companion;
        private boolean pending = true;
        private int refillAt;

        Encounter(ServerLevel level, UUID anchor, BlockPos origin, BlockPos placement) {
            this.level = level;
            this.anchor = anchor;
            this.origin = origin;
            this.placement = placement;
        }

        boolean hasLivingMembers() {
            return alive(anchor) || alive(companion);
        }

        boolean expired(int now) {
            if (pending || hasLivingMembers()) {
                refillAt = 0;
                return false;
            }
            if (refillAt == 0) {
                refillAt = now + REFILL_TICKS;
            }
            return now >= refillAt;
        }

        private boolean alive(UUID id) {
            Entity entity = id == null ? null : level.getEntity(id);
            return entity != null && entity.isAlive();
        }
    }
}
