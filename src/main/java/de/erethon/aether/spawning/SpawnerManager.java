package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpawnerManager extends BukkitRunnable implements Listener {

    private final List<AESpawner> configuredSpawners = new ArrayList<>();
    private final HashMap<ChunkPos, Set<AESpawner>> spawnersbyChunk = new HashMap<>();
    private final Set<AESpawner> activeSpawners = new HashSet<>();

    public SpawnerManager() {
        loadSpawners();
        runTaskTimer(Aether.getInstance(), 0, 20);
    }

    @Override
    public void run() {
        for (AESpawner spawner : activeSpawners) {
            if (spawner.isTicking()) {
                spawner.spawn();
            }
        }
    }

    public void loadSpawners() {
        for (File file : Aether.SPAWNERS.listFiles()) {
            YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            for (String key : fileConfig.getKeys(false)) {
                try {
                    configuredSpawners.add(new AESpawner(fileConfig.getConfigurationSection(key), key));
                } catch (Exception e) {
                    Aether.addException("SpawnerManager.loadSpawners", "Error while loading spawner " + key, "Error while loading spawner " + key, e);
                    e.printStackTrace();
                }
            }
        }
        Aether.log("Loaded " + configuredSpawners.size() + " spawners.");
        Bukkit.getPluginManager().registerEvents(this, Aether.getInstance());
        for (AESpawner spawner : configuredSpawners) {
            int x = spawner.getCenterLocation().blockX();
            int z = spawner.getCenterLocation().blockZ();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            Set<AESpawner> spawnersAtChunk = spawnersbyChunk.getOrDefault(new ChunkPos(chunkX, chunkZ), new HashSet<>());
            spawnersAtChunk.add(spawner);
            spawnersbyChunk.put(new ChunkPos(chunkX, chunkZ), spawnersAtChunk);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        int x = event.getChunk().getX();
        int z = event.getChunk().getZ();
        Set<AESpawner> spawnersAtChunk = spawnersbyChunk.getOrDefault(new ChunkPos(x, z), new HashSet<>());
        activeSpawners.addAll(spawnersAtChunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        int x = event.getChunk().getX();
        int z = event.getChunk().getZ();
        Set<AESpawner> spawnersAtChunk = spawnersbyChunk.getOrDefault(new ChunkPos(x, z), new HashSet<>());
        activeSpawners.removeAll(spawnersAtChunk);
    }

    public void triggerSpawner(String id) {
        for (AESpawner spawner : configuredSpawners) {
            if (spawner.getId().equals(id)) {
                spawner.spawn();
            }
        }
    }

    public void reloadSpawners() {
        configuredSpawners.clear();
        spawnersbyChunk.clear();
        activeSpawners.clear();
        loadSpawners();
    }

}
