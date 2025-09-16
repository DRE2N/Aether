package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.factions.Factions;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionManager;
import de.erethon.factions.region.RegionMode;
import de.erethon.papyrus.events.MobSpawnFinalizedEvent;
import de.erethon.papyrus.events.MobSpawnForBiomeEvent;
import de.erethon.papyrus.events.PreCategorySpawnEvent;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
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

        NPCData data = null;
        if (region != null) {
            if (!regionNpcDataCache.containsKey(region)) {
                WeightedList.Builder<NPCData> builder = WeightedList.builder();
                WeightedList<String> regionMobs = spawnConfig.getSpawnsForRegion(region.getName());
                for (var entry : regionMobs.unwrap()) {
                    NPCData npcData = plugin.getCreatureManager().getByID(entry.value());
                    if (npcData != null) {
                        builder.add(npcData, entry.weight());
                    }
                }
                regionNpcDataCache.put(region, builder.build());
            }
            WeightedList<NPCData> npcDataList = regionNpcDataCache.get(region);
            if (!npcDataList.isEmpty()) {
                Optional<NPCData> selected = npcDataList.getRandom(event.getLevel().getRandom());
                if (selected.isPresent()) {
                    data = selected.get();
                }
            }

            if (data == null) {
                Holder<Biome> biome = event.getLevel().getBiome(event.getPos());
                if (!npcDataCache.containsKey(biome)) {
                    WeightedList.Builder<NPCData> builder = WeightedList.builder();
                    WeightedList<String> biomeMobs = spawnConfig.getSpawnsForBiome(biome);
                    for (var entry : biomeMobs.unwrap()) {
                        NPCData npcData = plugin.getCreatureManager().getByID(entry.value());
                        if (npcData != null) {
                            builder.add(npcData, entry.weight());
                        }
                    }
                    npcDataCache.put(biome, builder.build());
                }
                WeightedList<NPCData> npcDataList2 = npcDataCache.get(biome);
                if (!npcDataList2.isEmpty()) {
                    Optional<NPCData> selected = npcDataList2.getRandom(event.getLevel().getRandom());
                    if (selected.isPresent()) {
                        data = selected.get();
                    }
                }
            }

            if (data == null) {
                return;
            }
            if (data.getMobCategoryOverride() != event.getMobCategory()) {
                return; // Only spawn if the categories match
            }
            Class<? extends AetherBaseMob> toSpawn = data.getEntityClass();
            AetherBaseMob activeNPC;
            Mob currentMob = event.getMob();
            int level = -1;
            if (region.getLowerLevelBound() != 0 && region.getUpperLevelBound() != 0 && region.getUpperLevelBound() > region.getLowerLevelBound()) {
                level = random.nextInt(region.getLowerLevelBound(), region.getUpperLevelBound());
            }
            try {
                if (level != -1) {
                    activeNPC = toSpawn.getConstructor(NPCData.class, World.class, Integer.class).newInstance(data, event.getLevel().getWorld(), level);
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

    }

    @EventHandler
    public void onMobSpawnForBiome(MobSpawnForBiomeEvent event) {
        if (event.getMobCategory() != MobCategory.MONSTER) {
            return;
        }
        WeightedList<String> customSpawns = null;
        Location loc = new Location(event.getLevel().getWorld(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());

        Region region = regionManager.getRegionByLocation(loc);
        if (region != null) {
            customSpawns = spawnConfig.getSpawnsForRegion(region.getName());
        }
        if (customSpawns == null || customSpawns.isEmpty()) {
            Holder<Biome> biome = event.getLevel().getBiome(event.getPos());
            if (spawnerDataCache.containsKey(biome)) {
                event.setSpawnerData(spawnerDataCache.get(biome));
                return;
            }
            customSpawns = spawnConfig.getSpawnsForBiome(biome);
        }
        if (customSpawns.isEmpty()) {
            return;
        }

        WeightedList.Builder<MobSpawnSettings.SpawnerData> placeholderBuilder = WeightedList.builder();
        for (var weightedEntry : customSpawns.unwrap()) {
            int weight = weightedEntry.weight();
            MobSpawnSettings.SpawnerData placeholder = new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 1, 1);
            placeholderBuilder.add(placeholder, weight);
        }
        event.setSpawnerData(placeholderBuilder.build());
        spawnerDataCache.put(event.getBiome(), event.getSpawnerData());
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
