package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.tools.ErrorEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MobSpawnConfig {

    private final Aether plugin = Aether.getInstance();

    private final HashMap<Holder<Biome>, WeightedList<MobSpawnSettings.SpawnerData>> biomeSpawns = new HashMap<>();

    public MobSpawnConfig() {
        File file = new File(plugin.getDataFolder(), "biome-spawns.yml");
        if (!file.exists()) {
            Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Configuration file not found", "Ensure the mob-spawn.yml file exists in the plugin's data folder.", null));
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            load(config);
        } catch (Exception e) {
            Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Error loading configuration", "Check the mob-spawn.yml file for syntax errors.", e.getStackTrace()));
        }
    }

    protected WeightedList<MobSpawnSettings.SpawnerData> getSpawnsForBiome(Holder<Biome> biome) {
        return biomeSpawns.getOrDefault(biome, WeightedList.of());
    }

    private void load(YamlConfiguration config) {
        World world = Bukkit.getWorlds().getFirst();
        CraftWorld craftWorld = (CraftWorld) world;
        Level level = craftWorld.getHandle();
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (String biomeName : config.getConfigurationSection("biomes").getKeys(false)) {
            Biome biome = biomeRegistry.getValue(ResourceLocation.tryParse(biomeName));
            if (biome == null) {
                Aether.getErrors().add(new ErrorEntry("MobSpawnConfig", "Invalid biome name: " + biomeName, "Check your mob spawn configuration file.", null));
                continue;
            }
            Holder<Biome> biomeHolder = biomeRegistry.wrapAsHolder(biome);
            List<Weighted<MobSpawnSettings.SpawnerData>> spawns = new ArrayList<>();
            for (String spawnKey : config.getConfigurationSection("biomes." + biomeName).getKeys(false)) {
                String entityType = config.getString("biomes." + biomeName + "." + spawnKey + ".entity");
                int weight = config.getInt("biomes." + biomeName + "." + spawnKey + ".weight");
                int minCount = config.getInt("biomes." + biomeName + "." + spawnKey + ".minCount");
                int maxCount = config.getInt("biomes." + biomeName + "." + spawnKey + ".maxCount");
                EntityType<? extends Entity> type = BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.tryParse(entityType));
                MobSpawnSettings.SpawnerData spawnerData = new MobSpawnSettings.SpawnerData(type, minCount, maxCount);
                Weighted<MobSpawnSettings.SpawnerData> weighted = new Weighted<>(spawnerData, weight);
                spawns.add(weighted);
            }
            biomeSpawns.put(biomeHolder, WeightedList.of(spawns));
        }
    }
}
