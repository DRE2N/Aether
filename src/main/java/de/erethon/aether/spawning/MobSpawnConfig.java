package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.tools.ErrorEntry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.HashMap;

public class MobSpawnConfig {

    private final Aether plugin = Aether.getInstance();

    private final HashMap<String, WeightedList<String>> biomeSpawns = new HashMap<>();
    private final HashMap<String, WeightedList<String>> regionSpawns = new HashMap<>();
    private final HashMap<String, Double> biomeSpawnChances = new HashMap<>();
    private final HashMap<String, Double> regionSpawnChances = new HashMap<>();

    public MobSpawnConfig() {
        File file = new File(plugin.getDataFolder(), "mob-spawns.yml");
        if (!file.exists()) {
            Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Configuration file not found", "Ensure the mob-spawns.yml file exists in the plugin's data folder.", null));
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            load(config);
        } catch (Exception e) {
            Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Error loading configuration", "Check the mob-spawns.yml file for syntax errors.", e.getStackTrace()));
        }
    }

    public WeightedList<String> getSpawnsForBiome(Holder<Biome> biomeHolder) {
        String biomeName = biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(Object::toString)
                .orElse("minecraft:plains"); // Fallback for safety
        return biomeSpawns.getOrDefault(biomeName, WeightedList.of());
    }

    public WeightedList<String> getSpawnsForRegion(String regionName) {
        return regionSpawns.getOrDefault(regionName, WeightedList.of());
    }

    public double getBiomeSpawnChance(Holder<Biome> biomeHolder) {
        String biomeName = biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(Object::toString)
                .orElse("minecraft:plains");
        return biomeSpawnChances.getOrDefault(biomeName, 1.0);
    }

    public double getRegionSpawnChance(String regionName) {
        return regionSpawnChances.getOrDefault(regionName, 1.0);
    }

    public void reload() {
        biomeSpawns.clear();
        regionSpawns.clear();
        biomeSpawnChances.clear();
        regionSpawnChances.clear();

        File file = new File(plugin.getDataFolder(), "mob-spawns.yml");
        if (!file.exists()) {
            Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Configuration file not found", "Ensure the mob-spawns.yml file exists in the plugin's data folder.", null));
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            load(config);
        } catch (Exception e) {
            Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Error loading configuration", "Check the mob-spawns.yml file for syntax errors.", e.getStackTrace()));
        }
    }

    private void load(YamlConfiguration config) {
        ConfigurationSection biomesSection = config.getConfigurationSection("biomes");
        if (biomesSection != null) {
            for (String biomeName : biomesSection.getKeys(false)) {
                ConfigurationSection biomeSection = biomesSection.getConfigurationSection(biomeName);
                if (biomeSection != null) {
                    // Load spawn chance
                    double spawnChance = biomeSection.getDouble("spawnChance", 1.0);
                    biomeSpawnChances.put(biomeName, spawnChance);

                    // Load mobs
                    WeightedList.Builder<String> builder = WeightedList.builder();
                    ConfigurationSection mobsSection = biomeSection.getConfigurationSection("mobs");
                    if (mobsSection != null) {
                        for (String mobId : mobsSection.getKeys(false)) {
                            int weight = mobsSection.getInt(mobId, 1);
                            builder.add(mobId, weight);
                        }
                    }
                    biomeSpawns.put(biomeName, builder.build());
                    Aether.log("Loaded biome spawns for " + biomeName + " (spawn chance: " + spawnChance + ")");
                }
            }
        }

        ConfigurationSection regionsSection = config.getConfigurationSection("regions");
        if (regionsSection != null) {
            for (String regionName : regionsSection.getKeys(false)) {
                ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionName);
                if (regionSection != null) {
                    // Load spawn chance
                    double spawnChance = regionSection.getDouble("spawnChance", 1.0);
                    regionSpawnChances.put(regionName, spawnChance);

                    // Load mobs
                    WeightedList.Builder<String> builder = WeightedList.builder();
                    ConfigurationSection mobsSection = regionSection.getConfigurationSection("mobs");
                    if (mobsSection != null) {
                        for (String mobId : mobsSection.getKeys(false)) {
                            int weight = mobsSection.getInt(mobId, 1);
                            builder.add(mobId, weight);
                        }
                    }
                    regionSpawns.put(regionName, builder.build());
                    Aether.log("Loaded region spawns for " + regionName + " (spawn chance: " + spawnChance + ")");
                }
            }
        }
    }
}
