package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.factions.Factions;
import de.erethon.factions.region.PvERegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionManager;
import de.erethon.factions.region.RegionMode;
import de.erethon.papyrus.events.MobSpawnFinalizedEvent;
import de.erethon.papyrus.events.MobSpawnForBiomeEvent;
import de.erethon.papyrus.events.PreCategorySpawnEvent;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

public class NaturalSpawningListener implements Listener {

    private final Aether plugin = Aether.getInstance();
    private final Factions factions = Factions.get();
    private final RegionManager regionManager = factions.getRegionManager();
    private final MobSpawnConfig spawnConfig;

    // Cache some values
    private final HashMap<Holder<Biome>, WeightedList<MobSpawnSettings.SpawnerData>> spawnerDataCache = new HashMap<>();
    private final HashMap<Holder<Biome>, WeightedList<NPCData>> npcDataCache = new HashMap<>();
    private final HashMap<Region, WeightedList<NPCData>> regionNpcDataCache = new HashMap<>();

    private final Random random = new Random();

    public NaturalSpawningListener(MobSpawnConfig spawnConfig) {
        this.spawnConfig = spawnConfig;
    }

    @EventHandler
    private void onFinalizeSpawn(MobSpawnFinalizedEvent event) {
        Location loc = new Location(event.getLevel().getWorld(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        Region region = regionManager.getRegionByLocation(loc);
        Holder<Biome> biome = event.getLevel().getBiome(event.getPos());

        NPCData data = region == null ? null : selectMatchingMob(getRegionNpcData(region), event.getMobCategory(), event.getLevel().getRandom());
        if (data == null) {
            data = selectMatchingMob(getBiomeNpcData(biome), event.getMobCategory(), event.getLevel().getRandom());
        }
        if (data == null) {
            return;
        }

        Class<? extends AetherBaseMob> toSpawn = data.getEntityClass();
        AetherBaseMob activeNPC;
        Mob currentMob = event.getMob();
        int mobLevel = selectRegionLevel(region);
        try {
            if (mobLevel != -1) {
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class, Integer.class).newInstance(data, event.getLevel().getWorld(), mobLevel);
            } else {
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class).newInstance(data, event.getLevel().getWorld());
            }
            activeNPC.setPos(currentMob.getX(), currentMob.getY(), currentMob.getZ());
            activeNPC.spawnReason = CreatureSpawnEvent.SpawnReason.NATURAL;
            event.setMob(activeNPC);
            event.getLevel().chunkSource.chunkMap.updatePlayerMobTypeMap(activeNPC);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            plugin.getLogger().warning("Failed to spawn custom mob " + data.getID() + ": " + e.getMessage());
        }
    }

    private WeightedList<NPCData> getRegionNpcData(Region region) {
        if (!regionNpcDataCache.containsKey(region)) {
            regionNpcDataCache.put(region, buildNpcDataList(spawnConfig.getSpawnsForRegion(region.getName())));
        }
        return regionNpcDataCache.get(region);
    }

    private WeightedList<NPCData> getBiomeNpcData(Holder<Biome> biome) {
        if (!npcDataCache.containsKey(biome)) {
            npcDataCache.put(biome, buildNpcDataList(spawnConfig.getSpawnsForBiome(biome)));
        }
        return npcDataCache.get(biome);
    }

    private WeightedList<NPCData> buildNpcDataList(WeightedList<String> configuredMobs) {
        WeightedList.Builder<NPCData> builder = WeightedList.builder();
        for (var entry : configuredMobs.unwrap()) {
            NPCData npcData = plugin.getCreatureManager().getByID(entry.value());
            if (npcData != null) {
                builder.add(npcData, entry.weight());
            }
        }
        return builder.build();
    }

    private NPCData selectMatchingMob(WeightedList<NPCData> npcDataList, MobCategory mobCategory, RandomSource randomSource) {
        if (npcDataList.isEmpty()) {
            return null;
        }
        WeightedList.Builder<NPCData> builder = WeightedList.builder();
        for (var entry : npcDataList.unwrap()) {
            NPCData npcData = entry.value();
            if (npcData.getMobCategoryOverride() == mobCategory) {
                builder.add(npcData, entry.weight());
            }
        }
        WeightedList<NPCData> matchingMobs = builder.build();
        if (matchingMobs.isEmpty()) {
            return null;
        }
        Optional<NPCData> selected = matchingMobs.getRandom(randomSource);
        return selected.orElse(null);
    }

    private int selectRegionLevel(Region region) {
        if (!(region instanceof PvERegion pveRegion)) {
            return -1;
        }
        int lowerLevelBound = pveRegion.getLowerLevelBound();
        int upperLevelBound = pveRegion.getUpperLevelBound();
        if (lowerLevelBound == -1 || upperLevelBound == -1 || upperLevelBound <= lowerLevelBound) {
            return -1;
        }
        return random.nextInt(lowerLevelBound, upperLevelBound + 1);
    }

    private WeightedList<MobSpawnSettings.SpawnerData> buildPlaceholderSpawns(WeightedList<String> customSpawns, MobCategory mobCategory) {
        WeightedList.Builder<MobSpawnSettings.SpawnerData> placeholderBuilder = WeightedList.builder();
        for (var weightedEntry : customSpawns.unwrap()) {
            NPCData npcData = plugin.getCreatureManager().getByID(weightedEntry.value());
            if (npcData == null || npcData.getMobCategoryOverride() != mobCategory) {
                continue;
            }
            MobSpawnSettings.SpawnerData placeholder = new MobSpawnSettings.SpawnerData(npcData.getNaturalSpawnType(), 1, 1);
            placeholderBuilder.add(placeholder, weightedEntry.weight());
        }
        return placeholderBuilder.build();
    }

    @EventHandler
    public void onMobSpawnForBiome(MobSpawnForBiomeEvent event) {
        if (event.getMobCategory() != MobCategory.MONSTER) {
            return;
        }
        Location loc = new Location(event.getLevel().getWorld(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        Region region = regionManager.getRegionByLocation(loc);
        if (region != null) {
            WeightedList<MobSpawnSettings.SpawnerData> regionPlaceholders = buildPlaceholderSpawns(spawnConfig.getSpawnsForRegion(region.getName()), event.getMobCategory());
            if (!regionPlaceholders.isEmpty()) {
                event.setSpawnerData(regionPlaceholders);
                return;
            }
        }

        Holder<Biome> biome = event.getLevel().getBiome(event.getPos());
        if (spawnerDataCache.containsKey(biome)) {
            event.setSpawnerData(spawnerDataCache.get(biome));
            return;
        }

        WeightedList<MobSpawnSettings.SpawnerData> biomePlaceholders = buildPlaceholderSpawns(spawnConfig.getSpawnsForBiome(biome), event.getMobCategory());
        if (biomePlaceholders.isEmpty()) {
            return;
        }
        event.setSpawnerData(biomePlaceholders);
        spawnerDataCache.put(biome, biomePlaceholders);
    }

    @EventHandler
    private void onSpawnForCategory(PreCategorySpawnEvent event) {
        // Abort mob spawning if no custom spawns are defined for the biome and region
        if (event.getMobCategory() != MobCategory.MONSTER) {
            return; // Only interfere with monster spawning
        }
        Location loc = new Location(event.getLevel().getWorld(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        Region region = regionManager.getRegionByLocation(loc);

        double spawnChance;
        boolean hasCustomSpawns;

        if (region != null) {
            if (region.getMode() == RegionMode.SAFE_ZONE || region.getMode() == RegionMode.PVP) {
                event.setShouldAbortVanillaLogic(true);
                return; // No mob spawning in safe zones and PvP zones
            }

            spawnChance = spawnConfig.getRegionSpawnChance(region.getName());
            WeightedList<String> regionMobs = spawnConfig.getSpawnsForRegion(region.getName());
            WeightedList<String> biomeMobs = spawnConfig.getSpawnsForBiome(event.getLevel().getBiome(event.getPos()));
            hasCustomSpawns = !regionMobs.isEmpty() || !biomeMobs.isEmpty();

            // If no region-specific spawn chance is set, fall back to biome spawn chance
            if (spawnChance == 1.0) {
                spawnChance = spawnConfig.getBiomeSpawnChance(event.getLevel().getBiome(event.getPos()));
            }
        } else {
            spawnChance = spawnConfig.getBiomeSpawnChance(event.getLevel().getBiome(event.getPos()));
            WeightedList<String> biomeMobs = spawnConfig.getSpawnsForBiome(event.getLevel().getBiome(event.getPos()));
            hasCustomSpawns = !biomeMobs.isEmpty();
        }

        // Apply spawn chance - abort if chance check fails
        if (spawnChance < 1.0 && random.nextDouble() > spawnChance) {
            event.setShouldAbortVanillaLogic(true);
            return;
        }

        if (region != null) {
            WeightedList<String> regionMobs = spawnConfig.getSpawnsForRegion(region.getName());
            WeightedList<String> biomeMobs = spawnConfig.getSpawnsForBiome(event.getLevel().getBiome(event.getPos()));
            if (regionMobs.isEmpty() && biomeMobs.isEmpty()) {
                event.setShouldAbortVanillaLogic(true);
            }
            return;
        }

        if (!hasCustomSpawns) {
            event.setShouldAbortVanillaLogic(true);
        }
    }
}
